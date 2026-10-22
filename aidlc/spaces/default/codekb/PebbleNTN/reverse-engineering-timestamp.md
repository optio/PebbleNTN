# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-22
- Commit: 1ad9ec8f64ec3e2552f46134ee7dabd51df9e7a7
- Breadth requested: focused scan (snapshot paths watchapp/src/c, watchapp/tests, the Android update, ui, pebble, protocol, di, system and core packages, res/values, AndroidManifest.xml, the Android unit tests, protocol); depth Minimal
- Intent: `261022-version-compare-update`
- Coverage: version exchange in both directions, the #94 update state flow (UpdateCheckRepository, NavigationController `latestPhoneUpdate`, ProtocolCodec bit 4/key 14, watch update_notice and main.c), the dashboard "Update available" card and its link, install-source detection (none found), and the distribution channels. This run merges into a STALE store (prior full scan at b5a9e8b for `companion-update-notice`). Prior prose outside the focused area is kept. The prior `./` coverage is demoted to shallow. Build outputs, ignored files and the AI-DLC install were left out.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: 261022-version-compare-update
fingerprint: 216a362e7f0e6f849806a8f81931cb1cd73bda7b
analyzed:
  paths:
    - watchapp/src/c/main.c
    - watchapp/src/c/phone_version.c
    - watchapp/src/c/phone_version.h
    - watchapp/src/c/update_notice.c
    - watchapp/src/c/update_notice.h
    - watchapp/src/c/glance_text.h
    - watchapp/src/c/about_text.h
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
    - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt
    - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
    - android/app/src/main/res/values/strings.xml
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/test/java/com/pebblentn/app/update/AppVersionsTest.kt
    - protocol/protocol-definition.json
  components:
    - Watchapp Main Renderer
    - Watchapp Update Notice
    - Phone Protocol Codec
    - Phone Watch Transport
    - Navigation Controller
    - App Update Check
    - Dependency Container
shallow:
  paths:
    - ./
    - watchapp/src/c/
    - watchapp/tests/
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
