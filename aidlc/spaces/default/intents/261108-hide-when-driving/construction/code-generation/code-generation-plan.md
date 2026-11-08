# Code Generation Plan: "When driving" (issue #132)

## Summary

- Builds: three synced "When driving" settings (protocol 1.4), the phone's speed estimator and hide/show logic, the watch and phone settings UI, a dashboard status line and a driving log in the diagnostics export
- Touches: `protocol/`, `android/app/src/main/java/com/pebblentn/app/{core,pebble,protocol,notification,data,export,di,ui/watchsettings,ui/dashboard}`, `strings.xml`, `watchapp/src/c/{settings_sync,theme,settings_window,about_text}`, `requirements/`, `spec/`, `docs/IMPLEMENTATION_STATUS.md`
- Tests: about 25 new JUnit tests (estimator, hide/show policy with virtual time, settings model and sync, codec, controller, export, UI labels) plus one host C test for the watch speed label; the existing Android and watch suites stay green

Source: `inception/requirements-analysis/requirements.md` (FR1–FR14, NFR1–NFR6). Zero-Unit express work: one implementation pass, no Units. Brownfield: files are changed in place.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "express",
  "test_strategy": "minimal",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "minimal",
    "strategy_volume": [
      "One verifiable test per requirement at the narrowest effective level.",
      "At least one happy-path unit test per component.",
      "Unit tests are the default; a bugfix/security scope floor may require an integration or E2E regression when that is the narrowest level that reproduces the defect."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:c8f84e35190e007d9863fad236154d0269ab9bf633fc2a3ff106ed965ba4a9b9",
  "contract_sha256": "sha256:bb76c1b22f8307be6e5780d50cabab891684449d5b164bd0e825e9e4e5c61e5b"
}
```

## Blast radius

| Area | Files | Impact |
|---|---|---|
| Protocol | `protocol/protocol-definition.json`, `examples/protocol-definition.json` (+ `MANIFEST.sha256.json`), generated `Protocol.kt` / `protocol.h` | high: shared by phone and watch; additive keys only |
| Phone core | `WatchSettingsModel.kt`, new `DrivingSpeedEstimator.kt`, new `DrivingPolicy.kt` | medium: pure logic, new files plus one enum extension |
| Phone controller and transport | `NavigationController.kt`, `WatchTransport.kt`, `PebbleWatchTransport.kt`, `FakeWatchTransport.kt`, `DebugCaptureProcessor.kt`, `AppContainer.kt` | high: send path and launch policy |
| Phone data and export | `WatchSettingsMirrorRepository.kt` / store, new `DrivingLogRepository.kt`, `ExportBuilder.kt`, `DiagnosticExporter.kt` | medium |
| Phone UI | `ui/watchsettings/*`, `ui/dashboard/*`, `res/values/strings.xml` | medium |
| Watchapp | `settings_sync.c/.h`, `theme.c/.h`, `settings_window.c`, `about_text.c`, new `driving_label.c/.h` | medium: aplite heap budget (NFR4) |
| Docs | `requirements/REQ-WATCH.md`, `REQ-ANDROID.md`, `REQ-DEBUG.md`, `spec/200-architecture/Pebble.md`, `spec/300-data/ExportFormat.md`, `docs/IMPLEMENTATION_STATUS.md`, `watchapp/tools/capture_screenshots.py` (menu row count) | low |

## Steps

### Step 1: Structure and configuration skeleton (FR1, constraints)
- [ ] In `protocol/protocol-definition.json` append keys 41 `SETTING_DRIVING_MODE`, 42 `SETTING_DRIVING_SPEED_METRIC`, 43 `SETTING_DRIVING_SPEED_IMPERIAL` after `SETTING_SHORTCUT_TAP`, and raise the minor from 3 to 4; mirror the change in the protected `examples/protocol-definition.json` and update its hash in `MANIFEST.sha256.json`.
- [ ] Regenerate `Protocol.kt` and `watchapp/src/c/generated/protocol.h` with `scripts/generate-protocol.sh` (never by hand) and run `protocol/check_consistency.py`.

### Step 2: Verify the test runners (unit-test-instructions.md)
- [ ] Verify the Gradle unit-test runner with one existing test class (`./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.core.DistanceQuantizerTest'` from `android/`) and the host C runner with one existing watch test, and record the exact scoped commands in `unit-test-instructions.md`.

### Step 3: Data model — implement (FR1.1–FR1.5, FR4)
- [ ] `WatchSettingsModel.kt`: append `DRIVING_MODE(2)` (0 = Show, 1 = Hide), `DRIVING_SPEED_METRIC(11, defaultValue = 3)` (index i = 10 + 5·i km/h, so 3 = 25 km/h) and `DRIVING_SPEED_IMPERIAL(8, defaultValue = 2)` (index i = 5 + 5·i mph, so 2 = 15 mph); add getters and a helper `drivingThresholdKmh(units)` that returns the active unit's threshold in km/h; limit `isShortcut` to `SHORTCUT_UP..SHORTCUT_TAP` so the new entries are not treated as shortcuts.
- [ ] Record the watch's reported protocol minor in the mirror state (beside the existing `watchappOutdated`), so the phone knows whether the watchapp has the driving settings (minor ≥ 4).
- [ ] Watch `theme.c/.h` and `settings_sync.c/.h`: three new `SettingId`s with `kRules` and `kDefaults` rows (2, 11, 8 values; defaults 0, 3, 2), getters and setters, `_Static_assert` counts from 23 to 26, and `sync_persist_key` changed so the new settings use persist keys 29, 30 and 31 (no collision with 27 and 28).

### Step 4: Data model — tests (FR1, FR4)
- [ ] JUnit: defaults and ranges of the three settings; `drivingThresholdKmh` for metric and imperial including the defaults (25 km/h, 15 mph ≈ 24.1 km/h); a units change keeps both stored values (FR1.4); `fromWatch` with an old watch (no keys 41–43) gives the defaults; `isShortcut` is false for the new entries; update the tests that pin 23 settings to 26.
- [ ] Run the scoped Gradle tests and the existing watch C tests that cover the settings table.

### Step 5: Repository — implement (FR14)
- [ ] New `data/DrivingLogRepository.kt`: an in-memory ring buffer (last 500 entries) of `DrivingLogEntry(timeMillis, measuredKmh?, plannedKmh?, thresholdKmh, decision)` where decision is one of `SAMPLE`, `HIDE`, `SHOW`, `KEPT_OPEN`, `RESET`; no text fields at all (FR14.1).

### Step 6: Repository — tests (FR14.1)
- [ ] JUnit: entries are kept in order, the oldest drop after 500, and the entry type has no string field that could hold notification text.

### Step 7: Business logic — implement (FR5–FR12, NFR2, NFR3)
- [ ] New pure `core/DrivingSpeedEstimator.kt`: takes samples `(postTimeMillis, progress, progressMax, subText, packageFamily)` and returns the smoothed measured speed (remaining route metres over time, window 30–60 s) and the planned speed (remaining metres ÷ time to the "Arrive h:mm" clock, parsed for 12 h and 24 h formats); ignores samples without progress values, during "Rerouting…", or where the distance did not count down (FR5.1–FR5.3); only Google Maps and MicroG Maps produce samples (FR5, FR5.4).
- [ ] New pure `core/DrivingPolicy.kt`: state machine `Visible → Hidden → Visible` with inputs (speed estimate, clock, mode, threshold, auto-launch, kept-open, session start/stop) and decisions `Hide`, `Show`, `None`: hide after ≥ 30 s above the threshold, or sooner when the planned speed is above 40 km/h (FR7); show after ≥ 120 s below the threshold − 5 km/h (FR9); never `Show` automatically when auto-launch is off but leave the hidden state (FR9.1); a user open while hidden sets kept-open for the session (FR10); a session stop resets everything (FR11); switching the mode to Show while hidden gives `Show` (FR12); at most one hide and one show per 120 s (NFR2). All timing constants in one companion object.
- [ ] `notification/DebugCaptureProcessor.kt` and `di/AppContainer.kt`: forward each eligible Google Maps / MicroG Maps snapshot's speed fields to the controller (new `onSpeedSample` callback), next to the matched instruction.

### Step 8: Business logic — tests (FR5–FR12, NFR2)
- [ ] `DrivingSpeedEstimatorTest`: walking samples give about 5 km/h; driving samples about 50 km/h; planned speed from "Arrive 12:12 PM" and from "Arrive 12:12"; rerouting, missing progress and a non-decreasing distance are ignored; another app's package gives no estimate.
- [ ] `DrivingPolicyTest` with virtual time: stays visible with mode Show at any speed; hides after 30 s above 25 km/h; does not hide on a 20 s burst; hides early on a planned 60 km/h; shows after 2 min below 20 km/h; traffic-light pattern (crossing every 20 s) gives at most one hide and no show within 2 min; auto-launch off leaves the hidden state without `Show`; kept-open blocks hiding until the session stops; session stop resets; mode switched to Show while hidden gives `Show`; imperial threshold of 15 mph is used when units are imperial.

### Step 9: Controller and transport ("API" layer) — implement (FR6–FR12, NFR5)
- [ ] `WatchTransport.kt`: add `suspend fun closeApp()` (default no-op for fakes); `PebbleWatchTransport.kt` implements it with PebbleKit's `stopAppOnTheWatch(appUuid)`, failures logged, not thrown.
- [ ] `NavigationController.kt`: own the estimator and the policy, read mode and speeds from the settings mirror, and act on decisions:
  - `Hide`: call `transport.closeApp()`, enter hidden, log `HIDE`.
  - While hidden: `SendState` is not sent through `send` (which would relaunch); the latest state is kept so it is sent on show (FR8.1, FR8.3). Settings sends keep using `sendToOpenApp`.
  - `Show`: run the existing launch path (`launchApp` + autonomous READY) so the watch gets the current state (FR9); not when auto-launch is off (FR9.1).
  - Manual open: an open signal while hidden that does not follow the controller's own launch within 5 s sets kept-open and resumes sends (FR10).
  - A coroutine timer (every 10 s while hidden) feeds the clock to the policy, so "below the threshold for 2 minutes" also works when Google Maps posts less often (no timer runs while visible or with mode Show).
  - Expose a `drivingStatus: StateFlow<DrivingStatus>` (hidden flag, last measured speed) for the dashboard (FR13).
- [ ] `FakeWatchTransport.kt`: record `closeApp` calls.

### Step 10: Controller and transport — tests (FR6–FR13)
- [ ] `NavigationControllerTest` (virtual time, fake transport): with mode Hide the watchapp still launches at session start (FR6); sustained driving samples lead to one `closeApp` and no further state sends (FR7, FR8.1); slow samples for 2 min relaunch and send the current state (FR9); an open while hidden keeps it open for the session (FR10); `drivingStatus` reports hidden with the speed (FR13); with default settings no `closeApp` ever happens and the send pattern is unchanged (NFR6).

### Step 11: Export — implement and test (FR14)
- [ ] `ExportBuilder.kt` / `DiagnosticExporter.kt`: add a `drivingLog` section from `DrivingLogRepository` to both the privacy-safe and the full export (FR14.2), raise the export schema version from 5 to 6 (FR14.3); wire it in `AppContainer`.
- [ ] JUnit: the section appears in both modes, holds only numbers, times and decision names, and the schema version is 6.

### Step 12: Frontend — implement (FR2, FR3, FR13)
- [ ] Phone `ui/watchsettings/`: a **When driving** section with the switch "Hide directions when driving", "Driving speed" with the value text and a slider over the active unit's list (steps of 5), greyed out while the switch is off, "Reset to default (25 km/h)" or "(15 mph)" shown only when changed, and the explanation text from FR3.4; no screenshot preview (FR3.5); the whole section hidden while the watchapp's protocol minor is below 4 (FR4). Strings in `strings.xml`: section title, switch, speed labels per unit (`watch_values_driving_speed_metric` / `_imperial` arrays), reset label and explanation. `data-testid`-style test tags on the switch, slider and reset button.
- [ ] Phone dashboard Status section: the line "Watch: hidden while driving (about 48 km/h)", in the active unit, while `drivingStatus.hidden` (FR13).
- [ ] Watchapp `settings_window.c`: a **When driving** row in the main list (value "Show directions" or "Hide above 25 km/h"), a door window headed **When driving** with **Directions** (Select toggles) and **Driving speed** listed only when Hide (FR2.1, FR2.2), and a speed list starting with "Default (25 km/h)" / "Default (15 mph)" with the "Selected" marker (FR2.3); `MAIN_ROW_COUNT` and the hand-copied row count in `watchapp/tools/capture_screenshots.py` updated. The label formatting goes into a new pure unit `driving_label.c/.h` so it is host-testable.
- [ ] Watchapp About text: one line "When driving: guessed from your speed" (FR2.4).

### Step 13: Frontend — tests (FR2, FR3, FR4, FR13)
- [ ] Host C test `watchapp/tests/test_driving_label.c`: "Show directions", "Hide above 25 km/h", "Hide above 15 mph", "Default (25 km/h)", "Above 60 km/h".
- [ ] JUnit (Robolectric/Compose, like the existing Watch settings and dashboard tests): the section is hidden for a minor-3 watchapp and shown for minor 4 (FR4); the slider is disabled while the switch is off (FR3.2); the reset button only shows when the speed differs from the default (FR3.3); the dashboard shows the hidden line with the speed in km/h and mph (FR13).

### Step 14: Build configuration
- [ ] Build the watchapp for all platforms with the Pebble SDK (`pebble build`) to confirm it compiles and the aplite build stays within its memory budget (NFR4); build the debug APK.

### Step 15: Documentation and traceability
- [ ] `requirements/REQ-WATCH.md`: new REQ-WATCH-026 (When driving settings and menu); amend REQ-WATCH-006 (the one allowed relaunch after a driving hide), REQ-WATCH-005 and REQ-WATCH-007 (combination with auto-launch off), and REQ-WATCH-025 (26 settings). `REQ-ANDROID.md`: new REQ-ANDROID-024 (speed estimate, hide/show, dashboard line), REQ-ANDROID-021 group list. `REQ-DEBUG.md`: new REQ-DEBUG-016 (driving log). `spec/200-architecture/Pebble.md` launch policy and `spec/300-data/ExportFormat.md` schema 6; update MANIFEST hashes for any protected file touched.
- [ ] `docs/IMPLEMENTATION_STATUS.md`: milestone, requirements, commands run, blockers, next task (project CLAUDE.md).
- [ ] Write `code-summary.md` and `traceability.json` (FR1–FR14 and NFR1–NFR6 to steps and tests).

## Requirement-to-step traceability

| Requirement | Steps |
|---|---|
| FR1 (FR1.1–FR1.5) | 1, 3, 4 |
| FR2 (FR2.1–FR2.4) | 12, 13 |
| FR3 (FR3.1–FR3.5) | 12, 13 |
| FR4 | 3, 4, 12, 13 |
| FR5 (FR5.1–FR5.4) | 7, 8 |
| FR6–FR12 | 7, 8, 9, 10 |
| FR13 | 9, 10, 12, 13 |
| FR14 (FR14.1–FR14.3) | 5, 6, 11 |
| NFR1 | 7, 9 (no permission, network or location code added) |
| NFR2, NFR3 | 7, 8 |
| NFR4 | 12, 14 |
| NFR5 | 1, 3, 4, 12 |
| NFR6 | 4, 10, existing suites |
