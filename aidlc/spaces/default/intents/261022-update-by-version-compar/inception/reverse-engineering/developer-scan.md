# Developer Code Scan: update-by-version-compar

Focused scan at 8f08e02 (v0.0.74), depth Minimal, scope express. The intent as clarified by the person: comparing versions (watchapp version higher than the companion app version) becomes the **default** way an update is detected. The online GitHub check **stays**, as an optional extra the user runs by hand or turns on as the weekly automatic check. The earlier statement that "Core Devices does not forward watch messages" is wrong (forwarding was confirmed on a real phone with v0.0.74) and must be corrected wherever it is recorded.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt
  - android/app/src/main/java/com/pebblentn/app/update/ReleaseFetcher.kt
  - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRow.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt (update prompt, update card, Status row and Updates section)
  - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt (update state collection, stale/weekly wiring, `checkForUpdate`, `setAutoCheckUpdates`, `openUpdateSource`)
  - android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt
  - android/app/src/main/java/com/pebblentn/app/pebble/WatchListenerService.kt
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt (update repository, controller wiring, startup check)
  - watchapp/src/c/version_compare.c, watchapp/src/c/version_compare.h
  - watchapp/src/c/update_notice.c, watchapp/src/c/update_notice.h
- **Skimmed only**:
  - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt, Protocol.kt (bit 4, keys 11 and 14 by grep)
  - android/app/src/main/AndroidManifest.xml (INTERNET permission and its comment, listener service)
  - android/app/src/main/res/values/strings.xml (update, stale-check, weekly-question, disclosure and watchapp-version strings by grep)
  - watchapp/src/c/main.c (update section, lines 925-1085, and glance; rest by grep)
  - android/app/src/test/java/com/pebblentn/app/ (test names of UpdateCheckRepositoryTest, UpdatePromptTest, ProtocolCodecTest, NavigationControllerTest; counts of AppVersionsTest, UpdateCardTest, WatchAppVersionRowTest, InstallSourceTest)
  - watchapp/tests/ (test_version_compare.c, test_update_notice.c headers)
  - protocol/protocol-definition.json (keys 11, 14, flag bit 4)
  - Outside the snapshot, grep-level only: requirements/REQ-ANDROID.md (REQ-ANDROID-013/018/019), spec/200-architecture/Pebble.md, docs/IMPLEMENTATION_STATUS.md, watchapp/package.json (`companionApp`), android/pebblekit (`companionApp` KDoc)
- **Left out**: android/app/build/, android/.gradle/, watchapp/build/, and the AI-DLC install (.claude/, aidlc/), which "What to Skip" excludes or the brief excludes

### Packages Found
- `com.pebblentn.app.update` — service + adapter — Kotlin — the GitHub check, version comparison, stored watchapp version, install source
- `com.pebblentn.app.ui.dashboard` — Compose view + pure helpers — Kotlin — update card, stale/weekly prompt, watchapp version row
- `com.pebblentn.app.pebble` — adapter — Kotlin — controller (reads `latestPhoneUpdate`, records the watchapp version), PebbleKit listener
- `com.pebblentn.app.di` — configuration — Kotlin — wiring and the startup auto-check
- `watchapp/src/c` — C (Pebble SDK) — `version_compare` and `update_notice` pure units, `main.c` glue

### Build System
- **Type**: Gradle 8.13 (AGP 8.13.2, Kotlin 2.3.10) for Android; pebble-tool (waf) for the watchapp; host C tests through `scripts/test-watchapp-unit.sh`
- **Config Files**: android/app/build.gradle.kts, android/gradle/libs.versions.toml, watchapp/wscript, watchapp/package.json
- **Build Dependencies**: `:app` -> `:pebblekit`; protocol JSON -> generated `Protocol.kt` and `protocol.h`

### APIs Discovered
- Outbound HTTP — `update/ReleaseFetcher.kt` `GitHubReleaseFetcher` — 1 call (`GET api.github.com/repos/optio/PebbleNTN/releases/latest`, reads `tag_name`); the app's only network use
- AppMessage — `ProtocolCodec.encodeState` — phone to watch: key 11 `APP_VERSION` on every state message; flag bit 4 `PHONE_UPDATE_AVAILABLE` + key 14 `LATEST_PHONE_VERSION` while `effectiveLatest` is non-null
- AppMessage — watch `send_ready` — watch to phone: `WATCH_READY` with key 11 (`APP_VERSION_STR`), read by `ProtocolCodec.watchAppVersion`
- Outbound intents — `MainActivity.openUpdateSource` — GitHub releases page, F-Droid page, Obtainium launch intent

### Frameworks & Libraries
- Jetpack Compose Material 3 — BOM 2024.12.01 — dashboard
- kotlinx-coroutines — 1.9.0 — StateFlow state, `lifecycleScope` checks
- kotlinx-serialization-json — 1.7.3 — GitHub JSON parsing
- PebbleKit Android 2 — 1.2.0 (vendored) — `BasePebbleListenerService`
- `java.net.HttpURLConnection` — platform — the GitHub GET
- Pebble C SDK 3 — AppMessage, persist, AppGlance

### Test Coverage
- **Test Directories**: android/app/src/test/java/com/pebblentn/app/{update,ui/dashboard,pebble,protocol}/, watchapp/tests/
- **Test Frameworks**: JUnit 4 + Robolectric + coroutines-test; plain C host tests (`-Werror`)
- **Coverage Config**: absent
- Relevant tests: `UpdateCheckRepositoryTest` (17: throttle, force, failure, auto-check default off, staleness x5, watch version x5, persistence), `UpdatePromptTest` (4: stale item, weekly question, never with weekly on, nothing pending), `AppVersionsTest` (9), `UpdateCardTest` (3), `WatchAppVersionRowTest` (3), `ProtocolCodecTest` (bit 4/key 14 x3, key 11 x3), `NavigationControllerTest` (`everySendReadsTheLatestPhoneUpdate`, `genuineWatchReadyReportsTheWatchappVersion`, `madeUpReadinessNeverReportsAWatchappVersion`), `test_version_compare.c` (87 lines), `test_update_notice.c`. No test pins the stale prompt against an update that version compare found, or the manual-check toast against the watch version.

### Code Quality Indicators
- **Linting**: Android Lint (default config, CI and pre-commit); C warnings only in host tests
- **CI/CD**: .github/workflows/android.yml, watchapp.yml, release.yml (watchapp host tests run only in scripts/test-all.sh)
- **Documentation**: KDoc and C headers cite REQ IDs and issue numbers; several of them state the outdated Core Devices limitation (below)

### Technical Debt Signals
- Outdated "Core Devices does not forward watch messages" statements:
  - `pebble/NavigationController.kt:148-157` (`scheduleAutonomousReady` KDoc: "do not forward them to third-party PebbleKit listeners, so WATCH_READY never reaches us ... confirmed on-device")
  - `pebble/WatchListenerService.kt:50-55` (`onAppOpened` KDoc: "even though it does not forward the watch's WATCH_READY AppMessage")
  - `update/UpdateCheckRepository.kt:91-96` (`recordWatchAppVersion` KDoc: "Best effort: with the Core Devices app WATCH_READY never arrives and this is never called")
  - `android/app/src/test/.../pebble/NavigationControllerTest.kt:50` and `:282` (test comments)
  - Outside the snapshot (grep only): `requirements/REQ-ANDROID.md:78` (REQ-ANDROID-019 "best effort: companion apps that do not forward ... (the Core Devices Pebble app) never deliver WATCH_READY"), `spec/200-architecture/Pebble.md:41-51` ("Companion limitation and synthesized readiness"), `docs/IMPLEMENTATION_STATUS.md:97`, and the codekb `dependencies.md` / `architecture.md`. `docs/IMPLEMENTATION_STATUS.md:2230` records the likely real cause: `watchapp/package.json` lacked `companionApp`, which is now declared (`com.pebblentn.app`).
- The manual-check toast reflects GitHub only (`UpdateCheckRepository.checkForUpdate` returns `UP_TO_DATE` from `isNewer(currentVersion, latest)`; MainActivity.kt:450-459), so "PebbleNTN is up to date." can appear while the card shows an update found by version compare.
- Two comparison rules: tolerant `AppVersions.isNewer` for GitHub tags, strict `isNewerStrict` for the watch version (documented, intentional).
- `DashboardScreen` keeps growing (about 40 parameters).

## Intent Inventory: what each update source does today

### A. Online check (GitHub) — stays, becomes optional

| Piece | Location | Behaviour |
|---|---|---|
| `ReleaseFetcher` / `GitHubReleaseFetcher` | update/ReleaseFetcher.kt | GET `releases/latest`, 8 s timeouts, `tag_name`, null on any failure; the only network call |
| `checkForUpdate(force)` | UpdateCheckRepository.kt:144-169 | throttled to 7 days unless forced; always writes `last_check_millis`; on success writes `latest_version`; outcome from the GitHub tag only |
| `autoCheckEnabled` (`auto_check_enabled`, default **off**) | UpdateCheckRepository.kt:108-120 | gates the startup check in AppContainer.kt:238-242 (`checkForUpdate(force = false)`) |
| `lastCheckMillis` / `isCheckStale` (30 days, or never) | UpdateCheckRepository.kt:122-137 | drives the #94 stale prompt |
| `UpdatePrompt` `STALE_CHECK` / `WEEKLY_QUESTION` | ui/dashboard/UpdatePrompt.kt; MainActivity.kt:121-203; DashboardScreen.kt:146-168 | stale item first on the dashboard (above the setup checklist); after a check from it with weekly off, the weekly question |
| Updates section | DashboardScreen.kt:346-365 | "Check for updates now" (hidden while the stale item shows) and the "Check for updates weekly" toggle; turning it on runs a check (MainActivity.kt:436-439) |
| Manual check toast | MainActivity.kt:450-459 | available / up to date / failed, from the GitHub result only |
| INTERNET permission | AndroidManifest.xml:4-8 | the comment says the update check is the sole network use |
| Texts | strings.xml:9 (`onboarding_disclosure`), 49-63 (`dashboard_check_update`, `dashboard_auto_update(_hint)`, `update_check_*`, `dashboard_stale_check_*`, `dashboard_weekly_question_*`) | all describe checking GitHub |

### B. Version compare — already works on both sides, with no internet check

- **Phone** (v0.0.73/v0.0.74): a genuine `WATCH_READY` key 11 -> `ProtocolCodec.watchAppVersion` -> `NavigationController.handleInbound` (before the reducer dispatch, so the reply state already carries it) -> `recordWatchAppVersion` (prefs `watch_app_version`, write on change, never cleared). `UpdateState.updateAvailable = githubNewer || watchNewer` and `effectiveLatest` = the higher of the two (UpdateCheckRepository.kt:34-61). The card (`if (updateAvailable)`, DashboardScreen.kt:192) and bit 4 + key 14 (AppContainer.kt:168, `latestPhoneUpdate = effectiveLatest`, read on every SendState) are therefore **not gated** by any online check: with no check ever run, a newer watchapp already shows the card and sets bit 4. The v0.0.74 "Watchapp version (last seen)" row shows the stored value.
- **Watch**: `effective_update()` = `version_effective_update(s_latest_phone_version, APP_VERSION_STR, s_phone_version)` (main.c:973-975): the higher of the phone-reported key 14 and the watch's own version when strictly newer than the phone's key 11 (received on every state message). It drives the notice (once per version, deferred during a route), the idle text and the glance subtitle. A message with bit 4 clear empties key 17 but never hides the watch's own finding. The watch side is independent of the phone's online check and of `WATCH_READY` forwarding.
- **Redundancy**: when the phone found the update from the watch version, key 14 echoes the watch's own version back; harmless (`version_effective_update` picks the same value). Key 14 / bit 4 remain the only way the watch learns of a GitHub-only update (watch not yet auto-updated), so they stay needed while the online check stays.

### C. What pushes users toward the online check today (conflicts with "version compare is the default")

1. **Stale item on every fresh install**: `isCheckStale` is true when `last_check_millis == 0`, so every install that never ran an online check gets "Check for updates — PebbleNTN has never checked for a newer version. Tap below to check GitHub now" first on the dashboard, above the setup checklist. It also returns every 30 days. It ignores the version compare entirely: it shows even when the watch version proves the app is current, and above the "Update available" card when version compare already found one.
2. **Weekly question**: after a check from the stale item with weekly checks off, the dashboard asks "Check for updates automatically every week?".
3. **"Check for updates now" placement**: hidden from the Updates section while the stale item is shown at the top.
4. **Texts that frame GitHub as the only way**: `onboarding_disclosure` ("connects to the network only to check GitHub for a newer app version"), `dashboard_stale_check_body` / `_never_body` ("hasn't checked for a newer version" — untrue when the watch compare ran), `dashboard_auto_update_hint`, the manifest comment, `UpdateCheckRepository` and `AppContainer` KDoc ("Weekly (or on-demand) check ... on GitHub").
5. **Manual check result**: the toast says "up to date" from GitHub alone, even when the card shows a version-compare update.
6. **REQ-ANDROID-018** (outside the snapshot) specifies the stale behaviour; changing it needs a requirement amendment.

## Handoff Summary
- **Intent-relevant finding**: version compare is already functional and ungated on both devices. Phone: `UpdateState.updateAvailable = githubNewer || watchNewer` (UpdateCheckRepository.kt:42-44), and the card and bit 4/key 14 follow `effectiveLatest` with no check ever run (DashboardScreen.kt:192, AppContainer.kt:168). Watch: `effective_update()` (main.c:973) compares `APP_VERSION_STR` with the phone's key 11 by itself. What keeps the online check "the default" in the UI is the #94 stale prompt (`isCheckStale`: never checked or over 30 days, UpdateCheckRepository.kt:134-137; `updatePrompt`, UpdatePrompt.kt:23-27), which tops the dashboard on every fresh install and every 30 days, plus the weekly question and the GitHub-only texts.
- **Risks / follow-up**:
  - Open design questions for requirements: (a) keep, change or drop the stale item (for example, suppress it when a watchapp version has been received, or when version compare already shows an update, or drop it altogether since the watch compare now covers detection); (b) whether the weekly question stays; (c) whether the manual-check toast should take the watch version into account ("up to date" vs card); (d) new wording for the onboarding disclosure, the stale texts, the auto-check hint and the manifest comment; (e) what the phone shows when no watchapp version was ever received (the only signal then is the optional online check); (f) REQ-ANDROID-018 and REQ-ANDROID-019 amendments.
  - Correct the Core Devices statements (list under Technical Debt Signals). The synthetic readiness (`scheduleAutonomousReady`, `onAppOpened`) still serves auto-launch-off and settle timing, so only the rationale text changes, not necessarily the mechanism; whether to keep it is a separate decision.
  - Keep: `GitHubReleaseFetcher`, `checkForUpdate`, the auto-check toggle (default off), INTERNET, bit 4/key 14, `version_compare.c`, `update_notice.c`. Nothing in this intent removes them.
  - `watch_app_version` is never cleared and has no timestamp; a watchapp uninstalled or downgraded leaves the last value, but `watchNewer` compares against the running phone version, so updating the phone clears the card by itself.
  - Tests that pin the current stale behaviour: `UpdatePromptTest` (4), `UpdateCheckRepositoryTest` staleness cases (5).
