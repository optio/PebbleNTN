# Google Play Data Safety — declared answers

Draft answers for the Play Console Data Safety form (spec/600-security-release/BuildRelease.md:
"Complete Data Safety accurately"). Confirm before submission.

## Summary

- **Does the app collect or share any of the required user data types?** No data is collected or
  shared **off the device**. All processing is on-device; nothing is transmitted automatically.
- **Is all user data encrypted in transit?** Not applicable — the app makes no automatic network
  transmissions of user data. (User-initiated diagnostic exports are shared by the user via the
  Android share sheet, outside the app's control.)
- **Do you provide a way for users to request data deletion?** Yes — all data is local; users can
  delete debug history and all local data in-app, or by uninstalling.

## Data types

| Data type | Collected | Shared | Purpose | Notes |
|---|---|---|---|---|
| App activity / other (notification-derived turn info) | On-device only | No | App functionality | Never leaves the device automatically; only selected fields; identifiers hashed. |
| Device or other IDs | No | No | — | Not collected. |
| Location | No | No | — | The app does **not** access GPS/location; it only reads text from navigation notifications. |
| Personal info / contacts / messages | No | No | — | Contact/people fields are explicitly excluded from the snapshot. |

## Notification access declaration

Justify the `BIND_NOTIFICATION_LISTENER_SERVICE` use as **core app functionality**: reading enabled
navigation-app notifications to render turn-by-turn guidance on a Pebble watch. Provide the prominent
in-app disclosure (shown in onboarding) and link the privacy policy (`docs/PRIVACY_POLICY.md`).

## Permissions

- `BIND_NOTIFICATION_LISTENER_SERVICE` — core functionality (read enabled navigation notifications).
- `POST_NOTIFICATIONS` — only in the debug `fixture-publisher` module (not the release app).
- Package `<queries>` — targeted visibility of the catalog navigation apps to detect installation;
  not QUERY_ALL_PACKAGES.
