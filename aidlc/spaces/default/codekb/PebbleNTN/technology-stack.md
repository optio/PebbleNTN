# Technology Stack

## Languages

| Language | Where | Notes |
|---|---|---|
| Kotlin 2.3.10 | `android/app`, `android/pebblekit`, `android/fixture-publisher` | JVM target 21; Compose compiler plugin bundled with Kotlin |
| AIDL | `android/pebblekit/src/main/aidl/` | PebbleKit 2 service interfaces |
| C (C11 for host tests) | `watchapp/src/c/` | Pebble SDK 3; host units built with `gcc -std=c11 -Wall -Wextra -Werror` |
| Python 3.10+ (CI 3.12) | `protocol/`, `scripts/`, `tools/` | Standard library plus `jsonschema`; Pillow optional for store graphics |
| Bash | `scripts/`, `.githooks/` | Build, test, release, hook orchestration |
| JSON | `rules/`, `schemas/`, `protocol/`, `examples/` | Rule data, JSON Schema draft 2020-12 contracts |

## Android Platform

| Item | Value | Source |
|---|---|---|
| minSdk / compileSdk / targetSdk | 31 / 36 / 36 | `android/app/build.gradle.kts` |
| Gradle / AGP / KSP | 8.13 / 8.13.2 / 2.3.10 | wrapper properties, `libs.versions.toml` |
| App id / version | `com.pebblentn.app` / 0.0.82 (code 82) | `build.gradle.kts`, `VERSION` |
| Release build | R8 minify + shrink resources; baseline profile disabled for reproducible F-Droid builds; dependency metadata excluded | `build.gradle.kts` |

## Frameworks and Libraries (Android)

| Library | Version | Purpose |
|---|---|---|
| androidx.core-ktx | 1.15.0 | Android KTX |
| androidx.lifecycle (runtime, viewmodel-compose) | 2.8.7 | View models, lifecycle |
| androidx.activity-compose | 1.9.3 | Compose host |
| Compose BOM (ui, material3, icons-core, tooling) | 2024.12.01 | UI |
| navigation-compose | 2.8.5 | Screen navigation |
| Room (runtime, ktx, compiler via KSP) | 2.7.2 | SQLite persistence (schema v8) |
| DataStore Preferences | 1.1.1 | Key-value settings |
| kotlinx-serialization-json | 1.7.3 | Rules, snapshots, traces, exports |
| kotlinx-coroutines-android | 1.9.0 | Serial queue, controller, flows |
| Timber | 5.0.1 | Logging |
| PebbleKit Android 2 (vendored source) | Rebble `io.rebble.pebblekit2` | Watch transport |
| kermit / androidx.annotation | 2.0.4 / 1.9.1 | PebbleKit 2 dependencies |

Standard-library pieces central to the rule engine: `java.util.regex.Pattern` (with `SafeRegex` time budget), `java.time` (`ZoneId`, `LocalTime` in `DrivingSpeedEstimator`).

## Testing Stack

| Tool | Version | Use |
|---|---|---|
| JUnit | 4.13.2 | All JVM unit tests |
| Robolectric | 4.14.1 | Android-dependent tests (Room, Compose UI) on the JVM; 2 GB heap, single fork |
| kotlinx-coroutines-test | 1.9.0 | Coroutine tests |
| androidx.test core / ext-junit / espresso | 1.6.1 / 1.2.1 / 3.6.1 | Instrumented test |
| Compose ui-test-junit4, Room testing | BOM / 2.7.2 | UI and migration tests |
| Host gcc + C test files | — | Watchapp pure units |
| Python `unittest`, rule-workbench `regression` | — | Tools, catalog validator, Python rule engine |

## Watch Platform

Pebble SDK 3 (`pebble-tool`, installed with `uv` in CI), platforms `aplite`, `basalt`, `chalk`, `diorite`, `emery`; UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`; app glance; message keys from the generated `protocol.h`.

## CI/CD and Distribution

GitHub Actions (checkout v7, setup-java v5 Temurin 21, setup-android v4, setup-python v6, cache v6, upload-artifact v7, CodeQL v4). Releases: GitHub (APK + PBW, every push to main after a verify job), F-Droid (reproducible), Obtainium, Play (manual signed AAB), Rebble store. Workflows and their gates are described in [code-quality-assessment.md](code-quality-assessment.md#cicd).

## Version Policy

`libs.versions.toml` states the policy: newest stable versions compatible with minSdk 31 / compileSdk 36, with each pin's rationale in `docs/IMPLEMENTATION_STATUS.md` (CLAUDE.md requirement).
