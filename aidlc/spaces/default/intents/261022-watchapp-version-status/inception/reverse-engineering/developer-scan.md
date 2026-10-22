# Developer Code Scan: watchapp version in the dashboard status section

Intent: `261022-watchapp-version-status`. Focused scan at Minimal depth, commit c50710e (v0.0.73, the version-compare work is merged).

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt
  - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt
  - android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt
  - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/WatchListenerService.kt
  - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt
  - android/app/src/main/res/values/strings.xml
  - android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt
  - android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt
  - android/app/src/test/java/com/pebblentn/app/protocol/ProtocolCodecTest.kt
  - android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdateCardTest.kt
  - android/app/src/test/java/com/pebblentn/app/ui/apps/NavigationAppsScreenTest.kt
- **Skimmed only**:
  - android/app/src/main/java/com/pebblentn/app/ui/ (other screens, listed by size)
  - android/app/src/main/java/com/pebblentn/app/pebble/ (PebbleWatchTransport.kt, PebbleAppMessageMapper.kt)
  - android/app/src/main/java/com/pebblentn/app/protocol/ (AppMessage.kt, WatchTransport.kt, Protocol.kt)
  - android/app/src/test/java/com/pebblentn/app/ (file list only)
  - watchapp/src/c/, watchapp/tests/ (file list only: version_compare.c/.h and test_version_compare.c exist)
- **Left out**:
  - android/app/build/ (ignored by android/.gitignore)
  - android/.gradle/ (tool cache)
  - .claude/, aidlc/ (AI-DLC install and workspace)

### Packages Found
- `:app` (com.pebblentn.app) — Android application — Kotlin — the companion app; this intent touches `ui/`, `ui/dashboard/`, `update/`, and `res/values/` only.

### Build System
- **Type**: Gradle 8.13 (Kotlin DSL), AGP 8.13.2
- **Config Files**: android/settings.gradle.kts, android/app/build.gradle.kts, android/gradle/libs.versions.toml (not re-read, known from the store)
- **Build Dependencies**: `:app` -> `:pebblekit`

### APIs Discovered
- Internal Kotlin — `UpdateCheckRepository.state: StateFlow<UpdateState>`; `UpdateState.watchAppVersion: String?` (UpdateCheckRepository.kt:32), persisted under prefs `pebblentn_update_check` key `watch_app_version` (line 176), restored at construction (line 85).
- Internal Kotlin — `UpdateCheckRepository.recordWatchAppVersion(version): Boolean` (lines 97-106): trims, ignores blank, writes only on change.
- Internal Kotlin — `ProtocolCodec.watchAppVersion(message): String?` (ProtocolCodec.kt:106-109): key 11 of a `WATCH_READY` only, trimmed, non-blank.
- Internal Kotlin — `NavigationController(onWatchAppVersion: (String) -> Unit)` (NavigationController.kt:61), called in `handleInbound` (lines 103-111) inside `runCatching`.
- Internal Compose — `DashboardScreen(...)` stateless, about 40 defaulted parameters (DashboardScreen.kt:75-121); the Status section is lines 218-255.
- No HTTP, protocol or watch change is needed for this intent.

### Frameworks & Libraries
- Jetpack Compose Material 3 (BOM 2024.12.01) — `ListItem`-based rows
- kotlinx-coroutines 1.9.0 — `StateFlow` collected with `collectAsState` in `MainActivity.AppNavHost`
- JUnit 4.13.2, Robolectric 4.14.1, Compose UI test — tests

### Test Coverage
- **Test Directories**: android/app/src/test/java/com/pebblentn/app/ (JVM + Robolectric)
- **Test Frameworks**: JUnit 4, Robolectric, coroutines-test, Compose UI test (`createComposeRule`)
- **Coverage Config**: absent
- Watch-version path is covered: `UpdateCheckRepositoryTest` lines 142-195 (record, no rewrite, blank ignored, unparsable or older is no update, precedence with GitHub); `ProtocolCodecTest` lines 214-246; `NavigationControllerTest` lines 128-173 (genuine READY reports, synthetic READYs do not).
- No `DashboardScreen` test exists. Dashboard logic is tested through pure helpers next to the screen (`UpdateCardTest` -> `updateCardModel`, `UpdatePromptTest`, `SetupProgressTest`). The one Compose screen test pattern is `NavigationAppsScreenTest` / `RuleReviewScreenTest` (Robolectric, `@Config(qualifiers = "w411dp-h891dp")`, a rule chain that registers `ComponentActivity` with the shadow package manager, `onNodeWithText`).

### Code Quality Indicators
- **Linting**: Android Lint, default config, no baseline
- **CI/CD**: .github/workflows/android.yml (test, lint, assembleDebug), release.yml
- **Documentation**: KDoc on every public symbol with REQ IDs (REQ-ANDROID-019 for the watch version); strings grouped with issue-number comments.

### Technical Debt Signals
- `DashboardScreen.kt` is 568 lines, and `DashboardScreen` takes about 40 parameters; a new row adds one more defaulted parameter.
- `strings.xml` holds two dashboard groups (lines 219-235 "#28" and 362-367 "Leftovers of #28, part 2"); `dashboard_watch*` strings live in the second group.
- No timestamp is stored with `watch_app_version`, so "last seen" can only mean the stored value, not when it was seen.

## Handoff Summary
- **Intent-relevant finding**: the data is already exposed. `MainActivity.AppNavHost` collects `updateCheckRepository.state` as `updateState` (MainActivity.kt:121) and already passes `updateState.updateAvailable` and `updateState.effectiveLatest` to `DashboardScreen` (lines 155-157). Adding `watchAppVersion = updateState.watchAppVersion` there and a defaulted `watchAppVersion: String? = null` parameter to `DashboardScreen` is enough. The Status section (DashboardScreen.kt:218-255) is a master-switch `ToggleRow`, an access chip, then `InfoRow`s "Watch", "Last navigation notification", "Last sent to watch". The new row fits the existing `InfoRow(title, value)` pattern, with the same `?.let { ... } ?: stringResource(..._none)` fallback the "Last navigation notification" row uses (lines 243-248). `UpdateState` is a `StateFlow`, so a newly recorded version recomposes the dashboard live.
- **Risks / follow-up**: (1) The value is shown raw (trimmed, non-blank), including strings the strict parser rejects such as `0.0.72-debug`; that is fine for a diagnostic row and must not be filtered by `parseStrict`. (2) The stored value persists across restarts and is never cleared, so the row shows the last version ever seen, not proof that forwarding works right now; the label must say "last seen". (3) With the Core Devices companion the value stays null (WATCH_READY is not forwarded), which is the very case the row is meant to reveal; the synthetic READYs must keep not recording a version (covered by `NavigationControllerTest`). (4) There is no existing Dashboard UI test; a test needs either a pure helper or the Robolectric Compose pattern of `NavigationAppsScreenTest`. (5) Strings are English only; add the new ones beside `dashboard_watch*`.
