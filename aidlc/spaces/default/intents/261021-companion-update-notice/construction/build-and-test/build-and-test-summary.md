# Build and Test Summary — #94

## Build Status

- **Builds:** the watchapp builds for all five platforms, and the Android app builds.
- **Tests:** the full project suite (`scripts/test-all.sh`) passes.
- **Prerequisites:** see build-instructions.md.

## Test Types

- **Unit (host C):**
  - `test_update_notice.c` (new, 25 assertions);
  - `test_glance_text.c` (extended, 15).
- **Unit (JVM):**
  - `ProtocolCodecTest`, `UpdateCheckRepositoryTest`, `NavigationControllerTest` (extended);
  - `UpdatePromptTest` (new).
- **Emulator check on four platforms:** done in Code Generation. Evidence: `../code-generation/emulator-*.png`.
- **Integration, performance and security:** not applicable at the Minimal strategy. Each file says why.

## Coverage Expectations

- **Minimal strategy:** one test per unit-testable requirement, plus a happy path per unit (`update_notice`, `glance_text`, `ProtocolCodec`, `UpdateCheckRepository`, `UpdatePrompt`).
- **Express:** no coverage floor.
- **CI before merge:** does not apply, because the plan runs no CI Pipeline.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-STRATEGY | code-generation-plan.md Testing Contract, strategy_volume | One test per unit-testable requirement; a happy path per component | FR2.1–FR2.3, FR3.1, FR4.1, FR4.3, FR4.4, FR5.1, FR6.1–FR6.3, FR7.1–FR7.3 each tested; happy path for all 5 units | test-results.md unit table | build-and-test | Met |
| TC-SUITE-GREEN | Testing Contract, scope_floor | Existing suite stays green | `scripts/test-all.sh` exit 0, 402 JVM tests, 0 failures | test-results.md | build-and-test | Met |
| NFR1 | requirements.md | Either side can update first: no error with mixed 1.1/1.2 | Only the minor is bumped (1.2), and the compatibility check compares the major only; no update or key 14 on the compatibility error | ProtocolCodecTest; Protocol.md | build-and-test | Met |
| NFR2 | requirements.md | No new network access; weekly checks stay off by default | Existing "auto-check default off" / "no call on launch" tests green; the stale check only shows a prompt | UpdateCheckRepositoryTest | build-and-test | Met |
| NFR3 | requirements.md | Persist keys written only on change | The "same again" / "clear again" cases return no change | test_update_notice output | build-and-test | Met |
| NFR4-NOTICE | requirements.md NFR4 | Notice and both idle texts inside the bezel on basalt, chalk, emery, diorite | Verified on all four (chalk uses a smaller font for long messages) | ../code-generation/emulator-*-1/6/9/10-*.png | code-generation | Met |
| NFR4-SUBTITLE | requirements.md NFR4 | Subtitle readable on basalt, chalk, emery | `Update: 0.0.71` shown in full on basalt, chalk, emery, diorite (after loop-back 1; the first run's `Update phone app to …` was cut off) | ../code-generation/emulator-*-7-launcher-subtitle.png | code-generation | Met |
| NFR5 | requirements.md | Pure helpers host-tested, phone parts JVM-tested, test-all green | All listed tests present and passing | test-results.md | build-and-test | Met |
| TC-CI | Testing Contract (express) | CI execution before merge | Not in this plan | The plan runs no CI Pipeline | — | N/A (the plan runs no CI Pipeline) |

## Readiness

- **Build-ready:** yes.
- **Test-ready:** yes. Every target is Met; the subtitle was fixed in loop-back 1.
- **Deployment-ready:** yes. It ships through the existing release pipeline on merge to `main`.

## Known Limitations

- Not checked yet on a real phone and watch. The emulator used `pebble send-app-message` for bit 4 and key 14.
- The Android dashboard prompts are covered by `UpdatePromptTest` and a compile, but haven't been checked on a screen.
- The watch host tests don't run in CI.
