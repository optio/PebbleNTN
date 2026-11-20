# Component Inventory

Each component is listed once here; other artifacts link to these headings. Android paths are under `android/app/src/main/java/com/pebblentn/app/` unless shown in full; tests under the matching `src/test/` package.

## Companion App Components

### Notification Intake
- **Location**: `notification/` (listener service, `NotificationDispatcher`, `NotificationSnapshot(Factory)`, `SerialProcessingQueue`, `NotificationProcessor`, `DebugCaptureProcessor`, `LastEligibleNotificationStore`, `ManeuverHeuristic`)
- **Responsibility**: Privacy gate before any content read; immutable snapshot; serial processing; runs the rule engine; records debug events.
- **Depends on**: Rule Engine, Large Icon Recognizer, Persistence, Watch Link.

### Large Icon Recognizer
- **Location**: `notification/icon/` (5 files)
- **Responsibility**: Match a notification's large icon to the navigation app's turn drawable name (`iconDrawable`).

### Rule Engine
- **Location**: `rules/` (13 files: `RulesetModel`, `RuleEngine`, `ExtractorRunner`, `ConditionEvaluator`, `SnapshotFields`, `DistanceParser`, `DurationParser`, `ArrivalClock`, `SafeRegex`, `RuleValidator`, `RulesetCodec`, `RuleRepository`, `RulePreviewService`)
- **Responsibility**: Strict parsing, validation, layered precedence, family and locale filtering, extraction, instruction assembly incl. `compactText` and arrival-clock ETA.
- **Tests**: 19 files incl. per-app `*RulesRegressionTest`.

### Navigation App Catalog
- **Location**: `catalog/` (3 files), `rules/catalog/navigation-apps.json`, `schemas/app-catalog.schema.json`
- **Responsibility**: 21 supported apps and packages, rule families (`rulesFrom`), user-added packages.

### Session Core
- **Location**: `core/NavigationInstruction.kt`, `Maneuver.kt`, `NavigationSessionReducer.kt`, `NavigationState.kt`, `DistanceQuantizer.kt`, `AppAllowlist.kt`, `EpochClock.kt`, `Hashing.kt`, `WatchSettings.kt` (phone fixed flags)
- **Responsibility**: Normalized instruction; pure reducer (sessions, launch once, dedup, quantization, ETA carry-over, staleness, stop).

### Driving Detection
- **Location**: `core/DrivingSpeedEstimator.kt`, `core/DrivingPolicy.kt`, `data/DrivingLogRepository.kt`
- **Responsibility**: Speed estimate from navigation progress; hide/relaunch the watchapp above the chosen speed (#132).

### Watch Settings Sync
- **Location**: `core/WatchSettingsModel.kt`, `core/WatchSettingsSync.kt`, `core/PreviewCatalog.kt`, `data/WatchSettingsMirrorRepository.kt`, `data/WatchSettingsRepository.kt` (phone flags: auto-launch)
- **Responsibility**: The 27 `WatchSetting`s (protocol keys 18-44) with value counts, capability needs and defaults; capability/range rules; revisioned two-way merge; pending edits queued while the watchapp is closed; persisted mirror.
- **Tests**: `WatchSettingsModelTest`, `WatchSettingsSyncTest`, `WatchSettingsQueueTest`, `WatchSettingsMirrorRepositoryTest`, `WatchSettingsRepositoryTest`, `PreviewCatalogTest`.

### Watch Settings Screen
- **Location**: `ui/watchsettings/` (`WatchSettingsUi.kt`, `WatchSettingsScreen.kt`, `OptionListScreen.kt`, `DrivingSection.kt`), strings in `android/app/src/main/res/values/strings.xml` lines 473-675, routes in `ui/MainActivity.kt`
- **Responsibility**: Phone UI of the watch settings (REQ-ANDROID-021, -024, -025). States WAITING / OUTDATED / READY; intro text; queued notice; sections Appearance, Directions, Arrival time, Distance, Backlight, Vibration, Early warning, Shortcuts, then When driving.
- **Row rendering**: `SettingRow` is a Material 3 `ListItem` with `headlineContent` = title, `supportingContent` = current value label, trailing chevron; the whole row opens `OptionListScreen`. No per-row help text.
- **Explanations today**: screen intro `watch_settings_explanation`; section `note` (only Directions: `watch_directions_text_explanation`, drawn as `bodySmall` text under the card); `watch_driving_explanation` under the When driving card. `OptionListScreen` shows preview, queued card and values, no description.
- **Visibility rules**: Backlight colour only with RGB backlight; Touch only on touch watches; the 5 gestures only while Touch is on; early warning repeat, pattern, strength only when a distance is set; When driving only for protocol 1.4+; Directions text for every watchapp that sends settings.
- **Tests**: `WatchSettingsUiTest` (12), `WatchSettingsScreenTest` (14, Robolectric, exact English text), `StoreScreenshotsTest` (opt-in, `2-watch-settings`).

### Watch Link
- **Location**: `pebble/` (`NavigationController`, `PebbleWatchTransport`, `PebbleAppMessageMapper`, `WatchListenerService`), `protocol/` (`Protocol` generated, `ProtocolCodec`, `AppMessage`, `WatchTransport`)
- **Responsibility**: Executes reducer effects, encodes (Full/Compact road line) and sends with retry, decodes READY/requests/settings, launches and closes the watchapp, re-sends on Directions text change, applies When driving.
- **Tests**: `NavigationControllerTest`, `ProtocolCodecTest`, `WatchLinkTest`, `DrivingEmulatorReplayTest`; instrumented `ManualOpenWithAutoLaunchOffTest`.

### Persistence
- **Location**: `data/` (18), `data/db/` (11, Room v8), `android/app/schemas/room/`
- **Responsibility**: Debug history, user rules, official ruleset store, app enablement, state cache, DataStore preferences.
- **Tests**: repository tests, `PebbleNtnDatabaseMigrationTest`.

### Diagnostics Export
- **Location**: `export/` (4 files)
- **Responsibility**: Full, privacy-safe, rules-only exports (schema 7), Sharesheet sharing.

### Updates
- **Location**: `update/` (10 files)
- **Responsibility**: Install source, version comparison, optional GitHub check, update card, signed rulesets (disabled).

### Companion UI
- **Location**: `ui/` except `watchsettings/` (MainActivity, apps, components, dashboard, debug, format, onboarding, rules, share, theme), `res/values/strings.xml`
- **Responsibility**: Onboarding, dashboard, navigation apps, rules and editor, debug history, share diagnostics; navigation host.

### App Container
- **Location**: `di/` (3 files), `PebbleNtnApplication.kt`, `system/` (3 files)
- **Responsibility**: Manual wiring; `setWatchSetting`; listener refresh; install receiver.

### PebbleKit 2 Vendored
- **Location**: `android/pebblekit/`
- **Responsibility**: Rebble PebbleKit Android 2 client from source (F-Droid).

### Fixture Publisher
- **Location**: `android/fixture-publisher/`
- **Responsibility**: Debug app posting synthetic navigation notifications.

## Data Components

### Bundled Rule Packs
- **Location**: `rules/bundled/<app>/<lang>.json` (22: google-maps 10 incl. `any`, osmand 9, vela-maps 1, organic-maps 1, comaps 1)
- **Responsibility**: Official rules shipped as assets (Gradle `syncRuleData`).

### Rule Fixtures and Regression
- **Location**: `rules/fixtures/` (5 sets + README), `*RulesRegressionTest.kt`
- **Responsibility**: Sanitized snapshots with expected outputs, checked by the Kotlin engine and the Python workbench.

## Watch Components

### Watchapp
- **Location**: `watchapp/src/c/` (43 files; `main.c` 1,479 lines, `settings_window.c` 1,120, `settings_sync.c`, `theme.c`, `shortcuts.c`, `early_warning.c`, `eta_text.c`, `glance_text.c`, `driving_label.c`, `revision.c`, `generated/protocol.h`), `watchapp/resources/`, `watchapp/package.json`, `watchapp/wscript`
- **Responsibility**: Render navigation, on-watch settings menu (titles and value subtitles, no help text) and sync, shortcuts, early warning, glance, update notice, about page.
- **Tests**: 13 host C tests in `watchapp/tests/`.

## Tooling Components

### Protocol Generator
- **Location**: `protocol/`, `scripts/generate-protocol.sh`
- **Responsibility**: Emits `Protocol.kt` and `protocol.h`; `--check` in CI.

### Rule Workbench
- **Location**: `tools/rule-workbench/`
- **Responsibility**: Validate, diff, scaffold, sanitize, test, regression; generates localized Google Maps packs.

### Route Capture Harness
- **Location**: `tools/route-capture/`
- **Responsibility**: Drive navigation apps along public routes in an emulator, record and evaluate notifications.

### Store Graphics Generator
- **Location**: `tools/play_store_graphics.py`, `tools/test_play_store_graphics.py`, `tools/play-store-sources/` (phone `1-dashboard`, `2-watch-settings`, `3-accent-colour`, `4-navigation-apps`; watch shots)
- **Responsibility**: Builds fastlane store images from the rendered screenshots; `--check` in `scripts/test-all.sh`.

### Build and Release Scripts
- **Location**: `scripts/`, `.githooks/pre-commit`, `.github/workflows/`
- **Responsibility**: Bootstrap, build, test-all, validation, version sync, changelog, emulator helpers; CI and release pipelines.

### Specification and Contracts
- **Location**: `spec/`, `requirements/`, `schemas/`, `examples/`, `MANIFEST.sha256.json`, `AGENTS.md`, `docs/`
- **Responsibility**: Product and architecture spec, REQ ids, schemas, 34 hash-protected files, implementation status.

### Store and Distribution Assets
- **Location**: `fastlane/metadata/`, `fdroid/`, `screenshots/`, `watchapp/store/`
- **Responsibility**: Play, F-Droid and Pebble store listings and screenshots.
