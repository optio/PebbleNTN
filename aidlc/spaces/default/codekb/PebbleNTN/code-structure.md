# Code Structure

Components are described once in [component-inventory.md](component-inventory.md); this file shows layout, classification and patterns.

## Repository Layout

```
android/                 Gradle multi-module build (app, pebblekit, fixture-publisher)
  app/src/main/java/com/pebblentn/app/
    catalog/  core/  data/ data/db/  di/  export/  notification/ notification/icon/
    pebble/  protocol/  rules/  system/  update/
    ui/ (MainActivity, apps, components, dashboard, debug, format, onboarding, rules, share, theme, watchsettings)
  app/src/main/res/      values/strings.xml (English only), colors, themes, drawables (previews, icons)
  app/src/test/          106 JVM test files (JUnit 4 + Robolectric), resources/driving/*.json
  app/src/androidTest/   1 instrumented test
  app/schemas/room/      Room schema exports v1-v8
  pebblekit/             vendored Rebble PebbleKit Android 2
  fixture-publisher/     debug app posting synthetic notifications
watchapp/                Pebble C watchapp: src/c/ (43 files), tests/ (13), resources/, tools/, store/
protocol/                protocol-definition.json + generate.py + check_consistency.py
rules/                   bundled/<app>/<lang>.json (22), catalog/navigation-apps.json, fixtures/ (5 + README)
schemas/                 ruleset and app-catalog JSON Schemas
tools/                   rule-workbench/, route-capture/, play_store_graphics.py, play-store-sources/
scripts/                 build, test, validate, version, changelog, emulator helpers
spec/ requirements/ docs/ examples/   specification, REQ ids, status, privacy, store docs
fastlane/ fdroid/ screenshots/        store listings and screenshots
.github/workflows/ .githooks/         CI/CD and pre-commit
```

## Android Package Classification

| Package | Files | Category |
|---|---|---|
| `core/` | 14 | Model/Entity + Service (pure domain: instruction, reducer, watch settings model and sync, driving policy, preview catalog) |
| `rules/` | 13 | Service + Utility (engine, extractors, parsers, validator, codec) |
| `notification/`, `notification/icon/` | 9 + 5 | Controller/Handler (listener, dispatcher) + Service |
| `pebble/` | 4 | Service + Adapter (controller, PebbleKit transport, listener service, mapper) |
| `protocol/` | 4 | Model + Utility (generated `Protocol.kt`, codec, message, transport interface) |
| `data/`, `data/db/` | 18 + 11 | Repository/DAO (Room, DataStore) |
| `export/` | 4 | Service (export builder, redactor, share) |
| `update/` | 10 | Service (version, install source, update check, signed rulesets) |
| `catalog/` | 3 | Service + Model |
| `di/`, `system/`, `PebbleNtnApplication.kt` | 3 + 3 + 1 | Configuration |
| `ui/**` | 39 | Controller (Compose screens, view models, pure UI rules) |

## Watch Settings Area (intent focus)

| File | Lines | Role |
|---|---|---|
| `core/WatchSettingsModel.kt` | 287 | `WatchSetting` enum (27 entries in protocol key order, value counts, capability needs, defaults), `WatchSettingsRules` (valid, offered, options), `WatchSettingsModel`, `WatchCapabilities`, `WatchPlatform`, value constants |
| `core/WatchSettingsSync.kt` | 147 | Revision rule, merge, phone edit, outgoing; `WatchSettingsSyncState` (mirror, pending, protocol minor) |
| `core/PreviewCatalog.kt` | 224 | Drawable per visual setting value and platform |
| `ui/watchsettings/WatchSettingsUi.kt` | 229 | Pure rules: status, sections (with optional `note`), title `@StringRes` per setting, value label `@ArrayRes`, options, preview, driving helpers, accent colours |
| `ui/watchsettings/WatchSettingsScreen.kt` | 167 | Scaffold; notice cards; `SettingsSection` (title, outlined card, optional note); `SettingRow` (`ListItem`: headline title, supporting value, chevron) |
| `ui/watchsettings/OptionListScreen.kt` | 158 | Value list with radio buttons, preview image, queued card, accent swatches |
| `ui/watchsettings/DrivingSection.kt` | 144 | When driving: switch, speed slider, reset, explanation |
| `res/values/strings.xml` lines 473-675 | — | All Watch settings strings and value arrays |

## Code Patterns

- **Pure logic objects** (`WatchSettingsUi`, `WatchSettingsRules`, `WatchSettingsSync`, `RuleEngine`, `NavigationSessionReducer`) with Compose/Android at the edges.
- **Exhaustive `when` over enums** for resource mapping (`WatchSettingsUi.title`, `valueLabels`): adding an enum entry fails compilation until mapped.
- **Immutable data classes + `copy`/`with`**; `StateFlow` state exposed by repositories/controller.
- **Manual DI** in `AppContainer`; routes in `MainActivity` (`watch-settings`, `watch-settings/{setting}`).
- **KDoc citing REQ ids and issue numbers** on nearly every declaration.
- **Test tags** as constants (`DrivingSectionTags`); most UI tests find nodes by English text.
- **Watch C**: modules per concern with host-testable pure parts (`*_text.c`, `revision.c`, `settings_sync.c`), windows in `settings_window.c`, `main.c`.
