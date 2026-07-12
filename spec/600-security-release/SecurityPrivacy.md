# Security and Privacy

## Data minimization

The listener receives system callbacks broadly, but the app checks `StatusBarNotification.packageName` before accessing content. Disabled packages are discarded immediately.

## User disclosure

Before opening notification-listener settings, show:

> PebbleNTN reads notifications from navigation apps you enable, extracts upcoming turn information, and sends it to your Pebble watch. Processing occurs on this device. Notification content is not uploaded automatically. Diagnostic files are shared only when you explicitly export them.

## Debug-data explanation

The export UI clearly states:

> Diagnostic notification content can contain only information that the navigation app placed in its Android notification. This can still include private information such as destinations, road names, saved-place labels, route context, and travel timing.

Do not imply that notification-derived information is harmless merely because it appeared in a notification.

## Remote official rules

- Disabled initially.
- HTTPS required.
- Signed canonical payload required.
- Embedded public verification keys only.
- Bounded size and complexity.
- Validate schema and semantic limits before activation.
- Run embedded self-tests.
- Atomic activation.
- Retain last-known-good version.
- No executable code or code-like expressions.

## Sharing

Use Android Sharesheet. Do not silently choose a recipient. Do not bundle a developer email address as an automatic destination; a prefilled subject is allowed. Files use temporary content URIs and expire.

## Secrets

No secrets belong in rules, Remote Config, source code, fixtures or CI logs.
