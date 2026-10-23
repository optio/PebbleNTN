# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-24
- Commit: 3727ddc8ab3c1dd531940aced5cfcd6e0be98de1
- Breadth requested: focused scan (snapshot paths: `watchapp/src/c/`, `watchapp/tests/`, `watchapp/tools/`, `watchapp/package.json`, `watchapp/wscript`, `requirements/REQ-WATCH.md`); depth Minimal
- Intent: `261023-touch-gestures` (GitHub issue #12, configurable gestures on touchscreen Pebbles)
- Coverage: the navigation window in `main.c` (click config, window lifecycle, the touch bridge calls, timers, message and notice paths, `on_settings_changed`, `apply_backlight`, the vibration and early-warning paths), every persisted setting in `theme.c/.h` with its key, setter and range, the early-warning enums, the settings menu and its doors in `settings_window.c`, the update notice and About windows (their click and bridge handling), the `stops_text` unit and its test (the pure-unit test pattern), `touch_emulator.py` and `capture_screenshots.py`, the watchapp build manifest and `wscript`, and the whole of `REQ-WATCH.md`. The other pure units were read by their headers only; the other host tests were skimmed. `scripts/test-watchapp-unit.sh` (outside the snapshot) was read only to confirm how host tests build. Issue #12 and its edit dates came from GitHub, and git history was used to compare the menu and persist keys at issue creation (50c4c3f).
- Merge: focused merge into a STALE store (prior focused scan at 8f08e02 for `261022-update-by-version-compar`, since shipped in v0.0.75). Prior prose outside the watchapp input and settings area is kept. Added: the active-intent context, the full watch persist-key and settings table, the settings menu rows, the watch input surface, an input and setting-change diagram, a new "Watchapp Emulator Test Tools" component (split from "Watchapp Asset Tools"), and the touch gesture findings. The prior analyzed paths are demoted to shallow. The repo-root `./` entry of the prior shallow list is dropped, because this block is partial. Build outputs (`watchapp/build/`, `watchapp/tools/__pycache__/`), ignored files and the AI-DLC install were left out.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: touch-gestures
fingerprint: 748e25fbe74e3186cdcbd7e87a122da063242bad
analyzed:
  paths:
    - watchapp/src/c/main.c
    - watchapp/src/c/theme.c
    - watchapp/src/c/theme.h
    - watchapp/src/c/settings_window.c
    - watchapp/src/c/settings_window.h
    - watchapp/src/c/early_warning.c
    - watchapp/src/c/early_warning.h
    - watchapp/src/c/update_notice_window.c
    - watchapp/src/c/update_notice_window.h
    - watchapp/src/c/about_window.c
    - watchapp/src/c/about_window.h
    - watchapp/src/c/stops_text.c
    - watchapp/src/c/stops_text.h
    - watchapp/tests/test_stops_text.c
    - watchapp/tools/touch_emulator.py
    - watchapp/tools/capture_screenshots.py
    - watchapp/package.json
    - watchapp/wscript
    - requirements/REQ-WATCH.md
  components:
    - Watchapp Main Renderer
    - Watchapp Settings Menu
    - Watchapp Settings Store and Theme
    - Watchapp Emulator Test Tools
    - Watchapp Host Unit Tests
shallow:
  paths:
    - watchapp/src/c/
    - watchapp/tests/
    - watchapp/src/c/about_text.h
    - watchapp/src/c/eta_text.h
    - watchapp/src/c/glance_text.h
    - watchapp/src/c/panel_layout.h
    - watchapp/src/c/phone_version.h
    - watchapp/src/c/update_notice.h
    - watchapp/src/c/version_compare.h
    - watchapp/tests/test_early_warning.c
    - scripts/test-watchapp-unit.sh
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
    - watchapp/src/c/update_notice.c
    - android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt
    - android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt
    - android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/res/values/strings.xml
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
    - watchapp/src/c/phone_version.c
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
    - android/app/build.gradle.kts
    - .github/workflows/release.yml
    - android/pebblekit/
```
