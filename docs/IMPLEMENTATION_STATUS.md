# Implementation Status

_Last updated: 2026-07-12_

## Current milestone

**M0 — Repository and builds** (`spec/800-roadmap/Milestones.md`)

> Create monorepo, licenses, wrappers, scripts, CI skeleton, empty Android app and empty Pebble
> watchapp.

Status: **complete except for compile-verification of the Android build** (see Blockers).

## Requirements in scope for M0

M0 is scaffolding; it establishes the ground for these requirements without yet implementing
their behavior:

- REQ-ANDROID-001 (platform: Kotlin, Compose M3, minSdk 31, release-verified target SDK) —
  Gradle configuration in place; CI (`android.yml`) exercises it. Compile not yet verified locally.
- Build/CI/release structure from `spec/600-security-release/BuildRelease.md`.
- Monorepo layout from `spec/200-architecture/System.md`.

## What exists after M0

```
android/                Gradle project (wrapper 8.11.1), :app module
  gradle/libs.versions.toml   version catalog (see rationale below)
  app/                  empty Compose M3 app (MainActivity placeholder, Application, theme)
watchapp/               Pebble C watchapp (builds to build/watchapp.pbw)
protocol/               placeholder (M1 generates protocol constants)
rules/                  catalog/ bundled/ fixtures/ test-cases/ (M4/M6)
tools/rule-workbench/   placeholder (M8)
scripts/                bootstrap, build-all, test-all, build-watchapp,
                        run-android-emulator, install-android-debug,
                        validate-rules(.sh + .py), validate-spec-assets.sh
.github/workflows/      android, watchapp, rules, release, codeql
docs/                   this file
```

## Dependency version selections (newest stable compatible with the Android baseline)

Policy: CLAUDE.md — pick the newest stable version compatible with the declared baseline
(minSdk 31, compileSdk 36), record it in the version catalog, and note the rationale here.

| Component | Version | Rationale |
|---|---|---|
| Gradle wrapper | 8.11.1 | Minimum Gradle required by AGP 8.9.x; current stable line. |
| Android Gradle Plugin | 8.9.1 | First AGP line that supports `compileSdk = 36` (Android 16). |
| Kotlin | 2.1.0 | Current stable; bundles the Compose compiler plugin (`kotlin.plugin.compose`). |
| KSP | 2.1.0-1.0.29 | Must track the Kotlin version; used by Room. |
| Compose BOM | 2024.12.01 | Latest stable BOM aligning Compose UI/Material3 artifacts. |
| Room | 2.6.1 | Current stable; KSP-compatible. |
| kotlinx.serialization | 1.7.3 | Current stable JSON. |
| kotlinx.coroutines | 1.9.0 | Current stable. |
| JDK | 21 | Per `spec/600-security-release/BuildRelease.md` (LTS baseline). |

If a newer stable set is available at build time and remains baseline-compatible, bump the
catalog and update this table.

## Commands executed this session

```bash
# Watchapp build — PASSED (5 platforms: aplite, basalt, chalk, diorite, emery -> watchapp.pbw)
cd watchapp && pebble build

# Spec asset integrity — PASSED (34 files match MANIFEST.sha256.json)
./scripts/validate-spec-assets.sh

# Ruleset schema validation — PASSED (examples/example-ruleset.json)
./scripts/validate-rules.sh

# YAML + bash syntax checks — PASSED (all workflows, all scripts)
```

## Blockers

1. **No JVM/Android toolchain in the current environment** — `java`, `gradle`, `sdkmanager`,
   `adb`, and `emulator` are absent; `ANDROID_HOME` is unset. Therefore the Android build
   (`./android/gradlew -p android test lint assembleDebug`) **could not be compile-verified
   locally**. The Gradle project, version catalog, manifest, Compose sources and unit test are
   written to standard, buildable conventions and are exercised by `.github/workflows/android.yml`
   (JDK 21 + `android-actions/setup-android`). Per AGENTS.md rule 12, this blocker is recorded
   rather than worked around by fabricating results.
   - To verify locally: install JDK 21 + Android SDK (compileSdk 36), then run
     `./scripts/bootstrap.sh` followed by `./scripts/test-all.sh`.
2. **Pebble SDK build in CI is best-effort** — `watchapp.yml` installs `pebble-tool` and the SDK
   at run time. The local build passes with SDK v4.9.169; pin a maintained SDK image if the
   public distribution changes.

## Next atomic task

Begin **M1 — Domain model and protocol**:

1. Add a `protocol/` code generator that reads `examples/protocol-definition.json` and emits a
   **read-only** Kotlin constants file (into `:app` or a `core-model` module) and a C header for
   the watchapp (AGENTS.md rule 5 — never hand-edit generated protocol files).
2. Define the normalized navigation model (immutable data classes, sealed event/result types)
   with no Android framework types.
3. Implement the `NavigationSessionReducer` (session start/end, dedup, distance quantization,
   launch-once, READY sync, stale detection, stop behavior) with pure JVM unit tests.
4. Provide a fake `WatchTransport` test double.

## Notes / decisions

- Modules kept minimal (`:app` only) per System.md guidance ("do not prematurely create dozens
  of modules"); `core-model` / `rule-engine` / `pebble-transport` extraction deferred until the
  boundaries reduce compile coupling (likely M1/M4).
- `fixture-publisher`, `protocol`, `tools/rule-workbench`, and `rules/*` carry placeholder READMEs
  so the committed layout matches `spec/200-architecture/System.md`; they are wired in their
  owning milestones.
- Watchapp `main.c` deliberately defines **no** protocol key/event constants — those come from the
  generated header in M1.
