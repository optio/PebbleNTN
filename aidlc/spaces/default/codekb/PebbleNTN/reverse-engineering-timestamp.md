# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-26
- Commit: 6dec020c6548a0cdcbd39c0d1846ef164a45068a
- Breadth requested: focused scan (snapshot paths: `android/app/build.gradle.kts`, `android/build.gradle.kts`, `android/gradle/`, `android/app/src/main/`, `android/app/src/test/`, `.github/workflows/`, `docs/`, `fastlane/`, `fdroid/`, `spec/600-security-release/`, `requirements/`, `scripts/`); depth Minimal
- Intent: `261025-play-release` (GitHub issue #114, prepare the first Google Play release)
- Coverage: the Android build files and version catalog, the manifest, the `update/` install-source, GitHub-check, manual-message and rule-update code, `AppContainer` and the application class, `MainActivity` (update source, check, feedback and email actions), the dashboard update card and prompt models, the onboarding screen, `DiagnosticShareManager`, the Robolectric configuration and `InstallSourceTest`, the `play-release` workflow, the privacy policy, Data safety, privacy review and release checklist docs, the whole `fastlane/` listing (text lengths and image sizes), the F-Droid recipe, `spec/600-security-release/` and REQ-SECURITY, and `validate-spec-assets.sh`. Skimmed: the update parts of `DashboardScreen.kt`, the update, onboarding, feedback and URL strings, a search of all phone packages for network I/O, outbound intents and Android 16 exposure, the test listing, `release.yml` and `android.yml`, REQ-ANDROID-001/013/017-020, the F-Droid notes, the status log header, and (outside the snapshot) `MANIFEST.sha256.json`, the `:pebblekit` and `:fixture-publisher` SDK lines, the `screenshots/` listing and the watch QR generator's docstring. Issue #114 came from GitHub.
- Merge: focused merge into a STALE store (prior focused scan at 4779760 for `261025-settings-sync`, since shipped in v0.0.77). Prior prose outside the update, install-source, build, privacy and store-listing area is kept. Added or updated: the #114 business context and the Google Play channel row, the "Update UI by install source" diagram, a one-build runtime-source design decision, the outbound links table and the Android 16 exposure note, three new components (Dashboard Update UI, Android Build Configuration, Store and Privacy Documents), SDK and Robolectric facts, and the "Play release findings". The prior analyzed paths are demoted to shallow, except those re-analyzed in this run. Build outputs (`android/app/build/`, `android/.gradle/`, `scripts/__pycache__/`), the wrapper jar, ignored files and the AI-DLC install were left out.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: play-release
fingerprint: 95f135304ff638724223b429f4444a103534ff17
analyzed:
  paths:
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
  components:
    - App Update Check
    - Ruleset Signing and Activation
    - Dependency Container
    - Dashboard Update UI
    - Android Build Configuration
    - CI Workflows
    - Distribution Metadata
    - Store and Privacy Documents
shallow:
  paths:
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/FeedbackCard.kt
    - android/app/src/main/java/com/pebblentn/app/data/FeedbackCampaign.kt
    - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt
    - android/app/src/main/java/com/pebblentn/app/update/SignedRuleset.kt
    - android/app/src/main/java/com/pebblentn/app/update/RulesetSignatureVerifier.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt
    - android/app/src/main/java/
    - android/app/src/main/res/values/strings.xml
    - android/app/src/main/res/
    - android/app/src/test/
    - .github/workflows/release.yml
    - .github/workflows/android.yml
    - docs/IMPLEMENTATION_STATUS.md
    - docs/FOR-HUMAN.md
    - fdroid/README.md
    - fdroid/REPRODUCIBLE_BUILDS.md
    - requirements/REQ-ANDROID.md
    - scripts/
    - MANIFEST.sha256.json
    - android/fixture-publisher/build.gradle.kts
    - android/pebblekit/build.gradle.kts
    - screenshots/
    - watchapp/tools/gen_connect_qr.py
    - protocol/
    - scripts/generate-protocol.sh
    - scripts/test-all.sh
    - watchapp/package.json
    - watchapp/src/c/main.c
    - watchapp/src/c/theme.c
    - watchapp/src/c/theme.h
    - watchapp/src/c/shortcuts.c
    - watchapp/src/c/shortcuts.h
    - watchapp/src/c/shortcut_apply.c
    - watchapp/src/c/shortcut_apply.h
    - watchapp/src/c/settings_window.c
    - watchapp/src/c/settings_window.h
    - watchapp/src/c/early_warning.h
    - watchapp/src/c/generated/protocol.h
    - watchapp/tools/capture_screenshots.py
    - android/app/src/main/java/com/pebblentn/app/pebble/
    - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/AppMessage.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
    - android/app/src/main/java/com/pebblentn/app/core/WatchSettings.kt
    - android/app/src/main/java/com/pebblentn/app/data/WatchSettingsRepository.kt
    - spec/200-architecture/Protocol.md
    - spec/200-architecture/Pebble.md
    - watchapp/src/c/
    - watchapp/tests/
    - watchapp/tools/
    - watchapp/tests/test_shortcuts.c
    - watchapp/tools/touch_emulator.py
    - watchapp/src/c/early_warning.c
    - watchapp/src/c/update_notice_window.c
    - watchapp/src/c/update_notice_window.h
    - watchapp/src/c/about_window.c
    - watchapp/src/c/about_window.h
    - watchapp/src/c/stops_text.c
    - watchapp/src/c/stops_text.h
    - watchapp/src/c/about_text.h
    - watchapp/src/c/eta_text.h
    - watchapp/src/c/glance_text.h
    - watchapp/src/c/panel_layout.h
    - watchapp/src/c/phone_version.h
    - watchapp/src/c/update_notice.h
    - watchapp/src/c/version_compare.h
    - watchapp/src/c/version_compare.c
    - watchapp/src/c/update_notice.c
    - watchapp/src/c/phone_version.c
    - watchapp/tests/test_stops_text.c
    - watchapp/tests/test_early_warning.c
    - watchapp/tests/test_version_compare.c
    - watchapp/tests/test_update_notice.c
    - watchapp/wscript
    - scripts/test-watchapp-unit.sh
    - requirements/REQ-WATCH.md
    - spec/200-architecture/Android.md
    - AGENTS.md
    - android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt
    - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt
    - android/app/src/main/java/com/pebblentn/app/ui/format/DisplayLabels.kt
    - android/app/src/main/java/com/pebblentn/app/ui/apps/NavigationAppsScreen.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRow.kt
    - android/app/src/main/java/com/pebblentn/app/ui/
    - android/app/src/main/java/com/pebblentn/app/system/
    - android/app/src/main/java/com/pebblentn/app/core/
    - android/app/src/main/java/com/pebblentn/app/di/
    - android/app/src/main/res/drawable-nodpi/
    - android/app/src/test/java/com/pebblentn/app/
    - android/app/src/test/java/com/pebblentn/app/protocol/FakeWatchTransport.kt
    - android/app/src/test/java/com/pebblentn/app/data/WatchSettingsRepositoryTest.kt
    - android/app/src/test/java/com/pebblentn/app/core/DistanceQuantizerTest.kt
    - android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt
    - android/app/src/test/java/com/pebblentn/app/update/AppVersionsTest.kt
    - android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt
    - android/app/src/test/java/com/pebblentn/app/protocol/ProtocolCodecTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdateCardTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdatePromptTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRowTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/apps/NavigationAppsScreenTest.kt
    - README.md
    - fdroid/
    - scripts/version.sh
    - android/pebblekit/
```
