# Component Inventory

Each component lists its location, responsibility and dependencies. The `###` headings are the component names the Scope of Analysis block uses.

## Watchapp

### Watchapp Main Renderer
- **Location**: watchapp/src/c/main.c
- **Responsibility**: opens AppMessage and sends WATCH_READY (protocol + `APP_VERSION_STR`); `inbox_received_handler` sets `s_heard_from_phone`, stops the watchdog, records the phone version (key 11) and the #94 update state (bit 4 + key 14) and dispatches by event; shows the once-per-version update notice when no route is active and Main is on top (`maybe_show_update_notice`), and the update idle text; renders the panel (glyph + distance or stops), strip (clock/STALE + ETA), road text and the full-screen message layer (`MSG_PLAIN`, `MSG_CONNECTING`, `MSG_QR`); 5 s handshake watchdog (REQ-WATCH-017); backlight, vibration and early warning; app glance on exit (REQ-WATCH-019; "Update: <version>" while an update is recorded); SELECT opens settings with an `AboutInfo` snapshot and `on_settings_changed` as the change callback. UP and DOWN are unsubscribed and BACK is the default exit. On emery it opts into the system touch bridge for the whole app and disables it on its own window (#53, REQ-WATCH-021), with no gesture recognizer yet (#12). Input and feedback detail: [api-documentation.md](api-documentation.md) "Watch Input Surface".
- **Key state**: `APP_VERSION_STR` (the watch's own version), `s_phone_version[17]` (persist key 16), `s_latest_phone_version[17]` (key 17), `s_update_notice_shown[17]` (key 18), `s_heard_from_phone`, `s_navigating`, `s_flags`, `s_eta_epoch`, `s_primary_buf[65]`, `s_secondary_buf[25]`, `s_message_buf[96]`.
- **Depends on**: generated/protocol.h, theme, settings_window, eta_text, glance_text, phone_version, update_notice, update_notice_window, stops_text, panel_layout, early_warning.

### Watchapp Settings Menu
- **Location**: watchapp/src/c/settings_window.c, settings_window.h
- **Responsibility**: top-level menu with `ROW_BACKLIGHT`..`ROW_ABOUT` (`MAIN_ROW_COUNT 10`, the same rows on every platform; About is last, subtitle = watchapp version) and sub-windows (colour list with previews, glyph pack preview, backlight duration/colour on RGB hardware, vibration with new-instruction and early-warning sections). Every change calls a `theme.c` setter and then the `on_change` callback. The windows leave the touch bridge on, so on emery they take touches as button presses. The row order is mirrored in `capture_screenshots.py`. Rows: [api-documentation.md](api-documentation.md) "Watch Settings Menu".
- **Depends on**: theme, about_window, early_warning.

### Watchapp About Screen
- **Location**: watchapp/src/c/about_window.c, about_window.h, about_text.c, about_text.h
- **Responsibility**: read-only, black-on-white, scrollable page (REQ-WATCH-020): title, details ("Watchapp X / Phone app Y (last seen) / Protocol 1.1 / status"), "Install phone app" QR and link text; round-display insets; formatted once on open.
- **Depends on**: about_text (pure), generated/protocol.h, `connection_service_peek_pebble_app_connection()`.

### Watchapp Settings Store and Theme
- **Location**: watchapp/src/c/theme.c, theme.h
- **Responsibility**: setting getters/setters persisted under keys 1-15 (accent, units, invert, glyph pack, arrow side, ETA mode, backlight, vibration pattern and strength, backlight colour, ETA label style, early-warning distance, repeat, pattern and strength); setters clamp and persist at once; theme colours (accent panel, shaded strip, black or white road area); vibration patterns (pulse length as strength); backlight hold and RGB tint. Keys and ranges: [api-documentation.md](api-documentation.md).
- **Depends on**: Pebble persist, light and vibes APIs, early_warning.

### Watchapp Update Notice
- **Location**: watchapp/src/c/update_notice.c, update_notice.h, update_notice_window.c, update_notice_window.h
- **Responsibility**: #94 / REQ-WATCH-023 rules. `update_record` copies key 14 when bit 4 is set and clears the record when bit 4 is clear. `update_notice_action` returns NONE / DEFER / SHOW: once per version, deferred during a route or under another window. `update_idle_text` picks the idle text. The window shows `UPDATE_NOTICE_TEXT`. Since v0.0.73 the watch also derives an update from its own version compare (`version_compare.c/.h`, tested by `test_version_compare.c`): `version_parse` (optional `v`, dot-separated parts of 1-9 digits, nothing else), `version_newer(a, b)` (unparsable is never newer), and `version_effective_update(reported, own, phone)` = the higher of key 14 and the watch's own version when strictly newer than the phone's key 11 (key 14 kept when the two cannot be compared). `main.c` `effective_update()` feeds it the stored keys 17 and 16 on every call, so a phone message with bit 4 clear never hides the watch's own finding, and it drives the notice, idle text and glance with no online check.
- **Depends on**: string.h only (pure part); Pebble Window APIs (window part).

### Watchapp Pure Text Modules
- **Location**: watchapp/src/c/{about_text,glance_text,phone_version,update_notice,version_compare,eta_text,stops_text,panel_layout,early_warning}.{c,h}
- **Responsibility**: Pebble-free logic: About text, glance template and `GLANCE_HINT`, phone-version recording rule (16-byte cap, change detection), update-notice rules (see Watchapp Update Notice), `glance_update_subtitle`, ETA parsing/labels, stop count, panel placement, early-warning decision.

### Watchapp Host Unit Tests
- **Location**: watchapp/tests/test_*.c (9 files: about_text, early_warning, eta_text, glance_text, panel_layout, phone_version, stops_text, update_notice, version_compare)
- **Responsibility**: host-compiled assertions per pure unit, run by `scripts/test-watchapp-unit.sh` (each test must have a matching `src/c/<unit>.c` and is linked against that unit alone, under `gcc -std=c11 -Wall -Wextra -Werror`, so units cannot share helpers or include `pebble.h`). Each test is a plain `main` with a `failures` counter and `ok`/`FAIL` lines. Run by `scripts/test-all.sh`, not by CI.

### Watchapp Asset Tools
- **Location**: watchapp/tools/ (gen_maneuver_bitmaps.py, gen_connect_qr.py, gen_store_icon.py); outputs in watchapp/resources/, watchapp/store/
- **Responsibility**: generate committed glyph, QR and icon bitmaps.

### Watchapp Emulator Test Tools
- **Location**: watchapp/tools/capture_screenshots.py, watchapp/tools/touch_emulator.py; outputs in screenshots/ and watchapp/build/touch-check/
- **Responsibility**: `capture_screenshots.py` wipes and installs per platform (basalt, chalk, emery), injects a navigation update with `send-app-message`, walks the settings menu with button clicks using a hand-kept copy of the `ROW_*` indices, accent and glyph-pack rows, and pins the clock. `touch_emulator.py` is a standard-library RFB client for the emery emulator (VNC port 5901, 200x228) with `tap X Y`, `swipe X1 Y1 X2 Y2` (300 ms gestures) and a REQ-WATCH-021 `check` that screenshots each step. It never wipes, because `pebble wipe` turns touch navigation off in the emulator; its docstring says #12 can reuse `tap`/`swipe`.
- **Depends on**: pebble-tool (`--emulator`, `--vnc`, `emu-button`, `emu-set-time`, `screenshot`), a built `watchapp/build/watchapp.pbw`.

## Protocol

### Protocol Definition and Generator
- **Location**: protocol/ (protocol-definition.json, generate.py, check_consistency.py), examples/protocol-definition.json, scripts/generate-protocol.sh
- **Responsibility**: single protocol definition; emits Protocol.kt and protocol.h with a DO-NOT-EDIT banner; `--check` mode for CI and pre-commit; enforces keys/events/version parity with the protected example.

## Android Companion

### Phone Protocol Codec
- **Location**: android/app/src/main/java/com/pebblentn/app/protocol/ (ProtocolCodec.kt, AppMessage.kt, Protocol.kt generated)
- **Responsibility**: encode states (always with APP_VERSION; bit 4 + key 14 when a latest phone version is given) and compatibility errors; decode WATCH_READY (major/minor) / WATCH_REQUEST_STATE; `watchAppVersion` extracts key 11 from a WATCH_READY (trimmed, non-blank); UTF-8-safe text fitting to the watch buffers.

### Phone Watch Transport
- **Location**: protocol/WatchTransport.kt, pebble/PebbleWatchTransport.kt, pebble/PebbleAppMessageMapper.kt, pebble/WatchListenerService.kt
- **Responsibility**: send/launch over PebbleKit 2 with one relaunch-and-retry on `FailedDifferentAppOpen`; inbound bridge through `WatchInboundBus` (`onMessageReceived` forwards our UUID's messages, including the real WATCH_READY, which the Core Devices companion does deliver with `companionApp` declared (confirmed in v0.0.74), and marks the watchapp seen); synthetic WATCH_READY without key 11 on `onAppOpened` (its KDoc still claims Core Devices does not forward WATCH_READY: outdated); `WatchLink` status for the dashboard.
- **Depends on**: PebbleKit 2 Vendored Library.

### Navigation Controller
- **Location**: pebble/NavigationController.kt
- **Responsibility**: serialises reducer access with a mutex, runs effects, retries sends (3 attempts, exponential backoff), sends an autonomous READY 1.5 s after a launch, persists state, exposes `WatchLinkStatus`, and passes `appVersion` and `latestPhoneUpdate()` (read on every SendState) to the codec. In `handleInbound` it hands key 11 of a genuine WATCH_READY to `onWatchAppVersion` (failures logged, never thrown) before dispatching the READY, so the state sent in reply already carries the updated `effectiveLatest`; its own autonomous READY carries no version. The `scheduleAutonomousReady` KDoc justifies the autonomous READY with the outdated "Core Devices does not forward WATCH_READY" claim.
- **Depends on**: Navigation Session Reducer, Phone Protocol Codec, `WatchTransport`, `NavigationStateRepository`.

### Navigation Session Reducer
- **Location**: core/NavigationSessionReducer.kt, NavigationState.kt, NavigationInstruction.kt, Maneuver.kt, DistanceQuantizer.kt, WatchSettings.kt
- **Responsibility**: pure session logic: start/stop, dedupe, quantization, launch-once, READY sync and major-version check, 30 s staleness, ETA carry-over (300 s), flag computation (`flagsForNavigating`, `flagsForStopped`).

### Notification Pipeline
- **Location**: notification/ (+ icon/), core/AppAllowlist.kt, system/
- **Responsibility**: listener service, master switch and allowlist gate before content, minimal snapshot, serial queue, capture processor, maneuver heuristic, rule-driven large-icon recognition (#74), listener refresh, install receiver.

### Rule Engine
- **Location**: rules/
- **Responsibility**: strict ruleset codec and canonicalisation, validation, layered precedence (user > downloaded > bundled), condition evaluation, extractors, distance/duration parsing, `SafeRegex` time budget, preview.

### Persistence Layer
- **Location**: data/, data/db/, android/app/schemas/room/
- **Responsibility**: Room database v8 (supported_app_settings, notification_debug_event, user_rule, navigation_state, official_ruleset) with migration tests; SharedPreferences repositories (`pebblentn_settings`: master switch, watch settings, appearance, presence, feedback); debug history with hashed keys and grouping.

### App Update Check
- **Location**: update/UpdateCheckRepository.kt, update/AppVersions.kt, update/ReleaseFetcher.kt, update/InstallSource.kt
- **Responsibility**: opt-in weekly or manual GitHub release check (REQ-ANDROID-013); persists last check time, latest tag, the auto-check choice and the last-seen watchapp version (prefs `pebblentn_update_check`, key `watch_app_version`, REQ-ANDROID-019); exposes `UpdateState` (`watchAppVersion`, `updateAvailable`, `effectiveLatest`), `autoCheckEnabled` and `lastCheckMillis` flows and `isCheckStale` (30 days, REQ-ANDROID-018); tolerant (GitHub) and strict (watch) version comparison in `AppVersions`; install-source detection (`InstallSources`, REQ-ANDROID-020). The stored watchapp version has no timestamp and is never cleared. `updateAvailable` (`githubNewer || watchNewer`) needs no online check; `checkForUpdate`'s outcome (and so the manual-check toast) comes from the GitHub tag only; `isCheckStale` reads only `last_check_millis`. The `recordWatchAppVersion` KDoc ("with the Core Devices app WATCH_READY never arrives") is outdated.
- **Depends on**: GitHub Releases API, kotlinx-serialization, coroutines.

### Ruleset Signing and Activation
- **Location**: update/RuleUpdateRepository.kt, update/SignedRuleset.kt, update/RulesetSignatureVerifier.kt
- **Responsibility**: disabled-by-default remote official rules: Ed25519 verification, schema validation, embedded self-tests, atomic activation and rollback (REQ-SEC-005/006); no keys are embedded yet.

### Compose UI
- **Location**: ui/ (MainActivity, dashboard, rules, debug, share, apps, onboarding, components, format, theme)
- **Responsibility**: onboarding/disclosure; dashboard with setup checklist, feedback card, `PromptCard` attention cards ("Update available" via `UpdateAvailableCard`: "Update from {source}", F-Droid delay line, "Other sources" dialog, model from the pure `updateCardModel`; overridden rules; share nudge), the #94 stale-check item / weekly-check question (`UpdatePrompt`; the stale item sits above the setup checklist and the update card, keyed off the online check only, so it shows on every fresh install and every 30 days whatever version compare found), and sections Status (master switch, access chip, `InfoRow`s Watch link, Watchapp version (v0.0.74, `watchAppVersionText`), Last navigation notification, Last sent to watch) / Watch / Navigation / Troubleshooting / Appearance / Updates ("Check for updates now" with "Version X", "Check for updates weekly" toggle); rules browse/edit/review; debug history; diagnostics sharing. `MainActivity.AppNavHost` collects flows (including `updateCheckRepository.state`) and wires the actions (`checkForUpdate`, `setAutoCheckUpdates`, `openUpdateSource`, `openUrl`). There is no `DashboardScreen` UI test; Compose screen tests follow `NavigationAppsScreenTest` (Robolectric + `createComposeRule`).

### Diagnostics Export
- **Location**: export/
- **Responsibility**: rules-only, privacy-safe and full exports; redaction; size cap; share-sheet hand-off through FileProvider.

### Dependency Container
- **Location**: di/AppContainer.kt, PebbleNtnApplication.kt
- **Responsibility**: manual DI and startup: builds every repository, the transport and the controller; forwards `WatchSettings` changes to the controller; on start restores state, starts inbound collection, warms caches, and runs the update check only when auto-check is enabled. Supplies `latestPhoneUpdate` = `updateCheckRepository.state.value.effectiveLatest` (#94, REQ-ANDROID-019), `onWatchAppVersion` = `updateCheckRepository.recordWatchAppVersion`, and `installSource` (detected once per app start).

### PebbleKit 2 Vendored Library
- **Location**: android/pebblekit/
- **Responsibility**: Rebble PebbleKit Android 2 1.2.0 (`PebbleSender`, `PebbleInfoRetriever`, `PebbleAndroidAppPicker`, `BasePebbleListenerService`, dictionary model), built from source for F-Droid.

### Fixture Publisher
- **Location**: android/fixture-publisher/
- **Responsibility**: debug-only app posting synthetic navigation notifications (capture-only catalog entry).

## Data and Tooling

### Rule Data and Schemas
- **Location**: rules/catalog/, rules/bundled/, rules/fixtures/, schemas/, examples/
- **Responsibility**: catalog of 19 entries (18 navigation apps, 4 of them with official rules, plus the fixture publisher), bundled rulesets per app and language, sanitised fixtures; staged into the APK by Gradle `syncRuleData`.

### Rule Workbench
- **Location**: tools/rule-workbench/
- **Responsibility**: Python rule inspect/validate/diff/scaffold/sanitize/promote/test/regression, plus localised Google Maps ruleset generation.

### Route Capture Harness
- **Location**: tools/route-capture/
- **Responsibility**: plays fixed routes in an Android emulator, records notifications, checks them against bundled rules, files per-app GitHub issues (#55); includes device helpers (locale setter, icon probe).

### Build and Release Scripts
- **Location**: scripts/, .githooks/pre-commit, VERSION
- **Responsibility**: build-all, build-watchapp, test-all, test-watchapp-unit, validate-*, generate-protocol, version sync (`version.sh`), changelog, bootstrap, emulator and install helpers, manual-open E2E; pre-commit gate.

### CI Workflows
- **Location**: .github/workflows/
- **Responsibility**: android, watchapp, rules, codeql, release (push to main), play-release (manual). Detail is in [code-quality-assessment.md](code-quality-assessment.md).

### Distribution Metadata
- **Location**: fastlane/metadata/android/, fdroid/, watchapp/store/, screenshots/
- **Responsibility**: F-Droid/fastlane descriptions and per-versionCode changelogs, F-Droid recipe (`Binaries:` = the GitHub APK, `UpdateCheckMode: Tags`) and reproducible-build notes, Pebble store art and watch screenshots. The channels (GitHub, F-Droid, Obtainium, Rebble store) are listed in [business-overview.md](business-overview.md).

### Specification and Requirements
- **Location**: spec/, requirements/, docs/, MANIFEST.sha256.json, AGENTS.md, README.md
- **Responsibility**: numbered product, architecture, data, UI, testing and release specs; REQ-* requirements with acceptance criteria; SHA-256 integrity manifest for protected files; agent contract; implementation status log.
