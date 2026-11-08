# Component Inventory

Each component lists its location, responsibility and dependencies. The `###` headings are the component names the Scope of Analysis block uses. Components re-checked at 61787f5 for #132 say so; the others keep the commit they were last checked at.

## Watchapp

### Watchapp Main Renderer
- **Location**: watchapp/src/c/main.c (1,479 lines, checked at 61787f5)
- **Responsibility**: opens AppMessage with inbox, outbox-sent and outbox-failed handlers; queues `WATCH_READY` once from `init()` and SETTINGS after watch edits and on every phone reconnect through a one-at-a-time outbox (pending bits, one retry, then held until reconnect). `inbox_received_handler` sets `s_heard_from_phone`, stops the watchdog, records the phone version (key 11, persist 16) and the update state (bit 4 + key 14, persist 17) and dispatches by event: UPDATE renders and buzzes per the watch's own vibration settings and the early warning; STOPPED exits to the watchface on bit 0 (`window_stack_pop_all(true)`) or shows "Arrived"; NONE shows the "No navigation detected" idle text; SETTINGS applies a newer phone set and replies. Renders the panel, strip, road text and full-screen message layer; 5 s handshake watchdog (REQ-WATCH-017); backlight; app glance on exit; SELECT opens settings; UP/DOWN and emery gestures run shortcuts with a 1.5 s banner. BACK is the default exit. Input detail: [api-documentation.md](api-documentation.md) "Watch Input Surface"; outbox: "Outbox and handshake".
- **Key state**: `APP_VERSION_STR` ("0.0.81"), `s_phone_version`, `s_latest_phone_version`, `s_update_notice_shown`, `s_heard_from_phone`, `s_navigating`, `s_flags`, `s_last_maneuver`, `s_early_warn`, `s_out_pending`, `s_out_in_flight`, `s_out_retried`, `s_out_held`.
- **Depends on**: generated/protocol.h, theme, settings_sync_glue, settings_window, shortcut_apply, qr_code, eta_text, glance_text, phone_version, update_notice, update_notice_window, stops_text, panel_layout, early_warning, version_compare.

### Watchapp Settings Menu
- **Location**: watchapp/src/c/settings_window.c, settings_window.h (checked at 61787f5)
- **Responsibility**: top-level menu of 11 rows, About first and Shortcuts last, drawn from `kMainTitles` and cycled from `kMainCycles`; doors for colour (previews), glyph pack (previews), vibration (New instruction and Early warning sections), backlight (RGB only: duration and colour) and Shortcuts (Buttons, Touch on emery, one shared action list). Every change goes through `settings_set` (a watch edit) and then `notify_change`; `settings_window_refresh()` closes open doors and reloads after a phone edit. Static const callback tables keep code small for aplite. A new door follows the Backlight door (`BL_ROW_*`) or Vibration door shape; a units-aware value follows `early_warn_label`. Rows: [api-documentation.md](api-documentation.md) "Watch Settings Menu".
- **Depends on**: theme, about_window, shortcut_apply, shortcuts, early_warning.

### Watchapp Settings Store and Theme
- **Location**: watchapp/src/c/theme.c, theme.h (checked at 61787f5)
- **Responsibility**: holds the 23 synced values in one `s_values[SET_COUNT]` byte array: `settings_get`, `settings_store` (range-check against `sync_valid`, default when invalid, write only a changed value under `sync_persist_key`, live backlight tint, last audible vibration pattern at persist 27), `settings_set` (store plus the edit handler), `settings_load`; named getter and setter macros per setting; `_Static_assert`s that the enum counts match `kRules`. Theme colours, name tables, vibration patterns (pulse length as strength) and backlight hold. Keys and ranges: [api-documentation.md](api-documentation.md).
- **Depends on**: Pebble persist, light and vibes APIs, settings_sync, early_warning, shortcuts.

### Watchapp Settings Sync
- **Location**: watchapp/src/c/settings_sync.c, settings_sync.h, settings_sync_glue.c, settings_sync_glue.h, revision.c, revision.h; tested by watchapp/tests/test_settings_sync.c and test_revision.c (checked at 61787f5)
- **Responsibility**: (#102, REQ-WATCH-025) the pure table of 23 settings (`SettingId`, `kRules` with the value count in 5 bits and `NEEDS_RGB`/`NEEDS_TOUCH`/`FROM_ONE` flags, `kDefaults`), validation by capability, defaults, bool storage, the persist-key mapping (1-15, then 19-26), build capabilities and platform, and the revision rule and decision (`revision_newer`, `sync_decide`). The glue keeps the revision at persist key 28, routes every `settings_set` to a revision bump and a SETTINGS request, writes revision, platform, capabilities and 23 uint8 values into an outgoing message, and applies a phone SETTINGS (int32 tuples only) when the phone's revision is newer, storing only changed values. `_Static_assert`s pin 23 settings, 7 shortcut slots and the contiguous SETTING_* key range.
- **Depends on**: generated/protocol.h, shortcuts (pure part); theme, Pebble persist and dictionary APIs (glue).

### Watchapp Shortcuts
- **Location**: watchapp/src/c/shortcuts.c, shortcuts.h, shortcut_apply.c, shortcut_apply.h; tested by watchapp/tests/test_shortcuts.c
- **Responsibility**: (v0.0.76, REQ-WATCH-024) slots, 15 append-only actions, defaults, labels, offered list with and without RGB, cycling, the vibration toggle, notice text; `shortcut_apply` runs an action through the settings setters. `VibePatternId` (6 values, Off = 5 since #102) lives in shortcuts.h.
- **Depends on**: string.h, stdio.h (pure part); theme (apply part).

### Watchapp About Screen
- **Location**: watchapp/src/c/about_window.c, about_window.h, about_text.c, about_text.h
- **Responsibility**: read-only scrollable page (REQ-WATCH-020): versions, protocol, status, install QR.
- **Depends on**: about_text (pure), generated/protocol.h, connection service.

### Watchapp Update Notice
- **Location**: watchapp/src/c/update_notice.c, update_notice.h, update_notice_window.c, update_notice_window.h, version_compare.c, version_compare.h
- **Responsibility**: #94 / REQ-WATCH-023 rules: record key 14 while bit 4 is set, NONE / DEFER / SHOW once per version, idle texts (`IDLE_TEXT` "No navigation detected. If navigating, open PebbleNTN on your phone.", `UPDATE_IDLE_TEXT`), plus the watch's own version compare (`version_effective_update`).
- **Depends on**: string.h only (pure part); Pebble Window APIs (window part).

### Watchapp Pure Text Modules
- **Location**: watchapp/src/c/{about_text,glance_text,phone_version,update_notice,version_compare,eta_text,stops_text,panel_layout,early_warning,shortcuts,settings_sync,revision}.{c,h}
- **Responsibility**: Pebble-free logic: About text, glance template, phone-version recording, update-notice rules, ETA parsing and labels, stop count, panel placement, early-warning decision and labels, shortcuts, the settings table and revision.

### Watchapp Host Unit Tests
- **Location**: watchapp/tests/test_*.c (12 at 61787f5: about_text, early_warning, eta_text, glance_text, panel_layout, phone_version, revision, settings_sync, shortcuts, stops_text, update_notice, version_compare)
- **Responsibility**: host-compiled assertions per pure unit, run by `scripts/test-watchapp-unit.sh` under `gcc -std=c11 -Wall -Wextra -Werror`, each linked with its unit and any unit named in a `// Links:` header line. Run by `scripts/test-all.sh`, not by CI.

### Watchapp Asset Tools
- **Location**: watchapp/tools/ (gen_maneuver_bitmaps.py, gen_connect_qr.py, gen_store_icon.py); outputs in watchapp/resources/, watchapp/store/
- **Responsibility**: generate committed glyph, QR and icon bitmaps.

### Watchapp Emulator Test Tools
- **Location**: watchapp/tools/capture_screenshots.py, watchapp/tools/touch_emulator.py
- **Responsibility**: `capture_screenshots.py` installs per platform, injects a navigation update, walks the settings menu with a hand-kept copy of the `ROW_*` indices, and (`--previews`) makes the phone's `watch_preview_*` images; `touch_emulator.py` drives emery touch over VNC.
- **Depends on**: pebble-tool, a built `watchapp/build/watchapp.pbw`.

## Protocol

### Protocol Definition and Generator
- **Location**: protocol/ (protocol-definition.json, generate.py, check_consistency.py, README.md), examples/protocol-definition.json, scripts/generate-protocol.sh (protocol/ checked at 61787f5)
- **Responsibility**: single protocol definition (1.3: keys 0-40, events 1-4 and 10-12, flag bits 0-4, capability bits 0-3, platforms 0-5); emits `Protocol.kt` and `protocol.h` with a DO-NOT-EDIT banner; `--check` mode for CI and pre-commit; `check_consistency.py` enforces version, keys and events parity with the protected example. Adding a synced setting or a new event means editing both JSON files and the MANIFEST hash, then regenerating.

## Android Companion

### Phone Protocol Codec
- **Location**: android/app/src/main/java/com/pebblentn/app/protocol/ (ProtocolCodec.kt, AppMessage.kt, Protocol.kt generated) (checked at 61787f5)
- **Responsibility**: encode states (always with APP_VERSION; bit 4 + key 14 for an update), compatibility errors and phone SETTINGS (revision plus edited values); decode WATCH_READY / WATCH_REQUEST_STATE; `watchAppVersion`, `realWatchReadyMinor`, `decodeWatchSettings` (real READY or SETTINGS with a revision; missing values become defaults); UTF-8-safe text fitting. `AppMessage` carries only int and string values.

### Phone Watch Transport
- **Location**: protocol/WatchTransport.kt, pebble/PebbleWatchTransport.kt, pebble/PebbleAppMessageMapper.kt, pebble/WatchListenerService.kt (checked at 61787f5)
- **Responsibility**: `launchApp` (`startAppOnTheWatch`), `send` with one relaunch-and-retry on `FailedDifferentAppOpen` while auto-launch is on, `sendToOpenApp` (never relaunches; used for settings), `watchappClosed` (from `onAppClosed`), `watchLink`, `linkDiagnostics`. No close or stop call, although PebbleKit's `stopAppOnTheWatch` exists. Inbound bridge through `WatchInboundBus` (messages and closes); synthetic READY on `onAppOpened`; marks the watchapp seen. The mapper sends ints as `Int32` and normalises inbound ints of any width, dropping bytes.
- **Depends on**: PebbleKit 2 Vendored Library.

### Navigation Controller
- **Location**: pebble/NavigationController.kt (checked at 61787f5)
- **Responsibility**: serialises reducer access with a mutex and runs effects: `LaunchWatchApp` -> `launchApp()` plus an autonomous READY after 1.5 s; `SendState` -> `sendWithRetry` over `transport.send` (3 attempts, exponential backoff; so a send while the watchapp is closed relaunches it); `SendCompatibilityError`. Persists state, exposes `WatchLinkStatus`, passes `appVersion` and `latestPhoneUpdate()` to the codec, records a real READY's watchapp version. Beside the reducer, under a second mutex, it runs the settings sync: merges watch settings, sets `watchappOpen` on settings received and clears it on `onAppClosed` or a failed settings send, sends pending phone edits through `sendToOpenApp`, marks an outdated watchapp. It is the natural owner of a hide/show policy (#132), which would also need to gate `SendState` while hidden.
- **Depends on**: Navigation Session Reducer, Watch Settings Sync and Mirror, Phone Protocol Codec, `WatchTransport`, `NavigationStateRepository`.

### Navigation Session Reducer
- **Location**: core/NavigationSessionReducer.kt, NavigationState.kt, NavigationInstruction.kt, Maneuver.kt, DistanceQuantizer.kt, WatchSettings.kt (checked at 61787f5)
- **Responsibility**: pure session logic: start and stop, dedupe, distance quantization (10/25/50/100 m steps), launch-once per session while auto-launch is on (`launchedSessionId`), READY sync and major-version check, 30 s staleness, ETA carry-over (300 s), flag computation. Time enters only as event timestamps; `FreshnessChecked` exists but nothing emits it, so there is no periodic tick.

### Watch Settings Sync and Mirror
- **Location**: core/WatchSettingsModel.kt, core/WatchSettingsSync.kt, data/WatchSettingsMirrorRepository.kt (with `WatchSettingsStore`, `InMemoryWatchSettingsStore`); tested by WatchSettingsModelTest, WatchSettingsSyncTest, WatchSettingsMirrorRepositoryTest, WatchSettingsQueueTest (checked at 61787f5)
- **Responsibility**: (#102, REQ-ANDROID-021) the phone's copy of the watch's table (`WatchSetting` with value count, first value, capability needs, default, protocol key), the watch's validation and option rules, the revision rule and merge decision, phone edits (ignored before any watch settings), the outgoing pending set, and persistence of the mirror, pending edits and flags in prefs `pebblentn_watch_settings`. `fromWatch` fills absent keys with defaults, so the mirror cannot tell an older watchapp's missing setting from a default.
- **Depends on**: Protocol constants, SharedPreferences.

### Watch Settings Screen
- **Location**: ui/watchsettings/WatchSettingsScreen.kt, OptionListScreen.kt, WatchSettingsUi.kt; routes in ui/MainActivity.kt; tested by WatchSettingsUiTest, WatchSettingsScreenTest (checked at 61787f5)
- **Responsibility**: shows the waiting, outdated or ready state, the queued notice, and the sections and rows the watch offers; the option list with previews for the seven visual settings and colour swatches. Pure `WatchSettingsUi` holds status, sections, options, previews, exhaustive title and label-array mappings (units-aware for distances). Edits go to `AppContainer.setWatchSetting`.
- **Depends on**: Watch Settings Sync and Mirror, Navigation Controller (`watchappOpen`), `PreviewCatalog`, string resources.

### Navigation App Catalog
- **Location**: catalog/NavigationAppCatalog.kt, catalog/EffectiveCatalog.kt, catalog/CustomPackageValidator.kt; data in rules/catalog/navigation-apps.json
- **Responsibility**: catalog entries (`appId`, ordered `packageNames`, flags, `rulesFrom`, `website`, `playListed`), strict parse, rule families, the Google Play view, user-added packages (REQ-ANDROID-023).
- **Depends on**: kotlinx-serialization; read by Persistence Layer, Dependency Container and Compose UI.

### Notification Pipeline
- **Location**: notification/ (+ icon/), core/AppAllowlist.kt, system/
- **Responsibility**: listener service, master switch and allowlist gate before content, minimal snapshot (text fields, `progress`/`progressMax` when present, times, matched icon drawable name), serial queue, capture processor, maneuver heuristic, rule-driven large-icon recognition, listener refresh, install receiver. Checked at 61787f5 for the snapshot path: `DebugCaptureProcessor.onPosted` evaluates rules, records the event and forwards only the instruction to the controller; the raw fields a speed estimate needs stop there.
- **Depends on**: Rule Engine, Persistence Layer (allowlist), Android package visibility.

### Rule Engine
- **Location**: rules/
- **Responsibility**: strict ruleset codec, validation, layered precedence, condition evaluation, extractors, distance and duration parsing, `SafeRegex`, preview; family-aware package matching (#113). Rule fields do not include `progress`.

### Persistence Layer
- **Location**: data/, data/db/, android/app/schemas/room/
- **Responsibility**: Room database v8 with migration tests; SharedPreferences repositories (`pebblentn_settings`: master switch, auto-launch, appearance, presence, feedback; `pebblentn_watch_settings`: the settings mirror); debug history with hashed keys and grouping of repeated cards (the grouping key ignores `progress`, so only the first and last snapshot of a run are kept).

### App Update Check
- **Location**: update/UpdateCheckRepository.kt, update/AppVersions.kt, update/ReleaseFetcher.kt, update/InstallSource.kt, update/InstallRecord.kt, update/UpdatePolicy.kt, update/UpdateCheckMessage.kt
- **Responsibility**: opt-in weekly or manual GitHub release check (REQ-ANDROID-013), the stored watchapp version (REQ-ANDROID-019), `UpdateState`, staleness (REQ-ANDROID-018), install source and record (REQ-ANDROID-020), the per-source update policy (REQ-ANDROID-022).
- **Depends on**: GitHub Releases API, kotlinx-serialization, coroutines.

### Ruleset Signing and Activation
- **Location**: update/RuleUpdateRepository.kt, update/SignedRuleset.kt, update/RulesetSignatureVerifier.kt
- **Responsibility**: disabled-by-default remote official rules with Ed25519 verification; no download code.

### Compose UI
- **Location**: ui/ (MainActivity, dashboard, watchsettings, rules, debug, share, apps, onboarding, components, format, theme)
- **Responsibility**: onboarding, dashboard (setup checklist, prompt cards, Status, Watch with the Watch settings row and the auto-launch toggle, Navigation, Troubleshooting, Appearance, Updates), Watch settings, rules, debug history, diagnostics sharing. `MainActivity.AppNavHost` collects flows and wires actions. No `@Preview` composables.

### Diagnostics Export
- **Location**: export/ (ExportBuilder.kt, Redactor.kt, DiagnosticExporter.kt, DiagnosticShareManager.kt) (checked at 61787f5, share manager not reopened)
- **Responsibility**: rules-only, privacy-safe and full exports, schema 5: one `ExportMetadata` for every mode (versions, locale, user-added packages, install record) and notification events with versions, occurrences, the watch output and first and last snapshots. `Redactor` replaces letters outside a keyword list, keeping digits, so "Arrive 12:45", distances and the progress integers survive privacy-safe exports. `DiagnosticExporter` takes export-time inputs as lambdas for both `build` and the 10 MB `buildCapped`. No section for phone-side decisions (sends, launches, hides).
- **Depends on**: Persistence Layer (debug history, user rules), Dependency Container (wiring).

### Dependency Container
- **Location**: di/AppContainer.kt, PebbleNtnApplication.kt (AppContainer checked at 61787f5)
- **Responsibility**: manual DI and startup: builds the catalog, repositories, rule engine, icon recognizer, exporter (with `userAddedPackages` and `installRecord` lambdas), transport (with `isAutoLaunchEnabled`), the settings mirror and the controller (with `latestPhoneUpdate`, `onWatchAppVersion`, `watchSettings`); forwards `WatchSettings` changes to the controller; wires `DebugCaptureProcessor` with `onMatchedInstruction = navigationController.onInstruction` and `onSessionEnded = onNavigationStopped`; `setWatchSetting` and `setWatchAutoLaunch`; on start restores state, starts inbound collection and runs the opt-in update check.

### Dashboard Update UI
- **Location**: ui/dashboard/UpdateCard.kt, UpdatePrompt.kt, PhoneAppVersionRow.kt, the update parts of DashboardScreen.kt, MainActivity.kt, OnboardingScreen.kt
- **Responsibility**: the stale-check card and weekly question, the "Update available" card, the Updates section, the "Phone app version" row; pure helpers for every decision.
- **Depends on**: App Update Check, Dependency Container.

### Android Build Configuration
- **Location**: android/build.gradle.kts, android/app/build.gradle.kts, android/gradle/libs.versions.toml, android/gradle/wrapper/gradle-wrapper.properties
- **Responsibility**: plugins, `:app` SDK levels, versioning, the target-SDK gate, release signing, R8 and resource shrinking, reproducibility settings, rule-data staging, Room schema export, test JVM.
- **Depends on**: Gradle 8.13, JDK 21, Android SDK 36.

### PebbleKit 2 Vendored Library
- **Location**: android/pebblekit/
- **Responsibility**: Rebble PebbleKit Android 2 1.2.0 (`PebbleSender` with `sendDataToPebble`, `startAppOnTheWatch`, `stopAppOnTheWatch`; `PebbleInfoRetriever`; `PebbleAndroidAppPicker`; `BasePebbleListenerService` with `onAppOpened`/`onAppClosed`), built from source for F-Droid.

### Fixture Publisher
- **Location**: android/fixture-publisher/
- **Responsibility**: debug-only app posting synthetic navigation notifications.

## Data and Tooling

### Rule Data and Schemas
- **Location**: rules/catalog/, rules/bundled/, rules/fixtures/, schemas/, examples/
- **Responsibility**: the catalog, bundled rulesets per app and language, sanitised fixtures, staged into the APK by Gradle `syncRuleData`. At c7b6ba0: 20 entries, 28 package names; rulesets google-maps (10 files), osmand (9), comaps (1), organic-maps (1); fixtures google-maps 189, osmand 195, comaps 21, organic-maps 12; Vela Maps added by #120.
- **Checks**: `scripts/validate_catalog.py`, `scripts/validate_rules.py`, per-app `*RulesRegressionTest`, `NavigationAppCatalogTest`.

### Rule Workbench
- **Location**: tools/rule-workbench/
- **Responsibility**: Python rule inspect/validate/diff/scaffold/sanitize/promote/test/regression and localised Google Maps ruleset generation; its engine subset mirrors the Kotlin engine (checked at c7b6ba0).

### Route Capture Harness
- **Location**: tools/route-capture/
- **Responsibility**: plays fixed routes in an Android emulator, records notifications, checks them against bundled rules, files per-app GitHub issues (#55). Drivers for Google Maps, OsmAnd, Organic Maps and CoMaps at c7b6ba0; a Vela driver proposed by #120. Runs on the Windows emulator from WSL.
- **Depends on**: Rule Workbench, rule data, `ManeuverHeuristic.kt` word list, adb and the emulator, `gh`.

### Build and Release Scripts
- **Location**: scripts/, .githooks/pre-commit, VERSION
- **Responsibility**: build, test (`test-all.sh`, `test-watchapp-unit.sh`), validation, protocol generation, version sync, changelog, emulator helpers; pre-commit gate.

### CI Workflows
- **Location**: .github/workflows/
- **Responsibility**: android, watchapp, rules, codeql, release (push to main), play-release (manual). Detail in [code-quality-assessment.md](code-quality-assessment.md).

### Distribution Metadata
- **Location**: fastlane/metadata/android/, fdroid/, watchapp/store/, screenshots/
- **Responsibility**: store descriptions and changelogs, F-Droid recipe, Pebble store art and watch screenshots; channels in [business-overview.md](business-overview.md).

### Store and Privacy Documents
- **Location**: docs/PRIVACY_POLICY.md, docs/PLAY_DATA_SAFETY.md, docs/PRIVACY_REVIEW.md, docs/RELEASE_CHECKLIST.md; normative sources in spec/600-security-release/ and requirements/REQ-SECURITY.md
- **Responsibility**: privacy policy, Play Data safety answers, privacy review, release checklist (REQ-ANDROID-022 also requires a `docs/PLAY_CONSOLE.md`; not checked in this run).

### Specification and Requirements
- **Location**: spec/, requirements/, docs/, MANIFEST.sha256.json, AGENTS.md, README.md
- **Responsibility**: numbered specs; REQ-* requirements with acceptance criteria; SHA-256 integrity manifest for protected files (hashes updated by hand in the same commit). At 61787f5 the newest IDs are REQ-WATCH-025, REQ-ANDROID-023 and REQ-DEBUG-015, so #132 takes REQ-WATCH-026, REQ-ANDROID-024 and, for the export log, REQ-DEBUG-016. Protected files #132 is likely to touch: `requirements/REQ-WATCH.md` (025's "23 user settings", 006's relaunch rule), `requirements/REQ-ANDROID.md`, `requirements/REQ-DEBUG.md`, `examples/protocol-definition.json`, `spec/200-architecture/Protocol.md`, `spec/200-architecture/Pebble.md` (launch policy), `spec/300-data/ExportFormat.md`, `spec/400-ui/*`.
