# Unit Test Instructions: "When driving" (issue #132)

## Frameworks and configuration

- **Phone app:** JUnit 4 with kotlinx-coroutines-test (`runTest`, virtual time) and Robolectric/Compose UI tests, as already configured in `android/app/build.gradle.kts`. No new test dependency or configuration is needed.
- **Watchapp:** host C tests in `watchapp/tests/test_<unit>.c`, compiled with the host `gcc` against the matching pure unit in `watchapp/src/c/<unit>.c` (the pattern of `scripts/test-watchapp-unit.sh`). No Pebble SDK or emulator needed.

## Runner check (before the first test step)

From `android/`:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.core.DistanceQuantizerTest'
```

From the repository root (an existing pure watch unit):

```bash
gcc -std=c11 -Wall -Wextra -Werror -o "$TMPDIR/test_early_warning" watchapp/tests/test_early_warning.c watchapp/src/c/early_warning.c && "$TMPDIR/test_early_warning"
```

Verified on 2026-11-08 (Code Generation Step 2): the Gradle command above passed (exit 0) from
`android/`, and `scripts/test-watchapp-unit.sh` (all 12 existing host C tests, the same `gcc -std=c11
-Wall -Wextra -Werror` compile per unit, `// Links:` honoured) passed from the repository root. The
script is the recorded host C command for this change: it picks up the new
`watchapp/tests/test_driving_label.c` automatically.

## Commands for this change only

From `android/` (exact test classes of this change, including the existing classes it extends):

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.pebblentn.app.core.WatchSettingsModelTest' \
  --tests 'com.pebblentn.app.core.WatchSettingsSyncTest' \
  --tests 'com.pebblentn.app.core.DrivingSpeedEstimatorTest' \
  --tests 'com.pebblentn.app.core.DrivingPolicyTest' \
  --tests 'com.pebblentn.app.data.DrivingLogRepositoryTest' \
  --tests 'com.pebblentn.app.pebble.NavigationControllerTest' \
  --tests 'com.pebblentn.app.protocol.ProtocolCodecTest' \
  --tests 'com.pebblentn.app.export.*' \
  --tests 'com.pebblentn.app.ui.watchsettings.*' \
  --tests 'com.pebblentn.app.ui.dashboard.*'
```

Use the test class names that exist after generation; where the settings model or sync tests live under another existing class name, that class is listed in `code-summary.md` and replaces the name above.

From the repository root:

```bash
gcc -std=c11 -Wall -Wextra -Werror -o "$TMPDIR/test_driving_label" watchapp/tests/test_driving_label.c watchapp/src/c/driving_label.c && "$TMPDIR/test_driving_label"
```

## Expected scope and coverage

Minimal strategy: at least one test per requirement (FR1–FR14, NFR2, NFR6) and a happy-path test per new component (estimator, policy, log repository, export section, settings section, dashboard line, watch label). About 25 new tests. No coverage percentage floor applies to the express scope; the existing suites must stay green.

## Mocking and stubbing

- The watch link uses the existing `FakeWatchTransport` (extended to record `closeApp`). No PebbleKit in unit tests.
- Time: the controller and policy take an injectable clock; tests drive it with `runTest` virtual time (`advanceTimeBy`) and a fake `EpochClock`.
- Notification input: speed samples are built directly as data objects (post time, progress, progress max, subText, package); no Android notification objects.

## Test data

- Walking: remaining distance drops about 1.4 m/s. Driving: about 14 m/s (50 km/h). Traffic lights: alternating 20 s at 40 km/h and 20 s at 0.
- Arrival clocks in both "Arrive 12:12 PM" and "Arrive 12:12" forms.
- No real notification text, street names or captured diagnostics are used as test data.
