# Project documentation

This directory covers the entire intended Personal Styling & Outfit Recommendation Application. Phase 0/1 authentication and Phase 2 onboarding/profile management are delivered; wardrobe and recommendations remain planned.

## Requirements baseline

- [Software Requirements Specification](requirements/SRS.md): product scope, 66 functional and 26 non-functional requirements, interfaces/data/rules, 13 use cases, and 18 decision entries (DEC-002 resolved; DEC-003/004 partly resolved).
- [Requirements traceability](requirements/requirements-traceability.md): all 92 FR/NFR IDs mapped to implementation/test evidence or explicitly pending verification.

Baseline 1.0 was prepared on 2026-10-16 after Phase 1. Revision 1.1 records Phase 2; revision 1.3, dated 2026-10-17, records the implemented owner-approved Phase 2E appearance extension. Future priorities and decisions remain proposals. Actual local verification is recorded separately from implementation status and production/Neon claims.

## Existing implementation and testing guides

- [Frontend authentication integration and testing](../apps/web/README.md): local setup, browser session behavior, automated tooling, and the live browser walkthrough.
- [Backend session authentication API and testing](../apps/api/SESSION_AUTH.md): endpoint contracts, cookie/CSRF/CORS behavior, PowerShell walkthrough, and security limitations.
- [Styling and appearance profile API](api/styling-profile.md): catalogs, snapshot/lifecycle, optional data and independent deletion, ownership, concurrency, onboarding compatibility, and browser consistency.
- [Phase 2 verification](testing/phase-2-verification.md): commands/results, isolated PostgreSQL and live-browser setup, and pending manual Neon checks.
- [Learning journal](learning/LEARNINGS.md): chronological concepts, observed challenges, verified results, and further study.
- [Original epics and user stories](<../Personal Styling App — Epics & User Stories 1.md>): informal whole-product roadmap, used as planning evidence rather than an approved specification.
- [Landing image notes](../apps/web/public/images/landing/README.md): replaceable local development imagery.

Source code takes precedence for the current implementation. Keep older milestone context identifiable, and update current contract/status notes when delivery changes them.

## Documentation categories

| Category | Purpose / current location |
| --- | --- |
| Requirements | SRS and traceability under requirements/ |
| Architecture | Future system boundaries, deployment, and architecture decisions; no new architecture document is created by this milestone |
| Design | Future approved flows, visual behavior, and interaction specifications; current UI is evidenced by source and tests |
| API | Session guide in apps/api/SESSION_AUTH.md and styling profile contract under api/ |
| Testing | Phase 2 reproducible verification under testing/, plus frontend/backend guides and source tests |
| Learning | Phase-by-phase technical journal under learning/ |

Only categories with actual documents are linked. Empty placeholder documents or directories are unnecessary.

## Keeping requirements maintainable

1. Resolve the relevant DEC decisions with the project owner before implementing uncertain rules or provider choices.
2. Preserve stable FR/NFR IDs. Add IDs for new requirements, and retire obsolete IDs explicitly rather than renumbering or reusing them.
3. State inputs, outputs, errors, acceptance criteria, priority, and status; label proposed behavior and unconfirmed assumptions.
4. Update the SRS and traceability together when scope or implementation changes. Link existing source/test paths and record actual verification outcomes separately from test presence.
5. Do not invent performance targets, ranking weights, media/retention policy, or provider contracts. Mark unresolved details and dependencies openly.
6. Review links, uniqueness, cross-document consistency, and secrets, then run git diff --check.
7. Keep credentials, real account examples, private images, session/CSRF values, and generated artifacts out of documentation and Git.

Documentation records approved implementation and verified outcomes; it does not authorize future scope, destructive schema changes, provider choices, or production deployment.
