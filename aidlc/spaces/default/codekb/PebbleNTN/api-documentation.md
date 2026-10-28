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

The next free key is **15**. Keys and events must equal `examples/protocol-definition.json` (checked by `protocol/check_consistency.py`). That file is SHA-256 protected by `MANIFEST.sha256.json`, so a new key means editing both JSON files and the manifest hash in one commit. Version comparison needs no new key: key 11 already flows both ways. Still free at 4779760: keys **15** and up, event values 4-9 and 13 and up, flag bits 5-31. The generator emits every entry of `keys`, `events`, `flagBits`, `maneuvers` and `errorCodes` as plain constants (`Protocol.Keys.X` / `PBNTN_KEY_X`); it has no notion of a key range, a type or an optional key, so a per-setting key block is just more entries. `watchapp/package.json` `messageKeys` holds only `"dummy"`: both sides use the numeric keys from `protocol.h` directly.

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

A message without EVENT is ignored. Any other message sets `s_heard_from_phone` and stops the watchdog. `record_phone_version` (TUPLE_CSTRING, non-empty, capped at 16 bytes, persisted under key 16 only on change) and `record_update` (bit 4 + key 14 → `s_latest_phone_version`, key 17; bit 4 clear empties it) run before the event switch. Events: UPDATE goes to `render_navigation`. STOPPED goes to exit, or to "Arrived" plus `maybe_show_update_notice`. NONE shows `update_idle_text(effective_update())` plus `maybe_show_update_notice`. COMPAT_ERROR shows "Update phone app". Any other event value falls to `default: break`, after the two records above have run, so an older watchapp ignores a new event such as #102's SETTINGS. Integer tuples are read as `->value->int32` without a type or length check; strings are checked for `TUPLE_CSTRING`.

### AppMessage buffers and handshake (watch, `main.c` `init()` / `send_ready()`)

- `app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum())`: both buffers at the platform maximum, allocated from the app heap. The actual byte counts are not in the code.
- Only `app_message_register_inbox_received` is registered: no outbox sent/failed handler, no inbox dropped handler.
- `send_ready()` runs once, at the end of `init()`: `app_message_outbox_begin` (returns quietly when not `APP_MSG_OK`), then int32 EVENT=10, MAJOR, MINOR and cstring APP_VERSION=`APP_VERSION_STR`. It is never resent: `app_connection_handler` only restarts the watchdog on a Pebble-app reconnect, and the watch never sends WATCH_REQUEST_STATE.
- Size today: dictionary header 1 byte plus 7 bytes per tuple header: 3 int32 tuples (33) + "0.0.76\0" (14) = 48 bytes. Adding 23 settings, a revision, a platform and a capability field as int32 tuples adds about 26 x 11 = 286 bytes (about 334 in total; about 256 with uint8 tuples), which a state message (about 250 bytes with the 64/24-byte texts) does not exceed by much.

### Inbound parse (phone)

`WatchListenerService.onMessageReceived` forwards our UUID's dictionaries; `PebbleAppMessageMapper.fromDictionary` turns every integer width into `AppMessage.Value.IntValue` and **drops byte-array tuples** (`AppMessage` has only int and string values), so a packed byte-array settings blob would need mapper and model changes. `NavigationController.handleInbound` calls `ProtocolCodec.decodeInbound` (WATCH_READY -> `ReducerEvent.WatchReady(major, minor)`, missing keys read as 0; unknown events -> null and are ignored) and `ProtocolCodec.watchAppVersion` (key 11 of a WATCH_READY). The phone also synthesizes WATCH_READY in two places with **its own** `Protocol.MAJOR`/`MINOR` and no key 11 (`scheduleAutonomousReady` 1.5 s after a launch; `WatchListenerService.appOpenedReadyMessage` on `onAppOpened`). The reducer uses only the major (mismatch -> PHONE_COMPATIBILITY_ERROR); the minor is carried but never read. Outbound, every value is sent as `Int32` or `Text`.

## Watch-side Persist Keys and Settings (`theme.h` / `theme.c`, `shortcuts.h`, `main.c`, at 4779760)

Rows 1-15 and 19-26 are the 23 user settings #102 would sync; 16-18 and 27 are internal records.

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
| 19 | `PERSIST_KEY_TOUCH` | theme.c | bool | **false** (Off), true | `settings_set_touch_enabled` / Shortcuts door, Touch row (emery while `touch_service_is_enabled()`) |
| 20-26 | `PERSIST_KEY_SHORTCUT_FIRST + slot` | theme.c | int `ShortcutAction`, slot order Up, Down, Swipe left, Swipe right, Swipe up, Swipe down, Tap | 0 None, 1 ETA mode, 2 ETA label, 3 Next colour, 4 Previous colour, 5 Invert, 6 Arrow side, 7 Next glyph pack, 8 Vibration on/off, 9 Early warning next, 10 Early warning previous, 11 Backlight now, 12 Next backlight colour (offered only with `PBL_RGB_BACKLIGHT`), 13 Units, 14 Open settings (`ACTION_COUNT` 15). Defaults: **None**, except Swipe left and Swipe right **1 ETA mode** | `settings_set_shortcut` / Shortcuts door -> action list |
| 27 | `PERSIST_KEY_VIBE_LAST` | theme.c | int `VibePatternId` 1-4 | last New instruction pattern that was not Off (Single when none) | written by `settings_set_vibe_pattern` only when it changes |

The next free key is **28** (the `theme.c` comment says so). Setters clamp an out-of-range id to the default, and `settings_load` keeps the default when the stored value is out of range. `settings_set_accent` accepts any id below 16 on every platform: only the Colour menu limits black-and-white watches to Black and White, through `accent_for_row` / `row_for_accent` over `ACCENT_AVAILABLE_COUNT` (16 on colour, 2 on black-and-white), and the renderer draws any other accent as black there. `shortcut_valid_action` likewise keeps action 12 (Next backlight colour) on watches without an RGB backlight, where the menu never offers it and `shortcut_apply` turns it into a no-op. The bool settings (invert, arrow side, Touch) are stored with `persist_write_bool` and read back unchecked. Preview helpers: `settings_vibe_play()` and `settings_early_warn_play()` (no-op when Off). Name helpers: `accent_name`, `units_name`, `glyph_pack_name`, `eta_mode_name`, `eta_label_style_name`, `backlight_mode_name`, `backlight_color_name`, `vibe_pattern_name`, `vibe_intensity_name`, `early_warn_distance_label`, `early_warn_repeat_name`.

## Watch Settings Menu (`settings_window.c`)

Main menu rows (`ROW_*`, `MAIN_ROW_COUNT 11` at 4779760, mirrored by hand in `watchapp/tools/capture_screenshots.py`): 0 Backlight, 1 Vibration (subtitle "Double / Warn 50 m"), 2 Colour, 3 Invert, 4 ETA display, 5 ETA label, 6 Arrow side, 7 Glyph pack, 8 Distance units, 9 Shortcuts (subtitle "Up: None / Down: None"), 10 About (subtitle = watchapp version; REQ-WATCH-020 requires it last). The row set is the same on every platform; only what Backlight does differs (door on RGB hardware, in-place cycle elsewhere). Doors: Colour (16 or 2 rows), Glyph pack (3 rows with previews), Vibration (sections New instruction: `NEW_ROW_PATTERN`, `NEW_ROW_STRENGTH`; Early warning: `WARN_ROW_DISTANCE`, then `WARN_ROW_REPEAT`, `WARN_ROW_PATTERN`, `WARN_ROW_STRENGTH` only while a distance is set), Backlight (`BL_ROW_DURATION`, `BL_ROW_COLOUR` -> colour list; RGB only), Shortcuts (section Buttons: Up, Down; section Touch on emery while `touch_service_is_enabled()`: the Touch row, then the five gesture rows while Touch is On; each slot opens the shared action list, which saves and pops), About. `settings_window_push(on_change, about)` is the only entry point; there is no call to refresh an open menu from outside, and the menus re-read the settings only when they redraw or reappear (`menu_layer_reload_data` in the `appear` handlers).

## Watch Input Surface (navigation window, `main.c`)

- Buttons (4779760): `click_config_provider` subscribes SELECT (`open_settings`, with an `AboutInfo` of `APP_VERSION_STR`, `s_phone_version` and `s_heard_from_phone`), UP and DOWN (`run_shortcut(settings_shortcut(SLOT_UP_BUTTON / SLOT_DOWN_BUTTON))`). BACK uses the system default (the app exits). The window has `load`, `disappear` (drops the glyph cache and the QR to fit aplite's heap under the menus) and `unload` handlers.
- Touch, emery only (`#if defined(PBL_PLATFORM_EMERY)`): `init()` calls `app_touch_navigation_enable(true)` and `window_set_touch_bridge_disabled(s_window, true)`; `window_load` attaches a four-direction swipe recognizer and a tap recognizer, whose handlers run the slot's shortcut only while `settings_touch_enabled() && touch_service_is_enabled()`. The settings windows, About and the update notice window leave the bridge on.
- Shortcut run: `run_shortcut(action)` -> `shortcut_apply(action, s_notice_buf, 32)` (setters, notice text) -> `open_settings()` for Open settings, else `on_settings_changed()` and `notice_show()` (a `TextLayer` banner over the road area, `NOTICE_MS` 1500).
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

## Phone-side Watch Settings (`core/WatchSettings.kt`, `data/WatchSettingsRepository.kt`, at 4779760)

| Field | Default | Stored | UI | Effect |
|---|---|---|---|---|
| `autoLaunchOnSessionStart` | true | prefs `pebblentn_settings`, key `watch_auto_launch` | Dashboard, Watch section `ToggleRow` (`dashboard_auto_launch`) | reducer `LaunchWatchApp`; transport relaunch-and-retry on `FailedDifferentAppOpen` |
| `exitToWatchfaceOnStop` | true | not stored | none | FLAGS bit 0 on every NAVIGATION_STOPPED |
| `vibrateOnManeuverChange` | true | not stored | none | FLAGS bit 1 on a maneuver change; the watch pulses only while its own New instruction pattern is Off |
| `activateBacklight` | false | not stored | none | FLAGS bit 2, so never sent; the watch uses it only while its Backlight is Watch default |

`WatchSettingsRepository` exposes `settings: StateFlow<WatchSettings>`, `autoLaunchEnabled: StateFlow<Boolean>`, a `@Volatile` cache read by `isAutoLaunchEnabled()` on callback threads, and `setAutoLaunch`. `AppContainer` forwards every `settings` emission to `NavigationController.onSettingsChanged`. The same `pebblentn_settings` SharedPreferences file is the convention for small settings; `androidx.datastore:datastore-preferences` is a declared `:app` dependency with no use in app code, and Room holds rules, debug events and the cached navigation state. Nothing on the phone knows any watchapp setting value, the watch's platform or its capabilities (PebbleKit 2 reports watch name and firmware only).

## Watch Preview Capture Surface (`watchapp/tools/capture_screenshots.py`, at 4779760)

- Platforms `basalt` 144x168, `chalk` 180x180, `emery` 200x228 (no aplite or diorite). Per platform: `pebble kill`, `pebble wipe`, `pebble install`, then `send-app-message` with a fixed right turn (`3=6`), 450 m (or 80 m), "Rue de la Loi", FLAGS 0, and an arrival epoch and "HH:MM" string both derived from the pinned clock + 25 min.
- Settings are reached by button clicks through the menu, from a tracked state and a hand-kept copy of the `ROW_*` indices (`ROW_SHORTCUTS 9`, `ROW_ABOUT 10`), the accent rows (identity order of `AccentId`) and the glyph-pack rows. The setting keys it can drive: `accent`, `pack`, `arrow_left`, `inverted`, `imperial`, `eta_duration`, `eta_compact`.
- Clock: `emu-set-time 12:35:00` before each shot, a 1.5 s wait for the minute tick, and `repair_clock_outliers`, which compares a shape mask of the clock region (`CLOCK_REGION`, `clock_mask` with a stdlib PNG decoder) and re-shoots shots that disagree with the majority. It does not mask or blank pixels.
- Output: `screenshots/<name>-<WxH>.png` (hyphenated names, unsuitable as Android resource names), straight from `pebble screenshot`, not resized or optimised. Navigation shots (`MAIN_SHOTS`, 18, plus 2 short-distance): the default; accents blue, red, orange, purple, cyan; inverted green/blue/red; arrow left green/blue and left+inverted red; bold and outline packs; imperial; ETA time to arrival; ETA label compact, and compact with time to arrival. Settings-screen shots: menu, scrolled menu, colour list (+scrolled, +white), glyph list, Shortcuts door and its action list, About.
- Missing for one image per value (#102 Previews): the accents teal, pink, yellow, lime, tan, navy, amber, grey, black and white; a per-value naming scheme; an output mode into `android/app/src/main/res/drawable-nodpi/`; optimisation. The default shot already stands for every default value (green, invert off, arrival time, labelled, arrow right, Classic, metric). The committed `screenshots/` folder has no `settings-shortcuts*` files yet, although the script writes them.

## External HTTP API (outbound only)

| Call | Location | Contract |
|---|---|---|
| `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` | `update/ReleaseFetcher.kt` `GitHubReleaseFetcher` | Headers `Accept: application/vnd.github+json`, `User-Agent: PebbleNTN-Android`; 8 s timeouts; reads `tag_name` (for example `v0.0.71`); a non-200 response or any exception returns null. No auth and no user data; the request reveals the phone's IP address to GitHub. Triggers: the manual row, the stale card, turning the weekly toggle on, and the start-up check while `auto_check_enabled`. |

It is the only network call at 6dec020. Remote official rules (`update/RuleUpdateRepository.kt`) have no download code: `tryActivate(envelopeJson)` verifies a JSON string it is handed (Ed25519, schema, self-tests), `enabled` defaults to false, `AppContainer` passes no public keys, and nothing in the app calls it.

### Outbound links and drafts (user actions, at 6dec020)

| Action | Code | Destination and what it carries |
|---|---|---|
| Update from GitHub | `MainActivity.openUpdateSource` | `https://github.com/optio/PebbleNTN/releases/latest` (`update_releases_url`) |
| Update from F-Droid | same | `https://f-droid.org/packages/com.pebblentn.app/` |
| Update from Obtainium | same | Obtainium's launch intent, else `https://obtainium.imranr.dev/` |
| Report a bug / Request a feature / Suggest a name | `MainActivity.feedbackActions` | GitHub `issues/new` with `app-version` (and `android-version` for bugs) in the query; discussion #45 |
| Send by email (feedback) | `MainActivity.emailFeedback` | `mailto:` draft to `share_logs_recipient` with app and Android versions |
| Share diagnostics by email / share sheet | `DiagnosticShareManager` | the reviewed JSON export, prefilled recipient for email, no recipient for the sheet |
| Get the watchapp | `MainActivity` | `https://apps.repebble.com/ad4971e345854b909b73b1a9` |
| Get a navigation app | `MainActivity` | `https://play.google.com/store/apps/details?id=<package>`, with the catalog entry's first package (`NavigationAppsScreen` `onGetApp(entry.packageNames.first())`) |

None of these sends anything by itself; the browser or mail app does once the user acts.

## Android Platform Interfaces

- `NavigationNotificationListenerService` (permission `BIND_NOTIFICATION_LISTENER_SERVICE`, not exported).
- `WatchListenerService` (exported, action `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH`, filters on watchapp UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`). `onAppOpened` emits a synthetic WATCH_READY without key 11.
- `FileProvider` `${applicationId}.fileprovider` (diagnostic exports).
- Outbound intents: `MainActivity.openUpdateSource(source)` opens `update_releases_url` (GitHub `releases/latest`), `update_fdroid_url`, or Obtainium's launch intent (`InstallSources.OBTAINIUM_PACKAGES`) falling back to `update_obtainium_url`; also `watchapp_store_url` and notification-listener settings.
- `<queries>` (AndroidManifest.xml:18-48 at 1c7f4c1) lists the 25 navigation-app packages of `rules/catalog/navigation-apps.json` (every catalog package except `com.pebblentn.fixturepublisher`, which shares the signing key) plus the two Obtainium packages; `scripts/validate_catalog.py` fails when a catalog package is missing. #113 adds `app.morphe.android.apps.maps` (26). `InstallSources.detect` reads the app's own install source (`PackageManager.getInstallSourceInfo`, API 30+, minSdk is 31) with no entry, once per app start in `AppContainer.installSource`.
- Cross-package resources (unchanged at 2fa2aa5; this intent adds a retry with the family's root package name in the same context): `AppIconReferences` calls `createPackageContext(packageName, 0)` and `Resources.getIdentifier(name, "drawable", packageName)` on the navigation app that posted the notification, and `getPackageInfo(packageName).longVersionCode` for its cache key; it needs that package in `<queries>`.
- PebbleKit 2 `PebbleInfoRetriever.getConnectedWatches()` gives the watch name and firmware version only, not the watchapp version.
- Android 16 (API 36) exposure at 6dec020 (targetSdk 35): `MainActivity` calls `enableEdgeToEdge()` and screens use `Scaffold` insets, with no edge-to-edge opt-out; no `onBackPressed` override (one Compose `BackHandler` in the rule editor); no `screenOrientation`/`resizeableActivity`; no foreground services; the one runtime receiver is `RECEIVER_NOT_EXPORTED`; outbound intents are implicit `ACTION_VIEW`/`ACTION_SEND(TO)`. The PebbleKit 2 binding to the companion (vendored) is the path to check on a device after raising targetSdk.

## Navigation-App Catalog and Rule Package Contracts (at 1c7f4c1)

- **Catalog file** `rules/catalog/navigation-apps.json` (schema `schemas/app-catalog.schema.json`): `{"schemaVersion": 1, "apps": [...]}`, at most 200 apps. Each app: `appId` (kebab-case, the settings key and the `rules/bundled/<appId>/` folder name), `displayName`, `packageNames` (1-20 non-empty strings, ordered: the first is the main build), `hasOfficialRules`, `captureAvailable`, `defaultEnabled`, optional `channelHints`; no other field (`additionalProperties: false`, and Kotlin parses with `ignoreUnknownKeys = false`). A package may belong to one app only. The Google Maps entry: `"appId": "google-maps"`, `"packageNames": ["com.google.android.apps.maps"]`, official rules, default enabled.
- **Rule package field** (schema `schemas/ruleset.schema.json`, MANIFEST-protected; `RuleValidator`): every rule has `packageNames`, 1-20 exact package names (REQ-RULE-003). Selection is by `packageNames` and `locales` only, never by file name (REQ-RULE-015).
- **`NavigationAppCatalog`**: `parse(text)` (throws `IllegalArgumentException` on an unsupported schema version, a duplicate `appId` or a package claimed twice, `SerializationException` on bad JSON or an unknown field); `entryForPackage(packageName): NavigationAppEntry?`; `allPackageNames: Set<String>`; top-level `supportedNotInstalled(catalog, installedAppIds)`.
- **`RuleEngine.evaluate(snapshot, rules, locale, nowEpochSeconds): RuleEvaluation`**: only rules with `snapshot.packageName in rule.packageNames` are evaluated or traced, at most `MAX_RULES_PER_PACKAGE` (500); first match in precedence order wins.
- **`AssetRuleRepository`**: `current(): LayeredRules` (bundled layer only; `AppContainer` adds the user and downloaded layers), `rulesetVersions(packageName): List<String>` (ruleset versions holding a rule for the package, in load order).
- **`IconCandidates.forPackage(rules, packageName): Map<String, String>`**: drawable name to maneuver from the `maneuverMap` extractors on `iconDrawable` in the enabled rules for that package; empty means the icon is never read.
- **Consistency checks**: `NavigationAppCatalogTest.everyBundledRuleCoversExactlyItsAppsPackages` (each rule's package set equals its app's), `captureOnlyMatchesTheBundledRules`, the package-to-appId lock table in `commonNavigationAppsAreDetectedAndCaptureOnly`, and `validate_catalog.py` (`<queries>` coverage, prints "AndroidManifest <queries> covers all N catalog packages."). Adding a package variant to an app touches the catalog entry, every rule of that app, `<queries>`, the lock table and the fixtures; see [code-quality-assessment.md](code-quality-assessment.md), "MicroG Maps findings".

## Large-Icon Recognition and Rule Family APIs (at 2fa2aa5)

- **`fun interface LargeIconRecognizer { fun recognize(packageName: String, notification: Notification): IconMatch? }`**: null means the package has no icon rule and the icon is not read. `NotificationSnapshotFactory.recognizeIcon` turns a thrown exception into `NoMatch("error: ...")`.
- **`RuleDrivenLargeIconRecognizer(context, rules: () -> LayeredRules, references: AppIconReferences = AppIconReferences(context), families: RuleFamilies = RuleFamilies.NONE)`**: one instance per process, `AppContainer.largeIconRecognizer` (`families = catalog.ruleFamilies()`, default `references`).
- **`AppIconReferences(context)`**: `references(packageName, candidates: Map<String, String>): List<IconReference>`, cached in a `ConcurrentHashMap` keyed by (package, versionCode, densityDpi, candidates), evicting older keys of the same package on a miss and caching empty results; `largeIconAlpha(context, notification): AlphaImage?` (companion). No constructor parameter for families or a lookup seam.
- **`IconCandidates.forPackage(rules, packageName, families = RuleFamilies.NONE): Map<String, String>`**: drawable to maneuver from the `iconDrawable` maneuver maps of the enabled rules that apply to the package's family; higher precedence wins a disagreement.
- **`IconMatcher.match(icon, references): IconMatch`**: `Matched(drawable, maneuver, distance, runnerUp)` or `NoMatch(reason)`; `NoMatch("no references")` when the list is empty. `SIZE` 32, `MAX_DISTANCE` 24, `MIN_MARGIN` 8.
- **`NavigationAppEntry`** gains `rulesFrom: String?`, `website: String?`, `playListed: Boolean = true` and `rulesAppId = rulesFrom ?: appId`. `parse` also rejects a `rulesFrom` that names itself, an unknown app, an app that itself uses `rulesFrom`, or one without its own rules, and a non-https `website`.
- **`NavigationAppCatalog.ruleFamilies(): RuleFamilies`** and **`forGooglePlay(): NavigationAppCatalog`** (unlisted `rulesFrom` apps join their target entry after its own packages; others are dropped; `website` cleared).
- **`RuleFamilies`**: `familyOf(pkg): Set<String>` (itself alone when unknown), `applies(pkg, rulePackages)`, `NONE`, `of(groups)` (a package in two groups throws). No root accessor; the set order follows catalog order.

## Internal Kotlin APIs Relevant to #94, Version Compare and the Watchapp Version Row

- `UpdateCheckRepository(context, currentVersion, fetcher, now)`: `state: StateFlow<UpdateState(currentVersion, latestVersion?, checking, watchAppVersion?)>` with `updateAvailable` (GitHub newer or watch strictly newer) and `effectiveLatest` (the higher of the two, or null); `recordWatchAppVersion(version): Boolean` (trims, ignores blank, writes prefs key `watch_app_version` only on change, true only then; restored into the state at construction; never cleared); `autoCheckEnabled`; `lastCheckMillis: StateFlow<Long>`; `isCheckStale(now)` (over 30 days, or never); `setAutoCheckEnabled`; `suspend checkForUpdate(force): UpdateCheckOutcome {SKIPPED_NOT_DUE, UP_TO_DATE, UPDATE_AVAILABLE, FAILED}` (the outcome compares the GitHub tag only, not the watch version; `MainActivity.checkForUpdate` turns it into the toast `update_check_available` / `_up_to_date` / `_failed`). Prefs `pebblentn_update_check`: `last_check_millis`, `latest_version`, `auto_check_enabled`, `watch_app_version`. `CHECK_INTERVAL_MS` 7 days, `STALE_AFTER_MS` 30 days. A GitHub check keeps the stored watch version.
- `AppVersions.isNewer(current, latest)` / `compare`: tolerates a leading `v`, uneven component counts and `-pre`/`+build` suffixes; a non-numeric component counts as 0 (used for GitHub tags). `parseStrict(version)` / `isNewerStrict(current, candidate)` (v0.0.73): optional `v`, then dot-separated parts of 1-9 digits and nothing else; unparsable is never newer. Same rule as the watch's `version_parse`.
- `InstallSource {GITHUB, FDROID, OBTAINIUM}` (no Play value at 6dec020: `com.android.vending` maps to GITHUB, and `InstallSourceTest` pins that); `InstallSources.fromInstaller(pkg)` (F-Droid clients `org.fdroid.fdroid`, `org.fdroid.basic`, `com.looker.droidify`, `com.machiav3lli.fdroid`; `dev.imranr.obtainium[.*]`; anything else or null is GitHub) and `detect(context)` (logs and falls back to GitHub on failure).
- `updateCardModel(source)` -> `UpdateCardModel(main, others, showFdroidDelay)` (`ui/dashboard/UpdateCard.kt`).
- `ProtocolCodec.watchAppVersion(message)` and `NavigationController(onWatchAppVersion: (String) -> Unit)`: called from `handleInbound` for every inbound message that yields a version, wrapped in `runCatching` with a warning log. AppContainer wires it to `updateCheckRepository.recordWatchAppVersion`.
- `updatePrompt(stale, autoCheckEnabled, staleCheckFinished)` → `UpdatePrompt {NONE, STALE_CHECK, WEEKLY_QUESTION}` (`ui/dashboard/UpdatePrompt.kt`).
- `NavigationController(transport, scope, appVersion, ..., onWatchappReached, latestPhoneUpdate: () -> String?)`: `onInstruction`, `onNavigationStopped`, `onSettingsChanged`, `onConnectionLost`, `status: StateFlow<WatchLinkStatus>`. `latestPhoneUpdate` is read on every SendState. AppContainer supplies `updateCheckRepository.state.value.effectiveLatest`.
- `DashboardScreen(accessGranted, lastEligibleAtMillis, ..., updateAvailable, latestVersion, installSource, onOpenUpdateSource, appVersion, watchLink, updatePrompt, ...)`: stateless, every parameter but the first two defaulted. `MainActivity` passes `updateState.updateAvailable`, `updateState.effectiveLatest` and (since v0.0.74) `updateState.watchAppVersion`, plus `updatePrompt(stale = isCheckStale(), autoCheckEnabled, staleCheckFinished)` and `updateNeverChecked = lastCheckMillis == 0L`. The update card is `UpdateAvailableCard(model, latestVersion, appVersion, onOpenSource)` over `PromptCard`, with strings `dashboard_update_title`, `dashboard_update_body`, `dashboard_update_from`, `dashboard_update_fdroid_delay`, `dashboard_update_other_sources`, `dashboard_update_pick_source`, `dashboard_update_source_*`.
- Status-section strings: `dashboard_section_status`, `dashboard_watch` / `dashboard_watch_checking` / `_no_companion` / `_not_connected` / `_connected`, `dashboard_last_notification`, `dashboard_last_eligible` / `_none`, `dashboard_last_sent` / `_none`, and (v0.0.74) `dashboard_watchapp_version`, `dashboard_watchapp_version_seen` ("%1$s (last seen)"), `dashboard_watchapp_version_none` ("Not received yet").
- Online-check strings that describe GitHub as the update source: `onboarding_disclosure`, `dashboard_check_update`, `dashboard_auto_update` / `_hint`, `update_check_available` / `_up_to_date` / `_failed`, `dashboard_stale_check_title` / `_body` / `_never_body` / `_action`, `dashboard_weekly_question_title` / `_body` / `_turn_on` / `_not_now` (strings.xml:9, 49-63).
- `WatchTransport`: `inbound`, `launchApp()`, `send(AppMessage): SendResult`, `watchLink()`, `linkDiagnostics()`.

Rule JSON contracts are `schemas/ruleset.schema.json` and `schemas/app-catalog.schema.json` (see `spec/300-data/RulesJSON.md`). The diagnostic export format is in `spec/300-data/ExportFormat.md`.
