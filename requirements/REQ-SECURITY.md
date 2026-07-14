# Security and Privacy Requirements

## REQ-SEC-001 — Local processing
Notification parsing SHALL occur locally.

## REQ-SEC-002 — No automatic upload
Notification content and diagnostics SHALL never be uploaded automatically.

## REQ-SEC-003 — Minimal snapshot
The app SHALL serialize only the documented selected text fields and SHALL exclude PendingIntents, actions, RemoteViews, notification icons and images (small icon, large icon, big-picture) and arbitrary bundles. Notification icons SHALL NOT be extracted or forwarded to the watch; maneuver graphics use bundled glyph packs on the watch instead (REQ-WATCH-012). This keeps the data-handling posture minimal ("no data collected") and avoids reaching into notification imagery.

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
