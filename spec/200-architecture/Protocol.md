# AppMessage Protocol

## Principles

- Numeric keys generated from a single JSON definition.
- Compact integer enums.
- UTF-8 strings length-limited on phone.
- Protocol version in every handshake.
- Unknown optional keys ignored.
- Incompatible major versions produce a visible compatibility error.

## Message keys

| Key | Name | Type |
|---:|---|---|
| 0 | event | int32 |
| 1 | protocolMajor | int32 |
| 2 | protocolMinor | int32 |
| 3 | maneuver | int32 |
| 4 | distanceMeters | int32 |
| 5 | primaryText | string |
| 6 | secondaryText | string |
| 7 | etaEpochSeconds | int32 |
| 8 | stateTimestampSeconds | int32 |
| 9 | sessionId | int32 |
| 10 | flags | int32 |
| 11 | appVersion | string |
| 12 | errorCode | int32 |
| 13 | stopsRemaining | int32 (optional; protocol 1.1) |
| 14 | latestPhoneVersion | string (optional; protocol 1.2; sent with flag bit 4) |
| 15 | settingsRevision | integer 0–99 (protocol 1.3; settings messages only) |
| 16 | watchPlatform | integer (protocol 1.3; from the watch) |
| 17 | watchCapabilities | integer bitmask (protocol 1.3; from the watch) |
| 18–40 | settingAccent … settingShortcutTap | integer (protocol 1.3; one key per watch setting) |

The current protocol version is 1.3. Minor versions only add optional keys, flag bits and events, which an older peer ignores: a 1.2 watchapp ignores the phone's `SETTINGS`, and a 1.2 phone app ignores the settings keys in `WATCH_READY` and the watch's `SETTINGS`.

### Watch settings (protocol 1.3)

Keys 18–40 carry the watchapp's 23 synced settings in the watch's persist order (REQ-WATCH-025, REQ-ANDROID-021): accent, units, invert, glyph pack, arrow left, ETA mode, backlight, vibration pattern, vibration intensity, backlight colour, ETA label, early-warning distance, repeat, pattern and intensity, Touch, then the shortcut slots Up, Down, Swipe left, Swipe right, Swipe up, Swipe down and Tap. Values are the watch's enum indices; bools are 0/1. The watch writes them as uint8 and the phone as int32; receivers accept the width each side sends.

- `watchPlatform`: 0 unknown, 1 aplite, 2 basalt, 3 chalk, 4 diorite, 5 emery.
- `watchCapabilities`: bit 0 colour, bit 1 round, bit 2 RGB backlight, bit 3 touch.
- `settingsRevision`: moved on by one, wrapping 99 → 0, by every edit on its side. `a` is newer than `b` when `(a − b + 100) % 100` is 1–49; 0 is the same revision; 50 apart neither is newer and the watch's settings stand.

The watch sends every setting, its revision, platform and capabilities in its `WATCH_READY`, in a `SETTINGS` message after every edit on the watch, and again when the phone reconnects. The phone sends `SETTINGS` with its revision and only the edited settings, and only to a watchapp that is open; it never launches the watchapp for it. The watch applies a newer one (valid values only, changed values only, adopting the phone's revision) and always answers with one `SETTINGS` carrying its stored settings. The `WATCH_READY` the phone makes up itself carries no settings.

## Events

- `1 NAVIGATION_UPDATE`
- `2 NAVIGATION_STOPPED`
- `3 NO_ACTIVE_NAVIGATION`
- `4 SETTINGS` (protocol 1.3, both ways: the watch settings with their revision; see above)
- `10 WATCH_READY` (carries `protocolMajor`, `protocolMinor` and the watchapp's `appVersion`; the phone stores that version to spot a newer watchapp, REQ-ANDROID-019, with no protocol change; since 1.3 also the watch settings, platform and capabilities)
- `11 WATCH_REQUEST_STATE`
- `12 PHONE_COMPATIBILITY_ERROR`

## Flags

- bit 0: exit to watchface on stop;
- bit 1: vibrate on maneuver change (the watch plays it only while its New instruction pattern is Short pulse; Off is silent, REQ-WATCH-016);
- bit 2: activate backlight;
- bit 3: state is stale;
- bit 4: phone app update available (protocol 1.2): set on every state message while the phone knows of a newer phone app, with `latestPhoneVersion` (key 14, no leading `v`) on the same message (REQ-ANDROID-017, REQ-WATCH-023);
- remaining bits reserved.

## Delivery behavior

The phone caches exactly one latest state per active session. Failed delivery is retried with bounded exponential backoff while the watchapp is known ready. A newer state replaces an older pending state.
