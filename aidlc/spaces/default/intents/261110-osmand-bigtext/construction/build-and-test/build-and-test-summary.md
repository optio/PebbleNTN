# Build and Test Summary: OsmAnd street and arrival time, Directions text (#134, #136)

## Build status

- **Build: success.** `scripts/test-all.sh` exits 0, all 5 watch platforms build, and the debug and release APKs build. The release APK is byte-identical with the #136 line.
- **Prerequisites:** see `build-instructions.md`.

## Test types

| Type | Instructions | Run here |
|---|---|---|
| Unit (JVM, workbench, watch C) | `construction/code-generation/unit-test-instructions.md` | yes, 753 JVM per variant, workbench 345 fixtures, 13 watch programs, all green |
| Integration: OsmAnd emulator (FR5.1) | `integration-test-instructions.md` §1 | yes, en and fr, 0 unrecognised, outputs as designed |
| Integration: watch row (FR7.2) | `integration-test-instructions.md` §2 | yes, basalt and aplite |
| Integration: phone and watch | `integration-test-instructions.md` §3 | no, needs a real phone with a Pebble (person's check before release) |
| Performance (NFR3, aplite heap) | `performance-test-instructions.md` | yes |
| Security and privacy (NFR6) | `security-test-instructions.md` | yes |

## Coverage

Express Minimal: no line-coverage floor. Every requirement except FR6.1 is traced to a test or file (`cross-unit-traceability.md`).

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-MINIMAL-1 | Testing Contract | every FR/NFR traced | 35 of 36; FR6.1 not | `cross-unit-traceability.md` | build-and-test | Not Met (FR6.1, accepted) |
| TC-MINIMAL-2 | Testing Contract | happy-path test per new component | all present, passing | `test-results.md` | build-and-test | Met |
| TC-FLOOR-1 | Testing Contract | existing suite green | 0 failures | `scripts/test-all.sh` exit 0 | build-and-test | Met |
| FR5.1 | requirements.md | en/fr OsmAnd layout confirmed | 0 unrecognised; road lines and clocks as designed | `test-results.md` | build-and-test | Met |
| FR6.1 | requirements.md | bundle has `debugsymbols/` | absent (stripped prebuilt libraries) | bundleRelease output | build-and-test | Not Met (accepted) |
| FR6.2 | requirements.md | APK unchanged | byte-identical | sha256 | build-and-test | Met |
| FR7.2 | requirements.md | watch row Full/Compact | as designed | `evidence/` | build-and-test | Met |
| NFR1 | requirements.md | fit 64/24 bytes | passes | `FixtureTextFitTest` | build-and-test | Met |
| NFR2 | requirements.md | no extra sends | passes | `NavigationControllerTest` | build-and-test | Met |
| NFR3 | requirements.md | < 5 ms per notification | about 0.17 ms | test XML timing | build-and-test | Met |
| NFR4 | requirements.md | min app version / ruleset bumps | passes | `RulesetVersionsTest` | build-and-test | Met |
| NFR5 | requirements.md | full suite green | exit 0 | `scripts/test-all.sh` | build-and-test | Met |
| NFR6 | requirements.md | no private data | none found | privacy check | build-and-test | Met |
| NFR7 | requirements.md | other apps unchanged at default | regressions pass | fixture regressions | build-and-test | Met |
| WATCH-HEAP | REQ-WATCH-025 | aplite ≥ 3,282 B free | 3,431 B | `scripts/build-watchapp.sh` | build-and-test | Met |
| CI | Testing Contract | CI before merge | the plan runs no CI Pipeline stage | `aidlc-state.md` | n/a | N/A |

## Readiness

- **Build-ready:** yes.
- **Test-ready:** yes. All suites green, emulator checks done.
- **Deployment-ready:** yes, with the accepted FR6.1 failure. #136 will be closed as not fixable from the app; the Play warning stays and is informational.

## Known limitations

- FR6.1 accepted as Not Met (see `test-results.md`).
- Compact roundabout text in en, es, it and pt-BR keeps connecting words ("1 exit and go <Street>").
- The aplite heap margin above REQ-WATCH-025's earlier floor is thin (3,431 B at load).
- The phone and watch re-send check needs a real device before release.
