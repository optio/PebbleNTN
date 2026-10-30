# Technology Stack

## Languages

| Language | Where | Notes |
|---|---|---|
| Kotlin 2.3.10 | android/app, android/pebblekit, android/fixture-publisher | JVM target 21, official code style |
| AIDL | android/pebblekit | PebbleKit 2 IPC contracts |
| C (C11 for host tests) | watchapp | Pebble SDK 3; host tests run under `gcc -Wall -Wextra -Werror` |
| Python 3.10+ (CI 3.12) | protocol, scripts, tools, watchapp/tools | stdlib, plus jsonschema and pyqrcode where used |
| Bash | scripts, .githooks | `set -euo pipefail` throughout |
| Java | tools/route-capture/device | small on-device helpers (dex, icon probe) |
| JSON | protocol, rules, schemas, examples | JSON Schema (draft 2020-12) for rulesets and the catalog |

## Platforms and Baselines

- Android: minSdk 31, targetSdk 36 at 494a6dd (35 at 6dec020; #114 raised it with the Play floor gate `verifyReleaseTargetSdk`), compileSdk 36. Google Play has required targetSdk 36 for new apps and updates since 31 August 2026 (#114); AGP 8.13.2 already compiles against 36, so no toolchain change is needed for it. `:fixture-publisher` (debug only) also targets 35.
- Package visibility (Android 11+): the app declares each navigation package in the manifest `<queries>` instead of `QUERY_ALL_PACKAGES`, so a package missing there cannot be discovered, and its drawables cannot be loaded for icon matching.
- Pebble: aplite, basalt, chalk (round), diorite, emery (200x228, touch, RGB backlight). App glance on everything except aplite. flint and gabbro are not build targets and are not mentioned anywhere in the watchapp; the touch calls are guarded by `PBL_PLATFORM_EMERY`, not by a capability macro, and the RGB backlight by `PBL_RGB_BACKLIGHT`. Colour versus black-and-white is `PBL_COLOR` (`PBL_IF_COLOR_ELSE`), round is `PBL_ROUND`, and the large layout is a runtime `w >= 200`. The watchapp never calls `watch_info_*`, so nothing reports the platform at runtime today. Pebble apps load their code into the same RAM as their heap, so on aplite every byte of new code or static data comes off the free heap (about 3.9 KB at load after v0.0.76, per the intent brief).
- Build: Gradle 8.13 wrapper, AGP 8.13.2, KSP 2.3.10, JDK 21 (Temurin in CI); pebble-tool with the latest SDK.

## Frameworks

- Jetpack Compose (BOM 2024.12.01) with Material 3 and Navigation Compose 2.8.5.
- AndroidX Lifecycle 2.8.7 and Activity Compose 1.9.3.
- Room 2.7.2 (KSP) for persistence; SharedPreferences for small settings (`pebblentn_settings`); DataStore Preferences 1.1.1 is on the `:app` classpath but unused by app code.
- kotlinx-coroutines 1.9.0 (Flow/StateFlow, Mutex) and kotlinx-serialization-json 1.7.3 (the catalog and rules are parsed strictly, unknown keys rejected).
- PebbleKit Android 2 1.2.0 (vendored).
- Pebble C SDK: Window/Layer/MenuLayer/ScrollLayer, AppMessage, persist, AppGlance, connection and tick services.

## Tooling

- Android Lint (default rules), CodeQL (java-kotlin).
- Test stack: JUnit 4, Robolectric 4.14.1, coroutines-test, Compose UI test, room-testing, Espresso (instrumented).
- No coverage tool, formatter enforcement, ktlint or detekt.
- Robolectric runs at `sdk=34` (`android/app/src/test/resources/robolectric.properties`), independent of targetSdk, so raising targetSdk to 36 does not need a newer Robolectric; it also means JVM tests never run API 36 behaviour.
- Watch emulator tooling: pebble-tool emulators driven by `watchapp/tools/capture_screenshots.py` (buttons and `send-app-message`) and `watchapp/tools/touch_emulator.py` (touch over VNC, a standard-library RFB client, no third-party dependency).
- No stack change in v0.0.73 or v0.0.74 (watchapp version row), and none implied by making version compare the default update signal: `PackageManager.getInstallSourceInfo` (platform API 30+) is the only new platform call, and the row uses existing Compose Material 3 `ListItem`s.
- Rule data validation: Python `jsonschema` (`Draft202012Validator`) in `scripts/validate_catalog.py` and `scripts/validate_rules.py`.

- Release builds run R8 and resource shrinking (`isMinifyEnabled`, `isShrinkResources`), which matters for bundled preview drawables (#102); unit tests merge Android resources (`isIncludeAndroidResources = true`), so a JVM test can check resources exist.
- No stack change is implied by #102: Compose `Image` with `ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })` is in Compose UI already, and the capture tool needs only pebble-tool and the standard library.
- No stack change is implied by #113: adding a package variant is JSON data, one manifest line and tests, with no new library or platform call.
- No stack change is implied by #119: `InstallSourceInfo` (API 30) and `PackageInfo.firstInstallTime` / `lastUpdateTime` are platform APIs below minSdk 31, and ISO 8601 times come from `java.time.Instant` (already used for `exportedAt`). `getUpdateOwnerPackageName` (API 34) and `getPackageSource` (API 33) would need a version check.
- No stack change is implied by #120 (Vela Maps): a ruleset, fixtures, a catalog entry and a manifest line are JSON and XML data, the regression test is JUnit 4, and the route-capture driver is standard-library Python (`urllib`, `re`, adb through `host`). The rules use only existing operators and extractors (Java regex on the phone, Python `re` in the workbench and route-capture).

Library versions and transitive relationships are in [dependencies.md](dependencies.md).
