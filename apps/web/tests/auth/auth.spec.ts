import { expect, test, type Page } from "@playwright/test";

const account = { id: 7, fullName: "Example User", email: "user@example.com" };
const password = "ExamplePassword123";
type Failure = { status?: number; message?: string; fieldErrors?: Record<string, string>; abort?: boolean };

async function mockBackend(page: Page) {
  const state = {
    authenticated: false,
    token: "",
    csrfRequests: 0,
    csrfRejects: 0,
    calls: [] as { path: string; body: Record<string, string>; header: string | undefined; cookie: string }[],
    failures: {} as Record<string, Failure | undefined>,
    before: async (path: string) => { void path; },
  };
  await page.route("http://localhost:8080/api/auth/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.split("/").at(-1)!;
    const headers = {
      "Access-Control-Allow-Origin": "http://localhost:3000",
      "Access-Control-Allow-Credentials": "true",
      "Access-Control-Allow-Headers": "Content-Type, X-XSRF-TOKEN",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Cache-Control": "no-store",
    };
    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    const requestHeaders = await request.allHeaders();
    state.calls.push({ path, body: request.postDataJSON() ?? {}, header: requestHeaders["x-xsrf-token"], cookie: requestHeaders.cookie ?? "" });
    await state.before(path);
    const failure = state.failures[path];
    const error = async (status: number, message: string, fieldErrors = {}) => route.fulfill({
      status, headers, json: { timestamp: "2026-10-14T00:00:00Z", status, error: "Request failed", message, path: `/api/auth/${path}`, fieldErrors },
    });
    if (failure?.abort) { await route.abort("failed"); return; }
    if (failure?.status) { await error(failure.status, failure.message ?? "Request failed", failure.fieldErrors); return; }
    if (path === "csrf") {
      state.token = `synthetic-csrf-${++state.csrfRequests}`;
      await route.fulfill({ status: 200, headers: { ...headers, "Set-Cookie": `XSRF-TOKEN=${state.token}; Path=/; SameSite=Lax` }, json: { headerName: "X-XSRF-TOKEN", token: state.token } });
      return;
    }
    if (request.method() === "POST") {
      if (state.csrfRejects > 0) {
        state.csrfRejects--;
        await error(403, "Invalid or missing CSRF token");
        return;
      }
      if (!state.token || requestHeaders["x-xsrf-token"] !== state.token || !requestHeaders.cookie?.includes(`XSRF-TOKEN=${state.token}`)) {
        await error(403, "Invalid or missing CSRF token");
        return;
      }
    }
    if (path === "register") { await route.fulfill({ status: 201, headers, json: account }); return; }
    if (path === "login") {
      state.authenticated = true;
      await route.fulfill({ status: 200, headers: { ...headers, "Set-Cookie": "JSESSIONID=synthetic-session; Path=/; HttpOnly; SameSite=Lax" }, json: account });
      return;
    }
    if (path === "me") {
      if (state.authenticated && requestHeaders.cookie?.includes("JSESSIONID=")) await route.fulfill({ status: 200, headers, json: account });
      else await error(401, "Authentication is required");
      return;
    }
    if (path === "logout") {
      if (!state.authenticated) { await error(401, "Authentication is required"); return; }
      state.authenticated = false;
      await route.fulfill({ status: 204, headers: { ...headers, "Set-Cookie": "JSESSIONID=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0" } });
      return;
    }
    await error(404, "Not found");
  });
  return state;
}

async function fillLogin(page: Page, value = password) {
  await page.getByLabel("Email address", { exact: true }).fill(account.email);
  await page.getByLabel("Password", { exact: true }).fill(value);
}
async function signIn(page: Page) {
  await page.goto("/login");
  await fillLogin(page);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}
async function fillRegister(page: Page, value = password) {
  await page.getByLabel("Full name", { exact: true }).fill(account.fullName);
  await page.getByLabel("Email address", { exact: true }).fill(account.email);
  await page.getByLabel("Password", { exact: true }).fill(value);
}

test("register creates an account without signing in and preserves password whitespace", async ({ page }) => {
  const backend = await mockBackend(page);
  await page.goto("/register");
  await fillRegister(page, `  ${password}  `);
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("status")).toHaveText("Account created. Please sign in.");
  const call = backend.calls.find((call) => call.path === "register")!;
  expect(call.body).toEqual({ fullName: account.fullName, email: account.email, password: `  ${password}  ` });
  expect(call.header).toBeTruthy();
  expect(call.cookie).toContain("XSRF-TOKEN=");
  expect(backend.authenticated).toBe(false);
});

test("login, refresh restoration, safe dashboard and logout use rotated CSRF", async ({ page }) => {
  const backend = await mockBackend(page);
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await signIn(page);
  await expect(page.getByRole("heading", { name: /Welcome, Example User/ })).toBeVisible();
  await expect(page.getByText(account.email, { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("button", { name: "Sign out", exact: true })).toBeVisible();
  await expect.poll(() => backend.calls.filter((call) => call.path === "me").length).toBeGreaterThan(1);
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  expect(backend.authenticated).toBe(false);
  // Refresh clears the module's in-memory token and requires another bootstrap.
  expect(backend.csrfRequests).toBe(4);
  expect(backend.calls.find((call) => call.path === "logout")!.header).toBe("synthetic-csrf-3");
  expect(await page.evaluate(() => ({ local: localStorage.length, session: sessionStorage.length }))).toEqual({ local: 0, session: 0 });
  expect(await page.locator("body").innerText()).not.toContain(password);
  expect(errors).toEqual([]);
});

test("registration shows duplicate and backend field errors without navigation", async ({ page }) => {
  const backend = await mockBackend(page);
  backend.failures.register = { status: 409 };
  await page.goto("/register");
  await fillRegister(page);
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("Email is already registered");
  backend.failures.register = { status: 400, fieldErrors: { email: "Email must be valid" } };
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.getByLabel("Email address", { exact: true })).toHaveAttribute("aria-invalid", "true");
  await expect(page.getByLabel("Email address", { exact: true })).toBeFocused();
  await expect(page).toHaveURL(/\/register$/);
});

test("local required errors are associated and Unicode over-limit passwords never reach API", async ({ page }) => {
  const backend = await mockBackend(page);
  await page.goto("/register");
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.getByLabel("Full name", { exact: true })).toBeFocused();
  await expect(page.getByLabel("Full name", { exact: true })).toHaveAttribute("aria-describedby", "full-name-error");
  await fillRegister(page, "a".repeat(69) + "\u{1f600}");
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.getByText("Password must be at most 72 UTF-8 bytes")).toBeVisible();
  expect(backend.calls.filter((call) => call.path === "register")).toHaveLength(0);
  await page.goto("/login");
  await fillLogin(page, "a".repeat(69) + "\u{1f600}");
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page.getByText("Password must be at most 72 UTF-8 bytes")).toBeVisible();
  expect(backend.calls.filter((call) => call.path === "login")).toHaveLength(0);
});

test("exact Unicode byte boundary and login password whitespace are sent unchanged", async ({ page }) => {
  const backend = await mockBackend(page);
  await page.goto("/register");
  await fillRegister(page, "a".repeat(68) + "\u{1f600}");
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await fillLogin(page, `  ${password}  `);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  expect(new TextEncoder().encode(backend.calls.find((call) => call.path === "register")!.body.password).length).toBe(72);
  expect(backend.calls.find((call) => call.path === "login")!.body.password).toBe(`  ${password}  `);
});

test("credential failures are generic and do not create an authenticated UI", async ({ page }) => {
  const backend = await mockBackend(page);
  backend.failures.login = { status: 401, message: "Unknown account" };
  await page.goto("/login");
  await fillLogin(page);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("Invalid email or password");
  await expect(page).toHaveURL(/\/login$/);
  expect(backend.authenticated).toBe(false);
});

test("protected page distinguishes loading, network error, retry and signed-out redirect", async ({ page }) => {
  const backend = await mockBackend(page);
  backend.before = async (path) => { if (path === "me") await new Promise((resolve) => setTimeout(resolve, 250)); };
  backend.failures.me = { abort: true };
  await page.goto("/dashboard");
  await expect(page.getByRole("status")).toHaveText("Checking your session…");
  await expect(page.locator("main").getByRole("alert")).toHaveText("Unable to verify your session. Please try again.");
  await expect(page.getByRole("button", { name: "Sign out", exact: true })).toHaveCount(0);
  backend.failures.me = undefined;
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
});

test("expired session redirects on focus and explains sign-in is needed", async ({ page }) => {
  const backend = await mockBackend(page);
  await signIn(page);
  backend.authenticated = false;
  await page.evaluate(() => window.dispatchEvent(new Event("focus")));
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("status")).toHaveText("Your session has expired. Please sign in again.");
});

test("logout failure remains visible and retry actually signs out", async ({ page }) => {
  const backend = await mockBackend(page);
  await signIn(page);
  backend.failures.logout = { status: 500 };
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("Something went wrong. Please try again.");
  await expect(page).toHaveURL(/\/dashboard$/);
  backend.failures.logout = undefined;
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
});

test("a lost logout response is reconciled with the server instead of retaining stale identity", async ({ page }) => {
  const backend = await mockBackend(page);
  await signIn(page);
  backend.before = async (path) => { if (path === "logout") backend.authenticated = false; };
  backend.failures.logout = { abort: true };
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  expect(backend.calls.filter((call) => call.path === "logout")).toHaveLength(1);
  await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
});

test("an old session-check response cannot redirect during a newer logout", async ({ page }) => {
  const backend = await mockBackend(page);
  await signIn(page);
  await expect.poll(() => backend.calls.filter((call) => call.path === "me").length).toBeGreaterThan(1);
  await page.waitForTimeout(50);
  let releaseMe!: () => void;
  let releaseLogout!: () => void;
  const meGate = new Promise<void>((resolve) => { releaseMe = resolve; });
  const logoutGate = new Promise<void>((resolve) => { releaseLogout = resolve; });
  let checking = false;
  let signingOut = false;
  backend.failures.me = { status: 401 };
  backend.before = async (path) => {
    if (path === "me") { checking = true; await meGate; }
    if (path === "logout") { signingOut = true; await logoutGate; }
  };
  try {
    await page.evaluate(() => window.dispatchEvent(new Event("focus")));
    await expect.poll(() => checking).toBe(true);
    await page.getByRole("button", { name: "Sign out", exact: true }).click();
    await expect(page.getByRole("button", { name: "Signing out…", exact: true })).toBeDisabled();
    releaseMe();
    await expect.poll(() => signingOut).toBe(true);
    await expect(page).toHaveURL(/\/dashboard$/);
    releaseLogout();
    await expect(page).toHaveURL(/\/login$/);
  } finally {
    releaseMe();
    releaseLogout();
  }
});

test("pending registration disables duplicate submissions and CSRF rejection retries once", async ({ page }) => {
  const backend = await mockBackend(page);
  backend.csrfRejects = 1;
  backend.before = async (path) => { if (path === "register") await new Promise((resolve) => setTimeout(resolve, 300)); };
  await page.goto("/register");
  await fillRegister(page);
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.getByRole("button", { name: "Creating account…", exact: true })).toBeDisabled();
  await expect(page.getByLabel("Password", { exact: true })).toBeDisabled();
  await page.getByRole("form", { name: "Create your account" }).evaluate((form: HTMLFormElement) => form.requestSubmit());
  await expect(page).toHaveURL(/\/login$/);
  expect(backend.calls.filter((call) => call.path === "register")).toHaveLength(2);
});

test("server and network registration failures never pretend success or retry", async ({ page }) => {
  const backend = await mockBackend(page);
  backend.failures.register = { status: 500, message: "Internal SQL details" };
  await page.goto("/register");
  await fillRegister(page);
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("Something went wrong. Please try again.");
  expect(backend.calls.filter((call) => call.path === "register")).toHaveLength(1);
  backend.failures.register = { abort: true };
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("Unable to connect. Please try again.");
  expect(backend.calls.filter((call) => call.path === "register")).toHaveLength(2);
  await expect(page).toHaveURL(/\/register$/);
});

test("navigation cancels pending login and does not produce a stale redirect", async ({ page }) => {
  const backend = await mockBackend(page);
  backend.before = async (path) => { if (path === "login") await new Promise((resolve) => setTimeout(resolve, 600)); };
  await page.goto("/login");
  await fillLogin(page);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await page.getByRole("link", { name: "Sign up", exact: true }).click();
  await expect(page).toHaveURL(/\/register$/);
  await page.waitForTimeout(800); // Let the deliberately delayed server result arrive.
  await expect(page).toHaveURL(/\/register$/);
  await expect(page.getByRole("heading", { name: "Create your account" })).toBeVisible();
});

test("forgot password and social providers are clearly unavailable", async ({ page }) => {
  const backend = await mockBackend(page);
  await page.goto("/login");
  await expect(page.getByRole("button", { name: "Google", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Apple", exact: true })).toBeDisabled();
  await page.getByRole("link", { name: "Forgot password?" }).click();
  await expect(page).toHaveURL(/\/forgot-password$/);
  await expect(page.getByRole("button", { name: "Send Reset Link", exact: true })).toBeDisabled();
  await expect(page.getByText(/Password reset is not available yet/)).toBeVisible();
  await page.getByRole("link", { name: "Back to sign in" }).click();
  await expect(page).toHaveURL(/\/login$/);
  expect(backend.calls.some((call) => call.path.includes("password"))).toBe(false);
});

test("approved auth image travels in both directions and browser history resolves state", async ({ page }) => {
  await mockBackend(page);
  await page.goto("/register");
  const shell = page.locator("main[data-screen]");
  const panel = shell.locator(':scope > div[aria-hidden="true"]');
  const isDesktop = (page.viewportSize()?.width ?? 0) >= 768;
  if (isDesktop) {
    await expect.poll(() => panel.evaluate((element) => getComputedStyle(element).transform)).not.toBe("matrix(1, 0, 0, 1, 0, 0)");
  }
  await page.getByRole("link", { name: "Sign in", exact: true }).click();
  await expect(shell).toHaveAttribute("data-image-screen", "login");
  if (isDesktop) {
    const initial = await panel.evaluate((element) => getComputedStyle(element).transform);
    await page.waitForTimeout(100);
    expect(await panel.evaluate((element) => getComputedStyle(element).transform)).not.toBe(initial);
  }
  await expect(page).toHaveURL(/\/login$/);
  await expect(shell).toHaveAttribute("data-screen", "login");
  if (isDesktop) await expect.poll(() => panel.evaluate((element) => getComputedStyle(element).transform)).toBe("matrix(1, 0, 0, 1, 0, 0)");
  await page.getByRole("link", { name: "Sign up", exact: true }).click();
  await expect(page).toHaveURL(/\/register$/);
  await expect(shell).toHaveAttribute("data-screen", "register");
  if (isDesktop) await expect.poll(() => panel.evaluate((element) => getComputedStyle(element).transform)).not.toBe("matrix(1, 0, 0, 1, 0, 0)");
  await page.goBack();
  await expect(shell).toHaveAttribute("data-screen", "login");
  await page.goForward();
  await expect(shell).toHaveAttribute("data-screen", "register");
});

test("landing and auth navigation retain reduced motion and responsive widths", async ({ page }) => {
  test.setTimeout(60_000);
  await mockBackend(page);
  await page.emulateMedia({ reducedMotion: "reduce" });
  for (const width of [360, 390, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of ["/", "/register", "/login", "/forgot-password"]) {
      await page.goto(path, { waitUntil: "domcontentloaded" });
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      await expect(page.locator("h1")).toBeVisible();
    }
  }
  await page.goto("/");
  await page.getByRole("link", { name: "Get Started" }).first().click();
  await expect(page).toHaveURL(/\/register$/);
  const panel = page.locator('main[data-screen] > div[aria-hidden="true"]');
  expect(await panel.evaluate((element) => getComputedStyle(element).transitionDuration)).toBe("0s");
  await page.getByRole("link", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
});
