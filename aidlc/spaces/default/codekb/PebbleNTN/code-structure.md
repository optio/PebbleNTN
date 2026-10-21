# Code Structure

## Repository Layout

| Path | Contents |
|---|---|
| android/app/ | Companion app (`com.pebblentn.app`): src/main (Kotlin, ~120 files), src/test (66 JVM/Robolectric tests), src/androidTest (1), schemas/room (Room v1-v8) |
| android/pebblekit/ | Vendored PebbleKit Android 2 (`io.rebble.pebblekit2`, Kotlin + AIDL) |
| android/fixture-publisher/ | Debug app that posts synthetic navigation notifications |
| watchapp/ | Pebble watchapp: src/c (C sources), src/c/generated/protocol.h, tests/ (host C tests), resources/images (glyph packs, QR), store/ (store art), tools/ (Python asset and emulator tools), package.json, wscript |
| protocol/ | protocol-definition.json, generate.py, check_consistency.py |
| rules/ | catalog/ (navigation-app catalog), bundled/<app>/<lang>.json (official rules), fixtures/ |
| schemas/ | ruleset.schema.json, app-catalog.schema.json |
| examples/ | example-ruleset.json, protocol-definition.json (spec contract copy) |
| spec/, requirements/ | Numbered specification and REQ-* requirement files (integrity-protected) |
| scripts/ | Build, test, validation, versioning, changelog, emulator helpers |
| tools/ | rule-workbench (Python), route-capture (Python harness + Java device helpers, route/scenario JSON) |
| docs/, fastlane/, fdroid/, screenshots/ | Status and policy docs, store metadata, F-Droid metadata, emulator screenshots |
| .github/, .githooks/ | CI workflows, issue templates, pre-commit hook |

## Android Package Organization (`com.pebblentn.app`)

Feature-and-layer hybrid; classification by package:

| Package | Classification | Notes |
|---|---|---|
| core/ | Model/Entity + Service | Pure domain: `Maneuver`, `NavigationInstruction`, `NavigationState`, `NavigationSessionReducer`, `AppAllowlist`, `DistanceQuantizer`, `EpochClock` |
| notification/ (+ icon/) | Controller/Handler + Service | Listener service, dispatcher (privacy gate), snapshot factory, serial queue, large-icon recognizer |
| rules/ | Service + Utility | Rule model, codec, validator, engine, extractors, `SafeRegex` time budget |
| pebble/ | Adapter + Service | `NavigationController`, `PebbleWatchTransport`, `PebbleAppMessageMapper`, `WatchListenerService` |
| protocol/ | Model + Utility | `AppMessage`, generated `Protocol.kt`, `ProtocolCodec`, `WatchTransport` interface |
| data/ (+ db/) | Repository/DAO | Room database, DAOs, entities; DataStore-backed repositories |
| update/ | Service | Release check, signed ruleset download and verification |
| export/ | Service | Export builder, redactor, share manager |
| ui/ | Static/UI | Compose screens and view models by feature (dashboard, apps, rules, debug, share, onboarding) |
| di/, system/ | Configuration | Manual DI container, installed-apps provider, notification-access helpers |

## Watchapp Source Organization (`watchapp/src/c`)

| File | Classification | Responsibility |
|---|---|---|
| main.c | Controller + UI | Window, layers, AppMessage inbox/outbox, handshake watchdog, backlight, vibration, glance |
| settings_window.c/.h | UI | Settings menu tree (10 main rows, sub-windows) |
| theme.c/.h | Configuration + Repository | Settings state, `persist_*` keys 1-15, theme colours, vibe patterns |
| about_window.c/.h | UI | About page; `AboutInfo` struct |
| about_text.c/.h | Utility (pure) | About status line and details text |
| eta_text, glance_text, stops_text, panel_layout, early_warning (.c/.h) | Utility (pure) | Host-testable formatting and decision logic |
| generated/protocol.h | Generated | Protocol constants (`PBNTN_*`) |

## Code Patterns

- **Pure core, thin shell**: Kotlin reducer and codec are pure; on the watch every unit without Pebble APIs has a `test_<unit>.c` twin (enforced by scripts/test-watchapp-unit.sh, which fails if a test has no matching unit).
- **Requirement IDs in comments**: headers and KDoc cite `REQ-*` and issue numbers (for example about_text.h cites REQ-WATCH-020, #48).
- **Static module state on the watch**: each window keeps `static` pointers and copies its input struct (`s_info = *info`).
- **Generated constants only**: both sides reference `Protocol.Keys.*` / `PBNTN_KEY_*`, never literals.
