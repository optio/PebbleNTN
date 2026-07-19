# Implementation Status

_Last updated: 2026-07-19_

## Emulator screenshot capture (2026-07-19)

**Milestone:** none — tooling/documentation task, no product code changed.

New `watchapp/tools/capture_screenshots.py` drives the Pebble emulator to produce the reference
screenshot set in `screenshots/` (see `screenshots/README.md` for the index). Per platform it wipes
the emulator to known setting defaults, installs the `.pbw`, injects a navigation update over
AppMessage (`pebble send-app-message`), then walks the on-watch settings menu with `pebble
emu-button` presses to reach each appearance configuration before grabbing `pebble screenshot`.
Setting state is tracked in the script so the button path to each configuration is computed rather
than hardcoded; the emulator clock is pinned to 12:35 per grab so the shots are reproducible.

**Commands executed:**

```bash
./scripts/build-watchapp.sh
python3 watchapp/tools/capture_screenshots.py basalt chalk emery
```

**Output:** 60 PNGs — 20 configurations × basalt (144×168), chalk (180×180), emery (200×228).
15 navigation-screen configurations (accent colours, invert, arrow/distance swap, glyph packs,
imperial units) and 5 settings screens. Colour correction left enabled, so the images match what a
real display shows rather than the raw `GColor` values.

**Defect surfaced and fixed: chalk (round) layout clipping (REQ-WATCH-013, extended).**
The capture showed the rectangular layout being applied nearly as-is on chalk — the distance text
and maneuver arrow clipped at the top corners, the road name cut off at the left edge, the settings
header running past the bezel. REQ-WATCH-013 adapted by width (`>= 200`) only, so chalk fell into
the 144-wide branch despite being round.

- **`round_inset(top, bottom, margin)`** (new, `main.c`, `PBL_ROUND` only) returns the horizontal
  inset that keeps every row of a band inside the circle: rows are chords, so the narrowest row in
  the band is whichever end is further from the vertical centre. Insets are derived from the
  geometry rather than hardcoded, so they stay correct if the layout bands move.
- **Panel** now places the arrow/distance band at the *bottom* of the panel on round (where the
  display is widest) instead of centring it, and insets it to the circle. Panel height on chalk
  78 → 92 to make room. The glyph is additionally clamped inside the content area, since a glyph
  wider than its third-of-the-width column would otherwise spill past the inset.
- **Strip and road name** inset to their own bands; the road name is centred and its box capped to
  one line on round, so `fit_font` picks a size that stays out of the bottom cap of the circle.
- **Settings headers** are centre-drawn on round (`draw_menu_header`); `menu_cell_basic_header_draw`
  is left-aligned and ran into the bezel. Note that `menu_layer_set_center_focused` is *already* the
  SDK default on `PBL_ROUND`, so no call is needed — the row layout was never the problem.
- Non-round platforms take the `#else` branches and are byte-identical: verified by re-capturing
  basalt and emery after the change and diffing the PNGs against the pre-change set.

**Remaining known defect (not fixed, no requirement filed):** glyph-pack menu previews overlap their
labels in `settings_window.c` (`pack_row`) — the preview bitmap is drawn over the "Classic" / "Bold"
/ "Outline" text rather than beside it. Affects all platforms.

## Watchapp UI refinement + spec reconciliation (2026-07-16)

On-wrist follow-up after the Core Devices bring-up, driven by user feedback on a Pebble Time 2.

**Watchapp** (`watchapp/src/c/main.c`, `theme.c`, `settings_window.c`, `tools/gen_maneuver_bitmaps.py`):
- **Screen-size adaptation (REQ-WATCH-013, new).** Runtime detection (`bounds.size.w >= 200`): emery
  (Pebble Time 2, 200×228) gets larger status-strip fonts, a larger 64px maneuver arrow bundled as an
  emery-only resource (`resources/images/emery/`, wired via disjoint `targetPlatforms` in
  `package.json`), and a panel sized to hug the arrow (80px) instead of a fixed 104px that wasted
  ~20px above and below. The 144-wide models keep the compact layout; both are auto-selected.
- **Arrow-corner setting (REQ-WATCH-011, extended).** New on-watch setting: which top corner holds
  the arrow (top-right default), distance moves to the opposite corner. Persisted
  (`PERSIST_KEY_ARROW_LEFT`); menu row in `settings_window.c`.
- **Status-strip typography.** Clock and ETA at one size with an "ETA" label, vertically centred via
  per-font top-trim; distance number + stacked unit centred on the arrow line.
- Verified on the emery/basalt/diorite emulators; the panel-margin alignment measured pixel-exact
  (arrow centre = panel centre). Emulator shut down after each run.

**Android:** dashboard **version footer** (`BuildConfig.VERSION_NAME`) shipped in the same push.

**Battery-safety review (no code change).** Traced every BLE-touching path: the pipeline is fully
event-driven — no timer/alarm/WorkManager/wakelock/foreground service, the app holds no `BLUETOOTH`
permission (all radio goes through the companion), `FreshnessChecked`/`onConnectionLost` are dead
code (never dispatched). When no nav app is navigating, PebbleNTN initiates zero watch traffic. The
only amplification is the REQ-WATCH-006 relaunch during *active* navigation (below).

**Spec-driven reconciliation (this doc's companion commit).** Folded the above back into the contract:
- REQ-WATCH-011 now lists the arrow-corner setting and its acceptance includes emery; new
  REQ-WATCH-013 covers screen-size adaptation.
- **REQ-WATCH-006 amended** to reconcile a real conflict: the transport's relaunch-on-
  `FailedDifferentAppOpen` (added during bring-up) *does* re-launch the watchapp after the user
  leaves it, which the old "launch once, never steal focus" wording forbade. The companion refuses
  delivery unless our app is foreground, so the re-launch is the only way an update reaches the
  watch; the requirement and `Pebble.md` → Launch policy now carve out that update-driven re-launch
  (bounded to material state changes, never speculative/idle) instead of leaving the conflict
  implicit in code.
- `Pebble.md` gained the companion-limitation / synthesized-readiness and `companionApp`
  authorization notes; `WatchUI.md` now describes the panel/strip/arrow-corner layout and the
  large-screen adaptation.
- Updated the three protected files' hashes in `MANIFEST.sha256.json`.

> **Known stale artifact:** `docs/PebbleNTN-Consolidated-Spec.md` is a generated snapshot dated
> 2026-07-12 and already predates REQ-WATCH-011/012, REQ-DEBUG-010, REQ-RULE-014/015 and
> REQ-ANDROID-011. There is no regenerator script in the repo. It was left as the dated snapshot it
> declares itself to be rather than hand-patched; it should be regenerated from the source specs in a
> dedicated pass.

## On-device bring-up on the Core Devices Pebble app (2026-07-15)

End-to-end phone→watch delivery verified live on a Samsung SM-S916B + Pebble (Core Devices
`coredevices.coreapp` companion, PebbleKit Android 2). Three root causes found and fixed; the watch
now shows live maneuvers (`send ok -> Success`).

1. **Companion never forwards the watch's `WATCH_READY`** to third-party PebbleKit listeners (its own
   protocol runner logged our inbound packet, but our listener service was never called). Fix:
   `NavigationController` synthesizes readiness after launch (`scheduleAutonomousReady`), and
   `WatchListenerService.onAppOpened` (a lifecycle callback the companion *does* forward) triggers a
   state re-send.
2. **`send()` reported success even when nothing was transmitted.** Fix: `PebbleWatchTransport.send`
   inspects the per-watch `TransmissionResult`; empty/failed ⇒ `FAILED` (+ logging). Added
   `linkDiagnostics()` startup line (selected companion, connected watches).
3. **Root cause of the stuck "Connecting": `FailedDifferentAppOpen` on every send** — the watchapp
   was missing the `companionApp` declaration in `watchapp/package.json`, so the companion had no
   registered association between our UUID and `com.pebblentn.app` and refused to route messages to
   it (persisted through relaunch/library-install). Fix: added `companionApp.android.apps = [{package:
   com.pebblentn.app}]`; the watchapp must be reinstalled through the Core Devices app so the new
   metadata registers. Diagnosed against the working `konsumer/pebble-map-android` reference.
   `send()` also relaunches + waits ~900 ms + retries once on `FailedDifferentAppOpen` to bridge each
   session's foreground-transition window.

Also added a **Version footer** to the dashboard (`BuildConfig.VERSION_NAME`). 185+ JVM tests green.

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
  - `rules/bundled/google-maps/en.json` (12 rules: arrive, roundabout, u-turn, sharp/slight L+R,
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

## M12 — watch appearance + on-watch settings (2026-07-14)

Driven by user feedback on the first on-wrist run and by the Rebble reference screenshots.

**Layout** (`watchapp/src/c/main.c`): the maneuver glyph and the distance now sit side by side in a
coloured panel (glyph = 1/3 of the width), a status strip carries the watch clock and the ETA, and
the road name gets the whole lower half. Both the distance and the road name pick the largest font
that fits (`fit_font`) instead of truncating — "12.3 km" and long street names both fit.

Two rendering bugs found and fixed on the emulator, neither of which any test would have caught:
- `graphics_draw_bitmap_in_rect` **crops** (and tiles); it does not scale. A 64px glyph drawn into a
  48px column was silently cut in half. Glyphs are now generated at 48px and drawn at their natural
  size, centred in the column.
- `gcolor_legible_over()` returns *black* over mid-brightness colours like Islamic green, which made
  the arrow black-on-green. Replaced with a perceived-luminance test.

**Themes** (`theme.c`): 16 accent colours + an invert flag, covering both target watches — Pebble
Time 2 (64 colours) and Pebble 2 Duo (black and white, where the accent list collapses to
Black/White so both polarities and their inverses stay reachable). Panel, strip and road area each
carry their own background/foreground. The glyph is recoloured at runtime by rewriting the 1-bit
palette (`memoryFormat: 1BitPalette` on every platform) — one asset serves every theme, and the
same path works on black-and-white watches.

**On-watch settings** (`settings_window.c`): SELECT opens a menu — accent colour, invert, distance
units (metric/imperial). Persisted; no phone round-trip.

**ETA** is now extracted by the bundled rules (`secondaryText` ← Google Maps' "Arrive 23:51"
subText) and rendered in the strip. The protocol already carried SECONDARY_TEXT; nothing filled it.

Verified on the basalt and diorite emulators (screenshots): all three navigation states, the
settings menu, and the black-and-white path.

**Not verified**: chalk (round) and emery (large) layouts, and the settings menu driven by a real
button press — the emulator screenshots were taken with the menu pushed programmatically.

## CI: CodeQL + Node 24 action bump (2026-07-14)

The CodeQL workflow failed on every run with *"Code scanning is not enabled for this repository"*.
The analysis ran fine (81/120 Kotlin files); only the SARIF upload failed, because the repo was
**private** and uploading to code scanning needs GitHub Advanced Security there.

**Resolved by making the repository public** (owner's decision): code scanning is free for public
repositories, so `codeql.yml` uploads natively again (`security-events: write`, no SARIF-artifact
workaround, no local error-level gate). The short-lived workaround — `upload: never` + artifact +
jq gate — is gone; see git history if it is ever needed again.

**Node 20 deprecation:** every action in every workflow was bumped to the newest major that runs on
Node 24, verified by reading each action's `action.yml` (`runs.using`) rather than assuming:

| Action | Was | Now |
|---|---|---|
| `actions/checkout` | v4 (node20) | **v7** |
| `actions/setup-java` | v4 (node20) | **v5** |
| `actions/setup-python` | v5 (node20) | **v6** |
| `actions/upload-artifact` | v4 (node20) | **v7** (note: v5 is still node20) |
| `actions/cache` | v4 (node20) | **v6** |
| `android-actions/setup-android` | v3 (node20) | **v4** |
| `github/codeql-action` | v3 (node20) | **v4** |

Release notes for the skipped majors were checked for breaking changes that affect us: none do
(checkout v7 blocks fork checkout for `pull_request_target`, which we do not use; upload-artifact v7
adds an opt-in `archive` input). All inputs in use (`name`, `path`, `key`) are unchanged. The new
majors require Actions Runner ≥ 2.327.1, which GitHub-hosted runners satisfy.

**Unverified:** these workflows cannot run locally — the bump is verified by manifest inspection and
YAML parsing only. The first CI run after this push is the real check.

## Post-push CI fixes + pre-commit hook (2026-07-14)

The first CI run after the push failed on `rules / Validate spec asset integrity`. Two real problems
came out of it, plus a third found while fixing them.

**1. Spec-asset manifest not updated.** `requirements/REQ-ANDROID.md`, `requirements/REQ-WATCH.md`
and `schemas/ruleset.schema.json` are integrity-protected by `MANIFEST.sha256.json`; adding
REQ-ANDROID-011, REQ-WATCH-011 and the rule `comment` property changed them without updating their
hashes. The mismatch is the guard working as designed. All three changes are pure additions (no
existing spec text altered); the three hashes were updated deliberately, not by blanket-regenerating
the manifest.

**2. `DistanceParser` invented distances from the ETA.** The parser treated a unit-less number as
metres and took the *first* number in the text. Rules extract distance from `combinedText`, which
includes Google Maps' `subText` — "Arrive 23:41" — so **every step with no real distance reported
"23 m" on the watch**. The unit is now mandatory: a bare number in a navigation notification is far
more often the ETA hour, a road number ("A12") or an exit number than a distance. No distance beats a
wrong one. This is the same failure mode as the ARRIVE bug and came from the same capture; it was
only caught because the rule-workbench regression started asserting the full extraction result.

**3. The Python rule-workbench had drifted from the Kotlin engine.** It ignored `expected.matched`
(so a deliberate no-match fixture read as a failure), lacked the `regexCapture` extractor, and never
emitted `primaryText`/`secondaryText`. It now mirrors `GoogleMapsRulesRegressionTest`, so both
engines are held to the same contract — which is the entire point of keeping a second implementation.
14/14 fixtures agree.

**Pre-commit hook** (`.githooks/pre-commit`, `./scripts/install-git-hooks.sh`): secret scan →
spec-asset integrity → generated-protocol check → rule schema + regression → shell/YAML/Python/JSON
syntax → Android unit tests + lint (only when `android/` is touched). Cheapest first, so the common
failures report in under a second. Verified against four deliberate breakages: an unsynced manifest
(the failure above), a staged keystore/`local.properties`, a signing password in a diff, and a rule
change that breaks the fixtures. `--no-verify` and `PEBBLENTN_SKIP_GRADLE=1` are the escape hatches.

## Release automation (2026-07-14)

**Single source of truth for the version:** `VERSION` at the repo root. It was duplicated in three
files that could silently disagree (`build.gradle.kts`, `watchapp/package.json`,
`main.c APP_VERSION_STR`) — and the phone and watch exchange their versions in the READY handshake,
so drift is not cosmetic. `scripts/version.sh` bumps/sets/verifies all of them; `--check` runs in the
pre-commit hook, `test-all.sh` and the release workflow's verify job. `versionCode` is derived from
the semver (`major*10000 + minor*100 + patch`), so it is monotonic without separate bookkeeping.

**`release.yml` (rewritten):** every push to `main` → verify (same checks as CI; nothing is published
from a broken commit) → bump patch → changelog from the commits since the last tag, grouped by
Conventional Commit type (`scripts/changelog.py`) → tag → GitHub release with `pebble-ntn.apk`,
`pebble-ntn.pbw` and `SHA256SUMS.txt`. `workflow_dispatch` allows a minor/major bump. No loop: the
bump commit is pushed with `GITHUB_TOKEN`, and GitHub does not start workflows for those pushes.

**`play-release.yml`:** the old tag-triggered signed-AAB job, now manual (`workflow_dispatch` with a
tag input). A Play upload should be a deliberate act, not a side effect of pushing to main.

**Signing caveat (deliberate, and stated in every release note):** with no keystore secrets set, the
published APK is **debug-signed**. It installs, but it is not a Play-grade artifact. Setting
`UPLOAD_KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` switches it to a signed
release build automatically.

**Unverified:** the release workflow cannot be run locally. The version script, the changelog
generator and the YAML are verified here; the first release run is the real check.

## Comparison-driven changes (2026-07-14) — konsumer/pebble-map-android review

A review against `konsumer/pebble-map-android` (same concept: scrape nav notifications → Pebble)
produced these changes. New behaviour is recorded as requirements (see `requirements/`).

- **PebbleKit Android 2 migration (REQ-WATCH-003/004 transport).** Replaced the abandoned
  `com.getpebble:pebblekit:4.0.1` with Rebble's maintained `io.rebble.pebblekit2:client:1.2.0`.
  Outbound now uses `DefaultPebbleSender` (coroutine API); inbound arrives on a manifest-declared
  `WatchListenerService` (`BasePebbleListenerService`) and is bridged to `PebbleWatchTransport.inbound`
  via `WatchInboundBus`. This **retires the classic-PebbleKit Android 14 `registerReceiver` crash
  workaround** documented above — the maintained library registers correctly, so the manual
  `RECEIVER_EXPORTED` receiver is gone. `PebbleAppMessageMapper` now maps to
  `PebbleDictionaryItem.Int32/Text` and reads any integer width back to `Int`.
  **Unverified here:** no Android SDK / device in this environment — compilation and the phone↔watch
  handshake are gated by CI (`android.yml`) and on-device testing. Version pinned to konsumer's
  `1.2.0`; confirm the newest stable on the first green CI run.
- **Glyph packs (REQ-WATCH-012)** and **per-element debug breakdown (REQ-DEBUG-010)**,
  **per-app/language rules (REQ-RULE-015)** and **expanded distance units (REQ-RULE-014)** — see
  those requirements and the commits.
- **Icon forwarding deliberately NOT adopted.** konsumer forwards the notification's own arrow
  bitmap (via RemoteViews inflation). We keep **REQ-SEC-003** (minimal snapshot, no icon extraction)
  intact and instead ship built-in glyph packs; rationale in `README.md`.
