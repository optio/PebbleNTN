# Emulator and GPS Workflow

## Android emulator

Use a Google Play-enabled system image so Google Maps can be installed and updated. Create and start AVDs from scripts. Account sign-in remains a manual local step and is never automated with stored credentials.

## GPS simulation

Support:
- Android Emulator Extended Controls route playback;
- GPX/KML import;
- `adb emu geo fix` for individual points;
- repository GPX fixtures containing no private route data.

## Google Maps capture procedure

1. Start clean AVD snapshot.
2. Install PebbleNTN debug build.
3. Grant notification-listener access manually or through documented test setup.
4. Enable Google Maps in Navigation Apps.
5. Start Google Maps navigation with a reproducible public route.
6. Play a matching emulator route.
7. Export selected captures in privacy-safe mode.
8. Convert captures to sanitized fixtures.
9. Never commit Google account data or private destinations.

## Synthetic publisher

The separate debug app SHALL:
- create/update/remove ongoing notifications;
- control title, text, big text, subtext, text lines, progress, category and channel;
- replay JSON fixture sequences;
- support rapid-update and malformed-input tests;
- use a debug-only package admitted by debug catalog configuration.

## Limitation

Android and Pebble emulators are not assumed to provide a production-equivalent PebbleKit Bluetooth path. Use a logging/fake watch transport for Android emulator tests and inject protocol messages into the Pebble emulator. Physical hardware is the release gate.
