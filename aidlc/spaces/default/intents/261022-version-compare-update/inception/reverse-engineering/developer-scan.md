# Developer Code Scan: 261022-version-compare-update (focused)

Snapshot head: `1ad9ec8f64ec3e2552f46134ee7dabd51df9e7a7`. Repo: PebbleNTN (unrecorded project-root repo). Depth: Minimal, scope: express, brownfield. Read-only scan: no builds, tests or programs were run.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - watchapp/src/c/main.c
  - watchapp/src/c/phone_version.c
  - watchapp/src/c/phone_version.h
  - watchapp/src/c/update_notice.c
  - watchapp/src/c/update_notice.h
  - watchapp/src/c/glance_text.h
  - watchapp/src/c/about_text.h
  - watchapp/tests/ (structure, and test_phone_version.c / test_update_notice.c)
  - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt
  - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt
  - android/app/src/main/java/com/pebblentn/app/update/ReleaseFetcher.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/WatchListenerService.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/PebbleAppMessageMapper.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/AppMessage.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt
  - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt (events, WatchReady handling, sendCurrent)
  - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt (dashboard wiring, openUrl, checkForUpdate)
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt (parameters, update card, PromptCard)
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
  - android/app/src/main/res/values/strings.xml (update strings and URLs)
  - android/app/src/main/AndroidManifest.xml
  - android/app/src/test/java/com/pebblentn/app/update/AppVersionsTest.kt
  - protocol/protocol-definition.json
- **Skimmed only**:
  - watchapp/src/c/ (remaining units: about_window, settings_window, update_notice_window, theme, eta_text, stops_text, early_warning, panel_layout, glance_text.c)
  - watchapp/tests/ (other test files: names and per-file structure only)
  - android/app/src/main/java/com/pebblentn/app/ui/ (rules, debug, share, apps, onboarding, theme, format: not opened beyond file list)
  - android/app/src/main/java/com/pebblentn/app/system/, core/ (other than the reducer), di/ (AssetCatalogSource, InstalledAppsProvider)
  - android/app/src/test/java/com/pebblentn/app/ (file list; grep of update/protocol/controller tests)
  - protocol/README.md, protocol/generate.py, protocol/check_consistency.py (grep only)
  - Outside the snapshot, grep/heading level only, for distribution facts the brief asked for: README.md (Distribution), fdroid/README.md, fdroid/metadata/com.pebblentn.app.yml, scripts/version.sh, scripts/test-watchapp-unit.sh, watchapp/wscript, watchapp/package.json, android/app/build.gradle.kts (SDK levels), .github/workflows/release.yml (trigger), android/pebblekit/ (grep for version APIs)
- **Left out**:
  - watchapp/build/
  - android/build/, android/app/build/, android/.gradle/
  - android/local.properties (gitignored)
  - watchapp/tools/__pycache__/, scripts/__pycache__/
  - AI-DLC install and workspace (.claude/, aidlc/, AI-DLC sections of AGENTS.md/.gitignore)

### Packages Found
- watchapp — Pebble C watchapp (SDK 3, waf) — C11 — renders navigation state; holds `APP_VERSION_STR`, records phone version (key 11) and the #94 update notice state.
- android/app (`com.pebblentn.app`) — Android app (Gradle Kotlin DSL, Compose) — Kotlin — notification reader, watch link, GitHub update check, dashboard.
- android/pebblekit — vendored PebbleKit Android 2 (Apache-2.0) — Kotlin — watch transport (not deep-scanned).
- protocol — protocol definition plus generator — JSON / Python — single source for `Protocol.kt` and `watchapp/src/c/generated/protocol.h` (generated, never hand-edited).

### Build System
- **Type**: Gradle (Android, AGP; `compileSdk 36`, `minSdk 31`, `targetSdk 35`, `versionName "0.0.71"`, `versionCode 71`) and Pebble SDK waf (`watchapp/wscript`, compiles `src/c/**/*.c`). Host C tests: `scripts/test-watchapp-unit.sh` (gcc `-std=c11 -Wall -Wextra -Werror`).
- **Config Files**: android/app/build.gradle.kts, android/settings.gradle.kts, android/gradle/ (version catalog), watchapp/wscript, watchapp/package.json, protocol/protocol-definition.json, VERSION + scripts/version.sh.
- **Build Dependencies**: protocol/protocol-definition.json → (scripts/generate-protocol.sh) → android `protocol/Protocol.kt` and watchapp `src/c/generated/protocol.h`; VERSION → (scripts/version.sh) → build.gradle.kts versionName/Code, watchapp/package.json version, main.c `APP_VERSION_STR`; android/app → android/pebblekit.

### APIs Discovered
- AppMessage protocol 1.2 (phone ↔ watch) — protocol/protocol-definition.json — 15 keys, 6 events, 5 flag bits. Relevant: key 11 `APP_VERSION`, key 14 `LATEST_PHONE_VERSION`, flag bit 4 `PHONE_UPDATE_AVAILABLE`, event 10 `WATCH_READY`, event 11 `WATCH_REQUEST_STATE`.
- GitHub Releases REST (outbound GET) — update/ReleaseFetcher.kt:56 `https://api.github.com/repos/optio/PebbleNTN/releases/latest` — 1 call, reads `tag_name`.
- External links opened by `Intent.ACTION_VIEW` — strings.xml:58 `update_releases_url` (`https://github.com/optio/PebbleNTN/releases/latest`), :313 `watchapp_store_url` (Rebble store).
- PebbleKit 2 listener — WatchListenerService (`onMessageReceived`, `onAppOpened`).

### Frameworks & Libraries
- Jetpack Compose + Material 3 — BOM via catalog — UI (DashboardScreen, PromptCard).
- kotlinx.coroutines / Flow — catalog — StateFlow-based repositories, controller mutex.
- kotlinx.serialization JSON — catalog — parses GitHub response.
- PebbleKit Android 2 (vendored) — source — `DefaultPebbleSender`, `DefaultPebbleInfoRetriever` (exposes watch firmware version only, not watchapp version: android/pebblekit/.../model/ConnectedWatch.kt:10-13).
- Timber — catalog — logging.
- Pebble SDK 3 — watch.

### Test Coverage
- **Test Directories**: android/app/src/test/java/com/pebblentn/app/ (JVM + Robolectric), watchapp/tests/ (host C, one `main` per file).
- **Test Frameworks**: JUnit 4, kotlinx-coroutines-test, Robolectric, Compose ui-test-junit4; plain C asserts with a `failures` counter.
- **Relevant existing tests**: update/AppVersionsTest.kt, update/UpdateCheckRepositoryTest.kt, ui/dashboard/UpdatePromptTest.kt, protocol/ProtocolCodecTest.kt (bit 4 / key 14 at :73-141), pebble/NavigationControllerTest.kt (`latestPhoneUpdate` at :111-119, manual WATCH_READY at :155-235), core/NavigationSessionReducerTest.kt, watchapp/tests/test_update_notice.c, test_phone_version.c, test_glance_text.c.
- **Coverage Config**: not seen in the focused paths.

### Code Quality Indicators
- **Linting**: none found in app/build.gradle.kts by grep (no detekt/ktlint); C built with `-Wall -Wextra -Werror` for host tests.
- **CI/CD**: .github/workflows/android.yml, watchapp.yml, rules.yml, codeql.yml, release.yml (push to `main` builds and publishes `pebble-ntn.apk` + `pebble-ntn.pbw` to a GitHub Release), play-release.yml.
- **Documentation**: good. Every unit carries requirement tags (REQ-ANDROID-013/017/018, REQ-WATCH-020/023, #94, #103) and states its rules; pure logic is split from Pebble/Android APIs for host testing.

### Technical Debt Signals
- `AppVersions.parts` maps any non-numeric component to 0 and drops pre-release suffixes (update/AppVersions.kt:26-35). "garbage" becomes `[0]`, so a junk version is never ignored. It only compares lower than a real version. The intent's "ignore unparsable versions" needs a strict parse, or a guard, before this is used on a version the watch reports (see risks).
- No version comparison exists in C on the watch. `phone_version.c` and `update_notice.c` each keep their own capped string-copy (update_notice.h:58-60 says so on purpose, because each host test links exactly one unit: scripts/test-watchapp-unit.sh:19-24).
- The phone ignores the watch's key 11. `ProtocolCodec.decodeInbound` reads only PROTOCOL_MAJOR/MINOR from WATCH_READY (protocol/ProtocolCodec.kt:85-98). `ReducerEvent.WatchReady` has no version field (core/NavigationSessionReducer.kt:21-25).
- No install-source or installer-package detection anywhere in the focused paths (grep for installer/getInstallSourceInfo/fdroid/obtainium/vending found nothing). The update card has a single hard-coded GitHub URL.
- The update body text is GitHub-specific ("download the latest APK from GitHub ... uninstall first", strings.xml:40).

## Handoff Summary
- **Intent-relevant finding**:
  - **Watch learns the phone version: yes, already.** Every phone message carries key 11 `APP_VERSION` = `BuildConfig.VERSION_NAME` (ProtocolCodec.kt:40; AppContainer.kt:157). The watch records it into `s_phone_version` and persists it (key 16, written only on change) (main.c:1012-1026, 1051; phone_version.c). Its own version is `APP_VERSION_STR "0.0.71"` (main.c:38). scripts/version.sh keeps that in lockstep with versionName and package.json (one VERSION line for both apps), so a straight comparison of the two is meaningful.
  - **#94 watch update flow:** `inbox_received_handler` → `record_update` (main.c:951-964) reads FLAGS bit 4 and key 14 into `s_latest_phone_version` (persist key 17). `update_record` **clears the recorded version whenever bit 4 is clear** (update_notice.c:110-117). The notice runs `update_notice_action` once per version (shown version in persist key 18), deferred during a route or when another window is on top (update_notice.c:138-150; main.c:969-980, called on NO_ACTIVE_NAVIGATION :1061-1066 and on stop :991). The idle text uses `update_idle_text(s_latest_phone_version)`. The launcher subtitle uses `glance_update_subtitle(s_latest_phone_version)` → "Update: <version>" (main.c:1213-1217). A watch-side "watch > phone" signal has to feed these same three consumers. It also has to survive `record_update`'s clear-on-bit-4-off, for example as a separate derived "effective latest" that is not overwritten by each message.
  - **#94 phone update flow:** `UpdateCheckRepository.state` (`UpdateState.updateAvailable` = `AppVersions.isNewer(current, latestVersion)` from the GitHub check, persisted in prefs `pebblentn_update_check`) (UpdateCheckRepository.kt:21-29). AppContainer passes `latestPhoneUpdate` = latestVersion when updateAvailable (AppContainer.kt:162-164). NavigationController reads it on every SendState (NavigationController.kt:55, 129). `ProtocolCodec.encodeState` sets bit 4 and key 14 without the leading `v` (ProtocolCodec.kt:29-41). The dashboard card shows when `updateAvailable` (MainActivity.kt:155-157 → DashboardScreen.kt:179-186), using `dashboard_update_title`/`_body`/`_download` ("Download latest version"). The action is `openUrl(update_releases_url)` (GitHub releases/latest, MainActivity.kt:157, 403-406). `PromptCard` supports one primary and one optional `TextButton` secondary action (DashboardScreen.kt:437-480), which a "pick another source" choice could use.
  - **Phone learns the watch version: not today.** The watch sends key 11 only inside WATCH_READY, once at `init()` (main.c:997-1007, 1197). The phone drops it (ProtocolCodec.kt:85-98).
  - **Distribution channels in code and docs:** GitHub Releases (release.yml, `update_releases_url`). F-Droid (`https://f-droid.org/en/packages/com.pebblentn.app/` in README.md:42; fdroid metadata uses `Binaries:` pointing at the GitHub APK with `UpdateCheckMode: Tags`, so F-Droid lags a tag by its build cycle). Obtainium (README.md:43, tracks GitHub releases). Rebble store for the watchapp only (strings.xml:313). Play Store is out of scope (README.md:45).
- **Risks / follow-up**:
  - **Phone side may never receive the watch's version.** On the Core Devices Pebble app, watch-originated AppMessages, WATCH_READY included, are not forwarded to PebbleKit listeners (NavigationController.kt:138-148). The phone then synthesizes WATCH_READY itself (:149-154), and `onAppOpened` emits a synthetic WATCH_READY without key 11 (WatchListenerService.kt:56-80). PebbleKit 2 exposes only watch firmware version, not watchapp version. So "phone shows the card when the watch reports a higher version" works only where watch messages actually arrive. The design must treat the watch's version as optional, best-effort and possibly persisted, and the requirement should say so. The watch-side half does not have this problem: the phone's key 11 reaches the watch on every message.
  - **Unparsable versions:** `AppVersions` treats junk as 0 (see debt). A reported watch version of "garbage" must be rejected explicitly, not compared. The brief's example 0.0.9 < 0.0.10 already holds (numeric per part). The C side needs a new pure, host-testable unit, `test_<unit>.c` paired with `src/c/<unit>.c`, because each host test links only its own unit.
  - **Merging two update sources on the phone:** `UpdateState.latestVersion` is the GitHub value, used by the card body, by `latestPhoneUpdate`, and by persistence. If the watch-reported version also drives bit 4/key 14, the phone could echo the watch's own version back to it. That is harmless but needs a defined precedence: take the max of GitHub-latest and watch-reported, ignoring unparsable values. The card's `%1$s` version should be the same chosen value.
  - **Install source detection is greenfield:** minSdk 31, so `PackageManager.getInstallSourceInfo(packageName)` (API 30+) is available with no fallback, and no `<queries>` entry is needed for the app's own package (manifest has only navigation-app queries). Known F-Droid client installer ids are not in code. The architect must choose the mapping, for example `org.fdroid.fdroid`, `org.fdroid.basic`, plus third-party clients, and Obtainium `dev.imranr.obtainium`, with an unknown/browser fallback to GitHub. Strings will be needed: "Update from {source}", the F-Droid "about a day" warning, and source-picker labels, with any store URLs as `translatable="false"` like the existing ones.
  - **Signing across sources:** the current body text warns "uninstall first" for GitHub installs (strings.xml:40). F-Droid may or may not ship the developer signature, depending on whether reproducible builds are enabled (fdroid/REPRODUCIBLE_BUILDS.md). Letting the user switch source may therefore hit a signature mismatch, so the copy should keep that warning per source.
  - **Protocol:** no new key is needed for the watch side, since key 11 exists both ways. Any protocol change must go through protocol-definition.json plus the generator, never the generated files.
