# Developer Code Scan: PebbleNTN (intent 261025-play-release, GitHub issue #114)

Focused scan at 6dec020 (v0.0.77), depth Minimal. The scan read the code and ran none of the project's builds or tests.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - android/app/build.gradle.kts
  - android/build.gradle.kts
  - android/gradle/libs.versions.toml
  - android/gradle/wrapper/gradle-wrapper.properties
  - android/app/src/main/AndroidManifest.xml
  - android/app/src/main/res/values/themes.xml
  - android/app/src/main/java/com/pebblentn/app/PebbleNtnApplication.kt
  - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt
  - android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt
  - android/app/src/main/java/com/pebblentn/app/update/ReleaseFetcher.kt
  - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt
  - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckMessage.kt
  - android/app/src/main/java/com/pebblentn/app/update/RuleUpdateRepository.kt
  - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
  - android/app/src/main/java/com/pebblentn/app/ui/onboarding/OnboardingScreen.kt
  - android/app/src/main/java/com/pebblentn/app/export/DiagnosticShareManager.kt
  - android/app/src/test/resources/robolectric.properties
  - android/app/src/test/java/com/pebblentn/app/update/InstallSourceTest.kt
  - .github/workflows/play-release.yml
  - docs/PRIVACY_POLICY.md
  - docs/PLAY_DATA_SAFETY.md
  - docs/PRIVACY_REVIEW.md
  - docs/RELEASE_CHECKLIST.md
  - fastlane/
  - fdroid/metadata/com.pebblentn.app.yml
  - spec/600-security-release/BuildRelease.md
  - spec/600-security-release/SecurityPrivacy.md
  - requirements/REQ-SECURITY.md
  - scripts/validate-spec-assets.sh
- **Skimmed only**:
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt (the update prompt, update card, feedback card, Updates section and `sourceName`; the other sections not read)
  - android/app/src/main/java/com/pebblentn/app/ui/dashboard/FeedbackCard.kt (strings and buttons only)
  - android/app/src/main/java/com/pebblentn/app/data/FeedbackCampaign.kt (end date only)
  - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt, SignedRuleset.kt, RulesetSignatureVerifier.kt (signatures only)
  - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt (KDoc only)
  - android/app/src/main/java/ (a search of every package for network I/O, outbound intents, Android 16 exposure; files with no hit were not opened)
  - android/app/src/main/res/values/strings.xml (update, onboarding, feedback and URL strings only)
  - android/app/src/main/res/ (folder listing; `drawable-nodpi/` 100 PNGs, `mipmap-anydpi-v26/`, `xml/file_paths.xml`)
  - android/app/src/test/ (file listing, `@Test` counts for update/ and ui/dashboard/, `@Config` use)
  - .github/workflows/release.yml (triggers, verify job, signing, changelog commit), .github/workflows/android.yml (steps)
  - docs/IMPLEMENTATION_STATUS.md (header and latest entry only), docs/FOR-HUMAN.md (Play mentions only)
  - fdroid/README.md, fdroid/REPRODUCIBLE_BUILDS.md
  - requirements/REQ-ANDROID.md (REQ-ANDROID-001, 013, 017-020 read; the rest headings only)
  - scripts/ (listing; `test-all.sh`, `build-all.sh` gradle lines)
  - MANIFEST.sha256.json (which docs/spec/requirements files it protects; outside the snapshot)
  - android/fixture-publisher/build.gradle.kts, android/pebblekit/build.gradle.kts (SDK lines only; outside the snapshot)
  - screenshots/ (listing and README; outside the snapshot)
  - watchapp/tools/gen_connect_qr.py (docstring only; outside the snapshot)
- **Left out**: `android/app/build/`, `android/.gradle/`, `scripts/__pycache__/`, `android/gradle/wrapper/gradle-wrapper.jar` (binary), ignored files, and AI-DLC's own install.

### Packages Found
- `:app` (`com.pebblentn.app`) — Android application — Kotlin — the companion app; 123 Kotlin files under `src/main/java`, 79 test sources.
- `:pebblekit` — Android library — Kotlin + AIDL — vendored PebbleKit 2 (not rescanned).
- `:fixture-publisher` — Android application, debug only — Kotlin — targetSdk 35, not shipped.
- `fastlane/` — store metadata (F-Droid reads it today; Play would reuse it).

### Build System
- **Type**: Gradle 8.13 wrapper (SHA-256 pinned), AGP 8.13.2, Kotlin 2.3.10, KSP 2.3.10, JDK 21.
- **Config Files**: android/build.gradle.kts, android/app/build.gradle.kts, android/gradle/libs.versions.toml, gradle-wrapper.properties.
- **Build Dependencies**: `:app` → `:pebblekit`; `syncRuleData` stages `rules/catalog` and `rules/bundled` as assets.

### APIs Discovered
- HTTP (outbound) — `update/ReleaseFetcher.kt` — 1 call: `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest`.
- Outbound intents — `ui/MainActivity.kt`, `export/DiagnosticShareManager.kt` — 9 destinations (table in Facts below).
- No inbound network API. No HTTP client library (plain `HttpURLConnection`).

### Frameworks & Libraries
Unchanged since the store's last scan: Compose BOM 2024.12.01, Material 3, Navigation Compose 2.8.5, Activity Compose 1.9.3, Lifecycle 2.8.7, Room 2.7.2, kotlinx-serialization 1.7.3, coroutines 1.9.0, Timber 5.0.1, DataStore 1.1.1 (declared, unused), Robolectric 4.14.1, JUnit 4.13.2.

### Test Coverage
- **Test Directories**: android/app/src/test/ (79 sources), android/app/src/androidTest/.
- **Test Frameworks**: JUnit 4, Robolectric (pinned to `sdk=34` in `robolectric.properties`), Compose UI test.
- **Coverage Config**: absent.
- Update tests: `InstallSourceTest` (3), `UpdateCheckRepositoryTest` (17), `UpdateCheckMessageTest` (5), `AppVersionsTest` (9), `RuleUpdateRepositoryTest` (6), `RulesetSignatureVerifierTest` (6); dashboard: `UpdateCardTest` (3), `UpdatePromptTest` (4), `SetupProgressTest` (3), `WatchAppVersionRowTest` (3).

### Code Quality Indicators
- **Linting**: Android Lint (default rules) in `android.yml`, `release.yml` verify and pre-commit.
- **CI/CD**: android.yml, watchapp.yml, rules.yml, codeql.yml, release.yml (push to main), play-release.yml (manual).
- **Documentation**: good (REQ IDs, KDoc citing requirements); some privacy docs are out of date (see Technical Debt).

### Technical Debt Signals
- `play-release.yml:38` swallows the target-SDK gate failure (`|| echo "TODO(M10) ..."`).
- `release.yml` never runs `verifyReleaseTargetSdk`, although `docs/RELEASE_CHECKLIST.md:10` lists it as an automated gate.
- `docs/PRIVACY_REVIEW.md:17` says "There is no network code in the app" (the GitHub check exists).
- `docs/PRIVACY_POLICY.md` is still a template (`[DATE]`, `[MAINTAINER EMAIL / URL]`, lines 7-8).
- The onboarding disclosure (`strings.xml:9`) differs from the normative text in `spec/600-security-release/SecurityPrivacy.md:11` (the spec text has no app list or network sentence); nothing checks they agree.
- `fdroid/metadata/com.pebblentn.app.yml` still records 0.0.25 as the current version (F-Droid updates it from tags).
- `InstallSourceTest.kt:26` pins `com.android.vending` to GitHub, and REQ-ANDROID-020's acceptance says "Play give GitHub": both must change for #114.

## Facts relevant to #114

### 1. SDK levels and toolchain
- `android/app/build.gradle.kts:84` `compileSdk = 36`; `:88` `minSdk = 31`; `:90` `targetSdk = 35` (comment "Target SDK baseline 35"); `:91-92` versionCode 77, versionName 0.0.77.
- Play gate: `:69` `val playMinTargetSdk = 35` (comment says "PLAY_MIN_TARGET_SDK"); task `verifyReleaseTargetSdk` (`:70-79`) throws when `targetSdk < playMinTargetSdk`.
- AGP 8.13.2, Kotlin 2.3.10, KSP 2.3.10, Compose BOM 2024.12.01, Gradle 8.13 (`libs.versions.toml`, wrapper). AGP 8.13 already compiles against 36, so only `targetSdk` and the gate must move.
- `:pebblekit` compileSdk 36, no targetSdk (library); `:fixture-publisher` targetSdk 35 (debug only, not uploaded).
- Robolectric runs at `sdk=34` (`android/app/src/test/resources/robolectric.properties`), so a targetSdk of 36 does not hit Robolectric 4.14.1's maximum SDK; JVM tests do not exercise API 36 behaviour.
- Release builds: R8 + resource shrinking; `dependenciesInfo { includeInApk = false; includeInBundle = false }` (`:102-105`) for F-Droid reproducibility, which also removes the dependency metadata Play would otherwise read from the bundle (Play Console may show a notice; not a blocker). The release ART profile tasks are disabled (`:61-65`).

### 2. Android 16 (API 36) exposure seen in the code
From Android 16's behaviour changes for apps targeting 36 (list from memory, to confirm against the Android docs at requirements):
- **Edge-to-edge opt-out removed**: `MainActivity.onCreate` calls `enableEdgeToEdge()` (`MainActivity.kt:98`); 12 screens use `Scaffold` and pad with `innerPadding`; no `windowOptOutEdgeToEdgeEnforcement`, `statusBarColor` or `navigationBarColor` anywhere. Low exposure; check the onboarding and dashboard bottoms on a 3-button-navigation device.
- **Predictive back on by default** (`onBackPressed` no longer called): no `onBackPressed` override; the only custom back is Compose `BackHandler` in `RuleEditorScreen.kt:82` (works through `OnBackPressedDispatcher`); Navigation Compose 2.8.5 handles the rest. The manifest sets no `enableOnBackInvokedCallback`. Low exposure; check the rule editor's discard dialog.
- **Orientation and resizability ignored on large screens (sw600dp+)**: no `screenOrientation` or `resizeableActivity` in the manifest. No exposure.
- **Foreground services**: none declared, no `FOREGROUND_SERVICE` permission. No exposure.
- **Broadcasts**: the only runtime receiver, `NavigationAppInstallReceiver`, uses `ContextCompat.RECEIVER_NOT_EXPORTED` (`system/NavigationAppInstallReceiver.kt:32`). No exposure.
- **Intents (safer intent matching / redirection hardening)**: outbound intents are implicit `ACTION_VIEW`, `ACTION_SENDTO`, `ACTION_SEND` and a launch intent, all resolved by the system. PebbleKit 2's binding to the Core Devices companion (`io.rebble.pebblekit2.SEND_DATA_TO_WATCH`, vendored, not rescanned) is the one path worth a device check. The exported `WatchListenerService` filters by action. 
- **Scheduling / JobScheduler / `scheduleAtFixedRate`**: none used (event-driven, no polling).
- **Notification listener**: no Android 16 change seen that affects `NavigationNotificationListenerService`.
- No device run was made: the issue's "full suite plus a device check" is for Construction.

### 3. Install source detection (`update/InstallSource.kt`)
- `enum class InstallSource { GITHUB, FDROID, OBTAINIUM }` (`:10`).
- `fromInstaller(pkg)` (`:33-39`): `null` → GITHUB; `org.fdroid.fdroid`, `org.fdroid.basic`, `com.looker.droidify`, `com.machiav3lli.fdroid` → FDROID; `dev.imranr.obtainium` or `dev.imranr.obtainium.*` → OBTAINIUM; anything else (including `com.android.vending`) → GITHUB.
- `detect(context)` (`:45-51`): `packageManager.getInstallSourceInfo(packageName).installingPackageName`; any exception → GITHUB (logged).
- Detected once per process start: `AppContainer.installSource` (`AppContainer.kt:102`), a plain `val`.
- Exhaustive `when`s over the enum that a new `PLAY` value breaks at compile time: `MainActivity.openUpdateSource` (`MainActivity.kt:455-468`) and `DashboardScreen.sourceName` (`DashboardScreen.kt:485-490`). `UpdateCard.kt:17` `SOURCE_ORDER` lists the three sources and `updateCardModel` puts every other source under "Other sources".
- Tests: `InstallSourceTest.kt:26` asserts `com.android.vending` → GITHUB; `UpdateCardTest` covers the three current sources.

### 4. Every place GitHub / F-Droid / Obtainium update UI or links appear
| Where | What | Shown when |
|---|---|---|
| `DashboardScreen.kt:151-160` | Stale-check `PromptCard` "Check for updates" (strings `dashboard_stale_check_*`, body says "check GitHub now") | `updatePrompt == STALE_CHECK` (never checked, or over 30 days); first on the dashboard |
| `DashboardScreen.kt:161-169` | Weekly-check question (`dashboard_weekly_question_*`, "check GitHub about once a week") | after a stale check while weekly checks are off |
| `DashboardScreen.kt:195-201`, `:500-540` | `UpdateAvailableCard`: "Update from {source}", F-Droid delay line, "Other sources" dialog listing the other two sources | `updateAvailable` (GitHub newer or watchapp strictly newer) |
| `strings.xml:40` | card body: "...Tap below to get the update. If it won't install over the current app, uninstall PebbleNTN first..." | with the card |
| `DashboardScreen.kt:357-375` | Updates section: "Check for updates now" row (hidden while stale) and the "Check for updates weekly" toggle (`dashboard_auto_update_hint`: "also check GitHub about once a week") | always |
| `MainActivity.kt:455-468` `openUpdateSource` | GITHUB → `update_releases_url` (`https://github.com/optio/PebbleNTN/releases/latest`); FDROID → `update_fdroid_url` (`https://f-droid.org/packages/com.pebblentn.app/`); OBTAINIUM → launch intent of `dev.imranr.obtainium[.fdroid]`, else `update_obtainium_url` (`https://obtainium.imranr.dev/`) | card buttons |
| `MainActivity.kt:474-477` `setAutoCheckUpdates` | turning weekly on runs a check at once | toggle |
| `MainActivity.kt:489-500` `checkForUpdate` | forced GitHub check, toast `update_check_available_version` / `_up_to_date` / `_failed` | manual row, stale card |
| `AppContainer.kt:254-258` | weekly self-throttled `checkForUpdate(force = false)` on app start | only if `auto_check_enabled` |
| `AndroidManifest.xml:4-10` | INTERNET comment: "Sole use of the network: the optional app-update check against the GitHub Releases API" | n/a |
| `AndroidManifest.xml:42-45` | `<queries>` for `dev.imranr.obtainium` and `.fdroid` | n/a |
| `strings.xml:9` `onboarding_disclosure` | "...It uses the network only for the optional GitHub check (Check for updates, or weekly if you turn it on), which sends no personal data." | onboarding |
| `fastlane/.../full_description.txt` | "Internet — used only for an optional, off-by-default weekly check for a newer version on GitHub" | store listing |
| watch: bit 4 + key 14 (`AppContainer.kt:175`, `latestPhoneUpdate = effectiveLatest`) and the watch's notice "A phone app update is available. Open the mobile app to find the update." | phone-vs-watchapp notice; names no source | while `effectiveLatest` is set |
| watch: connection-hint QR (`watchapp/tools/gen_connect_qr.py`, outside the snapshot) | encodes the GitHub releases page for installing the phone app | on the watch, after the 5 s handshake timeout |

The phone-vs-watchapp version notice works offline: `UpdateState.updateAvailable = githubNewer || watchNewer` (`UpdateCheckRepository.kt:43-44`); `effectiveLatest` (`:51-61`) can come from the stored GitHub `latest_version` even when no check runs now. A Play install that updated over an earlier GitHub install with the same signature keeps prefs `pebblentn_update_check` (`auto_check_enabled`, `latest_version`, `last_check_millis`), so Play mode must ignore them, not only hide the toggle.

### 5. Every network call and outbound link, and what it sends
| Call | Code | Sends | Trigger |
|---|---|---|---|
| `GET https://api.github.com/repos/optio/PebbleNTN/releases/latest` | `ReleaseFetcher.kt:33-53` | headers `Accept: application/vnd.github+json`, `User-Agent: PebbleNTN-Android`; no auth, no body, no app version; the request reveals the IP address to GitHub; 8 s timeouts; reads `tag_name` | manual check, stale card, weekly toggle on, weekly on app start when opted in |
| Online rule updates | `RuleUpdateRepository.kt` | **no network code exists**: `tryActivate(envelopeJson)` only verifies a JSON string handed to it; `enabled = false` by default and `AppContainer.kt:110-113` passes `publicKeys = emptyList()`; nothing downloads or calls `tryActivate` in production code | never today |
| Feedback "Report a bug" | `MainActivity.kt:418`, `feedback_bug_url` | opens GitHub `issues/new?template=bug_report.yml&app-version=<versionName>&android-version=<Build.VERSION.RELEASE>` in the browser | user tap |
| Feedback "Request a feature" | `:419`, `feedback_feature_url` | GitHub `issues/new?...&app-version=<versionName>` | user tap |
| Feedback "Suggest a name" | `:420`, `feedback_name_url` | GitHub discussion #45 | user tap |
| Feedback "Send by email" | `:428-435` | `ACTION_SENDTO mailto:` draft to `share_logs_recipient` (`pebblentn@mg.mijnesite.be`, `strings.xml:131`), subject, body with app and Android versions | user tap; user sends |
| Diagnostics by email | `DiagnosticShareManager.shareViaEmail` | JSON export attached, same prefilled recipient; falls back to the chooser | user, after review |
| Diagnostics share sheet | `DiagnosticShareManager.share` | JSON export via `ACTION_SEND` chooser, no recipient | user |
| Update source links | `openUpdateSource` (section 4) | URL open only | user tap |
| Get the watchapp | `MainActivity.kt:180`, `watchapp_store_url` | `https://apps.repebble.com/ad4971e345854b909b73b1a9` | user tap |
| Get a navigation app | `MainActivity.kt:308`, `navigation_apps_store_url` | `https://play.google.com/store/apps/details?id=<pkg>` (web URL; a Play install would open the Play app) | user tap |
| PebbleKit 2 IPC | `pebble/` | local IPC to the companion app, not network | navigation |

The feedback card runs until 2026-12-31 (`FeedbackCampaign.CURRENT`, `data/FeedbackCampaign.kt:28`). `spec/600-security-release/SecurityPrivacy.md` "Sharing" says not to bundle a developer email as an automatic destination; the prefilled recipient is a draft the user sends, but the Data safety answers should describe it.

### 6. What the privacy docs say today
- `docs/PRIVACY_POLICY.md` (49 lines, a template with `[DATE]` and `[MAINTAINER EMAIL / URL]`): processing on the device; "Notification content and diagnostics are never uploaded automatically and PebbleNTN contains no analytics or tracking"; sharing only by user export; controls; not directed at children. It does not mention INTERNET, the GitHub check, online rule updates, feedback links or the email address.
- `docs/PLAY_DATA_SAFETY.md` (36 lines): "No data is collected or shared off the device ... nothing is transmitted automatically"; encryption in transit "Not applicable"; a data-types table (notification-derived info on-device only; no IDs, location, contacts); the notification-access justification; permissions list without INTERNET.
- `docs/PRIVACY_REVIEW.md:17`: "There is no network code in the app; exports are user-initiated only" (outdated).
- `spec/600-security-release/SecurityPrivacy.md:11` normative disclosure (no network sentence); "Remote official rules: Disabled initially. HTTPS required ...".
- `REQ-SEC-002` (no automatic upload), `REQ-SEC-004` (prominent disclosure), `REQ-SEC-005/006` (remote rules "when enabled in a later milestone"). `REQ-ANDROID-013` says the GitHub check "is the app's only network access" and that it must be reflected in the disclosure.
- `docs/RELEASE_CHECKLIST.md` "Store submission" (lines 46-51): policy hosted, Data safety entered, notification-access justification, Play App Signing. No `docs/PLAY_CONSOLE.md` exists.
- MANIFEST-protected (hash in `MANIFEST.sha256.json`, checked by `scripts/validate-spec-assets.sh`): `requirements/REQ-*.md`, `spec/600-security-release/*.md`; `docs/PRIVACY_POLICY.md`, `PLAY_DATA_SAFETY.md`, `RELEASE_CHECKLIST.md` are not protected.

### 7. Store listing (`fastlane/metadata/android/en-US/`)
- `title.txt` "PebbleNTN": 9 characters (limit 30).
- `short_description.txt`: 71 characters (limit 80).
- `full_description.txt`: 2,272 characters (limit 4,000). It claims "More apps can be added without a new release" (online rules do not exist yet) and "Internet — used only for an optional, off-by-default weekly check ... on GitHub"; it says the watchapp comes from the "Rebble app store / .pbw".
- `images/icon.png` 512x512 (4,573 bytes): matches Play's 512x512 icon.
- `images/phoneScreenshots/1.png`, `2.png`, `3.png`: each 1080x2280 (161-224 KB). 2280/1080 = 2.11, over Play's 2:1 limit; 1080x2160 is exactly 2:1.
- No `images/featureGraphic.png` (Play needs 1024x500).
- No tablet or watch screenshot folders. The watch shots in `screenshots/` are 144x168, 180x180 and 200x228, below Play's 320 px minimum side, so they need upscaling or composing into a larger image.
- `changelogs/`: 62 files up to `77.txt`; none over Play's 500-character release-notes limit. `release.yml:130-139` writes `changelogs/<versionCode>.txt` and commits it with `[skip ci]`.

### 8. The `play-release` workflow (`.github/workflows/play-release.yml`)
- `workflow_dispatch` with input `tag`; job `release` in environment `release`, `permissions: contents: write`.
- Checks out the tag, JDK 21, Android SDK; step "Verify Play target-SDK policy" runs `./gradlew -q verifyReleaseTargetSdk || echo "TODO(M10) implement release-time target SDK policy check"` (`:38`), so a failure never fails the job.
- Decodes `secrets.UPLOAD_KEYSTORE_BASE64` (fails when empty), builds `./gradlew bundleRelease` with `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; uploads `*.aab` + `.sha256` as artifact `release-aab`. No Play upload step.
- `release.yml` uses the same `UPLOAD_KEYSTORE_BASE64` secret for the GitHub APK, so the "upload key" today is the release key itself (#114 item 7).
- `release.yml` runs on push to `main` except `**/*.md`, `docs/**`, `fdroid/**`, `tools/**`, `scripts/test-all.sh`: a change to `fastlane/` images or `android/` triggers a release unless the commit says `[skip ci]`.

### 9. Onboarding disclosure (`strings.xml:9`, shown by `OnboardingScreen.kt:61-64` before the "Grant notification access" button)
"PebbleNTN reads notifications from supported navigation apps, extracts upcoming turn information, and sends it to your Pebble watch. Every supported app is enabled by default when installed; you can turn any of them off in Navigation apps. Processing occurs on this device. Notification content is not uploaded automatically. Diagnostic files are shared only when you explicitly export them. PebbleNTN spots new versions offline, by comparing the phone app with your watchapp. It uses the network only for the optional GitHub check (Check for updates, or weekly if you turn it on), which sends no personal data."
A Play build needs a variant without the GitHub sentence (or a source-neutral one), chosen at runtime from the install source.

### 10. Reviewer app access (what works without a watch)
Onboarding and notification access, the dashboard, Navigation apps list (`navigation-apps`), Rules (browse, edit, review), Debug history and detail, Share diagnostics (all three modes), Watch settings screen (needs a mirror from a watch, otherwise it pops back: `MainActivity.kt:283-284`). Needs a watch and the Core Devices app: turn-by-turn on the watch, the watchapp version row, settings sync.

## Handoff Summary
- **Intent-relevant finding**: Play detection fits the existing runtime install-source pattern: add `PLAY` for `com.android.vending` in `InstallSources.fromInstaller` (`InstallSource.kt:33-39`) and branch the update UI on `AppContainer.installSource` (`AppContainer.kt:102`), with no build flavour, so the F-Droid reproducible APK and the GitHub APK stay the same bytes as before. The surfaces to gate are listed in Facts section 4: the stale card, the weekly question, the Updates section rows, the weekly start-up check, "Other sources", the card body and `openUpdateSource`. The offline watchapp-newer notice keeps working, but `effectiveLatest` must ignore a stored GitHub `latest_version` on Play.
- **Risks / follow-up**:
  - "Online rule updates" are not implemented (`RuleUpdateRepository` is disabled, has no fetcher and no keys). The privacy policy, Data safety and review notes should describe them as not active, or as planned, not as a current network use. The listing's "More apps can be added without a new release" says the same thing too strongly.
  - The play-release gate is `|| echo` (`play-release.yml:38`), and `release.yml` does not run the gate at all.
  - Changing REQ-ANDROID-020 (acceptance "Play give GitHub"), REQ-ANDROID-013 ("only network access", "download the latest release from GitHub") or `SecurityPrivacy.md` needs the MANIFEST hashes updated in the same commit.
  - The phone screenshots are 1080x2280 (over 2:1), the feature graphic is missing, and the raw watch screenshots are below Play's 320 px minimum.
  - `docs/PRIVACY_REVIEW.md:17` and the manifest INTERNET comment are outdated; the privacy policy is still a template.
  - Android 16 exposure looks low (edge-to-edge already on, no foreground services, no `onBackPressed`), but nothing was run on a device; the PebbleKit companion binding is the main thing to check.
  - CLAUDE.md requires `docs/IMPLEMENTATION_STATUS.md` to be updated before coding.
