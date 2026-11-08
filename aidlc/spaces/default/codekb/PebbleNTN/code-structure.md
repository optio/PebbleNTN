# Code Structure

## Repository Layout

| Path | Contents |
|---|---|
| `android/app/src/main/java/com/pebblentn/app/` | Companion source (114 Kotlin files at c50710e, 123 at 6dec020, 129 at 494a6dd, 131 tracked files at 61787f5), organised by feature package |
| `android/app/src/test/`, `src/androidTest/` | JVM/Robolectric test sources (92 at 494a6dd, 98 under `src/test/java/com/pebblentn/app/` at 61787f5; Robolectric pinned to `sdk=34` in `src/test/resources/robolectric.properties`); 1 instrumented test |
| `android/app/schemas/room/` | Exported Room schemas v1-v8 (migration tests) |
| `android/app/src/main/res/` | Resources; `values/strings.xml` (629 lines at 61787f5, English only, content descriptions named `cd_*`, the Watch settings strings and `watch_values_*` arrays at lines 474-629); `drawable-nodpi/` holds the 13 `ic_maneuver_*.png` icons and the `watch_preview_*` PNGs (#102); `values/themes.xml` is one `android:Theme.Material.NoActionBar` style |
| `android/pebblekit/` | Vendored PebbleKit Android 2 1.2.0 (26 Kotlin files + AIDL) |
| `android/fixture-publisher/` | Debug app with synthetic notifications |
| `watchapp/src/c/` | Watchapp C (20 `.c` at 61787f5, headers, `generated/protocol.h`) |
| `watchapp/tests/` | Host C tests, `test_<unit>.c` paired with `src/c/<unit>.c` (12 at 61787f5) |
| `watchapp/tools/`, `resources/`, `store/` | Python asset generators and emulator tools, bitmaps and QR, store art |
| `protocol/` | `protocol-definition.json` (protocol 1.3), `generate.py`, `check_consistency.py`, README |
| `rules/` (`catalog/`, `bundled/<app>/<lang>.json`, `fixtures/`), `schemas/`, `examples/` | Rule data and JSON Schemas (detail at c7b6ba0 in the Vela Maps findings) |
| `tools/rule-workbench/`, `tools/route-capture/` | Maintainer CLIs (rule workbench; route-capture harness, `out/` git-ignored) |
| `scripts/`, `.githooks/`, `.github/` | Build, test, release automation, CI, issue templates |
| `spec/`, `requirements/`, `docs/` | Specification, REQ-* IDs, status log, privacy, release checklist |
| `fastlane/`, `fdroid/`, `screenshots/` | Store metadata, F-Droid recipe, watch screenshots |

## Android Package Organisation (`com.pebblentn.app`)

| Package | Classification | Notes |
|---|---|---|
| `core/` | Model/Entity + Service (pure) | `NavigationSessionReducer` (events, effects, `ReducerState`), `NavigationState`, `NavigationInstruction`, `Maneuver`, `WatchSettings` (the phone's own flags: auto-launch plus three fixed flags), `WatchSettingsModel` (`WatchSetting` enum of 23, `WatchSettingsRules`, `WatchCapabilities`, `WatchPlatform`, `ShortcutActions`, `WatchValues`), `WatchSettingsSync` (revision rule, merge, phone edit, outgoing), `PreviewCatalog`, `DistanceQuantizer`, `EpochClock`, `Hashing`, `AppAllowlist`. The natural home for a pure speed estimator and hide/show decision (#132) |
| `protocol/` | Utility (pure codec) + interface | `ProtocolCodec` (state, compatibility error, inbound decode, `watchAppVersion`, `decodeWatchSettings`, `realWatchReadyMinor`, `encodeSettings`), `AppMessage`, `WatchTransport`/`WatchLink`/`SendResult`, generated `Protocol.kt` |
| `pebble/` | Adapter + Service | `NavigationController` (reducer under a mutex, effects, retries, settings sync under a second mutex, `watchappOpen`), `PebbleWatchTransport`, `PebbleAppMessageMapper`, `WatchListenerService` + `WatchInboundBus` (messages and closes) |
| `notification/`, `notification/icon/` | Controller/Handler + Service | listener, dispatcher (privacy gate), queue, snapshot factory (text fields plus `progress`/`progressMax`), capture processor (forwards only the instruction), maneuver heuristic, large-icon matcher |
| `rules/` | Service (pure) | engine, conditions, extractors, parsers, `SafeRegex`, codec, validator, preview, asset repository |
| `data/`, `data/db/` | Repository/DAO + Migration | Room entities and DAOs (v8), SharedPreferences repositories: `WatchSettingsRepository` (`pebblentn_settings`, auto-launch only), `WatchSettingsMirrorRepository` (`pebblentn_watch_settings`, the mirror, implements `WatchSettingsStore`), `DebugHistoryRepository` with `DebugEventGrouping`, `EnabledAppRepository` |
| `update/` | Service + Adapter | update check, versions, install source and record, update policy, rule signing |
| `export/` | Service | `ExportBuilder` (pure, schema 5), `Redactor`, `DiagnosticExporter`, `DiagnosticShareManager` |
| `system/` | Adapter | notification access, listener refresher, install receiver |
| `catalog/` | Model | navigation-app catalog, effective catalog with user-added packages, rule families |
| `di/` | Configuration | `AppContainer`, asset catalog source, installed apps provider |
| `ui/` | Controller + View (Compose) | `MainActivity` (NavHost with string routes, including `watch-settings` and `watch-settings/{setting}`), `dashboard/`, `watchsettings/` (`WatchSettingsScreen`, `OptionListScreen`, pure `WatchSettingsUi`), `rules/`, `debug/`, `share/`, `apps/`, `onboarding/`, `components/`, `format/`, `theme/`. No `@Preview` composables exist |

## Watchapp Module Organisation

| File | Classification | Pebble APIs |
|---|---|---|
| `main.c` | Controller + View + lifecycle (1,479 lines at 61787f5) | yes (AppMessage inbox and an outbox with pending bits and retry, layers, persist keys 16-18, timers, glance, click config, emery recognizers, shortcut banner) |
| `settings_window.c/.h` | View (966 lines): main menu `ROW_*` 0-10 with About first (`ROW_ABOUT 0`) and Shortcuts last, tables `kMainTitles`/`kMainCycles`; doors for colour, glyph pack, vibration, Shortcuts (with the shared action list) and, on RGB hardware, backlight; `settings_window_refresh` | yes |
| `settings_sync_glue.c/.h` | Service (#102): revision at persist key 28, `settings_sync_write`, `settings_sync_apply`, edit handler | yes |
| `settings_sync.c/.h` | Utility (pure, #102): `SettingId`, `kRules`, `kDefaults`, `sync_valid`, `sync_value_count`, `sync_default`, `sync_is_bool`, `sync_persist_key`, `sync_decide`, build caps and platform | no; host-tested |
| `revision.c/.h` | Utility (pure, #102): wrap-at-99 revision | no; host-tested |
| `theme.c/.h` | Configuration + Repository: one `s_values[SET_COUNT]` array, `settings_get`/`settings_store`/`settings_set`/`settings_load`, name tables, colours, vibration and backlight helpers, persist key 27 (last vibration pattern) | yes |
| `shortcut_apply.c/.h` | Service: runs a `ShortcutAction` through `settings_set` | yes (via `theme.h`) |
| `qr_code.c/.h`, `about_window.c/.h`, `update_notice_window.c/.h` | Utility / View | yes |
| `about_text`, `glance_text`, `phone_version`, `update_notice`, `version_compare`, `eta_text`, `stops_text`, `panel_layout`, `early_warning`, `shortcuts` (`.c/.h`) | Utility (pure) | no; host-tested |
| `generated/protocol.h` | Configuration (generated, read-only) | no |

## Code Patterns

- **Pure core plus adapters**: domain logic receives time (`EpochClock`), I/O (`ReleaseFetcher`), storage (`WatchSettingsStore`) and transport (`WatchTransport`) through interfaces with fakes in tests (`FakeWatchTransport`, `InMemoryWatchSettingsStore`, `FakeFetcher`).
- **StateFlow repositories**: SharedPreferences-backed repositories expose `StateFlow`s and `@Volatile` caches for callback-thread reads.
- **Stateless Compose screens**: screens take plain parameters and lambdas; `MainActivity.AppNavHost` collects flows and passes values; decisions sit in pure helpers beside the screen (`updateCardModel`, `WatchSettingsUi.sections`/`options`/`status`).
- **Effect-returning reducer**: `reduce(state, event) -> ReducerResult(state, effects)`, run under a mutex by `NavigationController`. Time enters only as event timestamps; no production code emits a periodic event (`FreshnessChecked` is unused).
- **Settings sync beside the reducer**: `NavigationController` runs `WatchSettingsSync` under its own `settingsMutex`, outside the reducer.
- **Watch pure-unit split**: each piece of formatting or decision logic is a Pebble-free `.c/.h` with a host test linked against that unit (plus any `// Links:` unit). `main.c` owns the persisted state and the UI.
- **Watch settings pattern (since #102)**: one table row per setting in `settings_sync.c` (`kRules` count in 5 bits with `NEEDS_RGB`, `NEEDS_TOUCH`, `FROM_ONE` flags; `kDefaults`), one byte per value in `theme.c`'s `s_values`, a named getter and setter macro per setting in `theme.h` over `settings_get`/`settings_set`. A menu row that cycles in place lists its setting in `kMainCycles` (value + 1); a door has its own window, `MenuLayerCallbacks` table and `push_*_window`. Units-aware labels follow `early_warn_label` (`early_warn_distance_label(..., settings_units() == UNITS_IMPERIAL, ...)`).
- **Phone settings pattern (since #102)**: `WatchSetting` enum entries in protocol-key order with `valueCount`, `firstValue`, `needs`, `defaultValue`; exhaustive `when`s for title and label array in `WatchSettingsUi`; units-aware label arrays chosen by `imperial` (`watch_values_early_warn_distance_metric`/`_imperial`); rows grouped by `WatchSettingsUi.sections`.
- **Preview images on the phone**: static PNGs in `res/drawable-nodpi/` referenced by `R.drawable` constant in `PreviewCatalog`, checked by `PreviewCatalogTest`; only the seven visual settings have previews.
- **Watch platform guards**: `PBL_COLOR`, `PBL_ROUND`, `PBL_RGB_BACKLIGHT`, `PBL_PLATFORM_EMERY`, `!PBL_PLATFORM_APLITE`; capabilities are also reported at runtime through `sync_build_caps` (colour, round, RGB backlight, touch via `PBL_TOUCH`).
- **Code is heap on aplite**: menus use static const callback tables and name tables instead of switches to save code (comments in `settings_window.c` and `theme.c`).
- **Requirement tagging**: comments cite REQ IDs and issue numbers throughout.
- **Package lists, first entry is the main build**; **one regression test per app with rules**; **rule data as classpath resources in tests** (unchanged; see the Vela Maps findings).

## Dependency Graph Notes

There are no circular package dependencies. `ui` -> `di` -> everything; `pebble` -> `core`, `protocol`, `data`; `notification` -> `rules`, `data`; `export` -> `data`, `update`; `update` -> `rules`, `data.db`; `catalog` is read by `data`, `di` and `ui`. Watch: `main.c` -> all pure units, `settings_sync_glue`, `update_notice_window`, `settings_window`, `shortcut_apply`, `qr_code`, `theme`; `settings_sync_glue` -> `settings_sync`, `revision`, `theme`; `settings_sync` -> `revision`, `shortcuts`, `generated/protocol.h`; `settings_window` -> `theme`, `shortcut_apply`; `theme` -> `early_warning`, `settings_sync`, `shortcuts`. External libraries are listed in [dependencies.md](dependencies.md).
