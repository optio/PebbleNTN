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
| 11 | APP_VERSION | string | phone (every state message); watch (real `WATCH_READY` only, `APP_VERSION_STR`; recorded by the phone as `watch_app_version` since v0.0.73) |
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
- `decodeInbound`: `WATCH_READY` becomes `ReducerEvent.WatchReady(major, minor)`, `WATCH_REQUEST_STATE` becomes `WatchRequestedState`, anything else becomes null.
- `watchAppVersion(message)`: key 11 of a `WATCH_READY`, trimmed and non-blank, else null (any other event, or no key 11). Not strictly parsed: a value such as `0.0.72-debug` is returned as is.
- Wire mapping (`PebbleAppMessageMapper`): ints go out as `Int32`, strings as `Text`; inbound ints of any width are normalised, and bytes are dropped.

### Watch handling (`main.c` `inbox_received_handler`)

A message without EVENT is ignored. Any other message sets `s_heard_from_phone` and stops the watchdog. `record_phone_version` (TUPLE_CSTRING, non-empty, capped at 16 bytes, persisted under key 16 only on change) and `record_update` (bit 4 + key 14 → `s_latest_phone_version`, key 17; bit 4 clear empties it) run before the event switch. Events: UPDATE goes to `render_navigation`. STOPPED goes to exit, or to "Arrived" plus `maybe_show_update_notice`. NONE shows `update_idle_text(effective_update())` plus `maybe_show_update_notice`. COMPAT_ERROR shows "Update phone app".

## Watch-side Persist Keys and Settings (`theme.h` / `theme.c`, `main.c`, at 3727ddc)

| Key | Name | Owner | Stored as | Values (default **bold**) | Setter / menu row |
|---:|---|---|---|---|---|
| 1 | `PERSIST_KEY_ACCENT` | theme.c | int `AccentId` | 0 Green, Blue, Red, Orange, Purple, Teal, Pink, Yellow, Cyan, Lime, Tan, Navy, Amber, Grey, 14 Black, 15 White (**Green** on colour, **Black** on black-and-white) | `settings_set_accent` / Colour door |
| 2 | `PERSIST_KEY_UNITS` | theme.c | int `UnitsId` | **0 metric**, 1 imperial | `settings_set_units` / Distance units (in place) |
| 3 | `PERSIST_KEY_INVERT` | theme.c | bool | **false** ("Dark on light"), true ("Light on dark") | `settings_set_inverted` / Invert (in place) |
| 4 | `PERSIST_KEY_GLYPH_PACK` | theme.c | int `GlyphPack` | **0 Classic**, 1 Bold, 2 Outline | `settings_set_glyph_pack` / Glyph pack door |
| 5 | `PERSIST_KEY_ARROW_LEFT` | theme.c | bool | **false** (arrow right, distance left), true | `settings_set_arrow_left` / Arrow side (in place) |
| 6 | `PERSIST_KEY_ETA_MODE` | theme.c | int `EtaMode` | **0 Arrival time**, 1 Time to arrival | `settings_set_eta_mode` / ETA display (in place) |
| 7 | `PERSIST_KEY_BACKLIGHT` | theme.c | int `BacklightMode` | **0 Watch default**, 1 Low (3 s), 2 Medium (10 s), 3 High (until the app closes) | `settings_set_backlight` / Backlight (in place, or the Duration row of the RGB door) |
| 8 | `PERSIST_KEY_VIBE_PATTERN` | theme.c | int `VibePatternId` | **0 Off**, 1 Single, 2 Double, 3 Triple, 4 Long | `settings_set_vibe_pattern` / Vibration door, New instruction |
| 9 | `PERSIST_KEY_VIBE_INTENSITY` | theme.c | int `VibeIntensity` | 0 Light, **1 Medium**, 2 Strong (pulse 100/220/400 ms) | `settings_set_vibe_intensity` / same |
| 10 | `PERSIST_KEY_BACKLIGHT_COLOR` | theme.c | int `BacklightColorId` | **0 Watch default**, White, Warm white, Red, Orange, Yellow, Green, Cyan, Blue, Purple, 10 Pink | `settings_set_backlight_color` (applies the tint at once) / Colour row of the backlight door, `PBL_RGB_BACKLIGHT` only |
| 11 | `PERSIST_KEY_ETA_LABEL_STYLE` | theme.c | int `EtaLabelStyle` | **0 labelled** ("ETA at / ETA in"), 1 compact ("ETA / IN") | `settings_set_eta_label_style` / ETA label (in place) |
| 12 | `PERSIST_KEY_EARLY_WARN_DISTANCE` | theme.c | int `EarlyWarnDistance` | **0 Off**, 10, 25, 50, 100, 200 m (labels 30, 80, 160, 330, 650 ft in imperial) | `settings_set_early_warn_distance` / Vibration door, Early warning |
| 13 | `PERSIST_KEY_EARLY_WARN_REPEAT` | theme.c | int `EarlyWarnRepeat` | **0 Once**, 1 Every update | `settings_set_early_warn_repeat` / same, shown only while a distance is set |
| 14 | `PERSIST_KEY_EARLY_WARN_PATTERN` | theme.c | int `VibePatternId` | 1 Single, **2 Double**, 3 Triple, 4 Long (never Off) | `settings_set_early_warn_pattern` / same |
| 15 | `PERSIST_KEY_EARLY_WARN_INTENSITY` | theme.c | int `VibeIntensity` | **Medium** | `settings_set_early_warn_intensity` / same |
| 16 | `PERSIST_KEY_PHONE_VERSION` | main.c | string, 16 bytes at most | last phone app version (key 11) | `record_phone_version` |
| 17 | `PERSIST_KEY_LATEST_PHONE_VERSION` | main.c | string | key 14 while bit 4 is set | `record_update` |
| 18 | `PERSIST_KEY_UPDATE_NOTICE_SHOWN` | main.c | string | version the notice was shown for | `maybe_show_update_notice` |

The next free key is **19** (the `theme.c` comment says so). Setters clamp an out-of-range id to the default, and `settings_load` keeps the default when the stored value is out of range. `settings_set_accent` accepts any id below 16 on every platform: only the Colour menu limits black-and-white watches to Black and White, through `accent_for_row` / `row_for_accent` over `ACCENT_AVAILABLE_COUNT` (16 on colour, 2 on black-and-white), and the renderer draws any other accent as black there. Preview helpers: `settings_vibe_play()` and `settings_early_warn_play()` (no-op when Off). Name helpers: `accent_name`, `units_name`, `glyph_pack_name`, `eta_mode_name`, `eta_label_style_name`, `backlight_mode_name`, `backlight_color_name`, `vibe_pattern_name`, `vibe_intensity_name`, `early_warn_distance_label`, `early_warn_repeat_name`.

## Watch Settings Menu (`settings_window.c`)

Main menu rows (`ROW_*`, `MAIN_ROW_COUNT 10`, mirrored by hand in `watchapp/tools/capture_screenshots.py`): 0 Backlight, 1 Vibration (subtitle "Double / Warn 50 m"), 2 Colour, 3 Invert, 4 ETA display, 5 ETA label, 6 Arrow side, 7 Glyph pack, 8 Distance units, 9 About (subtitle = watchapp version; REQ-WATCH-020 requires it last). The row set is the same on every platform; only what Backlight does differs (door on RGB hardware, in-place cycle elsewhere). Doors: Colour (16 or 2 rows), Glyph pack (3 rows with previews), Vibration (sections New instruction: `NEW_ROW_PATTERN`, `NEW_ROW_STRENGTH`; Early warning: `WARN_ROW_DISTANCE`, then `WARN_ROW_REPEAT`, `WARN_ROW_PATTERN`, `WARN_ROW_STRENGTH` only while a distance is set), Backlight (`BL_ROW_DURATION`, `BL_ROW_COLOUR` -> colour list; RGB only), About. `settings_window_push(on_change, about)` is the only entry point.

## Watch Input Surface (navigation window, `main.c`)

- Buttons: `click_config_provider` subscribes only `BUTTON_ID_SELECT` (`select_click_handler` opens Settings with an `AboutInfo` of `APP_VERSION_STR`, `s_phone_version` and `s_heard_from_phone`). UP and DOWN are not subscribed. BACK uses the system default, which pops the window and exits the app. The window has `load` and `unload` handlers and no `appear` or `disappear` handler.
- Touch, emery only (`#if defined(PBL_PLATFORM_EMERY)`): `init()` calls `app_touch_navigation_enable(true)` and `window_set_touch_bridge_disabled(s_window, true)` before the push. No `touch_service_subscribe`, recognizer or `touch_service_is_enabled()` call exists anywhere in the watchapp. The settings windows, About and the update notice window leave the bridge on.
- Timers and services: `s_connect_timer` (1 s countdown, 5 s handshake watchdog), `s_backlight_timer` (3 s / 10 s hold), the update notice window's 10 s timer, `tick_timer_service` at `MINUTE_UNIT` (strip redraw), `connection_service` (watchdog restart).
- Screens: one navigation window with panel, strip and road layers plus a full-screen message layer (`MSG_PLAIN`, `MSG_CONNECTING`, `MSG_QR`) that hides the others (`set_message_mode`), and the separate full-screen update notice window. No small overlay or timed toast layer exists.
- Feedback paths a gesture action can reuse: `on_settings_changed()` (background, `redraw_all`, `apply_backlight`); vibration on new information (`settings_vibe_play` when the pattern is not Off, otherwise the phone's flag bit 1 pulse `vibes_short_pulse` on a maneuver change), then the early warning (`early_warn_should_fire` -> `settings_early_warn_play`); backlight (`apply_backlight` while the Backlight setting is not Watch default, otherwise the phone's flag bit 2 `light_enable_interaction`).

## Watch Update Notice Surface (#94, REQ-WATCH-023)

- `update_record(stored, len, flag_set, incoming)` / `update_notice_action(latest, shown, navigating, main_on_top)` → `NONE | DEFER | SHOW` / `update_idle_text(latest)` (`update_notice.h`; `UPDATE_VERSION_MAX 16`).
- Texts: `UPDATE_NOTICE_TEXT` "A phone app update is available. Open the mobile app to find the update.", `UPDATE_IDLE_TEXT`, `IDLE_TEXT`.
- `update_notice_window_push` (full-screen notice), and `glance_update_subtitle(version)` → "Update: <version>".

## App Glance (watch launcher)

`glance_reload_callback` (`main.c`, not on aplite) runs once in `deinit`. It adds a countdown slice (`glance_countdown_template`, expiring at the ETA) when navigating. It then adds a non-expiring slice: "Update: <version>" while `effective_update()` is non-empty (key 14, or the watch's own newer version), else `GLANCE_HINT "Navigate on phone"`.

## Watch Version Compare Surface (`version_compare.h`, v0.0.73)

- `version_parse(v)`: optional `v`/`V`, then `.`-separated parts of 1-9 digits (`VERSION_PART_DIGITS_MAX`), nothing else; NULL and "" are unparsable.
- `version_newer(a, b)`: true only when both parse and `a` is higher, missing parts counting as 0.
- `version_effective_update(reported, own, phone)`: the higher of `reported` (key 14) and `own` when `version_newer(own, phone)`; `reported` when they cannot be compared; "" when there is no update. `main.c` calls it as `effective_update()` with `s_latest_phone_version`, `APP_VERSION_STR` and `s_phone_version` for the notice, the NO_ACTIVE_NAVIGATION idle text and the glance.

## External HTTP API (outbound only)

| Call | Location | Contract |
|---|---|---|
| `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` | `update/ReleaseFetcher.kt` `GitHubReleaseFetcher` | Headers `Accept: application/vnd.github+json`, `User-Agent: PebbleNTN-Android`; 8 s timeouts; reads `tag_name` (for example `v0.0.71`); a non-200 response or any exception returns null. No auth and no user data. |

## Android Platform Interfaces

- `NavigationNotificationListenerService` (permission `BIND_NOTIFICATION_LISTENER_SERVICE`, not exported).
- `WatchListenerService` (exported, action `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH`, filters on watchapp UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`). `onAppOpened` emits a synthetic WATCH_READY without key 11.
- `FileProvider` `${applicationId}.fileprovider` (diagnostic exports).
- Outbound intents: `MainActivity.openUpdateSource(source)` opens `update_releases_url` (GitHub `releases/latest`), `update_fdroid_url`, or Obtainium's launch intent (`InstallSources.OBTAINIUM_PACKAGES`) falling back to `update_obtainium_url`; also `watchapp_store_url` and notification-listener settings.
- `<queries>` lists 25 navigation-app packages, which must match `rules/catalog/navigation-apps.json`. `InstallSources.detect` reads the app's own install source (`PackageManager.getInstallSourceInfo`, API 30+, minSdk is 31) with no entry, once per app start in `AppContainer.installSource`.
- PebbleKit 2 `PebbleInfoRetriever.getConnectedWatches()` gives the watch name and firmware version only, not the watchapp version.

## Internal Kotlin APIs Relevant to #94, Version Compare and the Watchapp Version Row

- `UpdateCheckRepository(context, currentVersion, fetcher, now)`: `state: StateFlow<UpdateState(currentVersion, latestVersion?, checking, watchAppVersion?)>` with `updateAvailable` (GitHub newer or watch strictly newer) and `effectiveLatest` (the higher of the two, or null); `recordWatchAppVersion(version): Boolean` (trims, ignores blank, writes prefs key `watch_app_version` only on change, true only then; restored into the state at construction; never cleared); `autoCheckEnabled`; `lastCheckMillis: StateFlow<Long>`; `isCheckStale(now)` (over 30 days, or never); `setAutoCheckEnabled`; `suspend checkForUpdate(force): UpdateCheckOutcome {SKIPPED_NOT_DUE, UP_TO_DATE, UPDATE_AVAILABLE, FAILED}` (the outcome compares the GitHub tag only, not the watch version; `MainActivity.checkForUpdate` turns it into the toast `update_check_available` / `_up_to_date` / `_failed`). Prefs `pebblentn_update_check`: `last_check_millis`, `latest_version`, `auto_check_enabled`, `watch_app_version`. `CHECK_INTERVAL_MS` 7 days, `STALE_AFTER_MS` 30 days. A GitHub check keeps the stored watch version.
- `AppVersions.isNewer(current, latest)` / `compare`: tolerates a leading `v`, uneven component counts and `-pre`/`+build` suffixes; a non-numeric component counts as 0 (used for GitHub tags). `parseStrict(version)` / `isNewerStrict(current, candidate)` (v0.0.73): optional `v`, then dot-separated parts of 1-9 digits and nothing else; unparsable is never newer. Same rule as the watch's `version_parse`.
- `InstallSource {GITHUB, FDROID, OBTAINIUM}`; `InstallSources.fromInstaller(pkg)` (F-Droid clients `org.fdroid.fdroid`, `org.fdroid.basic`, `com.looker.droidify`, `com.machiav3lli.fdroid`; `dev.imranr.obtainium[.*]`; anything else or null is GitHub) and `detect(context)` (logs and falls back to GitHub on failure).
- `updateCardModel(source)` -> `UpdateCardModel(main, others, showFdroidDelay)` (`ui/dashboard/UpdateCard.kt`).
- `ProtocolCodec.watchAppVersion(message)` and `NavigationController(onWatchAppVersion: (String) -> Unit)`: called from `handleInbound` for every inbound message that yields a version, wrapped in `runCatching` with a warning log. AppContainer wires it to `updateCheckRepository.recordWatchAppVersion`.
- `updatePrompt(stale, autoCheckEnabled, staleCheckFinished)` → `UpdatePrompt {NONE, STALE_CHECK, WEEKLY_QUESTION}` (`ui/dashboard/UpdatePrompt.kt`).
- `NavigationController(transport, scope, appVersion, ..., onWatchappReached, latestPhoneUpdate: () -> String?)`: `onInstruction`, `onNavigationStopped`, `onSettingsChanged`, `onConnectionLost`, `status: StateFlow<WatchLinkStatus>`. `latestPhoneUpdate` is read on every SendState. AppContainer supplies `updateCheckRepository.state.value.effectiveLatest`.
- `DashboardScreen(accessGranted, lastEligibleAtMillis, ..., updateAvailable, latestVersion, installSource, onOpenUpdateSource, appVersion, watchLink, updatePrompt, ...)`: stateless, every parameter but the first two defaulted. `MainActivity` passes `updateState.updateAvailable`, `updateState.effectiveLatest` and (since v0.0.74) `updateState.watchAppVersion`, plus `updatePrompt(stale = isCheckStale(), autoCheckEnabled, staleCheckFinished)` and `updateNeverChecked = lastCheckMillis == 0L`. The update card is `UpdateAvailableCard(model, latestVersion, appVersion, onOpenSource)` over `PromptCard`, with strings `dashboard_update_title`, `dashboard_update_body`, `dashboard_update_from`, `dashboard_update_fdroid_delay`, `dashboard_update_other_sources`, `dashboard_update_pick_source`, `dashboard_update_source_*`.
- Status-section strings: `dashboard_section_status`, `dashboard_watch` / `dashboard_watch_checking` / `_no_companion` / `_not_connected` / `_connected`, `dashboard_last_notification`, `dashboard_last_eligible` / `_none`, `dashboard_last_sent` / `_none`, and (v0.0.74) `dashboard_watchapp_version`, `dashboard_watchapp_version_seen` ("%1$s (last seen)"), `dashboard_watchapp_version_none` ("Not received yet").
- Online-check strings that describe GitHub as the update source: `onboarding_disclosure`, `dashboard_check_update`, `dashboard_auto_update` / `_hint`, `update_check_available` / `_up_to_date` / `_failed`, `dashboard_stale_check_title` / `_body` / `_never_body` / `_action`, `dashboard_weekly_question_title` / `_body` / `_turn_on` / `_not_now` (strings.xml:9, 49-63).
- `WatchTransport`: `inbound`, `launchApp()`, `send(AppMessage): SendResult`, `watchLink()`, `linkDiagnostics()`.

Rule JSON contracts are `schemas/ruleset.schema.json` and `schemas/app-catalog.schema.json` (see `spec/300-data/RulesJSON.md`). The diagnostic export format is in `spec/300-data/ExportFormat.md`.
