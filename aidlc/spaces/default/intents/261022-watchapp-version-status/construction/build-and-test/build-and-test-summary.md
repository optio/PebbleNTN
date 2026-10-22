# Build and Test Summary — Watchapp version in the dashboard status

## Build Status

The Android app builds, lint passes, and `scripts/test-all.sh` passes.

## Test Types

- **Unit (JVM):** `WatchAppVersionRowTest` (new, 3 tests).
- **Manual check on a phone:** see integration-test-instructions.md.
- **Performance and security:** not applicable.

## Coverage Expectations

Minimal strategy: one test for each of FR1.2 and FR1.3, plus a happy path for `watchAppVersionText`. Express adds no coverage floor. There is no CI obligation, because the plan runs no CI Pipeline.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-STRATEGY | Testing Contract, strategy_volume | One test per unit-testable requirement; a happy path per component | FR1.2 and FR1.3 tested; happy path for `watchAppVersionText` | WatchAppVersionRowTest 3/3 | build-and-test | Met |
| TC-SUITE-GREEN | Testing Contract, scope_floor | Existing suite stays green | `scripts/test-all.sh` exit 0; 424 tests, 0 failures | test-results.md | build-and-test | Met |
| NFR1 | requirements.md | No new storage, protocol or network | Only `updateState.watchAppVersion` is read; no new preference, key or call | MainActivity.kt, DashboardScreen.kt diff | build-and-test | Met |
| NFR2 | requirements.md | Row text unit-tested; test-all green | Yes | test-results.md | build-and-test | Met |
| NFR3 | requirements.md | Row uses the shared `InfoRow` style, the same as the other Status rows | It uses `InfoRow`, now compact for all Status rows at the person's request. How it looks on a device is a manual follow-up, as NFR3 states | DashboardScreen.kt | code-generation | Met |
| TC-CI | Testing Contract (express) | CI before merge | Not in this plan | The plan runs no CI Pipeline | — | N/A (the plan runs no CI Pipeline) |

## Readiness

- Build-ready: yes.
- Test-ready: yes. All targets are Met.
- Deployment-ready: yes, via the release pipeline when merged to `main`.

## Known Limitations

- The row and the tighter spacing haven't been seen on a phone yet (no Android emulator in WSL).
- "Last seen" has no time attached: the row proves that a WATCH_READY arrived at least once with that version [Q1].
