# Code Summary: "When driving" (issue #132)

Zero-Unit express work, brownfield, one implementation pass on branch `feat/hide-when-driving`.
Testing Contract: test-after, Minimal strategy; every step of the approved plan (1–15) is done and
ticked. Nothing was committed.

## Files created

| File | Purpose |
|---|---|
| `android/app/src/main/java/com/pebblentn/app/core/DrivingSpeedEstimator.kt` | `SpeedSample`, `SpeedEstimate` and the pure speed estimator (FR5, NFR1, NFR3) |
| `android/app/src/main/java/com/pebblentn/app/core/DrivingPolicy.kt` | `DrivingConfig`, `DrivingState`, `DrivingAction` and the pure hide/show policy (FR6–FR12, NFR2, NFR3) |
| `android/app/src/main/java/com/pebblentn/app/data/DrivingLogRepository.kt` | In-memory driving log, last 500 entries, numbers only (FR14.1) |
| `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/DrivingSection.kt` | The When driving section of Watch settings (FR3) |
| `android/app/src/main/java/com/pebblentn/app/ui/dashboard/DrivingStatusLine.kt` | Pure text of the dashboard line (FR13) |
| `watchapp/src/c/driving_label.c`, `watchapp/src/c/driving_label.h` | Pure, host-tested speed lists and labels (FR2) |
| `android/app/src/test/java/com/pebblentn/app/core/DrivingSpeedEstimatorTest.kt` | 14 tests |
| `android/app/src/test/java/com/pebblentn/app/core/DrivingPolicyTest.kt` | 14 tests |
| `android/app/src/test/java/com/pebblentn/app/data/DrivingLogRepositoryTest.kt` | 5 tests |
| `android/app/src/test/java/com/pebblentn/app/ui/dashboard/DashboardDrivingStatusTest.kt` | 5 tests (pure and Compose) |
| `watchapp/tests/test_driving_label.c` | Host C test of the labels |

## Files modified

- Protocol: `protocol/protocol-definition.json`, protected `examples/protocol-definition.json` (keys 41–43, minor 4), regenerated `android/app/src/main/java/com/pebblentn/app/protocol/Protocol.kt` and `watchapp/src/c/generated/protocol.h` (by `scripts/generate-protocol.sh`), `MANIFEST.sha256.json`.
- Phone core and data: `core/WatchSettingsModel.kt` (three settings, `DrivingSpeeds`, `isShortcut` limited to the slots, `isDriving`, getters, `drivingThresholdKmh`), `core/WatchSettingsSync.kt` (`watchProtocolMinor`, `hasDrivingSettings`, `DRIVING_PROTOCOL_MINOR`), `data/WatchSettingsMirrorRepository.kt` (persists the minor).
- Phone controller and transport: `protocol/WatchTransport.kt` (`closeApp`, default no-op), `pebble/PebbleWatchTransport.kt` (PebbleKit `stopAppOnTheWatch`, failures logged), `pebble/NavigationController.kt` (records the watch minor, owns estimator and policy, `onSpeedSample`, suppressed sends while hidden, ticker, manual-open detection, reset on stop, re-evaluation after settings changes, `drivingStatus`, `DrivingStatus`), `notification/DebugCaptureProcessor.kt` (`onSpeedSample` from raw snapshot fields), `di/AppContainer.kt` (log, estimator over the Google Maps rule family, wiring).
- Export: `export/ExportBuilder.kt` (`ExportedDrivingEntry`, `drivingLog`, schema 6), `export/DiagnosticExporter.kt`.
- Phone UI: `ui/watchsettings/WatchSettingsUi.kt`, `ui/watchsettings/WatchSettingsScreen.kt` (`onChangeSetting`), `ui/dashboard/DashboardScreen.kt` (`drivingStatus`, `imperialUnits`), `ui/MainActivity.kt`, `res/values/strings.xml`.
- Watch: `settings_sync.c/.h` (three ids, rules, defaults, persist keys 29–31, asserts 26, shortcut check limited to the slots), `theme.c/.h` (comment, getters and setters), `settings_window.c` (row, door and speed list), `about_text.h` and `about_window.c` (About line), `watchapp/tools/capture_screenshots.py` (row indices).
- Tests extended: `WatchSettingsModelTest`, `ProtocolCodecTest`, `WatchMessages` (a true 1.3 READY without keys 41–43, new `ready14`), `FakeWatchTransport` (`closeCount`), `NavigationControllerTest` (+9), `WatchSettingsMirrorRepositoryTest` (+1), `ExportBuilderTest` (+3, schema 6), `DiagnosticExporterTest` (+1), `WatchSettingsUiTest` (+3), `WatchSettingsScreenTest` (+6), `watchapp/tests/test_settings_sync.c`, `watchapp/tests/test_about_text.c`.
- Docs: `requirements/REQ-WATCH.md` (REQ-WATCH-026; 005, 006, 007, 025 amended), `requirements/REQ-ANDROID.md` (REQ-ANDROID-024; 021 amended), `requirements/REQ-DEBUG.md` (REQ-DEBUG-016), `spec/200-architecture/Pebble.md`, `spec/300-data/ExportFormat.md`, `docs/IMPLEMENTATION_STATUS.md`, and the Step 2 note in `unit-test-instructions.md`.

## Key implementation decisions

- **Hidden means no state sends at all.** `ReducerEffect.SendState` and `SendCompatibilityError` are skipped while hidden, so `PebbleWatchTransport.send`'s relaunch on `FailedDifferentAppOpen` can never reopen the watchapp. The reducer state stays untouched (still navigating, FR8.3), and the show goes through the normal launch path (launch plus the autonomous READY), which sends the current state.
- **Close via PebbleKit stop** (`stopAppOnTheWatch`), as the requirements assumed; no watch-side exit message was needed.
- **Manual open** = a WATCH_READY or WATCH_REQUEST_STATE while hidden that is not within 5 s of the controller's own launch.
- **Measured speed uses the time up to now**, not to the newest sample: Google Maps stops updating its card when the distance stops changing, so no new samples while hidden lower the speed. A 10 s timer re-evaluates only while hidden. History needs 30 s; window 60 s plus the newest older sample.
- **Reroute handling:** a card without a rule-matched instruction is not a sample (locale-independent, no extra text inspection); a growing remaining distance restarts the history.
- **Speed packages** come from the catalog's Google Maps rule family (Google Maps, MicroG Maps, packages the user added), so FR5's MicroG rule follows the catalog.
- **Timing constants** live in `DrivingPolicy` (30 s, 10 s early hide above a planned 40 km/h, 2 minutes, 5 km/h or 3 mph margin, 2-minute repeat limit) and `DrivingSpeedEstimator` (60 s window, 30 s minimum); the controller has the 10 s tick and the 5 s grace.
- **FR4 / NFR5:** the phone records the protocol minor of each real WATCH_READY in the mirror state (persisted); the section, and hiding itself, need minor ≥ 4.
- **Slider** edits are sent when the drag ends, so one change is one synced edit. Test tags: `watch-settings-driving-switch`, `-slider`, `-reset`, `dashboard-driving-status`.

## Deviations from the plan

1. **aplite has no When driving door (NFR4 wins over FR2.1–FR2.3 there).** The full door and list cost about 935 B of code plus 267 B of label code, so 1,245 B of aplite heap (3,615 → 2,370 B free at load). Measured against HEAD in a temporary git worktree, which was removed afterwards. HEAD is already about 290 B below #102's figure, which leaves roughly 196 B before REQ-WATCH-025's "v0.0.76 minus 500 bytes". On aplite the When driving row therefore switches Show / Hide in place and reads "Show directions" / "Hide when driving" (no speed). The speed is set in the phone app and syncs all the same. Cost on aplite is now 133 B (3,482 B free). basalt, chalk, diorite and emery have the full door and list as specified (about 1.26 KB of ~42 KB). This follows the project's precedent (the RGB-backlight door and the touch code are compiled only where the hardware allows). It is written into REQ-WATCH-026 and needs the reviewer's acceptance.
2. **The About line** lives in `about_text.h` (`ABOUT_INTRO_TEXT`, host-tested) and is appended to the existing intro block in `about_window.c`, not in a new `about_text.c` function. This adds no TextLayer (heap).
3. **Speed-list "Selected" marker** is on the chosen speed's row; the "Default (…)" row is an action that resets the unit's speed.
4. **`DrivingPolicy` has four actions** (`NONE`, `HIDE`, `SHOW`, `LEAVE_HIDDEN`) instead of three: `LEAVE_HIDDEN` is FR9.1's "leave the hidden state without reopening". It is logged as `SHOW`.
5. **The driving speed is stored as an int**, not as a bool-style persist write; mode and speeds use `persist_write_int`.
6. **`WatchMessages.ready13` now leaves out keys 41–43**, as a real 1.3 watchapp does. The existing 1.3 tests were unchanged apart from the pinned counts 23 → 26 and the protocol minor 3 → 4 in `ProtocolCodecTest` (expected-value updates for the new protocol, not relaxations).

## Test coverage summary (all run, all green)

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.core.DistanceQuantizerTest'` (from `android/`, runner check) | pass |
| `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.core.WatchSettings*' --tests 'com.pebblentn.app.protocol.*' --tests 'com.pebblentn.app.data.WatchSettingsMirrorRepositoryTest' --tests 'com.pebblentn.app.pebble.*' --tests 'com.pebblentn.app.ui.watchsettings.*'` (Step 4) | 99 tests, 0 failures (after one pinned minor updated 3 → 4) |
| `... --tests 'com.pebblentn.app.data.DrivingLogRepositoryTest'` | 5 / 0 failures |
| `... --tests 'com.pebblentn.app.core.DrivingSpeedEstimatorTest' --tests 'com.pebblentn.app.core.DrivingPolicyTest'` | 14 + 14 / 0 failures |
| `... --tests 'com.pebblentn.app.pebble.NavigationControllerTest'` | 29 (20 existing + 9 new) / 0 failures |
| `... --tests 'com.pebblentn.app.export.*'` | 9 + 17 + 6 / 0 failures |
| `... --tests 'com.pebblentn.app.ui.watchsettings.*' --tests 'com.pebblentn.app.ui.dashboard.*'` | 13 + 11 + 5 + 29 existing / 0 failures |
| `./android/gradlew -p android test lint assembleDebug` | debug 699 and release 699 unit tests, 0 failures, 4 skipped (were 630 each); lint 0 errors, no new warnings; debug APK built |
| `scripts/test-watchapp-unit.sh` | 13 host C tests (incl. new `test_driving_label`, extended `test_settings_sync` and `test_about_text`) OK |
| `scripts/build-watchapp.sh` (`pebble build`) | all five platforms; aplite free heap 3482 B (HEAD 3615 B) |
| `scripts/validate-spec-assets.sh`, `scripts/generate-protocol.sh --check`, `scripts/version.sh --check`, `scripts/validate-rules.sh` | OK |
| `scripts/test-all.sh` | exit 0 |

Every requirement FR1–FR14 and NFR2, NFR6 has at least one test; each new component has a happy-path test (estimator, policy, log, export section, settings section, dashboard line, watch label).

## Loop-back 1 fix (from Build and Test, 2026-11-08)

The emulator replay on real Google Maps notifications (Build and Test, Loop-back 1 in `../build-and-test/test-results.md`) showed that Google Maps re-plans the route a few times right after the start, changing `progressMax`; a re-plan onto a shorter route was read as travel (a false 188 km/h on a bike).

- `android/app/src/main/java/com/pebblentn/app/core/DrivingSpeedEstimator.kt`: each point keeps the route length, and a different `progressMax` restarts the window (as a growing remaining distance already did).
- `android/app/src/test/java/com/pebblentn/app/core/DrivingSpeedEstimatorTest.kt`: new `aReplanOntoAShorterRouteIsNotTravel`.
- New `android/app/src/test/java/com/pebblentn/app/pebble/DrivingEmulatorReplayTest.kt` with fixtures `android/app/src/test/resources/driving/google-maps-{car-then-slow,bike,foot}.json` (progress values, arrival line and post times only): car at 45 km/h measured 45 km/h, closed at 60 s and reopened 2 min 45 s after slowing; bike measured 18 km/h and foot 5 km/h, neither closed.
- After the fix: `scripts/test-all.sh` exit 0; debug and release 703 unit tests each, 0 failures, 4 skipped.

## Tests written but not executed

None. Not run, by constraint: Android emulator tests (no emulator in WSL) and the emulator checks named in REQ-WATCH-026 (basalt door and list, aplite row) and an end-to-end drive. Both are Build and Test work.
