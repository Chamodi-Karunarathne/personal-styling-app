# Personal Styling frontend

Phase 1C connects the approved authentication screens to the existing Spring Boot
session API. The dashboard is intentionally a minimal account screen.

## Local setup

Use Node.js 20 or newer and pnpm 10.34.5. Run `pnpm install --frozen-lockfile` from
the repository root. Create `apps/web/.env.local` with this public setting:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Never put database credentials in public frontend variables. `.env.local` is ignored.
Next.js embeds this URL at build time; restart development or rebuild after changing it.
Use an HTTP(S) origin without a path, credentials, query, or fragment.

Start Spring Boot using your existing environment setup for `DB_URL`, `DB_USERNAME`,
and `DB_PASSWORD`; do not place their values in frontend files:

```powershell
Set-Location "D:\Styling app\apps\api"
.\mvnw.cmd spring-boot:run
```

Start Next.js in another terminal:

```powershell
Set-Location "D:\Styling app\apps\web"
pnpm dev
```

Open `http://localhost:3000`. Use `localhost` consistently: `127.0.0.1` and port 3001
do not match the backend's exact development CORS origin.

## Authentication behavior

- Every API request uses native Fetch with `credentials: 'include'`.
- `/api/auth/me` restores the session on load and rechecks it on window focus,
  visibility restoration, and entry into private routes.
- Registration returns 201 and transitions to Login with an account-created notice.
  It does not automatically sign in.
- Login establishes the Spring session, refreshes CSRF, and opens `/dashboard`.
- Logout clears the server session. Its 204 response has no JSON body. An expired
  session is already signed out; a network failure does not prove logout succeeded.
- Passwords remain unchanged. Registration keeps the eight-character minimum;
  both forms enforce 72 UTF-8 bytes with `TextEncoder`.
- Password reset and Google/Apple authentication are clearly unavailable.

CSRF bootstrap GET `/api/auth/csrf` returns a raw token retained only in memory and
submitted as `X-XSRF-TOKEN`. The browser supplies the matching cookie. Login/logout
discard the token and refresh it. Requests are serialized to prevent bootstrap/cookie
races. A recognized CSRF 403 permits one refresh-and-retry; other failures never
automatically retry a POST. If refresh fails after successful login/logout, the
operation remains successful and the next POST bootstraps again.

Canceled form requests cannot redirect after navigation. A generation check prevents
old `/me` responses from overriding authentication changes. Uncertain authentication
outcomes are rechecked through `/me`. Requests have a 15-second timeout. Neither
passwords, session IDs, nor CSRF tokens are written to browser storage, URLs, or logs.

The client guard handles navigation only. Spring Security authorizes API requests.
Sensitive server-rendered data and Server Actions require their own server-side
Spring-session verification; this client guard is insufficient for them.

## Automated checks

Run from `apps/web`:

```powershell
pnpm lint
pnpm typecheck
pnpm build
pnpm exec playwright install chromium
pnpm test
# Helpers only, without a browser/server:
pnpm test:unit
```

Normal browser tests start the production build at port 3000; use
`$env:PLAYWRIGHT_PORT='3100'` for another free port. Normal API responses and
accounts/cookies/tokens are mocked and synthetic; these tests do not connect to Spring Boot or Neon. Chromium desktop/mobile
projects cover form flows, errors, CSRF, loading/retry, refresh, expiration, cancellation,
logout, animation, browser history, reduced motion, and responsive overflow checks.
Helper tests cover UTF-8 boundaries, serialization, and bounded retries.

Traces, screenshots, and videos are disabled; generated test results are ignored.
Mock tests cannot prove the actual cookie/CORS policies of a live deployment.

## Manual verification with Spring Boot and Neon

This walkthrough intentionally creates one account. Choose a development test email
you control and a development-only password; never reuse real credentials.

1. Start both applications and visit `/dashboard` while signed out. Verify the
   session-check state followed by a Login redirect.
2. Follow Sign up and register. Confirm POST 201 in Network tools, the animated
   Login transition, and "Account created. Please sign in." Registration alone
   must not establish a session.
3. Repeat registration with the same email: expect 409. Try invalid fields and a
   password exceeding 72 UTF-8 bytes; failures must never display success.
4. Try an incorrect login password: expect generic 401. Sign in correctly: expect
   200, a subsequent CSRF bootstrap, and safe account details/setup entry on `/dashboard`.
5. Refresh `/dashboard` and open another tab. `/me` should return 200 and restore
   account information after verification.
6. Confirm POSTs have `X-XSRF-TOKEN` and requests include cookies. `JSESSIONID`
   must be HttpOnly, host-scoped, Path=/, SameSite=Lax, Secure=false for local HTTP.
   Do not copy/export request bodies, headers, cookies, HARs, tokens, or session IDs.
7. Sign out: expect 204, CSRF refresh, and Login. Refresh/directly visit `/dashboard`:
   it must redirect. Sign in again to verify token refresh after logout.
8. Leave a tab idle for over 30 minutes or restart the backend, then refocus it.
   Expect Login and the expiration notice. API activity refreshes the idle timer.
9. Stop the backend and visit `/dashboard`: expect a connection error and Try again,
   not a false signed-out result. Restart it and retry. Stop it before Sign out too;
   a failed request must not claim successful logout.
10. Check landing links, both auth animation directions, back/forward, keyboard
    submission/error focus, reduced motion, and 360/390/768/1024/1440px layouts.
    Forgot Password and social controls must remain unavailable.

Safe Neon verification (do not select `password_hash`):

```sql
SELECT id, full_name, email, created_at, updated_at
FROM app_users
ORDER BY id DESC;
```

## Limits

Spring sessions are in-memory with a 30-minute idle timeout and expire on restart.
Cross-tab changes are detected on focus; there is no polling or immediate broadcast.
A canceled HTTP write may still complete on the server, so uncertain outcomes are
reconciled rather than treated as a rollback.

Production requires HTTPS, the backend `prod` profile, and an exact `FRONTEND_ORIGIN`.
Secure cookies are forced. SameSite=Lax supports same-site HTTPS frontend/API hosts;
unrelated sites require a separately considered cookie/proxy arrangement. See
`../api/SESSION_AUTH.md` for authoritative backend configuration.

## Phase 2 styling profiles

`/onboarding` and `/profile` reuse the protected layout and existing session provider.
Dashboard offers Start setup, Resume setup, or Edit preferences without blocking access.
Auth and profiles use one credentialed transport/CSRF cache. The workspace loads the
server catalog and saved snapshot, preserves failed edits, rejects stale versions,
and reconciles uncertain writes with GET rather than automatic PUT replay.

See the [profile API](../../docs/api/styling-profile.md) and
[Phase 2 verification](../../docs/testing/phase-2-verification.md) for state semantics,
isolated PostgreSQL testing, separate opt-in live browser tests, actual results, and
the pending manual Neon walkthrough. The live config is separate from `pnpm test`.

No additional auth system, JWT, OAuth, password-reset backend, full dashboard,
wardrobe, planner, or recommendation engine is implemented.
