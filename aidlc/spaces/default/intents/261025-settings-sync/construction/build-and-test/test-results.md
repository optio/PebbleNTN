# Test Results — Settings sync (#102)

## Build

`pebble clean && pebble build` in `watchapp/` finishes successfully for all five platforms.

| Platform | Free heap at load |
|---|---|
| aplite | 3597 B (v0.0.76: 3916 B; 3903 B before the changes below) |
| basalt | 43669 B |
| chalk | 43309 B |
| diorite | 44069 B |
| emery | 108092 B |

`:app:assembleDebug :app:assembleRelease :app:lintDebug` succeed; the release APK keeps all 87 preview drawables (checked with aapt2 in Code Generation).

## Unit Tests (this work)

| Suite | Result |
|---|---|
| test_revision (new) | 21 assertions passed |
| test_settings_sync (new) | passed, with the new defaults |
| test_shortcuts (updated) | 60 assertions passed (was 47) |
| The other 9 watch host suites | pass |
| Android, new and extended: WatchSettingsSyncTest, WatchSettingsModelTest, PreviewCatalogTest, WatchSettingsMirrorRepositoryTest, WatchSettingsQueueTest, WatchSettingsUiTest, WatchSettingsScreenTest, ProtocolCodecTest, NavigationControllerTest | pass (272 tests in the scoped command, 0 failures) |

## Full Suite

`scripts/test-all.sh` rerun after the requested changes: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Spec asset validation, protocol `--check` and consistency | OK |
| Watch host suites, rules, regressions, route capture | pass |
| Android JVM tests (debug and release) and lint | 479 tests per variant, 0 failures |

## Emulator Checks (from Code Generation, basalt and aplite)

| Requirement | Result |
|---|---|
| FR2a launch READY carries 1.3 and all 23 settings | pass |
| FR2b a watch menu edit sends SETTINGS with the next revision | pass |
| FR3 newer phone SETTINGS applied, screen redrawn, confirmed at revision 97 | pass |
| FR3 stale phone revision ignored, watch replies with its own | pass |
| FR8 Off silent with the phone flag; Short pulse buzzes | pass |
| NFR1 aplite heap per Settings path within v0.0.76 − 500 B | pass (16 B less on every path) |
| FR2c resend on reconnect | **not testable**: `pebble emu-bt-connection` times out |

## Changes Requested at the Gate (2026-10-25)

The person asked for new defaults, About first and larger, and the phone's Watch settings row first. Checked:

| Check | Result |
|---|---|
| Watch defaults: vibration Off; early warning 25 m, every update, Double, Medium; Touch On; Up and swipes left/right ETA toggle; Down and Tap Backlight now | host tests (`test_settings_sync`, `test_shortcuts`) and `WatchSettingsModelTest` pass |
| About is the first Settings row, in larger type, with the two new sentences | emulator: aplite, basalt, chalk, emery |
| Watch settings row above "Launch watchapp when navigation starts" | built into the test APK |

**Defect found and fixed:** on aplite the Shortcuts action list (the only menu three windows deep) stopped scrolling and the app faulted after a few presses. This came with the #102 build (heap 3819 B without the new About text), not with v0.0.76, which scrolls the same list fine. The Shortcuts door now frees its menu while the action list covers it and rebuilds it on the same row when it shows again. Emulator on aplite: the list scrolls to its end, a pick returns to the door on the same row, About scrolls to its end, and Back returns to the main screen.

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: pass, no uncovered IDs (FR1–FR9).

## Not Covered (real phone and watch)

- The phone ↔ watch path end to end, and the Watch settings screen on a device (no Android emulator in WSL; JVM and Robolectric only).
- The reconnect resend (FR2c).
