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
| kotlinx-serialization-json | 1.7.3 | app | rules, catalog, state, GitHub JSON |
| kotlinx-coroutines-android | 1.9.0 | app, pebblekit (`api`) | concurrency |
| com.jakewharton.timber | 5.0.1 | app | logging |
| co.touchlab:kermit | 2.0.4 | pebblekit | logging (upstream) |
| androidx.annotation | 1.9.1 | pebblekit | annotations |
| junit 4.13.2, robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, androidx.test core 1.6.1, ext-junit 1.2.1, espresso 3.6.1 | - | app tests | testing |

Gradle plugins: AGP 8.13.2, Kotlin Android, serialization and Compose 2.3.10, KSP 2.3.10. Repositories: google() and mavenCentral() only (`FAIL_ON_PROJECT_REPOS`). Gradle wrapper 8.13 is pinned by SHA-256.

## External Dependencies (other)

- Pebble SDK 3 and pebble-tool (CI installs `latest`, so it is not pinned). The `package.json` `sdkVersion` is `"3"`; the touch APIs (`app_touch_navigation_enable`, `window_set_touch_bridge_disabled`, and the recognizers #12 needs) come from the installed SDK's emery headers, which are no-op macros on the other platforms according to #12. Touch testing in the emery emulator needs `--vnc` on every `pebble` command (VNC port 5901).
- Python: `jsonschema` (rules and catalog validation, CI; not pinned), `pyqrcode` (QR asset generation tool only).
- GitHub Actions: checkout@v7, setup-java@v5, setup-android@v4, setup-python@v6, cache@v6, upload-artifact@v7, codeql-action@v4.
- Runtime services: the GitHub Releases API (update check), and the Pebble / Core Devices mobile app as the PebbleKit 2 companion (`SEND_DATA_TO_WATCH` service). The Core Devices app does forward the watch's own AppMessages (`WATCH_READY` with key 11) to `WatchListenerService` once the watchapp declares `companionApp` (`watchapp/package.json`, package `com.pebblentn.app`): confirmed on a real phone with v0.0.74, where the dashboard "Watchapp version" row shows the value. The earlier statement that it does not forward them is outdated; it survives in code comments, REQ-ANDROID-019, `spec/200-architecture/Pebble.md` and the implementation status log (see [code-quality-assessment.md](code-quality-assessment.md)). Distribution clients (F-Droid clients, Obtainium) are known to the code only by installer package name and launch intent (`InstallSources`, v0.0.73), with no library dependency. The Play Store (`com.android.vending`) was not known to the code at 6dec020; since #114 it maps to `PLAY` (checked at 494a6dd). The full list of outbound destinations (GitHub issues and discussion, the support email, the Rebble store, Play links to navigation apps) is in [api-documentation.md](api-documentation.md) "Outbound links and drafts"; no SDK of any of those services is linked.
- Navigation apps are runtime dependencies known only by package name: their notifications (read through the listener) and, for icon matching, their own `maneuver_*` drawables (read by name through `createPackageContext`). No navigation-app SDK is linked. MicroG Maps (#113) is the Google Maps APK under another package name (`app.morphe.android.apps.maps`), so it depends on the same notification shape and drawable names; whether the renamed package still resolves those drawables by name is unverified.

PebbleKit 2 dictionary types (`PebbleDictionaryItem`: Int8/UInt8/Int16/UInt16/Int32/UInt32/Text/Bytes) are converted by `PebbleAppMessageMapper`; outbound uses only `Int32` and `Text`, inbound `Bytes` is dropped. A settings design that packs values into a byte array would therefore depend on changing that mapper and `AppMessage`.

## Internal Cross-Package Dependencies

```mermaid
flowchart TD
  APP[":app"] --> PK[":pebblekit"]
  APP -.assets.-> RULES["rules/catalog + rules/bundled"]
  APPTEST[":app tests"] -.resources.-> FIX["rules/fixtures"]
  APPTEST -.resources.-> RULES
  CAT["rules/catalog/navigation-apps.json"] -.packages must equal.-> BUN["every rule in rules/bundled/appId"]
  CAT -.packages must appear in.-> MAN["AndroidManifest queries"]
  GEN["gen_localized_google_maps.py"] -.writes 7 languages.-> BUN
  DEF["protocol/protocol-definition.json"] -.generate.py.-> KT["Protocol.kt"]
  DEF -.generate.py.-> H["watchapp generated/protocol.h"]
  EX["examples/protocol-definition.json"] -.check_consistency.-> DEF
  VER["VERSION"] -.version.sh.-> GR["app build.gradle.kts"]
  VER -.version.sh.-> PJ["watchapp/package.json"]
  VER -.version.sh.-> MC["watchapp main.c APP_VERSION_STR"]
  WB["tools/rule-workbench"] -.reads.-> RULES
  RC["tools/route-capture"] -.imports workbench.-> WB
```

Text fallback: `:app` depends on `:pebblekit` and stages rule data as assets and as test resources, and fixtures as test resources only. Each catalog entry's package set must equal the `packageNames` of every rule under `rules/bundled/<appId>/` (checked by `NavigationAppCatalogTest`) and must appear in the manifest `<queries>` (checked by `scripts/validate_catalog.py`). The Google Maps generator writes seven of that app's ruleset files. The protocol JSON generates the Kotlin and C constants and must agree with the protected example. `VERSION` drives three version locations. route-capture imports the workbench engine.

### Package-list consistency (checked at 1c7f4c1)

One app's package names are copied into four places: the catalog entry, every rule of the app's bundled rulesets, the manifest `<queries>` and the fixtures. Two checks hold the first three together: `NavigationAppCatalogTest.everyBundledRuleCoversExactlyItsAppsPackages` (JVM, runs with `gradlew test` in CI) and `validate_catalog.py` `check_manifest_queries` (Python, runs through `validate-rules.sh` in `test-all.sh`, the rules CI job and pre-commit). Fixtures are not checked for coverage; the #54 convention is one `package-variant-<package>` fixture per added package. `gen_localized_google_maps.py` is a fifth, hidden copy (its `PACKAGE` constant): running it after #113 without updating it would drop the new package from seven files, which the JVM test then catches.

### Rule families and the icon lookup (checked at 2fa2aa5)

`AppContainer` builds one `RuleFamilies` from the install's catalog view (`forGooglePlay()` on Play) and passes it to `AssetRuleRepository`, `RuleEngine` and `RuleDrivenLargeIconRecognizer`; `RulesViewModel` builds its own from the same catalog. Any change to how families are built (for example recording a root package) reaches all of them, so `familyOf` and `applies` should keep their results. `AppIconReferences` depends only on `Context`; the posting package's `<queries>` entry is what makes its package context and version code visible.

### Install source fan-out (checked at 494a6dd)

`AppContainer.installSource` (one `InstallSources.detect` per process) is read by `EffectiveCatalog` (`googlePlay`), `updatePolicy`, `UpdateCheckRepository`, `MainActivity` (onboarding disclosure variant, dashboard, `openUpdateSource`) and, through `UpdatePolicy`, `updateCardModel`, `updatePrompt` and `DashboardScreen`. A richer install record for #119 can sit beside it without changing these readers; `DiagnosticExporter` reaches it only through a new constructor input wired in `AppContainer`. Two exhaustive `when`s over `InstallSource` (`MainActivity.openUpdateSource`, `DashboardScreen.sourceName`) break on a new enum value, so #119's `manual`/`unknown` belong in the record, not in the enum.

## Version Coupling

The phone and watch share one version (`VERSION`, 0.0.77 at 6dec020, versionCode 77, which a Play upload must also increase; 0.0.76 at 4779760, versionCode 76; 0.0.75 at 3727ddc, where `APP_VERSION_STR` and `watchapp/package.json` both read 0.0.75; 0.0.74 at 8f08e02; versionCode = major*10000 + minor*100 + patch = 74). The release workflow tags `v<version>`, which is the `tag_name` the update check reads. F-Droid metadata (`fdroid/metadata/com.pebblentn.app.yml`) tracks tags (`UpdateCheckMode: Tags`), so F-Droid builds lag the GitHub release (the intent's "about a day" warning). It uses `Binaries:` (the GitHub APK) for reproducible-build verification. The Rebble store watchapp auto-updates on the watch, so after a release the watch is normally ahead of a phone that needs a manual update. That is what makes a watch-higher-than-phone comparison a valid "update available" signal, as long as both versions follow the shared `VERSION` line.

Bundled ruleset versions (`rulesetVersion`, for example `google-maps-en-2026.10.19`) are independent of the app version. The app compares none of them; it only records which rulesets hold a rule for an event's package (#84). The generator keeps per-language dates so a regeneration does not bump the others, and #54 added packages to rulesets without bumping their versions.
