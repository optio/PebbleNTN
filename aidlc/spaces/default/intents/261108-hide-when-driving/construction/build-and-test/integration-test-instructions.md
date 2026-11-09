# Integration Test Instructions: "When driving" (#132)

The test strategy is Minimal, so automated tests are unit level (see `../code-generation/unit-test-instructions.md`). The integrated behaviour needs a real Pebble (the emulators cannot run the Pebble companion app and a navigation app together), so it is checked by hand.

## Watch menu (done in this stage, Pebble emulator)

Install `watchapp/build/watchapp.pbw` on the basalt and aplite emulators, open Settings, scroll to **When driving**:

- basalt: row "When driving / Show directions" → door with **Directions: Show** → Select → **Hide**, **Driving speed: Above 25 km/h** appears → speed list starts with **Default (25 km/h)**, then 10, 15, 20 km/h… → back: the row reads "Hide above 25 km/h".
- aplite: the row switches in place: "Show directions" ↔ "Hide when driving" (no door, by design).

Evidence: `evidence/basalt-*.png`, `evidence/aplite-*.png`.

## On a phone with a Pebble (before release)

Install the debug APK (`android/app/build/outputs/apk/debug/app-debug.apk`) and the watchapp (`watchapp/build/watchapp.pbw`) on a real phone and watch.

1. **Settings sync:** in the phone app's Watch settings, the **When driving** section is visible (watchapp 1.4). Turn on "Hide directions when driving": the watch's row changes to "Hide above 25 km/h". Change the speed on the watch: the phone's slider follows. Switch Distance units: the speed shown switches to the mph value (15 mph by default) without changing the km/h one.
2. **Walking or cycling trip:** start Google Maps walking or cycling navigation: the watchapp opens and stays open the whole trip.
3. **Drive:** start Google Maps driving navigation. The watchapp opens at the start, then closes back to the watch face after about 30 s above 25 km/h (sooner on a fast road). No vibrations while hidden. The phone dashboard shows "Watch: hidden while driving (about … km/h)".
4. **Slow down:** park and walk on (or stand still in a long jam): after about 2 minutes below 20 km/h the watchapp reopens with the current instruction.
5. **Manual open:** while hidden, open PebbleNTN on the watch: it stays open and keeps updating until navigation ends.
6. **Auto-launch off:** turn off "Launch watchapp when navigation starts" and drive: the watchapp closes when driving and does not come back by itself.
7. **Export:** share a diagnostics export (privacy-safe): it contains a `drivingLog` section with times, speeds, threshold and decisions, and no street names.

Record the outcome of each check, and the actual speeds from the export, in the GitHub issue (#132) or the PR.
