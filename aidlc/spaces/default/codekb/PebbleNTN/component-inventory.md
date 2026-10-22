# Component Inventory

Each component lists its location, responsibility and dependencies. The `###` headings are the component names the Scope of Analysis block uses.

## Watchapp

### Watchapp Main Renderer
- **Location**: watchapp/src/c/main.c
- **Responsibility**: opens AppMessage and sends WATCH_READY (protocol + `APP_VERSION_STR`); `inbox_received_handler` sets `s_heard_from_phone`, stops the watchdog, records the phone version and dispatches by event; renders the panel (glyph + distance or stops), strip (clock/STALE + ETA), road text and the full-screen message layer (`MSG_PLAIN`, `MSG_CONNECTING`, `MSG_QR`); 5 s handshake watchdog (REQ-WATCH-017); backlight, vibration and early warning; app glance on exit (REQ-WATCH-019); SELECT opens settings with an `AboutInfo` snapshot.
- **Key state**: `s_phone_version[17]` (persist key 16), `s_heard_from_phone`, `s_navigating`, `s_flags`, `s_eta_epoch`, `s_primary_buf[65]`, `s_secondary_buf[25]`, `s_message_buf[96]`.
- **Depends on**: generated/protocol.h, theme, settings_window, eta_text, glance_text, phone_version, stops_text, panel_layout, early_warning.

### Watchapp Settings Menu
- **Location**: watchapp/src/c/settings_window.c, settings_window.h
- **Responsibility**: top-level menu with `ROW_BACKLIGHT`..`ROW_ABOUT` (`MAIN_ROW_COUNT 10`; About is last, subtitle = watchapp version) and sub-windows (colour list, glyph pack preview, backlight duration/colour, vibration with new-instruction and early-warning sections).
- **Depends on**: theme, about_window, early_warning.

### Watchapp About Screen
- **Location**: watchapp/src/c/about_window.c, about_window.h, about_text.c, about_text.h
- **Responsibility**: read-only, black-on-white, scrollable page (REQ-WATCH-020): title, details ("Watchapp X / Phone app Y (last seen) / Protocol 1.1 / status"), "Install phone app" QR and link text; round-display insets; formatted once on open.
- **Depends on**: about_text (pure), generated/protocol.h, `connection_service_peek_pebble_app_connection()`.

### Watchapp Settings Store and Theme
- **Location**: watchapp/src/c/theme.c, theme.h
- **Responsibility**: setting getters/setters persisted under keys 1-15; theme colours; vibration patterns; backlight hold and RGB tint.
- **Depends on**: Pebble persist, light and vibes APIs, early_warning.

### Watchapp Pure Text Modules
- **Location**: watchapp/src/c/{about_text,glance_text,phone_version,eta_text,stops_text,panel_layout,early_warning}.{c,h}
- **Responsibility**: Pebble-free logic: About text, glance template and `GLANCE_HINT`, phone-version recording rule (16-byte cap, change detection), ETA parsing/labels, stop count, panel placement, early-warning decision.

### Watchapp Host Unit Tests
- **Location**: watchapp/tests/test_*.c (7 files)
- **Responsibility**: host-compiled assertions per pure unit, run by `scripts/test-watchapp-unit.sh` (each test must have a matching `src/c/<unit>.c`).

### Watchapp Asset Tools
- **Location**: watchapp/tools/ (gen_maneuver_bitmaps.py, gen_connect_qr.py, gen_store_icon.py, capture_screenshots.py, touch_emulator.py); outputs in watchapp/resources/, watchapp/store/, screenshots/
- **Responsibility**: generate committed glyph, QR and icon bitmaps; drive emulators for screenshots and touch checks.

## Protocol

### Protocol Definition and Generator
- **Location**: protocol/ (protocol-definition.json, generate.py, check_consistency.py), examples/protocol-definition.json, scripts/generate-protocol.sh
- **Responsibility**: single protocol definition; emits Protocol.kt and protocol.h with a DO-NOT-EDIT banner; `--check` mode for CI and pre-commit; enforces keys/events/version parity with the protected example.

## Android Companion

### Phone Protocol Codec
- **Location**: android/app/src/main/java/com/pebblentn/app/protocol/ (ProtocolCodec.kt, AppMessage.kt, Protocol.kt generated)
- **Responsibility**: encode states (always with APP_VERSION) and compatibility errors; decode WATCH_READY / WATCH_REQUEST_STATE; UTF-8-safe text fitting to the watch buffers.

### Phone Watch Transport
- **Location**: protocol/WatchTransport.kt, pebble/PebbleWatchTransport.kt, pebble/PebbleAppMessageMapper.kt, pebble/WatchListenerService.kt
- **Responsibility**: send/launch over PebbleKit 2 with one relaunch-and-retry on `FailedDifferentAppOpen`; inbound bridge through `WatchInboundBus`; synthetic WATCH_READY on `onAppOpened`; `WatchLink` status for the dashboard.
- **Depends on**: PebbleKit 2 Vendored Library.

### Navigation Controller
- **Location**: pebble/NavigationController.kt
- **Responsibility**: serialises reducer access with a mutex, runs effects, retries sends (3 attempts, exponential backoff), sends an autonomous READY 1.5 s after a launch, persists state, exposes `WatchLinkStatus`, and passes `appVersion` to the codec.
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
- **Location**: update/UpdateCheckRepository.kt, update/AppVersions.kt, update/ReleaseFetcher.kt
- **Responsibility**: opt-in weekly or manual GitHub release check (REQ-ANDROID-013); persists last check time, latest tag and the auto-check choice (prefs `pebblentn_update_check`); exposes `UpdateState` and `autoCheckEnabled` flows; tolerant version comparison.
- **Depends on**: GitHub Releases API, kotlinx-serialization, coroutines.

### Ruleset Signing and Activation
- **Location**: update/RuleUpdateRepository.kt, update/SignedRuleset.kt, update/RulesetSignatureVerifier.kt
- **Responsibility**: disabled-by-default remote official rules: Ed25519 verification, schema validation, embedded self-tests, atomic activation and rollback (REQ-SEC-005/006); no keys are embedded yet.

### Compose UI
- **Location**: ui/ (MainActivity, dashboard, rules, debug, share, apps, onboarding, components, format, theme)
- **Responsibility**: onboarding/disclosure; dashboard with setup checklist, feedback card, `PromptCard` attention cards (update, overridden rules, share nudge) and sections Status / Watch / Navigation / Troubleshooting / Appearance / Updates ("Check for updates now", "Check for updates weekly" toggle); rules browse/edit/review; debug history; diagnostics sharing. `MainActivity` collects flows and wires the actions (`checkForUpdate`, `setAutoCheckUpdates`, `openUrl`).

### Diagnostics Export
- **Location**: export/
- **Responsibility**: rules-only, privacy-safe and full exports; redaction; size cap; share-sheet hand-off through FileProvider.

### Dependency Container
- **Location**: di/AppContainer.kt, PebbleNtnApplication.kt
- **Responsibility**: manual DI and startup: builds every repository, the transport and the controller; forwards `WatchSettings` changes to the controller; on start restores state, starts inbound collection, warms caches, and runs the update check only when auto-check is enabled.

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
- **Responsibility**: F-Droid/fastlane descriptions and per-versionCode changelogs, F-Droid recipe and reproducible-build notes, Pebble store art and watch screenshots.

### Specification and Requirements
- **Location**: spec/, requirements/, docs/, MANIFEST.sha256.json, AGENTS.md, README.md
- **Responsibility**: numbered product, architecture, data, UI, testing and release specs; REQ-* requirements with acceptance criteria; SHA-256 integrity manifest for protected files; agent contract; implementation status log.
