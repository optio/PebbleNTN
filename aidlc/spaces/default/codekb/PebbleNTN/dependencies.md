# Dependencies

## External Dependencies (Android, `android/gradle/libs.versions.toml`, not re-read at 61787f5)

| Library | Version | Used by | Purpose |
|---|---|---|---|
| androidx.core:core-ktx | 1.15.0 | app, pebblekit | Kotlin extensions |
| androidx.lifecycle runtime-ktx / viewmodel-compose | 2.8.7 | app | ViewModels, lifecycle scope |
| androidx.activity:activity-compose | 1.9.3 | app | Compose host activity |
| androidx.navigation:navigation-compose | 2.8.5 | app | NavHost |
| androidx.compose BOM | 2024.12.01 | app | ui, ui-graphics, tooling, material3, material-icons-core |
| androidx.room runtime/ktx/compiler/testing | 2.7.2 | app | database, migration tests |
| androidx.datastore:datastore-preferences | 1.1.1 | pebblekit; also declared by `:app` but unused in app code | PebbleKit app-picker storage |
| kotlinx-serialization-json | 1.7.3 | app | rules, catalog, state, snapshots, exports, GitHub JSON |
| kotlinx-coroutines-android | 1.9.0 | app, pebblekit (`api`) | concurrency |
| com.jakewharton.timber | 5.0.1 | app | logging |
| co.touchlab:kermit | 2.0.4 | pebblekit | logging (upstream) |
| androidx.annotation | 1.9.1 | pebblekit | annotations |
| junit 4.13.2, robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, androidx.test core 1.6.1, ext-junit 1.2.1, espresso 3.6.1 | - | app tests | testing |

Gradle plugins: AGP 8.13.2, Kotlin Android, serialization and Compose 2.3.10, KSP 2.3.10. Repositories: google() and mavenCentral() only (`FAIL_ON_PROJECT_REPOS`). Gradle wrapper 8.13 is pinned by SHA-256.

## External Dependencies (other)

- Pebble SDK 3 and pebble-tool (CI installs `latest`, so it is not pinned).
- Python: `jsonschema` (rules and catalog validation, CI; not pinned), `pyqrcode` (QR asset generation tool only).
- GitHub Actions: checkout@v7, setup-java@v5, setup-android@v4, setup-python@v6, cache@v6, upload-artifact@v7, codeql-action@v4.
- Runtime services: the GitHub Releases API (update check), and the Pebble / Core Devices mobile app as the PebbleKit 2 companion. The companion forwards the watch's own AppMessages (`WATCH_READY`, SETTINGS) to `WatchListenerService` once the watchapp declares `companionApp`, and reports the watchapp opening and closing (`onAppOpened`, `onAppClosed`). It refuses a send with `FailedDifferentAppOpen` while another watch app is in front, which is what makes `PebbleWatchTransport.send` relaunch our watchapp. Whether the companion honours `stopAppOnTheWatch` for our UUID, and whether it reports `onAppClosed` for a stop the phone itself requested, are unverified (needed by #132).
- Navigation apps are runtime dependencies known only by package name: their notifications and, for icon matching, their own `maneuver_*` drawables. No navigation-app SDK is linked.
- Google Maps' Android 16 `ProgressStyle` trip card (#132): the speed estimate would depend on its undocumented meaning of `EXTRA_PROGRESS` / `EXTRA_PROGRESS_MAX` (metres along the route, per the issue) and on its `subText` "Arrive h:mm" wording, which is localized and changes between Google Maps versions. Other navigation apps (OsmAnd, Organic Maps, CoMaps, Vela Maps) were not checked for progress fields; for them only the distance countdown and an ETA from the rules' `secondaryText` would be available.
- Vela Maps (#120, `app.vela`) is known only through its notification text; its large icon is drawn at runtime.
- route-capture runtime services: the Android SDK emulator and adb, F-Droid's package API, GitHub and Codeberg release APIs, download.osmand.net, routing.openstreetmap.de (`fetch-routes` only), and `gh` for `--publish`.

PebbleKit 2 dictionary types (`PebbleDictionaryItem`: Int8/UInt8/Int16/UInt16/Int32/UInt32/Text/Bytes) are converted by `PebbleAppMessageMapper`; outbound uses only `Int32` and `Text`, inbound integers of any width become `Int` (the watch sends its settings as uint8), inbound `Bytes` is dropped.

## Internal Cross-Package Dependencies

```mermaid
flowchart TD
  APP[":app"] --> PK[":pebblekit"]
  APP -.assets.-> RULES["rules/catalog + rules/bundled"]
  APPTEST[":app tests"] -.resources.-> FIX["rules/fixtures"]
  APPTEST -.resources.-> RULES
  CAT["rules/catalog/navigation-apps.json"] -.packages must equal.-> BUN["every rule in rules/bundled/appId"]
  CAT -.packages must appear in.-> MAN["AndroidManifest queries"]
  DEF["protocol/protocol-definition.json"] -.generate.py.-> KT["Protocol.kt"]
  DEF -.generate.py.-> H["watchapp generated/protocol.h"]
  EX["examples/protocol-definition.json"] -.check_consistency.-> DEF
  KT -.SETTING keys in WatchSetting order.-> WSM["WatchSettingsModel.kt"]
  H -.SETTING keys in SettingId order.-> SSC["settings_sync.c"]
  WSM -.mirrors table.-> SSC
  VER["VERSION"] -.version.sh.-> GR["app build.gradle.kts"]
  VER -.version.sh.-> PJ["watchapp/package.json"]
  VER -.version.sh.-> MC["watchapp main.c APP_VERSION_STR"]
  WB["tools/rule-workbench"] -.reads.-> RULES
  RC["tools/route-capture"] -.imports workbench.-> WB
```

Text fallback: `:app` depends on `:pebblekit` and stages rule data as assets and test resources. Each catalog entry's package set must equal its rules' and appear in `<queries>`. The protocol JSON generates the Kotlin and C constants and must agree with the protected example. The settings table is kept twice by hand, `WatchSettingsModel.kt` on the phone and `settings_sync.c` on the watch, both in the order of the generated SETTING_* keys. `VERSION` drives three version locations. route-capture imports the workbench engine.

### Settings table coupling (checked at 61787f5)

A synced setting exists in eight places that must agree: the two protocol JSON files (plus the MANIFEST hash), the generated `Protocol.kt` and `protocol.h`, `SettingId` with `kRules`/`kDefaults` in `settings_sync.c` (and `sync_persist_key`), the enum and its `_Static_assert` in `theme.c`/`theme.h` with a named getter and setter, the `WatchSetting` enum with `WatchSettingsUi` title and label mappings and string arrays, and the watch menu in `settings_window.c` (and its hand copy in `capture_screenshots.py` when a main row is added). Checks that hold them together: the C `_Static_assert`s (count 23, contiguous keys, enum counts), `generate.py --check`, `check_consistency.py`, `WatchSettingsModelTest.defaultsMatchTheWatch` and `twentyThreeSettingsInProtocolKeyOrder`, `test_settings_sync.c` (count, persist keys), `ProtocolCodecTest` (23 decoded), and the exhaustive `when`s. Nothing compares the two tables' defaults or counts mechanically across languages; the tests copy the values.

### Controller fan-out for a hide policy (checked at 61787f5)

`DebugCaptureProcessor` holds the snapshot and calls `navigationController.onInstruction(instruction)`; `NavigationController` owns the transport, the reducer state (`launchedSessionId`, `watchReady`, current session) and the settings mirror (`watchSettings.state.value.mirror` gives the When driving and Driving speed values and the watch's units); `WatchListenerService` feeds opens (as synthetic READY messages) and closes through `WatchInboundBus`; `DiagnosticExporter` reads only `DebugHistoryRepository` plus lambdas from `AppContainer`. A hide policy therefore needs the snapshot's raw fields to reach the controller (a new callback or a wider `onInstruction`), an explicit "app opened" signal rather than the synthetic READY, and a new exporter input for its log.

### Earlier coupling notes

- **Package-list consistency** (1c7f4c1): catalog, rules, `<queries>` and fixtures; two checks hold the first three together.
- **Rule families and the icon lookup** (2fa2aa5): `AppContainer` builds one `RuleFamilies` and passes it to the rule repository, engine and icon recognizer.
- **Install source fan-out** (494a6dd): `AppContainer.installSource` and the install record are read by the catalog view, update policy, update repository, `MainActivity`, the dashboard and the exporter.

## Version Coupling

The phone and watch share one version (`VERSION`, 0.0.81 at 61787f5, where `APP_VERSION_STR` reads "0.0.81"; versionCode = major*10000 + minor*100 + patch). The release workflow tags `v<version>`. F-Droid metadata tracks tags, so F-Droid builds lag the GitHub release. The Rebble store watchapp auto-updates, so after a release the watch is normally ahead of the phone, which matters for #132: a new watchapp with the driving settings can meet an older phone (it ignores the extra SETTING keys), and a new phone can meet an older watchapp (protocol 1.3, no driving keys; `fromWatch` would fill defaults).

The protocol version (1.3 at 61787f5) is independent of the app version; a watch and phone on different minors interoperate by ignoring unknown keys and events, and the reducer rejects only a different major.

Bundled ruleset versions (`rulesetVersion`) are independent of the app version.
