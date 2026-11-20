# Technology Stack

## Languages

| Language | Where |
|---|---|
| Kotlin 2.3.10 (JVM 21) | Android app, PebbleKit 2, fixture publisher |
| C (Pebble SDK 3) | Watchapp |
| Python 3.12 | Protocol generator, rule workbench, route capture, validators, store graphics, changelog |
| Bash | Build, test, release scripts, pre-commit |
| JSON / JSON Schema | Rules, catalog, fixtures, protocol definition |
| AIDL | PebbleKit 2 IPC |

## Android Platform

- minSdk 31, compileSdk 36, targetSdk 36 (Play floor checked by `verifyReleaseTargetSdk`)
- Gradle 8.13, Android Gradle Plugin 8.13.2, KSP 2.3.10
- Release: R8 minify + resource shrinking, reproducible APK (no dependency-info blob, no ART profile), AAB with native symbol tables

## Frameworks and Libraries (`android/gradle/libs.versions.toml`)

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.12.01 | UI toolkit (ui, graphics, tooling, material3, material-icons-core) |
| Material 3 (from BOM) | about 1.3.x | Components (`ListItem` with headline, overline, supporting, leading, trailing slots) |
| Navigation Compose | 2.8.5 | Screen routes |
| Activity Compose | 1.9.3 | Host activity |
| Lifecycle (runtime, viewmodel-compose) | 2.8.7 | View models, lifecycle |
| Core KTX | 1.15.0 | Android extensions |
| Room | 2.7.2 | SQLite persistence (schema v8) |
| DataStore Preferences | 1.1.1 | Preferences, watch settings mirror |
| kotlinx.serialization JSON | 1.7.3 | Rules, exports, catalog |
| kotlinx.coroutines | 1.9.0 | Async, Flow |
| Timber | 5.0.1 | Logging |
| PebbleKit Android 2 (Rebble) | vendored source | Watch messaging (deps: kermit 2.0.4, androidx.annotation 1.9.1) |

## Test Stack

| Tool | Version | Use |
|---|---|---|
| JUnit | 4.13.2 | JVM tests |
| Robolectric | 4.14.1 | Android + Compose UI tests on the JVM |
| Compose ui-test-junit4 | BOM | UI assertions |
| kotlinx-coroutines-test | 1.9.0 | Coroutine tests |
| Room testing | 2.7.2 | Migration tests |
| AndroidX test core / ext-junit / Espresso | 1.6.1 / 1.2.1 / 3.6.1 | Instrumented test |
| Python unittest | stdlib | Tool tests |
| Host C test programs | — | Watch logic |

## Tooling and Infrastructure

- GitHub Actions (6 workflows), CodeQL (java-kotlin), Dependabot not present
- Pebble tool + SDK installed with `uv` in CI
- Fastlane metadata layout for Play and F-Droid
- No cloud services; optional GitHub Releases API only

Dependency relationships are in [dependencies.md](dependencies.md).
