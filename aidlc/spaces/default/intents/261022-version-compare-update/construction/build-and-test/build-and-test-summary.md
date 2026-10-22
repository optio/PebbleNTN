# Build and Test Summary — Version-compare update detection

## Build Status

The watchapp builds for all five platforms, the Android app builds, and `scripts/test-all.sh` passes. Prerequisites are in build-instructions.md.

## Test Types

- **Unit (host C):** `test_version_compare.c` (new, 38 assertions).
- **Unit (JVM):** `AppVersionsTest`, `UpdateCheckRepositoryTest`, `ProtocolCodecTest` and `NavigationControllerTest` (extended); `InstallSourceTest` and `UpdateCardTest` (new).
- **Emulator check on four watch platforms:** done in Code Generation.
- **Integration, performance and security:** not applicable at the Minimal strategy (each file says why).

## Coverage Expectations

- **Minimal strategy:** one test per unit-testable requirement and a happy path per unit (`version_compare`, `AppVersions`, `UpdateCheckRepository`, `InstallSources`, `ProtocolCodec`, `NavigationController`, `UpdateCard`).
- **Express scope:** no coverage floor.
- **CI before merge:** not applicable, because the plan runs no CI Pipeline.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-STRATEGY | code-generation-plan.md Testing Contract, strategy_volume | One test per unit-testable requirement; a happy path per component | FR1.1–FR1.4, FR2.1, FR2.2, FR3.1–FR3.4, FR4.1, FR5.2 and FR5.4 each tested; a happy path for all 7 units | test-results.md unit table | build-and-test | Met |
| TC-SUITE-GREEN | Testing Contract, scope_floor | Existing suite stays green | `scripts/test-all.sh` exit 0; 421 JVM tests, 0 failures | test-results.md | build-and-test | Met |
| NFR1 | requirements.md | No protocol change | `protocol/` and the generated files have no diff; the generated-constants check passes | `git status -- protocol …`; test-all | build-and-test | Met |
| NFR2 | requirements.md | No new network access | The watch-version update needs no fetch (FakeFetcher counts 0 calls); the existing "no call on launch" tests pass | UpdateCheckRepositoryTest | build-and-test | Met |
| NFR3 | requirements.md | Watch: no new persist writes; phone: written only on change | No new watch persist key; "same value again → no write" passes | main.c review; UpdateCheckRepositoryTest | build-and-test | Met |
| NFR4 | requirements.md | Watch texts inside the bezel on basalt, chalk, emery and diorite (pass criterion: emulator screenshots); the card layout is a manual check on a device | Verified on all four platforms. The card check is listed as a manual follow-up, as NFR4 says | ../code-generation/emulator-*.png | code-generation | Met |
| NFR5 | requirements.md | Version rule and logic host- and JVM-tested; test-all green | All listed tests present and passing | test-results.md | build-and-test | Met |
| TC-CI | Testing Contract (express) | CI execution before merge | Not in this plan | The plan runs no CI Pipeline | — | N/A (the plan runs no CI Pipeline) |

## Readiness

- **Build-ready:** yes.
- **Test-ready:** yes. All targets are Met.
- **Deployment-ready:** yes. It ships through the existing release pipeline when merged to `main`.

## Known Limitations

- **The phone card hasn't been checked on a device:** its layout, the source list, and the three destinations, including the Obtainium launch.
- **Core Devices app:** the phone can't learn the watch's version, so only the watch shows the update (FR3.5, accepted).
- **Card version label:** when the GitHub version wins, the card shows it with its `v` (for example `v0.0.73`), as before. The watch gets it without the `v`.
- **The F-Droid link** assumes the app is listed at `f-droid.org/packages/com.pebblentn.app/`.
