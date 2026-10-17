import { expect, test } from "@playwright/test";
import { emptyAppearanceSnapshot, APPEARANCE_OPTIONS } from "../fixtures/appearance";
import { emptyAppearance, validateAppearance, sameAppearance } from "../../src/lib/appearance/types";
import { parseAppearance } from "../../src/lib/api/appearance-contract";
import { AuthClient } from "../../src/lib/api/auth-client";
import { ProfileClient } from "../../src/lib/api/profile-client";
import { SessionApiClient, StaleSessionError } from "../../src/lib/api/session-client";

const original = globalThis.fetch;
const version = "11111111-1111-4111-8111-111111111111";
type Call = { path: string; init: RequestInit };
let calls: Call[]; let handler: (call: Call) => Response | Promise<Response>;
let auth: AuthClient; let profiles: ProfileClient;
const user = { id: 7, fullName: "Example User", email: "user@example.com" };
function normal({ path, init }: Call) {
  if (path.endsWith("csrf")) return Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic-appearance-csrf" });
  if (path.endsWith("appearance")) return init.method === "DELETE" ? new Response(null, { status: 204 }) : Response.json(emptyAppearanceSnapshot());
  if (path.endsWith("logout")) return new Response(null, { status: 204 });
  return Response.json(user);
}
test.beforeEach(async () => {
  calls = []; handler = normal;
  globalThis.fetch = async (url, init = {}) => { const call = { path: new URL(String(url)).pathname, init }; calls.push(call); return handler(call); };
  const transport = new SessionApiClient("http://localhost:8080"); auth = new AuthClient(transport); profiles = new ProfileClient(transport); await auth.me(); calls = [];
});
test.afterEach(() => { globalThis.fetch = original; });
test("all fields are optional, height is bounded whole cm, and false differs from unanswered", () => {
  expect(validateAppearance(emptyAppearance(), APPEARANCE_OPTIONS)).toEqual({});
  for (const height of [49, 261, 170.5, NaN]) expect(validateAppearance({ ...emptyAppearance(), heightCm: height }, APPEARANCE_OPTIONS)).toHaveProperty("heightCm");
  for (const height of [50, 260]) expect(validateAppearance({ ...emptyAppearance(), heightCm: height }, APPEARANCE_OPTIONS)).toEqual({});
  expect(sameAppearance(emptyAppearance(), { ...emptyAppearance(), paletteSuggestions: false })).toBe(false);
});
test("response parsing preserves nullable values and copies no unexpected sensitive fields", () => {
  expect(parseAppearance({ ...emptyAppearanceSnapshot(), passwordHash: "synthetic", weight: 80 })).toEqual(emptyAppearanceSnapshot());
  expect(() => parseAppearance({ ...emptyAppearanceSnapshot(), details: {} })).toThrow("unexpected response");
  expect(() => parseAppearance({ ...emptyAppearanceSnapshot(), version: "not-a-revision" })).toThrow();
});
test("appearance mutations share credentialed transport and send only observed version/details", async () => {
  const client = profiles.forAccount(7); await client.appearance();
  await client.saveAppearance({ version: null, details: emptyAppearance() }); await client.deleteAppearance(version);
  const writes = calls.filter((call) => ["PUT", "DELETE"].includes(call.init.method ?? ""));
  expect(writes).toHaveLength(2); expect(calls.filter((call) => call.path.endsWith("csrf"))).toHaveLength(1);
  for (const call of writes) { expect(call.init.credentials).toBe("include"); expect(call.init.cache).toBe("no-store"); expect(new Headers(call.init.headers).get("X-XSRF-TOKEN")).toBe("synthetic-appearance-csrf"); }
  expect(JSON.parse(String(writes[1].init.body))).toEqual({ version });
});
test("appearance validation fields and conflict messages are endpoint-specific", async () => {
  const client = profiles.forAccount(7);
  handler = (call) => call.init.method === "PUT" ? Response.json({ fieldErrors: { "details.heightCm": "Use whole centimeters", "details.weight": "hidden", email: "hidden" } }, { status: 400 }) : normal(call);
  await expect(client.saveAppearance({ version: null, details: emptyAppearance() })).rejects.toMatchObject({ fieldErrors: { "details.heightCm": "Use whole centimeters" } });
  handler = (call) => call.init.method === "DELETE" ? Response.json({}, { status: 409 }) : normal(call);
  await expect(client.deleteAppearance(version)).rejects.toThrow("personal details changed elsewhere");
});
test("DELETE CSRF refresh is bounded and rechecks owner on retry", async () => {
  let attempts = 0;
  handler = (call) => call.init.method === "DELETE" && ++attempts === 1 ? Response.json({ message: "Invalid or missing CSRF token" }, { status: 403 }) : normal(call);
  await profiles.forAccount(7).deleteAppearance(version);
  expect(attempts).toBe(2); expect(calls.filter((call) => call.path === "/api/auth/me")).toHaveLength(2);
});
test("uncertain DELETE is marked uncertain without automatic replay", async () => {
  handler = (call) => { if (call.init.method === "DELETE") throw new TypeError("Network unavailable"); return normal(call); };
  await expect(profiles.forAccount(7).deleteAppearance(version)).rejects.toMatchObject({ uncertain: true });
  expect(calls.filter((call) => call.init.method === "DELETE")).toHaveLength(1);
});
test("an old bound appearance client cannot write after logout begins", async () => {
  const client = profiles.forAccount(7); const logout = auth.logout();
  const write = client.saveAppearance({ version: null, details: emptyAppearance() }).catch((failure) => failure);
  await logout; expect(await write).toBeInstanceOf(StaleSessionError);
  expect(calls.filter((call) => call.init.method === "PUT")).toHaveLength(0);
});
