import { type Page, type Route } from "@playwright/test";
import { INITIAL_CATALOG } from "../../src/lib/profile/catalog";
import { emptyProfile, type ProfileSnapshot, type ProfileWrite } from "../../src/lib/profile/types";

export async function mockProfileBackend(page: Page) {
  const state = {
    authenticated: true,
    account: { id: 7, fullName: "Example User", email: "user@example.com" },
    profile: emptyProfile(), writes: [] as ProfileWrite[], token: "",
    failWrite: null as null | { status?: number; fieldErrors?: Record<string, string>; loseResponse?: boolean; commit?: boolean },
    failRead: false,
    before: async (path: string) => { void path; },
  };
  const headers = {
    "Access-Control-Allow-Origin": `http://localhost:${process.env.PLAYWRIGHT_PORT ?? "3000"}`,
    "Access-Control-Allow-Credentials": "true", "Access-Control-Allow-Headers": "Content-Type, X-XSRF-TOKEN",
    "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS", "Cache-Control": "no-store",
  };
  await page.route("http://localhost:8080/api/**", async (route: Route) => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    if (request.method() === "OPTIONS") { await route.fulfill({ status: 204, headers }); return; }
    await state.before(path);
    const error = (status: number, message: string, fieldErrors = {}) => route.fulfill({ status, headers,
      json: { timestamp: "2026-10-17T00:00:00Z", status, error: "Request failed", message, path, fieldErrors } });
    if (path === "/api/auth/csrf") {
      state.token = "synthetic-profile-csrf";
      await route.fulfill({ status: 200, headers: { ...headers, "Set-Cookie": `XSRF-TOKEN=${state.token}; Path=/; SameSite=Lax` },
        json: { headerName: "X-XSRF-TOKEN", token: state.token } }); return;
    }
    if (!state.authenticated) { await error(401, "Authentication is required"); return; }
    if (path === "/api/auth/me") { await route.fulfill({ status: 200, headers, json: state.account }); return; }
    if (path === "/api/profile/options") { await route.fulfill({ status: 200, headers, json: INITIAL_CATALOG }); return; }
    if (path === "/api/profile/me" && request.method() === "GET") {
      if (state.failRead) await error(503, "private database details");
      else await route.fulfill({ status: 200, headers, json: state.profile });
      return;
    }
    if (request.method() === "PUT" || request.method() === "POST") {
      const requestHeaders = await request.allHeaders();
      if (!state.token || requestHeaders["x-xsrf-token"] !== state.token || !requestHeaders.cookie?.includes(`XSRF-TOKEN=${state.token}`)) {
        await error(403, "Invalid or missing CSRF token"); return;
      }
    }
    if (path === "/api/profile/me" && request.method() === "PUT") {
      const input = request.postDataJSON() as ProfileWrite; state.writes.push(input);
      const failure = state.failWrite; state.failWrite = null;
      if (failure?.status) { await error(failure.status, "private error details", failure.fieldErrors); return; }
      if (input.version !== state.profile.version) { await error(409, "Your preferences changed elsewhere. Reload before saving again"); return; }
      if (!failure?.loseResponse || failure.commit) {
        const now = "2026-10-17T00:00:00Z";
        const status = input.intent === "COMPLETE" ? "COMPLETED" : input.intent === "SKIP" ? "SKIPPED"
          : state.profile.onboardingStatus === "COMPLETED" ? "COMPLETED" : "IN_PROGRESS";
        state.profile = { version: (state.profile.version ?? -1) + 1, onboardingStatus: status, lastVisitedStep: input.lastVisitedStep,
          preferences: structuredClone(input.preferences), updatedAt: now,
          completedAt: status === "COMPLETED" ? state.profile.completedAt ?? now : null } satisfies ProfileSnapshot;
      }
      if (failure?.loseResponse) await route.abort("failed");
      else await route.fulfill({ status: 200, headers, json: state.profile });
      return;
    }
    if (path === "/api/auth/logout") { state.authenticated = false; await route.fulfill({ status: 204, headers }); return; }
    await error(404, "Unexpected mocked endpoint");
  });
  return state;
}
