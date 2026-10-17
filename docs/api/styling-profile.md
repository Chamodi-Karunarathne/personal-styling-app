# Styling and appearance profile APIs

Phase 2 and Phase 2E contracts, verified locally on 2026-10-17. Related requirements: FR-ONB-001 through FR-ONB-006 and FR-APPR-001 through FR-APPR-008; only profile capture, lifecycle, and editing are delivered. Recommendation consumption and enforcement remain planned. See the [SRS](../requirements/SRS.md) and [verification guide](../testing/phase-2-verification.md).

## Access and ownership

All endpoints below require the existing Spring Security session. Browser requests use `credentials: 'include'` and `cache: 'no-store'`. Ownership comes exclusively from `AccountPrincipal.id`; there is no owner selector in the body or response. A query parameter cannot select another account. Unknown top-level fields and preference categories, including owner IDs, are rejected.

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
  "updatedAt": null,
  "appearanceStepPending": false
}
```

- `null` means unanswered. `[]` means explicitly **No preference**. Removing the last selected choice returns the group to unanswered; it does not silently choose No preference.
- Ordinary answers are arrays of codes. Coverage is `null`, `[]`, or an array such as `[{"code":"COVER_KNEES","strength":"REQUIRED"}]`. Strength is PREFERENCE or REQUIRED.
- All personal answers are optional. No inference is made from gender, religion, activity, occasion, or transportation.
- Style, colors, fit, footwear, comfort, and lifestyle are soft preferences. Disliked colors are not automatically hard exclusions. This phase stores intent; no recommendation engine applies it yet.
- Stored steps remain STYLE, COMFORT, LIFESTYLE, REVIEW. The fifth UI step APPEARANCE uses stored REVIEW with `appearanceStepPending: true`; false means the actual review screen. Status is NOT_STARTED, IN_PROGRESS, SKIPPED, or COMPLETED.

## Full replacement write

Bootstrap CSRF through `GET /api/auth/csrf` and send its `X-XSRF-TOKEN` header with PUT or DELETE. Login/logout discard and refresh the shared in-memory token. Profile mutations do not rotate authentication state. CORS permits GET/POST/PUT/DELETE/OPTIONS for the single configured frontend origin.

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

Every preference key and `version` must be present, even when null. `intent`, `lastVisitedStep`, and `preferences` must be non-null. This is replacement, not PATCH; omissions must not erase existing answers accidentally. Send the last returned version for subsequent saves. The server returns the seven-field profile shape above with an advanced nonnegative version and timestamps. No user entity, email, password, hash, or session data is included. The optional additive request field `appearanceStepPending` defaults to false when omitted/null for old-client compatibility. True is allowed only with REVIEW and SAVE/SKIP; COMPLETE requires false. Existing clients can omit it; current clients preserve the returned value when editing styling preferences.

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

- `styling_profiles`: owner key, version, status, step, timestamps, and `appearance_step_pending` (non-null, default false).
- `styling_profile_answers`: answered categories, including explicitly empty groups.
- `styling_profile_choices`: category, option code, and strength.

The transactional [service](../../apps/api/src/main/java/com/chamodi/styling/profile/service/StylingProfileService.java) validates the entire replacement before changing managed state and flushes before returning. Only the known profile primary-key uniqueness race is translated to 409; unrelated integrity failures remain safe 500 errors. Existing account/email constraints are untouched. Hibernate `ddl-auto=update` remains the development strategy; no destructive migration, table recreation, or Flyway baseline is introduced.

## Browser consistency and limits

[SessionApiClient](../../apps/web/src/lib/api/session-client.ts) owns one queue/CSRF cache for auth and profiles. Login/logout invalidate account scopes synchronously before queueing. Bound profile clients check immutable account/generation before dispatch, after awaits, and before accepting results. They recheck `/api/auth/me` before profile reads and every PUT attempt. Stale queued work cannot adopt a newer managed session.

After an uncertain PUT, the workspace performs one GET. It confirms only a newer version with matching complete preferences, step, and resulting status; order is ignored, but unanswered/No preference and coverage strength remain distinct. Otherwise edits remain and further saves require explicit reload. A lost response is not treated as proof of rollback or success.

Same-tab managed requests are serialized. Cross-tab cookie changes are detected on focus or the ownership preflight; there is no immediate broadcast or atomic browser cookie compare-and-write. Another tab can change cookies after a check. Spring Security always authorizes the actual request's current session. Broader cross-tab coordination and production HTTPS/browser coverage remain further work.

## Appearance profile

Phase 2E introduces a separate optional resource. It is not included in authentication responses or the session principal. Registration and GET never create an appearance record. An explicit all-null PUT is valid and creates a configured, empty record; a missing record instead has `version: null` and `updatedAt: null`.

| Endpoint | Success | Contract |
| --- | --- | --- |
| `GET /api/profile/appearance` | 200 | `{version, details, updatedAt, options}`; virtual empty when absent |
| `PUT /api/profile/appearance` | 200 | Complete `{version, details}`; creates with null version, replaces with observed UUID |
| `DELETE /api/profile/appearance` | 204, empty body | JSON `{version}`; matching UUID required for an existing record; absent record succeeds idempotently |

The public `version` is an opaque UUID concurrency tag, **not** an authentication credential. The existing Spring session authenticates the user. DELETE carries the observed version in its JSON body, with `Content-Type: application/json` and CSRF header, never in the URL. A missing version property is malformed JSON-contract input (400); null is valid for first creation or an absent delete, but conflicts with an existing record (409). A stale UUID cannot delete or update a newer record.

Example first PUT (synthetic values; every key must be present):

```json
{
  "version": null,
  "details": {
    "heightCm": 172,
    "garmentSizeNote": null,
    "bodyProportions": null,
    "skinTone": null,
    "skinUndertone": "UNSURE",
    "paletteSuggestions": false,
    "hairColor": null,
    "hairTexture": null,
    "hairLength": null,
    "faceShape": "NOT_SPECIFIED"
  }
}
```

The response returns these ten details, the current revision/updatedAt, and `options`: a map from the seven categorical field names below to arrays of `{code, label}`. The frontend renders this server catalog; it does not infer choices. To clear a field, PUT the full current snapshot with that field null. `false` for palette interest is an explicit answer, distinct from null. Blank sizing notes become null; surrounding note whitespace is stripped. Other codes are not normalized or inferred.

| Field | Allowed input / validation |
| --- | --- |
| heightCm | Null or whole centimeters, 50–260 inclusive; technical malformed-input guard, no ideal-height judgment |
| garmentSizeNote | Null or up to 60 UTF-16 code units before whitespace stripping; sizing labels only, no required measurement/history |
| bodyProportions | Null, BALANCED (Similar torso and leg lengths), LONGER_TORSO, LONGER_LEGS, UNSURE |
| skinTone | Null, VERY_LIGHT, LIGHT, MEDIUM, DEEP, VERY_DEEP, UNSURE |
| skinUndertone | Null, WARM, COOL, NEUTRAL, UNSURE |
| paletteSuggestions | Null, true, false; interest in future suggestions, no current engine behavior |
| hairColor | Null, BLACK, BROWN, BLONDE, RED, GREY, WHITE, OTHER, UNSURE |
| hairTexture | Null, STRAIGHT, WAVY, CURLY, COILY, UNSURE |
| hairLength | Null, SHAVED, SHORT, MEDIUM, LONG, UNSURE |
| faceShape | Null, OVAL, ROUND, SQUARE, OBLONG, HEART, DIAMOND, UNSURE, NOT_SPECIFIED (Prefer not to specify) |

Unknown keys, missing nullable detail keys, malformed revisions/types, and unsupported codes return structured 400 errors without submitted values. Known validation paths use `details.heightCm`, `details.garmentSizeNote`, or other field names. Authentication/CSRF failures remain 401/403, concurrency conflicts 409, and unexpected failures safe 500. A write without CSRF can fail 403 before validation/authentication. The UI retains failed edits; conflicts and uncertain writes lock further saves until explicit reload unless a reconciliation GET confirms the write. A failed reload displays its error and keeps edits available. Uncertain DELETE is confirmed only by GET returning absence; it is not blindly replayed.

### Independent persistence and privacy

[AppearanceProfile](../../apps/api/src/main/java/com/chamodi/styling/profile/appearance/entity/AppearanceProfile.java) maps to `appearance_profiles` using a shared `user_id` primary/foreign key directly to `app_users`, rather than styling_profiles. This allows appearance deletion without touching styling, and supports accounts with no styling/appearance row. Ten data columns are nullable. Internal JPA `@Version` detects races; a fresh public UUID revision on every save prevents an old snapshot from matching a deleted/recreated record. Created/updated instants use PostgreSQL microsecond precision.

[AppearanceProfileService](../../apps/api/src/main/java/com/chamodi/styling/profile/appearance/service/AppearanceProfileService.java) validates before mutation, writes/flushes in a transaction, and narrowly translates the known appearance primary-key race to 409. Concurrent first creation has one winner, one conflict; other integrity failures retain safe 500 behavior. Existing Hibernate `update` adds the table and resume flag without changing the original stored-step constraint or deleting data. Production migration policy remains open.

No extra body measurements, age, weight, date of birth, photographs, ratings, BMI, or inferred traits are collected. Existing fit preferences are reused. No complexion rule excludes clothes. These fields go only to the configured application API, never AI or external providers. DTO/entity string representations are redacted; existing request/JDBC/security logging restrictions remain. Auth responses still expose only id/fullName/email. Frontend personal details live in account-scoped memory, with no local/sessionStorage persistence.

### Five-step onboarding and compatibility

The UI sequence is **Style and colors → Comfort and clothing preferences → Lifestyle and occasions → Personal details and fit → Review and finish**. Back retains local edits. Next/Save and exit validate changed details; Skip this step discards local personal edits only after confirmation and never PUTs/DELETEs saved appearance. Finishing without appearance creates no appearance row. An appearance-load failure does not prevent skipping/finishing; review discloses that data could not be loaded.

Original REVIEW drafts remain on Review because the new resume flag defaults false. Original completed profiles remain completed; no re-onboarding or dashboard gate is introduced. New personal-step drafts store REVIEW plus `appearanceStepPending: true`; saving actual Review/Finish resets it false. Existing drafts/answers are retained, and the original four-value database step constraint is unchanged.

Appearance and styling have **separate transactions and endpoints**. Changed appearance saves first; styling progress saves second. If the second fails, the UI says personal details were saved and onboarding progress could not be confirmed, retains remaining edits, and requires safe reload/review when appropriate. Recovery does not resend the already-confirmed appearance write. A failed first write does not send the styling write. Review and `/profile` reuse the same accessible fields and account-bound transport; future outfit matching remains unimplemented.
