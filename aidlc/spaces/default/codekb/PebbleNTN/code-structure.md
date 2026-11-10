# Code Structure

## Repository Layout

| Path | Contents | Classification |
|---|---|---|
| `android/app/` | Companion app `com.pebblentn.app` (Kotlin, Compose): 136 main Kotlin files, 107 unit-test files, Room schemas v1-8 | Application |
| `android/pebblekit/` | Vendored PebbleKit Android 2 (`io.rebble.pebblekit2`), Kotlin + AIDL | Library (third-party source) |
| `android/fixture-publisher/` | Debug app posting synthetic navigation notifications | Test tool |
| `watchapp/` | Pebble C watchapp: `src/c/` (42 files incl. generated `protocol.h`), `tests/` (13 host tests), `resources/` (maneuver bitmaps, 3 glyph packs), `tools/`, `store/` | Application |
| `protocol/` | `protocol-definition.json`, `generate.py`, `check_consistency.py` | Code generator |
| `rules/` | `bundled/<app>/<lang>.json` (22 rulesets), `catalog/navigation-apps.json`, `fixtures/<app>.json` (5) + README | Rule data and test fixtures |
| `schemas/` | `ruleset.schema.json`, `app-catalog.schema.json` | Contracts (ruleset schema hash-protected) |
| `examples/` | `example-ruleset.json`, `protocol-definition.json` (protected mirror) | Contracts |
| `tools/` | `rule-workbench/` (CLI + Google Maps generator), `route-capture/` (emulator harness), Play store graphics | Maintainer tools (Python) |
| `scripts/` | build, test, version, validate, release changelog, git hooks installer | Build scripts |
| `spec/`, `requirements/`, `docs/` | Specification (hash-protected), REQ files, implementation status, store/privacy docs | Documentation |
| `.github/` | 6 workflows, issue templates, funding | CI/CD |
| `fastlane/`, `fdroid/`, `screenshots/` | Store metadata, F-Droid recipe, watch screenshots | Distribution assets |

## Android App Packages (`android/app/src/main/java/com/pebblentn/app/`)

| Package | Files | Role | Key types |
|---|---|---|---|
| `notification/` | 9 | Listener, privacy gate, snapshot, serial queue, capture processor | `NavigationNotificationListenerService`, `NotificationDispatcher`, `NotificationSnapshot`, `NotificationSnapshotFactory`, `DebugCaptureProcessor` |
| `notification/icon/` | 5 | Large-icon-to-drawable-name matching (#74) | `LargeIconRecognizer`, `IconMatcher`, `AppIconReferences` |
| `rules/` | 12 | Declarative rule engine | `RulesetModel` (`Ruleset`, `Rule`, `Condition`, `RuleOutput`, 9 `Extractor` types), `RuleEngine`, `ExtractorRunner`, `ConditionEvaluator`, `SnapshotFields`, `DistanceParser`, `DurationParser`, `SafeRegex`, `RuleValidator`, `RulesetCodec`, `AssetRuleRepository`, `RulePreviewService` |
| `core/` | 14 | Pure domain: instruction, maneuver, reducer, state, quantizer, settings models, driving | `NavigationInstruction`, `Maneuver`, `NavigationSessionReducer`, `NavigationState`, `DrivingSpeedEstimator`, `DrivingPolicy`, `WatchSettingsModel`, `WatchSettingsSync` |
| `pebble/` | 4 | Watch bridge | `NavigationController`, `PebbleWatchTransport`, `PebbleAppMessageMapper`, `WatchListenerService` |
| `protocol/` | 4 | Wire vocabulary | `Protocol` (generated), `ProtocolCodec`, `AppMessage`, `WatchTransport` |
| `catalog/` | 3 | Navigation-app catalog, rule families, user-added packages | `NavigationAppCatalog`, `RuleFamilies`, `EffectiveCatalog` |
| `data/` + `data/db/` | 18 + 11 | Repositories (DataStore, Room), DAOs, entities | `DebugHistoryRepository`, `UserRuleRepository`, `EnabledAppRepository`, `PebbleNtnDatabase` (v8) |
| `export/` | 4 | Diagnostics export and share | `ExportBuilder`, `Redactor`, `DiagnosticExporter` |
| `update/` | 10 | Update checks, install source, signed ruleset updates | `UpdateCheckRepository`, `RuleUpdateRepository`, `RulesetSignatureVerifier` |
| `system/` | 3 | Android system hooks | `NotificationListenerRefresher`, `NavigationAppInstallReceiver` |
| `ui/` | 35 | Compose screens and view models | `MainActivity`, dashboard, rules, debug, watchsettings, share, onboarding, apps |
| `di/` | 3 | Manual DI container | `AppContainer` |

## Rule Data Structure

- `rules/bundled/<app>/<lang>.json`: one `Ruleset` (`schemaVersion` 1, `rulesetVersion` `<app>-<lang>-YYYY.MM.N`, `rules[]`). Rule ids end with the language suffix (`osmand-turn-right-de`). Variants of a base rule take priority +1 (`-street`, `-then`, `-distance-title`).
- OsmAnd ladder (all languages): roundabout 190, U-turn 180, sharp 170, slight 160, keep 120, turn 100, straight/continue 50, arrive 40. The German file adds `-street` and `-then` variants at +1 that read `bigText`.
- `rules/fixtures/<app>.json`: `{ note, fixtures: [{ name, source, locale, packageName, snapshot{title,text,subText,bigText,iconDrawable}, comment, expected{matched, maneuver, distanceMeters, ruleId, primaryText, secondaryText, noDistance, noPrimaryText, stopsRemaining} }] }`.

## Code Patterns

- **Pure function + injected clock**: `EpochClock`, `zone: () -> ZoneId`, `nowEpochSeconds` parameters make time-dependent logic testable.
- **Sealed result types**: `ExtractionResult` (`Text`, `Num`, `None`), `RuleValidationResult`, `PreviewResult`, `ReducerEffect`, `NavigationState`.
- **kotlinx.serialization polymorphism**: extractors use `@SerialName` with `classDiscriminator = "type"`; strict parsing rejects unknown keys and types.
- **Reducer and effects**: `reduce(state, event) → (state, effects)`; the controller runs the effects under a mutex.
- **Privacy gate by lambda**: the snapshot builder is a lambda only invoked after the allowlist passes.
- **Generated constants**: never hand-edit `Protocol.kt` / `protocol.h` (AGENTS.md rule 5).
- **Watch logic split for host tests**: pure C units (`eta_text.c`, `glance_text.c`, ...) each with `tests/test_<unit>.c`.
- **KDoc with traceability**: classes cite REQ ids and GitHub issues (#74, #76, #113, #132).

## File Classification Summary

| Category | Examples |
|---|---|
| Model/Entity | `RulesetModel.kt`, `NotificationSnapshot.kt`, `NavigationInstruction.kt`, `data/db/*Entity.kt` |
| Controller/Handler | `NavigationNotificationListenerService.kt`, `WatchListenerService.kt`, `NavigationController.kt` |
| Service/UseCase | `RuleEngine.kt`, `ExtractorRunner.kt`, `NavigationSessionReducer.kt`, `DrivingSpeedEstimator.kt`, `DebugCaptureProcessor.kt` |
| Repository/DAO | `data/*Repository.kt`, `data/db/*Dao.kt`, `rules/RuleRepository.kt` |
| Utility | `DistanceParser.kt`, `DurationParser.kt`, `SafeRegex.kt`, `SnapshotFields.kt`, `Hashing.kt` |
| Configuration | `di/AppContainer.kt`, `build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml` |
| Migration | `PebbleNtnDatabase.kt` (`MIGRATION_1_2`..`MIGRATION_7_8`), `android/app/schemas/room/` |
| Test | `android/app/src/test/**`, `watchapp/tests/`, `rules/fixtures/`, `tools/route-capture/tests/` |
| Static/Asset | `res/values/strings.xml` (443 strings), `watchapp/resources/images/`, `screenshots/` |

The component list and their dependencies are recorded once in [component-inventory.md](component-inventory.md).
