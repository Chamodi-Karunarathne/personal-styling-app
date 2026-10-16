# Styling profile API

Phase 2 contract, verified on 2026-10-17. Related requirements: FR-ONB-001 through FR-ONB-006; only profile capture, lifecycle, and editing are delivered. Recommendation consumption and enforcement remain planned. See the [SRS](../requirements/SRS.md) and [verification guide](../testing/phase-2-verification.md).

## Access and ownership

All three endpoints require the existing Spring Security session. Browser requests use `credentials: 'include'` and `cache: 'no-store'`. Ownership comes exclusively from `AccountPrincipal.id`; there is no owner selector in the body or response. A query parameter cannot select another account. Unknown top-level fields and preference categories, including owner IDs, are rejected.

Use the [session API](../../apps/api/SESSION_AUTH.md) to register/sign in. Registration still returns 201 without signing in. Login still leads to Dashboard; incomplete onboarding never blocks it. Identity/password changes are outside this feature.

| Endpoint | Success | Purpose |
| --- | --- | --- |
| `GET /api/profile/options` | 200 | Supported catalog with stable codes and display labels |
| `GET /api/profile/me` | 200 | Current owner's saved profile, or virtual NOT_STARTED |
| `PUT /api/profile/me` | 200 | Atomically replace the full snapshot using SAVE, SKIP, or COMPLETE |

## Catalog

`GET /api/profile/options` returns `{ "groups": [...] }`. Each group has `key`, `label`, `allowsRequired`, and `options`; each option has `code`, `label`, and an optional hexadecimal `swatch` for colors. Only coverage allows Required. The response contains all ten groups, in this order:

| Key | Supported codes |
| --- | --- |
| styles | CASUAL, MINIMAL, SMART_CASUAL, FORMAL, SPORTY, STREETWEAR, VINTAGE |
| preferredColors / dislikedColors | BLACK, WHITE, GREY, BEIGE, BROWN, NAVY, BLUE, GREEN, RED, PINK, PURPLE, YELLOW, ORANGE |
| fits | RELAXED, REGULAR, CLOSE_FITTING |
| footwear | TRAINERS, FLATS, SANDALS, BOOTS, HEELS |
| comfort | BREATHABLE, EASY_MOVEMENT, SOFT_FABRICS |
| coverage | COVER_SHOULDERS, COVER_KNEES, LONGER_SLEEVES |
| commonOccasions | WORK_STUDY, CASUAL_OUTINGS, FORMAL_EVENTS, ACTIVE_OUTDOOR |
| activities | MOSTLY_SEATED, WALKING, OUTDOOR_TIME |
| transportModes | WALKING, PUBLIC_TRANSPORT, CAR, CYCLING |

The server [PreferenceCatalog](../../apps/api/src/main/java/com/chamodi/styling/profile/service/PreferenceCatalog.java) is authoritative for integrated screens. The frontend initial catalog supports isolated previews/test fixtures, not a fallback that hides a failed server load. Changing catalog codes is a contract/data compatibility decision.

## Profile and snapshot semantics

A first GET returns the following representation **without inserting a row**:

```json
{
  "version": null,
  "onboardingStatus": "NOT_STARTED",
  "lastVisitedStep": "STYLE",
  "preferences": {
    "styles": null,
    "preferredColors": null,
    "dislikedColors": null,
    "fits": null,
    "footwear": null,
    "comfort": null,
    "coverage": null,
    "commonOccasions": null,
    "activities": null,
    "transportModes": null
  },
  "completedAt": null,
  "updatedAt": null
}
```

- `null` means unanswered. `[]` means explicitly **No preference**. Removing the last selected choice returns the group to unanswered; it does not silently choose No preference.
- Ordinary answers are arrays of codes. Coverage is `null`, `[]`, or an array such as `[{"code":"COVER_KNEES","strength":"REQUIRED"}]`. Strength is PREFERENCE or REQUIRED.
- All personal answers are optional. No inference is made from gender, religion, activity, occasion, or transportation.
- Style, colors, fit, footwear, comfort, and lifestyle are soft preferences. Disliked colors are not automatically hard exclusions. This phase stores intent; no recommendation engine applies it yet.
- Steps are STYLE, COMFORT, LIFESTYLE, REVIEW. Status is NOT_STARTED, IN_PROGRESS, SKIPPED, or COMPLETED.

## Full replacement write

Bootstrap CSRF through `GET /api/auth/csrf` and send its `X-XSRF-TOKEN` header with PUT. Login/logout discard and refresh the shared in-memory token. PUT does not rotate authentication state. CORS permits PUT alongside GET/POST/OPTIONS for the single configured frontend origin.

Example first save (synthetic preference values only):

```json
{
  "version": null,
  "intent": "SAVE",
  "lastVisitedStep": "COMFORT",
  "preferences": {
    "styles": ["CASUAL"],
    "preferredColors": ["NAVY"],
    "dislikedColors": null,
    "fits": [],
    "footwear": null,
    "comfort": ["EASY_MOVEMENT"],
    "coverage": [{"code":"COVER_KNEES","strength":"REQUIRED"}],
    "commonOccasions": null,
    "activities": null,
    "transportModes": ["PUBLIC_TRANSPORT"]
  }
}
```

Every preference key and `version` must be present, even when null. `intent`, `lastVisitedStep`, and `preferences` must be non-null. This is replacement, not PATCH; omissions must not erase existing answers accidentally. Send the last returned version for subsequent saves. The server returns the same six-field profile shape with an advanced nonnegative version and timestamps. No user entity, email, password, hash, or session data is included.

| Intent | Result |
| --- | --- |
| SAVE | First save creates IN_PROGRESS; resumes SKIPPED; preserves COMPLETED on later edits |
| SKIP | Saves current choices and marks SKIPPED; rejected with 400 after completion |
| COMPLETE | Atomically saves all choices and marks COMPLETED, including all-unanswered snapshots |

`completedAt` records the first explicit completion and survives later edits/repeated COMPLETE. Every accepted write advances version, including unchanged snapshots and collection-only edits. `updatedAt` is monotonic at PostgreSQL microsecond precision. First save initializes created/updated timestamps together.

## Validation and errors

The existing `ApiErrorResponse` shape is preserved: timestamp, status, error, message, path, fieldErrors. No submitted values, stack traces, SQL, or constraint names are returned.

| Status | Meaning / client handling |
| --- | --- |
| 400 | Malformed/incomplete JSON, unknown fields/enums/codes, duplicates, negative version, missing strength, contradictory preferred/disliked colors, or invalid transition; retain edits and display fields |
| 401 | Missing/expired authentication; restore/reconcile session and return to sign-in if needed |
| 403 | CSRF or access rejection; only the recognized CSRF message permits one bootstrap/retry |
| 409 | Genuine stale version, optimistic update race, or simultaneous first-save race; retain draft and explicitly reload before saving again |
| 500 | Unexpected failure; safe message; no automatic PUT replay |

An anonymous PUT without valid CSRF can receive 403 before authentication; an anonymous PUT with valid CSRF receives 401. Field keys include `preferences.dislikedColors` or indexed nested validation paths. The frontend maps known nested fields to their preference group and routes wizard errors to the affected step.

## Persistence and concurrency

[StylingProfile](../../apps/api/src/main/java/com/chamodi/styling/profile/entity/StylingProfile.java) has a shared primary key/foreign key to `app_users`: one account owns zero or one profile. The three tables are:

- `styling_profiles`: owner key, version, status, step, and timestamps.
- `styling_profile_answers`: answered categories, including explicitly empty groups.
- `styling_profile_choices`: category, option code, and strength.

The transactional [service](../../apps/api/src/main/java/com/chamodi/styling/profile/service/StylingProfileService.java) validates the entire replacement before changing managed state and flushes before returning. Only the known profile primary-key uniqueness race is translated to 409; unrelated integrity failures remain safe 500 errors. Existing account/email constraints are untouched. Hibernate `ddl-auto=update` remains the development strategy; no destructive migration, table recreation, or Flyway baseline is introduced.

## Browser consistency and limits

[SessionApiClient](../../apps/web/src/lib/api/session-client.ts) owns one queue/CSRF cache for auth and profiles. Login/logout invalidate account scopes synchronously before queueing. Bound profile clients check immutable account/generation before dispatch, after awaits, and before accepting results. They recheck `/api/auth/me` before profile reads and every PUT attempt. Stale queued work cannot adopt a newer managed session.

After an uncertain PUT, the workspace performs one GET. It confirms only a newer version with matching complete preferences, step, and resulting status; order is ignored, but unanswered/No preference and coverage strength remain distinct. Otherwise edits remain and further saves require explicit reload. A lost response is not treated as proof of rollback or success.

Same-tab managed requests are serialized. Cross-tab cookie changes are detected on focus or the ownership preflight; there is no immediate broadcast or atomic browser cookie compare-and-write. Another tab can change cookies after a check. Spring Security always authorizes the actual request's current session. Broader cross-tab coordination and production HTTPS/browser coverage remain further work.
