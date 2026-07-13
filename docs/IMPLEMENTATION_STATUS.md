# Implementation Status

_Last updated: 2026-07-12_

## Current milestone

**All roadmap milestones (M0–M11) implemented.** Remaining work is hardware/human: on-device
verification, non-English rule captures, and Play submission — tracked in the checklists.

**M11 — Remote official rules** (`spec/800-roadmap/Milestones.md`) — **complete; feature OFF by
default (post-v1, requires separate approval + embedded keys)**.

> Signed HTTPS updates, compatibility checks, self-tests, atomic activation and rollback.

- **M0 — Repository and builds:** complete and verified.
- **M1 — Domain model and protocol:** complete and verified (48 tests).
- **M2 — Notification access and early filtering:** complete and verified (72 tests).
- **M11 — Remote official rules:** complete (feature off by default).
  - `SignedRuleset` envelope + `RulesetSignatureVerifier` (Ed25519, key rotation).
  - `official_ruleset` table (schema v5 + migration + test); `RuleUpdateRepository` gates activation
    behind signature → schema → embedded self-tests, activates atomically, retains last-known-good
    and supports rollback; feeds the engine's *downloaded* layer.
  - Off by default (`enabled = false`, no embedded keys, no auto-checks) per the overview.
  - **185 JVM unit tests, 0 failures**; `assembleDebug test lint` green; all validators pass.
  - Blocker: actual HTTPS fetch + on-API-31/32 Ed25519 provider are deferred until the feature is
    approved for release (recorded).
- **M10 — Release readiness:** complete.
  - Release signing from env (no secrets committed); `verifyReleaseTargetSdk` gate; `bundleRelease`
    verified locally with R8 (AAB produced, no minification breakage).
  - `docs/PRIVACY_POLICY.md` (template), `docs/PLAY_DATA_SAFETY.md` (Data Safety answers),
    `docs/RELEASE_CHECKLIST.md` (automated gates + physical-device matrix + beta checklist).
  - **Human/hardware steps** (device matrix, store submission, App Signing) are the release
    checklist's manual section — they cannot run in this environment.
- **M9 — Hardening:** complete and verified.
  - Process restoration: `navigation_state` (schema v4 + migration + test) +
    `NavigationStateRepository`; controller persists/restores the single current state.
  - Connection loss + stale state already handled by the reducer/controller (ConnectionLost,
    FreshnessChecked; staleness computed at send time — no polling, REQ-ANDROID-007).
  - Regex limits + per-run time budget (`SafeRegex`); bounded rules-per-package (500) in the engine.
  - Migration tests through v4; `docs/PRIVACY_REVIEW.md` maps every privacy boundary to its
    enforcement + test.
  - Fixed a latent bug: unit tests bootstrapped the real Application; now use a stub Application.
  - **173 JVM unit tests, 0 failures**; `assembleDebug test lint` green.
- **M8 — Export and maintainer workflow:** complete and verified.
  - `ExportBuilder` (rules-only / privacy-safe / full) + `Redactor`; `DiagnosticExporter` +
    `DiagnosticShareManager` (FileProvider + Sharesheet, temp URIs, never auto-sends); export UI
    with the exact REQ-DEBUG-007 privacy warning for full exports.
  - `tools/rule-workbench/workbench.py` CLI: inspect / validate / diff / scaffold / sanitize /
    promote / test / regression; the Python subset engine agrees with the Kotlin engine on all
    Google Maps fixtures (9/9). Wired into `test-all.sh` and `rules.yml`.
  - **169 JVM unit tests, 0 failures**; workbench regression green; `assembleDebug test lint` green.
- **M7 — Pebble integration:** complete.
  - Watch renderer (`watchapp/src/c/main.c`) + 12 generated maneuver bitmaps; READY handshake;
    NAVIGATION_UPDATE / STOPPED (exit-to-watchface) / NO_ACTIVE_NAVIGATION / COMPATIBILITY_ERROR
    states; vibrate-on-change + backlight + stale marker. `pebble build` green (5 platforms).
  - `PebbleWatchTransport` (real PebbleKit 4.0.1 impl of `WatchTransport`) + `PebbleAppMessageMapper`.
  - `NavigationController`: serializes reducer access, runs effects (launch-once, send current
    state with bounded-backoff retry, compatibility error), decodes inbound READY / REQUEST_STATE.
  - Full pipeline wired: notification → snapshot → rule engine → matched instruction →
    `NavigationController` → transport → watch.
  - **157 JVM unit tests, 0 failures**; `assembleDebug test lint` green; watch build green.
  - **Blocker (recorded):** `PebbleWatchTransport` and the end-to-end phone↔watch flow are verified
    by compilation only — no Pebble/emulator here; on-device testing is a release gate.
- **M6 — Google Maps initial rules:** complete for English.
  - `rules/bundled/google-maps.json` (12 rules: arrive, roundabout, u-turn, sharp/slight L+R,
    keep L+R, turn L/R, continue) — schema-valid, packaged as an app asset, active at runtime.
  - Synthetic fixtures (`rules/fixtures/google-maps.json`, provenance in the fixtures README) +
    `GoogleMapsRulesRegressionTest` runs every fixture through the engine.
  - **152 JVM unit tests, 0 failures**; `assembleDebug test lint` green; validators pass.
  - **Gap (documented):** authored from documented notification patterns, not real captures; only
    English is implemented — nl/fr/de and capture-verification remain, per the roadmap's
    "evidence supports" rule.
- **M5 — Editors and preview:** complete and verified.
  - `user_rule` table (schema v3 + migration + test); `UserRuleRepository` (CRUD, clone-to-user,
    volatile snapshot as the engine's user layer).
  - `RuleValidator` (structural + semantic, collected errors); `RulePreviewService` (rerun a
    captured event against current or candidate rules with full trace).
  - Rules screen (official / user tabs, clone, enable/disable, delete) + expert JSON editor
    (validate / canonical-format / preview-against-last-capture / save-only-if-valid);
    `RulesViewModel`. Navigation from the dashboard.
  - **150 JVM unit tests, 0 failures**; `assembleDebug test lint` green; validators pass.
- **M4 — Rule engine:** complete and verified.
  - Ruleset model + strict/canonical `RulesetCodec`; condition operators + `SafeRegex`
    (bounds + time budget); extractors + distance/duration parsers; `RuleEngine` (package
    select, enabled/locale filter, priority+layer order, short-circuit, trace).
  - Wired into `DebugCaptureProcessor`: eligible notification → snapshot → engine → debug event
    records `matchedRuleId`, extraction and trace, disposition MATCHED / CAPTURED_UNMATCHED.
    `AssetRuleRepository` loads bundled rulesets from assets (empty until M6).
  - **125 JVM unit tests, 0 failures**; `assembleDebug test lint` green; validators pass.
- **M3 — Debug capture:** complete and verified.
  - `NotificationSnapshot` (selected fields only) + `NotificationSnapshotFactory`.
  - `notification_debug_event` table (schema v2 + `MIGRATION_1_2`, migration test);
    `DebugHistoryRepository` with hashed key/tag and transactional retention (default 500;
    50/100/500/1000/unlimited).
  - Reworked processor seam: `DebugCaptureProcessor` stores eligible posts; the content builder is
    only invoked after the allowlist passes.
  - Debug history list/detail/delete UI + Navigation Compose; `DebugHistoryViewModel`.
  - `fixture-publisher` debug app posting synthetic navigation notifications (catalog entry inert
    unless installed).
  - **85 JVM unit tests, 0 failures**; both app modules assemble; `test lint` green; validators pass.

Next: on-device verification, non-English Google Maps captures, and Play submission (hardware/human steps in RELEASE_CHECKLIST.md).

### Known local limitation
Instrumented tests (`connectedDebugAndroidTest`) and manual device runs (incl. the
fixture-publisher end-to-end flow) need an emulator/device, unavailable here. The REQ-ANDROID-003
acceptance is covered locally at the unit level by `NotificationDispatcherTest` (content builder /
processor never invoked for a disabled package); the full instrumented spy runs in CI.

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
2. **No Pebble watch** — the phone↔watch link cannot be exercised locally. An emulator is now
   available (see "Post-M11" below) and runs the app, but it has no Bluetooth stack, so PebbleKit
   cannot reach a watch there. Instrumented tests (`connectedDebugAndroidTest`) remain unwritten.

## Next atomic task

Begin **M6 — Google Maps initial rules** (spec/500-testing/RuleMaintainerWorkflow.md, REQ-RULES):

1. Capture/derive representative Google Maps notification shapes (English first; then nl/fr/de as
   evidence supports); create sanitized fixtures with expected extraction results under
   `rules/fixtures/` and `rules/test-cases/`.
2. Author the bundled Google Maps ruleset in `rules/bundled/` (maneuvers, distance, road text)
   using the M4 operators/extractors; validate against the schema and the engine.
3. Rule regression: a JVM test (and `validate-rules.sh` extension) that runs each fixture through
   the engine and checks the expected normalized output.
4. Document coverage and known gaps.

Note: real device captures are unavailable in this environment; fixtures will be authored to match
documented Google Maps notification structure and clearly marked as synthetic where not
capture-derived (AGENTS.md — do not fabricate; record provenance).

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

## Post-M11: first emulator run (2026-07-13)

Installed an Android emulator locally (`emulator` + `system-images;android-34;google_apis;x86_64`,
AVD `pebblentn-api34`) and ran the debug APK on it for the first time. This closed blocker 2 for
manual UI runs (instrumented tests still need `connectedDebugAndroidTest` to be written) and
immediately surfaced one real defect:

**PebbleKit 4.0.1 crashes the app on Android 14+.** `PebbleKit.registerReceivedDataHandler` calls
`Context.registerReceiver` without `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED`, which API 34 makes a
fatal `SecurityException` for non-system broadcasts. It was thrown on a `Dispatchers.Default` worker
inside `PebbleWatchTransport.inbound`, so the process died at startup ("PebbleNTN keeps stopping").
Fixes:

1. `PebbleWatchTransport` registers the `PebbleDataReceiver` itself via
   `ContextCompat.registerReceiver(..., RECEIVER_EXPORTED)` on `Constants.INTENT_APP_RECEIVE`.
   Exported is required (the broadcast comes from the Pebble app); `PebbleDataReceiver` filters by
   our UUID, so foreign broadcasts cannot inject watch messages.
2. `NavigationController.start` now `catch`es the inbound stream: a transport failure degrades the
   watch link instead of killing the process. Regression test:
   `NavigationControllerTest.failingInboundStreamDoesNotKillTheController`.

Verified on the emulator: no crash, onboarding renders, process stays alive. The watch link itself
remains unverifiable there (no Bluetooth stack) — still a hardware step.
