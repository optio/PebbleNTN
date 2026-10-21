# Technology Stack

## Languages

| Language | Where | Notes |
|---|---|---|
| Kotlin 2.3.10 | android/app, android/pebblekit, android/fixture-publisher | JVM target 21, `kotlin.code.style=official` |
| AIDL | android/pebblekit/src/main/aidl | PebbleKit 2 IPC |
| C (C11 for host tests) | watchapp/src/c, watchapp/tests | Pebble SDK 3; host tests built with `gcc -std=c11 -Wall -Wextra -Werror` |
| Python 3 (3.12 in CI) | protocol/, scripts/, tools/, watchapp/tools/ | mostly standard library; `jsonschema` for rule validation |
| Bash | scripts/, .githooks/ | `set -euo pipefail` |
| Java | tools/route-capture/device/ | small on-device helpers |
| JSON | protocol, rules, schemas, routes | data contracts |

## Platforms

| Platform | Baseline |
|---|---|
| Android | minSdk 31, targetSdk 35 (Play floor check `verifyReleaseTargetSdk` = 35), compileSdk 36 |
| Pebble | SDK 3; aplite, basalt, chalk (round), diorite, emery (Pebble Time 2, touch, RGB backlight) |

## Android Frameworks and Libraries

The version catalog android/gradle/libs.versions.toml is the single source of versions; full list with roles in [dependencies.md](dependencies.md). Main pieces: Jetpack Compose (BOM 2024.12.01, Material 3), Navigation Compose 2.8.5, Lifecycle 2.8.7, Room 2.7.2 with KSP 2.3.10, DataStore 1.1.1, kotlinx-serialization 1.7.3, kotlinx-coroutines 1.9.0, Timber 5.0.1, PebbleKit Android 2 (vendored).

## Build Tooling

| Tool | Version / config |
|---|---|
| Gradle wrapper | 8.13 (checksum-pinned) |
| Android Gradle Plugin | 8.13.2 |
| KSP | 2.3.10 |
| Pebble build | `pebble build` (waf, watchapp/wscript); `pebble-tool` + `pebble sdk install latest` in CI |
| R8 | release minify + resource shrink; ART baseline profile disabled for reproducible builds |

## Test Tooling

JUnit 4.13.2, Robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, androidx.test core 1.6.1 / ext-junit 1.2.1, Espresso 3.6.1, Room testing, Compose UI test (JUnit4); host C test harness; Python `unittest`.

## Distribution

GitHub Releases (APK + PBW + SHA256SUMS), F-Droid (fdroid/metadata, reproducible builds), Obtainium, Rebble app store (PBW), optional Play AAB (play-release.yml); fastlane/metadata for store listings.
