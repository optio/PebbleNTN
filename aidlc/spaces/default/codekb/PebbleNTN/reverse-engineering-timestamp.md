# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-22
- Commit: c50710efa110c75cf60bd121065a5fc665e956f8
- Breadth requested: focused scan (snapshot paths: the Android `ui`, `update`, `pebble`, `protocol` and `di` packages, `res/values`, and the Android unit tests); depth Minimal
- Intent: `261022-watchapp-version-status`
- Coverage: the dashboard Status section and its row helpers, how `MainActivity` passes `UpdateCheckRepository.state` to `DashboardScreen`, `UpdateState.watchAppVersion` and its persistence (`watch_app_version`), the watch-version path (`ProtocolCodec.watchAppVersion`, `NavigationController.onWatchAppVersion`, `WatchListenerService`, `AppContainer` wiring), the merged v0.0.73 version-compare work on the phone (strict `AppVersions`, `InstallSource`, `UpdateCard`), and the test patterns (pure helpers, Robolectric Compose). The watch-side `version_compare.c` was only listed, not re-read.
- Merge: focused merge into a STALE store (prior focused scan at 1ad9ec8 for `261022-version-compare-update`). Prior prose outside the analyzed area is kept; the earlier version-compare proposal diagram is marked implemented. The prior analyzed paths are demoted to shallow (paths re-analyzed in this run are listed only under analyzed). Build outputs, ignored files and the AI-DLC install were left out.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: 261022-watchapp-version-status
fingerprint: 89d5e05015bef140feafe34f7b1cac2bb8c294f6
analyzed:
  paths:
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
  components:
    - App Update Check
    - Compose UI
    - Navigation Controller
    - Phone Protocol Codec
    - Phone Watch Transport
    - Dependency Container
shallow:
  paths:
    - ./
    - watchapp/src/c/
    - watchapp/tests/
    - watchapp/src/c/main.c
    - watchapp/src/c/phone_version.c
    - watchapp/src/c/phone_version.h
    - watchapp/src/c/update_notice.c
    - watchapp/src/c/update_notice.h
    - watchapp/src/c/glance_text.h
    - watchapp/src/c/about_text.h
    - android/app/src/main/java/com/pebblentn/app/update/ReleaseFetcher.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt
    - android/app/src/main/java/com/pebblentn/app/pebble/PebbleAppMessageMapper.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/AppMessage.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt
    - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/test/java/com/pebblentn/app/update/AppVersionsTest.kt
    - protocol/protocol-definition.json
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
    - watchapp/package.json
    - android/app/build.gradle.kts
    - .github/workflows/release.yml
    - android/pebblekit/
```
