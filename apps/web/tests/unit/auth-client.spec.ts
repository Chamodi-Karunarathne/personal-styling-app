import { expect, test } from "@playwright/test";
import { ApiError, AuthClient, readableError } from "../../src/lib/api/auth-client";

const user = { id: 1, fullName: "Example User", email: "user@example.com" };
const credentials = { email: user.email, password: "  ExamplePassword123  " };
const originalFetch = globalThis.fetch;
type Call = { path: string; init: RequestInit };
let calls: Call[];
let handler: (call: Call) => Response | Promise<Response>;

test.beforeEach(() => {
  calls = [];
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic-csrf" })
    : Response.json(user);
  globalThis.fetch = async (url, init = {}) => {
    const call = { path: new URL(String(url)).pathname.split("/").at(-1)!, init };
    calls.push(call);
    return handler(call);
  };
});
test.afterEach(() => { globalThis.fetch = originalFetch; });

test("requests include credentials, bypass caches, and copy only safe identity fields", async () => {
  handler = () => Response.json({ ...user, passwordHash: "synthetic-not-a-real-hash", password: "synthetic" });
  expect(await new AuthClient("http://localhost:8080").me()).toEqual(user);
  expect(calls[0].init.credentials).toBe("include");
  expect(calls[0].init.cache).toBe("no-store");
});

test("registration bootstraps CSRF and preserves password contents", async () => {
  const client = new AuthClient("http://localhost:8080");
  await client.register({ ...credentials, fullName: user.fullName });
  expect(calls.map((call) => call.path)).toEqual(["csrf", "register"]);
  expect(new Headers(calls[1].init.headers).get("X-XSRF-TOKEN")).toBe("synthetic-csrf");
  expect(JSON.parse(String(calls[1].init.body)).password).toBe(credentials.password);
});

test("login refreshes CSRF and logout uses the rotated token then refreshes again", async () => {
  let generation = 0;
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: `synthetic-${++generation}` })
    : path === "logout" ? new Response(null, { status: 204 }) : Response.json(user);
  const client = new AuthClient("http://localhost:8080");
  await client.login(credentials);
  await client.logout();
  expect(calls.map((call) => call.path)).toEqual(["csrf", "login", "csrf", "logout", "csrf"]);
  expect(new Headers(calls[3].init.headers).get("X-XSRF-TOKEN")).toBe("synthetic-2");
});

test("successful login is retained if the post-login CSRF refresh is unavailable", async () => {
  let csrfCalls = 0;
  handler = ({ path }) => path === "csrf" && ++csrfCalls > 1
    ? Response.json({}, { status: 503 })
    : path === "csrf" ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" }) : Response.json(user);
  expect(await new AuthClient("http://localhost:8080").login(credentials)).toEqual(user);
});

test("recognized CSRF rejection gets one refresh and retry", async () => {
  let postCalls = 0;
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" })
    : ++postCalls === 1 ? Response.json({ message: "Invalid or missing CSRF token" }, { status: 403 }) : Response.json(user);
  await new AuthClient("http://localhost:8080").register({ ...credentials, fullName: user.fullName });
  expect(calls.map((call) => call.path)).toEqual(["csrf", "register", "csrf", "register"]);
});

test("persistent CSRF rejection stops after one retry", async () => {
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" })
    : Response.json({ message: "Invalid or missing CSRF token" }, { status: 403 });
  await expect(new AuthClient("http://localhost:8080").register({ ...credentials, fullName: user.fullName }))
    .rejects.toMatchObject({ status: 403 });
  expect(calls.filter((call) => call.path === "register")).toHaveLength(2);
});

for (const status of [403, 500]) {
  test(`${status} unrelated failures are safe and never retry a POST`, async () => {
    handler = ({ path }) => path === "csrf"
      ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" })
      : Response.json({ message: "Internal SQL details must not be shown" }, { status });
    const result = new AuthClient("http://localhost:8080").register({ ...credentials, fullName: user.fullName });
    await expect(result).rejects.toMatchObject({ status });
    await expect(result).rejects.not.toThrow("Internal SQL");
    expect(calls.filter((call) => call.path === "register")).toHaveLength(1);
  });
}

test("ambiguous network failure never retries a submitted registration", async () => {
  handler = ({ path }) => {
    if (path === "csrf") return Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" });
    throw new TypeError("Network failure");
  };
  await expect(new AuthClient("http://localhost:8080").register({ ...credentials, fullName: user.fullName }))
    .rejects.toThrow("Unable to connect");
  expect(calls.filter((call) => call.path === "register")).toHaveLength(1);
});

test("login failures remain generic and validation fields are restricted to known inputs", async () => {
  const client = new AuthClient("http://localhost:8080");
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" })
    : Response.json({ message: "Account does not exist" }, { status: 401 });
  await expect(client.login(credentials)).rejects.toThrow("Invalid email or password");
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" })
    : Response.json({ fieldErrors: { email: "Email must be valid", internal: "hidden", password: 123 } }, { status: 400 });
  await expect(client.register({ ...credentials, fullName: user.fullName }))
    .rejects.toMatchObject({ fieldErrors: { email: "Email must be valid" } });
});

test("logout accepts an expired session but reports genuine failures", async () => {
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" }) : Response.json({}, { status: 401 });
  await new AuthClient("http://localhost:8080").logout();
  handler = ({ path }) => path === "csrf"
    ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" }) : Response.json({}, { status: 500 });
  await expect(new AuthClient("http://localhost:8080").logout()).rejects.toMatchObject({ status: 500 });
});

test("requests are serialized so restoration finishes before CSRF bootstrap and login", async () => {
  let release!: () => void;
  const wait = new Promise<void>((resolve) => { release = resolve; });
  handler = async ({ path }) => {
    if (path === "me") { await wait; return Response.json(user); }
    return path === "csrf" ? Response.json({ headerName: "X-XSRF-TOKEN", token: "synthetic" }) : Response.json(user);
  };
  const client = new AuthClient("http://localhost:8080");
  const read = client.me();
  const write = client.login(credentials);
  await expect.poll(() => calls.length).toBe(1);
  expect(calls[0].path).toBe("me");
  release();
  await Promise.all([read, write]);
  expect(calls.map((call) => call.path)).toEqual(["me", "csrf", "login", "csrf"]);
});

test("canceled queued requests never send credentials", async () => {
  const controller = new AbortController();
  controller.abort();
  await expect(new AuthClient("http://localhost:8080").login(credentials, controller.signal)).rejects.toThrow();
  expect(calls).toHaveLength(0);
});

test("invalid API origins and malformed CSRF responses fail safely", async () => {
  await expect(new AuthClient("https://user:secret@example.com").me()).rejects.toThrow("not configured correctly");
  expect(calls).toHaveLength(0);
  handler = () => Response.json({ headerName: "Unexpected-Header", token: "synthetic" });
  await expect(new AuthClient("http://localhost:8080").login(credentials)).rejects.toThrow("unexpected response");
  expect(readableError(new Error("Internal sensitive detail"))).toBe("Unable to connect. Please try again.");
  expect(readableError(new ApiError(409, "Email is already registered"))).toBe("Email is already registered");
});
