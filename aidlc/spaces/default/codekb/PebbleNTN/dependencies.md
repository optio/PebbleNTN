# Dependencies

## External Dependencies (Android, `android/gradle/libs.versions.toml`)

| Library | Version | Used by | Purpose |
|---|---|---|---|
| androidx.core:core-ktx | 1.15.0 | app, pebblekit | Kotlin extensions |
| androidx.lifecycle runtime-ktx / viewmodel-compose | 2.8.7 | app | ViewModels, lifecycle scope |
| androidx.activity:activity-compose | 1.9.3 | app | Compose host activity |
| androidx.navigation:navigation-compose | 2.8.5 | app | NavHost |
| androidx.compose BOM | 2024.12.01 | app | ui, ui-graphics, tooling, material3, material-icons-core |
| androidx.room runtime/ktx/compiler/testing | 2.7.2 | app | database, migration tests |
| androidx.datastore:datastore-preferences | 1.1.1 | pebblekit; also declared by `:app` (build.gradle.kts) but unused in app code | PebbleKit app-picker storage |
| kotlinx-serialization-json | 1.7.3 | app | rules, state, GitHub JSON |
| kotlinx-coroutines-android | 1.9.0 | app, pebblekit (`api`) | concurrency |
| com.jakewharton.timber | 5.0.1 | app | logging |
| co.touchlab:kermit | 2.0.4 | pebblekit | logging (upstream) |
| androidx.annotation | 1.9.1 | pebblekit | annotations |
| junit 4.13.2, robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, androidx.test core 1.6.1, ext-junit 1.2.1, espresso 3.6.1 | - | app tests | testing |

Gradle plugins: AGP 8.13.2, Kotlin Android, serialization and Compose 2.3.10, KSP 2.3.10. Repositories: google() and mavenCentral() only (`FAIL_ON_PROJECT_REPOS`). Gradle wrapper 8.13 is pinned by SHA-256.

## External Dependencies (other)

- Pebble SDK 3 and pebble-tool (CI installs `latest`, so it is not pinned). The `package.json` `sdkVersion` is `"3"`; the touch APIs (`app_touch_navigation_enable`, `window_set_touch_bridge_disabled`, and the recognizers #12 needs) come from the installed SDK's emery headers, which are no-op macros on the other platforms according to #12. Touch testing in the emery emulator needs `--vnc` on every `pebble` command (VNC port 5901).
- Python: `jsonschema` (rules validation, CI), `pyqrcode` (QR asset generation tool only).
- GitHub Actions: checkout@v7, setup-java@v5, setup-android@v4, setup-python@v6, cache@v6, upload-artifact@v7, codeql-action@v4.
- Runtime services: the GitHub Releases API (update check), and the Pebble / Core Devices mobile app as the PebbleKit 2 companion (`SEND_DATA_TO_WATCH` service). The Core Devices app does forward the watch's own AppMessages (`WATCH_READY` with key 11) to `WatchListenerService` once the watchapp declares `companionApp` (`watchapp/package.json`, package `com.pebblentn.app`): confirmed on a real phone with v0.0.74, where the dashboard "Watchapp version" row shows the value. The earlier statement that it does not forward them is outdated; it survives in code comments, REQ-ANDROID-019, `spec/200-architecture/Pebble.md` and the implementation status log (see [code-quality-assessment.md](code-quality-assessment.md)). Distribution clients (F-Droid clients, Obtainium) are known to the code only by installer package name and launch intent (`InstallSources`, v0.0.73), with no library dependency.

PebbleKit 2 dictionary types (`PebbleDictionaryItem`: Int8/UInt8/Int16/UInt16/Int32/UInt32/Text/Bytes) are converted by `PebbleAppMessageMapper`; outbound uses only `Int32` and `Text`, inbound `Bytes` is dropped. A settings design that packs values into a byte array would therefore depend on changing that mapper and `AppMessage`.

## Internal Cross-Package Dependencies

```mermaid
flowchart TD
  APP[":app"] --> PK[":pebblekit"]
  APP -.assets.-> RULES["rules/catalog + rules/bundled"]
  APPTEST[":app tests"] -.resources.-> FIX["rules/fixtures"]
  DEF["protocol/protocol-definition.json"] -.generate.py.-> KT["Protocol.kt"]
  DEF -.generate.py.-> H["watchapp generated/protocol.h"]
  EX["examples/protocol-definition.json"] -.check_consistency.-> DEF
  VER["VERSION"] -.version.sh.-> GR["app build.gradle.kts"]
  VER -.version.sh.-> PJ["watchapp/package.json"]
  VER -.version.sh.-> MC["watchapp main.c APP_VERSION_STR"]
  WB["tools/rule-workbench"] -.reads.-> RULES
  RC["tools/route-capture"] -.imports workbench.-> WB
```

Text fallback: `:app` depends on `:pebblekit` and stages rule data as assets and fixtures as test resources. The protocol JSON generates the Kotlin and C constants and must agree with the protected example. `VERSION` drives three version locations. route-capture imports the workbench engine.

## Version Coupling

The phone and watch share one version (`VERSION`, 0.0.76 at 4779760, versionCode 76; 0.0.75 at 3727ddc, where `APP_VERSION_STR` and `watchapp/package.json` both read 0.0.75; 0.0.74 at 8f08e02; versionCode = major*10000 + minor*100 + patch = 74). The release workflow tags `v<version>`, which is the `tag_name` the update check reads. F-Droid metadata (`fdroid/metadata/com.pebblentn.app.yml`) tracks tags (`UpdateCheckMode: Tags`), so F-Droid builds lag the GitHub release (the intent's "about a day" warning). It uses `Binaries:` (the GitHub APK) for reproducible-build verification. The Rebble store watchapp auto-updates on the watch, so after a release the watch is normally ahead of a phone that needs a manual update. That is what makes a watch-higher-than-phone comparison a valid "update available" signal, as long as both versions follow the shared `VERSION` line.
