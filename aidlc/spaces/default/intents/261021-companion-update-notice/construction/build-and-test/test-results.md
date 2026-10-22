# Test Results — #94

## Build

- `scripts/build-watchapp.sh`: success (`'build' finished successfully`, `watchapp/build/watchapp.pbw` for emery, diorite, chalk, basalt, aplite).
- Android: `BUILD SUCCESSFUL` (inside `scripts/test-all.sh`).

## Unit Tests (this work, from unit-test-instructions.md)

| Command | Result |
|---|---|
| test_update_notice (host gcc, -Wall -Wextra -Werror) | 25/25 ok, `All update_notice assertions passed.` |
| test_glance_text (host gcc, -Wall -Wextra -Werror) | 15/15 ok (7 new) |
| ProtocolCodecTest | 16/16 passed (3 new) |
| UpdateCheckRepositoryTest | 12/12 passed (5 new) |
| UpdatePromptTest (new) | 4/4 passed |
| NavigationControllerTest | 16/16 passed (1 new) |

Failures: none.

## Full Suite

`scripts/test-all.sh`: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Spec asset validation | OK (34 files) |
| Protocol constants | consistent with `examples/protocol-definition.json`; `Protocol.kt` and `protocol.h` OK |
| Version sync | 0.0.70 (versionCode 70) |
| Watchapp unit tests | OK |
| Rules | Catalog and rule validation OK |
| Rule and regression suites | 185/185, 21/21, 12/12, 195/195 passed |
| Android build, JVM tests, lint | `BUILD SUCCESSFUL`, 402 JVM tests, 0 failures, 0 errors |

## Coverage

The C host tests have no coverage tool, and the Express scope has no line-coverage floor. The Minimal strategy needs one test per unit-testable requirement, and every one has a test (see the Target Verification Matrix in build-and-test-summary.md).

## Emulator Evidence (from Code Generation)

These were checked on basalt, chalk, emery and diorite:
- the notice on first launch;
- no notice when relaunching with the same version;
- the notice again for a new version;
- SELECT closes the notice without opening Settings;
- no notice during a route;
- the notice after the route stops;
- the notice closes after 10 s and the update idle text shows;
- the usual idle text when no update is pending;
- the launcher subtitle.

Screenshots: `../code-generation/emulator-*.png`.

## Re-run After Loop-back 1 (2026-10-22)

The subtitle is now `Update: {version}`. The re-run results:
- the build succeeds;
- test_update_notice and test_glance_text pass, with 15/15 for test_glance_text;
- `scripts/test-all.sh` exits 0 with 402 JVM tests and 0 failures.

The new launcher screenshots show the full `Update: 0.0.71` on basalt, chalk, emery and diorite (`../code-generation/emulator-*-7-launcher-subtitle.png`). NFR4-SUBTITLE is now Met.

### First run (kept for the record)

NFR4 (launcher subtitle readability) was Not Met: `Update phone app to {version}` was cut off. basalt, chalk and diorite showed `Update phone app …`, and emery showed `Update phone app to 0.…`.

## Loop-Back Log

### Loop-back 1 — 2026-10-22T17:54:44Z
- **Diagnosis**: NFR4-SUBTITLE Not Met. The launcher subtitle `Update phone app to {version}` is cut off after about 16 characters, so the version can't be read.
- **Root-cause stage**: code-generation (the subtitle text in `glance_text.c`).
- **Planned fix**: the person chose `Update: {version}` (14 characters for `0.0.71`). Change the glance template and its test, and update FR6 and REQ-WATCH-020 to the new wording. Then take new launcher screenshots on basalt, chalk, emery and diorite. The notice and the "No navigation detected" idle text stay as the clear update message when the watchapp is opened.
- **Estimated impact**: effort small (one text, one test, the spec wording, screenshots); financial cost none; risk low.
