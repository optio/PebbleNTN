# Developer Code Scan - PebbleNTN (full rescan)

- Intent: `companion-update-notice` (GitHub issue #94), express scope, Minimal depth, brownfield
- Snapshot paths: `./`; head commit `b5a9e8bedf0c9d57fe0e1808d3b082aa1e7015e3`; scanned 2026-10-22
- Method: every tracked, hand-written file was listed folder by folder (git-tracked set, `.gitignore` honoured). Intent focus areas were read line by line; every other source file was read at least at its declarations and KDoc/header-comment level. No build, test or program was run.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - `./` (whole repository at component level; the paths below were read in full)
  - `protocol/` (protocol-definition.json, generate.py, check_consistency.py, README.md)
  - `examples/protocol-definition.json`
  - `scripts/generate-protocol.sh`, `scripts/test-all.sh`, `scripts/test-watchapp-unit.sh`, `scripts/build-watchapp.sh`, `scripts/version.sh` (header)
  - `watchapp/src/c/main.c`, `glance_text.{h,c}`, `phone_version.{h,c}`, `about_text.{h,c}`, `about_window.{h,c}`, `settings_window.{h,c}` (top-level menu), `theme.{h,c}` (persist keys, API), `stops_text.h`, `panel_layout.h`, `eta_text.h`, `early_warning.h`, `generated/protocol.h`
  - `watchapp/tests/test_glance_text.c`, `watchapp/package.json`, `watchapp/wscript`
  - `android/app/src/main/java/com/pebblentn/app/update/` (all six files)
  - `android/app/src/main/java/com/pebblentn/app/protocol/` (all four files)
  - `android/app/src/main/java/com/pebblentn/app/pebble/` (all four files)
  - `android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt`, `WatchSettings.kt`, `NavigationState.kt`
  - `android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt`, `PebbleNtnApplication.kt`
  - `android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt`, `ui/MainActivity.kt` (dashboard wiring, update actions)
  - `android/app/src/main/res/values/strings.xml` (update strings), `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt`
  - `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `android/settings.gradle.kts`, `android/pebblekit/build.gradle.kts`
  - `spec/200-architecture/Protocol.md`, `spec/200-architecture/System.md`, `spec/400-ui/WatchUI.md`, `spec/700-ai/CodingStandards.md`
  - `requirements/REQ-WATCH.md` (REQ-WATCH-019, -020), `requirements/REQ-ANDROID.md` (REQ-ANDROID-013)
  - `.github/workflows/` (android, watchapp, rules, codeql; release and play-release outline), `.githooks/pre-commit`, `AGENTS.md`, `MANIFEST.sha256.json`, `README.md`
- **Skimmed only** (declarations, KDoc and headers):
  - `android/app/src/main/java/com/pebblentn/app/{catalog,data,data/db,notification,notification/icon,rules,export,system}/`, remaining `core/` and `ui/` screens
  - `android/app/src/test/`, `android/app/src/androidTest/`, `android/app/schemas/room/`
  - `android/pebblekit/` (vendored upstream PebbleKit Android 2 1.2.0, public API only), `android/fixture-publisher/`
  - `watchapp/src/c/` other bodies, `watchapp/tests/` other tests, `watchapp/tools/`, `watchapp/resources/`, `watchapp/store/`
  - `spec/` other files, `requirements/` other files, `docs/`, `rules/`, `schemas/`, `tools/rule-workbench/`, `tools/route-capture/`, `scripts/` others, `fastlane/`, `fdroid/`, `screenshots/`, `.github/ISSUE_TEMPLATE/`
- **Left out**:
  - `android/build/`, `android/app/build/`, `android/.gradle/`, `android/.kotlin/` (build outputs and Gradle/Kotlin caches)
  - `android/local.properties` (ignored, machine-local)
  - `watchapp/build/` (Pebble build output, ignored)
  - `watchapp/tools/__pycache__/`, `scripts/__pycache__/` (Python bytecode)
  - `*:Zone.Identifier` files in `watchapp/store/` (ignored download metadata)
  - AI-DLC install and workspace: `.claude/`, `aidlc/`, `aidlc.settings.json`, `.mcp.json`, `CLAUDE.md` AI-DLC import, AI-DLC block of `.gitignore`

### Packages Found
- `android/app` (`com.pebblentn.app`) - Android application - Kotlin - companion: notification listener, rule engine, reducer, PebbleKit transport, Compose UI, update check
- `android/pebblekit` (`io.rebble.pebblekit2`) - Android library - Kotlin + AIDL - vendored PebbleKit Android 2 1.2.0
- `android/fixture-publisher` (`com.pebblentn.fixturepublisher`) - debug Android app - Kotlin - posts synthetic navigation notifications
- `watchapp/` - Pebble watchapp (SDK 3, waf) - C - renderer for aplite, basalt, chalk, diorite, emery
- `protocol/` - generator - Python 3 - single AppMessage definition to Protocol.kt and protocol.h
- `tools/rule-workbench/` - CLI - Python 3 + jsonschema - rule inspect/validate/regression
- `tools/route-capture/` - CLI harness - Python 3 (stdlib) + small Java device helpers - emulator route playback and issue filing
- `scripts/` - shell + Python - build, test, version, validation, emulator helpers
- `rules/`, `schemas/`, `examples/` - JSON data - catalog, bundled rulesets, fixtures, JSON Schemas

### Build System
- **Type**: Gradle 8.13 (Kotlin DSL, version catalog) for Android; Pebble SDK waf (`pebble build`) for the watchapp; Python scripts for generation and validation
- **Config Files**: `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/app/build.gradle.kts`, `android/pebblekit/build.gradle.kts`, `android/fixture-publisher/build.gradle.kts`, `android/gradle/libs.versions.toml`, `android/gradle/wrapper/gradle-wrapper.properties`, `android/gradle.properties`, `watchapp/package.json`, `watchapp/wscript`, `VERSION`
- **Build Dependencies**: `:app` -> `:pebblekit`; `:app` assets <- `rules/catalog`, `rules/bundled` (Gradle `syncRuleData`); `:app` test resources <- `rules/fixtures` (`syncTestFixtures`); `Protocol.kt` and `protocol.h` <- `protocol/protocol-definition.json` (generate.py, `--check` in CI and pre-commit); `VERSION` -> `build.gradle.kts` versionName/versionCode, `package.json` version, `main.c` `APP_VERSION_STR` (`scripts/version.sh`)

### APIs Discovered
- AppMessage protocol 1.1 - `protocol/protocol-definition.json` - 14 keys (0-13), 6 events, 4 flag bits (0-3), 13 maneuver codes, 2 error codes
- GitHub Releases REST (outbound GET) - `update/ReleaseFetcher.kt` - 1 call (`/repos/optio/PebbleNTN/releases/latest`, reads `tag_name`)
- PebbleKit 2 Android intents/services - `pebble/WatchListenerService.kt`, `AndroidManifest.xml` - 1 exported listener service (`io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH`), sender via `SEND_DATA_TO_WATCH`
- Android NotificationListenerService - `notification/NavigationNotificationListenerService.kt` - 1 service
- FileProvider - `AndroidManifest.xml` - 1 provider for diagnostics sharing
- Pebble App Glance API - `watchapp/src/c/main.c` `glance_reload_callback` - 2 slices (countdown, hint)
- Watch persist storage - keys 1-15 (`theme.c`), key 16 (`main.c`, phone version)

### Frameworks & Libraries
- Kotlin — 2.3.10 — language and Compose compiler plugin
- Android Gradle Plugin — 8.13.2 — Android build
- KSP — 2.3.10 — Room code generation
- Jetpack Compose BOM — 2024.12.01 — UI (Material 3, material-icons-core)
- AndroidX core-ktx 1.15.0, lifecycle 2.8.7, activity-compose 1.9.3, navigation-compose 2.8.5 — app framework
- Room — 2.7.2 — SQLite persistence (schema v8)
- kotlinx-serialization-json — 1.7.3 — rules, state, GitHub JSON
- kotlinx-coroutines — 1.9.0 — concurrency
- DataStore preferences — 1.1.1 — used only inside `:pebblekit`; declared but unused in `:app` code
- Timber — 5.0.1 — logging
- Kermit — 2.0.4, androidx-annotation 1.9.1 — PebbleKit transitive dependencies
- Pebble SDK — 3 (pebble-tool latest in CI) — watchapp
- Test: JUnit 4.13.2, Robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, androidx.test core 1.6.1 / ext-junit 1.2.1, Espresso 3.6.1, Compose ui-test-junit4, room-testing
- Python: jsonschema (validation), pyqrcode (QR generation tool), unittest (route-capture tests)

### Test Coverage
- **Test Directories**: `android/app/src/test/` (66 JVM/Robolectric test-source files, including the `FakeWatchTransport` double), `android/app/src/androidTest/` (1 instrumented test), `watchapp/tests/` (7 host-compiled C tests), `tools/route-capture/tests/`, rule fixtures in `rules/fixtures/` driven by Kotlin regression tests and `workbench.py regression`
- **Test Frameworks**: JUnit 4 + Robolectric + coroutines-test + Compose UI test (Android); plain C assertion harness compiled with `gcc -std=c11 -Wall -Wextra -Werror` (watch); Python unittest (route-capture)
- **Coverage Config**: absent (no JaCoCo or Kover; no C coverage)

### Code Quality Indicators
- **Linting**: Android Lint via `./gradlew lint` (default configuration, no `lint.xml`); no ktlint/detekt; C warnings as errors only for host tests; `kotlin.code.style=official`
- **CI/CD**: `.github/workflows/android.yml`, `watchapp.yml`, `rules.yml`, `codeql.yml`, `release.yml` (push to main: verify, bump, tag `v<version>`, GitHub release with APK, PBW, SHA256SUMS), `play-release.yml` (manual signed AAB); local `.githooks/pre-commit` (secrets, MANIFEST integrity, protocol `--check`, version sync, rules, syntax, Android tests + lint)
- **Documentation**: good. README, AGENTS.md contract, numbered `spec/`, `requirements/` with IDs and acceptance criteria, a 2,483-line `docs/IMPLEMENTATION_STATUS.md` log, KDoc on most public Kotlin APIs, and explanatory header comments on every C module

### Technical Debt Signals
- `watchapp/src/c/main.c` is 1,124 lines mixing AppMessage decoding, rendering, backlight, watchdog and glance; `settings_window.c` is 741 lines (god-file risk; CodingStandards asks to keep the renderer independent from decoding)
- `render_navigation` in `main.c` reads `->value->int32` and `->value->cstring` without checking tuple type (only `record_phone_version` validates `TUPLE_CSTRING`), against CodingStandards "Validate every tuple and type"
- `NavigationSessionReducer.sendCurrent` sends `NoActiveNavigation` with `flags = 0` hard-coded and `encodeCompatibilityError` sends no flags or app version: any new phone-wide flag has to be added outside the per-state flag helpers
- `PebbleNtnDatabase` KDoc lists versions up to v5 while the schema is v8 (stale doc)
- `datastore-preferences` declared in `:app` but no app source uses it (only `:pebblekit` does)
- `scripts/__pycache__/changelog.cpython-312.pyc` is committed although `.gitignore` excludes `__pycache__/`
- `rules/README.md` and `spec/200-architecture/System.md` name `rules/test-cases/`, which holds no tracked file
- `play-release.yml` line 38 swallows a `verifyReleaseTargetSdk` failure with `|| echo "TODO(M10) ..."`
- Committed binary `tools/route-capture/device/routecap-locale.dex`
- No coverage tooling, so coverage floors cannot be measured
- Watchapp host C tests (`scripts/test-watchapp-unit.sh`) run only from `scripts/test-all.sh`, in no CI workflow, so the new watch units for #94 are only gated locally

## Handoff Summary
- **Intent-relevant finding**:
  1. Protocol extension is constrained by spec integrity: `protocol/check_consistency.py` requires `protocolMajor/Minor`, `keys` and `events` to be identical to `examples/protocol-definition.json`, which `MANIFEST.sha256.json` protects (as it does `spec/200-architecture/Protocol.md`, `spec/400-ui/WatchUI.md`, `requirements/REQ-WATCH.md`, `requirements/REQ-ANDROID.md`). Adding `LATEST_PHONE_VERSION` (next free key 14) therefore means editing `protocol/protocol-definition.json` and `examples/protocol-definition.json` together, then re-hashing the manifest. A new flag bit (bit 4, `PHONE_UPDATE_AVAILABLE`) only needs `protocol/protocol-definition.json` (`flagBits` is outside the consistency check; Protocol.md says remaining bits are reserved). Then `scripts/generate-protocol.sh` regenerates `Protocol.kt` and `protocol.h`, which must never be hand-edited (AGENTS.md rule 5).
  2. Update state and the watch path are not connected today. `UpdateCheckRepository` (`update/UpdateCheckRepository.kt`) exposes `state: StateFlow<UpdateState>` with `latestVersion` (the raw GitHub `tag_name`, for example `"v0.0.70"`, see `UpdateCheckRepositoryTest.forcedCheckDetectsNewerVersion`) and `updateAvailable`. It persists `last_check_millis` in SharedPreferences `pebblentn_update_check`, but does not expose it (0 means never). `NavigationController` (`pebble/NavigationController.kt`) only receives `appVersion`. Flags are computed in the pure reducer (`flagsForNavigating`, `flagsForStopped`, and a hard-coded `flags = 0` for NoActiveNavigation at `NavigationSessionReducer.kt` line 267). `AppContainer` wires settings into the controller through `watchSettingsRepository.settings.collect { onSettingsChanged }`, the model to copy for update state.
  3. The auto check is opt-in and off by default (`DEFAULT_AUTO_CHECK = false`, REQ-ANDROID-013), so most phones never learn the latest version. That is why the stale-check dashboard entry (more than 30 days or never) and the "check weekly?" suggestion matter. The dashboard (`ui/dashboard/DashboardScreen.kt`) has no ViewModel: `MainActivity.AppNavHost` collects the flows and passes parameters. Attention cards (`PromptCard`) sit above `Section`s, after `SetupChecklist` and `FeedbackCard`. The update prompt is the first `PromptCard`.
  4. Watch side: `inbox_received_handler` in `main.c` (line 928) records `APP_VERSION` (key 11) for every event and persists it under key 16. The pattern to reuse for a persisted latest-version string is `phone_version.{h,c}` plus `PERSIST_KEY_*`; the next free persist key is 17. The glance is written only in `deinit` (`glance_reload_callback`, main.c lines 1084-1097, not on aplite). Its hint is `GLANCE_HINT "Navigate on phone"` in `glance_text.h`, whose comment says "About 18 characters fit". The full-screen message layer (`MSG_PLAIN`/`MSG_CONNECTING`/`MSG_QR`) and the black-on-white `about_window` are the existing notice and window patterns. The settings menu rows are `#define ROW_*` with `MAIN_ROW_COUNT 10`.
- **Risks / follow-up**:
  - "Update Companion App to version X" (about 38 characters) is far longer than the documented 18-character glance width. Design must choose wording and truncation, and keep the countdown slice's priority during a route.
  - The GitHub tag carries a leading `v`. Strip it, or send it as-is, before putting it on the wire and the watch (`PHONE_VERSION_MAX` 16-byte cap pattern).
  - Watchapp and phone app share one version (`scripts/version.sh`). F-Droid builds lag GitHub releases, so a GitHub-based "update available" may advertise a version an F-Droid user cannot install yet.
  - "Non-intrusive" must not overwrite navigation, connection-hint or QR states. `s_heard_from_phone`/watchdog ordering in `inbox_received_handler` must be preserved.
  - New requirement IDs would be REQ-WATCH-023 and REQ-ANDROID-017, and protected spec edits need MANIFEST re-hash in the same commit. REQ-WATCH-020 explicitly states "no new protocol key", so the new key belongs to a new requirement, not that one.
  - New watch text needs host unit tests (`watchapp/tests/test_<unit>.c` paired 1:1 with `src/c/<unit>.c` by `test-watchapp-unit.sh`). New Android strings must be resources (AGENTS.md rule 16), and only `values/` (English) exists.
