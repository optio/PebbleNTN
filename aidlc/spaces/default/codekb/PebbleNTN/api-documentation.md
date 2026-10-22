# API Documentation

## AppMessage Protocol (phone <-> watch)

Source of truth: `protocol/protocol-definition.json` (protocol **1.2**, since #94). Generated into `android/.../protocol/Protocol.kt` (`Protocol.Keys`, `Events`, `FlagBits`, `FlagMasks`, `ManeuverCodes`, `ErrorCodes`) and `watchapp/src/c/generated/protocol.h` (`PBNTN_KEY_*`, `PBNTN_EVENT_*`, `PBNTN_FLAG_*_MASK`, ...). Regenerate with `scripts/generate-protocol.sh` (it first runs `check_consistency.py`). Never hand-edit the outputs. Normative description: `spec/200-architecture/Protocol.md` (MANIFEST-protected).

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
| 11 | APP_VERSION | string | phone (every state message); watch (real `WATCH_READY` only, `APP_VERSION_STR`; ignored by the phone today) |
| 12 | ERROR_CODE | int32 | phone (compat error) |
| 13 | STOPS_REMAINING | int32 (optional, 1.1) | phone (transit update) |
| 14 | LATEST_PHONE_VERSION | string, no leading `v` (optional, 1.2) | phone (every state message while an update is known) |

The next free key is **15**. Keys and events must equal `examples/protocol-definition.json` (checked by `protocol/check_consistency.py`). That file is SHA-256 protected by `MANIFEST.sha256.json`, so a new key means editing both JSON files and the manifest hash in one commit. Version comparison needs no new key: key 11 already flows both ways.

### Events

`1 NAVIGATION_UPDATE`, `2 NAVIGATION_STOPPED`, `3 NO_ACTIVE_NAVIGATION` (phone to watch); `10 WATCH_READY`, `11 WATCH_REQUEST_STATE` (watch to phone; the watchapp never sends 11 today); `12 PHONE_COMPATIBILITY_ERROR` (phone to watch, shown as "Update phone app").

### Flag bits (key 10)

| Bit | Name | Set by |
|---:|---|---|
| 0 | EXIT_TO_WATCHFACE_ON_STOP | `flagsForStopped` when `WatchSettings.exitToWatchfaceOnStop` |
| 1 | VIBRATE_ON_MANEUVER_CHANGE | `flagsForNavigating` on a maneuver change when the setting is on |
| 2 | ACTIVATE_BACKLIGHT | `flagsForNavigating` when `activateBacklight` |
| 3 | STATE_IS_STALE | `flagsForNavigating` when the state is older than 30 s |
| 4 | PHONE_UPDATE_AVAILABLE | `ProtocolCodec.encodeState` when `latestPhoneVersion` is non-blank (#94) |

`flagBits` is outside the consistency check. `PHONE_COMPATIBILITY_ERROR` carries no flags or version, and the watch's `record_update` ignores messages without FLAGS.

### Message shapes (`ProtocolCodec`)

- `encodeState(state, flags, appVersion, latestPhoneVersion = null)`: always MAJOR, MINOR, APP_VERSION. With a non-blank `latestPhoneVersion` (trimmed, leading `v` removed), bit 4 is ORed into FLAGS and key 14 is added; otherwise both are omitted. Then the per-state fields. Null optional values are omitted.
- `encodeCompatibilityError(code)`: EVENT, MAJOR, MINOR, ERROR_CODE.
- `decodeInbound`: `WATCH_READY` becomes `ReducerEvent.WatchReady(major, minor)` (key 11 dropped), `WATCH_REQUEST_STATE` becomes `WatchRequestedState`, anything else becomes null.
- Wire mapping (`PebbleAppMessageMapper`): ints go out as `Int32`, strings as `Text`; inbound ints of any width are normalised, and bytes are dropped.

### Watch handling (`main.c` `inbox_received_handler`)

A message without EVENT is ignored. Any other message sets `s_heard_from_phone` and stops the watchdog. `record_phone_version` (TUPLE_CSTRING, non-empty, capped at 16 bytes, persisted under key 16 only on change) and `record_update` (bit 4 + key 14 → `s_latest_phone_version`, key 17; bit 4 clear empties it) run before the event switch. Events: UPDATE goes to `render_navigation`. STOPPED goes to exit, or to "Arrived" plus `maybe_show_update_notice`. NONE shows `update_idle_text(latest)` plus `maybe_show_update_notice`. COMPAT_ERROR shows "Update phone app".

## Watch-side Persist Keys

1-15: `theme.c` settings (accent through early-warning intensity). 16: `PERSIST_KEY_PHONE_VERSION`. 17: `PERSIST_KEY_LATEST_PHONE_VERSION`. 18: `PERSIST_KEY_UPDATE_NOTICE_SHOWN` (all in `main.c`). The next free key is 19.

## Watch Update Notice Surface (#94, REQ-WATCH-023)

- `update_record(stored, len, flag_set, incoming)` / `update_notice_action(latest, shown, navigating, main_on_top)` → `NONE | DEFER | SHOW` / `update_idle_text(latest)` (`update_notice.h`; `UPDATE_VERSION_MAX 16`).
- Texts: `UPDATE_NOTICE_TEXT` "A phone app update is available. Open the mobile app to find the update.", `UPDATE_IDLE_TEXT`, `IDLE_TEXT`.
- `update_notice_window_push` (full-screen notice), and `glance_update_subtitle(version)` → "Update: <version>".

## App Glance (watch launcher)

`glance_reload_callback` (`main.c`, not on aplite) runs once in `deinit`. It adds a countdown slice (`glance_countdown_template`, expiring at the ETA) when navigating. It then adds a non-expiring slice: "Update: <version>" while `s_latest_phone_version` is set, else `GLANCE_HINT "Navigate on phone"`.

## External HTTP API (outbound only)

| Call | Location | Contract |
|---|---|---|
| `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` | `update/ReleaseFetcher.kt` `GitHubReleaseFetcher` | Headers `Accept: application/vnd.github+json`, `User-Agent: PebbleNTN-Android`; 8 s timeouts; reads `tag_name` (for example `v0.0.71`); a non-200 response or any exception returns null. No auth and no user data. |

## Android Platform Interfaces

- `NavigationNotificationListenerService` (permission `BIND_NOTIFICATION_LISTENER_SERVICE`, not exported).
- `WatchListenerService` (exported, action `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH`, filters on watchapp UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`). `onAppOpened` emits a synthetic WATCH_READY without key 11.
- `FileProvider` `${applicationId}.fileprovider` (diagnostic exports).
- Outbound intents: `ACTION_VIEW` of `update_releases_url` (`https://github.com/optio/PebbleNTN/releases/latest`, the only update destination today) and `watchapp_store_url`; notification-listener settings.
- `<queries>` lists 25 navigation-app packages, which must match `rules/catalog/navigation-apps.json`. Reading the app's own install source (`PackageManager.getInstallSourceInfo`, API 30+, minSdk is 31) needs no entry. No install-source code exists yet.
- PebbleKit 2 `PebbleInfoRetriever.getConnectedWatches()` gives the watch name and firmware version only, not the watchapp version.

## Internal Kotlin APIs Relevant to #94 and This Intent

- `UpdateCheckRepository(context, currentVersion, fetcher, now)`: `state: StateFlow<UpdateState(currentVersion, latestVersion?, checking)>` with `updateAvailable`; `autoCheckEnabled`; `lastCheckMillis: StateFlow<Long>`; `isCheckStale(now)` (over 30 days, or never); `setAutoCheckEnabled`; `suspend checkForUpdate(force): UpdateCheckOutcome {SKIPPED_NOT_DUE, UP_TO_DATE, UPDATE_AVAILABLE, FAILED}`. Prefs `pebblentn_update_check`: `last_check_millis`, `latest_version`, `auto_check_enabled`. `CHECK_INTERVAL_MS` 7 days, `STALE_AFTER_MS` 30 days.
- `AppVersions.isNewer(current, latest)` / `compare`: tolerates a leading `v`, uneven component counts and `-pre`/`+build` suffixes; a non-numeric component counts as 0 (it is not rejected).
- `updatePrompt(stale, autoCheckEnabled, staleCheckFinished)` → `UpdatePrompt {NONE, STALE_CHECK, WEEKLY_QUESTION}` (`ui/dashboard/UpdatePrompt.kt`).
- `NavigationController(transport, scope, appVersion, ..., onWatchappReached, latestPhoneUpdate: () -> String?)`: `onInstruction`, `onNavigationStopped`, `onSettingsChanged`, `onConnectionLost`, `status: StateFlow<WatchLinkStatus>`. `latestPhoneUpdate` is read on every SendState. AppContainer supplies `latestVersion` when `updateAvailable`.
- `DashboardScreen(updateAvailable, latestVersion, onDownloadUpdate, appVersion, updatePrompt, ...)`. The update card is a `PromptCard(title, body, action, onAction, primary, secondaryAction?, onSecondaryAction)` using strings `dashboard_update_title`, `dashboard_update_body` (`%1$s` latest, `%2$s` current, GitHub-specific text) and `dashboard_update_download`.
- `WatchTransport`: `inbound`, `launchApp()`, `send(AppMessage): SendResult`, `watchLink()`, `linkDiagnostics()`.

Rule JSON contracts are `schemas/ruleset.schema.json` and `schemas/app-catalog.schema.json` (see `spec/300-data/RulesJSON.md`). The diagnostic export format is in `spec/300-data/ExportFormat.md`.
