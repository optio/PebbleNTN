# Test Results — Shortcuts: touch gestures and UP/DOWN buttons (#12)

## Build

`pebble clean && pebble build` in `watchapp/` finishes successfully for all five platforms. The only linker note is the toolchain's existing "LOAD segment with RWX permissions" warning, which appears on every build.

| Platform | RAM footprint | Free heap at load |
|---|---|---|
| aplite | 20660 B / 24 KB | 3916 B (was 7430 B before this work) |
| basalt | 21542 B / 64 KB | 43994 B |
| chalk | 21854 B / 64 KB | 43682 B |
| diorite | 21150 B / 64 KB | 44386 B |
| emery | 22728 B / 128 KB | 108344 B |

## Unit Tests (this work)

| Suite | Result |
|---|---|
| test_shortcuts (new) | 47 assertions passed |
| The other 9 watchapp host suites | pass (unchanged) |

## Full Suite

`scripts/test-all.sh`: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Spec asset validation | OK (34 files) |
| Watch units, rules, regressions, route capture | pass |
| Android JVM tests and lint | 429 tests, 0 failures (no Android change) |

## Emulator Checks (from Code Generation, screenshots in `watchapp/build/shots/`)

| Requirement | Platform | Result |
|---|---|---|
| FR2 defaults: UP/DOWN do nothing | basalt | pass (0 differing pixels) |
| FR1 Shortcuts door with two button rows | basalt, aplite, chalk | pass |
| FR3/FR4 Up = Invert, banner shows, then clears after 1.5 s | basalt, chalk | pass |
| FR4 second action restarts the banner | basalt | pass |
| FR1 Touch Off hides the gesture rows; On shows five | emery | pass |
| FR5 Touch On + Tap = Next glyph pack | emery | pass |
| FR5 Touch Off: taps and swipes change nothing | emery | pass |
| FR5 a swipe right doesn't close the app | emery | pass |
| FR5 swipe directions run their own actions | emery | **not testable**: the emulator delivers swipes as taps |

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: pass, with no uncovered IDs (FR1–FR6).

## Not Covered (hardware check on a Pebble Time 2)

- Each of the four swipes runs its own action, and a swipe does not also run the Tap action.
- Touch is ignored while touch navigation is off in the watch's own settings.
- The aplite navigation screen: aplite gets no AppMessages in the emulator, the same as before this work. The checks ran on the connection-hint screen.
