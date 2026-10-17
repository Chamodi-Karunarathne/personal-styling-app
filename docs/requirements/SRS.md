# Software Requirements Specification

Personal Styling & Outfit Recommendation Application

| Document control | Value |
| --- | --- |
| Baseline | 1.3 — Phase 2E appearance and fit implementation verified locally |
| Date | 2026-10-17 |
| Evidence snapshot | feature/phase-2-user-onboarding through 0d37a4c; Phase 0, Phase 1, Phase 2 and Phase 2E delivered |
| Document state | Phase 2 and Phase 2E owner-approved; future product scope/decisions remain proposals |
| Maintainer / decision owner | Individual project developer / product owner |
| Related documents | [Documentation index](../README.md); [requirements traceability](requirements-traceability.md) |

## Contents

1. [Introduction](#1-introduction)
2. [Overall system description](#2-overall-system-description)
3. [System features and functional requirements](#3-system-features-and-functional-requirements)
4. [External interface requirements](#4-external-interface-requirements)
5. [Non-functional requirements](#5-non-functional-requirements)
6. [Data requirements](#6-data-requirements)
7. [Business rules and constraints](#7-business-rules-and-constraints)
8. [Use cases](#8-use-cases)
9. [Technology constraints and dependencies](#9-technology-constraints-and-dependencies)
10. [Verification and acceptance](#10-verification-and-acceptance)
11. [MVP scope and future enhancements](#11-mvp-scope-and-future-enhancements)
12. [Open decisions and assumptions](#12-open-decisions-and-assumptions)
13. [Evidence register](#13-evidence-register)

## 1. Introduction

### 1.1 Purpose

This specification defines the entire intended application, its existing authentication foundation, and the planned capabilities needed to organize a personal wardrobe and recommend practical outfits. It supports requirements review, architecture, implementation, and verification for an individual full-stack software engineering portfolio project.

The organization is inspired by ISO/IEC/IEEE 29148 requirements practices. It is adapted to this project's scale and is not a claim of formal standards certification or conformance. It is a requirements baseline, not a declaration that every described capability has been delivered.

### 1.2 Scope and product outcome

The intended product helps a person choose coordinated outfits from clothing they actually own. Recommendations consider occasion, location, date/time and weather, expected activities, relevant transportation context, wardrobe availability, and explicit style, comfort, and modesty choices.

For example, a user going out with friends in hot weather, traveling by public transport and expecting to walk, can ask for a comfortable, modest look. A future engine evaluates owned available items against the user's stated choices and documented practical rules. It explains its result or identifies insufficient matches. It must not infer personal restrictions merely from transport or an occasion label.

The initial recommendation baseline is transparent deterministic filtering and ranking. Optional AI can assist wording, item classification, or later personalization after their policies are approved. Training a model from scratch is not a requirement. The existing application does not yet implement a recommendation engine.

The landing page presents Outfits, Makeup, Hair, and Accessories. Those are currently visual categories, not functioning services. This SRS specifies wardrobe-based outfit recommendations, including accessories where appropriate. Standalone makeup/hair recommendation scope requires DEC-001; this document does not silently expand the MVP.

### 1.3 Audience

The primary audience is the project owner/developer. Reviewers, portfolio assessors, future maintainers, and testers can use the evidence and acceptance criteria to distinguish delivered engineering work from intended product behavior. No dedicated operations team, administrator role, or multi-tenant business organization is currently implemented.

### 1.4 Definitions

| Term | Meaning in this project |
| --- | --- |
| Account / owner | Authenticated individual who owns their profile, wardrobe, and future saved data |
| Wardrobe | User-maintained records of clothing/accessories actually available to that user |
| Context | Explicit occasion, location/time, weather, activity, transport, and request constraints |
| Hard constraint | Approved mandatory restriction that excludes an otherwise possible candidate |
| Soft preference | Approved ranking preference that does not itself exclude all other candidates |
| Candidate / look | Proposed combination of eligible owned items under agreed composition rules |
| Deterministic baseline | Documented rules giving the same ordered result for identical inputs and rule version |
| AI assistance | Optional external or local model help; provider and data-sharing policy are undecided |
| DTO | Boundary object for incoming/outgoing API data, separate from a persistence entity |
| Session | Server-held Spring Security authentication, referenced by a browser-managed cookie |
| CSRF / CORS | Cross-Site Request Forgery protection / Cross-Origin Resource Sharing policy |
| UTF-8 byte limit | Password encoding boundary; distinct from Java/JavaScript UTF-16 string length |
| MVP | Proposed first usable wardrobe-to-outfit release; its exact boundary still needs approval |
| TBD | Decision or detail intentionally unresolved, not an implementation instruction to guess |

### 1.5 Conventions and requirement lifecycle

- FR identifies a functional requirement; NFR identifies a quality or engineering requirement; UC identifies a use case; DEC identifies an unresolved decision.
- Must, Should, and Could express relative priority. Priorities for unimplemented features are proposed for review and do not establish approved delivery dates.
- Implemented means concrete source supports the stated behavior. It does not mean independently certified, deployed, or re-tested during this documentation task.
- Planned means intended but not implemented. Requires clarification means part of the behavior or its release selection cannot be accepted until the linked decision is resolved.
- “Shall” states the target behavior. Planned/clarification requirements apply when their feature is selected; they are not claims about the current build.
- IDs remain stable. Preserve IDs on wording changes, retire obsolete IDs explicitly, and do not reuse them. Resolve decisions with dated rationale and update traceability.
- Each functional requirement specifies input/output, error behavior, and acceptance criteria. Quality requirements state observable verification criteria; unknown thresholds remain TBD.
- Requirements and use cases reference actual source/test files only where they exist. Future schemas, API routes, vendors, rules, and numeric service levels are not finalized by illustrative text.

### 1.6 Evidence and precedence

Current source, dependency manifests, and test definitions establish the implementation snapshot. [Frontend integration guide][webdoc] and [Backend session guide][apidoc] explain integration/testing. The root [Product backlog][roadmap] is an informal roadmap, not a confirmed set of stakeholder decisions. At the baseline 1.0 inspection, no root README, SRS, or requirements directory existed; subsequent milestones created them.

Baseline 1.0 recorded the Phase 1 source snapshot without rerunning tests. Revision 1.1 records the owner-approved Phase 2 implementation and actual verification in the [Phase 2 guide](../testing/phase-2-verification.md). Revision 1.3 records the implemented optional appearance extension and local PostgreSQL/live browser verification. The backend guide links current frontend/profile documentation; informal future epics remain proposed.

## 2. Overall system description

### 2.1 Product perspective and system boundary

The repository is a monorepo: apps/web is a Next.js application and apps/api is a Spring Boot service, backed by PostgreSQL hosted on Neon. The browser talks directly to the API using credentialed Fetch requests. Spring Security is the authorization boundary. Future weather, media, and AI providers are external dependencies to be selected, not existing connected services.

The intended processing chain is: user-owned wardrobe + confirmed context and preferences → eligibility filtering → outfit composition → deterministic ranking → grounded explanation → optional assisted wording → user selection/save/feedback. Inputs and reasons must remain inspectable; generated text must not be treated as the source of wardrobe ownership or suitability facts.

### 2.2 Implementation inventory

| Area | Evidence-backed current state |
| --- | --- |
| Phase 0 | Frontend/backend scaffolding, workspace tooling, theme/components, JPA/Neon configuration |
| Public entry | Landing page with local editorial images and functioning registration/login links |
| Authentication UI | Approved Register/Login split screen with reversible image-panel animation, responsive mobile form, reduced-motion behavior |
| Account creation | Validated request, normalized name/email, duplicate protection, BCrypt persistence, safe 201 response |
| Authentication | Session login, current-account endpoint, logout, CSRF bootstrap/rotation, CORS/cookie configuration |
| Frontend integration | Typed Fetch client, session restoration/reconciliation, loading/errors, client private-route gate |
| Dashboard | Safe account information, sign-out, and non-blocking Start/Resume/Edit preference entry points |
| Forgot Password / social | UI present but reset submission and Google/Apple actions are explicitly unavailable |
| Styling profile | Five-step optional onboarding; save/resume/skip/complete; separate styling and appearance editors/persistence; owner-bound versioned REST APIs |
| Future product modules | No wardrobe, event, recommendation, saved-outfit, or feedback implementation |
| Operational scope | Local/testable configuration; production profile exists, but production deployment and multi-instance continuity are not established |

### 2.3 User classes and characteristics

| Class | Capabilities and needs |
| --- | --- |
| Visitor | Reads the landing page, creates an account, signs in; may encounter the unavailable recovery screen |
| Authenticated individual | Views safe account details, signs out, and manages a styling profile; future owner of wardrobe, plans, and feedback |
| Returning or expired-session user | Needs session restoration, meaningful loading/retry states, and clear sign-in recovery |
| User with accessibility needs | May use keyboard, assistive technology, small screens, zoom, or reduced motion; avoid relying on animation or imagery alone |
| Developer / maintainer | Operates local services, manages configuration, reviews tests, and evolves documented rules; not an in-application admin role |

Different fashion tastes, comfort/modesty choices, and wardrobes are user-supplied data, not different authorization roles. A dedicated administrator and taxonomy-management interface are unconfirmed later scope.

### 2.4 Current frontend routes and reusable foundations

| URL | Source / current function |
| --- | --- |
| / | [Landing page][landing]: public landing with header, hero, and category imagery |
| /register | [Register page][register]: real account creation |
| /login | [Login page][login]: real session login |
| /forgot-password | [Forgot Password page][forgot]: UI-only unavailable recovery, back link to Login |
| /dashboard | [Minimal dashboard][dashboard]: protected account and styling setup entry |
| /onboarding | [Onboarding page][onboarding]: protected five-step save/resume flow with optional personal details |
| /profile | [Profile page][profilePage]: protected styling and separate Personal Details & Fit editors; identity read-only |

The (auth) and (app) route groups organize layouts without appearing in URLs. Wardrobe, planner, recommendations, and saved have no implemented pages. AuthShell/AuthLink/AuthField, SessionProvider/RequireAuth, Button/theme, shared preference controls, and the credentialed session transport are reused. The approved authentication design and navigation remain intact.

### 2.5 Operating environment and dependencies

Development uses Windows/PowerShell, Java 21 with Maven Wrapper, Node.js 20.9.0 or newer, and pnpm. Local browser/API origins default to localhost:3000 and localhost:8080. Neon is the development database target; isolated tests can instead use a dedicated local PostgreSQL database. The Phase 2 opt-in integration tests require a guarded loopback phase2_test database and do not connect to Neon.

Future wardrobe media, weather sourcing, AI calls, and notification delivery add availability, cost, privacy, and quota dependencies that are not yet selected.

### 2.6 Assumptions and constraints

The initial product serves an individual's own wardrobe. There is no sharing, marketplace, organization tenancy, or trained proprietary model in the baseline. Preferences are editable explicit choices. Same-site browser/API hosting is the current cookie assumption. The owner reviews unresolved product choices before implementation rather than accepting defaults invented by a developer.

Secrets remain outside source control and frontend-public variables. Existing working authentication, UI transitions, and database email uniqueness must be preserved. Phase 2 implementation follows the approved onboarding contract. Further modules, destructive schema changes, and production migration decisions require separate review.

## 3. System features and functional requirements

There are 66 functional requirements. FR-AUTH-001 through FR-AUTH-012, FR-ONB-001, FR-ONB-002, FR-ONB-004 and FR-APPR-001 through FR-APPR-008 are implemented. Other functional requirements remain planned or require clarification. Profile metadata supporting future recommendation requirements is not evidence of an implemented engine.

### 3.1 Authentication and public entry

<a id="fr-auth-001"></a>

#### FR-AUTH-001 — Public entry and authentication navigation

**Priority:** Must. **Status:** Implemented.

The system shall provide a public landing page and client-side entry links to registration and login.

- **Inputs / outputs:** Input: public navigation. Output: /, /register, or /login with the existing fashion/editorial presentation.
- **Validation / errors:** Unknown URLs must not be presented as implemented features; category tiles remain presentational.
- **Acceptance:** Get Started opens /register; Sign in opens /login without a full-page reload. Outfits, Makeup, Hair, and Accessories tiles do not imply working category services.
- **Evidence:** [Landing page][landing], [Landing hero][hero], [Styling categories][categories]; tests: [Frontend auth browser tests][tBrowser].

<a id="fr-auth-002"></a>

#### FR-AUTH-002 — Register an account

**Priority:** Must. **Status:** Implemented.

The system shall create an account from fullName, email, and raw password, storing only its encoded password hash.

- **Inputs / outputs:** Input: valid registration JSON and CSRF header/cookie. Output: HTTP 201 with id, fullName, email.
- **Validation / errors:** Trim the name; trim and lowercase email with Locale.ROOT. A failed persistence operation must not report creation.
- **Acceptance:** Valid registration persists one account and returns only safe fields. The frontend transitions to Login with an account-created notice; registration does not establish a new authenticated session.
- **Evidence:** [AuthController][controller], [AuthService][service], [Password encoder][encoder], [User entity][entity], [Register page][register], [SessionProvider][provider]; tests: [AuthServiceTests][tService], [AuthControllerTests][tController], [Frontend auth browser tests][tBrowser].

<a id="fr-auth-003"></a>

#### FR-AUTH-003 — Validate registration input

**Priority:** Must. **Status:** Implemented.

The system shall validate registration at the HTTP boundary and provide field-specific input errors.

- **Inputs / outputs:** Input: fullName, email, password. Output: accepted request or HTTP 400 with fieldErrors.
- **Validation / errors:** Required nonblank name up to 150 string units; valid nonblank email up to 254; nonblank password at least 8 UTF-16 units and at most 72 UTF-8 bytes. Do not add complexity patterns or alter passwords.
- **Acceptance:** Empty fields, invalid email, seven-unit passwords, and 73-byte passwords fail before account creation. Eight-unit passwords and otherwise valid 72-byte ASCII/Unicode passwords pass validation. Frontend trimming of email precedes submission.
- **Evidence:** [RegisterRequest][regdto], [UTF-8 validator][byteval], [Frontend validation][validation], [Register page][register], [MVC exception handler][errors]; tests: [RegisterRequestTests][tReg], [AuthControllerTests][tController], [Frontend validation tests][tValidation], [Frontend auth browser tests][tBrowser].

<a id="fr-auth-004"></a>

#### FR-AUTH-004 — Reject duplicate email safely

**Priority:** Must. **Status:** Implemented.

The system shall reject registration for an existing normalized email, including an email uniqueness race.

- **Inputs / outputs:** Input: registration using an existing normalized address. Output: HTTP 409, Email is already registered.
- **Validation / errors:** The pre-check does not replace PostgreSQL uniqueness. Only the recognized email constraint violation maps to this conflict; unrelated database errors remain safe server errors.
- **Acceptance:** Case/whitespace variants checked by the service collide; no second account is created. Tests demonstrate duplicate pre-check and unique-constraint race handling without misclassifying unrelated failures.
- **Evidence:** [AuthService][service], [UserRepository][repository], [User entity][entity], [MVC exception handler][errors]; tests: [AuthServiceTests][tService], [AuthControllerTests][tController].

<a id="fr-auth-005"></a>

#### FR-AUTH-005 — Authenticate with a server session

**Priority:** Must. **Status:** Implemented.

The system shall authenticate a normalized email/password through Spring Security and BCrypt, then persist the security context in the HTTP session.

- **Inputs / outputs:** Input: login JSON plus CSRF. Output: HTTP 200 safe account and browser-managed session cookie.
- **Validation / errors:** Unknown email and wrong password both produce HTTP 401 with Invalid email or password; credentials must not appear in the response.
- **Acceptance:** A correct registration-compatible BCrypt password authenticates; an existing session ID rotates; a later /me request authenticates using the session. Successful frontend login opens /dashboard.
- **Evidence:** [AuthController][controller], [UserDetailsService][details], [AccountPrincipal][principal], [SecurityConfig][security], [Login page][login], [SessionProvider][provider]; tests: [PasswordAuthenticationTests][tPassword], [RepositoryUserDetailsServiceTests][tDetails], [SessionAuthenticationTests][tSession], [SessionCookieTests][tCookie], [Frontend auth browser tests][tBrowser].

<a id="fr-auth-006"></a>

#### FR-AUTH-006 — Validate login without changing passwords

**Priority:** Must. **Status:** Implemented.

The system shall reject malformed login input before BCrypt verification while preserving password contents.

- **Inputs / outputs:** Input: email and raw password. Output: HTTP 400 field errors or credential verification.
- **Validation / errors:** Email is trimmed, nonblank, valid, and at most 254 string units. Password is nonblank and at most 72 UTF-8 bytes; the registration minimum is not imposed during login.
- **Acceptance:** An over-limit multibyte password fails before lookup/verification. A short nonblank password reaches verification and receives generic 401 if incorrect. Surrounding password whitespace is preserved.
- **Evidence:** [LoginRequest][logindto], [UTF-8 validator][byteval], [UserDetailsService][details], [Frontend validation][validation], [Login page][login]; tests: [LoginRequestTests][tLogin], [SessionAuthenticationTests][tSession], [Frontend validation tests][tValidation], [Frontend auth browser tests][tBrowser].

<a id="fr-auth-007"></a>

#### FR-AUTH-007 — Bootstrap and refresh CSRF

**Priority:** Must. **Status:** Implemented.

The system shall require a matching CSRF cookie/header for authentication POSTs and expose a public bootstrap endpoint.

- **Inputs / outputs:** Input: credentialed GET /api/auth/csrf. Output: headerName and raw token; send token as X-XSRF-TOKEN on POST.
- **Validation / errors:** Refresh after login/logout rotation. Recognized CSRF 403 permits only one refresh/retry; unrelated 403, network, and 500 failures must not trigger automatic POST replay.
- **Acceptance:** Registration/login/logout fail with 403 when the token pair is missing or mismatched. Bootstrap plus matching cookies succeeds; subsequent operations use refreshed tokens. The frontend retains its token only in memory; the browser also manages the CSRF cookie.
- **Evidence:** [AuthController][controller], [SecurityConfig][security], [Typed Fetch client][client]; tests: [SessionAuthenticationTests][tSession], [Fetch client tests][tClient], [Frontend auth browser tests][tBrowser].

<a id="fr-auth-008"></a>

#### FR-AUTH-008 — Restore and reconcile the current session

**Priority:** Must. **Status:** Implemented.

The system shall restore browser authentication from GET /api/auth/me and reconcile expiration or uncertain authentication outcomes.

- **Inputs / outputs:** Input: credentialed session check on initial load, focus/visibility restoration, or private-route entry. Output: safe account, signed-out state, or retryable connection error.
- **Validation / errors:** Treat /me 401 as signed out; do not treat network/500 failure as proof of logout. Ignore stale checks superseded by authentication mutations.
- **Acceptance:** Refreshing an authenticated dashboard restores its identity. Expiration redirects to Login with a sign-in notice. Delayed old checks cannot override a newer login/logout decision.
- **Evidence:** [AuthController][controller], [SessionProvider][provider], [Typed Fetch client][client]; tests: [SessionAuthenticationTests][tSession], [Frontend auth browser tests][tBrowser], [Fetch client tests][tClient].

<a id="fr-auth-009"></a>

#### FR-AUTH-009 — Log out reliably

**Priority:** Must. **Status:** Implemented.

The system shall invalidate the authenticated session, clear its security context, and expire session/CSRF cookies on logout.

- **Inputs / outputs:** Input: authenticated, CSRF-protected POST /api/auth/logout. Output: HTTP 204, cleared frontend identity, Login navigation.
- **Validation / errors:** Handle 204 without JSON parsing. An already-expired session is signed out; network/server failure must not falsely claim successful logout and must trigger session reconciliation.
- **Acceptance:** After logout, /me returns 401. Logout without valid CSRF cannot invalidate a valid session. A lost response is reconciled from the server, and an authenticated failure remains retryable.
- **Evidence:** [AuthController][controller], [SecurityConfig][security], [Typed Fetch client][client], [SessionProvider][provider], [Minimal dashboard][dashboard]; tests: [SessionAuthenticationTests][tSession], [SessionCookieTests][tCookie], [Fetch client tests][tClient], [Frontend auth browser tests][tBrowser].

<a id="fr-auth-010"></a>

#### FR-AUTH-010 — Gate private navigation

**Priority:** Must. **Status:** Implemented.

The system shall gate private client navigation until the current session can be resolved.

- **Inputs / outputs:** Input: direct, refreshed, or client-side /dashboard navigation. Output: checking, authenticated content, sign-in redirect, or connection-error retry.
- **Validation / errors:** Client gating is not authorization for API data, Server Components, or Server Actions; Spring remains the backend authorization boundary.
- **Acceptance:** Signed-out users reach Login; unresolved/network-error states do not display account details. Try again repeats verification without claiming authentication from cookie presence.
- **Evidence:** [RequireAuth][guard], [SessionProvider][provider], [SecurityConfig][security]; tests: [Frontend auth browser tests][tBrowser], [SessionAuthenticationTests][tSession].

<a id="fr-auth-011"></a>

#### FR-AUTH-011 — Show a minimal authenticated account screen

**Priority:** Must. **Status:** Implemented.

The system shall display a welcome message and only safe current-account information on the initial dashboard.

- **Inputs / outputs:** Input: verified /me or login account. Output: full name, email, account ID, and Sign out.
- **Validation / errors:** Do not fetch an arbitrary client-selected user ID or expose a password/hash. No wardrobe summary or recommendation is implied by this screen.
- **Acceptance:** Dashboard identity matches the session principal; logout has pending/error states. The current /dashboard remains an account placeholder rather than the planned full styling dashboard.
- **Evidence:** [Minimal dashboard][dashboard], [AuthController][controller]; tests: [Frontend auth browser tests][tBrowser], [SessionAuthenticationTests][tSession].

<a id="fr-auth-012"></a>

#### FR-AUTH-012 — Preserve approved auth UX and unavailable actions

**Priority:** Must. **Status:** Implemented.

The system shall preserve approved Register/Login navigation and clearly mark unsupported authentication actions unavailable.

- **Inputs / outputs:** Input: auth links, keyboard submission, or unavailable controls. Output: responsive layouts, opposite image/form states, reduced-motion behavior, and disabled unsupported controls.
- **Validation / errors:** Forgot Password must not send mail or simulate success. Google/Apple buttons must not establish fake authentication.
- **Acceptance:** Register-to-Login moves the image right-to-left and the reverse restores it; URLs/history agree with the screen. Reduced motion removes the large transition. Reset/social controls are disabled with explanations.
- **Evidence:** [AuthShell][shell], [Auth motion CSS][motion], [AuthLink][authlink], [Forgot Password page][forgot], [Social buttons][social]; tests: [Frontend auth browser tests][tBrowser].

### 3.2 Onboarding and styling preferences

<a id="fr-onb-001"></a>

#### FR-ONB-001 — Capture a personal styling profile

**Priority:** Must. **Status:** Implemented.

The system shall allow an authenticated user to enter optional style, color, fit, footwear, comfort, coverage, occasion, activity, and transport preferences from the supported catalog.

- **Inputs / outputs:** Ten complete nullable groups; output is an owner-bound profile snapshot. null is unanswered and [] is explicit No preference.
- **Validation / errors:** Reject unknown/duplicate codes, invalid coverage strength, incomplete snapshots, and contradictory preferred/disliked colors before mutation. Account ownership is session-derived.
- **Acceptance:** Valid choices and explicit empty/unanswered groups survive save/refresh for their owner; a second account cannot retrieve them. Invalid writes leave the prior profile intact.
- **Evidence:** [Profile controller][profileController], [profile service][profileService], [profile workspace][profileWorkspace]; [persistence tests][tProfilePersistence], [HTTP integration][tProfileHttp], [browser tests][tOnboarding].
- **Decision:** DEC-002 resolved for Phase 2; initial profile vocabulary approved. Wardrobe/context taxonomy under DEC-004 remains open.

<a id="fr-onb-002"></a>

#### FR-ONB-002 — Control onboarding completion and skipping

**Priority:** Should. **Status:** Implemented.

The system shall provide five optional UI steps (STYLE, COMFORT, LIFESTYLE, APPEARANCE, REVIEW) and distinct NOT_STARTED, IN_PROGRESS, SKIPPED, COMPLETED states without blocking Dashboard.

- **Inputs / outputs:** Explicit SAVE, SKIP, COMPLETE with version, resume step, and complete preference snapshot; output is the committed snapshot.
- **Validation / errors:** Next saves before advancing; Back preserves local edits; Save and exit persists; Skip retains choices; Finish atomically completes the styling resource even with all-unanswered input. Appearance is a separate resource: changed appearance saves first, then styling progress; a failed second save must disclose partial success and preserve edits. Completed profiles remain completed on SAVE and cannot SKIP; first completedAt is retained.
- **Acceptance:** Refresh restores the saved step/choices. Dashboard offers Start, Resume, or Edit. GET alone creates no row. Only an explicit COMPLETE finishes; no checkbox change auto-saves. Stale writes conflict instead of overwriting.
- **Evidence:** [Onboarding flow][onboardingFlow], [profile service][profileService]; [service tests][tProfileService], [onboarding browser tests][tOnboarding], [live browser test][tProfileLive].
- **Decision:** DEC-002 resolved; future recommendation access/constraint precedence is not implemented by this flow.

<a id="fr-onb-003"></a>

#### FR-ONB-003 — Separate restrictions from preferences

**Priority:** Must. **Status:** Requires clarification.

The system shall distinguish user-designated mandatory restrictions from preferences used for ranking.

- **Inputs / outputs:** Input: preference/restriction choices. Output: explicit constraint type and scope for the recommendation request.
- **Validation / errors:** Do not silently convert a preference to a restriction or relax a restriction. Default classifications and conflict precedence require approval.
- **Acceptance:** Fixtures show a mandatory exclusion removing an item while an otherwise comparable soft preference changes ranking only; the user can identify which constraints are active. Phase 2 stores optional coverage as PREFERENCE/REQUIRED and all other choices as preferences, but exclusion/ranking/override policies remain unimplemented.
- **Open decisions:** DEC-003.

<a id="fr-onb-004"></a>

#### FR-ONB-004 — Read and update styling preferences

**Priority:** Must. **Status:** Implemented.

The system shall allow the profile owner to review and update saved styling preferences without editing account identity/passwords.

- **Inputs / outputs:** A complete preference replacement with the last returned version; output is the updated saved profile. Saved values are available to future recommendation consumers, which remain planned under FR-ONB-006 and FR-REC requirements.
- **Validation / errors:** Invalid/unauthenticated changes are rejected. Genuine version conflicts preserve local edits and require reload. Uncertain writes are reconciled with GET, never blindly replayed. Cancel discards edits without bypassing conflict locks.
- **Acceptance:** Save, refresh, and read back changed choices; preserve null versus []; reject stale versions without partial writes; account A state/queued writes cannot be accepted under account B's managed session. Future recommendation use and historical-outfit behavior are verified when those modules exist.
- **Evidence:** [Profile editor][profileEditor], [bound profile client][profileClient], [profile service][profileService]; [editor tests][tProfileEditor], [transport tests][tProfileClient], [integration browser tests][tProfileBrowser], [HTTP integration][tProfileHttp].
- **Decision:** DEC-002 resolved for profile editing; broader taxonomy/consumer rules remain open.

<a id="fr-onb-005"></a>

#### FR-ONB-005 — Use self-declared comfort and modesty choices

**Priority:** Must. **Status:** Planned.

The system shall use the user's explicit comfort/modesty choices rather than infer restrictions from gender, event labels, or transportation.

- **Inputs / outputs:** Input: declared preferences and request-specific constraints. Output: attributed suitability criteria.
- **Validation / errors:** Unknown preferences must remain unknown; do not invent cultural or personal restrictions.
- **Acceptance:** Changing transportation alone does not create a new modesty restriction. Adding an explicit coverage constraint changes eligibility according to the approved rule catalog. Phase 2 implements self-declared capture without inference; eligibility enforcement remains planned.
- **Open decisions:** DEC-003, DEC-004.

<a id="fr-onb-006"></a>

#### FR-ONB-006 — Review profile-derived request defaults

**Priority:** Should. **Status:** Planned.

The system shall show profile-derived defaults when preparing a recommendation and allow permitted request-specific overrides.

- **Inputs / outputs:** Input: saved profile and current context. Output: an effective preference/constraint set visible before generation.
- **Validation / errors:** An unsaved request override must not silently alter the stored profile; mandatory/soft precedence follows the approved policy.
- **Acceptance:** An override affects the current result; a later request uses the saved profile unless the user separately updates it. Effective constraints are inspectable.
- **Open decisions:** DEC-003.

### 3.3 Digital wardrobe

<a id="fr-ward-001"></a>

#### FR-WARD-001 — Add a wardrobe item

**Priority:** Must. **Status:** Planned.

The system shall let an authenticated user record clothing they own using the agreed item attributes.

- **Inputs / outputs:** Input: item description and category plus required attributes once defined. Output: an owned wardrobe item.
- **Validation / errors:** Reject missing required attributes and unsupported values without creating a partial item.
- **Acceptance:** A valid submission appears in that user's wardrobe; invalid input identifies the affected fields. Required attributes are resolved under DEC-004.
- **Open decisions:** DEC-004.

<a id="fr-ward-002"></a>

#### FR-WARD-002 — Browse wardrobe items

**Priority:** Must. **Status:** Planned.

The system shall display and retrieve the authenticated user's wardrobe items and their recorded details.

- **Inputs / outputs:** Input: current account and item selection. Output: item list, empty state, or item details.
- **Validation / errors:** Never return another user's item; distinguish not-found from service-unavailable without disclosing ownership.
- **Acceptance:** An owner can view a newly added item; another account cannot retrieve it by substituting its identifier.

<a id="fr-ward-003"></a>

#### FR-WARD-003 — Edit wardrobe information

**Priority:** Must. **Status:** Planned.

The system shall let an item owner correct the item's descriptive attributes.

- **Inputs / outputs:** Input: owned item and changed attributes. Output: updated item.
- **Validation / errors:** Validate changes before persistence and retain prior values on failure.
- **Acceptance:** A saved edit is visible after refresh and is used by subsequent recommendations; invalid edits do not partially overwrite the item.
- **Open decisions:** DEC-004.

<a id="fr-ward-004"></a>

#### FR-WARD-004 — Remove a wardrobe item

**Priority:** Must. **Status:** Planned.

The system shall let an owner remove an item from their active wardrobe after an explicit confirmation.

- **Inputs / outputs:** Input: owned item and confirmation. Output: item no longer selectable for new recommendations.
- **Validation / errors:** Cancel leaves the item unchanged; failed deletion must not report success. Historical references and media retention need a decision.
- **Acceptance:** Removed items are absent from new recommendation candidates; treatment of existing saved looks is specified before implementation.
- **Open decisions:** DEC-005, DEC-014.

<a id="fr-ward-005"></a>

#### FR-WARD-005 — Represent item availability

**Priority:** Must. **Status:** Requires clarification.

The system shall exclude wardrobe items the user has marked unavailable from new outfit recommendations.

- **Inputs / outputs:** Input: item availability state. Output: eligible or excluded item.
- **Validation / errors:** Availability states, default, duration, and whether temporary unavailability affects saved looks remain TBD.
- **Acceptance:** With one item explicitly unavailable, a new recommendation cannot select it; restoring availability makes it eligible again.
- **Open decisions:** DEC-004.

<a id="fr-ward-006"></a>

#### FR-WARD-006 — Search and filter the wardrobe

**Priority:** Should. **Status:** Planned.

The system shall let users narrow their own wardrobe using supported text and attribute filters.

- **Inputs / outputs:** Input: search text and selected filters. Output: matching owned items and a clear empty state.
- **Validation / errors:** Unsupported filter values must not broaden access or cause an unhandled error; filter combination rules remain to be specified.
- **Acceptance:** A known matching item is returned, a nonmatching item is excluded, and clearing filters restores the complete owned list.
- **Open decisions:** DEC-004.

<a id="fr-ward-007"></a>

#### FR-WARD-007 — Attach wardrobe images

**Priority:** Should. **Status:** Requires clarification.

The system shall support an owner attaching and replacing an image for a wardrobe item once media handling is defined.

- **Inputs / outputs:** Input: user-selected image. Output: an item image or actionable rejection.
- **Validation / errors:** Allowed formats, size limits, metadata handling, storage, access controls, and deletion policy are unresolved; do not accept arbitrary remote fetch URLs.
- **Acceptance:** An allowed image is associated only with its owner's item; rejected files create no usable media association; unauthorized access is denied.
- **Open decisions:** DEC-005.

<a id="fr-ward-008"></a>

#### FR-WARD-008 — Assist item classification

**Priority:** Could. **Status:** Requires clarification.

The system shall offer optional AI-assisted suggestions for wardrobe attributes, subject to user review before saving.

- **Inputs / outputs:** Input: permitted item image or description. Output: proposed attributes, clearly identified as suggestions.
- **Validation / errors:** A provider failure or uncertain suggestion must leave manual entry available; suggestions cannot silently replace confirmed data.
- **Acceptance:** A user can accept, edit, or discard suggested attributes and complete item entry without AI availability.
- **Open decisions:** DEC-004, DEC-006.

### 3.4 Occasion and outfit planning

<a id="fr-plan-001"></a>

#### FR-PLAN-001 — Specify an occasion

**Priority:** Must. **Status:** Planned.

The system shall accept an occasion or event description as recommendation context.

- **Inputs / outputs:** Input: supported occasion selection or custom description. Output: explicit occasion context.
- **Validation / errors:** Do not infer a person's preferences from the occasion label; predefined options and formality mapping are not finalized.
- **Acceptance:** A user can request a look for an outing with friends and review the occasion supplied to the recommendation process.
- **Open decisions:** DEC-004.

<a id="fr-plan-002"></a>

#### FR-PLAN-002 — Specify location and environment

**Priority:** Must. **Status:** Requires clarification.

The system shall accept location and indoor/outdoor context sufficient for the selected weather and styling functions.

- **Inputs / outputs:** Input: user-provided place and environment. Output: confirmed context with the needed location precision.
- **Validation / errors:** Location precision, geocoding, permission requests, retention, and a manual alternative remain TBD; do not require continuous tracking.
- **Acceptance:** A user can review and correct the location; refusing optional device location does not prevent a manual-context recommendation.
- **Open decisions:** DEC-008.

<a id="fr-plan-003"></a>

#### FR-PLAN-003 — Specify date, time, and duration

**Priority:** Must. **Status:** Planned.

The system shall accept the date, time, and relevant duration of the intended activity with an explicit time-zone interpretation.

- **Inputs / outputs:** Input: chosen date/time and optional duration as agreed. Output: time context for weather and suitability.
- **Validation / errors:** Reject impossible or ambiguous values with guidance; forecast limits and time-zone defaults must be disclosed.
- **Acceptance:** The reviewed context preserves the intended local event time, including when the user and event locations differ.
- **Open decisions:** DEC-007, DEC-008.

<a id="fr-plan-004"></a>

#### FR-PLAN-004 — Specify activities and transportation

**Priority:** Must. **Status:** Planned.

The system shall let users supply relevant activities, walking exposure, and transportation context.

- **Inputs / outputs:** Input: user-selected activities and travel details. Output: explicit recommendation context.
- **Validation / errors:** Options and optionality require definition; travel mode must not imply unselected gender, modesty, or style preferences.
- **Acceptance:** A public-transport outing involving walking carries those inputs to the suitability evaluation without inventing user preferences.
- **Open decisions:** DEC-004.

<a id="fr-plan-005"></a>

#### FR-PLAN-005 — Review request-specific constraints

**Priority:** Must. **Status:** Planned.

The system shall let users review profile defaults and request-specific comfort, modesty, and style choices before generating outfits.

- **Inputs / outputs:** Input: profile defaults and per-request selections. Output: confirmed effective context.
- **Validation / errors:** Resolve precedence and incompatible constraints explicitly rather than silently dropping restrictions.
- **Acceptance:** The request summary shows the effective constraints; changing a request setting does not silently overwrite the saved profile.
- **Open decisions:** DEC-003.

<a id="fr-plan-006"></a>

#### FR-PLAN-006 — Save a future outfit plan

**Priority:** Should. **Status:** Requires clarification.

The system shall let users associate a selected outfit with a future event if event persistence is included in the release.

- **Inputs / outputs:** Input: event details and owned saved outfit. Output: a retrievable plan.
- **Validation / errors:** Handling deleted items, unavailable items, time changes, and whether plans reserve clothing remain TBD.
- **Acceptance:** An owner can retrieve the selected outfit and event details after refresh; no claim of clothing reservation is made without an agreed rule.
- **Open decisions:** DEC-009, DEC-016.

<a id="fr-plan-007"></a>

#### FR-PLAN-007 — Manage planned events

**Priority:** Could. **Status:** Requires clarification.

The system shall let users view, revise, and cancel their saved outfit plans if calendar planning is selected.

- **Inputs / outputs:** Input: owned plan and changes. Output: updated agenda or cancellation.
- **Validation / errors:** Reject unauthorized changes; external calendar synchronization and reminders are separate future decisions.
- **Acceptance:** An edited event appears at its new time; cancellation removes it from upcoming plans without deleting the wardrobe.
- **Open decisions:** DEC-016.

### 3.5 Context-aware recommendations

<a id="fr-rec-001"></a>

#### FR-REC-001 — Use the available owned wardrobe

**Priority:** Must. **Status:** Planned.

The system shall generate wardrobe-based outfit candidates only from items owned by the authenticated user and eligible for the request.

- **Inputs / outputs:** Input: owned wardrobe, availability, and confirmed context. Output: eligible candidate pool.
- **Validation / errors:** An empty wardrobe must produce guidance, not fabricated owned items; shopping suggestions are a separate future feature.
- **Acceptance:** Every selected item belongs to the requesting account and satisfies the agreed availability rule.
- **Open decisions:** DEC-004, DEC-009.

<a id="fr-rec-002"></a>

#### FR-REC-002 — Apply explicit constraints

**Priority:** Must. **Status:** Requires clarification.

The system shall enforce confirmed hard restrictions before ranking outfit candidates.

- **Inputs / outputs:** Input: candidate pool and effective hard constraints. Output: eligible candidates or an explained conflict.
- **Validation / errors:** Hard versus soft classification and precedence require approval; the engine must not silently relax a hard restriction.
- **Acceptance:** A fixture with a disallowed item never includes that item; incompatible hard restrictions yield a visible no-result or conflict outcome.
- **Open decisions:** DEC-003.

<a id="fr-rec-003"></a>

#### FR-REC-003 — Compose coordinated outfits

**Priority:** Must. **Status:** Requires clarification.

The system shall compose candidates using the agreed clothing slots and coordination rules, including shoes or accessories where applicable.

- **Inputs / outputs:** Input: eligible items and slot/coordination rules. Output: candidates with identifiable component items.
- **Validation / errors:** Required versus optional slots, dress alternatives, layering, and incomplete wardrobes are unresolved.
- **Acceptance:** Each candidate satisfies the approved slot rules; if required components are absent, the result explains the gap instead of inventing an item.
- **Open decisions:** DEC-004, DEC-009.

<a id="fr-rec-004"></a>

#### FR-REC-004 — Rank candidates transparently

**Priority:** Must. **Status:** Requires clarification.

The system shall use a documented deterministic filtering and ranking baseline before optional AI assistance.

- **Inputs / outputs:** Input: eligible candidates and supported context factors. Output: an ordered result set with explainable ranking factors.
- **Validation / errors:** Weights, tie-breaks, candidate count, and missing-attribute handling are TBD; random or provider-dependent behavior must not define the baseline.
- **Acceptance:** With the same versioned rules, wardrobe, and context, repeated evaluation yields the same ordered candidates; ranking fixtures identify the contributing rules.
- **Open decisions:** DEC-010.

<a id="fr-rec-005"></a>

#### FR-REC-005 — Incorporate weather context

**Priority:** Must. **Status:** Requires clarification.

The system shall use relevant weather conditions for the chosen location and time when a supported weather source is available.

- **Inputs / outputs:** Input: confirmed location/time and provider response or user-supplied weather. Output: attributed weather context used by the rules.
- **Validation / errors:** Provider, fields, units, forecast horizon, staleness, and manual fallback need decisions; missing weather must not be presented as current facts.
- **Acceptance:** A weather fixture changes applicable suitability rules; unavailable or out-of-range forecasts produce a disclosed fallback rather than a fabricated forecast.
- **Open decisions:** DEC-007, DEC-008.

<a id="fr-rec-006"></a>

#### FR-REC-006 — Explain insufficient matches

**Priority:** Must. **Status:** Planned.

The system shall report when wardrobe availability or user constraints prevent a suitable recommendation.

- **Inputs / outputs:** Input: filtered candidate outcome. Output: understandable reasons and available user-controlled next steps.
- **Validation / errors:** Do not silently bypass restrictions or imply success with an unsuitable candidate.
- **Acceptance:** A no-match fixture identifies the relevant missing item category or conflicting restriction and permits context revision.
- **Open decisions:** DEC-003, DEC-009.

<a id="fr-rec-007"></a>

#### FR-REC-007 — Provide rule-based explanations

**Priority:** Must. **Status:** Planned.

The system shall explain recommendations using the actual context and deterministic rules that selected or ranked the outfit.

- **Inputs / outputs:** Input: candidate and rule evaluation evidence. Output: plain-language reasons.
- **Validation / errors:** Do not claim weather, preferences, or attributes that were not supplied or evaluated.
- **Acceptance:** A hot-weather, walking/public-transport, comfort/modesty scenario produces reasons tied to recorded inputs and selected item attributes, without invented suitability thresholds.
- **Open decisions:** DEC-003, DEC-007, DEC-010.

<a id="fr-rec-008"></a>

#### FR-REC-008 — Revise context and compare alternatives

**Priority:** Should. **Status:** Planned.

The system shall let users revise recommendation inputs and review alternative eligible outfits when alternatives exist.

- **Inputs / outputs:** Input: changed context or alternative selection. Output: refreshed results with their corresponding explanations.
- **Validation / errors:** Prevent old results from being mislabeled as results for the new context; do not promise a fixed number of alternatives.
- **Acceptance:** After changing an input, results identify the effective context; users can inspect another eligible candidate without altering wardrobe data.
- **Open decisions:** DEC-010.

<a id="fr-rec-009"></a>

#### FR-REC-009 — Evaluate practical activity and travel suitability

**Priority:** Must. **Status:** Requires clarification.

The system shall evaluate relevant activity and transportation factors using documented garment attributes and user-selected preferences.

- **Inputs / outputs:** Input: activities, exposure, transport, comfort preferences, and eligible item attributes. Output: applicable filtering or ranking evidence.
- **Validation / errors:** Specific suitability rules require approval; transport must not automatically imply a modesty or gender-based restriction.
- **Acceptance:** Fixtures distinguish a walking-intensive outing from a seated indoor event using approved rules while honoring the same explicit personal restrictions.
- **Open decisions:** DEC-003, DEC-004, DEC-010.

### 3.6 AI-assisted explanations

<a id="fr-ai-001"></a>

#### FR-AI-001 — Optionally enhance recommendation explanations

**Priority:** Should. **Status:** Requires clarification.

The system shall optionally use an AI provider to improve the wording of already grounded recommendation explanations.

- **Inputs / outputs:** Input: minimized candidate facts and rule evidence. Output: an optional enhanced explanation, identified as assisted content.
- **Validation / errors:** Provider, model, budget, invocation policy, and release scope remain unconfirmed; AI must not replace eligibility checks.
- **Acceptance:** With AI assistance enabled, the selected items and hard-constraint decisions remain those of the deterministic engine.
- **Open decisions:** DEC-011.

<a id="fr-ai-002"></a>

#### FR-AI-002 — Ground assisted explanations

**Priority:** Must. **Status:** Planned.

The system shall constrain AI-assisted explanations to supplied wardrobe, context, and rule facts.

- **Inputs / outputs:** Input: grounded facts and generated text. Output: a validated explanation or rule-based fallback.
- **Validation / errors:** Reject or avoid presenting unsupported claims, invented owned items, or advice that contradicts explicit restrictions; validation design is TBD.
- **Acceptance:** Test responses that invent items or contradict rule evidence are withheld or replaced by the grounded fallback.
- **Open decisions:** DEC-011.

<a id="fr-ai-003"></a>

#### FR-AI-003 — Retain recommendations without an AI provider

**Priority:** Must. **Status:** Planned.

The system shall keep deterministic recommendations and rule-based explanations available when optional AI assistance fails or is disabled.

- **Inputs / outputs:** Input: provider timeout, rejection, or disabled setting. Output: usable baseline result.
- **Validation / errors:** Do not present provider failure as authentication failure or erase the user's wardrobe/context.
- **Acceptance:** A simulated provider outage still returns eligible candidates and grounded reasons, with a safe notice where appropriate.
- **Open decisions:** DEC-011.

<a id="fr-ai-004"></a>

#### FR-AI-004 — Minimize outbound AI information

**Priority:** Must. **Status:** Requires clarification.

The system shall send only agreed recommendation facts to an AI provider and exclude authentication material and unnecessary identity/location details.

- **Inputs / outputs:** Input: proposed provider payload and permission policy. Output: minimized authorized request or no external request.
- **Validation / errors:** Consent/disclosure, provider retention, exact location, images, and personal-preference sharing require explicit decisions.
- **Acceptance:** A captured provider request contains no password, hash, session identifier, email, or unnecessary precise location; refusal follows the agreed baseline path.
- **Open decisions:** DEC-011, DEC-014.

<a id="fr-ai-005"></a>

#### FR-AI-005 — Offer conversational styling assistance

**Priority:** Could. **Status:** Requires clarification.

The system shall offer contextual styling questions or conversational refinement only if that later capability is approved.

- **Inputs / outputs:** Input: styling question and explicitly permitted context. Output: grounded guidance or a clear inability to answer.
- **Validation / errors:** Conversation retention, safety boundaries, costs, and whether edits require confirmation remain unresolved.
- **Acceptance:** An approved conversation flow cannot silently modify wardrobe/profile data or bypass recommendation restrictions.
- **Open decisions:** DEC-011, DEC-016.

### 3.7 Saved outfits and feedback

<a id="fr-save-001"></a>

#### FR-SAVE-001 — Save a recommended outfit

**Priority:** Must. **Status:** Planned.

The system shall let a user save an outfit they own with enough information to recognize its selected components.

- **Inputs / outputs:** Input: selected candidate. Output: a saved outfit owned by the account.
- **Validation / errors:** Snapshot versus live item references and behavior after item changes must be resolved.
- **Acceptance:** A saved outfit is retrievable after refresh only by its owner and identifies its components.
- **Open decisions:** DEC-009, DEC-014.

<a id="fr-save-002"></a>

#### FR-SAVE-002 — Manage saved outfits

**Priority:** Must. **Status:** Planned.

The system shall let users list, view, and remove their own saved outfits.

- **Inputs / outputs:** Input: account, saved-outfit selection, or removal confirmation. Output: owned list/detail or removal result.
- **Validation / errors:** Removal of a saved outfit must not delete wardrobe items; failed changes must not report success.
- **Acceptance:** A removed saved look disappears from the owner's saved list while its wardrobe items remain available.

<a id="fr-save-003"></a>

#### FR-SAVE-003 — Record recommendation feedback

**Priority:** Should. **Status:** Requires clarification.

The system shall let a user record feedback on a recommendation using the agreed feedback vocabulary.

- **Inputs / outputs:** Input: owned recommendation and feedback. Output: acknowledged feedback or validation error.
- **Validation / errors:** Like/dislike versus rating scale, repeat submissions, edits, and optional reasons require definition.
- **Acceptance:** Feedback is associated with the correct owner and recommendation; unsupported values are rejected.
- **Open decisions:** DEC-012.

<a id="fr-save-004"></a>

#### FR-SAVE-004 — Use feedback in later personalization

**Priority:** Should. **Status:** Requires clarification.

The system shall use recorded feedback in later recommendations only through documented, testable personalization rules.

- **Inputs / outputs:** Input: permitted feedback history and current request. Output: explainable preference adjustments.
- **Validation / errors:** Feedback cannot override explicit hard restrictions; adaptation mechanism and reset controls remain TBD.
- **Acceptance:** A controlled fixture shows the approved influence of feedback without making an otherwise ineligible item eligible.
- **Open decisions:** DEC-003, DEC-012.

<a id="fr-save-005"></a>

#### FR-SAVE-005 — Review outfit and recommendation history

**Priority:** Could. **Status:** Requires clarification.

The system shall distinguish previously recommended outfits from outfits a user explicitly reports wearing if history is introduced.

- **Inputs / outputs:** Input: recommendation event or user wear confirmation. Output: clearly labeled history.
- **Validation / errors:** Do not infer that a displayed or saved outfit was worn; retention and correction/removal rules require approval.
- **Acceptance:** History distinguishes recommendation, saving, and confirmed wearing; users can correct a mistaken wear confirmation.
- **Open decisions:** DEC-014, DEC-016.

<a id="fr-save-006"></a>

#### FR-SAVE-006 — Organize saved looks into collections

**Priority:** Could. **Status:** Requires clarification.

The system shall let users organize their own saved outfits into named collections if collections are selected for a later release.

- **Inputs / outputs:** Input: collection name and owned saved outfits. Output: collection membership.
- **Validation / errors:** Validate names and ownership; deleting a collection must not implicitly delete wardrobe items.
- **Acceptance:** The same saved outfit can be organized according to the agreed membership rule and remains owned by the user.
- **Open decisions:** DEC-016.

### 3.8 Account and privacy management

<a id="fr-acct-001"></a>

#### FR-ACCT-001 — Update account identity

**Priority:** Should. **Status:** Requires clarification.

The system shall support account name or email changes only through an agreed verification and session-consistency process.

- **Inputs / outputs:** Input: authorized changes and required reauthentication. Output: confirmed safe identity.
- **Validation / errors:** Email uniqueness and normalization remain mandatory; verification, reauthentication, and refresh of existing principal snapshots are undecided.
- **Acceptance:** A duplicate normalized email is rejected and subsequent account views follow the agreed identity-refresh policy.
- **Open decisions:** DEC-013.

<a id="fr-acct-002"></a>

#### FR-ACCT-002 — Change an authenticated password

**Priority:** Should. **Status:** Requires clarification.

The system shall support changing a password only with the agreed reauthentication and session-revocation controls.

- **Inputs / outputs:** Input: current authorization evidence and new password. Output: confirmed change or safe error.
- **Validation / errors:** Apply new-password policy and byte boundary without altering contents; session revocation rules and verification flow are TBD.
- **Acceptance:** A successful change stores only a new hash; acceptance includes the agreed behavior of existing sessions.
- **Open decisions:** DEC-013.

<a id="fr-acct-003"></a>

#### FR-ACCT-003 — Delete an account and associated information

**Priority:** Should. **Status:** Requires clarification.

The system shall provide an authenticated account-deletion process with explicit confirmation and a defined retention policy.

- **Inputs / outputs:** Input: authorized deletion request and confirmation. Output: deletion status and session invalidation.
- **Validation / errors:** Removal scope, timing, backups, retained records, and external media/provider deletion are unresolved.
- **Acceptance:** Under an approved policy the deleted account cannot authenticate and associated information is handled exactly as disclosed.
- **Open decisions:** DEC-013, DEC-014.

<a id="fr-acct-004"></a>

#### FR-ACCT-004 — Access or export personal information

**Priority:** Could. **Status:** Requires clarification.

The system shall support an account owner's data-access or export request if included in the agreed privacy scope.

- **Inputs / outputs:** Input: authorized request. Output: an agreed safe representation of that user's data.
- **Validation / errors:** Export format, identity verification, retention, and delivery need decisions; exclude password hashes and other users' information.
- **Acceptance:** An export fixture contains only the requesting user's allowed data and no authentication credentials.
- **Open decisions:** DEC-014.

<a id="fr-acct-005"></a>

#### FR-ACCT-005 — Recover account access

**Priority:** Should. **Status:** Requires clarification.

The system shall provide password recovery only after a secure reset flow and delivery mechanism are approved.

- **Inputs / outputs:** Input: recovery request and later authorized reset proof. Output: a non-enumerating acknowledgement and controlled reset flow.
- **Validation / errors:** Token lifecycle, email delivery, abuse controls, and session invalidation are TBD; the current Forgot Password screen remains explicitly unavailable.
- **Acceptance:** Before delivery is implemented no email-sent success is shown; later acceptance must verify non-enumeration and invalid/expired/reused reset proof.
- **Open decisions:** DEC-013, DEC-015.

### 3.9 Optional personal appearance and fit — approved Phase 2E

These owner-approved requirements are implemented and verified locally. Personal data minimization takes precedence over speculative future usefulness. Values are self-described, with no judgment or inference. See the [appearance contract](../api/styling-profile.md#appearance-profile) and [Phase 2E verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-001"></a>

#### FR-APPR-001 — Optional fit and garment details

**Priority:** Must. **Status:** Implemented.

The system shall allow optional height in whole centimeters, a garment sizing note, and self-described proportions for future garment length/size personalization. Existing fit preferences shall be reused, not duplicated.

- **Inputs / outputs:** Height, sizing note, proportions; a separate owner-bound appearance snapshot.
- **Validation / errors:** Height must be a whole number from 50 to 260 cm (a broad input guard, not an ideal); note at most 60 characters; proportions from supported choices. Other measurements are deferred until a concrete garment use case exists.
- **Acceptance:** All fields may be null. Valid values save/read back; invalid input preserves prior data. No appearance ranking, BMI, ideal weight, or desirability calculation exists.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-002"></a>

#### FR-APPR-002 — Self-declared appearance characteristics

**Priority:** Must. **Status:** Implemented.

The system shall accept optional self-selected skin tone/undertone, palette-suggestion interest, hair color/texture/length, and face shape.

- **Inputs / outputs:** Nullable supported choices and optional yes/no palette interest; saved values and user-readable labels.
- **Validation / errors:** Undertone offers Warm, Cool, Neutral, Unsure; face shape includes Unsure and Prefer not to specify; unsupported codes fail safely.
- **Acceptance:** Users may omit or clear each characteristic. No characteristic is inferred from photographs, gender, ethnicity, or other unrelated information.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-003"></a>

#### FR-APPR-003 — Read and update personal details

**Priority:** Must. **Status:** Implemented.

The system shall let the authenticated owner read and replace their optional appearance details independently of styling preferences and account identity.

- **Inputs / outputs:** GET and versioned complete PUT snapshot; current or empty representation.
- **Validation / errors:** A missing GET inserts nothing; first explicit save creates; stale/missing expected versions conflict. Entire input validates before transactional mutation.
- **Acceptance:** Create/read/update survive refresh; rejected writes make no partial change. Concurrent first creation is safe, and deleted/recreated records cannot accept an old version.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-004"></a>

#### FR-APPR-004 — Clear individual appearance fields

**Priority:** Must. **Status:** Implemented.

The system shall let users set individual appearance fields to null without changing other supplied appearance values or styling preferences.

- **Inputs / outputs:** Complete appearance snapshot with explicit nullable fields; updated snapshot.
- **Validation / errors:** Omitted fields must not silently become clearing requests. Empty sizing text is treated as unanswered; false palette interest differs from null.
- **Acceptance:** Clear/save/refresh leaves the field unanswered while retaining other appearance values. Cancel discards only unsaved local edits.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-005"></a>

#### FR-APPR-005 — Delete appearance information independently

**Priority:** Must. **Status:** Implemented.

The system shall let the owner confirm deletion of their entire appearance profile without deleting their account, styling choices, or onboarding completion.

- **Inputs / outputs:** DELETE with observed version; success without appearance payload.
- **Validation / errors:** Absent deletion is idempotent; an existing record requires the matching version. Uncertain deletion is reconciled by a read, never blindly retried.
- **Acceptance:** After confirmed deletion GET is empty; styling preferences and authentication still work. Stale deletion cannot remove newer details.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-006"></a>

#### FR-APPR-006 — Authenticated appearance ownership and privacy

**Priority:** Must. **Status:** Implemented.

The system shall derive appearance ownership exclusively from the existing authenticated principal and protect all modifying requests with existing CSRF/session controls.

- **Inputs / outputs:** Current session and CSRF header/cookie pair; safe owner-only responses/errors.
- **Validation / errors:** No owner ID in input; safe structured 400/401/403/409/500 responses; exact-origin CORS may add DELETE only as needed.
- **Acceptance:** Two-account tests prove isolation. Appearance values never enter auth responses/principals, logs, URLs, or browser storage. Session/account changes invalidate pending work.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-007"></a>

#### FR-APPR-007 — Optional fifth onboarding step with backward compatibility

**Priority:** Must. **Status:** Implemented.

The system shall offer Personal details and fit between Lifestyle and Review, keeping all details optional and old onboarding records valid.

- **Inputs / outputs:** Existing draft/completed styling snapshot and optional appearance record; five-step UI with resume state.
- **Validation / errors:** Existing REVIEW remains review and COMPLETED remains completed; no mandatory re-onboarding. Skipping the appearance step preserves saved appearance. Separate endpoint saves must disclose partial success.
- **Acceptance:** Users can finish without appearance data. Save/Next/Back/Skip/exit and refresh work. A failed second-resource save retains edits and accurately identifies already-saved progress; no false atomic success.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

<a id="fr-appr-008"></a>

#### FR-APPR-008 — Recommendation integration boundary

**Priority:** Must. **Status:** Implemented.

The system shall store appearance details only as voluntary future personalization inputs during Phase 2E, with no automatic recommendation restriction or external transmission.

- **Inputs / outputs:** Explicit appearance selections; owner-only persistence and presentation.
- **Validation / errors:** No AI/provider calls, image inference, complexion-based color rejection, or appearance ratings.
- **Acceptance:** Source/network tests show only the configured application API receives these fields; no outfit matching or appearance-based rule enforcement is claimed.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

## 4. External interface requirements

### 4.1 User interfaces

The current UI is fashion/editorial: warm neutrals, rose accent, serif headings, sans-serif body text, local photographs, and restrained motion. Landing is standalone. Register has form left/image right; Login and Forgot Password have image left/form right on desktop. Mobile prioritizes the form; the auth image is hidden below its established breakpoint. Large Register/Login motion is suppressed for reduced-motion users.

Current navigation is Landing Get Started → Register, Landing Sign in → Login, Register Sign in → Login, Login Sign up → Register, Login Forgot password? → Forgot Password, and Back to sign in → Login. Successful registration leads to Login; login leads to the minimal Dashboard; confirmed logout returns to Login. These are client-side navigations, not a second authentication mechanism.

Onboarding and profile editing extend the existing warm-neutral/rose styling with native fieldsets, named color controls, progress, review/edit actions, mobile layouts, loading/retry, field errors, conflicts, and session-expiration handling. Future wardrobe, planner, recommendation, and saved-look screens remain unimplemented.

### 4.2 Existing backend HTTP contracts

The following contracts are implemented in [AuthController][controller] and [ProfileController][profileController]. Profile JSON and lifecycle details are documented in the [profile API](../api/styling-profile.md). Bodies are JSON; the logout success body is empty. Request credentials are used only at the HTTPS/HTTP boundary and are not safe response fields.

| Method and path | Request / access | Success | Expected failures |
| --- | --- | --- | --- |
| GET /api/auth/csrf | Public; browser includes cookies | 200: headerName and raw token; matching CSRF cookie | Safe server failure if bootstrap cannot complete |
| POST /api/auth/register | fullName, email, password; public with CSRF | 201: id, fullName, email; no automatic login | 400 invalid JSON/fields; 403 CSRF; 409 duplicate email; safe 500 |
| POST /api/auth/login | email, password; public with CSRF | 200: id, fullName, email; session context saved | 400 fields; generic 401 credentials; 403 CSRF; safe 500 |
| GET /api/auth/me | Authenticated session; no user ID parameter | 200: id, fullName, email from current principal | 401 missing/expired authentication; safe 500 |
| POST /api/auth/logout | Authenticated session and CSRF | 204; context/session/cookies cleared | 401 if already unauthenticated; 403 CSRF; safe server failure |
| GET /api/profile/options | Authenticated session | 200: supported catalog | 401; safe 500 |
| GET /api/profile/me | Authenticated session; principal owner | 200: saved profile or virtual NOT_STARTED; no GET insert | 401; safe 500 |
| PUT /api/profile/me | Authenticated session, CSRF, full versioned snapshot and intent | 200: committed profile | 400 invalid snapshot/state; 401; 403 CSRF; 409 version/creation race; safe 500 |
| GET /api/profile/appearance | Authenticated session; principal owner | 200: appearance or virtual empty snapshot and catalog | 401; safe 500 |
| PUT /api/profile/appearance | Session, CSRF, complete nullable details and expected UUID revision | 200: committed appearance | 400 fields/JSON; 401; 403; 409 stale/creation race; safe 500 |
| DELETE /api/profile/appearance | Session, CSRF; JSON body with observed version | 204, including absent record | 400 JSON; 401; 403; 409 stale existing record; safe 500 |

Other /api/** requests require authentication. Unexpected non-API backend access is denied; error dispatch is deliberately allowed for framework error handling. Unsupported methods on the public register/login paths reach MVC method handling. No JWT, refresh token, authentication cookie readable by JavaScript, or login redirect HTML is part of the API contract.

A safe account response has exactly the currently exposed account fields:

```json
{
  "id": 1,
  "fullName": "Example User",
  "email": "user@example.com"
}
```

The example is illustrative and contains no real account. No password, passwordHash, ORM entity, or session identifier is returned.

The shared error shape is timestamp, numeric status, error, message, path, and fieldErrors. Field errors map field names to concise messages; non-validation errors use an empty map. Repeated constraints on a field are represented by the handler's selected message, not a complete ordered list of every violation.

```json
{
  "timestamp": "2026-10-16T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": {
    "email": "Email must be valid"
  }
}
```

Unknown email and wrong password share “Invalid email or password”. A missing session uses “Authentication is required”; recognized CSRF errors use “Invalid or missing CSRF token”. Registration deliberately exposes a duplicate-email conflict, so the non-enumerating login behavior is not a claim that every account-related operation conceals account existence.

### 4.3 Browser session, CSRF, and CORS interface

Every frontend API request uses credentials: 'include' and no-store caching. The frontend obtains a raw token via GET /api/auth/csrf, retains it in memory, and submits it as X-XSRF-TOKEN. The browser supplies XSRF-TOKEN and the HttpOnly JSESSIONID cookie; frontend code must not read the session cookie.

The backend uses Spring Security's SPA CSRF handling. Login rotates an existing session ID and clears CSRF; logout invalidates the session and clears CSRF/cookies. The frontend discards and refreshes its token after both operations. Registration does not rotate authentication state. Requests are serialized to avoid token-bootstrap/cookie races. Only a recognized CSRF 403 permits one refresh-and-retry; network and general server failures do not cause blind POST replay. A failed post-success token refresh does not undo a confirmed login/logout: the next mutation bootstraps again.

Cookie-based CSRF validates a matching cookie/header pair; it is not a server-revocable token registry. Rotation does not invalidate an old matching pair deliberately restored by a client. This is an existing mechanism characteristic, not a new single-use-token guarantee.

Development accepts exactly http://localhost:3000, credentialed GET/POST/PUT/DELETE/OPTIONS, and Content-Type/X-XSRF-TOKEN. localhost:3000 and localhost:8080 are different origins but the same site; substituting 127.0.0.1 or port 3001 does not match the configured origin. Production requires one exact FRONTEND_ORIGIN and Secure cookies with the prod profile. No wildcard or arbitrary reflected origins are allowed.

SameSite=Lax assumes same-site hosting; truly cross-site production topology is unresolved. CORS alone cannot change SameSite behavior. Next.js server-rendered data or Server Actions would require explicit server-side session verification; the current client guard is insufficient for them.

### 4.4 Database interface

[UserRepository][repository] uses Spring Data JPA against Neon PostgreSQL. Server environment variables DB_URL, DB_USERNAME, and DB_PASSWORD configure the connection; their values are intentionally absent from this document. Hibernate development schema management is currently update. No manual SQL migration strategy or production migration tool is established by this specification.

Application tables are app_users plus styling_profiles, styling_profile_answers, styling_profile_choices, and appearance_profiles. The profile shares its owner primary key/foreign key, has @Version, status/step/timestamps, and answer/choice collections preserving null versus explicit empty. No existing account constraint is changed. Future conceptual entities are not finalized DDL.

### 4.5 Proposed external services

| Interface | Intended responsibility | Unresolved contract / fallback |
| --- | --- | --- |
| Weather | Obtain conditions relevant to confirmed place/time | Provider, fields, units, horizon, quotas, staleness, and manual fallback: DEC-007 |
| Location / geocoding | Resolve a user-selected place if necessary | Precision, permissions, time zones, and manual alternative: DEC-008 |
| Media storage | Store/access owned wardrobe images | File validation, limits, ownership, retention, and hosting: DEC-005 |
| AI explanations | Improve wording grounded in rule evidence | Provider/model, minimized payload, consent, grounding, budget, fallback: DEC-011 |
| Optional item recognition | Suggest item attributes for user confirmation | Scope/provider/uncertainty/privacy: DEC-006 |
| Email / notifications | Future recovery or event-related delivery | Not implemented; delivery, abuse controls, and release scope: DEC-015 / DEC-016 |

No external provider is selected here. Local AI-generated development photographs do not constitute runtime AI inference, training data, or an image-recognition integration.

## 5. Non-functional requirements

There are 26 quality/engineering requirements. Implemented statuses below are deliberately limited to existing authentication behavior. App-wide accessibility, future ownership rules, performance targets, and operational guarantees remain work to verify.

### 5.1 Security



<a id="nfr-sec-001"></a>

#### NFR-SEC-001 — Credential handling

**Priority:** Must. **Status:** Implemented.

The authentication implementation shall hash stored passwords with its configured BCrypt encoder, reject passwords over 72 UTF-8 bytes, preserve submitted password contents, and omit credentials from HTTP responses and request string representations.

- **Acceptance / verification:** DTO/service/controller tests cover Unicode and ASCII boundaries, password matching, redacted strings, and safe id/fullName/email responses. Database password_hash contains an encoded value, not the raw request.
- **Existing evidence:** [RegisterRequest][regdto], [LoginRequest][logindto], [UTF-8 validator][byteval], [Password encoder][encoder], [AuthService][service], [AccountPrincipal][principal], [AuthController][controller]; tests: [RegisterRequestTests][tReg], [LoginRequestTests][tLogin], [AuthServiceTests][tService], [PasswordAuthenticationTests][tPassword], [AuthControllerTests][tController], [SessionAuthenticationTests][tSession].

<a id="nfr-sec-002"></a>

#### NFR-SEC-002 — Server session lifecycle

**Priority:** Must. **Status:** Implemented.

The system shall rotate an existing session identifier on login, explicitly persist the security context, erase authentication credentials before persistence, and invalidate the session on successful logout.

- **Acceptance / verification:** Servlet/security tests verify identifier change without publishing the identifier, authenticated /me on a later request, no retained hash/password, and rejection of the logged-out session. Current idle timeout is 30 minutes.
- **Existing evidence:** [AuthController][controller], [SecurityConfig][security], [AccountPrincipal][principal], [Development configuration][props]; tests: [SessionAuthenticationTests][tSession], [SessionCookieTests][tCookie].

<a id="nfr-sec-003"></a>

#### NFR-SEC-003 — CSRF and origin controls

**Priority:** Must. **Status:** Implemented.

The session API shall require CSRF validation for state-changing requests and restrict credentialed browser CORS to the configured exact frontend origin.

- **Acceptance / verification:** Missing or invalid CSRF returns JSON 403; valid cookie/header pairs pass. Allowed-origin preflight succeeds; a different origin is rejected. No wildcard credentialed origin is configured.
- **Existing evidence:** [SecurityConfig][security], [AuthController][controller], [Typed Fetch client][client]; tests: [SessionAuthenticationTests][tSession], [SessionCookieTests][tCookie], [Fetch client tests][tClient].

<a id="nfr-sec-004"></a>

#### NFR-SEC-004 — Session cookie transport policy

**Priority:** Must. **Status:** Implemented.

The system shall use host-scoped, HttpOnly, SameSite=Lax session cookies, cookie-only session tracking, and Secure cookies under the production profile; the readable CSRF cookie shall not replace the session cookie.

- **Acceptance / verification:** Cookie tests inspect response attributes without logging cookie values. Local HTTP remains usable with Secure=false; production configuration forces Secure=true and requires an explicit frontend origin.
- **Existing evidence:** [SecurityConfig][security], [Development configuration][props], [Production configuration][prodprops]; tests: [SessionCookieTests][tCookie].

<a id="nfr-sec-005"></a>

#### NFR-SEC-005 — Resource ownership authorization

**Priority:** Must. **Status:** Planned.

Every future wardrobe, profile, plan, saved-outfit, and feedback API shall authorize access against the authenticated owner on the server.

- **Acceptance / verification:** For each implemented resource API, two-account tests attempt identifier substitution on read/update/delete and prove no cross-account data or mutation. A client route guard alone cannot satisfy this requirement.
- **Existing evidence:** [SecurityConfig][security]; no dedicated test establishes the pending operational guarantee.

<a id="nfr-sec-006"></a>

#### NFR-SEC-006 — Safe errors and diagnostics

**Priority:** Must. **Status:** Implemented.

Authentication errors and application logs shall not expose raw passwords, hashes, session identifiers, CSRF tokens, SQL details, or stack traces to clients; unknown-account and wrong-password login failures shall use the same public message.

- **Acceptance / verification:** Controller/security tests verify safe 400/401/403/409/500 bodies and generic invalid-credentials text; selected logging tests cover submitted secrets. This does not establish constant-time behavior or a comprehensive production log audit.
- **Existing evidence:** [MVC exception handler][errors], [Security error handler][apierrors], [RegisterRequest][regdto], [LoginRequest][logindto], [Development configuration][props]; tests: [AuthControllerTests][tController], [SessionAuthenticationTests][tSession], [PasswordAuthenticationTests][tPassword].

### 5.2 Privacy

<a id="nfr-priv-001"></a>

#### NFR-PRIV-001 — Data minimization and personal choices

**Priority:** Must. **Status:** Planned.

The system shall collect styling preferences as explicit user choices and collect location and personal information only to the extent required by a documented feature.

- **Acceptance / verification:** Review forms and payloads to confirm purpose and optionality; a user can correct preferences. No unselected sensitive trait or preference is inferred from gender, transport, or an occasion label.
- **Open decisions:** DEC-002, DEC-008, DEC-014.

<a id="nfr-priv-002"></a>

#### NFR-PRIV-002 — Retention and deletion policy

**Priority:** Must. **Status:** Requires clarification.

Before storing wardrobe media or recommendation history, the system shall document retention, deletion, backup treatment, and any external-provider retention.

- **Acceptance / verification:** An approved policy identifies each data category and its handling; deletion tests demonstrate the approved behavior. No legal compliance claim is implied before jurisdiction and policy review.
- **Open decisions:** DEC-005, DEC-011, DEC-014.

<a id="nfr-priv-003"></a>

#### NFR-PRIV-003 — No browser credential persistence

**Priority:** Must. **Status:** Implemented.

The frontend shall keep identity and CSRF application state in memory and shall not write authentication credentials or CSRF/session tokens to localStorage, sessionStorage, URLs, or application logs.

- **Acceptance / verification:** Source review confirms storage/URL/logging absence; a browser refresh restores identity from /me, and sign-out clears application identity. The browser-managed HttpOnly session cookie is the intended exception to client-memory state.
- **Existing evidence:** [Typed Fetch client][client], [SessionProvider][provider], [Register page][register], [Login page][login]; tests: [Fetch client tests][tClient], [Frontend auth browser tests][tBrowser].

<a id="nfr-priv-004"></a>

#### NFR-PRIV-004 — Appearance data minimization

**Priority:** Must. **Status:** Implemented.

Appearance collection shall exclude age, birth date, weight, images, inferred traits, and additional measurements without an approved functional use case. All supported fields shall be optional, clearable, and independently deletable, with no AI/external transmission in Phase 2E.

- **Acceptance / verification:** Inspect DTO/schema/UI/network boundaries; reject unknown fields; test null, deletion, two-account access, auth response exclusion, and log redaction. Future retention/backup/legal policies remain under DEC-014; no legal compliance claim is made.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

### 5.3 Reliability

<a id="nfr-rel-001"></a>

#### NFR-REL-001 — Async authentication consistency

**Priority:** Must. **Status:** Implemented.

The frontend shall prevent stale session responses, duplicate submissions, canceled form requests, and uncertain network outcomes from incorrectly overriding newer authentication state.

- **Acceptance / verification:** Tests delay /me across login/logout, cancel form navigation, and simulate a lost logout response; canceled submissions do not redirect, and uncertainty is reconciled rather than reported as confirmed sign-out.
- **Existing evidence:** [Typed Fetch client][client], [SessionProvider][provider], [Register page][register], [Login page][login]; tests: [Fetch client tests][tClient], [Frontend auth browser tests][tBrowser].

<a id="nfr-rel-002"></a>

#### NFR-REL-002 — Atomic registration and email integrity

**Priority:** Must. **Status:** Implemented.

Registration shall use a transactional persistence operation and retain the database unique-email constraint, translating only the known email-uniqueness race into a conflict.

- **Acceptance / verification:** Service tests cover pre-check duplicates, known PostgreSQL email uniqueness violations, and unrelated constraints; failures do not produce HTTP 201.
- **Existing evidence:** [AuthService][service], [User entity][entity], [UserRepository][repository]; tests: [AuthServiceTests][tService], [AuthControllerTests][tController].

<a id="nfr-rel-003"></a>

#### NFR-REL-003 — Recommendation dependency fallback

**Priority:** Must. **Status:** Planned.

The recommendation flow shall disclose unavailable external data and retain a documented deterministic/manual fallback where permitted by approved rules.

- **Acceptance / verification:** Simulated weather/AI timeouts and malformed responses never become fabricated conditions or facts; users see the supported fallback or an actionable inability to proceed.
- **Open decisions:** DEC-007, DEC-011.

### 5.4 Performance

<a id="nfr-perf-001"></a>

#### NFR-PERF-001 — Bounded frontend authentication waiting

**Priority:** Must. **Status:** Implemented.

Authentication fetch operations shall terminate through the existing 15-second client timeout and show actionable timeout/network states without automatically replaying arbitrary failed POSTs.

- **Acceptance / verification:** Inspect timeout/abort logic and exercise network-failure UI plus bounded CSRF retry tests. This is a configured client behavior, not a 15-second server response guarantee.
- **Existing evidence:** [Typed Fetch client][client], [SessionProvider][provider]; tests: [Fetch client tests][tClient], [Frontend auth browser tests][tBrowser].

<a id="nfr-perf-002"></a>

#### NFR-PERF-002 — Recommendation performance budget

**Priority:** Should. **Status:** Requires clarification.

Before recommendation release, the project shall define representative wardrobe sizes, concurrent load, measurement environment, percentile latency, and acceptable resource use, then verify the agreed budget.

- **Acceptance / verification:** Record a reproducible dataset and load procedure and compare measurements with an approved target; no numeric latency or throughput guarantee is established in this baseline.
- **Open decisions:** DEC-010, DEC-018.

### 5.5 Accessibility

<a id="nfr-acc-001"></a>

#### NFR-ACC-001 — Keyboard and form accessibility

**Priority:** Must. **Status:** Planned.

All user-facing flows shall support keyboard operation, visible focus, meaningful headings and labels, associated field errors, and accessible loading/status messages.

- **Acceptance / verification:** Perform keyboard-only tests and automated/manual accessibility review of each new flow, including error and loading states. Existing auth labels/focus tests are supporting evidence, not proof of full application conformance.
- **Existing evidence:** [AuthField][field], [Register page][register], [Login page][login], [Forgot Password page][forgot], [Minimal dashboard][dashboard]; tests: [Frontend auth browser tests][tBrowser].
- **Open decisions:** DEC-017.

<a id="nfr-acc-002"></a>

#### NFR-ACC-002 — Motion and assistive presentation

**Priority:** Must. **Status:** Planned.

All animated flows shall respect prefers-reduced-motion; meaningful images shall have contextual alternatives and text/control contrast shall meet the accessibility level selected for the release.

- **Acceptance / verification:** Reduced-motion tests suppress the large auth slide; inspect image alternatives and measure contrast. Proposed WCAG 2.2 AA scope and full screen-reader/browser matrix require confirmation.
- **Existing evidence:** [AuthShell][shell], [Auth motion CSS][motion], [Landing hero][hero], [Styling categories][categories]; tests: [Frontend auth browser tests][tBrowser].
- **Open decisions:** DEC-017.

<a id="nfr-acc-003"></a>

#### NFR-ACC-003 — Accessible optional appearance controls

**Priority:** Must. **Status:** Implemented.

Personal Details & Fit controls shall use native labels, groups, clear optional units/choices, keyboard-accessible actions, visible focus, field-linked validation, and responsive presentation. Missing data must not be presented as a deficiency.

- **Acceptance / verification:** Keyboard save/clear/delete confirmation; invalid-height focus/error association; mobile 360/390 and tablet/desktop overflow checks; five-step progress and review are understandable without motion or color alone. Full assistive-technology/browser conformance remains a separate acceptance decision.
- **Evidence:** [Appearance API](../api/styling-profile.md#appearance-profile), [source/test traceability](requirements-traceability.md), and [executed local verification](../testing/phase-2-verification.md#phase-2e-results).

### 5.6 Usability and visual consistency

<a id="nfr-ux-001"></a>

#### NFR-UX-001 — Responsive visual consistency

**Priority:** Must. **Status:** Planned.

The application shall preserve the approved warm-neutral/rose, serif-heading and sans-serif-body design language and adapt each new screen without forced desktop layouts or horizontal content overflow.

- **Acceptance / verification:** Review 360, 390, 768, 1024, and 1440 CSS-pixel widths with long content and error states; inputs and navigation remain usable. Existing landing/auth browser tests support the baseline; future modules need equivalent checks.
- **Existing evidence:** [Global theme][stylecss], [AuthShell][shell], [Auth motion CSS][motion], [Landing page][landing], [Styling categories][categories]; tests: [Frontend auth browser tests][tBrowser].

<a id="nfr-ux-002"></a>

#### NFR-UX-002 — Honest loading and unavailable states

**Priority:** Must. **Status:** Planned.

Each flow shall distinguish loading, empty results, validation failures, authorization loss, network failure, and unavailable future features, without claiming completion before confirmation.

- **Acceptance / verification:** Exercise each applicable state; button pending behavior blocks duplicate operations. Forgot Password and social authentication remain clearly unavailable until implemented.
- **Existing evidence:** [SessionProvider][provider], [RequireAuth][guard], [Forgot Password page][forgot], [Social buttons][social], [Register page][register], [Login page][login]; tests: [Frontend auth browser tests][tBrowser].

### 5.7 Maintainability

<a id="nfr-mnt-001"></a>

#### NFR-MNT-001 — Separation of responsibilities

**Priority:** Must. **Status:** Planned.

New functionality shall preserve feature-based backend packages, HTTP DTO boundaries, service-owned business rules, repository persistence, and a shared typed frontend API boundary.

- **Acceptance / verification:** Code review verifies controllers do not query repositories directly, entities are not returned as public account responses, and recommendation rules are separable from provider-specific code. Current auth architecture is the example, not a claim about unbuilt modules.
- **Existing evidence:** [AuthController][controller], [AuthService][service], [UserRepository][repository], [Typed Fetch client][client], [Frontend dependencies and scripts][pkg], [Backend dependencies][pom]; tests: [AuthServiceTests][tService], [AuthControllerTests][tController], [Fetch client tests][tClient].

<a id="nfr-mnt-002"></a>

#### NFR-MNT-002 — Traceable requirement changes

**Priority:** Must. **Status:** Planned.

Each implemented or changed requirement shall retain its stable identifier and update its implementation evidence, verification status, and affected decisions in the traceability matrix.

- **Acceptance / verification:** A review can follow every requirement to planned acceptance work or real source/test evidence; obsolete IDs are retired with a note rather than reused. This first baseline establishes the process, not historical compliance.

### 5.8 Compatibility

<a id="nfr-comp-001"></a>

#### NFR-COMP-001 — Supported browser and deployment compatibility

**Priority:** Should. **Status:** Requires clarification.

The project shall define its supported browser/version matrix and verify credentialed sessions in the chosen same-site HTTP development and HTTPS production topology.

- **Acceptance / verification:** Run target-browser cookie/CORS checks and responsive flows; Chromium mocked tests alone do not establish Safari/Firefox or a live HTTPS deployment. Cross-site hosting requires a separately approved cookie design.
- **Existing evidence:** [Playwright configuration][testconfig], [Development configuration][props], [Production configuration][prodprops], [SecurityConfig][security]; tests: [SessionCookieTests][tCookie], [Frontend auth browser tests][tBrowser].
- **Open decisions:** DEC-017, DEC-018.

### 5.9 Testability

<a id="nfr-test-001"></a>

#### NFR-TEST-001 — Layered reproducible verification

**Priority:** Must. **Status:** Planned.

Every delivered feature shall have tests at the appropriate rule, service, HTTP, and browser levels, with external providers isolated in deterministic tests and a separate live integration procedure.

- **Acceptance / verification:** Frontend lint/typecheck/build and relevant Playwright checks, backend Maven tests, and documented manual integration results are recorded before accepting a feature; mocked tests are never labeled as Neon/live-provider verification.
- **Existing evidence:** [Playwright configuration][testconfig], [Frontend integration guide][webdoc], [Backend session guide][apidoc]; tests: [RegisterRequestTests][tReg], [AuthServiceTests][tService], [AuthControllerTests][tController], [SessionAuthenticationTests][tSession], [Frontend auth browser tests][tBrowser], [Fetch client tests][tClient], [Full application context test][tContext].

### 5.10 Scalability

<a id="nfr-scale-001"></a>

#### NFR-SCALE-001 — Session deployment boundary

**Priority:** Must. **Status:** Requires clarification.

The system shall not claim multi-instance session continuity until a shared-session or routing strategy is selected and verified.

- **Acceptance / verification:** Document that current sessions are instance-local and lost on restart; before horizontal scaling, demonstrate the chosen behavior across instances and restarts. No shared store is currently configured.
- **Existing evidence:** [SecurityConfig][security], [Development configuration][props], [Backend session guide][apidoc]; no dedicated test establishes the pending operational guarantee.
- **Open decisions:** DEC-018.

<a id="nfr-scale-002"></a>

#### NFR-SCALE-002 — Bounded collection processing

**Priority:** Should. **Status:** Planned.

Before wardrobe and history collections are released, the system shall define and enforce bounded retrieval and recommendation-candidate processing suitable for the agreed workload.

- **Acceptance / verification:** Tests exercise agreed page/query bounds and avoid unbounded cross-user scans; thresholds and pagination contracts remain TBD rather than invented guarantees.
- **Open decisions:** DEC-010, DEC-018.

## 6. Data requirements

**Implemented Phase 2E extension:** `appearance_profiles` references `app_users` directly with a shared owner primary key/foreign key. Registration and GET create no record. Ten nullable fields support full replacement, transactional validation, versioned independent deletion, and optimistic concurrency. An internal JPA lock version and fresh public UUID revision per save prevent lost updates and stale delete/recreate matches. The UUID is a concurrency tag, not an authentication token. Styling preferences and account identity remain separate. Hibernate update adds the table and a non-null `appearance_step_pending` column defaulting to false on styling profiles; existing step constraints/data remain unchanged. No destructive schema operation was performed.


### 6.1 Existing persistent model

The existing account entity is [User entity][entity]. Phase 2 additionally implements [StylingProfile][profileEntity] with the owner-bound answer/choice model described below. The following summarizes its mapping rather than redefining the production schema.

| Java field | Mapped column | Existing constraint / behavior |
| --- | --- | --- |
| id: Long | id | Primary key; database identity generation |
| fullName: String | full_name | Required; column length 150 |
| email: String | email | Required; column length 254; unique |
| passwordHash: String | password_hash | Required; length 255 accommodates encoded hashes |
| createdAt: Instant | created_at | Required; Hibernate creation timestamp; not updated |
| updatedAt: Instant | updated_at | Required; Hibernate update timestamp |

The table name app_users avoids using user as a table identifier. Registration normalization and validation occur in request/service code; the database retains its unique-email constraint even with an application pre-check. Timestamps represent instants, not event-local time preferences.

The incoming password is a transient request value; it is not a User field. Account responses expose id/fullName/email only. The session principal holds a login-time identity snapshot; its temporary hash is erased before session persistence. The current /me does not query a client-selected account or automatically refresh identity changes.

### 6.2 Proposed conceptual model

The implemented profile relationship is identified explicitly; all other rows are future conceptual relationships, not finalized schemas or migration scripts. See the [profile data contract](../api/styling-profile.md#persistence-and-concurrency) for the implemented tables.

| Concept | Relationship / purpose | Decisions still needed |
| --- | --- | --- |
| Appearance profile (implemented) | Account owns zero or one independently deletable profile; all fields nullable; no GET insert; owner-derived CRUD | Future consumers, retention and wider measurement use cases remain separate decisions |
| Styling profile (implemented) | Account owns zero or one profile; shared key and answer/choice tables; versioned full replacement | Phase 2 contract approved; recommendation consumers/precedence remain planned |
| Wardrobe item | Account owns many item records | Attributes, availability, removal semantics: DEC-004 |
| Item media | Owned image associated with item | Number of images, storage, metadata and retention: DEC-005 |
| Occasion / request context | Account supplies place, time, activity, transport, weather, and request choices | Persisted versus transient request data, location precision/time zone: DEC-007 / DEC-008 / DEC-014 |
| Weather context | External/manual conditions attributed to a request and time | Fields, freshness, source and failure disclosure: DEC-007 |
| Recommendation | Context/rule evaluation produces candidates referencing owned items | Persistence, rule version, result count, composition: DEC-009 / DEC-010 |
| Explanation evidence | Candidate links to evaluated facts/rules; optional derived AI wording | Storage, grounding, provider data policy: DEC-011 |
| Saved outfit | Account saves identifiable candidate components | Snapshot versus live references and item changes: DEC-009 |
| Event plan | Account optionally associates an event with a saved outfit | Scheduling, availability/reservation and later calendar scope: DEC-016 |
| Feedback | Account records an approved reaction to its recommendation | Vocabulary, edit policy, influence: DEC-012 |
| Wear/history record | Optional explicit wear report; distinct from recommendation/save history | Scope, correction, retention: DEC-014 / DEC-016 |
| Collection | Optional owner-defined grouping of saved looks | Membership and release scope: DEC-016 |

An account is the ownership root. A recommendation must never reference another account's wardrobe item. Deleting an item/account can affect media, saved references, feedback, and history; cascade, anonymization, and retention behavior need explicit policy rather than assumed database cascades. Event time must retain an unambiguous zone interpretation if planning is persisted.

### 6.3 Data integrity and lifecycle

Profile writes validate the full snapshot before transactional mutation and flush, using optimistic version checks and narrow creation-race handling. Rejected snapshots do not partially replace collections. Future write operations must retain equivalent ownership/integrity safeguards. Unanswered preferences must remain distinct from an explicit “no preference” choice where that distinction affects rules. Changing profile defaults must not silently rewrite saved historical outfits.

No finalized wardrobe taxonomy, database schema for planned modules, personal-data retention period, legal basis, or export format is asserted. These decisions block acceptance where referenced. Test data must be synthetic or a deliberately authorized development account; production wardrobe images and real credentials must not become test fixtures.

## 7. Business rules and constraints

### 7.1 Established rules in the current code

| Rule | Existing behavior / related requirements |
| --- | --- |
| BR-001 | An email identifies an account uniquely after service trim/lowercase normalization with Locale.ROOT; the database constraint remains authoritative. FR-AUTH-002 / FR-AUTH-004 |
| BR-002 | Registration requires a nonblank password of at least 8 UTF-16 string units and at most 72 UTF-8 bytes. Login enforces nonblank and the byte maximum without imposing registration's minimum again. No trimming, normalization, silent truncation, or complexity regex. FR-AUTH-003 / FR-AUTH-006 |
| BR-003 | Registration creates an account but does not automatically sign in. Successful login creates/persists a Spring session and rotates an existing session identifier. FR-AUTH-002 / FR-AUTH-005 |
| BR-004 | Current account identity comes from the authenticated principal; safe responses exclude credentials. /me reflects the login-time identity snapshot. FR-AUTH-011 |
| BR-005 | Private client navigation waits for session verification; Spring authorizes API requests independently. FR-AUTH-010 |
| BR-006 | Recovery and Google/Apple actions are unavailable; no fake success or substitute local authentication is allowed. FR-AUTH-012 |
| BR-007 | All profile answers are optional; null differs from []; explicit completion only; skipping never blocks Dashboard. FR-ONB-001 / FR-ONB-002 |
| BR-008 | Only explicit coverage may be REQUIRED; other groups/disliked colors remain preferences; no personal inference or current recommendation enforcement. FR-ONB-003 / FR-ONB-005 |
| BR-009 | Full snapshot and expected version required; first explicit save creates; stale updates conflict; GET does not insert; completedAt is retained. FR-ONB-002 / FR-ONB-004 |

The string-unit minimum is a statement of the actual Java/JavaScript validation semantics, not a new Unicode password policy. Any later policy change must consider existing accounts and have explicit acceptance tests.

### 7.2 Proposed recommendation rules

| Rule | Proposed behavior | Pending decisions |
| --- | --- | --- |
| BR-101 | Use only owned, eligible, available items for wardrobe-based results | Availability states: DEC-004 |
| BR-102 | Apply explicit hard restrictions before ranking; never silently relax them | Hard/soft classification and conflict resolution: DEC-003 |
| BR-103 | Compose complete looks under approved required/optional slot rules | Categories, layers, missing slots: DEC-004 / DEC-009 |
| BR-104 | Evaluate weather/time/activity/transport using documented attributes and rules | Weather contract and suitability cutoffs: DEC-007 / DEC-010 |
| BR-105 | Use deterministic ranking and tie-breaking; disclose effective inputs and relevant reasons | Weights, priorities, number of results: DEC-010 |
| BR-106 | Treat comfort/modesty/style as self-declared; transport is context, not a proxy for identity or preference | Approved catalog and semantics: DEC-003 / DEC-004 |
| BR-107 | Return an explained no-match when hard constraints cannot be satisfied | Alternative suggestions and missing-slot handling: DEC-003 / DEC-009 |
| BR-108 | AI may improve explanations but cannot invent owned items or override eligibility | Grounding, payload and provider controls: DEC-011 |
| BR-109 | Feedback may influence later ranking only through approved rules; a recommendation is not proof of wearing | Adaptation and history: DEC-012 / DEC-016 |
| BR-110 | Changes/deletion must have explicit saved-look/history semantics; no implicit clothing reservation | Reference and retention policy: DEC-009 / DEC-014 |

No fixed temperature thresholds, material suitability scores, modesty classifications, recommendation weights, garment color-matching matrix, or provider choices are confirmed. They must become documented and testable decisions before implementation, not inferred from the example scenario.

## 8. Use cases

The following 13 use cases illustrate requirement interactions. A planned use case is not executable functionality in the current build. Steps become acceptance scenarios only after their linked decisions and selected release scope are approved.

<a id="uc-001"></a>

### UC-001 — Register a new account

**Actor:** Visitor. **Status:** Implemented.

**Preconditions:** Registration UI and API are available; the visitor is creating an account rather than requesting automatic login.

**Main success flow:**

1. Open /register from the landing page or Login.
2. Enter a full name, email, and unchanged raw password.
3. Frontend validates and obtains CSRF when needed; submit the credentialed registration request.
4. Backend validates, normalizes name/email, checks uniqueness, hashes the password, and persists the account.
5. Receive 201 with safe account fields; transition to Login with the creation notice.

**Alternative / error flows:**

- Invalid input: show field errors and do not claim creation.
- Duplicate normalized email: show safe 409 message.
- Network/timeout uncertainty: show failure guidance without blindly replaying POST; do not pretend registration logged the user in.

**Postconditions:** One account exists on confirmed success; no new authenticated session is established.

**Related requirements:** FR-AUTH-002, FR-AUTH-003, FR-AUTH-004, FR-AUTH-007.

<a id="uc-002"></a>

### UC-002 — Sign in and enter the account screen

**Actor:** Registered user. **Status:** Implemented.

**Preconditions:** Account exists; Login is available.

**Main success flow:**

1. Open /login and enter email and password.
2. Validate without changing password contents and submit with CSRF and cookies.
3. Spring verifies BCrypt, rotates an existing session ID, and saves the security context.
4. Frontend refreshes CSRF and navigates to /dashboard.
5. Display the safe authenticated account identity.

**Alternative / error flows:**

- Unknown email or wrong password: same generic 401 failure.
- Invalid field/over-limit password: 400, with useful field feedback.
- Canceled or uncertain request: suppress stale navigation and reconcile /me as applicable.

**Postconditions:** Confirmed authentication is retained across subsequent requests; no token is returned as a second auth system.

**Related requirements:** FR-AUTH-005, FR-AUTH-006, FR-AUTH-007, FR-AUTH-011.

<a id="uc-003"></a>

### UC-003 — Restore a session or recover from expiration

**Actor:** Returning user. **Status:** Implemented.

**Preconditions:** Browser opens or refreshes a private route; backend may hold a valid session.

**Main success flow:**

1. Show the session-check loading state without flashing protected account content.
2. Fetch /api/auth/me with credentials.
3. On 200, display safe account details.
4. Recheck on private entry, visibility restoration, or focus as implemented.

**Alternative / error flows:**

- 401: clear identity and redirect protected navigation to Login.
- Network failure: show retry state, distinct from confirmed sign-out.
- Older in-flight /me: discard its result when a newer authentication generation supersedes it.

**Postconditions:** Private UI reflects the latest confirmed session result; Spring remains the authorization boundary.

**Related requirements:** FR-AUTH-008, FR-AUTH-010, NFR-REL-001.

<a id="uc-004"></a>

### UC-004 — Sign out

**Actor:** Authenticated user. **Status:** Implemented.

**Preconditions:** User is on the minimal dashboard or another future authenticated UI.

**Main success flow:**

1. Select Sign out.
2. Submit a credentialed logout POST with a current CSRF token.
3. Backend clears context, invalidates the session, and expires cookies.
4. On 204, clear application identity, refresh CSRF, and return to Login.

**Alternative / error flows:**

- Expired session/401: treat as already unauthenticated.
- Recognized CSRF failure: refresh and retry once.
- Network failure: do not assert success; recheck /me and retain an actionable state.

**Postconditions:** On confirmed success, the former session cannot access /me.

**Related requirements:** FR-AUTH-007, FR-AUTH-009, NFR-SEC-002.

<a id="uc-005"></a>

### UC-005 — Recognize unavailable recovery or social sign-in

**Actor:** Visitor. **Status:** Implemented limitation.

**Preconditions:** Visitor views Login, Register, or Forgot Password.

**Main success flow:**

1. Follow Forgot password? to /forgot-password when needed.
2. Read the supporting recovery text and explicit unavailable notice.
3. Return to Login with Back to sign in.

**Alternative / error flows:**

- Google/Apple actions remain disabled and do not invoke an external flow.
- No recovery email is sent and no email-sent confirmation is fabricated.

**Postconditions:** Visitor understands that recovery/social authentication is not implemented.

**Related requirements:** FR-AUTH-012, FR-ACCT-005.

<a id="uc-006"></a>

### UC-006 — Create or revise a styling profile

**Actor:** Authenticated user. **Status:** Implemented for profile capture/editing; recommendation use remains planned.

**Preconditions:** Verified session; supported catalog loaded; no profile is also valid.

**Main success flow:**

1. Enter setup from Dashboard (Start or Resume), or open Edit preferences.
2. Choose optional style/colors, comfort/coverage, and lifestyle values; distinguish unanswered from No preference. Optionally fill Personal details and fit, or skip it without changing saved appearance.
3. Next saves before advancing; review and edit steps as needed.
4. Explicitly finish, save and exit, or save editor changes; refresh restores the committed snapshot.

**Alternative / error flows:**

- Skip retains choices and leaves Dashboard accessible; Back preserves local unsaved changes.
- Invalid fields preserve edits and focus relevant errors; missing session restores/signs in.
- Conflict retains draft and requires explicit reload; uncertain delivery uses GET reconciliation without PUT replay.
- Another account cannot select this owner in the API; logout/account change discards account-specific UI state.
- Appearance can be cleared field by field or independently deleted after confirmation, keeping account/styling choices/completion. Old Review/completed records remain valid without repeating the optional step.
- When appearance saves but styling progress fails, disclose partial success and recover without replaying the confirmed appearance save. The resources are not atomic together.

**Postconditions:** Each resource stores only confirmed owner changes in its own transaction. Future recommendation request defaults are not yet available.

**Related requirements:** FR-ONB-001, FR-ONB-002, FR-ONB-004, FR-APPR-001 through FR-APPR-008; future FR-ONB-003, FR-ONB-005, FR-ONB-006.

<a id="uc-007"></a>

### UC-007 — Manage the digital wardrobe

**Actor:** Authenticated item owner. **Status:** Planned; decisions pending.

**Preconditions:** Wardrobe feature and item taxonomy are delivered.

**Main success flow:**

1. Add a manually described owned item and an optional permitted image.
2. Review the item in the wardrobe and search/filter as needed.
3. Edit attributes or mark availability according to approved states.
4. Confirm removal of an item when appropriate.

**Alternative / error flows:**

- Invalid item/image: show the applicable error without partial success.
- AI classification, if approved, requires review and leaves manual entry available.
- Removal respects the agreed saved-reference/media policy; substituted foreign IDs are denied.

**Postconditions:** The owner's active wardrobe accurately reflects confirmed records and availability.

**Related requirements:** FR-WARD-001, FR-WARD-002, FR-WARD-003, FR-WARD-004, FR-WARD-005, FR-WARD-006, FR-WARD-007, FR-WARD-008.

<a id="uc-008"></a>

### UC-008 — Request a practical context-aware outfit

**Actor:** Authenticated user. **Status:** Planned; decisions pending.

**Preconditions:** A usable owned wardrobe exists; profile/default and recommendation rules have been approved.

**Main success flow:**

1. Choose an outing with friends, event location/time, and applicable weather.
2. Specify public transport and walking plus explicit comfort and modesty choices.
3. Review the effective request without silently changing saved preferences.
4. Filter owned available items by hard restrictions; compose and deterministically rank eligible looks.
5. Inspect a selected candidate and the reasons tied to actual inputs; request an alternative if available.

**Alternative / error flows:**

- Weather unavailable: disclose and follow the approved manual/fallback path.
- Missing clothing or conflicting constraints: explain no-match and let the user revise inputs.
- AI unavailable: retain deterministic results and rule-based explanations.

**Postconditions:** A recommendation uses only eligible owned items and respects confirmed constraints; suitability cutoffs are not assumed.

**Related requirements:** FR-PLAN-001, FR-PLAN-002, FR-PLAN-003, FR-PLAN-004, FR-PLAN-005, FR-REC-001, FR-REC-002, FR-REC-003, FR-REC-004, FR-REC-005, FR-REC-006, FR-REC-007, FR-REC-008, FR-REC-009.

<a id="uc-009"></a>

### UC-009 — Read an optionally AI-assisted explanation

**Actor:** Authenticated user. **Status:** Planned; decisions pending.

**Preconditions:** A deterministic candidate and its rule evidence exist; provider use and data policy are approved.

**Main success flow:**

1. Present the baseline grounded explanation.
2. If enabled and permitted, send minimized facts to the selected AI service.
3. Validate the returned wording and display it only if grounded.

**Alternative / error flows:**

- Provider disabled, timeout, or invalid output: show baseline reasons.
- Unsupported item/weather claims or restriction violations: reject the assisted explanation.
- Conversation and wardrobe classification are separate optional capabilities.

**Postconditions:** The explanation cannot change the selected items or overrule hard restrictions.

**Related requirements:** FR-AI-001, FR-AI-002, FR-AI-003, FR-AI-004.

<a id="uc-010"></a>

### UC-010 — Save and review an outfit

**Actor:** Authenticated user. **Status:** Planned; decisions pending.

**Preconditions:** A candidate exists and the saved-reference policy is approved.

**Main success flow:**

1. Save the selected outfit.
2. Open the owner's saved list after refresh.
3. Review its components or remove the saved look.
4. Organize into a collection only if that later capability is delivered.

**Alternative / error flows:**

- Item later changed/deleted: follow the explicit reference policy.
- Unauthorized saved-look access is denied.
- Deleting a saved look/collection does not delete wardrobe items.

**Postconditions:** The saved look is persisted only for its owner; missing references are disclosed.

**Related requirements:** FR-SAVE-001, FR-SAVE-002, FR-SAVE-006.

<a id="uc-011"></a>

### UC-011 — Give feedback and review personalization

**Actor:** Authenticated user. **Status:** Planned; decisions pending.

**Preconditions:** Feedback vocabulary and adaptation rules have been approved.

**Main success flow:**

1. Choose the supported feedback action for an owned recommendation.
2. Record or amend feedback according to policy.
3. Generate later results and inspect the documented feedback influence.

**Alternative / error flows:**

- Invalid rating or foreign recommendation: reject safely.
- Feedback cannot override a hard restriction.
- Recommendation history does not imply the outfit was worn; explicit wear reporting is separate.

**Postconditions:** Feedback is attributable to the owner and has only the approved influence.

**Related requirements:** FR-SAVE-003, FR-SAVE-004, FR-SAVE-005.

<a id="uc-012"></a>

### UC-012 — Plan an outfit for a future event

**Actor:** Authenticated user. **Status:** Later scope; clarification required.

**Preconditions:** Saved event/planning capability is selected and delivered.

**Main success flow:**

1. Create event details with a confirmed time zone.
2. Associate an owned saved outfit.
3. Reopen, revise, or cancel the plan.

**Alternative / error flows:**

- Missing/unavailable items: follow the agreed plan-reference policy.
- Date beyond forecast horizon: do not fabricate weather.
- No calendar synchronization, reservation, or notification is assumed.

**Postconditions:** The owned plan can be retrieved and changed according to the agreed scope.

**Related requirements:** FR-PLAN-006, FR-PLAN-007.

<a id="uc-013"></a>

### UC-013 — Manage account information and privacy

**Actor:** Authenticated account owner. **Status:** Planned; decisions pending.

**Preconditions:** Account mutation/export/deletion flows and verification/retention policies are approved.

**Main success flow:**

1. Select an available identity/password change or privacy action.
2. Complete the required reauthentication and explicit confirmation.
3. Apply the approved validation, uniqueness, retention, and session-consistency policy.
4. Show the confirmed outcome without returning credentials.

**Alternative / error flows:**

- Duplicate email, failed verification, or invalid input: preserve existing account state.
- Deletion/export follows the agreed data scope and backup policy.
- Password recovery remains a separate future process, not a bypass for authenticated changes.

**Postconditions:** The confirmed change applies only to the owner's account and its sessions follow the approved policy.

**Related requirements:** FR-ACCT-001, FR-ACCT-002, FR-ACCT-003, FR-ACCT-004.

## 9. Technology constraints and dependencies

### 9.1 Confirmed implementation choices

| Concern | Repository baseline / evidence |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Spring Security 7.1.1 (Boot-managed), Spring Data JPA, Hibernate, Jakarta Validation, PostgreSQL JDBC, Maven Wrapper; [Backend dependencies][pom] / [Backend session guide][apidoc] |
| Backend package structure | com.chamodi.styling; feature-based auth/profile packages and common DTO/exception handling |
| Frontend | Next.js 16.3.5 App Router, React 19.2.8, TypeScript, Tailwind CSS v4, shadcn/Base UI components; [Frontend dependencies and scripts][pkg] |
| Workspace | pnpm 10.34.5; frontend workspace and separate Maven backend |
| Database | Neon PostgreSQL, JPA development schema update; [Development configuration][props] |
| Auth protocol | Stateful Spring sessions, BCrypt, CSRF cookie/header, exact-origin credentialed CORS; no JWT/OAuth |
| Frontend state | React context/provider, native typed Fetch, in-memory identity/CSRF state; no third-party auth system |
| Test tooling | JUnit/Mockito/Spring MVC and real servlet cookie tests; Playwright helper and Chromium desktop/mobile tests |
| Assets | Local auth/landing images, including temporary AI-generated development imagery; no runtime external-image dependency |

Versions are snapshot evidence, not a requirement to forbid future supported upgrades. Dependency changes should preserve contracts and rerun affected checks. This baseline does not authorize adding another authentication system, provider SDK, model-training stack, or deployment platform.

### 9.2 Current operational limitations

Sessions are in one backend instance's memory and expire on restart or after the configured idle period. No shared session store, account-wide revocation, rate limiting, or production deployment infrastructure is implemented. A production cookie profile does not itself prove TLS deployment or operational readiness.

DB configuration is server-only; NEXT_PUBLIC_API_BASE_URL is a nonsecret frontend origin setting. Public frontend environment variables must never contain database or service credentials. Environment files and generated build/test artifacts are ignored by Git. Tests/diagnostics around credentials must not enable sensitive debug logging or retain HAR/session captures.

Future weather/media/AI integrations require contract, failure, privacy, cost, and provider decisions. A blocked provider must not silently redefine requirements or cause a custom token/session mechanism to be introduced.

## 10. Verification and acceptance

### 10.1 Evidence and verification status

Baseline 1.0 inspected source without rerunning applications. Revision 1.1 records actual Phase 2 verification: 176 backend tests with isolated PostgreSQL; frontend lint/typecheck/build; 107 deterministic helper/browser tests; and one real browser integration case using local Spring/PostgreSQL. Neon, production HTTPS, and wider browser/accessibility acceptance remain pending. See [exact commands and limits](../testing/phase-2-verification.md).

| Layer | Existing evidence | What it establishes / does not establish |
| --- | --- | --- |
| DTO validation | [RegisterRequestTests][tReg], [LoginRequestTests][tLogin] | Required/format/string/UTF-8 boundaries and redaction; not HTTP/session behavior |
| Registration service | [AuthServiceTests][tService] | Normalization, hashing orchestration, duplicates/races using mocks; not live PostgreSQL connectivity |
| Authentication components | [PasswordAuthenticationTests][tPassword], [RepositoryUserDetailsServiceTests][tDetails] | BCrypt matching and repository-backed principal behavior; not a live browser |
| Controller / security | [AuthControllerTests][tController], [SessionAuthenticationTests][tSession] | JSON/status/validation, CSRF, context persistence, unauthorized access, and session lifecycle without Neon |
| Servlet cookies | [SessionCookieTests][tCookie] | Real servlet cookie attributes and associated security behavior in isolated test configurations |
| Frontend helpers | [Frontend validation tests][tValidation], [Fetch client tests][tClient] | Client validation, byte boundaries, credentialed requests, CSRF serialization/refresh/retries |
| Browser flows | [Frontend auth browser tests][tBrowser] and [Playwright configuration][testconfig] | Mocked API flows, refresh/expiration/errors, stale requests, navigation/motion and responsive checks; not true live cookie/CORS/Neon behavior |
| Full Spring context | [Full application context test][tContext] | Application startup with configured datasource; requires appropriate environment and is not a substitute for future resource integration tests |
| Profile persistence / HTTP | [Persistence tests][tProfilePersistence], [HTTP integration][tProfileHttp] | 14 real profile PostgreSQL tests, including two-account sessions, atomic validation, races, lifecycle, CSRF/CORS |
| Live integration | [Live profile browser test][tProfileLive] and [verification guide](../testing/phase-2-verification.md) | Real local HTTP/CSRF/cookies and PostgreSQL verified; Neon/production HTTPS still pending |

Illustrative existing method references include AuthServiceTests.registersWithNormalizedDetailsAndOnlyTheEncodedPassword, AuthControllerTests.createsAccountAndReturnsOnlySafeFields, and SessionAuthenticationTests.rotatesExistingSessionIdOnSuccessfulLoginAndKeepsAuthentication. Test paths and scope are linked in the matrix; the Phase 2 verification guide records the executed suites and environment separately from test presence.

### 10.2 Reproducible application checks for later feature verification

When application code changes, the existing checks are:

```powershell
# From apps/api (configured environment needed for the full context test):
.\mvnw.cmd test

# From apps/web:
pnpm lint
pnpm typecheck
pnpm build
pnpm test
```

The full backend context may require DB environment variables; distinguish an environment limitation from an assertion failure rather than weakening configuration. Playwright browser checks use the built frontend and mocked API responses, with desktop/mobile Chromium projects. Review existing guides for setup/ports. These checks were executed during Phase 2 as recorded in the verification guide; additional future-feature acceptance remains planned.

### 10.3 Planned feature acceptance strategy

- Profile and wardrobe: service validation plus two-account ownership integration tests; persist/read/update/remove tests and image rejection/access tests when media is added.
- Context and rules: deterministic fixtures for occasion, time zone, weather availability, walking, transport, comfort/modesty, empty wardrobes, missing slots, and conflicting restrictions. Cover a hot-weather public-transport outing without inventing thresholds.
- Ranking: identical input/rule-version ordering, approved tie-breaks, hard constraints excluded before ranking, and performance measurements against an approved workload.
- External services: contract tests against representative sanitized fixtures, schema failures, timeouts, quota errors, minimized outbound payloads, and deterministic fallback.
- AI explanations: invented facts, wrong item references, unsupported weather, and contradictory restrictions must trigger grounding safeguards/fallback.
- Saved data/feedback: ownership, persistence across refresh, reference changes after wardrobe edits, duplicate/edit semantics, and approved feedback influence.
- Account/privacy: reauthentication, email uniqueness, password byte policy, session invalidation, retention/deletion, and export exclusion of credentials once policies are agreed.
- Browser/accessibility: keyboard/focus, associated errors, screen-reader review, reduced motion, contrast, zoom, and responsive widths 360/390/768/1024/1440, including long/error content and mobile input behavior.
- Live deployment: test actual allowed/disallowed origins, credentialed fetch, cookie attributes, CSRF bootstrap/login/logout refresh, reload/expiration, and HTTPS profile; keep credentials/token values out of recorded evidence.

A feature is accepted only when its selected requirements and decisions are approved, implementation is linked, appropriate tests pass, error/ownership flows are checked, and traceability records the result and environment. Future performance and accessibility conformance claims require their specific agreed measurements, not inference from unit tests.

### 10.4 Documentation acceptance

Every FR/NFR definition must have one stable unique ID and one matching matrix row with the same priority/status. Every use-case/decision reference must resolve to a defined ID. Markdown links must resolve to actual files/anchors. No real secret, account credential, cookie value, or provider token may appear. Review contradictions, planned/implemented boundaries, and actual dependency versions; run git diff --check before committing documentation.

## 11. MVP scope and future enhancements

### 11.1 Delivered baseline

Phase 0 and Phase 1 provide setup, approved public/authentication UI, persistence/validation, backend session authentication, frontend integration, isolated automated tests, and integration walkthroughs. Phase 2 adds optional onboarding, owner-bound versioned profiles, editing, setup entry points, and layered verification. Phase 2E adds independently deletable appearance details and a compatible optional fifth onboarding step; it does not add appearance-based recommendations. Dashboard remains minimal; no wardrobe or recommendation dashboard is implemented.

### 11.2 Proposed first usable outfit MVP

Subject to DEC-001, the informal roadmap suggests: styling profile → event/location/time/weather context → manually maintained digital wardrobe → transparent eligibility/composition/ranking → rule-based explanations → saved looks, with basic feedback as prioritized. Styling profile capture/editing is delivered; wardrobe, context, recommendation consumers, saving, and feedback remain planned. Optional AI explanation wording must not be necessary to obtain a useful result.

Priorities identify implementation order and risk, not an approved promise to deliver every Must requirement in the next phase. Media handling, onboarding details, provider choices, privacy controls, and acceptance measurements must be explicitly selected.

### 11.3 Later or unresolved capabilities

| Informal roadmap area | Treatment in this baseline |
| --- | --- |
| Conversational stylist | Optional FR-AI-005; not a required MVP engine |
| Wardrobe image recognition | Optional FR-WARD-008; manual entry remains available |
| Wear history / collections | FR-SAVE-005 / FR-SAVE-006; distinguish wear reports from recommendations |
| Calendar, future event management, reminders | FR-PLAN-006 / FR-PLAN-007 plus DEC-016; no notification integration |
| Shopping gaps / budget / trends | Later exploration under DEC-016; not current wardrobe-based result behavior |
| Full dashboard | Future styling summary/agenda; current account page must not be described as that feature |
| Admin taxonomies, rule maintenance, usage analytics | Unconfirmed actor/permissions/scope; no current administrator role |
| Notifications | Later decision, including channels, opt-in, weather-change triggers, and abuse controls |
| Makeup / hair styling recommendations | Landing imagery only; DEC-001 must define broader product scope |
| Recovery / Google / Apple | Explicitly unavailable now; future policy under DEC-015 |
| Advanced personalization / virtual try-on | Future exploration; no training-from-scratch or model-selection commitment |
| Multi-instance deployment / production operations | DEC-018; production profile alone is insufficient |

No payments, ecommerce checkout, social networking, public wardrobe sharing, medical/health advice, or deployment platform is confirmed scope. These are excluded from the proposed outfit MVP unless the owner explicitly revises it. Exclusion is a proposed scope boundary, not a claim that an informal mention was already approved or permanently rejected.

### 11.4 Informal backlog coverage

The root backlog's 21 epics map as follows: Epic 1 → AUTH/ACCT; 2 → ONB; 3–5 → PLAN/REC weather; 6 → WARD; 7–8 and 16 → REC; 9 → AI; 10 → SAVE feedback; 11 → SAVE history; 12 → SAVE collections; 13 → PLAN later calendar; 14–15 → later shopping/trends; 17 → later notifications; 18 → minimal AUTH-011 now/full dashboard later; 19 → unconfirmed administration; 20 → privacy/ACCT; 21 → NFR quality requirements.

This mapping preserves the whole-product roadmap without manufacturing finalized requirements for every aspirational epic.

## 12. Open decisions and assumptions

### 12.1 Decision log

There are 18 stable decision entries. DEC-002 is resolved for Phase 2; DEC-003 and DEC-004 have approved profile-only portions with remaining work. The other 15 entries remain Open (17 entries still contain unresolved work). The project owner remains the decision owner. Dates below record decisions, not delivery deadlines.

| ID | Decision / question | Affected areas | Needed before |
| --- | --- | --- | --- |
| <a id="dec-001"></a>DEC-001 — Release scope and styling breadth | Confirm the overall outfit MVP boundary and whether makeup, hair, and accessory engines extend beyond wardrobe outfits. Phase 2 profile scope is approved independently; landing imagery is not evidence of those engines. | FR-AUTH-001; all planned modules | Before recommendation/broader styling scope |
| <a id="dec-002"></a>DEC-002 — Onboarding contract | **Resolved, recorded 2026-10-17:** original four screens, extended by approved Phase 2E to five optional screens; ten optional catalog groups, null versus No preference, SAVE/SKIP/COMPLETE, save/resume, no Dashboard gate, separate identity editing. | FR-ONB-001, FR-ONB-002, FR-ONB-004 | Implemented Phase 2 |
| <a id="dec-003"></a>DEC-003 — Constraint semantics | **Partially resolved, recorded 2026-10-17:** only optional coverage may be Required; other choices/disliked colors are preferences; never infer restrictions. Engine conflict precedence, request overrides, and relaxation remain Open. | FR-ONB-003, FR-PLAN-005, FR-REC-002, FR-REC-009 | Before rule implementation |
| <a id="dec-004"></a>DEC-004 — Wardrobe and context taxonomy | **Partially resolved, recorded 2026-10-17:** initial profile style/color/comfort/lifestyle catalog approved. Wardrobe categories, item metadata, availability, rule taxonomy, and missing attributes remain Open. | FR-WARD-001 through FR-WARD-008; FR-PLAN-001; FR-REC-003 | Before wardrobe schema/API |
| <a id="dec-005"></a>DEC-005 — Media policy | Select local/object storage, supported file types and limits, image metadata handling, ownership access, replacement, and deletion. | FR-WARD-004, FR-WARD-007; NFR-PRIV-002 | Before image upload |
| <a id="dec-006"></a>DEC-006 — Optional item recognition | Decide whether AI classification is in scope, provider/processing location, confidence handling, user confirmation, and consent. | FR-WARD-008 | Before recognition work |
| <a id="dec-007"></a>DEC-007 — Weather contract | Select provider and usable fields, units, forecast horizon, freshness, rate limits, failure behavior, and user-entered fallback. | FR-PLAN-003; FR-REC-005; NFR-REL-003 | Before weather adapter |
| <a id="dec-008"></a>DEC-008 — Location and event time | Define location precision, manual/geocoding/device-location paths, permission flow, time-zone interpretation, and retention. | FR-PLAN-002, FR-PLAN-003; NFR-PRIV-001 | Before location collection |
| <a id="dec-009"></a>DEC-009 — Outfit composition and saved references | Define complete look slots, optional accessories, dress/layer alternatives, missing-item treatment, and saved-plan behavior after wardrobe edits/deletion. | FR-REC-003, FR-SAVE-001, FR-PLAN-006 | Before candidate composition |
| <a id="dec-010"></a>DEC-010 — Ranking and workload | Choose documented rules, weights if needed, deterministic tie-breaks, candidate count, feedback inputs, bounded processing, and representative performance workload. | FR-REC-004, FR-REC-008, FR-REC-009; NFR-PERF-002 | Before ranking acceptance |
| <a id="dec-011"></a>DEC-011 — AI explanation boundary | Decide if/when AI wording is enabled, provider/model, payload minimization, consent/disclosure, grounding checks, retention, quotas, cost, and failure policy. | FR-AI-001 through FR-AI-005 | Before external AI calls |
| <a id="dec-012"></a>DEC-012 — Feedback and adaptation | Choose like/dislike or rating vocabulary, edit/repeat rules, reasons, learning/adaptation method, explainability, and reset controls. | FR-SAVE-003, FR-SAVE-004 | Before feedback persistence |
| <a id="dec-013"></a>DEC-013 — Account changes and reauthentication | Define name/email/password change verification, account deletion proof, session snapshot refresh, and account-wide revocation. | FR-ACCT-001 through FR-ACCT-003; FR-ACCT-005 | Before account mutation endpoints |
| <a id="dec-014"></a>DEC-014 — Privacy lifecycle | Agree jurisdiction and privacy obligations, data access/export scope, retention/deletion timing, backups, history, media, and provider retention. | FR-ACCT-003, FR-ACCT-004; NFR-PRIV-002 | Before new personal-data categories |
| <a id="dec-015"></a>DEC-015 — Recovery and social sign-in | Choose later password-reset delivery/token/abuse policy and whether Google/Apple sign-in and account linking are in scope. | FR-ACCT-005; FR-AUTH-012 | Before enabling currently disabled actions |
| <a id="dec-016"></a>DEC-016 — Later feature boundaries | Prioritize calendar/reminders, conversational stylist, history/collections, full dashboard, admin tools, notifications, trends, shopping/budget, and any virtual try-on. Define actors and permissions only when approved. | FR-PLAN-006, FR-PLAN-007, FR-AI-005, FR-SAVE-005, FR-SAVE-006 | Before allocating those features |
| <a id="dec-017"></a>DEC-017 — Accessibility and browser acceptance | Confirm proposed WCAG 2.2 AA target, testing scope, assistive technologies, supported browser/version matrix, and touch/zoom checks. | NFR-ACC-001, NFR-ACC-002, NFR-COMP-001 | Before new-screen acceptance |
| <a id="dec-018"></a>DEC-018 — Deployment and operational targets | Choose same-site production origins/TLS, active production profile, server-rendered authorization policy if introduced, multi-instance session strategy, rate limiting, workloads, and measurable availability/performance targets. | NFR-SEC-004, NFR-PERF-002, NFR-SCALE-001, NFR-SCALE-002 | Before production claims or scaling |

### 12.2 Assumptions requiring continued validation

- The product owner and implementer are currently the same individual; no research or stakeholder interviews are claimed as evidence.
- The first recommendation product uses owned wardrobe items and deterministic rules. AI assistance is optional, not a prerequisite or a training project.
- Existing auth UI is approved; future screens extend its design language rather than redesigning it.
- Product preferences come from the user; no gender/cultural/transport stereotypes substitute for explicit choices.
- Browser/API deployment remains same-site unless a reviewed cookie/CSRF arrangement is chosen.
- Local assets are development illustrations and may be replaced; generated asset use does not establish runtime AI capability or future media rights.
- Planned acceptance criteria are proposals; implementation cannot silently resolve missing thresholds, privacy periods, provider choices, or release boundaries.

### 12.3 Change control

Phase 2 resolved its onboarding contract and approved initial profile vocabulary/strength storage. Before each subsequent slice, resolve the remaining wardrobe/media/context/rule dependencies, add acceptance tests, and update this document and the matrix together. Keep completed behavior distinct from proposals, and retain obsolete IDs/history rather than renumbering the baseline.

## 13. Evidence register

All links below are repository-relative. Source/test definitions establish scope; executed Phase 2 results and environment limits are recorded in the [verification guide](../testing/phase-2-verification.md). Local PostgreSQL/live-browser verification is not a Neon or production claim.

| Evidence | Repository path |
| --- | --- |
| [Product backlog][roadmap] | Personal Styling App — Epics & User Stories 1.md |
| [Frontend integration guide][webdoc] | apps/web/README.md |
| [Backend session guide][apidoc] | apps/api/SESSION_AUTH.md |
| [Backend dependencies][pom] | apps/api/pom.xml |
| [Frontend dependencies and scripts][pkg] | apps/web/package.json |
| [AuthController][controller] | apps/api/src/main/java/com/chamodi/styling/auth/controller/AuthController.java |
| [AuthService][service] | apps/api/src/main/java/com/chamodi/styling/auth/service/AuthService.java |
| [Profile controller][profileController] | apps/api/src/main/java/com/chamodi/styling/profile/controller/StylingProfileController.java |
| [Profile service][profileService] | apps/api/src/main/java/com/chamodi/styling/profile/service/StylingProfileService.java |
| [Profile entity][profileEntity] | apps/api/src/main/java/com/chamodi/styling/profile/entity/StylingProfile.java |
| [Profile workspace][profileWorkspace] | apps/web/src/components/profile/profile-workspace.tsx |
| [Profile transport][profileClient] | apps/web/src/lib/api/profile-client.ts |
| [Profile HTTP integration][tProfileHttp] | apps/api/src/test/java/com/chamodi/styling/profile/controller/StylingProfileHttpIntegrationTests.java |
| [Live browser profile test][tProfileLive] | apps/web/tests/live/profile-live.spec.ts |
| [User entity][entity] | apps/api/src/main/java/com/chamodi/styling/auth/entity/User.java |
| [UserRepository][repository] | apps/api/src/main/java/com/chamodi/styling/auth/repository/UserRepository.java |
| [RegisterRequest][regdto] | apps/api/src/main/java/com/chamodi/styling/auth/dto/RegisterRequest.java |
| [LoginRequest][logindto] | apps/api/src/main/java/com/chamodi/styling/auth/dto/LoginRequest.java |
| [UTF-8 validator][byteval] | apps/api/src/main/java/com/chamodi/styling/auth/validation/Utf8ByteLengthValidator.java |
| [Password encoder][encoder] | apps/api/src/main/java/com/chamodi/styling/auth/config/PasswordEncoderConfig.java |
| [SecurityConfig][security] | apps/api/src/main/java/com/chamodi/styling/auth/config/SecurityConfig.java |
| [UserDetailsService][details] | apps/api/src/main/java/com/chamodi/styling/auth/security/RepositoryUserDetailsService.java |
| [AccountPrincipal][principal] | apps/api/src/main/java/com/chamodi/styling/auth/security/AccountPrincipal.java |
| [MVC exception handler][errors] | apps/api/src/main/java/com/chamodi/styling/common/exception/GlobalExceptionHandler.java |
| [Security error handler][apierrors] | apps/api/src/main/java/com/chamodi/styling/auth/security/ApiSecurityErrorHandler.java |
| [Development configuration][props] | apps/api/src/main/resources/application.properties |
| [Production configuration][prodprops] | apps/api/src/main/resources/application-prod.properties |
| [Typed Fetch client][client] | apps/web/src/lib/api/auth-client.ts |
| [Frontend validation][validation] | apps/web/src/lib/auth/validation.ts |
| [SessionProvider][provider] | apps/web/src/components/auth/session-provider.tsx |
| [RequireAuth][guard] | apps/web/src/components/auth/require-auth.tsx |
| [AuthShell][shell] | apps/web/src/components/auth/auth-shell.tsx |
| [Auth motion CSS][motion] | apps/web/src/components/auth/auth-shell.module.css |
| [AuthLink][authlink] | apps/web/src/components/auth/auth-link.tsx |
| [AuthField][field] | apps/web/src/components/auth/auth-field.tsx |
| [Register page][register] | apps/web/src/app/(auth)/register/page.tsx |
| [Login page][login] | apps/web/src/app/(auth)/login/page.tsx |
| [Forgot Password page][forgot] | apps/web/src/app/(auth)/forgot-password/page.tsx |
| [Social buttons][social] | apps/web/src/components/auth/social-auth-buttons.tsx |
| [Minimal dashboard][dashboard] | apps/web/src/app/(app)/dashboard/page.tsx |
| [Landing page][landing] | apps/web/src/app/page.tsx |
| [Landing hero][hero] | apps/web/src/components/landing/hero-section.tsx |
| [Styling categories][categories] | apps/web/src/components/landing/style-categories.tsx |
| [Global theme][stylecss] | apps/web/src/app/globals.css |
| [RegisterRequestTests][tReg] | apps/api/src/test/java/com/chamodi/styling/auth/dto/RegisterRequestTests.java |
| [LoginRequestTests][tLogin] | apps/api/src/test/java/com/chamodi/styling/auth/dto/LoginRequestTests.java |
| [AuthServiceTests][tService] | apps/api/src/test/java/com/chamodi/styling/auth/service/AuthServiceTests.java |
| [AuthControllerTests][tController] | apps/api/src/test/java/com/chamodi/styling/auth/controller/AuthControllerTests.java |
| [SessionAuthenticationTests][tSession] | apps/api/src/test/java/com/chamodi/styling/auth/controller/SessionAuthenticationTests.java |
| [SessionCookieTests][tCookie] | apps/api/src/test/java/com/chamodi/styling/auth/controller/SessionCookieTests.java |
| [PasswordAuthenticationTests][tPassword] | apps/api/src/test/java/com/chamodi/styling/auth/security/PasswordAuthenticationTests.java |
| [RepositoryUserDetailsServiceTests][tDetails] | apps/api/src/test/java/com/chamodi/styling/auth/security/RepositoryUserDetailsServiceTests.java |
| [Full application context test][tContext] | apps/api/src/test/java/com/chamodi/styling/StylingApiApplicationTests.java |
| [Frontend auth browser tests][tBrowser] | apps/web/tests/auth/auth.spec.ts |
| [Fetch client tests][tClient] | apps/web/tests/unit/auth-client.spec.ts |
| [Frontend validation tests][tValidation] | apps/web/tests/unit/validation.spec.ts |
| [Playwright configuration][testconfig] | apps/web/playwright.config.ts |

[roadmap]: <../../Personal Styling App — Epics & User Stories 1.md>
[webdoc]: <../../apps/web/README.md>
[apidoc]: <../../apps/api/SESSION_AUTH.md>
[pom]: <../../apps/api/pom.xml>
[pkg]: <../../apps/web/package.json>
[controller]: <../../apps/api/src/main/java/com/chamodi/styling/auth/controller/AuthController.java>
[service]: <../../apps/api/src/main/java/com/chamodi/styling/auth/service/AuthService.java>
[entity]: <../../apps/api/src/main/java/com/chamodi/styling/auth/entity/User.java>
[repository]: <../../apps/api/src/main/java/com/chamodi/styling/auth/repository/UserRepository.java>
[regdto]: <../../apps/api/src/main/java/com/chamodi/styling/auth/dto/RegisterRequest.java>
[logindto]: <../../apps/api/src/main/java/com/chamodi/styling/auth/dto/LoginRequest.java>
[byteval]: <../../apps/api/src/main/java/com/chamodi/styling/auth/validation/Utf8ByteLengthValidator.java>
[encoder]: <../../apps/api/src/main/java/com/chamodi/styling/auth/config/PasswordEncoderConfig.java>
[security]: <../../apps/api/src/main/java/com/chamodi/styling/auth/config/SecurityConfig.java>
[details]: <../../apps/api/src/main/java/com/chamodi/styling/auth/security/RepositoryUserDetailsService.java>
[principal]: <../../apps/api/src/main/java/com/chamodi/styling/auth/security/AccountPrincipal.java>
[errors]: <../../apps/api/src/main/java/com/chamodi/styling/common/exception/GlobalExceptionHandler.java>
[apierrors]: <../../apps/api/src/main/java/com/chamodi/styling/auth/security/ApiSecurityErrorHandler.java>
[props]: <../../apps/api/src/main/resources/application.properties>
[prodprops]: <../../apps/api/src/main/resources/application-prod.properties>
[client]: <../../apps/web/src/lib/api/auth-client.ts>
[validation]: <../../apps/web/src/lib/auth/validation.ts>
[provider]: <../../apps/web/src/components/auth/session-provider.tsx>
[guard]: <../../apps/web/src/components/auth/require-auth.tsx>
[shell]: <../../apps/web/src/components/auth/auth-shell.tsx>
[motion]: <../../apps/web/src/components/auth/auth-shell.module.css>
[authlink]: <../../apps/web/src/components/auth/auth-link.tsx>
[field]: <../../apps/web/src/components/auth/auth-field.tsx>
[register]: <../../apps/web/src/app/(auth)/register/page.tsx>
[login]: <../../apps/web/src/app/(auth)/login/page.tsx>
[forgot]: <../../apps/web/src/app/(auth)/forgot-password/page.tsx>
[social]: <../../apps/web/src/components/auth/social-auth-buttons.tsx>
[dashboard]: <../../apps/web/src/app/(app)/dashboard/page.tsx>
[landing]: <../../apps/web/src/app/page.tsx>
[hero]: <../../apps/web/src/components/landing/hero-section.tsx>
[categories]: <../../apps/web/src/components/landing/style-categories.tsx>
[stylecss]: <../../apps/web/src/app/globals.css>
[tReg]: <../../apps/api/src/test/java/com/chamodi/styling/auth/dto/RegisterRequestTests.java>
[tLogin]: <../../apps/api/src/test/java/com/chamodi/styling/auth/dto/LoginRequestTests.java>
[tService]: <../../apps/api/src/test/java/com/chamodi/styling/auth/service/AuthServiceTests.java>
[tController]: <../../apps/api/src/test/java/com/chamodi/styling/auth/controller/AuthControllerTests.java>
[tSession]: <../../apps/api/src/test/java/com/chamodi/styling/auth/controller/SessionAuthenticationTests.java>
[tCookie]: <../../apps/api/src/test/java/com/chamodi/styling/auth/controller/SessionCookieTests.java>
[tPassword]: <../../apps/api/src/test/java/com/chamodi/styling/auth/security/PasswordAuthenticationTests.java>
[tDetails]: <../../apps/api/src/test/java/com/chamodi/styling/auth/security/RepositoryUserDetailsServiceTests.java>
[tContext]: <../../apps/api/src/test/java/com/chamodi/styling/StylingApiApplicationTests.java>
[tBrowser]: <../../apps/web/tests/auth/auth.spec.ts>
[tClient]: <../../apps/web/tests/unit/auth-client.spec.ts>
[tValidation]: <../../apps/web/tests/unit/validation.spec.ts>
[testconfig]: <../../apps/web/playwright.config.ts>

[onboarding]: <../../apps/web/src/app/(app)/onboarding/page.tsx>
[profilePage]: <../../apps/web/src/app/(app)/profile/page.tsx>
[onboardingFlow]: ../../apps/web/src/components/onboarding/onboarding-flow.tsx
[profileEditor]: ../../apps/web/src/components/profile/profile-editor.tsx
[profileWorkspace]: ../../apps/web/src/components/profile/profile-workspace.tsx
[profileClient]: ../../apps/web/src/lib/api/profile-client.ts
[profileController]: ../../apps/api/src/main/java/com/chamodi/styling/profile/controller/StylingProfileController.java
[profileService]: ../../apps/api/src/main/java/com/chamodi/styling/profile/service/StylingProfileService.java
[profileEntity]: ../../apps/api/src/main/java/com/chamodi/styling/profile/entity/StylingProfile.java
[tProfilePersistence]: ../../apps/api/src/test/java/com/chamodi/styling/profile/repository/StylingProfilePersistenceTests.java
[tProfileHttp]: ../../apps/api/src/test/java/com/chamodi/styling/profile/controller/StylingProfileHttpIntegrationTests.java
[tProfileService]: ../../apps/api/src/test/java/com/chamodi/styling/profile/service/StylingProfileServiceTests.java
[tOnboarding]: ../../apps/web/tests/auth/onboarding.spec.ts
[tProfileEditor]: ../../apps/web/tests/auth/profile-editor.spec.ts
[tProfileClient]: ../../apps/web/tests/unit/profile-client.spec.ts
[tProfileBrowser]: ../../apps/web/tests/auth/profile-integration.spec.ts
[tProfileLive]: ../../apps/web/tests/live/profile-live.spec.ts
