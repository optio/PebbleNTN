# Integration Test Instructions — Version-compare update detection

## Applicability

Not generated. The test strategy is Minimal, which covers the change with requirement-driven unit tests (see `../code-generation/unit-test-instructions.md`).

## Covered Elsewhere

- **Watch side:** the version reaches the watch through key 11, and the watch shows the update UI. This was checked in the Pebble emulator on four platforms during Code Generation (`../code-generation/emulator-*.png`).
- **Phone side:** `ProtocolCodecTest` covers decoding the watch's WATCH_READY, and `NavigationControllerTest` covers the callback and the self-made ready events.
- **Manual follow-ups:**
  - on a phone, check the update card: its layout, the "Other sources" list, and the GitHub, F-Droid and Obtainium destinations;
  - with the original Pebble app, check that the phone picks up a newer watchapp.
