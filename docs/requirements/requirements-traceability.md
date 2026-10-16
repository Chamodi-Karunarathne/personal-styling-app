# Requirements traceability matrix

Baseline 1.0, 2026-10-16. [SRS](SRS.md) · [Documentation index](../README.md).

This matrix covers every functional and non-functional requirement in the SRS: **58 FR + 24 NFR = 82 requirements**. Use cases and open decisions are supporting specifications, not additional requirements counted here.

## Reading and maintenance

- Implemented means source supports the stated behavior, not a fresh passing test result or a production audit.
- Planned means no complete implementation is claimed. Requires clarification identifies an unresolved behavior, policy, or release decision.
- Source links on a planned quality requirement identify supporting foundations only, not delivery of the whole requirement.
- Existing test files are real repository paths. Their definitions were inspected; no application suite was rerun for this documentation-only milestone.
- “Not yet verified” on a future requirement means its SRS acceptance criteria must become tests after scope/decisions are approved; no nonexistent source or test is supplied.
- Keep IDs stable, retain retired IDs with explanation, and update both documents together. Add execution date, environment, command/test case, and outcome when actual verification is performed.

| Category | Implemented | Planned | Requires clarification | Total |
| --- | --- | --- | --- | --- |
| Functional | 12 | 21 | 25 | 58 |
| Non-functional | 9 | 11 | 4 | 24 |

## Functional requirements

| Requirement ID | Summary | Module | Priority | Implementation status | Related source files | Relevant tests | Verification status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [FR-AUTH-001](SRS.md#fr-auth-001) | Public entry and authentication navigation | Authentication | Must | Implemented | [Landing page][landing]<br>[Landing hero][hero]<br>[Styling categories][categories] | [Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-002](SRS.md#fr-auth-002) | Register an account | Authentication | Must | Implemented | [AuthController][controller]<br>[AuthService][service]<br>[Password encoder][encoder]<br>[User entity][entity]<br>[Register page][register]<br>[SessionProvider][provider] | [AuthServiceTests][tService]<br>[AuthControllerTests][tController]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-003](SRS.md#fr-auth-003) | Validate registration input | Authentication | Must | Implemented | [RegisterRequest][regdto]<br>[UTF-8 validator][byteval]<br>[Frontend validation][validation]<br>[Register page][register]<br>[MVC exception handler][errors] | [RegisterRequestTests][tReg]<br>[AuthControllerTests][tController]<br>[Frontend validation tests][tValidation]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-004](SRS.md#fr-auth-004) | Reject duplicate email safely | Authentication | Must | Implemented | [AuthService][service]<br>[UserRepository][repository]<br>[User entity][entity]<br>[MVC exception handler][errors] | [AuthServiceTests][tService]<br>[AuthControllerTests][tController] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-005](SRS.md#fr-auth-005) | Authenticate with a server session | Authentication | Must | Implemented | [AuthController][controller]<br>[UserDetailsService][details]<br>[AccountPrincipal][principal]<br>[SecurityConfig][security]<br>[Login page][login]<br>[SessionProvider][provider] | [PasswordAuthenticationTests][tPassword]<br>[RepositoryUserDetailsServiceTests][tDetails]<br>[SessionAuthenticationTests][tSession]<br>[SessionCookieTests][tCookie]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-006](SRS.md#fr-auth-006) | Validate login without changing passwords | Authentication | Must | Implemented | [LoginRequest][logindto]<br>[UTF-8 validator][byteval]<br>[UserDetailsService][details]<br>[Frontend validation][validation]<br>[Login page][login] | [LoginRequestTests][tLogin]<br>[SessionAuthenticationTests][tSession]<br>[Frontend validation tests][tValidation]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-007](SRS.md#fr-auth-007) | Bootstrap and refresh CSRF | Authentication | Must | Implemented | [AuthController][controller]<br>[SecurityConfig][security]<br>[Typed Fetch client][client] | [SessionAuthenticationTests][tSession]<br>[Fetch client tests][tClient]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-008](SRS.md#fr-auth-008) | Restore and reconcile the current session | Authentication | Must | Implemented | [AuthController][controller]<br>[SessionProvider][provider]<br>[Typed Fetch client][client] | [SessionAuthenticationTests][tSession]<br>[Frontend auth browser tests][tBrowser]<br>[Fetch client tests][tClient] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-009](SRS.md#fr-auth-009) | Log out reliably | Authentication | Must | Implemented | [AuthController][controller]<br>[SecurityConfig][security]<br>[Typed Fetch client][client]<br>[SessionProvider][provider]<br>[Minimal dashboard][dashboard] | [SessionAuthenticationTests][tSession]<br>[SessionCookieTests][tCookie]<br>[Fetch client tests][tClient]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-010](SRS.md#fr-auth-010) | Gate private navigation | Authentication | Must | Implemented | [RequireAuth][guard]<br>[SessionProvider][provider]<br>[SecurityConfig][security] | [Frontend auth browser tests][tBrowser]<br>[SessionAuthenticationTests][tSession] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-011](SRS.md#fr-auth-011) | Show a minimal authenticated account screen | Authentication | Must | Implemented | [Minimal dashboard][dashboard]<br>[AuthController][controller] | [Frontend auth browser tests][tBrowser]<br>[SessionAuthenticationTests][tSession] | Source/test definitions inspected; not executed for this milestone |
| [FR-AUTH-012](SRS.md#fr-auth-012) | Preserve approved auth UX and unavailable actions | Authentication | Must | Implemented | [AuthShell][shell]<br>[Auth motion CSS][motion]<br>[AuthLink][authlink]<br>[Forgot Password page][forgot]<br>[Social buttons][social] | [Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [FR-ONB-001](SRS.md#fr-onb-001) | Capture a personal styling profile | Onboarding | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-002, DEC-004 |
| [FR-ONB-002](SRS.md#fr-onb-002) | Control onboarding completion and skipping | Onboarding | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-002 |
| [FR-ONB-003](SRS.md#fr-onb-003) | Separate restrictions from preferences | Onboarding | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003 |
| [FR-ONB-004](SRS.md#fr-onb-004) | Read and update styling preferences | Onboarding | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-002, DEC-004 |
| [FR-ONB-005](SRS.md#fr-onb-005) | Use self-declared comfort and modesty choices | Onboarding | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003, DEC-004 |
| [FR-ONB-006](SRS.md#fr-onb-006) | Review profile-derived request defaults | Onboarding | Should | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003 |
| [FR-WARD-001](SRS.md#fr-ward-001) | Add a wardrobe item | Wardrobe | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004 |
| [FR-WARD-002](SRS.md#fr-ward-002) | Browse wardrobe items | Wardrobe | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending |
| [FR-WARD-003](SRS.md#fr-ward-003) | Edit wardrobe information | Wardrobe | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004 |
| [FR-WARD-004](SRS.md#fr-ward-004) | Remove a wardrobe item | Wardrobe | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-005, DEC-014 |
| [FR-WARD-005](SRS.md#fr-ward-005) | Represent item availability | Wardrobe | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004 |
| [FR-WARD-006](SRS.md#fr-ward-006) | Search and filter the wardrobe | Wardrobe | Should | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004 |
| [FR-WARD-007](SRS.md#fr-ward-007) | Attach wardrobe images | Wardrobe | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-005 |
| [FR-WARD-008](SRS.md#fr-ward-008) | Assist item classification | Wardrobe | Could | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004, DEC-006 |
| [FR-PLAN-001](SRS.md#fr-plan-001) | Specify an occasion | Planning | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004 |
| [FR-PLAN-002](SRS.md#fr-plan-002) | Specify location and environment | Planning | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-008 |
| [FR-PLAN-003](SRS.md#fr-plan-003) | Specify date, time, and duration | Planning | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-007, DEC-008 |
| [FR-PLAN-004](SRS.md#fr-plan-004) | Specify activities and transportation | Planning | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004 |
| [FR-PLAN-005](SRS.md#fr-plan-005) | Review request-specific constraints | Planning | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003 |
| [FR-PLAN-006](SRS.md#fr-plan-006) | Save a future outfit plan | Planning | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-009, DEC-016 |
| [FR-PLAN-007](SRS.md#fr-plan-007) | Manage planned events | Planning | Could | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-016 |
| [FR-REC-001](SRS.md#fr-rec-001) | Use the available owned wardrobe | Recommendations | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004, DEC-009 |
| [FR-REC-002](SRS.md#fr-rec-002) | Apply explicit constraints | Recommendations | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003 |
| [FR-REC-003](SRS.md#fr-rec-003) | Compose coordinated outfits | Recommendations | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-004, DEC-009 |
| [FR-REC-004](SRS.md#fr-rec-004) | Rank candidates transparently | Recommendations | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-010 |
| [FR-REC-005](SRS.md#fr-rec-005) | Incorporate weather context | Recommendations | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-007, DEC-008 |
| [FR-REC-006](SRS.md#fr-rec-006) | Explain insufficient matches | Recommendations | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003, DEC-009 |
| [FR-REC-007](SRS.md#fr-rec-007) | Provide rule-based explanations | Recommendations | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003, DEC-007, DEC-010 |
| [FR-REC-008](SRS.md#fr-rec-008) | Revise context and compare alternatives | Recommendations | Should | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-010 |
| [FR-REC-009](SRS.md#fr-rec-009) | Evaluate practical activity and travel suitability | Recommendations | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003, DEC-004, DEC-010 |
| [FR-AI-001](SRS.md#fr-ai-001) | Optionally enhance recommendation explanations | AI explanations | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-011 |
| [FR-AI-002](SRS.md#fr-ai-002) | Ground assisted explanations | AI explanations | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-011 |
| [FR-AI-003](SRS.md#fr-ai-003) | Retain recommendations without an AI provider | AI explanations | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-011 |
| [FR-AI-004](SRS.md#fr-ai-004) | Minimize outbound AI information | AI explanations | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-011, DEC-014 |
| [FR-AI-005](SRS.md#fr-ai-005) | Offer conversational styling assistance | AI explanations | Could | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-011, DEC-016 |
| [FR-SAVE-001](SRS.md#fr-save-001) | Save a recommended outfit | Saved / feedback | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-009, DEC-014 |
| [FR-SAVE-002](SRS.md#fr-save-002) | Manage saved outfits | Saved / feedback | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending |
| [FR-SAVE-003](SRS.md#fr-save-003) | Record recommendation feedback | Saved / feedback | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-012 |
| [FR-SAVE-004](SRS.md#fr-save-004) | Use feedback in later personalization | Saved / feedback | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-003, DEC-012 |
| [FR-SAVE-005](SRS.md#fr-save-005) | Review outfit and recommendation history | Saved / feedback | Could | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-014, DEC-016 |
| [FR-SAVE-006](SRS.md#fr-save-006) | Organize saved looks into collections | Saved / feedback | Could | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-016 |
| [FR-ACCT-001](SRS.md#fr-acct-001) | Update account identity | Account | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-013 |
| [FR-ACCT-002](SRS.md#fr-acct-002) | Change an authenticated password | Account | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-013 |
| [FR-ACCT-003](SRS.md#fr-acct-003) | Delete an account and associated information | Account | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-013, DEC-014 |
| [FR-ACCT-004](SRS.md#fr-acct-004) | Access or export personal information | Account | Could | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-014 |
| [FR-ACCT-005](SRS.md#fr-acct-005) | Recover account access | Account | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-013, DEC-015 |

## Non-functional requirements

| Requirement ID | Summary | Module | Priority | Implementation status | Related source files or supporting foundations | Relevant tests | Verification status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [NFR-SEC-001](SRS.md#nfr-sec-001) | Credential handling | Security | Must | Implemented | [RegisterRequest][regdto]<br>[LoginRequest][logindto]<br>[UTF-8 validator][byteval]<br>[Password encoder][encoder]<br>[AuthService][service]<br>[AccountPrincipal][principal]<br>[AuthController][controller] | [RegisterRequestTests][tReg]<br>[LoginRequestTests][tLogin]<br>[AuthServiceTests][tService]<br>[PasswordAuthenticationTests][tPassword]<br>[AuthControllerTests][tController]<br>[SessionAuthenticationTests][tSession] | Source/test definitions inspected; not executed for this milestone |
| [NFR-SEC-002](SRS.md#nfr-sec-002) | Server session lifecycle | Security | Must | Implemented | [AuthController][controller]<br>[SecurityConfig][security]<br>[AccountPrincipal][principal]<br>[Development configuration][props] | [SessionAuthenticationTests][tSession]<br>[SessionCookieTests][tCookie] | Source/test definitions inspected; not executed for this milestone |
| [NFR-SEC-003](SRS.md#nfr-sec-003) | CSRF and origin controls | Security | Must | Implemented | [SecurityConfig][security]<br>[AuthController][controller]<br>[Typed Fetch client][client] | [SessionAuthenticationTests][tSession]<br>[SessionCookieTests][tCookie]<br>[Fetch client tests][tClient] | Source/test definitions inspected; not executed for this milestone |
| [NFR-SEC-004](SRS.md#nfr-sec-004) | Session cookie transport policy | Security | Must | Implemented | [SecurityConfig][security]<br>[Development configuration][props]<br>[Production configuration][prodprops] | [SessionCookieTests][tCookie] | Source/test definitions inspected; not executed for this milestone |
| [NFR-SEC-005](SRS.md#nfr-sec-005) | Resource ownership authorization | Security | Must | Planned | [SecurityConfig][security] | None yet | Not yet verified; SRS acceptance pending |
| [NFR-SEC-006](SRS.md#nfr-sec-006) | Safe errors and diagnostics | Security | Must | Implemented | [MVC exception handler][errors]<br>[Security error handler][apierrors]<br>[RegisterRequest][regdto]<br>[LoginRequest][logindto]<br>[Development configuration][props] | [AuthControllerTests][tController]<br>[SessionAuthenticationTests][tSession]<br>[PasswordAuthenticationTests][tPassword] | Source/test definitions inspected; not executed for this milestone |
| [NFR-PRIV-001](SRS.md#nfr-priv-001) | Data minimization and personal choices | Privacy | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-002, DEC-008, DEC-014 |
| [NFR-PRIV-002](SRS.md#nfr-priv-002) | Retention and deletion policy | Privacy | Must | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-005, DEC-011, DEC-014 |
| [NFR-PRIV-003](SRS.md#nfr-priv-003) | No browser credential persistence | Privacy | Must | Implemented | [Typed Fetch client][client]<br>[SessionProvider][provider]<br>[Register page][register]<br>[Login page][login] | [Fetch client tests][tClient]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [NFR-REL-001](SRS.md#nfr-rel-001) | Async authentication consistency | Reliability | Must | Implemented | [Typed Fetch client][client]<br>[SessionProvider][provider]<br>[Register page][register]<br>[Login page][login] | [Fetch client tests][tClient]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [NFR-REL-002](SRS.md#nfr-rel-002) | Atomic registration and email integrity | Reliability | Must | Implemented | [AuthService][service]<br>[User entity][entity]<br>[UserRepository][repository] | [AuthServiceTests][tService]<br>[AuthControllerTests][tController] | Source/test definitions inspected; not executed for this milestone |
| [NFR-REL-003](SRS.md#nfr-rel-003) | Recommendation dependency fallback | Reliability | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-007, DEC-011 |
| [NFR-PERF-001](SRS.md#nfr-perf-001) | Bounded frontend authentication waiting | Performance | Must | Implemented | [Typed Fetch client][client]<br>[SessionProvider][provider] | [Fetch client tests][tClient]<br>[Frontend auth browser tests][tBrowser] | Source/test definitions inspected; not executed for this milestone |
| [NFR-PERF-002](SRS.md#nfr-perf-002) | Recommendation performance budget | Performance | Should | Requires clarification | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-010, DEC-018 |
| [NFR-ACC-001](SRS.md#nfr-acc-001) | Keyboard and form accessibility | Accessibility | Must | Planned | [AuthField][field]<br>[Register page][register]<br>[Login page][login]<br>[Forgot Password page][forgot]<br>[Minimal dashboard][dashboard] | [Frontend auth browser tests][tBrowser] | Not yet verified; SRS acceptance pending; DEC-017 |
| [NFR-ACC-002](SRS.md#nfr-acc-002) | Motion and assistive presentation | Accessibility | Must | Planned | [AuthShell][shell]<br>[Auth motion CSS][motion]<br>[Landing hero][hero]<br>[Styling categories][categories] | [Frontend auth browser tests][tBrowser] | Not yet verified; SRS acceptance pending; DEC-017 |
| [NFR-UX-001](SRS.md#nfr-ux-001) | Responsive visual consistency | Usability | Must | Planned | [Global theme][stylecss]<br>[AuthShell][shell]<br>[Auth motion CSS][motion]<br>[Landing page][landing]<br>[Styling categories][categories] | [Frontend auth browser tests][tBrowser] | Not yet verified; SRS acceptance pending |
| [NFR-UX-002](SRS.md#nfr-ux-002) | Honest loading and unavailable states | Usability | Must | Planned | [SessionProvider][provider]<br>[RequireAuth][guard]<br>[Forgot Password page][forgot]<br>[Social buttons][social]<br>[Register page][register]<br>[Login page][login] | [Frontend auth browser tests][tBrowser] | Not yet verified; SRS acceptance pending |
| [NFR-MNT-001](SRS.md#nfr-mnt-001) | Separation of responsibilities | Maintainability | Must | Planned | [AuthController][controller]<br>[AuthService][service]<br>[UserRepository][repository]<br>[Typed Fetch client][client]<br>[Frontend dependencies and scripts][pkg]<br>[Backend dependencies][pom] | [AuthServiceTests][tService]<br>[AuthControllerTests][tController]<br>[Fetch client tests][tClient] | Not yet verified; SRS acceptance pending |
| [NFR-MNT-002](SRS.md#nfr-mnt-002) | Traceable requirement changes | Maintainability | Must | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending |
| [NFR-COMP-001](SRS.md#nfr-comp-001) | Supported browser and deployment compatibility | Compatibility | Should | Requires clarification | [Playwright configuration][testconfig]<br>[Development configuration][props]<br>[Production configuration][prodprops]<br>[SecurityConfig][security] | [SessionCookieTests][tCookie]<br>[Frontend auth browser tests][tBrowser] | Not yet verified; SRS acceptance pending; DEC-017, DEC-018 |
| [NFR-TEST-001](SRS.md#nfr-test-001) | Layered reproducible verification | Testability | Must | Planned | [Playwright configuration][testconfig]<br>[Frontend integration guide][webdoc]<br>[Backend session guide][apidoc] | [RegisterRequestTests][tReg]<br>[AuthServiceTests][tService]<br>[AuthControllerTests][tController]<br>[SessionAuthenticationTests][tSession]<br>[Frontend auth browser tests][tBrowser]<br>[Fetch client tests][tClient]<br>[Full application context test][tContext] | Not yet verified; SRS acceptance pending |
| [NFR-SCALE-001](SRS.md#nfr-scale-001) | Session deployment boundary | Scalability | Must | Requires clarification | [SecurityConfig][security]<br>[Development configuration][props]<br>[Backend session guide][apidoc] | None yet | Not yet verified; SRS acceptance pending; DEC-018 |
| [NFR-SCALE-002](SRS.md#nfr-scale-002) | Bounded collection processing | Scalability | Should | Planned | None; unimplemented | None yet | Not yet verified; SRS acceptance pending; DEC-010, DEC-018 |

## Use cases, decisions, and verification gaps

The SRS contains [13 use cases](SRS.md#8-use-cases) with related requirement IDs and [18 open decisions](SRS.md#12-open-decisions-and-assumptions). Those sections provide behavioral cross-links; the matrix above is the exhaustive FR/NFR inventory.

Current gaps are deliberate:

1. No profile, wardrobe, planning, recommendation, runtime AI, saved-outfit, or feedback source/test implementation exists. Directory names and landing imagery do not count as delivery.
2. Frontend browser tests mock the API. They do not prove live Spring/Neon connectivity, actual browser CORS/cookie enforcement in deployment, or external-service behavior.
3. The backend full-context test requires environment-dependent datasource setup. It was not run and no live database was contacted for this milestone.
4. Existing accessibility and responsive tests support the approved entry/auth baseline, not complete WCAG conformance, all target browsers, or future screens.
5. Performance/service-level targets, privacy retention, deployment topology, and multi-instance session continuity require decisions before verification can close.
6. Linked automated test definitions are coverage evidence, not an assertion that every acceptance clause is fully automated. Live, manual, and future tests remain identified in SRS section 10.

Before marking a future row Implemented, link real source and appropriate tests, resolve blocking decisions, and record the acceptance evidence. If a requirement is deferred or removed, retain its ID and explain the scope decision instead of silently dropping its row.

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
