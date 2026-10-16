# Project documentation

This directory contains the requirements baseline for the Personal Styling & Outfit Recommendation Application. It covers the entire intended product while separating the delivered Phase 0/Phase 1 authentication foundation from planned features. Phase 2 has not started.

## Requirements baseline

- [Software Requirements Specification](requirements/SRS.md): product scope, current implementation, 58 functional requirements, 24 non-functional requirements, interfaces, conceptual data, business rules, 13 use cases, verification strategy, and 18 open decisions.
- [Requirements traceability](requirements/requirements-traceability.md): all 82 FR/NFR IDs mapped to implementation/test evidence or explicitly pending verification.

Baseline 1.0 is dated 2026-10-16 and was prepared against main at fc2931b. It is a draft for owner review; proposed future priorities and acceptance criteria are not confirmed release commitments. Implemented status reflects inspected source, not newly executed application tests or a production certification.

## Existing implementation and testing guides

- [Frontend authentication integration and testing](../apps/web/README.md): local setup, browser session behavior, automated tooling, and the live browser walkthrough.
- [Backend session authentication API and testing](../apps/api/SESSION_AUTH.md): endpoint contracts, cookie/CSRF/CORS behavior, PowerShell walkthrough, and security limitations.
- [Original epics and user stories](<../Personal Styling App — Epics & User Stories 1.md>): informal whole-product roadmap, used as planning evidence rather than an approved specification.
- [Landing image notes](../apps/web/public/images/landing/README.md): replaceable local development imagery.

Source code takes precedence for the current implementation. The older backend guide's final scope note predates frontend integration; the SRS records that historical discrepancy without rewriting existing guides.

## Documentation categories

| Category | Purpose / current location |
| --- | --- |
| Requirements | SRS and traceability under requirements/ |
| Architecture | Future system boundaries, deployment, and architecture decisions; no new architecture document is created by this milestone |
| Design | Future approved flows, visual behavior, and interaction specifications; current UI is evidenced by source and tests |
| API | Existing backend contract guide remains in apps/api/SESSION_AUTH.md; future API documentation can be organized here when needed |
| Testing | Existing frontend/backend guides and source tests; future cross-feature plans can be organized here when needed |

Only categories with actual documents are linked. Empty placeholder documents or directories are unnecessary.

## Keeping requirements maintainable

1. Resolve the relevant DEC decisions with the project owner before implementing uncertain rules or provider choices.
2. Preserve stable FR/NFR IDs. Add IDs for new requirements, and retire obsolete IDs explicitly rather than renumbering or reusing them.
3. State inputs, outputs, errors, acceptance criteria, priority, and status; label proposed behavior and unconfirmed assumptions.
4. Update the SRS and traceability together when scope or implementation changes. Link existing source/test paths and record actual verification outcomes separately from test presence.
5. Do not invent performance targets, ranking weights, media/retention policy, or provider contracts. Mark unresolved details and dependencies openly.
6. Review links, uniqueness, cross-document consistency, and secrets, then run git diff --check.
7. Keep credentials, real account examples, private images, session/CSRF values, and generated artifacts out of documentation and Git.

The documentation milestone changes no application code, database schemas, or existing UI. It does not authorize implementation of Phase 2.
