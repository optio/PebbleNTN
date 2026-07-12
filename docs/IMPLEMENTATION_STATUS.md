# Implementation Status

_Last updated: 2026-07-12_

## Current milestone

**M2 — Notification access and early filtering** (`spec/800-roadmap/Milestones.md`) — **complete**.

> Onboarding, listener declaration, permission status, app catalog and strict package allowlist.

- **M0 — Repository and builds:** complete and verified (Android + watch builds green locally).
- **M1 — Domain model and protocol:** complete and verified (48 tests).
- **M2 — Notification access and early filtering:** complete and verified.
  - Bundled navigation-app catalog (`rules/catalog/navigation-apps.json` + schema) staged into
    app assets and test classpath; pure `NavigationAppCatalog` loader.
  - Room (`PebbleNtnDatabase` v1, schema exported) + `EnabledAppRepository` with a `@Volatile`
    allowlist cache for synchronous `isEnabled()` on the callback thread.
  - `NavigationNotificationListenerService` + pure `NotificationDispatcher` (allowlist before any
    content read) + `SerialProcessingQueue`; manual `AppContainer` DI; `<queries>` for installed
    detection.
  - Onboarding + disclosure UI (exact REQ-SEC-004 text), `AccessStatusChip`, minimal dashboard,
    `OnboardingViewModel`; Robolectric enabled for Room/Android unit tests.
  - **72 JVM unit tests, 0 failures**; `assembleDebug test lint` green; validators pass.

Next: **M3 — Debug capture**.

### Known local limitation (M2)
Instrumented tests (`connectedDebugAndroidTest`) and manual UI runs need an emulator/device, which
is not available here. The REQ-ANDROID-003 acceptance ("instrumented spy proves snapshot factory /
parser / DB are never called for a disabled package") is covered locally at the unit level by
`NotificationDispatcherTest` (spy processor never invoked for disabled packages); the full
instrumented spy should run in CI.

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
# Android build — PASSED (assembleDebug + test + lint) after installing local JDK 21 + SDK 36
source ~/.local/pebblentn-env.sh && ./android/gradlew -p android assembleDebug test lint

# Watchapp build — PASSED (5 platforms: aplite, basalt, chalk, diorite, emery -> watchapp.pbw)
cd watchapp && pebble build

# Spec asset integrity — PASSED (34 files match MANIFEST.sha256.json)
./scripts/validate-spec-assets.sh

# Ruleset schema validation — PASSED (examples/example-ruleset.json)
./scripts/validate-rules.sh

# YAML + bash syntax checks — PASSED (all workflows, all scripts)
```

## Local toolchain

Resolved the earlier "no JVM/Android toolchain" blocker by installing a local toolchain (network
was available):

- Temurin **JDK 21.0.11** at `~/.local/jdk-21.0.11+10`.
- Android SDK at `~/.local/android-sdk` — `platforms;android-36`, `build-tools;36.0.0`,
  `platform-tools`; licenses accepted.
- Env helper: `source ~/.local/pebblentn-env.sh` sets `JAVA_HOME`/`ANDROID_HOME`/`PATH`.
- `android/local.properties` (git-ignored) points Gradle at the SDK.

**M0 Android build is now verified locally:** `./gradlew assembleDebug test lint` → BUILD
SUCCESSFUL. (Fixed one real defect found this way: `BuildConfig.DEBUG` required
`buildFeatures.buildConfig = true` under AGP 8.)

## Blockers

1. **Pebble SDK build in CI is best-effort** — `watchapp.yml` installs `pebble-tool` and the SDK
   at run time. The local build passes with SDK v4.9.169; pin a maintained SDK image if the
   public distribution changes.
2. **No emulator / connected device** — instrumented tests
   (`connectedDebugAndroidTest`) and manual UI runs cannot execute here; JVM unit tests, lint,
   and APK/AAB assembly all run locally.

## Next atomic task

Begin **M3 — Debug capture** (REQ-DEBUG-001/002, REQ-SEC-003, REQ-ANDROID-008,
spec/300-data/Database.md):

1. `NotificationSnapshotFactory` — build the minimal documented snapshot (selected fields only,
   excluding PendingIntents/actions/RemoteViews/bundles) from an eligible `StatusBarNotification`.
2. `notification_debug_event` Room entity/DAO + `DebugHistoryRepository`; hash notification key/tag.
3. Extend the M2 `NotificationProcessor` to snapshot + store eligible events (never for disabled
   packages — REQ-DEBUG-002).
4. Retention (default 500; 50/100/500/1000/unlimited) with transactional cleanup after insert.
5. Debug history list/detail + delete UI; synthetic `fixture-publisher` app.

## Notes / decisions

- Kept everything in `:app` for M1 (pure `core`/`protocol` packages have no Android imports, so a
  future `:core-model` extraction is mechanical). Revisit module split when compile coupling
  warrants it (likely around M4 rule engine).
- Authoritative protocol input lives at `protocol/protocol-definition.json` (not the
  integrity-protected `examples/` copy); `check_consistency.py` prevents drift and lets us define
  flag bits + maneuver codes the example asset does not carry.

- Modules kept minimal (`:app` only) per System.md guidance ("do not prematurely create dozens
  of modules"); `core-model` / `rule-engine` / `pebble-transport` extraction deferred until the
  boundaries reduce compile coupling (likely M1/M4).
- `fixture-publisher`, `protocol`, `tools/rule-workbench`, and `rules/*` carry placeholder READMEs
  so the committed layout matches `spec/200-architecture/System.md`; they are wired in their
  owning milestones.
- Watchapp `main.c` deliberately defines **no** protocol key/event constants — those come from the
  generated header in M1.
