# API Documentation

## AppMessage Protocol (phone <-> watch)

Source of truth: `protocol/protocol-definition.json` (protocol **1.1**). Generated into `android/.../protocol/Protocol.kt` (`Protocol.Keys`, `Events`, `FlagBits`, `FlagMasks`, `ManeuverCodes`, `ErrorCodes`) and `watchapp/src/c/generated/protocol.h` (`PBNTN_KEY_*`, `PBNTN_EVENT_*`, `PBNTN_FLAG_*_MASK`, ...). Regenerate with `scripts/generate-protocol.sh` (it first runs `check_consistency.py`). Never hand-edit the outputs. Normative description: `spec/200-architecture/Protocol.md` (MANIFEST-protected).

### Keys

| Key | Name | Type | Sent by |
|---:|---|---|---|
| 0 | EVENT | int32 | both |
| 1 | PROTOCOL_MAJOR | int32 | both |
| 2 | PROTOCOL_MINOR | int32 | both |
| 3 | MANEUVER | int32 | phone (update) |
| 4 | DISTANCE_METERS | int32 | phone (update) |
| 5 | PRIMARY_TEXT | string, max 64 UTF-8 bytes | phone (update) |
| 6 | SECONDARY_TEXT | string, max 24 UTF-8 bytes | phone (update) |
| 7 | ETA_EPOCH_SECONDS | int32 (optional) | phone (update) |
| 8 | STATE_TIMESTAMP_SECONDS | int32 | phone (update) |
| 9 | SESSION_ID | int32 | phone (update, stopped) |
| 10 | FLAGS | int32 bitfield | phone (update, stopped, none) |
| 11 | APP_VERSION | string | phone (every state message); watch (READY, `APP_VERSION_STR`) |
| 12 | ERROR_CODE | int32 | phone (compat error) |
| 13 | STOPS_REMAINING | int32 (optional, 1.1) | phone (transit update) |

The next free key is **14**. Keys and events must equal `examples/protocol-definition.json` (checked by `protocol/check_consistency.py`). That file is SHA-256 protected by `MANIFEST.sha256.json`, so a new key means editing both JSON files and the manifest hash in one commit.

### Events

`1 NAVIGATION_UPDATE`, `2 NAVIGATION_STOPPED`, `3 NO_ACTIVE_NAVIGATION` (phone to watch); `10 WATCH_READY`, `11 WATCH_REQUEST_STATE` (watch to phone); `12 PHONE_COMPATIBILITY_ERROR` (phone to watch, shown as "Update phone app").

### Flag bits (key 10)

| Bit | Name | Set by |
|---:|---|---|
| 0 | EXIT_TO_WATCHFACE_ON_STOP | `flagsForStopped` when `WatchSettings.exitToWatchfaceOnStop` |
| 1 | VIBRATE_ON_MANEUVER_CHANGE | `flagsForNavigating` on a maneuver change when the setting is on |
| 2 | ACTIVATE_BACKLIGHT | `flagsForNavigating` when `activateBacklight` |
| 3 | STATE_IS_STALE | `flagsForNavigating` when the state is older than 30 s |

Bits 4 and up are reserved. `flagBits` is outside the consistency check, so a new bit only changes `protocol/protocol-definition.json` (plus the protected `Protocol.md` text). `NO_ACTIVE_NAVIGATION` is always sent with `flags = 0` (`NavigationSessionReducer.sendCurrent`), and `PHONE_COMPATIBILITY_ERROR` carries no flags or version.

### Message shapes (`ProtocolCodec`)

- `encodeState(state, flags, appVersion)`: always MAJOR, MINOR, APP_VERSION, plus per-state fields (above). Null optional values are omitted.
- `encodeCompatibilityError(code)`: EVENT, MAJOR, MINOR, ERROR_CODE.
- `decodeInbound`: `WATCH_READY` becomes `ReducerEvent.WatchReady(major, minor)`, `WATCH_REQUEST_STATE` becomes `WatchRequestedState`, anything else becomes null.
- Wire mapping (`PebbleAppMessageMapper`): ints go out as `Int32`, strings as `Text`; inbound ints of any width are normalised, and bytes are dropped.

### Watch handling (`main.c` `inbox_received_handler`)

A message without EVENT is ignored. Any other message sets `s_heard_from_phone` and stops the watchdog, and `record_phone_version` (TUPLE_CSTRING, non-empty, capped at 16 bytes, persisted under key 16 only on change) runs before the event switch. Events: UPDATE goes to `render_navigation`; STOPPED goes to exit or "Arrived"; NONE shows "No navigation detected..."; COMPAT_ERROR shows "Update phone app".

## Watch-side Persist Keys

1-15: `theme.c` settings (accent through early-warning intensity). 16: `PERSIST_KEY_PHONE_VERSION` (`main.c`). The next free key is 17.

## App Glance (watch launcher)

`glance_reload_callback` (`main.c`, not on aplite) runs once in `deinit`. It adds a countdown slice (`glance_countdown_template`, expiring at the ETA) when navigating, then a non-expiring hint slice, `GLANCE_HINT "Navigate on phone"` (`glance_text.h`; about 18 characters fit).

## External HTTP API (outbound only)

| Call | Location | Contract |
|---|---|---|
| `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` | `update/ReleaseFetcher.kt` `GitHubReleaseFetcher` | Headers `Accept: application/vnd.github+json`, `User-Agent: PebbleNTN-Android`; 8 s timeouts; reads `tag_name` (for example `v0.0.70`); a non-200 response or any exception returns null. No auth and no user data. |

## Android Platform Interfaces

- `NavigationNotificationListenerService` (permission `BIND_NOTIFICATION_LISTENER_SERVICE`, not exported).
- `WatchListenerService` (exported, action `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH`, filters on watchapp UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`).
- `FileProvider` `${applicationId}.fileprovider` (diagnostic exports).
- Outbound intents: `ACTION_VIEW` of `update_releases_url` (`https://github.com/optio/PebbleNTN/releases/latest`) and `watchapp_store_url`; notification-listener settings.
- `<queries>` lists 25 navigation-app packages, which must match `rules/catalog/navigation-apps.json`.

## Internal Kotlin APIs Relevant to #94

- `UpdateCheckRepository(context, currentVersion, fetcher, now)`: `state: StateFlow<UpdateState(currentVersion, latestVersion?, checking)>` with `updateAvailable`; `autoCheckEnabled: StateFlow<Boolean>`; `setAutoCheckEnabled`; `suspend checkForUpdate(force): UpdateCheckOutcome {SKIPPED_NOT_DUE, UP_TO_DATE, UPDATE_AVAILABLE, FAILED}`. Prefs file `pebblentn_update_check` holds `last_check_millis` (not exposed), `latest_version` and `auto_check_enabled`. `CHECK_INTERVAL_MS` is 7 days.
- `AppVersions.isNewer(current, latest)` / `compare`: tolerates a leading `v`, uneven component counts and `-pre`/`+build` suffixes.
- `NavigationController(transport, scope, appVersion, ...)`: `onInstruction`, `onNavigationStopped`, `onSettingsChanged(WatchSettings)`, `onConnectionLost`, `status: StateFlow<WatchLinkStatus>`.
- `WatchTransport`: `inbound`, `launchApp()`, `send(AppMessage): SendResult`, `watchLink()`, `linkDiagnostics()`.

Rule JSON contracts are `schemas/ruleset.schema.json` and `schemas/app-catalog.schema.json` (see `spec/300-data/RulesJSON.md`). The diagnostic export format is in `spec/300-data/ExportFormat.md`.
