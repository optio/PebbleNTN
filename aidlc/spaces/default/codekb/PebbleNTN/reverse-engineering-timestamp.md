# Reverse Engineering Timestamp

## Run Record

- Date: 2026-10-25
- Commit: 4779760ac7d0cba527fe1c14753ef2839d426720
- Breadth requested: focused scan (snapshot paths: `protocol/`, `watchapp/src/c/`, `watchapp/tests/`, `watchapp/tools/`, `watchapp/package.json`, `android/app/src/main/`, `android/app/src/test/`, `android/app/build.gradle.kts`, `scripts/test-all.sh`, `scripts/generate-protocol.sh`, `spec/200-architecture/`, `requirements/`); depth Minimal
- Intent: `261025-settings-sync` (GitHub issue #102, sync the watchapp settings with the companion app both ways, with per-value preview screenshots)
- Coverage: the protocol definition, generator and consistency check with the generated `protocol.h`; on the watch, the AppMessage open, `send_ready`, `inbox_received_handler`, the connection watchdog, `on_settings_changed`, the shortcut run path and `init()` in `main.c` (the drawing procedures only skimmed), every persisted setting in `theme.c/.h` with its key, range, default and load check, `shortcuts.c/.h`, `shortcut_apply.c/.h`, the change paths and main rows of `settings_window.c` (the door drawing code skimmed), the early-warning enums, `package.json` and `capture_screenshots.py`; on the phone, the whole `pebble/` package, `ProtocolCodec`, `AppMessage`, `WatchTransport`, `WatchSettings` and its repository, `AppContainer` and the release build settings; `Protocol.md` and `Pebble.md`; `scripts/test-all.sh` and `generate-protocol.sh`. Skimmed: the reducer's READY and flag code, the dashboard row helpers and Watch section, `MainActivity` routes, `DisplayLabels`, strings conventions, the test helpers, `test_shortcuts.c`, `touch_emulator.py`, REQ-WATCH (011, 024 and every "SHALL NOT change the protocol" line), REQ-ANDROID headings and 009, the MANIFEST key list, `AGENTS.md` rules 15-18 (outside the snapshot), `scripts/test-watchapp-unit.sh` (outside the snapshot) and the `screenshots/` listing (outside the snapshot; listed only). Issue #102 came from GitHub.
- Merge: focused merge into a STALE store (prior focused scan at 3727ddc for `261023-touch-gestures`, since shipped in v0.0.76). Prior prose outside the settings, protocol and phone-transport area is kept. Added or updated: the #102 business context, the 4779760 watch input diagram and a "settings today" sequence, the persist-key table rows 19-27, the 11-row menu and Shortcuts door, the handshake, buffer and inbound-parse facts, the phone-side `WatchSettings` table, the preview capture surface, a new "Watchapp Shortcuts" component, and the "Settings sync findings". The prior analyzed paths are demoted to shallow, except those re-analyzed in this run. Build outputs (`watchapp/build/`, `watchapp/tools/__pycache__/`, `scripts/__pycache__/`), ignored files and the AI-DLC install were left out.

## Scope of Analysis

```yaml
scope_version: 1
kind: partial
intent: settings-sync
fingerprint: 7a4ce27ba5a8188361590cabaab1a6a727f31768
analyzed:
  paths:
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
    - android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt
    - android/app/build.gradle.kts
    - spec/200-architecture/Protocol.md
    - spec/200-architecture/Pebble.md
  components:
    - Protocol Definition and Generator
    - Watchapp Main Renderer
    - Watchapp Settings Menu
    - Watchapp Settings Store and Theme
    - Watchapp Shortcuts
    - Watchapp Emulator Test Tools
    - Phone Protocol Codec
    - Phone Watch Transport
    - Navigation Controller
    - Dependency Container
shallow:
  paths:
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
    - requirements/REQ-ANDROID.md
    - spec/200-architecture/Android.md
    - MANIFEST.sha256.json
    - AGENTS.md
    - screenshots/
    - android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt
    - android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt
    - android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt
    - android/app/src/main/java/com/pebblentn/app/ui/format/DisplayLabels.kt
    - android/app/src/main/java/com/pebblentn/app/ui/apps/NavigationAppsScreen.kt
    - android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt
    - android/app/src/main/java/com/pebblentn/app/update/ReleaseFetcher.kt
    - android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt
    - android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdatePrompt.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt
    - android/app/src/main/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRow.kt
    - android/app/src/main/java/com/pebblentn/app/ui/
    - android/app/src/main/java/com/pebblentn/app/system/
    - android/app/src/main/java/com/pebblentn/app/core/
    - android/app/src/main/java/com/pebblentn/app/di/
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/res/values/strings.xml
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
    - docs/IMPLEMENTATION_STATUS.md
    - README.md
    - fdroid/
    - scripts/version.sh
    - .github/workflows/release.yml
    - android/pebblekit/
```
