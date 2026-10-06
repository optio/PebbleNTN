# Security and Privacy Requirements

## REQ-SEC-001 — Local processing
Notification parsing SHALL occur locally.

## REQ-SEC-002 — No automatic upload
Notification content and diagnostics SHALL never be uploaded automatically.

## REQ-SEC-003 — Minimal snapshot
The app SHALL serialize only the documented selected text fields — plus the documented numeric progress values (`EXTRA_PROGRESS` / `EXTRA_PROGRESS_MAX`), which are non-content, non-personal integers carrying no destination or identity — and SHALL exclude PendingIntents, actions, RemoteViews, notification icons and images (small icon, large icon, big-picture) and arbitrary bundles. Notification icons SHALL NOT be extracted or forwarded to the watch; maneuver graphics use bundled glyph packs on the watch instead (REQ-WATCH-012). One narrow exception exists for apps that draw the direction only as an icon (CoMaps, Organic Maps, Google Maps' classic card, #74): for an allowlisted package whose enabled rules map icons (a `maneuverMap` on the `iconDrawable` field), the app MAY read the large icon transiently, on the device, solely to compare it with that navigation app's own turn drawables. Only the matched drawable's resource name (e.g. `ic_turn_left`, non-personal) and a short match diagnostic MAY be kept; the image itself SHALL NOT be stored, serialized, exported or sent anywhere. This keeps the data-handling posture minimal ("no data collected") and avoids reaching into notification imagery.

## REQ-SEC-004 — Disclosure
The app SHALL show a prominent notification-access disclosure before sending the user to system settings.

## REQ-SEC-005 — Remote authenticity
Downloaded official rules, when enabled in a later milestone, SHALL be obtained over HTTPS and verified with an embedded public key before activation.

## REQ-SEC-006 — Rollback
A downloaded ruleset SHALL not replace the last-known-good ruleset until schema validation, signature validation and self-tests pass.

## REQ-SEC-007 — Secrets
No secret SHALL be included in the app, repository, rules, fixtures or export examples.

## REQ-SEC-008 — Temporary exports
Export files SHALL use temporary content URIs and have a documented cleanup policy.
