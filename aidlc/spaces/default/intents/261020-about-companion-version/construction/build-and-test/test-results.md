# Test Results — #103

## Build

`scripts/build-watchapp.sh`: success (`'build' finished successfully`, `watchapp/build/watchapp.pbw` for emery, diorite, chalk, basalt, aplite).

## Unit Tests (this work, from unit-test-instructions.md)

| Command | Result |
|---|---|
| test_phone_version (host gcc, -Wall -Wextra -Werror) | 10/10 ok, `All phone_version assertions passed.` |
| test_about_text (host gcc, -Wall -Wextra -Werror) | 12/12 ok, `All about_text assertions passed.` |

Failures: none.

## Full Suite

`scripts/test-all.sh`: exit 0.

| Check | Result |
|---|---|
| Spec asset validation | OK (34 files) |
| Protocol constants | `Protocol.kt` and `protocol.h` OK, consistent with the definition |
| Version sync | 0.0.68 (versionCode 68) |
| Watchapp unit tests | OK |
| Catalog / rules | Catalog validation OK, Rule validation OK |
| Rule and regression suites | 185/185, 21/21, 12/12, 195/195 passed |
| Android build and tests | `BUILD SUCCESSFUL`, `All tests finished.` |

No Android or protocol file changed (`git status -- android protocol` is empty).

## Coverage

The C host tests have no coverage tool. The Minimal strategy needs one test per host-testable requirement, and every one has a test (see the Target Verification Matrix in build-and-test-summary.md).

## Emulator Evidence (from Code Generation)

These were checked on basalt, chalk, emery and diorite:
- never connected;
- last seen;
- scroll to the link;
- the version is kept after a relaunch with no phone;
- the text stays inside the bezel on chalk.

Basalt only: no refresh while About is open, and the row subtitle is unchanged. Screenshots: `../code-generation/emulator-*.png`.

## Target Verification Matrix

See build-and-test-summary.md. All targets are Met.
