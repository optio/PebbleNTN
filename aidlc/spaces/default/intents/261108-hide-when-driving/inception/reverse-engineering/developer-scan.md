# Developer Code Scan: PebbleNTN, intent 261108-hide-when-driving

Focused scan for GitHub issue #132 "When driving" at snapshot commit `61787f5d064451ceb3578bd18b17e259ddd0934c` (v0.0.81, clean working tree in the scanned paths). Depth Minimal, lead-only run. Nothing was built, tested or run.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - `protocol/` (all four files: `protocol-definition.json`, `generate.py`, `check_consistency.py`, `README.md`)
  - `android/app/src/main/java/com/pebblentn/app/core/NavigationSessionReducer.kt`
  - `android/app/src/main/java/com/pebblentn/app/core/NavigationInstruction.kt`
  - `android/app/src/main/java/com/pebblentn/app/core/NavigationState.kt`
  - `android/app/src/main/java/com/pebblentn/app/core/DistanceQuantizer.kt`
  - `android/app/src/main/java/com/pebblentn/app/core/WatchSettings.kt`
  - `android/app/src/main/java/com/pebblentn/app/core/WatchSettingsModel.kt`
  - `android/app/src/main/java/com/pebblentn/app/core/WatchSettingsSync.kt`
  - `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt`
  - `android/app/src/main/java/com/pebblentn/app/pebble/PebbleWatchTransport.kt`
  - `android/app/src/main/java/com/pebblentn/app/pebble/PebbleAppMessageMapper.kt`
  - `android/app/src/main/java/com/pebblentn/app/pebble/WatchListenerService.kt`
  - `android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt`
  - `android/app/src/main/java/com/pebblentn/app/protocol/WatchTransport.kt`
  - `android/app/src/main/java/com/pebblentn/app/notification/NotificationSnapshot.kt`
  - `android/app/src/main/java/com/pebblentn/app/notification/NotificationSnapshotFactory.kt`
  - `android/app/src/main/java/com/pebblentn/app/notification/DebugCaptureProcessor.kt`
  - `android/app/src/main/java/com/pebblentn/app/notification/NotificationDispatcher.kt`
  - `android/app/src/main/java/com/pebblentn/app/notification/NavigationNotificationListenerService.kt`
  - `android/app/src/main/java/com/pebblentn/app/data/WatchSettingsMirrorRepository.kt`
  - `android/app/src/main/java/com/pebblentn/app/data/WatchSettingsRepository.kt`
  - `android/app/src/main/java/com/pebblentn/app/data/DebugEvent.kt`
  - `android/app/src/main/java/com/pebblentn/app/data/DebugHistoryRepository.kt`
  - `android/app/src/main/java/com/pebblentn/app/data/DebugEventGrouping.kt`
  - `android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt`
  - `android/app/src/main/java/com/pebblentn/app/export/ExportBuilder.kt`
  - `android/app/src/main/java/com/pebblentn/app/export/DiagnosticExporter.kt`
  - `android/app/src/main/java/com/pebblentn/app/export/Redactor.kt`
  - `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/` (all three files)
  - `android/app/src/test/java/com/pebblentn/app/protocol/FakeWatchTransport.kt`
  - `watchapp/src/c/main.c` (header, formatting, connection watchdog, backlight, `render_navigation`, update records, `handle_stopped`, outbox, inbox, shortcuts, window and `init`/`deinit`; the panel, strip, road and message draw procs only skimmed)
  - `watchapp/src/c/theme.c`, `watchapp/src/c/theme.h`
  - `watchapp/src/c/settings_sync.c`, `watchapp/src/c/settings_sync.h`
  - `watchapp/src/c/settings_sync_glue.c`, `watchapp/src/c/settings_sync_glue.h`
  - `watchapp/src/c/revision.c`, `watchapp/src/c/revision.h`
  - `watchapp/src/c/settings_window.c` (helpers, Vibration door, top-level menu, refresh, push; the colour, glyph-pack, backlight and Shortcuts doors skimmed by outline), `watchapp/src/c/settings_window.h`
  - `watchapp/src/c/early_warning.h`, `watchapp/src/c/shortcuts.h`
- **Skimmed only**:
  - `android/app/src/main/java/com/pebblentn/app/core/PreviewCatalog.kt` (first 60 lines and its `when`)
  - `android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt` (watch-settings routes, by grep)
  - `android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt` (Watch section, by grep)
  - `android/app/src/main/java/com/pebblentn/app/` (every other file: listed with line counts, opened only by grep for `progress`, `speed`, `driving`, `FreshnessChecked`, `idle`)
  - `android/app/src/main/res/values/strings.xml` (watch settings strings and value arrays, by grep)
  - `android/app/src/test/java/com/pebblentn/app/` (test names of the controller, queue, sync, model, codec, snapshot and watch-settings UI tests; pinned counts by grep)
  - `watchapp/src/c/` (every other unit: listed; `update_notice.h` idle texts by grep)
  - `watchapp/tests/` (file list; `test_settings_sync.c` header and pinned persist keys)
  - `requirements/REQ-ANDROID.md`, `requirements/REQ-WATCH.md`, `requirements/REQ-DEBUG.md` (all headings; full text of REQ-WATCH-005/006/007/009/022/025, REQ-ANDROID-009/021/022/023, REQ-DEBUG-012 to 015)
  - outside the snapshot, read only to name an API: `android/pebblekit/src/main/kotlin/io/rebble/pebblekit2/client/PebbleSender.kt` (by grep)
- **Left out**:
  - AI-DLC's own install and workspace (`.claude/`, `aidlc/`)
  - build outputs and caches beside the scanned paths (`android/app/build/`, `android/.gradle/`, `watchapp/build/`), not opened

### Packages Found
- `:app` (`com.pebblentn.app`) — Android application — Kotlin — companion: notification capture, rules, session reducer, watch sync, Compose UI
- `:pebblekit` — vendored library — Kotlin — PebbleKit Android 2 1.2.0; exposes `startAppOnTheWatch` and `stopAppOnTheWatch` (the app uses only the first)
- `watchapp` — Pebble C app — C — renderer, settings menu, settings sync
- `protocol/` — generator — Python + JSON — one definition generating `Protocol.kt` and `protocol.h`

### Build System
- **Type**: Gradle (Android), pebble-tool/waf (watch), Python generator (protocol)
- **Config Files**: not reopened in this run (see the store's technology-stack and dependencies)
- **Build Dependencies**: `protocol/protocol-definition.json` -> `generate.py` -> `android/.../protocol/Protocol.kt` and `watchapp/src/c/generated/protocol.h`; `check_consistency.py` requires `protocolMajor`, `protocolMinor`, `keys` and `events` to equal the MANIFEST-protected `examples/protocol-definition.json`

### APIs Discovered
- AppMessage protocol 1.3 — `protocol/protocol-definition.json` — keys 0-40 (next free 41), events 1-4, 10-12 (free 5-9, 13+), flag bits 0-4, capability bits 0-3, platforms 0-5
- `WatchTransport` — `protocol/WatchTransport.kt` — `launchApp`, `send` (relaunches on `FailedDifferentAppOpen`), `sendToOpenApp` (never relaunches), `watchappClosed`, `watchLink`, `linkDiagnostics`; no close/stop call
- PebbleKit 2 listener — `WatchListenerService` — `onMessageReceived`, `onAppOpened` (synthetic WATCH_READY without key 11), `onAppClosed` (feeds `watchappClosed`)
- Export JSON — `ExportBuilder` — schema 5, metadata plus events; no non-notification log section

### Frameworks & Libraries
- No version re-read in this run; the store's dependencies table stands. Used in the area: kotlinx-coroutines (Mutex, StateFlow, SharedFlow), kotlinx-serialization (snapshot and export JSON), Jetpack Compose Material 3 (Watch settings screens), Timber, Pebble C SDK (AppMessage, persist, MenuLayer, vibes, light).

### Test Coverage
- **Test Directories**: `android/app/src/test/java/com/pebblentn/app/` (98 files), `watchapp/tests/` (12 host tests, now including `test_revision.c` and `test_settings_sync.c`)
- **Test Frameworks**: JUnit 4, Robolectric, coroutines-test, Compose UI test; plain C host tests (a `// Links: <unit>` header lets a test link a second unit)
- **Coverage Config**: absent
- Relevant suites: `NavigationControllerTest` (20 tests: launch-once, autonomous READY, auto-launch off, manual open, settings queue), `WatchSettingsQueueTest` (5), `WatchSettingsSyncTest` (10), `WatchSettingsModelTest` (7), `WatchSettingsUiTest` (8), `WatchSettingsScreenTest` (7), `ProtocolCodecTest`, `NotificationSnapshotFactoryTest` (progress captured when present), `ExportBuilderTest`, `DiagnosticExporterTest`, `test_settings_sync.c`, `test_revision.c`, `test_early_warning.c`. No test or code covers speed, driving or hiding.

### Code Quality Indicators
- **Linting**: Android Lint; C `-Wall -Wextra -Werror` in host tests (not re-read)
- **CI/CD**: not re-read in this run
- **Documentation**: every unit has a rationale header citing REQ IDs and issues; KDoc on public Kotlin APIs

### Technical Debt Signals
- `sync_persist_key` (`watchapp/src/c/settings_sync.c:140-142`) maps ids from `SET_TOUCH` on to `id + 4`, so a 24th and 25th setting would land on persist keys 27 (`PERSIST_KEY_VIBE_LAST`, `theme.c:8`) and 28 (`PERSIST_KEY_SETTINGS_REVISION`, `settings_sync_glue.c:8`). The next free key is 29 (`theme.c:3-7`).
- `ReducerEvent.FreshnessChecked` is never emitted by production code (grep), so the reducer has no time tick; the stale flag is only set when a state is re-sent.
- `WatchSettingsModel.fromWatch` (`WatchSettingsModel.kt:201-210`) fills any key a message leaves out with its default, so the phone cannot tell "absent from an older watchapp" from "set to default".
- The count 23 is pinned in `settings_sync.c` `_Static_assert`s (lines 62-65), `theme.c:11-15`, `test_settings_sync.c:114`, `WatchSettingsModelTest.twentyThreeSettingsInProtocolKeyOrder`, `ProtocolCodecTest.kt:277` and REQ-WATCH-025's text.
- Exhaustive `when`s over `WatchSetting` in `WatchSettingsUi.title` and `valueLabels` (`WatchSettingsUi.kt:110-157`) break the build on a new enum value (intended).
- `main.c` is 1,479 lines; `settings_window.c` 966.
- `DebugEventGrouping.key` ignores `progress`/`progressMax`, so repeated Google Maps cards collapse to first and last snapshot only (`DebugEventGrouping.kt:19-45`).
- `NotificationDebugEventEntity.transportStatus` is always written as null (`DebugHistoryRepository.kt`), an unused column.
- Store prose is stale in this area: About is now the first settings row (`settings_window.c:795`), not the last; the New instruction pattern has 6 values with Off = 5 and default Off; early-warning defaults are 25 m / Every update; Touch defaults On; shortcut defaults are ETA on Up and both side swipes, Backlight now on Down and Tap (`settings_sync.c:43-56`); the watch now has outbox sent/failed handlers.

## Handoff Summary
- **Intent-relevant finding**: Adding the two settings end to end follows one table on each side, but four places need care. (1) Protocol: append keys after `SETTING_SHORTCUT_TAP` (40), so `WatchSetting.key = SETTING_ACCENT + ordinal` (`WatchSettingsModel.kt:51`) and the C `_Static_assert` (`settings_sync.c:64-65`) hold; edit `protocol-definition.json`, the protected `examples/protocol-definition.json` and its MANIFEST hash together, then `generate.py`. (2) Watch storage: extend `kRules`/`kDefaults` (`settings_sync.c:14-56`, count field 5 bits, so up to 31 values) and fix `sync_persist_key` to give the new ids keys 29 and 30. (3) Phone: append to the `WatchSetting` enum with `valueCount`/`defaultValue`, add `title` and `valueLabels` branches, a units-dependent label array like `watch_values_early_warn_distance_metric`/`_imperial` (`strings.xml:588-603`), and a section in `WatchSettingsUi.sections`. (4) Watch menu: a door in the Backlight-door style (`settings_window.c` `BL_ROW_*`) with a units-aware label like `early_warn_label`. The hide logic itself has no home yet: the speed inputs (`progress`, `progressMax`, `subText`, `postTimeMillis`) live in `NotificationSnapshot` and reach only `DebugCaptureProcessor.onPosted`, which forwards just the `NavigationInstruction` (`DebugCaptureProcessor.kt:28-38`, `AppContainer.kt:244`).
- **Risks / follow-up**:
  - Any state send while hidden relaunches the watchapp: `sendWithRetry` uses `transport.send` (`NavigationController.kt:242-252`), which calls `startAppOnTheWatch` on `FailedDifferentAppOpen` while auto-launch is on (`PebbleWatchTransport.kt:77-82`). Hiding must suppress `SendState` or route it through `sendToOpenApp`.
  - `WatchTransport` has no close call; PebbleKit's `stopAppOnTheWatch` exists but is unused. The alternative is a watch-side exit on a message, as `handle_stopped` already does with `window_stack_pop_all(true)` (`main.c:997-1014`).
  - A manual open cannot be told apart from the phone's own launch by the inbound messages alone: both give `onAppOpened` (synthetic READY) and a real READY. Only the phone knows when it launched (`ReducerEffect.LaunchWatchApp`, `NavigationSessionReducer.kt:145-148`).
  - REQ-WATCH-006 forbids relaunching "merely to bring it to the foreground ... after the user leaves it" and "on a timer"; the 2-minute re-show needs an amendment, and REQ-WATCH-005/007 interplay (auto-launch off) must be decided.
  - The reducer quantizes distances (`DistanceQuantizer`), so speed must come from raw snapshot fields, not the sent instruction. ETA from `subText` has minute resolution. No timer exists for "below threshold for 2 minutes" when notifications stop.
  - Older peers: a 1.3 watch ignores unknown SETTING keys and a 1.3 phone ignores extra keys, but the phone would show defaults for a watch that lacks the settings (see `fromWatch`); a protocol minor bump to 1.4, or a check for the keys' presence, is needed to offer the rows only for a watch that has them.
  - Units: the watch's `SETTING_UNITS` is the only units setting; the speed list (10-60 km/h = 11 values, 5-40 mph = 8 values) needs a representation that survives a units change.
  - Next free IDs: REQ-WATCH-026, REQ-ANDROID-024, REQ-DEBUG-016; export schema 6 if the export gains a log section. aplite heap: a new door costs code one for one (REQ-WATCH-025 memory budget).
