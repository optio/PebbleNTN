# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-23
- Commit: 8f08e0266e936c5758503c836c111bf0b2164bfb
- Breadth requested: focused scan (snapshot paths: the Android `update`, `ui`, `di`, `pebble` and `protocol` packages, `AndroidManifest.xml`, `res/values`, the Android unit tests, `watchapp/src/c/`, `watchapp/tests/`, `protocol/`); depth Minimal
- Intent: `261022-update-by-version-compar`
- Coverage: everything the online (GitHub) update check touches (`GitHubReleaseFetcher`, `checkForUpdate`, the auto-check toggle, `last_check_millis` / `isCheckStale`, the #94 stale item and weekly question, the Updates section, the manual-check toast, the startup auto-check), the phone half of version compare (`UpdateState.updateAvailable` / `effectiveLatest`, `recordWatchAppVersion`, `NavigationController.handleInbound`, `latestPhoneUpdate` wiring), the v0.0.74 watchapp version row, and the watch half (`version_compare.c/.h`, `update_notice.c/.h`, `main.c` `effective_update`). `ProtocolCodec`, `strings.xml`, the manifest, the `main.c` remainder and the tests were read by grep only.
- Merge: focused merge into a STALE store (prior focused scan at c50710e for `261022-watchapp-version-status`). Prior prose outside the analyzed area is kept; the "Core Devices does not forward watch messages" statements are corrected (forwarding confirmed on a real phone with v0.0.74 and `companionApp` declared), the v0.0.74 watchapp version row is marked implemented, and a diagram of the current update signals and #94 prompts is added. The prior analyzed paths not re-read deeply are demoted to shallow. Build outputs, ignored files and the AI-DLC install were left out.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: update-by-version-compar
fingerprint: 204d566b011027f9847744df1569961672847170
analyzed:
  paths:
    - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt
    - android/app/src/main/java/com/pebblentn/app/update/ReleaseFetcher.kt
    - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRow.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt
    - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/WatchListenerService.kt
    - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt
    - watchapp/src/c/version_compare.c
    - watchapp/src/c/version_compare.h
    - watchapp/src/c/update_notice.c
    - watchapp/src/c/update_notice.h
  components:
    - App Update Check
    - Compose UI
    - Navigation Controller
    - Phone Watch Transport
    - Dependency Container
    - Watchapp Update Notice
shallow:
  paths:
    - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt
    - android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/res/values/strings.xml
    - watchapp/src/c/main.c
    - android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt
    - android/app/src/test/java/com/pebblentn/app/update/AppVersionsTest.kt
    - android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt
    - android/app/src/test/java/com/pebblentn/app/protocol/ProtocolCodecTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdateCardTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdatePromptTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRowTest.kt
    - android/app/src/test/java/com/pebblentn/app/ui/apps/NavigationAppsScreenTest.kt
    - watchapp/tests/test_version_compare.c
    - watchapp/tests/test_update_notice.c
    - protocol/protocol-definition.json
    - requirements/REQ-ANDROID.md
    - spec/200-architecture/Pebble.md
    - docs/IMPLEMENTATION_STATUS.md
    - watchapp/package.json
    - ./
    - watchapp/src/c/
    - watchapp/tests/
    - watchapp/src/c/phone_version.c
    - watchapp/src/c/phone_version.h
    - watchapp/src/c/glance_text.h
    - watchapp/src/c/about_text.h
    - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/PebbleAppMessageMapper.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/AppMessage.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
    - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt
    - android/app/src/main/java/com/pebblentn/app/ui/
    - android/app/src/main/java/com/pebblentn/app/system/
    - android/app/src/main/java/com/pebblentn/app/core/
    - android/app/src/main/java/com/pebblentn/app/di/
    - android/app/src/test/java/com/pebblentn/app/
    - protocol/
    - README.md
    - fdroid/
    - scripts/version.sh
    - scripts/test-watchapp-unit.sh
    - watchapp/wscript
    - android/app/build.gradle.kts
    - .github/workflows/release.yml
    - android/pebblekit/
```
