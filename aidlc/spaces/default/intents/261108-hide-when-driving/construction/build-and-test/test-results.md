# Test Results: "When driving" (#132)

Date: 2026-11-08. Branch `feat/hide-when-driving` (uncommitted working tree on top of 61787f5).

## Build

| Command | Result |
|---|---|
| `scripts/test-all.sh` (spec assets, protocol check, version sync, watch C tests, rule and catalog validation, Android test + lint) | exit 0 |
| `scripts/generate-protocol.sh --check` (inside test-all) | `Protocol.kt` and `protocol.h` up to date, protocol 1.4 |
| `scripts/validate-spec-assets.sh` (inside test-all) | OK, 34 files |
| `scripts/version.sh --check` (inside test-all) | 0.0.81 (versionCode 81) |
| `scripts/build-watchapp.sh` (Code Generation run) | all five platforms built; aplite free heap 3,482 B |
| `./android/gradlew -p android assembleDebug` | debug APK built (`android/app/build/outputs/apk/debug/app-debug.apk`) |

## Tests

| Suite | Total | Passed | Failed | Skipped |
|---|---|---|---|---|
| Android debug unit tests (`testDebugUnitTest`) | 703 | 699 | 0 | 4 |
| Android release unit tests (`testReleaseUnitTest`) | 703 | 699 | 0 | 4 |
| Android lint | 0 errors, no new warnings | | | |
| Watchapp host C tests (`scripts/test-watchapp-unit.sh`) | 13 units | 13 | 0 | 0 |

Counts are from the re-run after Loop-back 1 (`scripts/test-all.sh` exit 0). The 4 skipped tests were skipped before this change too. Android tests rose from 630 to 703 (73 new or extended, including the 3 emulator replay tests and the re-plan test added by Loop-back 1), including `DrivingSpeedEstimatorTest` (14), `DrivingPolicyTest` (14), `DrivingLogRepositoryTest` (5), 9 new `NavigationControllerTest` cases, export, settings model/sync/codec and UI tests. New host C test: `test_driving_label`.

Coverage percentages are not measured: the express scope sets no coverage floor.

## Watch emulator check (integration, done here)

| Platform | Check | Result | Evidence |
|---|---|---|---|
| basalt | Main row "When driving / Show directions" | as designed | `evidence/basalt-1-main-row.png` |
| basalt | Door "When driving" with Directions: Show | as designed | `evidence/basalt-2-after-select.png` |
| basalt | Select → Hide; "Driving speed: Above 25 km/h" appears | as designed | `evidence/basalt-3-hide.png` |
| basalt | Speed list: "Default (25 km/h)", 10, 15, 20 km/h… | as designed | `evidence/basalt-4-speed-list.png` |
| basalt | Back: row reads "Hide above 25 km/h" | as designed | `evidence/basalt-5-main-row-after.png` |
| aplite | Row "Show directions" → Select → "Hide when driving" in place | as designed (accepted aplite deviation) | `evidence/aplite-1-main-row.png`, `evidence/aplite-2-after-select.png` |

Emulators were shut down afterwards.

## Emulator replay on real Google Maps notifications (requested at the gate)

Google Maps 26.14 on the Windows Android emulator, Brussels route, route-capture moving the location once per second at known speeds; the notification's progress, progress max, arrival line and post time recorded (titles left out) into `android/app/src/test/resources/driving/google-maps-{car-then-slow,bike,foot}.json`, then replayed through the real `NavigationController` (Hide on, 25 km/h, auto-launch on) by the new `DrivingEmulatorReplayTest`.

| Run | Simulated | Measured | Watchapp | Verdict |
|---|---|---|---|---|
| Car, then slow | 45 km/h for 150 s, then 3 km/h for 200 s | 37–46 km/h while driving (45 km/h median), 2.9 km/h when slow | closed at 60 s, reopened at 315 s (2 min 45 s after slowing) | as designed; confirms `progressMax − progress` is metres |
| Bike | 18 km/h for 180 s | **188 km/h at 44 s, 130 km/h at 60 s**, then 18 km/h | not closed (the spike did not last 30 s) | **defect** |
| Foot | 5 km/h for 180 s | a spike above 10 km/h, otherwise 0–5 km/h | not closed | **defect** (same cause) |

## Loop-Back Log

### Loop-back 1 — 2026-11-08T18:55:00Z

- **Diagnosis:** at the start of navigation Google Maps re-plans the route several times, and each re-plan changes `progressMax` (bike: 3091 → 3763 → 4457 → 3011 within 15 s). `DrivingSpeedEstimator` restarts its window only when the remaining distance grows; a re-plan onto a *shorter* route (4440 → 3011 m remaining in about 1 s) is counted as distance travelled, giving a false 188 km/h. A spike that lasted 30 s would close the watchapp on a walk or bike ride.
- **Root-cause stage:** code-generation (`android/app/src/main/java/com/pebblentn/app/core/DrivingSpeedEstimator.kt`).
- **Planned fix:** treat any change of `progressMax` as a new route and restart the window there (keep the existing restart for a growing remaining distance); add a unit test for a shorter re-plan; keep `DrivingEmulatorReplayTest` as the regression on real Google Maps data.
- **Estimated impact:** effort about 15 minutes (a few lines plus one test); financial cost none; risk low (only the estimator's window restart changes; the car run's hide and reopen times may shift by a few seconds).
- **Outcome (person chose "Retry with fix"):** fixed in Code Generation and approved. Re-run of `DrivingEmulatorReplayTest`:

| Run | Measured after the fix | Watchapp | Verdict |
|---|---|---|---|
| Car, then slow | 37–46 km/h while driving (45 km/h median), 2.9 km/h when slow | closed at 60 s, reopened at 315 s | Met |
| Bike (18 km/h) | 17.6–18.5 km/h, no spike | never closed | Met |
| Foot (5 km/h) | 0.1–5.0 km/h | never closed | Met |

## Not run here

- The phone-and-watch drive checks in `integration-test-instructions.md` need a real phone with a Pebble and a real drive; the Windows Android emulator cannot run the Pebble companion app with a watch. They are owned by the person's on-device test before release (see the summary).

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-MINIMAL-1 | code-generation-plan.md Testing Contract: one test per requirement | every FR/NFR traced to a test or file | 40/40 IDs covered, sensor pass | `cross-unit-traceability.md` | build-and-test | Met |
| TC-MINIMAL-2 | Testing Contract: happy-path test per new component | estimator, policy, log, export section, settings section, dashboard line, watch label each tested | all present and passing | test classes listed above | build-and-test | Met |
| TC-FLOOR-1 | Testing Contract scope floor: existing suite green | 0 failures | 0 failures (699/699 minus 4 pre-existing skips) | `scripts/test-all.sh` exit 0 | build-and-test | Met |
| NFR2 | requirements.md NFR2 | ≤ 1 close and reopen per 2 min under threshold-crossing samples | traffic-light test: one hide, no show within 2 min | `DrivingPolicyTest` | build-and-test | Met |
| NFR4 | requirements.md NFR4 / REQ-WATCH-025 | aplite free heap within v0.0.76 − 500 B | 3,482 B free, about 60 B margin | `scripts/build-watchapp.sh` output | build-and-test | Met |
| NFR1 | requirements.md NFR1 | no new permission, location or network | manifest unchanged; no such code | `security-test-instructions.md` | build-and-test | Met |
| NFR6 | requirements.md NFR6 | default settings: unchanged behaviour | controller default-settings test: no close, same sends | `NavigationControllerTest` | build-and-test | Met |
| GATE-REPLAY | person's request at the gate: emulator check of closing/opening and metre/speed detection with Google Maps | measured ≈ simulated speed; closes when driving, reopens after slowing, stays open on foot/bike | 45/18/5 km/h measured; closed at 60 s, reopened at 315 s; no close on foot or bike | `DrivingEmulatorReplayTest` | build-and-test | Met |
