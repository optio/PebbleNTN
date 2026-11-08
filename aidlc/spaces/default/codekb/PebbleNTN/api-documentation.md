# API Documentation

## AppMessage Protocol (phone <-> watch)

Source of truth: `protocol/protocol-definition.json` (protocol **1.3** at 61787f5, since #102). `protocol/generate.py` writes `android/.../protocol/Protocol.kt` (`Protocol.Keys`, `Events`, `FlagBits`, `FlagMasks`, `ManeuverCodes`, `ErrorCodes`, `CapabilityBits`, `CapabilityMasks`, `Platforms`) and `watchapp/src/c/generated/protocol.h` (`PBNTN_KEY_*`, `PBNTN_EVENT_*`, `PBNTN_FLAG_*`, `PBNTN_CAP_*`, `PBNTN_PLATFORM_*`), with a DO-NOT-EDIT banner; `--check` fails when the committed outputs differ. Regenerate with `scripts/generate-protocol.sh` (it first runs `check_consistency.py`). Never hand-edit the outputs. Normative description: `spec/200-architecture/Protocol.md` (MANIFEST-protected).

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
| 11 | APP_VERSION | string | phone (every state message); watch (real `WATCH_READY` only) |
| 12 | ERROR_CODE | int32 | phone (compat error) |
| 13 | STOPS_REMAINING | int32 (optional, 1.1) | phone (transit update) |
| 14 | LATEST_PHONE_VERSION | string, no leading `v` (optional, 1.2) | phone (while an update is known) |
| 15 | SETTINGS_REVISION | uint8 from the watch, int32 from the phone (1.3) | both (READY, SETTINGS) |
| 16 | WATCH_PLATFORM | uint8 (1.3): 0 unknown, 1 aplite, 2 basalt, 3 chalk, 4 diorite, 5 emery | watch |
| 17 | WATCH_CAPABILITIES | uint8 bits (1.3): 0 colour, 1 round, 2 RGB backlight, 3 touch | watch |
| 18-40 | SETTING_ACCENT .. SETTING_SHORTCUT_TAP | one per setting, in `SettingId` order (1.3) | watch (all 23), phone (edited ones) |

The next free key is **41**; free events are 5-9 and 13 and up; flag bits 5-31; capability bits 4-7. `check_consistency.py` requires `protocolMajor`, `protocolMinor`, `keys` and `events` to equal `examples/protocol-definition.json`, which `MANIFEST.sha256.json` protects, so a new key or a minor bump means editing both JSON files and the manifest hash in one commit. `flagBits`, maneuvers, error codes, capability bits and platforms are outside the check. The generator emits every entry as a plain constant; it has no notion of a key range. Both sides compute a setting's key as `SETTING_ACCENT + index` (`WatchSetting.key`, and `PBNTN_KEY_SETTING_ACCENT + i` in `settings_sync_glue.c`), guarded by `_Static_assert(PBNTN_KEY_SETTING_SHORTCUT_TAP - PBNTN_KEY_SETTING_ACCENT == SET_COUNT - 1)`, so new settings must take keys 41 and up, contiguous after 40.

### Events

`1 NAVIGATION_UPDATE`, `2 NAVIGATION_STOPPED`, `3 NO_ACTIVE_NAVIGATION` (phone to watch); `4 SETTINGS` (both ways, 1.3); `10 WATCH_READY`, `11 WATCH_REQUEST_STATE` (watch to phone; the watchapp never sends 11); `12 PHONE_COMPATIBILITY_ERROR` (phone to watch, shown as "Update phone app"). No event closes or hides the watchapp.

### Flag bits (key 10)

| Bit | Name | Set by |
|---:|---|---|
| 0 | EXIT_TO_WATCHFACE_ON_STOP | `flagsForStopped` when `WatchSettings.exitToWatchfaceOnStop` (fixed on); the watch then calls `window_stack_pop_all(true)` |
| 1 | VIBRATE_ON_MANEUVER_CHANGE | `flagsForNavigating` on a maneuver change (fixed on); the watch honours it only while its New instruction pattern is Short pulse |
| 2 | ACTIVATE_BACKLIGHT | `flagsForNavigating` when `activateBacklight` (fixed off) |
| 3 | STATE_IS_STALE | `flagsForNavigating` when the state is older than 30 s |
| 4 | PHONE_UPDATE_AVAILABLE | `ProtocolCodec.encodeState` when `latestPhoneVersion` is non-blank (#94) |

### Message shapes (`ProtocolCodec`)

- `encodeState(state, flags, appVersion, latestPhoneVersion = null)`: always MAJOR, MINOR, APP_VERSION; bit 4 + key 14 when an update is known; then the per-state fields. Null optional values are omitted.
- `encodeCompatibilityError(code)`: EVENT, MAJOR, MINOR, ERROR_CODE.
- `encodeSettings(revision, values)`: EVENT=4, SETTINGS_REVISION, then only the edited settings' keys.
- `decodeInbound`: `WATCH_READY` -> `ReducerEvent.WatchReady(major, minor)`, `WATCH_REQUEST_STATE` -> `WatchRequestedState`, anything else -> null.
- `watchAppVersion(message)`: key 11 of a `WATCH_READY`, trimmed and non-blank, else null.
- `decodeWatchSettings(message)`: for a READY or SETTINGS with a SETTINGS_REVISION, a `WatchSettingsModel` via `fromWatch` (missing or out-of-range values become defaults); null otherwise, including the made-up READY.
- `realWatchReadyMinor(message)`: the protocol minor of a READY that carries key 11, else null.
- Wire mapping (`PebbleAppMessageMapper`): ints go out as `Int32`, strings as `Text`; inbound ints of any width (the watch's uint8 settings included) become `Int`, bytes are dropped.

### Watch handling (`main.c` `inbox_received_handler`, 61787f5)

A message without EVENT is ignored. Any other message sets `s_heard_from_phone`, stops the watchdog, then `record_phone_version` (key 11 -> persist 16) and `record_update` (bit 4 + key 14 -> persist 17). Events: UPDATE -> `render_navigation` (buzz per the New instruction pattern, early warning, backlight); STOPPED -> exit when bit 0, else "Arrived" plus `maybe_show_update_notice`; NONE -> `update_idle_text` ("No navigation detected. If navigating, open PebbleNTN on your phone.", or the update variant) plus the notice; COMPAT_ERROR -> "Update phone app"; SETTINGS -> `settings_sync_apply`, and when the phone's revision is newer `on_settings_changed()` and `settings_window_refresh()`, then always `request_settings_send()`. Other events fall to `default: break`. Integer navigation tuples are read as `->value->int32` without a type check; settings tuples are read only when int32 of length 4 (`tuple_int`).

### Outbox and handshake (watch, `main.c`, 61787f5)

- `app_message_open(inbox max, outbox max)`; inbox received, outbox sent and outbox failed handlers are registered.
- Pending bits `OUT_READY` and `OUT_SETTINGS`; `outbox_flush` builds the message only when it goes out (READY: EVENT 10, MAJOR, MINOR, APP_VERSION; both: `settings_sync_write` = revision, platform, capabilities and 23 uint8 settings). READY settles a pending SETTINGS. A failure is retried once; the second failure holds the message until `app_connection_handler` sees the phone reconnect, which also queues a SETTINGS.
- READY is requested once, at the end of `init()`.

### Inbound parse (phone)

`WatchListenerService.onMessageReceived` forwards our UUID's dictionaries to `WatchInboundBus`; `onAppOpened` emits a synthetic READY with the phone's own MAJOR/MINOR and no key 11; `onAppClosed` emits on `WatchInboundBus.closed`. `NavigationController.handleInbound` decodes the reducer event, records a real READY's version, dispatches, then merges settings (`decodeWatchSettings`) or marks the watchapp outdated when a real READY reports a minor below 3. The reducer uses only the major.

## Watch-side Persist Keys and Settings (`settings_sync.c`, `theme.c`, `main.c`, `settings_sync_glue.c`, at 61787f5)

`SettingId` 0-22 are the 23 synced settings; `sync_persist_key(id)` is `id + 1` below `SET_TOUCH` and `id + 4` from it. Defaults are those of `kDefaults` and `sync_default`.

| Key | SettingId / name | Owner | Values (default **bold**) |
|---:|---|---|---|
| 1 | `SET_ACCENT` | theme.c | 16 accents, Green..Grey, Black (14), White (15); **Green** on colour, **Black** on black-and-white (only Black and White valid there) |
| 2 | `SET_UNITS` | theme.c | **0 metric**, 1 imperial |
| 3 | `SET_INVERT` (bool) | theme.c | **off** ("Dark on light"), on |
| 4 | `SET_GLYPH_PACK` | theme.c | **Classic**, Bold, Outline |
| 5 | `SET_ARROW_LEFT` (bool) | theme.c | **off** (arrow right), on |
| 6 | `SET_ETA_MODE` | theme.c | **Arrival time**, Time to arrival |
| 7 | `SET_BACKLIGHT` | theme.c | **Watch default**, Low 3 s, Medium 10 s, High until the app closes |
| 8 | `SET_VIBE_PATTERN` | theme.c | 0 Short pulse (the phone's bit 1), 1 Single, 2 Double, 3 Triple, 4 Long, **5 Off** |
| 9 | `SET_VIBE_INTENSITY` | theme.c | Light, **Medium**, Strong (pulse 100/220/400 ms) |
| 10 | `SET_BACKLIGHT_COLOR` | theme.c | **Watch default** and 10 tints; needs RGB backlight |
| 11 | `SET_ETA_LABEL` | theme.c | **labelled**, compact |
| 12 | `SET_WARN_DISTANCE` | theme.c | Off, 10, **25**, 50, 100, 200 m (30, 80, 160, 330, 650 ft labels in imperial) |
| 13 | `SET_WARN_REPEAT` | theme.c | Once, **Every update** |
| 14 | `SET_WARN_PATTERN` | theme.c | 1 Single, **2 Double**, 3 Triple, 4 Long (from one) |
| 15 | `SET_WARN_INTENSITY` | theme.c | **Medium** |
| 16 | phone version | main.c | string, 16 bytes at most |
| 17 | latest phone version | main.c | string (key 14 while bit 4 is set) |
| 18 | update notice shown | main.c | string |
| 19 | `SET_TOUCH` (bool) | theme.c | off, **on**; needs touch |
| 20-26 | `SET_SHORTCUT_FIRST + slot` | theme.c | 15 actions; defaults Up **ETA mode**, Down **Backlight now**, Swipe left and right **ETA mode**, Swipe up and down **None**, Tap **Backlight now**; gestures need touch, Next backlight colour needs RGB |
| 27 | last New instruction pattern | theme.c | not synced |
| 28 | settings revision | settings_sync_glue.c | 0-99 |

The next free key is **29** (`theme.c` header comment). A 24th setting appended after `SET_SHORTCUT_TAP` would get `sync_persist_key` 27 and a 25th 28, both taken, so the mapping must change for new ids. `settings_store` replaces an out-of-range value with the default, writes only a changed value, applies the backlight tint live and records the last audible vibration pattern; `settings_set` adds the edit handler; `settings_load` keeps the default for a missing or out-of-range stored value. Name helpers: `accent_name`, `units_name`, `glyph_pack_name`, `eta_mode_name`, `eta_label_style_name`, `backlight_mode_name`, `backlight_color_name`, `vibe_pattern_name`, `vibe_intensity_name`, `early_warn_distance_label`, `early_warn_repeat_name`, `shortcut_action_name`.

## Watch Settings Menu (`settings_window.c`, 61787f5)

Main menu rows (`MAIN_ROW_COUNT 11`): 0 About (subtitle = watchapp version; first, REQ-WATCH-020), 1 Backlight, 2 Vibration ("Double / Warn 50 m"), 3 Colour, 4 Invert, 5 ETA display, 6 ETA label, 7 Arrow side, 8 Glyph pack, 9 Distance units, 10 Shortcuts ("Up: ... / Down: ..."). Rows are drawn and selected through `kMainTitles` and `kMainCycles` (the setting a row cycles, plus one; 0 opens a door); Backlight cycles in place except on RGB hardware, where it opens a door (`BL_ROW_DURATION`, `BL_ROW_COLOUR`). Doors: Colour, Glyph pack (previews), Vibration (New instruction: Pattern, Strength; Early warning: Distance, then Repeat, Pattern, Strength while a distance is set), Backlight (RGB only), Shortcuts (Buttons; Touch on emery while `touch_service_is_enabled()`; one shared action list), About. `settings_window_push(on_change, about)` opens it; `settings_window_refresh()` pops any open door and reloads the top menu after a phone edit. `watchapp/tools/capture_screenshots.py` keeps a hand copy of the row indices.

## Watch Input Surface (navigation window, `main.c`)

- Buttons: SELECT opens settings; UP and DOWN run their shortcuts; BACK is the system default (the app exits).
- Touch, emery only: the bridge is on for the app and off for the navigation window, whose swipe and tap recognizers run shortcuts while Touch is on and `touch_service_is_enabled()`.
- Shortcut run: `shortcut_apply` -> `open_settings()` for Open settings, else `on_settings_changed()` and a 1.5 s banner.
- Timers and services: connection countdown and 5 s watchdog, backlight hold, the update notice's 10 s timer, minute tick, connection service (watchdog restart, SETTINGS on reconnect).
- Vibration: `render_navigation` plays the New instruction pattern on new information (changed maneuver or primary text), or the phone's bit 1 pulse when the pattern is Short pulse, or nothing when Off; then the early warning (`early_warn_should_fire`). Nothing vibrates when the watchapp is closed.

## Watch Update Notice Surface (#94, REQ-WATCH-023)

- `update_record`, `update_notice_action` (NONE | DEFER | SHOW), `update_idle_text` (`update_notice.h`; `UPDATE_VERSION_MAX 16`); texts `UPDATE_NOTICE_TEXT`, `UPDATE_IDLE_TEXT`, `IDLE_TEXT`.
- `update_notice_window_push`, `glance_update_subtitle(version)`.

## App Glance (watch launcher)

`glance_reload_callback` (not on aplite) runs once in `deinit`: a countdown slice to the ETA while navigating, then "Update: <version>" or `GLANCE_HINT`.

## Watch Version Compare Surface (`version_compare.h`, v0.0.73)

- `version_parse(v)`, `version_newer(a, b)`, `version_effective_update(reported, own, phone)`; `main.c` calls it as `effective_update()`.

## Phone-side Settings (61787f5)

**The phone's own `WatchSettings`** (`core/WatchSettings.kt`, `data/WatchSettingsRepository.kt`): `autoLaunchOnSessionStart` (default true, prefs `pebblentn_settings` key `watch_auto_launch`, dashboard Watch section toggle, read on callback threads by `isAutoLaunchEnabled()`; drives the reducer's `LaunchWatchApp` and the transport's relaunch), and three fixed flags with no storage or UI: `exitToWatchfaceOnStop` true, `vibrateOnManeuverChange` true, `activateBacklight` false. `AppContainer` forwards every emission to `NavigationController.onSettingsChanged`.

**The watchapp's settings mirror** (`core/WatchSettingsModel.kt`, `core/WatchSettingsSync.kt`, `data/WatchSettingsMirrorRepository.kt`):
- `WatchSetting` (23 entries, `valueCount`, `firstValue`, `needs`, `defaultValue`, `key`, `isShortcut`, `forKey`); `WatchSettingsRules.isValid` / `isOffered` / `defaultValue` / `options` (the watch's rules, Off first for the New instruction pattern); `WatchSettingsModel(values, revision, platform, capabilities)` with named getters (`units`, `earlyWarnDistance`, ...), `defaults(...)`, `fromWatch(...)`.
- `WatchSettingsSync`: `isNewer`, `next`, `decide(phoneRev, watchRev, differs, hasPending)` -> `ADOPT_WATCH | KEEP_PHONE | SAME`, `onWatchSettings`, `onPhoneEdit` (ignored before any watch settings or for a value the watch would not take), `outgoing`.
- `WatchSettingsSyncState(mirror?, pending, receivedFromWatch, watchappOutdated)`, stored by `WatchSettingsMirrorRepository` in prefs `pebblentn_watch_settings` (`revision`, `platform`, `capabilities`, `value_<setting>`, `pending`, `received_from_watch`, `watchapp_outdated`).
- `NavigationController.onPhoneSettingEdit(setting, value)`, `onWatchappClosed()`, `watchappOpen: StateFlow<Boolean>`; `AppContainer.setWatchSetting(setting, value)`.

## Watch Settings Screen (`ui/watchsettings/`, 61787f5)

- `WatchSettingsScreen(state, watchappOpen, onOpenSetting, onBack)`: status `WAITING` ("Open PebbleNTN on your watch to load its settings"), `OUTDATED` ("Update the watchapp to change these here") or `READY` (explanation, queued notice while edits are pending and the watchapp is closed, then the sections). Sections from `WatchSettingsUi.sections(model)`: Appearance, Arrival time, Distance (units), Backlight (colour only with RGB), Vibration, Early warning (other rows only once a distance is set), Shortcuts (gestures only while Touch is on).
- `OptionListScreen(setting, model, queued, onSelect, onBack)`: a radio list of `WatchSettingsUi.options`, a preview image above for the seven visual settings, swatches for the colour list.
- `WatchSettingsUi.title(setting)` (`@StringRes`) and `valueLabels(setting, imperial)` (`@ArrayRes`) are exhaustive `when`s over `WatchSetting`; distances use `watch_values_early_warn_distance_metric` or `_imperial` by the mirror's units. Routes `watch-settings` and `watch-settings/{setting}` in `MainActivity`; the dashboard's Watch section has the "Watch settings" row above the auto-launch toggle.

## Notification Snapshot Fields Relevant to Speed (#132, 61787f5)

`NotificationSnapshot` holds `packageName`, `notificationId`, `channelId`, `category`, `template`, `postTimeMillis`, `whenTimeMillis`, `title`, `text`, `subText`, `bigText`, `summaryText`, `infoText`, `progress` and `progressMax` (from `EXTRA_PROGRESS` / `EXTRA_PROGRESS_MAX`, only when present; documented as the Android 16 `ProgressStyle` whole-trip tracker, "NOT part of combinedText and not used for distance extraction"), `iconDrawable`, `iconMatch`. It is stored in debug history and exported (privacy-safe redacts letters of the text fields only, so "Arrive 12:45" survives and the progress integers are kept). `DebugCaptureProcessor.onPosted` passes only `evaluation.instruction` to `onMatchedInstruction`; the instruction's `distanceMeters` is quantized by the reducer before it is sent.

## Diagnostics Export (61787f5)

`ExportBundle { metadata: ExportMetadata (schema 5: mode, exportedAt, appVersion, appVersionCode, androidRelease, locale, userAddedPackages, install), userRules, events: [ExportedEvent] }`; events carry package, flags, disposition, matched rule, versions, occurrences with first and last seen times, the watch output and the first and last snapshots. `DiagnosticExporter.build(mode)` and `buildCapped(mode, maxBytes, onlyEventId)` take export-time inputs as constructor lambdas (`userAddedPackages`, `installRecord`) wired in `AppContainer`. There is no section for phone-side decisions; a new one would be a new metadata or top-level field, a schema bump to 6, a spec section in `spec/300-data/ExportFormat.md` and a REQ-DEBUG entry (next free REQ-DEBUG-016).

## External HTTP API (outbound only)

| Call | Location | Contract |
|---|---|---|
| `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` | `update/ReleaseFetcher.kt` `GitHubReleaseFetcher` | `Accept: application/vnd.github+json`, `User-Agent: PebbleNTN-Android`; 8 s timeouts; reads `tag_name`; a non-200 or exception returns null. No auth and no user data. Never on a Google Play install. |

Remote official rules (`update/RuleUpdateRepository.kt`) have no download code.

### Outbound links and drafts (user actions, at 6dec020)

| Action | Code | Destination and what it carries |
|---|---|---|
| Update from GitHub / F-Droid / Obtainium | `MainActivity.openUpdateSource` | GitHub `releases/latest`, the F-Droid page, Obtainium's launch intent or site |
| Report a bug / Request a feature / Suggest a name | `MainActivity.feedbackActions` | GitHub `issues/new` with app and Android versions; discussion #45 |
| Send by email (feedback) | `MainActivity.emailFeedback` | `mailto:` draft with app and Android versions |
| Share diagnostics | `DiagnosticShareManager` | the reviewed JSON export |
| Get the watchapp | `MainActivity` | `https://apps.repebble.com/ad4971e345854b909b73b1a9` |
| Get a navigation app | `MainActivity` | Google Play page of the entry's first package, or its `website` |

None of these sends anything by itself.

## Android Platform Interfaces

- `NavigationNotificationListenerService` (permission `BIND_NOTIFICATION_LISTENER_SERVICE`, not exported).
- `WatchListenerService` (exported, action `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH`, watchapp UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`): `onMessageReceived`, `onAppOpened` (synthetic READY), `onAppClosed` (#102).
- PebbleKit 2 sender (vendored): `sendDataToPebble`, `startAppOnTheWatch` (used by `launchApp` and the relaunch-and-retry) and `stopAppOnTheWatch` (unused). `PebbleInfoRetriever.getConnectedWatches()` gives watch name and firmware only.
- `FileProvider` `${applicationId}.fileprovider` (diagnostic exports).
- `<queries>` lists every catalog package (except the fixture publisher) plus the two Obtainium packages; `scripts/validate_catalog.py` fails when one is missing.
- Cross-package resources: `AppIconReferences` uses `createPackageContext` and `getIdentifier` on the posting navigation app.

## Navigation-App Catalog and Rule Package Contracts (at 1c7f4c1, extended by #113 at 2fa2aa5)

- **Catalog file** `rules/catalog/navigation-apps.json` (schema `schemas/app-catalog.schema.json`): `appId`, `displayName`, ordered `packageNames` (first is the main build), `hasOfficialRules`, `captureAvailable`, `defaultEnabled`, optional `rulesFrom`, `website` (https), `playListed`, `channelHints`; strict parse.
- **Rule package field**: every rule has `packageNames` (1-20); selection by family and `locales` only.
- **`NavigationAppCatalog`**: `parse`, `entryForPackage`, `allPackageNames`, `ruleFamilies()`, `forGooglePlay()`; `RuleFamilies.familyOf` / `applies`.
- **`RuleEngine.evaluate(snapshot, rules, locale, nowEpochSeconds)`**: first match in precedence order among rules applying to the package's family.
- **Consistency checks**: `NavigationAppCatalogTest` and `validate_catalog.py`.

## Large-Icon Recognition APIs (at 2fa2aa5)

- `LargeIconRecognizer.recognize(packageName, notification): IconMatch?`; `RuleDrivenLargeIconRecognizer`; `AppIconReferences.references(packageName, candidates)` (cached per package, version code, density); `IconCandidates.forPackage(rules, packageName, families)`; `IconMatcher.match(icon, references)` -> `Matched` or `NoMatch`.

## Internal Kotlin APIs: Update Check and Controller

- `UpdateCheckRepository(context, currentVersion, fetcher, now, installSource)`: `state` (`UpdateState` with `updateAvailable`, `effectiveLatest`, `watchAppVersion`), `recordWatchAppVersion`, `autoCheckEnabled`, `lastCheckMillis`, `isCheckStale`, `checkForUpdate(force)`.
- `AppVersions.isNewer` (tolerant) and `parseStrict` / `isNewerStrict` (strict, same rule as the watch).
- `NavigationController(transport, scope, appVersion, clock, maxSendAttempts = 3, baseBackoffMillis = 50, readySettleMillis = 1_500, stateStore, initialSettings, onWatchappReached, latestPhoneUpdate, onWatchAppVersion, watchSettings)`: `start`, `restore`, `onInstruction`, `onNavigationStopped`, `onSettingsChanged`, `onConnectionLost`, `onPhoneSettingEdit`, `onWatchappClosed`, `status: StateFlow<WatchLinkStatus(navigating, lastSent)>`, `watchappOpen`. Effects: `LaunchWatchApp` -> `transport.launchApp()` + autonomous READY after `readySettleMillis`; `SendState` -> `sendWithRetry` over `transport.send`; `SendCompatibilityError` -> `transport.send`.
- `WatchTransport`: `inbound`, `launchApp()`, `send`, `sendToOpenApp` (default `send`), `watchappClosed`, `linkDiagnostics()`, `watchLink()`. `FakeWatchTransport` records `launchCount`, `sent`, `sentToOpenApp`, scripts send results, and emits inbound messages and closes.

## Install Source and Export Metadata APIs (at 494a6dd)

- `enum class InstallSource { GITHUB, FDROID, OBTAINIUM, PLAY }`; `InstallSources.fromInstaller` and `detect`; `UpdatePolicy.forSource`; `updateCardModel`; `updatePrompt`. Since #119 an `InstallRecord` (`InstallRecords.detect`) feeds the "Phone app version" row and the export's `install` object (schema 5).

## Rule, Fixture and Route-Capture Contracts (at c7b6ba0, for #120)

- **Ruleset** (`schemas/ruleset.schema.json`): `schemaVersion` 1, `rulesetVersion`, `minimumAppVersionCode`, `createdAt`, `publisher`, `rules`. Rule: `id`, `enabled`, `priority`, `packageNames`, `locales`, `conditions` (operators `exists`, `notExists`, `equals`, `equalsIgnoreCase`, `contains`, `containsIgnoreCase`, `startsWith`, `endsWith`, `regex`, `in`; fields `packageName`, `title`, `text`, `subText`, `bigText`, `summaryText`, `infoText`, `category`, `channelId`, `template`, `combinedText`, `iconDrawable`), `output` (`maneuver`, `distanceMeters`, `primaryText`, `secondaryText`, `etaEpochSeconds`, `stopsRemaining`, from extractors `literal`, `regexCapture`, `fieldCopy`, `firstNonEmpty`, `distance`, `duration`, `maneuverMap`, `normalizeString`, `boundedJoin`). Rule fields do not include `progress`/`progressMax`. Order: layer, priority descending, id ascending.
- **Fixture file** (`rules/fixtures/<appId>.json`): fixtures with `snapshot` and `expected`.
- **Vela Maps notification** and **route-capture CLI**: as recorded for #120 (title `In <distance> · <instruction>`, text `<duration> · <distance> · Arrive <time>`; `routecap.py setup | fetch-routes | install | run | report`).

Rule JSON contracts are `schemas/ruleset.schema.json` and `schemas/app-catalog.schema.json` (see `spec/300-data/RulesJSON.md`). The diagnostic export format is in `spec/300-data/ExportFormat.md` (MANIFEST-protected, one section per schema version).
