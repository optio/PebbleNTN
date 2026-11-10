# Component Inventory

Each component is listed once here; other artifacts link to these headings. Paths are repo-relative; Android paths are under `android/app/src/main/java/com/pebblentn/app/` unless shown in full.

## Companion App Components

### Notification Intake
- **Location**: `notification/` (`NavigationNotificationListenerService`, `NotificationDispatcher`, `NotificationSnapshot`, `NotificationSnapshotFactory`, `SerialProcessingQueue`, `DebugCaptureProcessor`, `NotificationProcessor`, `LastEligibleNotificationStore`, `ManeuverHeuristic`)
- **Responsibility**: Privacy gate (master switch, then allowlist, before any content read); minimal immutable snapshot of selected extras; serial off-callback processing; runs the rule engine, records the event, forwards instruction and speed sample.
- **Depends on**: Rule Engine, Large Icon Recognizer, Persistence, Session Core (through `NavigationController` callbacks).
- **Tests**: `NotificationDispatcherTest`, `NotificationSnapshotFactoryTest`, `DebugCaptureProcessorTest`, `SerialProcessingQueueTest`, `ManeuverHeuristicTest`, `LastEligibleNotificationStoreTest`.

### Large Icon Recognizer
- **Location**: `notification/icon/`
- **Responsibility**: Compare a notification's large icon with the navigation app's own turn drawables on the phone, keep only the drawable name (`iconDrawable`) (#74). Candidates come from rules that `maneuverMap` on `iconDrawable`.
- **Depends on**: Rule Engine (rules as the candidate source), Navigation App Catalog (rule families).
- **Tests**: `IconMatcherTest`, `IconCandidatesTest`, `AppIconReferencesTest`, `IconResourcePackagesTest`.

### Rule Engine
- **Location**: `rules/` (12 files)
- **Responsibility**: Strict ruleset parsing and canonical form; semantic validation; layered precedence (user > downloaded > bundled, then priority, then id); rule-family and locale filtering; short-circuit conditions; fixed extractors; output assembly into `NavigationInstruction`; regex safety; per-rule trace; preview for the editor.
- **Key files**: `RulesetModel.kt`, `RuleEngine.kt`, `ExtractorRunner.kt`, `ConditionEvaluator.kt`, `SnapshotFields.kt`, `DistanceParser.kt`, `DurationParser.kt`, `SafeRegex.kt`, `RuleValidator.kt`, `RulesetCodec.kt`, `RuleRepository.kt`, `RulePreviewService.kt`.
- **Depends on**: Notification Intake (snapshot type), Session Core (`NavigationInstruction`, `Maneuver`), Navigation App Catalog (`RuleFamilies`), kotlinx.serialization.
- **Tests**: `RuleEngineTest`, `ExtractorRunnerTest`, `ConditionEvaluatorTest`, `DistanceDurationParserTest`, `StripLeadingDistanceTest`, `SnapshotFieldsTest`, `RuleValidatorTest`, `RulesetCodecTest`, `RulePreviewServiceTest`, `RuleFamiliesMatchingTest`, `RulesetVersionsTest`.
- **#134 touch points**: `DurationParser` (English units only); `RuleEngine.assemble` (only `duration` becomes now-relative ETA); `ExtractorRunner` (no clock extractor); `RulesetModel` + `RuleValidator` (closed extractor set).

### Bundled Rule Packs
- **Location**: `rules/bundled/<app>/<lang>.json` (22 files)
- **Responsibility**: Official rules shipped as app assets (staged by Gradle `syncRuleData`).

| App | Files (rules per file) | Outputs filled |
|---|---|---|
| `google-maps` | en (45), de/es/fr/it/nl/pl/pt (13, generated), zh (12), any (2) | maneuver, distance, primary, secondary = arrival clock from `subText` (en also from `text`), stops (en transit) |
| `osmand` | en, es, fr, it, nl, pl, pt, zh (12 each), de (29) | maneuver, distance, primary from `title`; de only: primary from `bigText` line 1 (`-street`/`-then` variants) and secondary = remaining distance from `bigText` line 2 |
| `vela-maps` | en (29) | maneuver, distance, primary, secondary = arrival clock from `text` |
| `organic-maps`, `comaps` | any (1 each) | maneuver from `iconDrawable`, distance, primary |

- **Notes**: no rule fills `etaEpochSeconds`. OsmAnd `zh` rules have no `locales` (they match any locale). Rule ids carry the language suffix.
- **Depends on**: Rule Engine schema; Rule Workbench generator (localized Google Maps).

### Rule Fixtures and Regression
- **Location**: `rules/fixtures/{google-maps,osmand,vela-maps,comaps,organic-maps}.json` + `README.md`; `android/app/src/test/java/com/pebblentn/app/rules/*RulesRegressionTest.kt`
- **Responsibility**: Sanitized snapshots with expected outputs, checked by the Kotlin engine and the Python workbench (both must agree). Fixture counts: google-maps 189 (18 locales), osmand 195 (12 locales), vela-maps 60, comaps 21, organic-maps 12. Only 10 OsmAnd fixtures (all `de`) carry `bigText`; OsmAnd fixtures are capture-backed only for en (6), it (5) and de (5).
- **Gap**: `OsmandRulesRegressionTest` never asserts `secondaryText`; no fixture schema field or assertion for `etaEpochSeconds`; tests pass `nowEpochSeconds = 0`.

### Navigation App Catalog
- **Location**: `catalog/` (`NavigationAppCatalog`, `RuleFamilies`, `EffectiveCatalog`, `CustomPackageValidator`), `rules/catalog/navigation-apps.json`, `schemas/app-catalog.schema.json`
- **Responsibility**: Supported apps and packages, rule families through `rulesFrom` (#113), user-added packages, Google Play view.
- **Entries (21)**: google-maps, microg-maps (`rulesFrom` google-maps), waze, osmand (`net.osmand`, `.plus`, `.dev`, `.huawei`), organic-maps, comaps, vela-maps, maps-me, mapy-com, amap, here-wego, sygic, yandex-navigator, yandex-maps, petal-maps, magic-earth, tomtom-go, tomtom-amigo, two-gis, baidu-maps, pebblentn-fixtures (debug).
- **Tests**: `NavigationAppCatalogTest`, `EffectiveCatalogTest`, `CustomPackageValidatorTest`, `scripts/test_validate_catalog.py`.

### Session Core
- **Location**: `core/NavigationInstruction.kt`, `Maneuver.kt`, `NavigationSessionReducer.kt`, `NavigationState.kt`, `DistanceQuantizer.kt`, `AppAllowlist.kt`, `EpochClock.kt`, `Hashing.kt`, `WatchSettings.kt`
- **Responsibility**: Normalized instruction model; pure reducer (session ids, launch once, dedup, quantization, ETA carry-over within 5 min, 30 s staleness, READY sync, stop); effects.
- **Tests**: `NavigationSessionReducerTest`, `NavigationInstructionTest`, `ManeuverTest`, `DistanceQuantizerTest`.

### Driving Detection
- **Location**: `core/DrivingSpeedEstimator.kt`, `core/DrivingPolicy.kt`, `data/DrivingLogRepository.kt`, `ui/watchsettings/DrivingSection.kt`
- **Responsibility**: Speed from Google Maps progress values and the arrival clock in `subText` (`arrivalMillis`); hide/relaunch the watchapp above a speed (#132).
- **Tests**: `DrivingSpeedEstimatorTest`, `DrivingPolicyTest`, `DrivingEmulatorReplayTest`, `DrivingLogRepositoryTest`.

### Watch Settings Sync
- **Location**: `core/WatchSettingsModel.kt`, `core/WatchSettingsSync.kt`, `core/PreviewCatalog.kt`, `data/WatchSettingsMirrorRepository.kt`, `data/WatchSettingsRepository.kt`, `ui/watchsettings/`
- **Responsibility**: Phone mirror of watch settings (ETA mode and label, theme, glyphs, shortcuts, early warning, driving), revisioned two-way sync (#102).
- **Tests**: `WatchSettingsModelTest`, `WatchSettingsSyncTest`, `WatchSettingsQueueTest`, `WatchSettingsMirrorRepositoryTest`, UI tests.

### Watch Link
- **Location**: `pebble/` (`NavigationController`, `PebbleWatchTransport`, `PebbleAppMessageMapper`, `WatchListenerService`), `protocol/` (`Protocol`, `ProtocolCodec`, `AppMessage`, `WatchTransport`)
- **Responsibility**: Runs reducer effects, encodes and sends AppMessages with retry, decodes inbound READY/request/settings, launches and closes the watchapp, link status.
- **Depends on**: PebbleKit 2, Session Core, Driving Detection, Watch Settings Sync.
- **Tests**: `NavigationControllerTest`, `ProtocolCodecTest`, `WatchLinkTest`, `FakeWatchTransport`; instrumented `ManualOpenWithAutoLaunchOffTest`.

### Persistence
- **Location**: `data/` (18 files), `data/db/` (11 files, Room v8, migrations 1→8), `android/app/schemas/room/`
- **Responsibility**: Debug history with grouping and traces, user rules, official ruleset store, app enablement, state cache, DataStore preferences (master switch, appearance, custom packages, feedback, watch presence).
- **Tests**: repository tests, `PebbleNtnDatabaseMigrationTest`.

### Diagnostics Export
- **Location**: `export/` (`ExportBuilder`, `Redactor`, `DiagnosticExporter`, `DiagnosticShareManager`)
- **Responsibility**: Full, privacy-safe and rules-only exports, size-capped, shared through the Sharesheet.
- **Tests**: `ExportBuilderTest`, `RedactorTest`, `DiagnosticExporterTest`.

### Updates
- **Location**: `update/` (10 files)
- **Responsibility**: Install source and record, version comparison, offline and optional GitHub update check, update card policy, signed official ruleset activation and rollback (disabled).
- **Tests**: one test per class under `test/.../update/`.

### Companion UI
- **Location**: `ui/` (35 files: dashboard, rules, debug, watchsettings, share, onboarding, apps, components, format, theme), `res/values/strings.xml` (443 strings)
- **Responsibility**: Onboarding and access, dashboard (status, last sent instruction with ETA), navigation apps and packages, official and user rules with editor and preview, debug history and detail, share diagnostics, watch settings.
- **Tests**: view model tests and Robolectric Compose UI tests, `StoreScreenshotsTest`.

### App Container
- **Location**: `di/AppContainer.kt`, `di/AssetCatalogSource.kt`, `di/InstalledAppsProvider.kt`, `PebbleNtnApplication.kt`, `system/`
- **Responsibility**: Manual wiring of every component; locale provider (`Locale.getDefault()`); listener refresh; install receiver.
- **Tests**: `NotificationListenerRefresherTest`, `NavigationAppInstallReceiverTest`, `BuildSmokeTest`.

### PebbleKit 2 Vendored
- **Location**: `android/pebblekit/`
- **Responsibility**: Rebble PebbleKit Android 2 client (sender, listener service, dictionary serialization, AIDL), vendored from source for F-Droid.

### Fixture Publisher
- **Location**: `android/fixture-publisher/`
- **Responsibility**: Debug app posting synthetic navigation notifications for manual end-to-end checks.

## Watch Components

### Watchapp
- **Location**: `watchapp/src/c/` (`main.c` 1,479 lines, `settings_window.c`, `settings_sync*.c`, `theme.c`, `eta_text.c`, `glance_text.c`, `stops_text.c`, `driving_label.c`, `early_warning.c`, `shortcuts.c`, `panel_layout.c`, `update_notice*.c`, `about*.c`, `qr_code.c`, `revision.c`, `version_compare.c`, `phone_version.c`, `generated/protocol.h`), `watchapp/resources/`, `watchapp/package.json`, `watchapp/wscript`
- **Responsibility**: Render navigation state (arrow bitmap, distance, road, ETA strip, stale marker, stops), on-watch settings and sync, shortcuts, early warning vibration, app glance countdown, update notice, about page.
- **Tests**: 13 host C tests in `watchapp/tests/` (`test_eta_text.c`, `test_glance_text.c`, ...) via `scripts/test-watchapp-unit.sh`.

## Tooling Components

### Protocol Generator
- **Location**: `protocol/` (`protocol-definition.json`, `generate.py`, `check_consistency.py`), `scripts/generate-protocol.sh`
- **Responsibility**: Single source for keys, events, flags, maneuvers, error codes, capabilities, platforms; emits `Protocol.kt` and `protocol.h`; `--check` in CI.

### Rule Workbench
- **Location**: `tools/rule-workbench/` (`workbench.py`, `gen_localized_google_maps.py`)
- **Responsibility**: inspect, validate, diff, scaffold, sanitize, promote, test, regression (Python subset engine, run in CI and pre-commit); generates `google-maps/{it,fr,es,de,nl,pt,pl}.json`.

### Route Capture Harness
- **Location**: `tools/route-capture/` (`routecap.py`, `routecap/`, `routes/`, `scenarios/` incl. `osmand-m3.json`, `device/`, `tests/`)
- **Responsibility**: Drive navigation apps along fixed public routes in an emulator per locale, record notifications, evaluate them with the workbench engine, publish misses as GitHub issues.

### Build and Release Scripts
- **Location**: `scripts/`, `.githooks/pre-commit`, `.github/workflows/`
- **Responsibility**: bootstrap, build-all, build-watchapp, test-all, test-watchapp-unit, validate-rules, validate-spec-assets, version sync, changelog, emulator and install helpers; CI and release pipelines.

### Specification and Contracts
- **Location**: `spec/`, `requirements/`, `schemas/`, `examples/`, `MANIFEST.sha256.json`, `AGENTS.md`, `docs/`
- **Responsibility**: Product and architecture specification, REQ ids, JSON schemas, protected-asset hashes, implementation status, privacy and store docs.

### Store and Distribution Assets
- **Location**: `fastlane/metadata/`, `fdroid/`, `screenshots/`, `watchapp/store/`, `tools/play_store_graphics.py`, `tools/play-store-sources/`
- **Responsibility**: Play, F-Droid and Pebble store listings, screenshots and generated graphics.
