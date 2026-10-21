# Developer Code Scan - PebbleNTN

Repo: unrecorded project-root repo (`/home/sacha/Workspace/PebbleNTN`). Breadth: full rescan. Depth: Minimal. Commit: `f8366a4b34a77c504df0194a9f31386553a74b8c`. Intent: `261020-about-companion-version` (GitHub #103 - the watchapp About screen shows the last connected companion app version).

AI-DLC's own install (`.claude/`, `aidlc/`, `.mcp.json`, `aidlc.settings.json`, the `AI-DLC:gitignore` block) was not scanned. Nothing was built, tested or run.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - watchapp/src/c/main.c
  - watchapp/src/c/about_text.c
  - watchapp/src/c/about_text.h
  - watchapp/src/c/about_window.c
  - watchapp/src/c/about_window.h
  - watchapp/src/c/settings_window.h
  - watchapp/src/c/generated/
  - watchapp/tests/test_about_text.c
  - watchapp/package.json
  - watchapp/wscript
  - protocol/
  - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt
  - spec/000-overview.md
  - spec/200-architecture/Protocol.md
  - spec/200-architecture/Pebble.md
  - spec/200-architecture/System.md
  - spec/400-ui/WatchUI.md
  - requirements/REQ-WATCH.md
  - scripts/test-all.sh
  - scripts/test-watchapp-unit.sh
  - scripts/generate-protocol.sh
  - scripts/build-watchapp.sh
  - .github/workflows/watchapp.yml
  - .github/workflows/android.yml
  - .github/workflows/rules.yml
  - android/gradle/libs.versions.toml
  - android/app/build.gradle.kts
  - AGENTS.md
  - MANIFEST.sha256.json
- **Skimmed only**:
  - watchapp/src/c/ (settings_window.c main menu section and theme.c persistence keys read; eta_text, glance_text, stops_text, panel_layout, early_warning headers only)
  - watchapp/tests/ (other test files not opened)
  - watchapp/tools/ (module docstrings)
  - watchapp/resources/, watchapp/store/ (listed; image assets)
  - android/app/src/ (all main-source files classified by their first doc line; NavigationSessionReducer.kt WATCH_READY path, WatchListenerService.kt, AndroidManifest.xml read in part)
  - android/pebblekit/, android/fixture-publisher/ (listed, sizes)
  - android/app/schemas/ (Room schema versions 1-8 listed)
  - spec/ (remaining files listed only)
  - requirements/ (other REQ files listed only)
  - scripts/ (other scripts by header; version.sh sync targets grepped)
  - .github/workflows/release.yml, play-release.yml, codeql.yml (step names)
  - .githooks/pre-commit (header and step list)
  - rules/, schemas/, examples/ (listed; rules/README.md read)
  - tools/rule-workbench/, tools/route-capture/ (listed; route-capture README read)
  - docs/, fastlane/, fdroid/, screenshots/ (listed)
  - README.md
- **Left out**:
  - watchapp/build/ (Pebble build output, gitignored)
  - android/.gradle/, android/**/build/ (Gradle caches and build output, gitignored)
  - scripts/__pycache__/ (Python bytecode; tracked despite `.gitignore`, see debt signals)

### Packages Found
- android/app (`com.pebblentn.app`) - Android application - Kotlin - production companion: notification listener, rule engine, reducer, PebbleKit transport, Compose UI
- android/pebblekit (`io.rebble.pebblekit2`) - Android library - Kotlin + AIDL - vendored Rebble PebbleKit Android 2 (Apache-2.0)
- android/fixture-publisher (`com.pebblentn.fixturepublisher`) - Android application (debug tool) - Kotlin - posts synthetic navigation notifications
- watchapp (`pebblentn`, UUID `9c7cef54-517e-42c8-8e95-0a8316e70cf1`) - Pebble watchapp - C (SDK 3) - turn-by-turn renderer for aplite, basalt, chalk, diorite, emery
- protocol - code generator - Python - JSON protocol definition to `Protocol.kt` and `protocol.h`
- tools/rule-workbench - CLI - Python - rule regression and localized ruleset generation
- tools/route-capture (`routecap`) - CLI harness - Python (+ small Java on-device helpers) - emulator route playback and missed-card reporting
- scripts - shell + Python - build, test, validation, versioning, changelog

### Build System
- **Type**: Gradle 8.13 (Kotlin DSL, version catalog) for Android; Pebble SDK waf (`pebble build`) for the watchapp; plain Python 3 for generators and validators
- **Config Files**: android/settings.gradle.kts, android/build.gradle.kts, android/app/build.gradle.kts, android/gradle/libs.versions.toml, android/gradle/wrapper/gradle-wrapper.properties, android/gradle.properties, watchapp/package.json, watchapp/wscript, VERSION
- **Build Dependencies**: :app -> :pebblekit; :app build -> `syncRuleData` (stages rules/catalog + rules/bundled as assets and test resources); protocol/generate.py -> android/app/.../protocol/Protocol.kt and watchapp/src/c/generated/protocol.h; scripts/version.sh keeps VERSION, app/build.gradle.kts, watchapp/package.json and `APP_VERSION_STR` in main.c in sync

### APIs Discovered
- AppMessage protocol (phone <-> watch) - protocol/protocol-definition.json - 14 keys, 6 events, 4 flag bits, 13 maneuver codes, 2 error codes; protocol 1.1
- Android component surface - android/app/src/main/AndroidManifest.xml - 1 activity, 2 services (NotificationListenerService, PebbleKit 2 listener), 1 FileProvider, package `<queries>` for 25 navigation apps
- Outbound HTTP - update/ReleaseFetcher.kt - 1 endpoint (GitHub `releases/latest`), plus signed ruleset download (update/)
- Internal Kotlin seam - protocol/WatchTransport.kt - `inbound`, `launchApp`, `send`, `linkDiagnostics`, `watchLink`

### Frameworks & Libraries
- Kotlin - 2.3.10 - language, Compose compiler plugin
- Android Gradle Plugin - 8.13.2 - build (compileSdk 36, minSdk 31, targetSdk 35, JVM 21)
- Jetpack Compose BOM - 2024.12.01 - UI (Material 3)
- Room - 2.7.2 (KSP 2.3.10) - persistence, schema v8 exported
- DataStore Preferences - 1.1.1 - settings
- kotlinx-serialization-json 1.7.3, kotlinx-coroutines 1.9.0 - JSON, concurrency
- Timber 5.0.1 - logging
- PebbleKit Android 2 (vendored source) with kermit 2.0.4, androidx-annotation 1.9.1
- Pebble SDK 3 (C) - watchapp
- Test: JUnit 4.13.2, Robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, Room testing, Compose UI test, Espresso 3.6.1

### Test Coverage
- **Test Directories**: android/app/src/test/ (66 JVM/Robolectric test files), android/app/src/androidTest/ (1 instrumentation test), watchapp/tests/ (6 host-compiled C tests), tools/route-capture/tests/
- **Test Frameworks**: JUnit 4 + Robolectric (Android); a self-written `check()` harness compiled with host `gcc -std=c11 -Wall -Wextra -Werror` (watch); Python `unittest` (route-capture); rule-workbench fixture regression
- **Coverage Config**: absent (no JaCoCo/Kover, no C coverage)

### Code Quality Indicators
- **Linting**: Android Lint via `./gradlew lint` (no `lint.xml`); `kotlin.code.style=official`; no ktlint/detekt, no `.editorconfig`, no `.clang-format`
- **CI/CD**: .github/workflows/android.yml (test, lint, assembleDebug), watchapp.yml (protocol check + pbw build), rules.yml (spec integrity, schema, regression), release.yml (verify + versioned APK/PBW release on push to main), play-release.yml (AAB), codeql.yml (java-kotlin); .githooks/pre-commit mirrors the cheap checks
- **Documentation**: strong - numbered spec/ tree, requirement IDs cited in code comments, AGENTS.md contract, per-folder READMEs; spec files protected by MANIFEST.sha256.json

### Technical Debt Signals
- Watch host unit tests (`scripts/test-watchapp-unit.sh`) run only in `scripts/test-all.sh`; no CI workflow and no pre-commit step runs them (.github/workflows/watchapp.yml, .githooks/pre-commit).
- watchapp/src/c/main.c is 1078 lines and owns rendering, handshake watchdog, backlight, vibration and AppMessage handling; settings_window.c is 741 lines.
- protocol/README.md says the source of truth is `examples/protocol-definition.json` and is still "Placeholder in M0", while protocol-definition.json's `_comment` says it is authoritative and cites a non-existent `scripts/check-protocol.py` (the real check is protocol/check_consistency.py).
- rules/README.md names a `rules/test-cases/` folder that does not exist.
- spec/200-architecture/Pebble.md "UI" layout (upper-half bitmap) predates the panel/strip layout described in spec/400-ui/WatchUI.md.
- scripts/__pycache__/changelog.cpython-312.pyc and tools/route-capture/device/routecap-locale.dex are committed binaries (the first despite `.gitignore`).
- .github/workflows/play-release.yml:38 swallows a failing `verifyReleaseTargetSdk` with `|| echo "TODO(M10) ..."`.
- `about_window.c` formats into a fixed `s_details[80]` buffer (truncates silently by design; covered by a test).

## Handoff Summary
- **Intent-relevant finding**: The phone already sends its version on every state message: `ProtocolCodec.encodeState` writes `Protocol.Keys.APP_VERSION` (key 11) for NAVIGATION_UPDATE, NAVIGATION_STOPPED and NO_ACTIVE_NAVIGATION (android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt:22-27), with `BuildConfig.VERSION_NAME` injected through `NavigationController(appVersion)`. `encodeCompatibilityError` does not carry it. The watch never reads key 11 inbound: `inbox_received_handler` only sets `s_heard_from_phone` and dispatches on EVENT (watchapp/src/c/main.c:885-913). The About screen receives `AboutInfo { app_version, heard_from_phone }` (about_window.h:7-10) from `select_click_handler` (main.c:925-931) via `settings_window_push`, and `about_details()` builds "Version / Protocol / phone status" into `s_details[80]` (about_text.c, about_window.c:65,101-104). So #103 is watch-only: read `PBNTN_KEY_APP_VERSION` in the inbox handler, persist it (next free persist key is 16; keys 1-15 in theme.c:3-17), extend `AboutInfo` and `about_details`, and extend test_about_text.c.
- **Risks / follow-up**: (1) requirements/REQ-WATCH.md is integrity-protected by MANIFEST.sha256.json; amending REQ-WATCH-020 needs the manifest updated in the same commit (pre-commit step 2, scripts/validate-spec-assets.sh). (2) REQ-WATCH-020 says the screen "adds no image resources" and is covered by an acceptance list (basalt, chalk, emery, diorite); the extra line must fit `s_details` (80 bytes; current worst case is about 50) and the round-display layout. (3) Protocol files are generated and must not be hand-edited (AGENTS.md rule 5); no protocol change is needed. (4) A phone older than the key-11 send, or the compatibility-error path, sends no version, so the screen needs a "not yet seen" state. (5) Watch unit tests are not in CI, so the new test is only exercised by `scripts/test-all.sh` locally.
