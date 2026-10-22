# Test Results — Version-compare update detection

## Build

- **Watch:** `scripts/build-watchapp.sh` succeeds (`'build' finished successfully`, `watchapp/build/watchapp.pbw`).
- **Android:** `BUILD SUCCESSFUL` inside `scripts/test-all.sh`.

## Unit Tests (this work, from unit-test-instructions.md)

| Command / class | Result |
|---|---|
| test_version_compare (host gcc, -Wall -Wextra -Werror) | 38/38 ok, `All version_compare assertions passed.` |
| AppVersionsTest | 9/9 passed (3 new) |
| UpdateCheckRepositoryTest | 17/17 passed (5 new) |
| InstallSourceTest (new) | 3/3 passed |
| ProtocolCodecTest | 19/19 passed (3 new) |
| NavigationControllerTest | 18/18 passed (2 new) |
| UpdateCardTest (new) | 3/3 passed |

Failures: none.

## Full Suite

`scripts/test-all.sh`: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Spec asset validation | OK (34 files) |
| Protocol | consistent with `examples/protocol-definition.json`; `protocol/` and the generated files are unchanged |
| Version sync | 0.0.71 (versionCode 71) |
| Watchapp unit tests | 9 programs, all pass |
| Rule and regression suites | 185/185, 21/21, 12/12, 195/195 passed |
| Android build, JVM tests, lint | 421 JVM tests (402 before this work), 0 failures, 0 errors |

## Traceability Check

`aidlc engine sensor-traceability --stage build-and-test` first failed on NFR1, because traceability.json wrote its target as `protocol/protocol-definition.json (unchanged)`. That is a record-format slip, not a code defect. It was fixed in this stage by giving the plain path, and the check now passes.

## Coverage

The Express scope has no line-coverage floor. The Minimal strategy needs one test per unit-testable requirement and a happy path per unit, and both are met (see the Target Verification Matrix in build-and-test-summary.md).

## Emulator Evidence (from Code Generation)

These were checked on basalt, chalk, emery and diorite:
- the watch's version (0.0.71) newer than the phone's (0.0.70) shows the notice once;
- the idle update text shows;
- no second notice appears;
- the launcher subtitle reads `Update: 0.0.71`;
- with equal versions, the usual idle text shows and the subtitle is "Navigate on phone".

Screenshots: `../code-generation/emulator-*.png`.
