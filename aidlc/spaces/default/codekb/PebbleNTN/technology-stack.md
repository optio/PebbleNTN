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
| JSON | protocol, rules, schemas, examples | JSON Schema for rulesets and the catalog |

## Platforms and Baselines

- Android: minSdk 31, targetSdk 35 (Play floor gate `verifyReleaseTargetSdk` = 35), compileSdk 36.
- Pebble: aplite, basalt, chalk (round), diorite, emery (200x228, touch, RGB backlight). App glance on everything except aplite.
- Build: Gradle 8.13 wrapper, AGP 8.13.2, KSP 2.3.10, JDK 21 (Temurin in CI); pebble-tool with the latest SDK.

## Frameworks

- Jetpack Compose (BOM 2024.12.01) with Material 3 and Navigation Compose 2.8.5.
- AndroidX Lifecycle 2.8.7 and Activity Compose 1.9.3.
- Room 2.7.2 (KSP) for persistence; SharedPreferences for small settings.
- kotlinx-coroutines 1.9.0 (Flow/StateFlow, Mutex) and kotlinx-serialization-json 1.7.3.
- PebbleKit Android 2 1.2.0 (vendored).
- Pebble C SDK: Window/Layer/MenuLayer/ScrollLayer, AppMessage, persist, AppGlance, connection and tick services.

## Tooling

- Android Lint (default rules), CodeQL (java-kotlin).
- Test stack: JUnit 4, Robolectric 4.14.1, coroutines-test, Compose UI test, room-testing, Espresso (instrumented).
- No coverage tool, formatter enforcement, ktlint or detekt.
- No stack change in v0.0.73 or v0.0.74 (watchapp version row), and none implied by making version compare the default update signal: `PackageManager.getInstallSourceInfo` (platform API 30+) is the only new platform call, and the row uses existing Compose Material 3 `ListItem`s.

Library versions and transitive relationships are in [dependencies.md](dependencies.md).
