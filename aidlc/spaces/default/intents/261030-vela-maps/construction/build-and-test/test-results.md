# Test Results — Vela Maps support (#120)

| Check | Result |
|---|---|
| `scripts/test-all.sh`, rerun after the fixes below | exit 0 |
| Android JVM tests (debug) | 630 tests, 4 skipped (opt-in store renderer), 0 failures (611 before this work) |
| Rule workbench regression | vela-maps 60/60 (50 synthetic + 10 from route capture); google-maps 189, comaps 21, organic-maps 12, osmand 195 |
| `validate-rules.sh`, `validate-spec-assets.sh` | OK; 28 catalog packages; 34 spec files match |
| Route-capture harness tests | 37 OK |
| `:app:assembleDebug`, lint | OK; no new issues |

## Route Capture on the Windows Emulator (FR4)

Vela Maps 0.5.2621 (stable release), Android 17 capture image, driven from WSL; the emulator was shut down after the run.

| Run | Scenarios | Captured | Recognised |
|---|---|---|---|
| First attempt | London en-GB, Austin en-US | 0 | both failed: "no navigation instructions after starting" |
| After the voice-dialog fix | London en-GB, Austin en-US | 102 | 102 |
| Remaining plan | Brussels, Paris, Amsterdam, Berlin, Milan, Madrid (en-GB) | 384 | 384 |

**Findings and fixes made in this stage:**
- **Harness:** Vela shows a one-time "Spoken navigation voice" dialog before its first route, which blocked navigation. "Use system voice" was added to the first-run labels (and its test). After that, the `google.navigation:` link opens a route preview and the driver taps "Start".
- **Rules:** with the first ruleset, 13 of the 486 cards matched only the generic no-arrow fallback: "Keep slight left onto …" and "Start your route". The slight-left/right rules now take "keep slight", and the head/continue rule takes "Start your route". Every captured card now gets a specific arrow: right 230, left 153, slight right 43, slight left 25, straight 25, arrive 6, roundabout 4.
- **Fixtures:** 10 real captures were added, including the two former misses, plus a synthetic "Keep slight right".
- **Observed:**
  - en-GB shows miles and feet with a 24-hour clock ("Arrive 15:26"); en-US uses a 12-hour clock ("Arrive 3:24 PM");
  - `secondaryText` is the clock in both;
  - "Your destination is on the left" matched as an arrival with a distance.

The run wasn't published as a GitHub issue (no `--publish`): nothing was unrecognised.

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: FR1–FR5 and NFR1–NFR3 are covered, with nothing uncovered.

## Not Covered

- Vela in other languages.
- The Play-install behaviour on a device (covered by JVM tests).
- A real phone.
