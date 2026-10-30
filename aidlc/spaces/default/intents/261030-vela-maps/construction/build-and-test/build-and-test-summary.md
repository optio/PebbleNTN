# Build and Test Summary — Vela Maps support (#120)

## Status: Ready

- **Build:** the debug APK builds.
- **Tests:** `scripts/test-all.sh` passes: 630 Android tests, 0 failures. The Vela regression passes 60/60, and the other apps are unchanged.
- **Route capture:** Vela on the Windows emulator, 8 scenarios (7 European cities in en-GB, Austin in en-US). All 486 captured cards are recognised with a specific arrow, after two fixes made here:
  - the harness now dismisses Vela's one-time voice dialog;
  - the rules now take "Keep slight …" and "Start your route".
- **Traceability:** FR1–FR5 and NFR1–NFR3 are covered.

## Open Items for the Person

- A drive with Vela on a real phone.
- Other languages need captures.
