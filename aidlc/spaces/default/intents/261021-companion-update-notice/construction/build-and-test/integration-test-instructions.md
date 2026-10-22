# Integration Test Instructions — #94

## Applicability

Not generated for this work. The test strategy is Minimal, and Minimal covers the change with requirement-driven unit tests only. See `../code-generation/unit-test-instructions.md`.

## Covered Elsewhere

- **Phone → watch:**
  - `ProtocolCodecTest` checks the phone half: bit 4 and key 14 on NO_ACTIVE, UPDATE and STOPPED.
  - `NavigationControllerTest.everySendReadsTheLatestPhoneUpdate` checks that every send reads the current update state.
  - The watch half was checked in the Pebble emulator during Code Generation, with `pebble send-app-message` carrying bit 4 and key 14. Screenshots: `../code-generation/emulator-*.png`.
- **Manual follow-ups:**
  - an end-to-end check with the real Android app on a watch;
  - an on-device check of the dashboard prompts.
