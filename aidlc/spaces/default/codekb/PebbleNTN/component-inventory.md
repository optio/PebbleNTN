# Component Inventory

Each component lists location, responsibility and dependencies. Headings are the names the Scope of Analysis block uses.

## Watchapp

### Watchapp Main Renderer
- **Location**: watchapp/src/c/main.c
- **Responsibility**: opens AppMessage, sends WATCH_READY with protocol and `APP_VERSION_STR`; decodes inbound events; renders panel (glyph + distance), strip (clock + ETA), road text and full-screen messages; 5 s handshake watchdog with install QR (REQ-WATCH-017); backlight, vibration and early warning; app glance on exit; opens settings on SELECT with an `AboutInfo` snapshot.
- **Key state**: `s_heard_from_phone` (set on any inbound message), `s_navigating`, text buffers (64/24 B), `s_eta_epoch`.
- **Depends on**: generated/protocol.h, theme.h, settings_window.h, eta_text, glance_text, stops_text, panel_layout, early_warning.

### Watchapp About Screen
- **Location**: watchapp/src/c/about_window.c, about_window.h, about_text.c, about_text.h
- **Responsibility**: read-only, black-on-white, scrollable page: title, details (watch version, protocol version, phone status), "Install phone app" QR and link text; round-display insets (REQ-WATCH-020).
- **Key state**: `s_info` (copy of `AboutInfo`), `s_details[80]`, four text layers, QR bitmap (`RESOURCE_ID_CONNECT_QR`).
- **Depends on**: about_text (pure), generated/protocol.h, Pebble `connection_service_peek_pebble_app_connection()`.

### Watchapp Settings Menu
- **Location**: watchapp/src/c/settings_window.c, settings_window.h
- **Responsibility**: main settings menu (Colour, Invert, ETA display, ETA label, Arrow side, Glyph pack, Distance units, Backlight, Vibration, About - `ROW_ABOUT` 9 of 10) and sub-windows (colour list, glyph pack preview, backlight, vibration).
- **Depends on**: theme.h, about_window.h, early_warning.

### Watchapp Settings Store and Theme
- **Location**: watchapp/src/c/theme.c, theme.h
- **Responsibility**: setting getters/setters persisted with `persist_write_*` under keys 1-15 (`PERSIST_KEY_ACCENT` ... `PERSIST_KEY_EARLY_WARN_INTENSITY`); theme colours; vibration patterns; RGB backlight tint.
- **Depends on**: Pebble persist, light and vibes APIs, early_warning.

### Watchapp Pure Text Modules
- **Location**: watchapp/src/c/eta_text, glance_text, stops_text, panel_layout, early_warning (.c/.h)
- **Responsibility**: ETA parsing and countdown (REQ-WATCH-014), launcher subtitle (REQ-WATCH-019), stop count (REQ-WATCH-018), arrow/distance placement (#80), early-warning decision (REQ-WATCH-022). No Pebble APIs.

### Watchapp Host Unit Tests
- **Location**: watchapp/tests/test_*.c (about_text, early_warning, eta_text, glance_text, panel_layout, stops_text)
- **Responsibility**: host-compiled assertions per pure unit, run by scripts/test-watchapp-unit.sh.

### Watchapp Asset Tools
- **Location**: watchapp/tools/ (gen_maneuver_bitmaps.py, gen_connect_qr.py, gen_store_icon.py, capture_screenshots.py, touch_emulator.py); outputs in watchapp/resources/, watchapp/store/, screenshots/
- **Responsibility**: generate committed glyph/QR/icon bitmaps; drive emulators for screenshots (including `settings-about`) and touch checks.

## Protocol

### Protocol Definition and Generator
- **Location**: protocol/ (protocol-definition.json, generate.py, check_consistency.py); scripts/generate-protocol.sh
- **Responsibility**: single protocol definition; emits Protocol.kt and protocol.h with a DO-NOT-EDIT banner; `--check` mode; consistency with examples/protocol-definition.json.

## Android Companion

### Phone Protocol Codec
- **Location**: android/app/src/main/java/com/pebblentn/app/protocol/ (ProtocolCodec.kt, AppMessage.kt, Protocol.kt generated)
- **Responsibility**: encode state (always with APP_VERSION) and compatibility errors; decode WATCH_READY / WATCH_REQUEST_STATE; UTF-8-safe text fitting.

### Phone Watch Transport
- **Location**: protocol/WatchTransport.kt, pebble/PebbleWatchTransport.kt, pebble/PebbleAppMessageMapper.kt, pebble/WatchListenerService.kt
- **Responsibility**: send/launch over PebbleKit 2 with one relaunch-and-retry on `FailedDifferentAppOpen`; inbound bridge via `WatchInboundBus`; synthetic WATCH_READY on `onAppOpened`; companion/watch link status.
- **Depends on**: PebbleKit 2 Vendored Library.

### Navigation Controller
- **Location**: pebble/NavigationController.kt
- **Responsibility**: serializes reducer access with a mutex, runs effects, bounded exponential retry (3 attempts), autonomous READY after 1.5 s settle, persists state, exposes `WatchLinkStatus`.
- **Depends on**: Navigation Session Reducer, Phone Protocol Codec, `WatchTransport`, `NavigationStateRepository`.

### Navigation Session Reducer
- **Location**: core/NavigationSessionReducer.kt, NavigationState.kt, NavigationInstruction.kt, Maneuver.kt, DistanceQuantizer.kt, WatchSettings.kt
- **Responsibility**: pure session logic: start/stop, dedupe, quantization, launch-once, READY sync and major-version check, staleness, flags.

### Notification Pipeline
- **Location**: notification/ (+ icon/), core/AppAllowlist.kt
- **Responsibility**: listener service, allowlist gate before reading text, minimal snapshot, serial queue, large-icon maneuver recognition (#74), debug capture.

### Rule Engine
- **Location**: rules/
- **Responsibility**: strict ruleset parsing, validation, layered precedence (user over official), condition evaluation, extractors, distance/duration parsing, regex time budget, rule preview.

### Persistence Layer
- **Location**: data/, data/db/, android/app/schemas/room/
- **Responsibility**: Room database v8 (navigation state, debug events, official rulesets, user rules, app settings) with migration tests; DataStore repositories (watch settings, appearance, enablement, presence, feedback).

### Compose UI
- **Location**: ui/ (MainActivity, onboarding, dashboard, apps, rules, debug, share, theme, format, components)
- **Responsibility**: onboarding and notification-access disclosure, dashboard with setup checklist and watch link, rule browsing/editing, debug history, diagnostics sharing.

### Update and Ruleset Signing
- **Location**: update/
- **Responsibility**: GitHub release check, version comparison, signed ruleset download, Ed25519 verification, atomic activation with rollback.

### Diagnostics Export
- **Location**: export/
- **Responsibility**: full, redacted and rules-only exports, size cap, share-sheet hand-off.

### PebbleKit 2 Vendored Library
- **Location**: android/pebblekit/
- **Responsibility**: Rebble PebbleKit Android 2 client (sender, info retriever, app picker, listener service, dictionary model), built from source.

### Fixture Publisher
- **Location**: android/fixture-publisher/
- **Responsibility**: debug-only app posting controlled navigation-like notifications.

## Data and Tooling

### Rule Data and Schemas
- **Location**: rules/catalog/, rules/bundled/, rules/fixtures/, schemas/, examples/
- **Responsibility**: navigation-app catalog, official rulesets per app and language, sanitized fixtures; staged into the APK by Gradle `syncRuleData`.

### Rule Workbench
- **Location**: tools/rule-workbench/
- **Responsibility**: Python rule regression (`workbench.py regression`) and localized Google Maps ruleset generation.

### Route Capture Harness
- **Location**: tools/route-capture/
- **Responsibility**: plays fixed routes in an Android emulator, records navigation notifications, checks them against bundled rules, files per-app GitHub issues (#55).

### Build and Release Scripts
- **Location**: scripts/, .githooks/pre-commit, VERSION
- **Responsibility**: build-all, build-watchapp, test-all, test-watchapp-unit, validate-*, generate-protocol, version sync (`version.sh`), changelog, emulator and install helpers; pre-commit checks.

### CI Workflows
- **Location**: .github/workflows/
- **Responsibility**: android, watchapp, rules, release, play-release, codeql (detail in [code-quality-assessment.md](code-quality-assessment.md)).

### Specification and Requirements
- **Location**: spec/, requirements/, docs/, MANIFEST.sha256.json, AGENTS.md
- **Responsibility**: numbered product/architecture/data/UI/testing/release specs and REQ-* requirements; SHA-256 integrity manifest; agent contract.
