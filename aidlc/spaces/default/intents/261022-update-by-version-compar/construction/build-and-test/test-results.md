# Test Results — Version comparison as the default update check

## Build

`:app:assembleDebug :app:lintDebug`: `BUILD SUCCESSFUL` (from Code Generation; `test-all.sh` builds again).

## Unit Tests (this work)

| Class | Result |
|---|---|
| UpdateCheckMessageTest (new) | 5/5 passed, 0 failures |

## Full Suite

`scripts/test-all.sh`: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Spec asset validation | OK (34 files), after the REQ-ANDROID-018 wording fix below |
| Version sync | 0.0.74 (versionCode 74) |
| Android JVM tests and lint | 429 tests (424 + 5 new), 0 failures, 0 errors |
| Watch units, rules, regressions | pass (unchanged) |

## In-Stage Fix

REQ-ANDROID-018 still said "the onboarding disclosure is unchanged (#94)", which this work makes untrue. It now says the disclosure describes the version comparison as the default and the GitHub check as optional (REQ-ANDROID-019). The `MANIFEST.sha256.json` hash was resynced and validation passes. This is a spec wording fix only.

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: pass, with no uncovered IDs.

## Not Covered

The manual check on a phone (see integration-test-instructions.md).
