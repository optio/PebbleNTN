# Code Structure

## Repository Layout

| Path | Contents |
|---|---|
| `android/app/src/main/java/com/pebblentn/app/` | Companion source (114 Kotlin files at c50710e, 116 at 4779760, 123 at 6dec020), organised by feature package |
| `android/app/src/test/`, `src/androidTest/` | 69 JVM/Robolectric test sources (71 at 4779760, 79 at 6dec020; Robolectric pinned to `sdk=34` in `src/test/resources/robolectric.properties`); 1 instrumented test |
| `android/app/schemas/room/` | Exported Room schemas v1-v8 (migration tests) |
| `android/app/src/main/res/` | Resources; `values/strings.xml` (431 lines at 4779760, English only, content descriptions named `cd_*`); `drawable-nodpi/` holds the 13 `ic_maneuver_*.png` icons, mapped from the `Maneuver` enum by `ui/format/DisplayLabels.maneuverIcon`, and since #102 the 87 `watch_preview_*` PNGs (100 files at 6dec020); `values/strings.xml` is 588 lines at 6dec020; `values/themes.xml` is one `android:Theme.Material.NoActionBar` style (Compose draws the rest) |
| `android/pebblekit/` | Vendored PebbleKit Android 2 1.2.0 (26 Kotlin files + AIDL) |
| `android/fixture-publisher/` | Debug app with synthetic notifications |
| `watchapp/src/c/` | Watchapp C (17 `.c` at 4779760, headers, `generated/protocol.h`) |
| `watchapp/tests/` | Host C tests, `test_<unit>.c` paired 1:1 with `src/c/<unit>.c` |
| `watchapp/tools/`, `resources/`, `store/` | Python asset generators, bitmaps and QR, store art |
| `protocol/` | Protocol JSON and generator |
| `rules/` (`catalog/`, `bundled/<app>/<lang>.json`, `fixtures/`), `schemas/`, `examples/` | Rule data and JSON Schemas |
| `tools/rule-workbench/`, `tools/route-capture/` | Maintainer CLIs |
| `scripts/`, `.githooks/`, `.github/` | Build, test, release automation, CI, issue templates |
| `spec/`, `requirements/`, `docs/` | Specification, REQ-* IDs, status log, privacy, release checklist |
| `fastlane/`, `fdroid/`, `screenshots/` | Store metadata (`fastlane/metadata/android/en-US/`: title, short and full description, `images/icon.png`, three `phoneScreenshots/`, 62 per-versionCode `changelogs/`; sizes in [code-quality-assessment.md](code-quality-assessment.md) "Play release findings"), F-Droid recipe, watch screenshots (`screenshots/`: 81 PNGs = 27 names x 3 sizes, 0.9-2.4 KB each, plus README) |

## Android Package Organisation (`com.pebblentn.app`)

| Package | Classification | Notes |
|---|---|---|
| `core/` | Model/Entity + Service (pure) | `NavigationSessionReducer`, `NavigationState`, `NavigationInstruction`, `Maneuver`, `WatchSettings` (phone-side flags config, not the watchapp's settings), `DistanceQuantizer`, `EpochClock`, `Hashing`, `AppAllowlist`; #102 asks for its Kotlin wrap-around revision helper here |
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
| `ui/` | Controller + View (Compose) | `MainActivity` (NavHost, state collection), `dashboard/` (`DashboardScreen`, pure helpers `UpdateCard.kt` `updateCardModel`, `UpdatePrompt.kt` `updatePrompt`, `WatchAppVersionRow.kt` `watchAppVersionText` (v0.0.74), `SetupChecklist.kt` `SetupProgress`), `rules/`, `debug/`, `share/`, `apps/`, `onboarding/`, `watchsettings/` (#102), `components/`, `format/`, `theme/` |

## Watchapp Module Organisation

| File | Classification | Pebble APIs |
|---|---|---|
| `main.c` | Controller + View + lifecycle (1,261 lines at 3727ddc, 1,407 at 4779760) | yes (AppMessage, layers, persist keys 16-18, timers, glance, click config for SELECT/UP/DOWN, swipe and tap recognizers on emery, shortcut banner) |
| `settings_window.c/.h` | View (menus; 741 lines at 3727ddc, 971 at 4779760): main menu `ROW_*` 0-10 plus doors for colour, glyph pack, vibration, Shortcuts (with the shared action list) and, on RGB hardware, backlight with its colour list | yes |
| `shortcut_apply.c/.h` | Service (v0.0.76): runs a `ShortcutAction` through the `theme.c` setters and writes the notice text | yes (via `theme.h`) |
| `qr_code.c/.h` | Utility (QR bitmap for the connection hint) | yes |
| `about_window.c/.h` | View | yes |
| `update_notice_window.c/.h` | View (full-screen #94 notice) | yes |
| `theme.c/.h` | Configuration + Repository (persist keys 1-15 and, since v0.0.76, 19-27) | yes |
| `about_text`, `glance_text`, `phone_version`, `update_notice`, `version_compare` (v0.0.73), `eta_text`, `stops_text`, `panel_layout`, `early_warning`, `shortcuts` (v0.0.76) (`.c/.h`) | Utility (pure) | no; host-tested |
| `generated/protocol.h` | Configuration (generated, read-only) | no |

## Code Patterns

- **Pure core plus adapters**: domain logic receives time (`EpochClock`), I/O (`ReleaseFetcher`) and transport (`WatchTransport`) through interfaces with fakes in tests (`FakeWatchTransport`, `FakeFetcher`).
- **StateFlow repositories**: SharedPreferences-backed repositories expose `StateFlow`s and `@Volatile` caches for callback-thread reads (`AppEnabledRepository`, `WatchSettingsRepository`, `UpdateCheckRepository`, `WatchappPresenceRepository`).
- **Stateless Compose screens**: `DashboardScreen` takes plain parameters and lambdas (about 40, all but two defaulted), and `MainActivity.AppNavHost` collects flows and passes values. There is no dashboard ViewModel. Decision logic is pulled into pure top-level functions beside the screen (`updateCardModel`, `updatePrompt`, `SetupProgress`) so it is unit-tested without Compose.
- **Dashboard rows**: private composables `Section`, `ToggleRow`, `NavRow` and `InfoRow(title, value, dimmed)` in `DashboardScreen.kt`. The Status section is a master-switch `ToggleRow`, the access chip, then `InfoRow`s "Watch", "Watchapp version" (v0.0.74), "Last navigation notification" and "Last sent to watch"; an absent value falls back to a `*_none` string (`value?.let { ... } ?: stringResource(...)`).
- **Effect-returning reducer**: `reduce(state, event) -> ReducerResult(state, effects)`, run under a mutex by `NavigationController`.
- **Watch pure-unit split**: each piece of formatting or decision logic is a Pebble-free `.c/.h` with a host test linked against that unit alone (so `update_notice.c` keeps its own capped copy instead of calling `phone_version.c`). `main.c` owns the persisted state and the UI.
- **Watch settings pattern**: one `static` value per setting in `theme.c`, a getter, a setter that clamps out-of-range ids to the default and persists at once (always, even when the value is unchanged; only `record_vibe_last` skips an unchanged write), and a range-checked read in `settings_load` (the three bools are read as stored, unchecked). Menu rows cycle in place with `(value + 1) % COUNT` (the early-warning pattern skips Off) or open a door (`push_*_window`), then call `notify_change()`. The shortcut slots are stored as one byte each at `20 + slot` and sanitised by `shortcut_valid_action`.
- **Preview images on the phone**: static PNGs in `res/drawable-nodpi/` mapped from an enum by an exhaustive `when` (`DisplayLabels.maneuverIcon`), drawn with `painterResource`. Release builds shrink resources (`isShrinkResources = true`), so preview drawables must be referenced statically like this, or kept explicitly, not looked up by a built name.
- **Watch platform guards**: `PBL_COLOR` (accent list, previews), `PBL_ROUND` (insets, centred headers), `PBL_RGB_BACKLIGHT` (backlight door and tint), `PBL_PLATFORM_EMERY` (touch bridge calls), `!PBL_PLATFORM_APLITE` (app glance). Layout size comes from the runtime screen width (`w >= 200`).
- **Pure units cannot use Pebble headers**: `scripts/test-watchapp-unit.sh` builds each `test_<unit>.c` with `src/c/<unit>.c` alone, under `gcc -std=c11 -Wall -Wextra -Werror`. `theme.h` includes `pebble.h`, so a pure unit cannot use its enums, names or setters; it takes counts and ids as plain integers.
- **Requirement tagging**: comments cite REQ IDs and issue numbers throughout.

## Dependency Graph Notes

There are no circular package dependencies. `ui` -> `di` -> everything; `pebble` -> `core`, `protocol`, `data`; `notification` -> `rules`, `data`; `update` -> `rules`, `data.db`. Watch: `main.c` -> all pure units, `update_notice_window`, `settings_window`, `shortcut_apply`, `qr_code`, `theme`; `settings_window` -> `theme`, `shortcut_apply`; `shortcut_apply` -> `shortcuts`, `theme`; `about_window` -> `about_text`; `theme` -> `early_warning`, `shortcuts` (their enums). External libraries are listed in [dependencies.md](dependencies.md).
