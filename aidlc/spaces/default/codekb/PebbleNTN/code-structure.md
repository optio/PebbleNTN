# Code Structure

## Repository Layout

| Path | Contents |
|---|---|
| `android/app/src/main/java/com/pebblentn/app/` | Companion source (114 Kotlin files at c50710e), organised by feature package |
| `android/app/src/test/`, `src/androidTest/` | 69 JVM/Robolectric test sources; 1 instrumented test |
| `android/app/schemas/room/` | Exported Room schemas v1-v8 (migration tests) |
| `android/app/src/main/res/` | Resources; `values/strings.xml` (426 lines, English only) |
| `android/pebblekit/` | Vendored PebbleKit Android 2 1.2.0 (26 Kotlin files + AIDL) |
| `android/fixture-publisher/` | Debug app with synthetic notifications |
| `watchapp/src/c/` | Watchapp C (14 `.c`, headers, `generated/protocol.h`) |
| `watchapp/tests/` | Host C tests, `test_<unit>.c` paired 1:1 with `src/c/<unit>.c` |
| `watchapp/tools/`, `resources/`, `store/` | Python asset generators, bitmaps and QR, store art |
| `protocol/` | Protocol JSON and generator |
| `rules/` (`catalog/`, `bundled/<app>/<lang>.json`, `fixtures/`), `schemas/`, `examples/` | Rule data and JSON Schemas |
| `tools/rule-workbench/`, `tools/route-capture/` | Maintainer CLIs |
| `scripts/`, `.githooks/`, `.github/` | Build, test, release automation, CI, issue templates |
| `spec/`, `requirements/`, `docs/` | Specification, REQ-* IDs, status log, privacy, release checklist |
| `fastlane/`, `fdroid/`, `screenshots/` | Store metadata, F-Droid recipe, watch screenshots |

## Android Package Organisation (`com.pebblentn.app`)

| Package | Classification | Notes |
|---|---|---|
| `core/` | Model/Entity + Service (pure) | `NavigationSessionReducer`, `NavigationState`, `NavigationInstruction`, `Maneuver`, `WatchSettings`, `DistanceQuantizer`, `EpochClock`, `Hashing`, `AppAllowlist` |
| `protocol/` | Utility (pure codec) + interface | `ProtocolCodec`, `AppMessage`, `WatchTransport`/`WatchLink`/`SendResult`, generated `Protocol.kt` |
| `pebble/` | Adapter + Service | `NavigationController`, `PebbleWatchTransport`, `PebbleAppMessageMapper`, `WatchListenerService` + `WatchInboundBus` |
| `notification/`, `notification/icon/` | Controller/Handler + Service | listener, dispatcher (privacy gate), queue, snapshot factory, capture processor, maneuver heuristic, large-icon matcher |
| `rules/` | Service (pure) | engine, conditions, extractors, parsers, `SafeRegex`, codec, validator, preview, asset repository |
| `data/`, `data/db/` | Repository/DAO + Migration | Room entities and DAOs (v8 with 7 migrations), SharedPreferences repositories |
| `update/` | Service + Adapter | `UpdateCheckRepository` (also the stored watchapp version), `AppVersions` (tolerant and strict compare), `InstallSource`/`InstallSources` (v0.0.73), `ReleaseFetcher`/`GitHubReleaseFetcher`, `RuleUpdateRepository`, signature verifier |
| `export/` | Service | export builder, redactor, exporter, share manager |
| `system/` | Adapter | notification access, listener refresher, install receiver |
| `catalog/` | Model | navigation-app catalog |
| `di/` | Configuration | `AppContainer`, asset catalog source, installed apps provider |
| `ui/` | Controller + View (Compose) | `MainActivity` (NavHost, state collection), `dashboard/` (`DashboardScreen`, pure helpers `UpdateCard.kt` `updateCardModel`, `UpdatePrompt.kt`, `SetupChecklist.kt` `SetupProgress`), `rules/`, `debug/`, `share/`, `apps/`, `onboarding/`, `components/`, `format/`, `theme/` |

## Watchapp Module Organisation

| File | Classification | Pebble APIs |
|---|---|---|
| `main.c` | Controller + View + lifecycle | yes (AppMessage, layers, persist, timers, glance) |
| `settings_window.c/.h` | View (menus) | yes |
| `about_window.c/.h` | View | yes |
| `update_notice_window.c/.h` | View (full-screen #94 notice) | yes |
| `theme.c/.h` | Configuration + Repository (persist keys 1-15) | yes |
| `about_text`, `glance_text`, `phone_version`, `update_notice`, `version_compare` (v0.0.73), `eta_text`, `stops_text`, `panel_layout`, `early_warning` (`.c/.h`) | Utility (pure) | no; host-tested |
| `generated/protocol.h` | Configuration (generated, read-only) | no |

## Code Patterns

- **Pure core plus adapters**: domain logic receives time (`EpochClock`), I/O (`ReleaseFetcher`) and transport (`WatchTransport`) through interfaces with fakes in tests (`FakeWatchTransport`, `FakeFetcher`).
- **StateFlow repositories**: SharedPreferences-backed repositories expose `StateFlow`s and `@Volatile` caches for callback-thread reads (`AppEnabledRepository`, `WatchSettingsRepository`, `UpdateCheckRepository`, `WatchappPresenceRepository`).
- **Stateless Compose screens**: `DashboardScreen` takes plain parameters and lambdas (about 40, all but two defaulted), and `MainActivity.AppNavHost` collects flows and passes values. There is no dashboard ViewModel. Decision logic is pulled into pure top-level functions beside the screen (`updateCardModel`, `updatePrompt`, `SetupProgress`) so it is unit-tested without Compose.
- **Dashboard rows**: private composables `Section`, `ToggleRow`, `NavRow` and `InfoRow(title, value, dimmed)` in `DashboardScreen.kt`. The Status section is a master-switch `ToggleRow`, the access chip, then `InfoRow`s "Watch", "Last navigation notification" and "Last sent to watch"; an absent value falls back to a `*_none` string (`value?.let { ... } ?: stringResource(...)`).
- **Effect-returning reducer**: `reduce(state, event) -> ReducerResult(state, effects)`, run under a mutex by `NavigationController`.
- **Watch pure-unit split**: each piece of formatting or decision logic is a Pebble-free `.c/.h` with a host test linked against that unit alone (so `update_notice.c` keeps its own capped copy instead of calling `phone_version.c`). `main.c` owns the persisted state and the UI.
- **Requirement tagging**: comments cite REQ IDs and issue numbers throughout.

## Dependency Graph Notes

There are no circular package dependencies. `ui` -> `di` -> everything; `pebble` -> `core`, `protocol`, `data`; `notification` -> `rules`, `data`; `update` -> `rules`, `data.db`. Watch: `main.c` -> all pure units, `update_notice_window`, `settings_window`, `theme`; `settings_window` -> `about_window` -> `about_text`. External libraries are listed in [dependencies.md](dependencies.md).
