# Phase 2 verification

Executed on 2026-10-17 on `feature/phase-2-user-onboarding`, using Windows/PowerShell, Java 21, Node 24, installed pnpm, Chromium, and an isolated temporary PostgreSQL 18 cluster. No Neon credentials were loaded, no Neon data was changed, and no tests are presented as live Neon verification.

## Original Phase 2 results (historical baseline)

| Check | Command | Result |
| --- | --- | --- |
| Backend complete suite | `./mvnw.cmd '-Ddebug=false' test` | 176 passed; 0 failures/errors/skips with dedicated test DB settings below |
| Profile persistence | Included in complete suite | 11 real PostgreSQL tests |
| Profile HTTP integration | Included in complete suite | 3 real HTTP → Spring Security → service → PostgreSQL tests |
| Backend authentication regression | Included in complete suite | Existing 88 tests remain passing |
| Frontend lint | `pnpm lint` | Passed |
| Frontend types | `pnpm typecheck` | Passed |
| Frontend production build | `pnpm build` | Passed for both normal API and isolated live-test API; final build restored to localhost:8080 |
| Deterministic frontend suite | `$env:PLAYWRIGHT_PORT='3100'; pnpm test` | 107 passed: 41 helper/transport tests and 66 desktop/mobile browser cases |
| Live browser suite | `pnpm exec playwright test --config playwright.live.config.ts` | 1 passed against actual local Spring Boot and isolated PostgreSQL |

The 176 total includes DTO, catalog, service, MVC, security, cookie, application-context, and opt-in profile integration tests. Mocked unit/MVC/browser cases do not demonstrate real database or browser cookie enforcement. The separate live browser case does exercise real credentialed CORS/cookies, CSRF rotation, UI saves, reloads, account isolation, and sign-out on local HTTP. It is not a production/Neon/HTTPS certification.

## Reproduce backend checks

From `apps/api`, without a database, run the explicitly selected isolated tests:

```powershell
.\mvnw.cmd '-Dtest=RegisterRequestTests,AuthServiceTests,AuthControllerTests,LoginRequestTests,RepositoryUserDetailsServiceTests,PasswordAuthenticationTests,SessionAuthenticationTests,SessionCookieTests,ProfileRequestJsonTests,PreferenceCatalogTests,StylingProfileServiceTests,StylingProfileControllerTests,AppearanceRequestJsonTests,AppearanceProfileServiceTests,AppearanceControllerTests' test
```

For the full suite, provision a **new dedicated local** PostgreSQL database named `phase2_test` on a loopback port. Do not reuse production/Neon databases. These settings apply only to the current terminal; values below are placeholders:

```powershell
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:<dedicated-port>/phase2_test'
$env:DB_USERNAME = '<test-user>'
$env:DB_PASSWORD = '<test-password>'
$env:PROFILE_INTEGRATION_TESTS = 'true'
$env:PROFILE_TEST_DB_URL = $env:DB_URL
$env:PROFILE_TEST_DB_USERNAME = $env:DB_USERNAME
$env:PROFILE_TEST_DB_PASSWORD = $env:DB_PASSWORD
.\mvnw.cmd '-Ddebug=false' test
```

The three profile integration classes require explicit opt-in and reject URLs outside `jdbc:postgresql://127.0.0.1:<port>/phase2_test`. They create synthetic accounts/profile rows and concurrent transactions. Schema management is update, not drop/recreate. The existing application-context test uses `DB_*`; supply the same isolated database. Without opt-in, the current 23 profile integration tests (19 persistence and four HTTP) are skipped; report that honestly. Database startup/provisioning is intentionally outside the test code, with no new test-container dependency.

The completed run used a temporary native PostgreSQL cluster because the installed Docker client had no available daemon. The temporary API and cluster were stopped after verification; no existing services or Neon databases were reset.

## Reproduce deterministic frontend checks

From `apps/web`:

```powershell
pnpm lint
pnpm typecheck
$env:NEXT_PUBLIC_API_BASE_URL = 'http://localhost:8080'
pnpm build
$env:PLAYWRIGHT_PORT = '3100'
pnpm test
```

Choose a free port; default is 3000. Playwright refuses to reuse a running server. Normal browser fixtures intercept all `/api/**` calls and never fall through to a running developer backend. No Spring server/Neon is required. `pnpm test:unit` runs the current 50 helper tests without a browser server.

## Separate real browser check

Use only a disposable development test database. These tests create synthetic accounts, styling profiles, and appearance profiles; it is not a read-only smoke test.

1. In a backend terminal, set the isolated `DB_*` values above, set `SERVER_PORT=8081` and `FRONTEND_ORIGIN=http://localhost:3101`, then run `./mvnw.cmd spring-boot:run` from `apps/api`. Keep local cookies non-Secure for HTTP.
2. In `apps/web`, run:

```powershell
$env:NEXT_PUBLIC_API_BASE_URL = 'http://localhost:8081'
pnpm build
$env:LIVE_PROFILE_TESTS = 'true'
$env:PLAYWRIGHT_PORT = '3101'
pnpm exec playwright test --config playwright.live.config.ts
```

The separate config requires explicit opt-in and exactly the isolated API origin localhost:8081. It starts only Next.js; it does not provision/start PostgreSQL or Spring. There are no mocked API routes in the live tests. Trace, screenshots, video, credentials, cookies, and token artifacts are not retained.

3. Stop only the services you started. Restore the normal frontend build before mocked tests/local use:

```powershell
Remove-Item Env:LIVE_PROFILE_TESTS -ErrorAction SilentlyContinue
$env:NEXT_PUBLIC_API_BASE_URL = 'http://localhost:8080'
pnpm build
```

`NEXT_PUBLIC_API_BASE_URL` is embedded at build time; changing only the runtime variable does not alter an existing build.

## Manual browser and Neon walkthrough — pending

Use the established [local setup](../../README.md#local-development) and [session walkthrough](../../apps/web/README.md#manual-verification-with-spring-boot-and-neon). Use an intentionally authorized development account/database; do not export HARs, cookie headers, submitted preferences, credentials, or session/CSRF values.

1. Register, sign in, and confirm Dashboard remains available without a profile. Verify GET `/api/profile/me` returns NOT_STARTED and no profile row is inserted by GET alone.
2. Start setup. Set styles/colors, explicitly choose No preference for one group, leave another unanswered. Next must save before advancing. Refresh and confirm saved step/choices restore.
3. Back, change a choice locally, and move forward. Confirm no save happens on each checkbox change. Save and exit, then Resume setup.
4. Mark a coverage choice Required; verify it remains explicit. Skip and confirm choices survive; Dashboard offers Resume setup. Resume and finish, including an all-unanswered completion on a separate test account if desired.
5. Open Edit preferences, save, refresh, and check persisted values. Account name/email must be read-only; identity/password endpoints are unchanged.
6. Open the same profile in two tabs, save in one, then save the old version in the other. Expect 409, preserved edits, and explicit reload rather than silent overwrite. Cancel must not bypass the conflict reload lock.
7. Stop the API before saving: retain edits and show an honest error. If delivery is uncertain, no automatic duplicate PUT may occur. Reload/review before further saves unless the follow-up GET confirms the snapshot.
8. Expire/sign out the session. A later save must not succeed anonymously. Sign in as a second account; no prior account preferences should appear. Test another-tab sign-in and refocus, recognizing the documented cross-tab timing limitation.
9. Check keyboard controls, named color labels, coverage labels, error-summary focus, reduced motion, and widths 1440/1024/768/390/360. Authentication images/transitions and unavailable social/recovery controls must remain intact.
10. Confirm profile PUT has the CSRF header and uses credentialed requests; inspect statuses without copying token/cookie values. A missing token returns 403; allowed-origin PUT preflight succeeds; untrusted origins remain rejected.

Safe database metadata query (no passwords, hashes, or preference values):

```sql
SELECT user_id, version, onboarding_status, last_visited_step,
       created_at, updated_at, completed_at
FROM styling_profiles
ORDER BY updated_at DESC;
```

## Coverage and known limits

- [Persistence tests](../../apps/api/src/test/java/com/chamodi/styling/profile/repository/StylingProfilePersistenceTests.java): no write on missing GET; null/empty/strength round-trip; versions; state transitions; stale updates; rollback; ownership/FK; simultaneous creation/update.
- [HTTP integration](../../apps/api/src/test/java/com/chamodi/styling/profile/controller/StylingProfileHttpIntegrationTests.java): real independent sessions, safe conflicts/validation, persisted lifecycle, CSRF, exact PUT CORS.
- [MVC tests](../../apps/api/src/test/java/com/chamodi/styling/profile/controller/StylingProfileControllerTests.java): principal-derived owner; real CSRF cookie/header bootstrap; strict JSON/validation; safe 400/401/403/409/500; no request-body logging.
- [Transport tests](../../apps/web/tests/unit/profile-client.spec.ts): shared token/queue; immutable account scopes; queued/active stale operations; cross-tab ownership preflight; cancellation; endpoint-specific errors; no blind retry.
- [Browser integration scenarios](../../apps/web/tests/auth/profile-integration.spec.ts): load/retry, conflicts, server fields, uncertain outcomes, expiration, account changes, and viewport checks. [Wizard](../../apps/web/tests/auth/onboarding.spec.ts) and [editor](../../apps/web/tests/auth/profile-editor.spec.ts) tests cover persisted interaction semantics.
- [Live browser test](../../apps/web/tests/live/profile-live.spec.ts): real registration/login, save/resume/finish/edit/refresh, two-account isolation, and logout.

No real Neon test, production HTTPS deployment, Firefox/Safari matrix, assistive-technology conformance audit, performance/load target, immediate cross-tab broadcast, or production migration policy is claimed. No recommendation engine, wardrobe, or profile-driven ranking behavior exists yet.

## Phase 2E results

Executed 2026-10-17 after appearance implementation, through verification commit `0d37a4c`. Earlier results above remain the original Phase 2 historical snapshot.

| Check | Exact command / selection | Result |
| --- | --- | --- |
| Complete backend | `.\mvnw.cmd '-Ddebug=false' test` with isolated DB/opt-in settings above | **243 passed**, 0 failures/errors/skips; includes full application context |
| PostgreSQL persistence | Included above: StylingProfilePersistenceTests and AppearancePersistenceTests | **19 passed** (11 styling + 8 appearance) |
| Real HTTP integration | Included above: StylingProfileHttpIntegrationTests | **4 passed**, including real appearance CRUD, owner isolation, CSRF, and unchanged styling/auth contracts |
| Appearance rules/JSON/MVC | Included above: AppearanceProfileServiceTests, AppearanceRequestJsonTests, AppearanceControllerTests | **57 passed** (17 + 18 + 22) |
| Packaging | `.\mvnw.cmd '-DskipTests' package`, after complete test run | Passed; package command does not itself rerun tests |
| Frontend lint | `pnpm lint` | Passed |
| Frontend types | `pnpm typecheck` | Passed |
| Production builds | `pnpm build` with API origins 8080 and 8081, then restored 8080 | All passed; normal local build restored |
| Deterministic frontend | `$env:PLAYWRIGHT_PORT='3100'; pnpm test` | **144 passed**: 50 helper/transport + 94 desktop/mobile browser cases |
| Real browser | `pnpm exec playwright test --config playwright.live.config.ts`, with explicit live opt-in/origins above | **2 passed**, no mocked API calls |
| Patch hygiene | `git diff --check` and staged checks per milestone | Passed |

All original auth/profile tests remain included. Actual local Spring Boot at localhost:8081, Next.js at localhost:3101, and an isolated loopback PostgreSQL 18 database were used. Native PostgreSQL reused this task's dedicated synthetic test cluster; Docker daemon remained unavailable. No Neon credentials/data were used. The task-owned API/database were stopped afterward, without resetting another service or deleting the test data. No package dependency was added.

### Appearance coverage and privacy

- [Service tests](../../apps/api/src/test/java/com/chamodi/styling/profile/appearance/AppearanceProfileServiceTests.java): optional/null/false values, bounds, codes, complete validation, expected revisions, narrow race mapping, and redacted strings.
- [JSON tests](../../apps/api/src/test/java/com/chamodi/styling/profile/appearance/AppearanceRequestJsonTests.java): every nullable property required, missing version rejected, unsupported/sensitive fields rejected.
- [Persistence](../../apps/api/src/test/java/com/chamodi/styling/profile/appearance/AppearancePersistenceTests.java): no GET insertion, create/read/clear/delete, rollback, unchanged styling/account, ownership, timestamps, stale/delete-recreate revisions, concurrent first creation/update, old REVIEW/completed compatibility and additive resume flag.
- [MVC/security](../../apps/api/src/test/java/com/chamodi/styling/profile/appearance/AppearanceControllerTests.java): principal ownership, CSRF for PUT/DELETE, safe errors, DELETE exact-origin preflight, no body-value logs, unchanged safe authentication response fields.
- [Transport](../../apps/web/tests/unit/appearance-client.spec.ts): runtime contract checks, account/session scope, no replay, credentialed CSRF-protected mutations.
- [Editor browser](../../apps/web/tests/auth/appearance-editor.spec.ts): empty state, optional fields, persistence/clearing/deletion, keyboard controls, mobile overflow, associated local/server errors, unsaved changes, confirmation, conflicts, uncertain deletion/reload failure, session expiry, account switching, and no browser storage.
- [Wizard browser](../../apps/web/tests/auth/appearance-onboarding.spec.ts): fifth-step resume, original Review/completed compatibility, Skip preserves existing appearance, unavailable optional resource, Back/validation, and accurate recovery after appearance save succeeds but styling save fails.
- [Real browser](../../apps/web/tests/live/profile-live.spec.ts): new personal-step save/exit/resume/complete, refresh, clear/delete, retained Casual styling/completion, second-account empty appearance; original skip/registration/login/logout lifecycle also passed.

One new test initially assumed every expired-session redirect included a query string; the actual safe redirect is /login. The assertion now accepts the correct route with or without a query. Another test selector initially matched age/weight fragments in unrelated copy; word boundaries and region scoping fixed the test. The observed failed-reload UI error was corrected and verified. No failing tests were silently excluded.

### Additional manual appearance checks — Neon/HTTPS pending

Use only an authorized development account/database; these steps intentionally write/delete that account's optional appearance data.

1. On an existing completed account, confirm Dashboard remains completed; /profile has Personal Details & Fit and old styling choices. A missing appearance GET is empty and does not insert a row.
2. Save any voluntary details, refresh, clear one field, save again. Verify false palette interest stays distinct from Unanswered. No age/weight/DOB/photo controls should exist.
3. Start a fresh onboarding draft; verify five steps, Back, Save and exit/resume, and review. Skip Personal details, including when it has saved data: saved values must remain. Finish with all personal fields empty.
4. In two tabs, edit the same appearance revision. The second stale save/delete must show 409 and preserve edits; Reload requires confirmation. Stop the API before reload and confirm an honest connection error with edits retained.
5. On a disposable development account, confirm Delete personal details returns 204 and leaves styling choices, completed status, account, and session intact. Repeat DELETE of an absent record returns 204; existing records require the observed version.
6. Fail styling progress after a successful appearance save and confirm the partial-success message. Recover through reload/review without resending the confirmed appearance write. Do not present the two endpoint writes as one transaction.
7. Confirm credentialed PUT/DELETE and CSRF header, exact origin, mobile/keyboard behavior, and session expiration/account change. Do not export payloads/cookies/CSRF values/HARs or enable request/JDBC debug logs.

Safe schema/lifecycle metadata only (does not display appearance values):

```sql
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('appearance_profiles', 'styling_profiles')
ORDER BY table_name, ordinal_position;

SELECT user_id, lock_version, created_at, updated_at
FROM appearance_profiles
ORDER BY updated_at DESC;
```

Deletion removes the live appearance row; it is not a claim of erasing database backups or provider logs. Retention/backup policy needs a production decision. Neon, HTTPS/Secure-cookie deployment, Safari/Firefox, screen-reader conformance, and load testing remain unverified. There is no AI/external appearance transfer, inference, rating, outfit engine, or garment-fit calculation.
