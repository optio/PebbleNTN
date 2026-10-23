# Build and Test Summary — Version comparison as the default update check

## Build Status

The Android app builds, lint passes, and `scripts/test-all.sh` passes.

## Test Types

- **Unit (JVM):** `UpdateCheckMessageTest` (new, 5 tests).
- **Manual check on a phone:** see integration-test-instructions.md.
- **Performance and security:** not applicable.

## Coverage Expectations

Minimal strategy: tests for FR1.1 and FR1.2, plus a happy path for `updateCheckMessage`. Express adds no coverage floor. There is no CI obligation, because the plan runs no CI Pipeline.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-STRATEGY | Testing Contract, strategy_volume | One test per unit-testable requirement; happy path per component | FR1.1 and FR1.2 tested; happy path for `updateCheckMessage` | UpdateCheckMessageTest 5/5 | build-and-test | Met |
| TC-SUITE-GREEN | Testing Contract, scope_floor | Existing suite green | test-all exit 0; 429 tests, 0 failures | test-results.md | build-and-test | Met |
| NFR1 | requirements.md | No behaviour change beyond FR1 | Only MainActivity's message changes; the rest is comments, KDoc, strings and specs | code-summary.md; diff | build-and-test | Met |
| NFR2 | requirements.md | No new network access | GitHub check triggers unchanged; existing tests green | UpdateCheckRepositoryTest | build-and-test | Met |
| NFR3 | requirements.md | JVM test for FR1; test-all green | Yes | test-results.md | build-and-test | Met |
| NFR4 | requirements.md | Texts as string resources; version in a placeholder | `update_check_available_version` uses `%1$s`; texts in strings.xml | strings.xml | build-and-test | Met |
| TC-CI | Testing Contract (express) | CI before merge | Not in this plan | The plan runs no CI Pipeline | — | N/A (the plan runs no CI Pipeline) |

## Readiness

- Build-ready: yes.
- Test-ready: yes. All targets are Met.
- Deployment-ready: yes.

## Known Limitations

- The texts and the message haven't been seen on a phone yet.
- The older status entries (2026-07-15 and 2026-09-25) still describe the Core Devices limitation as it was understood then. They are kept as history; the open note is corrected.
