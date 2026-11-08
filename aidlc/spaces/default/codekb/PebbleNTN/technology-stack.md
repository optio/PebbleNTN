# Technology Stack

## Languages

| Language | Where | Notes |
|---|---|---|
| Kotlin 2.3.10 | android/app, android/pebblekit, android/fixture-publisher | JVM target 21, official code style |
| AIDL | android/pebblekit | PebbleKit 2 IPC contracts |
| C (C11 for host tests) | watchapp | Pebble SDK 3; host tests run under `gcc -Wall -Wextra -Werror`; `_Static_assert` used to tie tables to enums and protocol keys |
| Python 3.10+ (CI 3.12) | protocol, scripts, tools, watchapp/tools | stdlib, plus jsonschema and pyqrcode where used |
| Bash | scripts, .githooks | `set -euo pipefail` throughout |
| Java | tools/route-capture/device | small on-device helpers (dex, icon probe) |
| JSON | protocol, rules, schemas, examples | JSON Schema (draft 2020-12) for rulesets and the catalog |

## Platforms and Baselines

- Android: minSdk 31, targetSdk 36 (raised by #114 with the Play floor gate `verifyReleaseTargetSdk`), compileSdk 36. Android 16 (API 36) adds `Notification.ProgressStyle`, which Google Maps uses for its trip card; the app reads its documented `EXTRA_PROGRESS` / `EXTRA_PROGRESS_MAX` with plain `Bundle.getInt` (`NotificationSnapshotFactory`), available on every supported API level, so speed estimation from them needs no new platform call or version check.
- Package visibility (Android 11+): each navigation package is declared in the manifest `<queries>` instead of `QUERY_ALL_PACKAGES`.
- Pebble: aplite, basalt, chalk (round), diorite, emery (200x228, touch, RGB backlight). App glance on everything except aplite. Colour is `PBL_COLOR`, round `PBL_ROUND`, RGB backlight `PBL_RGB_BACKLIGHT`, touch `PBL_TOUCH` (reported to the phone as a capability bit since #102) and `PBL_PLATFORM_EMERY` (touch bridge calls). Pebble apps load code into the same RAM as their heap, so on aplite every byte of new code or static data comes off the free heap; REQ-WATCH-025 caps the sync's cost at v0.0.76's free heap minus 500 bytes.
- Build: Gradle 8.13 wrapper, AGP 8.13.2, KSP 2.3.10, JDK 21 (Temurin in CI); pebble-tool with the latest SDK.

## Frameworks

- Jetpack Compose (BOM 2024.12.01) with Material 3 and Navigation Compose 2.8.5.
- AndroidX Lifecycle 2.8.7 and Activity Compose 1.9.3.
- Room 2.7.2 (KSP) for persistence; SharedPreferences for small settings (`pebblentn_settings`, `pebblentn_watch_settings`); DataStore Preferences 1.1.1 is on the `:app` classpath but unused by app code.
- kotlinx-coroutines 1.9.0 (Flow/StateFlow/SharedFlow, Mutex) and kotlinx-serialization-json 1.7.3.
- PebbleKit Android 2 1.2.0 (vendored): `startAppOnTheWatch`, `stopAppOnTheWatch`, `sendDataToPebble`, listener callbacks `onMessageReceived`, `onAppOpened`, `onAppClosed`.
- Pebble C SDK: Window/Layer/MenuLayer/ScrollLayer, AppMessage (inbox and outbox handlers), persist, AppGlance, connection and tick services, vibes and light.

## Tooling

- Android Lint (default rules), CodeQL (java-kotlin).
- Test stack: JUnit 4, Robolectric 4.14.1 (`sdk=34`), coroutines-test (`runTest` with virtual time, used by the controller tests), Compose UI test, room-testing, Espresso (instrumented).
- No coverage tool, formatter enforcement, ktlint or detekt.
- Watch emulator tooling: pebble-tool emulators driven by `watchapp/tools/capture_screenshots.py` and `watchapp/tools/touch_emulator.py`.
- Rule data validation: Python `jsonschema` in `scripts/validate_catalog.py` and `scripts/validate_rules.py`.
- Release builds run R8 and resource shrinking; unit tests merge Android resources (`isIncludeAndroidResources = true`).
- No stack change is implied by #132: the speed estimate and the hide/show decision are plain Kotlin over existing snapshot fields and `EpochClock`; closing the watchapp can use the vendored `stopAppOnTheWatch`; the settings are two more table rows on each side and two protocol keys; the export log is kotlinx-serialization. Earlier intents (#102, #113, #114, #119, #120) likewise added no library.

Library versions and transitive relationships are in [dependencies.md](dependencies.md).
