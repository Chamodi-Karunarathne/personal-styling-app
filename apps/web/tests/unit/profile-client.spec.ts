import { expect, test } from "@playwright/test";
import { AuthClient } from "../../src/lib/api/auth-client";
import { parseCatalog, parseProfile, ProfileClient } from "../../src/lib/api/profile-client";
import { SessionApiClient, StaleSessionError } from "../../src/lib/api/session-client";
import { INITIAL_CATALOG } from "../../src/lib/profile/catalog";
import { emptyProfile, type ProfileWrite } from "../../src/lib/profile/types";

const originalFetch = globalThis.fetch;
const user = { id: 7, fullName: "Example User", email: "user@example.com" };
const saved = { ...emptyProfile(), version: 0, onboardingStatus: "IN_PROGRESS", updatedAt: "2026-10-17T00:00:00Z" };
const input = (): ProfileWrite => ({ version: null, intent: "SAVE", lastVisitedStep: "STYLE", preferences: emptyProfile().preferences });
type Call = { path: string; init: RequestInit };
let calls: Call[];
let handler: (call: Call) => Response | Promise<Response>;
let transport: SessionApiClient;
let auth: AuthClient;
let profiles: ProfileClient;
function deferred() { let release!: () => void; const promise = new Promise<void>((resolve) => { release = resolve; }); return { promise, release }; }
function normal({ path, init }: Call): Response {
  if (path === "/api/auth/csrf") return Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic-csrf" });
  if (path === "/api/profile/options") return Response.json(INITIAL_CATALOG);
  if (path === "/api/profile/me") return Response.json(init.method === "PUT" ? saved : emptyProfile());
  if (path === "/api/auth/logout") return new Response(null, { status: 204 });
  return Response.json(user);
}
test.beforeEach(async () => {
  calls = []; handler = normal;
  globalThis.fetch = async (url, init = {}) => { const call = { path: new URL(String(url)).pathname, init }; calls.push(call); return handler(call); };
  transport = new SessionApiClient("http://localhost:8080"); auth = new AuthClient(transport); profiles = new ProfileClient(transport);
  await auth.me(); calls = [];
});
test.afterEach(() => { globalThis.fetch = originalFetch; });

test("auth and profile share CSRF cache, queue, credentials, and safe contracts", async () => {
  await auth.login({ email: user.email, password: "Example123" });
  const profile = profiles.forAccount(user.id);
  expect(await profile.options()).toEqual(INITIAL_CATALOG);
  expect(await profile.save(input())).toEqual(saved);
  expect(calls.filter((call) => call.path.endsWith("csrf"))).toHaveLength(2);
  const put = calls.find((call) => call.init.method === "PUT")!;
  expect(put.init.credentials).toBe("include"); expect(put.init.cache).toBe("no-store");
  expect(new Headers(put.init.headers).get("X-XSRF-TOKEN")).toBe("synthetic-csrf");
  expect(JSON.parse(String(put.init.body))).toEqual(input());
});
test("parsers reject incomplete snapshots and catalogs and omit unexpected identity fields", () => {
  expect(parseProfile({ ...emptyProfile(), passwordHash: "not-a-real-hash", userId: 99 })).toEqual(emptyProfile());
  expect(() => parseProfile({ ...emptyProfile(), preferences: {} })).toThrow("unexpected response");
  expect(() => parseProfile({ ...saved, version: -1 })).toThrow();
  expect(() => parseCatalog({ groups: [] })).toThrow();
  const catalog = structuredClone(INITIAL_CATALOG);
  expect(parseCatalog(catalog)).toEqual(INITIAL_CATALOG);
});

test("old four-step responses remain valid and new resume state stays within stored Review", () => {
  const legacy = { ...emptyProfile() }; delete legacy.appearanceStepPending;
  expect(parseProfile(legacy).appearanceStepPending).toBe(false);
  expect(parseProfile({ ...legacy, lastVisitedStep: "REVIEW", appearanceStepPending: true }).appearanceStepPending).toBe(true);
  expect(() => parseProfile({ ...legacy, lastVisitedStep: "APPEARANCE" })).toThrow();
  expect(() => parseProfile({ ...legacy, lastVisitedStep: "STYLE", appearanceStepPending: true })).toThrow();
});
test("profile conflicts are distinct from email conflicts and validation fields are restricted", async () => {
  const client = profiles.forAccount(user.id);
  handler = (call) => call.init.method === "PUT" ? Response.json({ message: "private details" }, { status: 409 }) : normal(call);
  await expect(client.save(input())).rejects.toThrow("preferences changed elsewhere");
  handler = (call) => call.init.method === "PUT" ? Response.json({ fieldErrors: {
    "preferences.coverage[0].strength": "Choose Preferred or Required", internal: "private", email: "wrong endpoint",
  } }, { status: 400 }) : normal(call);
  await expect(client.save(input())).rejects.toMatchObject({ fieldErrors: { "preferences.coverage": "Choose Preferred or Required" }, uncertain: false });
});
test("recognized CSRF rejection refreshes once and rechecks session ownership before retry", async () => {
  let writes = 0;
  handler = (call) => call.init.method === "PUT" && ++writes === 1
    ? Response.json({ message: "Invalid or missing CSRF token" }, { status: 403 }) : normal(call);
  await profiles.forAccount(user.id).save(input());
  expect(calls.filter((call) => call.path === "/api/auth/me")).toHaveLength(2);
  expect(calls.filter((call) => call.path === "/api/auth/csrf")).toHaveLength(2);
  expect(writes).toBe(2);
});
for (const failure of ["network", "500"]) test(`uncertain ${failure} PUT is reported without retry`, async () => {
  handler = (call) => {
    if (call.init.method !== "PUT") return normal(call);
    if (failure === "network") throw new TypeError("connection lost");
    return Response.json({ message: "database details" }, { status: 500 });
  };
  await expect(profiles.forAccount(user.id).save(input())).rejects.toMatchObject({ uncertain: true });
  expect(calls.filter((call) => call.init.method === "PUT")).toHaveLength(1);
});
test("queued account A writes never execute after login for account B starts", async () => {
  const wait = deferred();
  handler = async (call) => { if (call.path.endsWith("options")) await wait.promise; return normal(call); };
  const bound = profiles.forAccount(user.id);
  const read = bound.options().catch((error) => error);
  await expect.poll(() => calls.length).toBe(1);
  const write = bound.save(input()).catch((error) => error);
  const login = auth.login({ email: "second@example.com", password: "Example123" });
  wait.release();
  expect(await read).toBeInstanceOf(StaleSessionError); expect(await write).toBeInstanceOf(StaleSessionError);
  await login;
  expect(calls.filter((call) => call.init.method === "PUT")).toHaveLength(0);
});
test("scope invalidated during CSRF bootstrap cannot dispatch a queued PUT", async () => {
  const wait = deferred();
  handler = async (call) => { if (call.path.endsWith("csrf")) await wait.promise; return normal(call); };
  const save = profiles.forAccount(user.id).save(input()).catch((error) => error);
  await expect.poll(() => calls.some((call) => call.path.endsWith("csrf"))).toBe(true);
  const logout = auth.logout(); wait.release();
  expect(await save).toBeInstanceOf(StaleSessionError); await logout;
  expect(calls.filter((call) => call.init.method === "PUT")).toHaveLength(0);
});
test("a cross-tab account change detected by me prevents PUT", async () => {
  handler = (call) => call.path === "/api/auth/me" ? Response.json({ ...user, id: 8 }) : normal(call);
  await expect(profiles.forAccount(user.id).save(input())).rejects.toBeInstanceOf(StaleSessionError);
  expect(calls.filter((call) => call.init.method === "PUT")).toHaveLength(0);
});
test("old account-bound clients stay invalid even when the same account signs in again", async () => {
  const old = profiles.forAccount(user.id);
  await auth.logout(); await auth.login({ email: user.email, password: "Example123" });
  expect(old.isCurrent()).toBe(false);
  await expect(old.me()).rejects.toBeInstanceOf(StaleSessionError);
  expect(await profiles.forAccount(user.id).me()).toEqual(emptyProfile());
});
test("stale me response cannot rebind the transport after a new login begins", async () => {
  const wait = deferred();
  handler = async (call) => {
    if (call.path === "/api/auth/me") { await wait.promise; return Response.json(user); }
    if (call.path === "/api/auth/login") return Response.json({ ...user, id: 8 });
    return normal(call);
  };
  const read = auth.me(); await expect.poll(() => calls.length).toBe(1);
  const login = auth.login({ email: "second@example.com", password: "Example123" });
  wait.release(); await read; await login;
  expect(profiles.forAccount(user.id).isCurrent()).toBe(false);
  expect(profiles.forAccount(8).isCurrent()).toBe(true);
});
test("queued cancellation sends no PUT and callers cannot mutate the queued snapshot", async () => {
  const bound = profiles.forAccount(user.id);
  const controller = new AbortController(); controller.abort();
  await expect(bound.save(input(), controller.signal)).rejects.toThrow();
  const draft = input();
  const pending = bound.save(draft); draft.preferences.styles = ["FORMAL"];
  await pending;
  const put = calls.find((call) => call.init.method === "PUT")!;
  expect(JSON.parse(String(put.init.body)).preferences.styles).toBeNull();
});
