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

The current protocol version is 1.2. Minor versions only add optional keys and flag bits, which an older peer ignores.

## Events

- `1 NAVIGATION_UPDATE`
- `2 NAVIGATION_STOPPED`
- `3 NO_ACTIVE_NAVIGATION`
- `10 WATCH_READY` (carries `protocolMajor`, `protocolMinor` and the watchapp's `appVersion`; the phone stores that version to spot a newer watchapp, REQ-ANDROID-019, with no protocol change)
- `11 WATCH_REQUEST_STATE`
- `12 PHONE_COMPATIBILITY_ERROR`

## Flags

- bit 0: exit to watchface on stop;
- bit 1: vibrate on maneuver change;
- bit 2: activate backlight;
- bit 3: state is stale;
- bit 4: phone app update available (protocol 1.2): set on every state message while the phone knows of a newer phone app, with `latestPhoneVersion` (key 14, no leading `v`) on the same message (REQ-ANDROID-017, REQ-WATCH-023);
- remaining bits reserved.

## Delivery behavior

The phone caches exactly one latest state per active session. Failed delivery is retried with bounded exponential backoff while the watchapp is known ready. A newer state replaces an older pending state.
