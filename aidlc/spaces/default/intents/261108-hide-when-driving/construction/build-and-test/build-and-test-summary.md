# Build and Test Summary: "When driving" (#132)

## Overall status

**Build: success. Tests: all passing** (after Loop-back 1, which fixed a false speed on a Google Maps re-plan found by the emulator replay). `scripts/test-all.sh` exits 0; 703 Android unit tests (debug and release) with 0 failures and 4 pre-existing skips; lint 0 errors; 13 watch host C tests pass; the watchapp builds for all five platforms; the debug APK builds.

## Test types

| Type | Status |
|---|---|
| Unit (phone, JUnit/Robolectric/Compose) | generated in Code Generation, run here: pass |
| Unit (watch, host C) | generated in Code Generation, run here: pass |
| Watch menu on emulator (basalt, aplite) | run here: as designed (`evidence/`) |
| Google Maps on the Android emulator, replayed through the controller | run here: car 45 km/h closes at 60 s and reopens after slowing; bike 18 km/h and foot 5 km/h stay open (`DrivingEmulatorReplayTest`) |
| Phone + watch drive checks | instructions in `integration-test-instructions.md`; need a real phone, watch and drive |
| Performance | no latency targets; watch heap budget checked (`performance-test-instructions.md`) |
| Security / privacy | static checks (`security-test-instructions.md`): no new permission, no text in the log |

## Coverage expectations

Minimal strategy, express scope: one test per requirement and a happy-path test per new component; no percentage floor. All 39 requirement IDs traced (`cross-unit-traceability.md`).

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-MINIMAL-1 | code-generation-plan.md Testing Contract | one test per requirement | 39/39 IDs covered | `cross-unit-traceability.md` | build-and-test | Met |
| TC-MINIMAL-2 | Testing Contract | happy-path test per new component | all new components tested | `test-results.md` | build-and-test | Met |
| TC-FLOOR-1 | Testing Contract scope floor | existing suite green | 0 failures | `scripts/test-all.sh` exit 0 | build-and-test | Met |
| NFR1 | requirements.md | no new permission, location or network | none added | `security-test-instructions.md` | build-and-test | Met |
| NFR2 | requirements.md | ≤ 1 close/reopen per 2 min | one hide, no show in the traffic-light test | `DrivingPolicyTest` | build-and-test | Met |
| NFR4 | requirements.md / REQ-WATCH-025 | aplite heap within budget | 3,482 B free (≈ 60 B margin) | `scripts/build-watchapp.sh` | build-and-test | Met |
| NFR6 | requirements.md | defaults unchanged | no close, same sends with defaults | `NavigationControllerTest` | build-and-test | Met |
| GATE-REPLAY | person's request at the gate | metre/speed detection and close/reopen on real Google Maps data | 45/18/5 km/h measured; closed at 60 s, reopened at 315 s; no close on foot/bike | `DrivingEmulatorReplayTest` | build-and-test | Met |

## Readiness

- Build-ready: yes.
- Test-ready: yes (all automated checks green).
- Deployment-ready: yes for merge; the on-device drive checks are recommended before or right after the release, because two assumptions can only be confirmed on a real drive.

## Known limitations

- `progressMax − progress` is metres: confirmed by the emulator replay. The timing constants (30 s, 10 s, 2 min, 5 km/h, 40 km/h) still deserve a real drive; the driving log in the export records the speeds to tune them.
- Google Maps' arrival time implies a low planned speed in town (16–23 km/h while driving 45 km/h), so the early hide above a planned 40 km/h mostly applies on fast roads; the 30 s measured rule does the work in town.
- The watchapp itself closing and opening on a watch is not visible in the emulators (no Pebble companion with a watch inside the Android emulator); the replay verifies the phone sends close and launch at the right moments.
- aplite: no When driving door; the row switches Show/Hide in place and the speed is set from the phone (accepted at Code Generation).
- aplite heap margin is about 60 B under the REQ-WATCH-025 limit: the next watch menu feature on aplite will need room made first.
- Speed estimates exist only for Google Maps and MicroG Maps; other apps never hide (by design, FR5.4).
