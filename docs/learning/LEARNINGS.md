# Development Learning Journal

Personal Styling & Outfit Recommendation Application

## Introduction

This journal connects the project's implementation to the technical concepts behind it. It is intended for revision, portfolio discussion, and future maintenance: explain what changed, how it works, what the evidence supports, and what deserves further study.

The first entries are retrospective, prepared on 2026-10-16 against `main` at `1f8b491`, after Phase 0 and Phase 1. They are organized by development phase rather than presented as contemporaneous diary entries. The Phase 2 entry was appended on 2026-10-17 from actual implementation and verification on its feature branch. Current files may include later improvements; earlier entries retain their historical scope.

### Evidence and interpretation

- **Confirmed implementation** is supported by linked source, documentation, test definitions, or the identified Git commits.
- **Technical explanation** describes how the implementation works and why its structure is useful. Unless a rationale is explicitly documented, this is an explanation of the tradeoff, not a claim about an unrecorded personal decision.
- **Challenges** distinguish changes visible in history from risks addressed by code. No undocumented outage, debugging conversation, personal experience, or historical test result is reconstructed.
- **Verification** distinguishes coverage from execution. Phase 0/1 retrospective entries did not rerun suites. The Phase 2 entry records actual local runs; no live Neon result is claimed.
- **Further study** identifies work that has not been completed. It is not a delivery commitment.

The [root README](../../README.md) explains how to run the project. The [SRS](../requirements/SRS.md) defines intended behavior, priorities, and unresolved decisions. The [traceability matrix](../requirements/requirements-traceability.md) connects requirements to code/tests. This journal explains the engineering lessons without replacing those documents.

### Contents

1. [Phase 0 — Project Setup and Architecture](#phase-0--project-setup-and-architecture)
2. [Phase 1A — Authentication UI](#phase-1a--authentication-ui)
3. [Phase 1B — Backend Authentication](#phase-1b--backend-authentication)
4. [Phase 1C — Frontend–Backend Integration](#phase-1c--frontendbackend-integration)
5. [Documentation practices after Phase 1](#documentation-practices-after-phase-1)
6. [Phase 2 — Onboarding and Styling Profile Management](#phase-2--onboarding-and-styling-profile-management)
7. [Phase 2E — Personal Appearance and Fit](#phase-2e--personal-appearance-and-fit)
8. [Maintaining entries for future phases](#maintaining-entries-for-future-phases)

## Phase 0 — Project Setup and Architecture

### 0.1 What was implemented

The repository established `apps/web` for Next.js and `apps/api` for Spring Boot, a pnpm workspace, Maven Wrapper scripts, environment-based database configuration, and Git ignore rules. Commit `8ff994b` introduced the application scaffolds; `b449aaf` migrated database setup to Neon PostgreSQL and removed the earlier Docker Compose configuration. Commit `370ab24` added the shadcn component/theme foundation used by the UI work.

The current baseline specifies Java 21, Spring Boot 4.1.1, Next.js 16.3.5, React 19.2.8, and pnpm 10.34.5. These are current repository versions, not a claim that every version was identical at initialization. The installed Next.js package requires Node.js 20.9.0 or newer; Maven Wrapper selects Maven 3.9.16.

Evidence: [workspace configuration][workspace], [frontend manifest][package], [backend POM][pom], [Maven Wrapper settings][wrapper], and [datasource configuration][properties].

### 0.2 Important technical concepts

A monorepo puts related applications under one version history without requiring one language or build system. pnpm manages the JavaScript workspace and lockfile; Maven resolves Java dependencies and builds the backend. The backend directory is not a pnpm package simply because it shares a repository with the frontend.

The Next.js scaffold provides an App Router entry point, layouts, TypeScript configuration, and frontend scripts. The Spring Boot scaffold provides the Java application entry point, starter dependencies, external configuration, and a context-startup test. These scaffolds establish conventions; they do not implement product features automatically.

Java and Node.js are separate development runtimes. A Java project configured for version 21 still needs an appropriate JDK available to the Maven process. A frontend lockfile does not install Node.js itself. Runtime/tool version checks and application dependency resolution solve different setup problems.

JPA/Hibernate translates entity operations into database work, while PostgreSQL stores the data. Neon is the configured PostgreSQL host. Moving the database off the developer machine changes connection and availability dependencies without changing the application's basic repository abstraction.

### 0.3 Architectural decisions and their tradeoffs

**Confirmed structure:** frontend and backend run as separate processes, with independently managed dependencies. Database credentials are server-side environment variables: `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The frontend uses the public `NEXT_PUBLIC_API_BASE_URL` setting.

**Technical explanation:** keeping HTTP, database access, and frontend rendering separate makes boundaries easier to inspect and test. A shared repository makes coordinated changes visible together, but does not eliminate API contract or deployment coordination. The tradeoff is managing two runtimes and, in this development setup, two browser-facing origins.

The wrapper and lockfile make tool/dependency selection more reproducible. They do not guarantee identical operating systems, configured environment variables, network availability, or external database contents.

### 0.4 Challenges evidenced or risks addressed

- **Confirmed migration:** `b449aaf` changed the database setup to Neon. The history demonstrates the migration; it does not establish an outage or why an earlier connection might have failed.
- **Configuration risk:** a missing environment variable or using frontend configuration for backend secrets can prevent startup or expose sensitive information. Environment placeholders, ignore rules, and setup documentation address those concerns.
- **Workflow boundary:** a GitHub origin is configured and local commits show incremental work. Local history does not prove that pull-request reviews, branch protection, or CI runs occurred; no repository-defined GitHub Actions workflow was found.

### 0.5 How those concerns were addressed

The [application properties][properties] reference credentials by environment-variable name rather than embedding values. The [root ignore rules][ignore] exclude environment files, build output, dependencies, logs, and local tool state. A root `.env.local` is not automatically loaded by the current Spring configuration: variables must reach the backend process environment. Next.js reads its frontend setting from the web application's environment configuration.

The current setup documentation supplies separate terminal instructions for the two applications and explains the exact local origins. Git history separates setup, UI, persistence, security, integration, testing, and documentation changes. This is evidence of meaningful commit boundaries, not a reason to invent a particular branching or review policy.

### 0.6 Testing and verification practices

The starter [application context test][context-test] checks application startup under its configured environment. It is not an isolated database-free test: the current context includes datasource/JPA configuration. Its presence cannot prove that a particular Neon connection was successful on a particular date.

Setup verification should distinguish available runtimes, dependency/build success, Spring startup, and actual database access. The [README setup instructions](../../README.md#local-development) document the current commands. No installer choices or historical console output are inferred from those instructions.

### 0.7 Practical lessons

- Repository organization and deployment architecture are related but different: a monorepo can contain independently running services.
- Use the package manager that belongs to each application and run commands from the documented directory.
- Ignoring a secret file prevents ordinary staging; it is not a substitute for reviewing staged changes or remedying a secret already committed.
- A database URL is server configuration. A `NEXT_PUBLIC_*` value is exposed to the browser build and must never contain database credentials.
- Local commits and GitHub hosting are observable here; automated CI and formal review workflows must be evidenced separately.

### 0.8 Topics for further study

Production schema migrations instead of relying on development `ddl-auto=update`; CI workflows; deployment configuration; secret rotation; and structured architecture decision records. No deployment, GitHub Actions pipeline, or production migration tool is claimed as implemented.

## Phase 1A — Authentication UI

### 1A.1 What was implemented

This phase introduced the registration screen (`63b1048`), animated Register/Login flow (`8041567`), Forgot Password UI (`ceaadd6`), landing images/sections (`18d446b`, `86ccea8`, `f94be03`), and responsive/UI polish (`9b1b74e`). The forms were initially UI work; real API submission arrived in Phase 1C.

The approved desktop Register screen places the form left and image right. Login reverses those sides. Forgot Password uses the Login-side arrangement. The standalone landing page has its own header, hero, and category imagery rather than inheriting the auth shell.

Evidence: [auth layout][auth-layout], [AuthShell][shell], [transition CSS][motion], [AuthLink][auth-link], [landing page][landing], and [shared Button][button].

### 1A.2 Important technical concepts

In the App Router, `page.tsx` defines a route's page and `layout.tsx` composes shared structure. Parenthesized groups such as `(auth)` and `(app)` organize layouts without appearing in URLs: the login page is `/login`, not `/(auth)/login`.

React components divide responsibility between the shell, route-specific form content, fields, buttons, and landing sections. TypeScript describes props and permitted navigation paths, helping catch incompatible component use before runtime. It does not validate arbitrary backend JSON; that becomes an integration concern in Phase 1C.

Tailwind supplies utility classes linked to the theme, while CSS modules handle the auth panel's coordinated states and media queries. The checked-in shadcn Button wraps a Base UI primitive and exposes shared variants. It is not evidence that every form control comes from shadcn: `AuthField` wraps a native input.

Static landing composition can remain a Server Component while interactive auth navigation uses client state and hooks. The shared layout lets the image panel persist while the route's form content changes.

### 1A.3 Architectural decisions and their tradeoffs

**Confirmed approach:** one `AuthShell` receives the register/login image paths and wraps the route content. `AuthLink` delegates same-tab auth navigation to it while retaining Next.js link behavior for other navigation cases. Motion uses CSS and React state without a large animation library.

**Technical explanation:** a persistent shell gives the traveling image a continuous element to animate across route changes. Duplicating the whole split screen inside both pages would make that continuity harder to maintain. Keeping form content separate allows the approved forms to evolve without rebuilding the panel architecture.

The mobile layout hides the image and prioritizes a readable form. Minimum heights and internal padding allow vertical scrolling for taller content rather than forcing the form into a fixed-height card.

### 1A.4 Challenges evidenced or risks addressed

- **Coordination risk addressed in source:** changing the URL immediately could remove the outgoing form before its transition, while changing it too late could make navigation feel disconnected from the panel.
- **Visual risk addressed in source:** image loading/crossfading can expose the background; two overlapping image layers retain an opaque base.
- **Confirmed polish:** `9b1b74e` refined auth/landing UI, added route metadata, and made the image container positioned. This is a recorded refinement, not proof of a particular unrecorded browser error.
- **Usability risk:** motion, keyboard focus, and short/small viewports require separate handling from the desktop composition.

### 1A.5 How those concerns were addressed

`AuthShell` begins the image move immediately and schedules route navigation after 200 ms, once the outgoing form has faded. CSS moves the desktop image using a 650 ms transform transition and crossfades the Login image. When the pathname changes, URL state wins, including browser back/forward navigation; transient destination state is cleared.

At widths below 768 px, the image is hidden and the form uses the available width with internal padding and a readable maximum. Motion is enabled only for users without a reduced-motion preference. Auth navigation also moves focus to the destination heading. The landing page supplies a skip link and semantic content landmarks.

The current [AuthField][field] labels its input and associates error text through `aria-describedby`; those API-error additions came later in `6bc5be1`. This journal distinguishes the original UI foundation from integration-era accessibility improvements. Forgot Password and [social controls][social] now explicitly disclose unavailable functionality rather than simulate authentication.

### 1A.6 Testing and verification practices

The later [browser regression tests][browser-tests], introduced during Phase 1C, now exercise both animation directions, route history, reduced motion, and responsive widths. They support the current UI baseline; they are not claimed to have existed when the first Phase 1A screens were committed.

Visual review still needs to consider image crops, keyboard order, focus visibility, long error text, and vertical scrolling. Automated width checks can detect overflow, but do not establish comprehensive screen-reader support or full accessibility conformance.

### 1A.7 Practical lessons

- Route organization does not have to dictate visible URLs or duplicate layouts.
- Animation state must follow real navigation state; a visually correct panel with the wrong URL is still incorrect behavior.
- Accessibility includes motion, focus, landmarks, labels, and error handling, not only color choices.
- Reusing a design system means sharing tokens and component behavior while choosing native semantics where appropriate.
- A polished unavailable screen must remain honest about missing backend behavior.

### 1A.8 Topics for further study

Systematic screen-reader testing, measured contrast/WCAG acceptance, broader browser coverage, mobile keyboard/zoom behavior, and long-content visual regression. The SRS accessibility and browser decisions remain open; no full conformance certification is implied.

## Phase 1B — Backend Authentication

### 1B.1 What was implemented

The backend grew incrementally: persistence (`507a615`), registration validation (`a9f2a1b`), service logic (`a7ace4f`), registration HTTP API (`b0974e1`), session security (`7c7e995`), security tests/documentation (`99b2be2`), and the BCrypt byte-boundary fix (`43be241`).

The current HTTP surface is `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, and `POST /api/auth/logout`. Registration creates an account without automatically signing in. Responses expose account ID, full name, and email, not persistence entities or credentials.

Evidence: [AuthController][controller], [AuthService][service], [UserRepository][repository], [User entity][entity], [security configuration][security], and the [session API guide][api-guide].

### 1B.2 Important technical concepts

**HTTP and application layers.** A `@RestController` maps JSON requests/responses and HTTP status codes: account creation uses 201, login/current-account success uses 200, and logout uses 204 without a JSON body. Registration delegates business logic to `AuthService`; `UserRepository` handles persistence. Constructor injection supplies the repository and `PasswordEncoder` rather than having the service construct them. The service's transaction groups its registration operation; it does not remove the need for database constraints.

**DTOs and entities.** Request records represent incoming fields; `User` represents the database model. `RegisterRequest.password` is temporarily raw input at the request boundary. `User.passwordHash` stores an encoded value. Safe response DTOs prevent an entity's password-hash getter from accidentally becoming part of the HTTP contract. A Java record's generated string representation can expose its components, so both credential request records override `toString()` with redacted output.

**JPA and Hibernate.** `@Entity` makes `User` persistent, `@Table` maps it to `app_users`, and `@Id` with identity generation delegates ID creation to the database. Required columns and unique email express database integrity. Hibernate creation/update timestamps populate `Instant` fields. `JpaRepository<User, Long>` supplies persistence operations; method names such as `findByEmail` derive queries. `Optional<User>` explicitly represents an absent account.

**Validation and errors.** Jakarta constraints describe nonblank fields, format, string length, and the custom UTF-8 boundary. `@Valid` triggers request validation at the controller boundary; creating a DTO elsewhere does not automatically invoke all constraints. MVC advice returns a consistent safe error structure, while the security entry point/access-denied handler covers filter-chain failures. Validation errors are 400, duplicate email is 409, invalid credentials or missing authentication is 401, and CSRF rejection is 403.

**Password hashing.** BCrypt stores a salted one-way verifier, not reversible encryption. Verification uses `PasswordEncoder.matches()` through Spring's authentication provider rather than encoding again and comparing strings. The `PasswordEncoder` interface keeps service orchestration testable and hashing configuration centralized.

### 1B.3 Architectural decisions and their tradeoffs

**Confirmed authentication mechanism:** `AuthenticationManager`/`DaoAuthenticationProvider` loads a repository-backed principal and uses the configured BCrypt encoder. This uses Spring's framework authentication mechanism, not a custom filter or token format. The principal erases its temporary hash before persistence and retains safe identity information.

**Session versus JWT:** the implemented browser authentication uses server-held sessions referenced by an opaque cookie. It does not return a JWT for the frontend to store. The practical fit is explicit login/logout with framework session invalidation. The tradeoff is server-held state: current sessions are instance-local, expire after 30 minutes of inactivity, and disappear on restart. That explains the design properties; it is not a claim that a formal comparison of all token architectures was recorded.

**Explicit persistence:** putting authentication into `SecurityContextHolder` sets the current request's context. The login controller also invokes the session-authentication strategy and calls `SecurityContextRepository.saveContext()` so later requests authenticate. Spring's session strategy rotates an existing session ID to protect against fixation. Logout delegates to framework handlers for context clearing, session invalidation, and cookie expiry.

**Cookie/CSRF/origin boundaries:** HttpOnly prevents JavaScript from reading `JSESSIONID`; it does not stop a malicious script from issuing requests. The separately readable CSRF cookie is not an authentication credential. CSRF stays enabled, with Spring Security 7's SPA handling and a raw-token bootstrap for the request header. CORS allows one exact configured frontend origin with credentials, not a wildcard.

Local HTTP uses non-Secure cookies. The production profile forces Secure cookies and requires an explicit origin. SameSite=Lax supports the current same-site assumption; changing CORS alone does not enable unrelated cross-site hosts.

### 1B.4 Challenges evidenced or risks addressed

| Topic | Evidence and scope of the challenge |
| --- | --- |
| BCrypt input boundary | Confirmed correction in `43be241`: string-length maxima did not enforce the 72-byte UTF-8 limit |
| Duplicate registration race | Code/tests address a second insert between the email pre-check and database write; no actual concurrent production incident is claimed |
| Session not surviving a request | Explicit context saving and regression tests address this framework integration risk; the journal does not infer a historical failed login |
| CSRF token representation/rotation | The bootstrap reads the raw deferred token and login/logout clear CSRF; matching the browser header/cookie is part of the implemented contract |
| Account enumeration | Wrong-password and unknown-email login failures share a generic message; registration's deliberate 409 still reveals duplicate registration |
| Database startup issues | Documentation establishes environment dependencies, not a specific failed connection, error log, or personally encountered root cause |

### 1B.5 How those concerns were addressed

**UTF-8 correction.** [RegisterRequest][register-dto] preserves its eight-unit string minimum and uses [Utf8ByteLengthValidator][byte-validator] for the maximum. [LoginRequest][login-dto] uses the same byte maximum without imposing registration's minimum again. The validator measures UTF-8 bytes and does not trim, normalize, or truncate the input.

For example, 69 ASCII characters plus U+1F600 have 71 UTF-16 code units but 73 UTF-8 bytes. A maximum Java string length of 72 would accept that value even though BCrypt cannot. Seventy-two ASCII bytes, or 68 ASCII characters plus that emoji, are exactly at the byte limit. These are synthetic boundary examples, not submitted credentials. The existing minimum counts UTF-16 units, not user-perceived characters; changing that policy would be a separate compatibility decision.

**Uniqueness race.** Registration checks normalized email first, then uses `saveAndFlush` so a database failure occurs inside the service boundary. It translates only the recognized PostgreSQL email unique-constraint violation into the application conflict. Other integrity failures are not mislabeled as duplicate email. The constraint-name logic depends on the current Hibernate naming strategy and must be revisited if that mapping changes.

**Database troubleshooting method.** The documented setup supports checking that the required variables reach the backend process, the URL is a PostgreSQL JDBC URL with the required TLS settings, and the database host is reachable. Inspect configuration names without printing their secret values. Separate a datasource-dependent context failure from an isolated auth-test failure. This is a reusable diagnostic method, not a record that each failure occurred.

**Safe diagnostics.** Credential DTO strings are redacted; sensitive JDBC bind/error diagnostics are disabled and security logging remains at INFO. Generic API errors do not expose SQL details or stack traces. These safeguards do not justify enabling verbose logging with real credentials.

### 1B.6 Testing and verification practices

| Test layer | Existing examples | What the layer verifies |
| --- | --- | --- |
| DTO validation | [RegisterRequestTests][register-tests], [LoginRequestTests][login-tests] | Field constraints, ASCII/Unicode byte boundaries, redacted strings |
| Registration unit tests | [AuthServiceTests][service-tests] | Normalization, hash orchestration, duplicate pre-check/race classification, no save for rejected registration |
| Authentication components | [PasswordAuthenticationTests][password-tests], [RepositoryUserDetailsServiceTests][details-tests] | Real BCrypt compatibility, generic failure, normalized lookup, credential erasure |
| MVC/security HTTP tests | [AuthControllerTests][controller-tests], [SessionAuthenticationTests][session-tests] | Safe JSON/status contracts, validation, session persistence/fixation, CSRF and authorization |
| Embedded servlet tests | [SessionCookieTests][cookie-tests] | Real servlet session/cookie behavior in an isolated test application with mocked persistence |
| Full application context | [StylingApiApplicationTests][context-test] | Startup with actual datasource configuration; environment-dependent, not a Neon-free unit test |

Mocking the repository and encoder keeps service tests focused on orchestration; separate password tests use the real encoder to verify compatibility. An embedded HTTP test integrates servlet/security behavior but does not thereby test Neon persistence. The API guide includes the database-free auth selection and a separate safe PowerShell/live-database walkthrough. No application tests were executed for this journal.

### 1B.7 Practical lessons

- Validation has to match the downstream operation's units: bytes and string length are not interchangeable.
- Hashing belongs in the account-creation flow, not an entity setter or an HTTP response mapper.
- A pre-check improves error handling; the database constraint is still the final defense against concurrent duplicates.
- Authentication, security-context persistence, session fixation protection, and logout are separate lifecycle responsibilities.
- CORS, CSRF, SameSite, HttpOnly, and Secure address different concerns and must work together.
- Mocked tests and real-encoder/servlet tests answer different questions; neither should be described as live Neon verification.

### 1B.8 Topics for further study

Production migrations, authentication abuse/rate limits, shared-session storage and multi-instance behavior, account-wide revocation, password changes/recovery, and session identity refresh after account edits. The current `/me` returns the login-time principal snapshot. JWT/OAuth comparison can be studied without introducing a second authentication system into this project.

## Phase 1C — Frontend–Backend Integration

### 1C.1 What was implemented

Commits `79da5af`, `2b44c1c`, and `6bc5be1` added the typed client/validation, session provider/private navigation, and real form integration. Commit `cc2c00f` added frontend auth coverage; `fc2931b` documented integration and manual verification.

Registration now calls the real API and transitions to Login with an account-created notice. Login establishes the backend session and opens the minimal dashboard. Logout calls the protected backend operation. Forgot Password and social authentication remain unavailable; no fake local authentication was introduced.

Evidence: [Fetch client][client], [SessionProvider][provider], [RequireAuth][guard], [registration page][register-page], [login page][login-page], and [frontend integration guide][web-guide].

### 1C.2 Important technical concepts

**Typed API boundaries.** TypeScript request/account types make frontend usage explicit, while runtime parsers validate unknown JSON and copy only safe account fields. A successful HTTP response is not automatically trustworthy just because the caller used a TypeScript type. The client also handles logout's empty 204 response without trying to parse JSON.

**Credentialed Fetch.** `credentials: 'include'` lets the browser include the API cookies on cross-origin requests. The backend still controls CORS, authentication, CSRF, and cookie policy. The frontend does not read the HttpOnly session cookie or set a custom session identifier. Requests use `cache: 'no-store'`.

**Session state.** The React provider distinguishes loading, authenticated, unauthenticated, and error states, with safe account data held in memory. `/me` is the restoration source after a browser refresh, on focus/visibility restoration, and at private-route entry. Cookie presence alone is not proof of an authenticated account.

**Two validation boundaries.** Frontend validation provides immediate feedback; backend validation remains authoritative. `TextEncoder` enforces the matching UTF-8 maximum. Password whitespace may be examined to detect a blank value, but the submitted password remains unchanged. Email trimming does not justify trimming passwords.

### 1C.3 Architectural decisions and their tradeoffs

**Confirmed approach:** a native Fetch client owns request contracts and CSRF handling; `SessionProvider` owns application session state; forms own field/pending feedback; `RequireAuth` owns navigation UX. No NextAuth/Auth.js, JWT, localStorage credentials, or additional auth service was introduced.

**Technical explanation:** separating these responsibilities gives forms one consistent API/error model while preserving approved UI and animation behavior. A single shared session source avoids every component inventing its own authentication truth.

The guard is a client-side convenience, not server-side authorization. Sensitive Server Components or Server Actions would need explicit session verification on the server; the current guard does not provide that. Spring Security continues to authorize API requests.

The client serializes requests to prevent CSRF-cookie/bootstrap races. The tradeoff is that an operation can wait for earlier work. Each fetch has a 15-second timeout; that is not a total deadline for a queued, multi-request login flow.

### 1C.4 Challenges evidenced or risks addressed

- **Rotating token state:** login/logout can invalidate the frontend's previously cached CSRF value.
- **Stale asynchronous results:** an older `/me` response can arrive after a newer login/logout intent and incorrectly replace current state if not guarded.
- **Uncertain writes:** a canceled request or lost response does not prove that the backend rolled back registration, login, or logout.
- **Error ambiguity:** an unavailable backend must not be confused with a confirmed expired session or incorrect password.
- **UI regression risk:** connecting API submission must not break the persistent panel transition, mobile forms, focus handling, or browser history.

These concerns are explicitly represented by source comments and tests. They are recorded as handled engineering risks, not invented historical incidents.

### 1C.5 How those concerns were addressed

The client bootstraps CSRF lazily before a POST and sends the returned raw token in `X-XSRF-TOKEN`, while the browser supplies the matching cookie. Successful login/logout clears the cached token and attempts a fresh bootstrap. If refresh fails after confirmed success, that does not turn the successful operation into a false credential failure; a later POST bootstraps again.

Only a recognized CSRF 403 triggers one refresh-and-retry. A general 403, network failure, or server error is not blindly replayed. Cookie-based double-submit validation compares the pair; rotation is not a server-side revocation list for an old matching pair deliberately restored by a client.

The provider uses a generation counter to ignore outdated session checks and blocks concurrent auth mutations. Forms prevent duplicate submission, cancel pending work on navigation/unmount, and suppress stale redirects. Uncertain login/logout outcomes trigger a subsequent `/me` reconciliation. A client abort cannot undo a server write that has already completed.

A `/me` 401 clears identity; network/server failures produce a retryable error state. Logout treats an already-expired session as signed out, but a network failure does not falsely confirm sign-out. API errors map to field or safe form feedback; the first invalid field receives focus. The current [AuthField][field] associates error messages with inputs.

### 1C.6 Testing and verification practices

The [validation helper tests][validation-tests] exercise required fields, the registration-only minimum, exact/over-limit ASCII and multibyte cases, and unchanged password contents. [Client tests][client-tests] cover credentialed requests, safe response parsing, CSRF refresh/serialization, and bounded retries.

[Playwright browser tests][browser-tests] mock the API and exercise registration, generic login failure, restoration after refresh, logout, expiration, network retry, canceled navigation, delayed session responses, disabled unsupported actions, both panel directions, history, and responsive/reduced-motion behavior. [Configuration][playwright] defines helper, desktop Chromium, and mobile Chromium projects. The browser server uses a production build on port 3000; helpers can run without it. Traces, screenshots, and videos are disabled in the current configuration.

The frontend scripts serve different purposes: `pnpm lint` checks code rules; `pnpm typecheck` checks TypeScript; `pnpm build` checks production compilation; `pnpm test` executes Playwright tests. None alone proves the real cross-origin session flow. The [live browser walkthrough][web-guide] separately covers Spring Boot and Neon, cookies, CSRF, expiry, and safe database verification. Test definitions and commands were inspected, not rerun for this journal.

### 1C.7 Practical lessons

- Frontend authentication state is a view of the backend session, not an independent source of authorization.
- A refresh is an opportunity to revalidate `/me`, not a reason to persist credentials in browser storage.
- Model “unknown because of network failure” separately from “confirmed unauthenticated.”
- A POST retry needs a narrow reason; lost responses can hide a successful server operation.
- Cancellation protects UI behavior, while server reconciliation resolves uncertain authentication state.
- Browser mocks make failure cases reproducible but cannot prove live CORS/cookie behavior or database persistence.
- Passing lint/type checks/build and passing behavioral tests provide complementary evidence.

### 1C.8 Topics for further study

Live HTTPS cookie verification, broader browser/assistive-technology coverage, end-to-end tests against an isolated real database, cross-tab session synchronization, and server-rendered authorization if sensitive server-side data is added. Current cross-tab changes are detected on focus rather than an immediate broadcast.

## Documentation practices after Phase 1

Commit `35a2ff0` established the whole-product SRS and traceability matrix; `1f8b491` added the root README. These artifacts distinguish implemented authentication from the planned styling product. Stable requirement IDs and explicit open decisions make future work reviewable without implying that a proposed provider, ranking weight, or privacy policy is already approved.

The backend session guide's final historical scope note predates frontend integration. Current code and the frontend guide establish the later behavior. A practical documentation lesson is to identify the baseline and reconcile sources rather than copy an outdated statement into a new artifact. This journal does not alter the existing guides.

Future learning about wardrobe, recommendations, weather, AI explanations, and feedback belongs in new entries only after evidence exists. Development images generated with AI are not evidence of a runtime recommendation model or item-recognition integration.

## Phase 2 — Onboarding and Styling Profile Management

Recorded 2026-10-17 on `feature/phase-2-user-onboarding`, through verification commit `21d48f5`. Requirements and evidence are in the [updated SRS](../requirements/SRS.md), [profile contract](../api/styling-profile.md), and [verification guide](../testing/phase-2-verification.md).

### 2.1 What was implemented

Four optional onboarding steps collect style/colors, comfort/coverage, and lifestyle choices, followed by review. Next saves the full draft; Back keeps local changes; Save and exit, Skip, and explicit Finish have separate meanings. Dashboard remains available regardless of onboarding status. A profile editor reuses the controls, shows identity read-only, and preserves edits after errors/conflicts.

The backend adds a feature-based profile package, stable option catalog, full-snapshot DTOs, transactional service, relational persistence, and three authenticated endpoints. Frontend integration shares the established session/CSRF transport rather than adding another authentication system. No recommendation engine or wardrobe feature was added.

Commits were made incrementally after verification: `f828d80` onboarding UI, `f38e69b` editor, `a65b3f7` persistence, `4f8cba2` REST API, `5e9c309` shared transport, `27c1281` real integration, and `21d48f5` regression/live verification. UI-only milestones explicitly described local previews instead of pretending to save.

### 2.2 Important technical concepts

**Meaningful absence:** null means unanswered; [] means explicit No preference. An answered-category collection records empty-but-answered groups separately from selected choices. Removing the last checkbox returns to unanswered; choosing No preference is explicit. This distinction is part of both JSON and persistence, not only presentation.

**Ownership and optimistic concurrency:** a shared profile/user primary key gives zero-or-one profile per account. Controllers obtain the owner from the session principal. JPA @Version and expected-version comparison reject stale snapshots; database uniqueness handles concurrent first creation. A pre-check does not eliminate a race, and a generic integrity exception must not be mislabeled as a profile conflict.

**Atomic replacement:** the service validates codes, strengths, duplicates, colors, and state before changing managed collections, then flushes inside a transaction. Required JSON properties distinguish a missing group from an explicit null. Rejecting unknown write properties prevents silent acceptance of unsupported categories or owner fields without changing global JSON rules.

**Asynchronous identity:** an account-bound client captures a session generation. Login/logout invalidate it before joining the shared queue. Checks around awaits and immediately before writes prevent queued old work from using a new managed session. A separate `/me` preflight checks the current owner; it does not make cross-tab cookie changes atomic.

### 2.3 Architectural decisions and tradeoffs

The approved model uses three simple relational tables and element collections, with no new dependency, speculative audit framework, or change to User. Hibernate update remains the existing development strategy; production migrations still need a reviewed plan. A GET returns a virtual NOT_STARTED object without inserting a profile. Only an explicit write creates it.

Preference controls and review markup are reused between wizard/editor. Account-scoped workspaces remount on account/session change or explicit reload, not every saved version, so a successful Next is not interrupted. The authoritative catalog is loaded from the server; a network failure does not silently substitute an empty profile or local catalog.

Only explicit coverage choices may be Required. Other choices stay preferences, and no transport, gender, or cultural label creates a restriction. Storage of this distinction is complete; actual filtering/ranking remains a future module.

### 2.4 Observed challenges

PostgreSQL integration exposed timestamp precision drift: the first response retained nanoseconds while the reloaded timestamp used microseconds. Review also identified the risk of generated creation time following an earlier update time. Both were corrected before the persistence commit.

Replacing a request can silently clear data if omitted JSON fields become null. Required properties plus unknown-field rejection were tested with both standalone Jackson and the actual Spring MVC mapper.

Independent auth/profile request queues would allow stale tokens and old-account writes. Another risk was remounting a form on every saved version, which could cancel its navigation continuation. The shared transport and explicit workspace lifetime address those concrete design hazards.

One browser test initially injected a server error before the preceding successful save had finished. Its failure snapshot exposed the synchronization mistake; the test now waits for the resulting step before injecting the next failure. This was a test timing issue, not evidence that the backend ignored validation.

### 2.5 How challenges were addressed

Profile save instants are truncated to microseconds, initialized together, and advanced monotonically so collection-only/unchanged writes still dirty the versioned owner. Real database tests assert reload equality and version advancement.

Request contracts require all nullable groups and the version field. Validation completes before mutation; the transaction owns collection replacement. Conflict mapping is narrow, and the same safe API error structure serves validation, session/CSRF, conflict, and unexpected failures.

Network failure after PUT does not prove rollback. The UI reads once and confirms only a newer matching snapshot, step, and intended status. Otherwise it retains edits and blocks further saving until explicit reload; even Cancel does not bypass that lock. No blind PUT retry or localStorage draft persistence was added.

### 2.6 Testing and verification

The full Maven run passed **176 tests**, with no failures/errors/skips, using a new isolated local PostgreSQL cluster. This includes 11 persistence tests, three real profile HTTP integration tests, and all 88 existing backend tests. Concurrent transaction tests cover first-create and version races; integration covers ownership, null/empty values, status transitions, rollback, CSRF, and PUT CORS.

Frontend lint, TypeScript, and production build passed. The normal Playwright suite passed **107 cases**: 41 helper/transport tests and 66 desktop/mobile browser cases. A separate opt-in live suite passed **one browser case** using real Spring/PostgreSQL with cookies/CORS/CSRF and actual registration/save/resume/finish/edit/refresh/logout. Mocked test fixtures intercept every API route and do not fall through to a developer server.

The installed Docker client had no running daemon, so native PostgreSQL binaries were used for a temporary loopback-only test cluster. No package was installed and no existing/Neon data was reset. The [verification guide](../testing/phase-2-verification.md) gives exact commands, opt-in guards, and limitations. Neon, production HTTPS, broader browsers, and assistive-technology conformance were not verified.

### 2.7 Practical lessons

- Model unanswered, intentionally empty, partial, skipped, and completed states explicitly before building persistence.
- Exercise the actual database and HTTP mapper: mocks cannot establish timestamp precision, transaction races, or JSON defaults.
- A session change is a boundary for pending work, not just a change to displayed account information.
- Retry policy depends on certainty. A recognized rejected CSRF request is different from an uncertain delivered write.
- Keep each milestone coherent and tested before committing; preserve the earlier preview history without claiming it was already integrated.
- Record implemented profile foundations separately from future recommendation acceptance to avoid inflating delivery claims.

### 2.8 Further study

Production schema migrations, preference retention/deletion, bounded future vocabularies, immediate cross-tab session coordination, HTTPS and broader browser/accessibility checks, and consumer-side rule precedence remain open. Profile data is now ready for later consumers; no recommendation weights, wardrobe taxonomy, provider, or engine behavior was selected implicitly.

## Phase 2E — Personal Appearance and Fit

Recorded 2026-10-17 on feature/phase-2-user-onboarding through verification commit `0d37a4c`. This extends the original Phase 2 entry above; its four-step and historical test counts remain accurate for that earlier milestone. Current contracts are in the [profile API](../api/styling-profile.md#appearance-profile), with [verification evidence](../testing/phase-2-verification.md#phase-2e-results).

### 2E.1 What was implemented

A separate optional appearance profile stores ten voluntary details: height, sizing note, self-described proportions, complexion/color choices, hair characteristics, and face shape. Owner-only GET/PUT/DELETE support empty reads, full nullable replacement, individual clearing, and independent deletion. /profile adds a separate editor. Onboarding adds Personal details and fit before Review, with skip, resume, review, and recovery across two resource saves.

Requirements were committed before implementation: `0797b5c`, followed by a traceability-table formatting correction `4fa064a`. Verified milestones followed: persistence `ee6f290`, REST `ae35803`, editor `c7c5bba`, onboarding `f5b4716`, and regression/live verification `0d37a4c`. Each milestone was completed and checked before moving on; history was not reconstructed after implementing everything.

### 2E.2 Important technical concepts

**Optional data still needs a precise contract.** Every nullable property must be present on replacement writes. Missing differs from null; palette false differs from unanswered. An explicit all-null PUT can create a configured empty record, while GET of an absent record inserts nothing. Native fields and server validation agree on units, supported codes, and the 60-character sizing-note boundary.

**Optimistic locking across deletion.** JPA @Version detects simultaneous writes, but a numeric version starting over after delete/recreate could match an old client snapshot. Appearance uses an internal lock version plus a fresh public UUID revision on each save. The UUID is a concurrency tag, not a custom session credential. Real PostgreSQL races confirm one first-creation winner and one safe conflict.

**Two resources do not imply one transaction.** Appearance saves before styling progress. If the second request fails, the UI reports the first resource saved and retains remaining edits. It does not claim atomic completion or resend the already-confirmed first write. Uncertain delivery is reconciled by GET, with explicit reload when the result cannot be confirmed.

### 2E.3 Architectural decisions and tradeoffs

The appearance table references app_users directly, rather than styling_profiles. This supports independent deletion and accounts with no styling row without adding fields to User or changing account registration. Existing fit preferences remain in the styling profile. No extra measurements were added without a current garment use case; no age, weight, birth date, photos, ratings, or inference was implemented.

Catalog codes and labels are server-owned and validated without deriving traits. The frontend reuses accessible controls for onboarding/editor and the established credentialed session/CSRF queue, immutable account scopes, and stale-request checks. Appearance stays out of authentication responses and browser storage; no external provider receives it.

### 2E.4 Observed challenges

Inspection of the real PostgreSQL schema showed its existing step CHECK allowed only STYLE, COMFORT, LIFESTYLE, REVIEW. Expanding the Java enum alone would not safely support old databases. The implementation instead adds a default-false appearance_step_pending column and keeps the stored REVIEW code; true means the new personal screen, false means original Review.

Jackson 3 rejected an absent new primitive constructor boolean under its existing null-for-primitives rules. A nullable Boolean record component normalizes missing/null to false locally, preserving old JSON requests without weakening the global mapper. JSON and persistence tests reproduce the old payload/row compatibility.

A conflict-reload UI failure initially kept the form locked but did not display the reload network error. Regression tests prompted an explicit retry error while preserving the form and lock. During test development, a broad age selector matched coverage, and an expiration assertion assumed a query string on /login; both assertions were corrected to reflect the actual UI/route. A Mockito throwing stub also needed doThrow when replacing an existing throwing stub, rather than re-invoking it during setup.

### 2E.5 How challenges were addressed

The additive resume flag avoids dropping the original step constraint. Old REVIEW drafts remain Review, completed users remain completed, and new drafts restore the personal step after Save and exit. Skipping does not mutate saved appearance. Separate resources retain independent versions, transactions, and delete actions.

GET/PUT/DELETE use the authenticated principal only; expected revisions guard mutations. Known primary-key uniqueness and optimistic races become safe 409 responses, without mislabeling all database errors. Unknown fields are rejected; redacted string representations and existing logging restrictions keep payloads out of diagnostics. Save/cancel/delete controls, error focus, clear options, and confirmation reuse the approved visual system.

### 2E.6 Testing and verification

The complete Maven suite passed **243 tests**, with zero failures/errors/skips, against the isolated local PostgreSQL test database. It includes 19 persistence and four real HTTP integration tests. Appearance rules/JSON/MVC tests cover null/false, clearing, bounds/codes, unknown sensitive fields, ownership, CSRF, safe errors, and unchanged auth responses. PostgreSQL tests cover concurrency, delete/recreate, rollback, old onboarding states, and unchanged styling data.

Frontend lint, typecheck, production builds, and **144 deterministic cases** passed (50 helpers, 94 desktop/mobile browser cases). **Two live browser cases** passed against real local Spring Boot/PostgreSQL, covering the old skip flow and new personal-step persistence, refresh, clear/delete, and independent accounts. A test assertion failed during development and was fixed/re-run; no failing test was excluded. Temporary task-owned services were stopped and the normal API-origin build restored. No Neon/production HTTPS or screen-reader conformance result is claimed; see the verification guide for exact commands.

### 2E.7 Practical lessons

- Minimize data before choosing field types: future usefulness alone does not justify collecting more.
- Model absence, explicit empty saves, null, and false consistently across UI, JSON, and persistence.
- Review existing database constraints before extending workflow enums; backward-compatible metadata can avoid destructive changes.
- Check the actual JSON mapper's missing-property behavior rather than assuming a primitive default.
- Separate-resource saves need honest partial-success recovery and protection from duplicate writes.
- A delete action needs revision protection, independent ownership, and uncertainty handling as much as a save does.
- Mocked cases establish deterministic failures; real database and browser runs establish different parts of the contract.

### 2E.8 Further study

Reviewed production migrations, backup/retention deletion policy, immediate cross-tab coordination, HTTPS/Secure-cookie deployment, broader browser/assistive-technology checks, and evidence-based future garment/recommendation use remain open. Palette interest is stored intent only. No color exclusion, fit prediction, external AI transfer, or appearance-based ranking is implemented.

## Maintaining entries for future phases

Append a new entry after Phase 3 or subsequent completed milestones. For a long phase, use dated sub-milestone entries when useful, then summarize at completion. Do not add speculative “lessons learned” for work that has not started.

Use the same eight-part structure:

1. **What was implemented:** scope, status, baseline date/commit, and links to relevant requirements/source.
2. **Important technical concepts:** explain the mechanism using the actual implementation.
3. **Architectural decisions:** record confirmed rationale and tradeoffs; label technical interpretation separately.
4. **Challenges:** distinguish observed failures or confirmed corrections from anticipated risks; include sanitized evidence where available.
5. **Resolution:** connect a fix/design choice to the challenge and explain its limits.
6. **Testing and verification:** record exact commands, execution date/environment, results, and limitations when tests are actually run. Keep test presence separate from test results and mocks separate from live integrations.
7. **Practical lessons:** explain what can be reused or avoided in later work.
8. **Further study:** identify remaining questions without claiming those features exist.

Retain the historical meaning of earlier entries. Add a dated correction or later-milestone note when behavior changes, rather than silently rewriting the past. Prefer commit IDs and repository-relative source/test links; do not use machine-specific paths. Link SRS decisions when a lesson leads to a new requirement, but keep learning notes separate from approved product requirements.

Before committing an update, check links, terminology, code/history consistency, Markdown, and `git diff --check`. Never include credentials, raw password examples from real users, password hashes, session/CSRF values, private account data, or unredacted logs. Screenshots or test captures, if ever introduced, need the same review.

## Evidence references

Links point to current files; the phase commit IDs identify historical context. Use `git show <commit>` when comparing a historical implementation with the present one. Current files may change as later entries are added.

[workspace]: ../../pnpm-workspace.yaml
[package]: ../../apps/web/package.json
[pom]: ../../apps/api/pom.xml
[wrapper]: ../../apps/api/.mvn/wrapper/maven-wrapper.properties
[properties]: ../../apps/api/src/main/resources/application.properties
[ignore]: ../../.gitignore
[context-test]: ../../apps/api/src/test/java/com/chamodi/styling/StylingApiApplicationTests.java
[auth-layout]: <../../apps/web/src/app/(auth)/layout.tsx>
[shell]: ../../apps/web/src/components/auth/auth-shell.tsx
[motion]: ../../apps/web/src/components/auth/auth-shell.module.css
[auth-link]: ../../apps/web/src/components/auth/auth-link.tsx
[landing]: ../../apps/web/src/app/page.tsx
[button]: ../../apps/web/src/components/ui/button.tsx
[field]: ../../apps/web/src/components/auth/auth-field.tsx
[social]: ../../apps/web/src/components/auth/social-auth-buttons.tsx
[browser-tests]: ../../apps/web/tests/auth/auth.spec.ts
[controller]: ../../apps/api/src/main/java/com/chamodi/styling/auth/controller/AuthController.java
[service]: ../../apps/api/src/main/java/com/chamodi/styling/auth/service/AuthService.java
[repository]: ../../apps/api/src/main/java/com/chamodi/styling/auth/repository/UserRepository.java
[entity]: ../../apps/api/src/main/java/com/chamodi/styling/auth/entity/User.java
[security]: ../../apps/api/src/main/java/com/chamodi/styling/auth/config/SecurityConfig.java
[api-guide]: ../../apps/api/SESSION_AUTH.md
[register-dto]: ../../apps/api/src/main/java/com/chamodi/styling/auth/dto/RegisterRequest.java
[login-dto]: ../../apps/api/src/main/java/com/chamodi/styling/auth/dto/LoginRequest.java
[byte-validator]: ../../apps/api/src/main/java/com/chamodi/styling/auth/validation/Utf8ByteLengthValidator.java
[register-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/dto/RegisterRequestTests.java
[login-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/dto/LoginRequestTests.java
[service-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/service/AuthServiceTests.java
[password-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/security/PasswordAuthenticationTests.java
[details-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/security/RepositoryUserDetailsServiceTests.java
[controller-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/controller/AuthControllerTests.java
[session-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/controller/SessionAuthenticationTests.java
[cookie-tests]: ../../apps/api/src/test/java/com/chamodi/styling/auth/controller/SessionCookieTests.java
[client]: ../../apps/web/src/lib/api/auth-client.ts
[provider]: ../../apps/web/src/components/auth/session-provider.tsx
[guard]: ../../apps/web/src/components/auth/require-auth.tsx
[register-page]: <../../apps/web/src/app/(auth)/register/page.tsx>
[login-page]: <../../apps/web/src/app/(auth)/login/page.tsx>
[web-guide]: ../../apps/web/README.md
[validation-tests]: ../../apps/web/tests/unit/validation.spec.ts
[client-tests]: ../../apps/web/tests/unit/auth-client.spec.ts
[playwright]: ../../apps/web/playwright.config.ts
