# API Documentation

## AppMessage Protocol (phone <-> watch)

Source of truth: protocol/protocol-definition.json (protocol **1.1**). Generated into android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt and watchapp/src/c/generated/protocol.h by `scripts/generate-protocol.sh`; `--check` guards drift in CI and pre-commit. Shared sections must equal examples/protocol-definition.json (protocol/check_consistency.py). Contract text: spec/200-architecture/Protocol.md.

### Keys

| Key | Name | Type | Phone -> watch | Watch -> phone |
|---:|---|---|---|---|
| 0 | EVENT | int32 | all | all |
| 1 | PROTOCOL_MAJOR | int32 | state, compat error | WATCH_READY |
| 2 | PROTOCOL_MINOR | int32 | state, compat error | WATCH_READY |
| 3 | MANEUVER | int32 | NAVIGATION_UPDATE | - |
| 4 | DISTANCE_METERS | int32 | NAVIGATION_UPDATE | - |
| 5 | PRIMARY_TEXT | string (<= 64 B) | NAVIGATION_UPDATE | - |
| 6 | SECONDARY_TEXT | string (<= 24 B) | NAVIGATION_UPDATE | - |
| 7 | ETA_EPOCH_SECONDS | int32 (optional) | NAVIGATION_UPDATE | - |
| 8 | STATE_TIMESTAMP_SECONDS | int32 | NAVIGATION_UPDATE | - |
| 9 | SESSION_ID | int32 | UPDATE, STOPPED | - |
| 10 | FLAGS | int32 bitset | UPDATE, STOPPED, NO_ACTIVE | - |
| 11 | APP_VERSION | string | **every state message** (UPDATE, STOPPED, NO_ACTIVE) | WATCH_READY |
| 12 | ERROR_CODE | int32 | PHONE_COMPATIBILITY_ERROR | - |
| 13 | STOPS_REMAINING | int32 (optional, 1.1) | NAVIGATION_UPDATE | - |

### Events

| Code | Event | Direction | Watch behaviour |
|---:|---|---|---|
| 1 | NAVIGATION_UPDATE | phone -> watch | `render_navigation` |
| 2 | NAVIGATION_STOPPED | phone -> watch | exit to watchface if flag bit 0, else "Arrived" |
| 3 | NO_ACTIVE_NAVIGATION | phone -> watch | message screen |
| 10 | WATCH_READY | watch -> phone | sent once at `init` |
| 11 | WATCH_REQUEST_STATE | watch -> phone | defined, not sent by the current watchapp |
| 12 | PHONE_COMPATIBILITY_ERROR | phone -> watch | "Update phone app" |

Flag bits: 0 exit to watchface on stop, 1 vibrate on maneuver change, 2 activate backlight, 3 state is stale. Maneuver codes 0-12 (UNKNOWN to TRANSIT). Error codes: 0 NONE, 1 INCOMPATIBLE_PROTOCOL_MAJOR.

### Compatibility rules

- Unknown optional keys are ignored on both sides.
- The phone rejects a WATCH_READY with a different major (`NavigationSessionReducer.onWatchReady`) and sends PHONE_COMPATIBILITY_ERROR; that message carries no APP_VERSION (`ProtocolCodec.encodeCompatibilityError`).
- The watch asserts major 1 at compile time (`_Static_assert` in main.c).

### Intent note (#103)

The phone's version already reaches the watch in key 11 of every state message (`ProtocolCodec.encodeState`, value `BuildConfig.VERSION_NAME`), but the watch's `inbox_received_handler` (main.c) does not read it. Showing "last connected companion version" on About is therefore a watch-only change and needs no protocol change, consistent with how REQ-WATCH-011 to -022 keep watch features off the wire.

## Watch Internal API (C)

| Function / type | Contract |
|---|---|
| `AboutInfo { const char *app_version; bool heard_from_phone; }` | Snapshot handed from main.c to settings and About (about_window.h) |
| `void about_window_push(const AboutInfo *info)` | Copies `*info`, pushes the About window |
| `void settings_window_push(void (*on_change)(void), const AboutInfo *about)` | Pushes settings; About row subtitle is `about->app_version` |
| `const char *about_phone_status(bool pebble_app_connected, bool heard_from_phone)` | "Phone app connected" / "Phone app not answering" / "Pebble app not connected" |
| `void about_details(char *buf, size_t len, const char *app_version, int major, int minor, const char *phone_status)` | `"Version %s\nProtocol %d.%d\n%s"`, NULL-safe ("?" / ""), always NUL-terminated |
| `settings_*()` / `settings_set_*()` (theme.h) | Read and persist on-watch settings via `persist_*` keys 1-15 |

## Android Internal Seams

- `WatchTransport` (protocol/WatchTransport.kt): `inbound: Flow<AppMessage>`, `launchApp()`, `send(AppMessage): SendResult`, `linkDiagnostics()`, `watchLink(): Flow<WatchLink>`; production `PebbleWatchTransport`, test `FakeWatchTransport`.
- `ProtocolCodec`: `encodeState(state, flags, appVersion)`, `encodeCompatibilityError(code)`, `decodeInbound(message, at)`, `fitText(text, maxBytes)`.
- Android components (AndroidManifest.xml): `MainActivity`, `NavigationNotificationListenerService`, `WatchListenerService` (PebbleKit 2 listener), `FileProvider`; `<queries>` for 25 navigation packages; `INTERNET` permission.

## External HTTP

- `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` (update/ReleaseFetcher.kt) for the update check.
- Signed official ruleset download, verified with embedded Ed25519 keys (update/RulesetSignatureVerifier.kt). Automatic remote rule checks are off by default (spec/000-overview.md).

## Data Contracts

- Ruleset JSON: schemas/ruleset.schema.json (spec/300-data/RulesJSON.md); catalog JSON: schemas/app-catalog.schema.json.
- Diagnostic export: spec/300-data/ExportFormat.md (export/ExportBuilder.kt, format versions 1-3).
