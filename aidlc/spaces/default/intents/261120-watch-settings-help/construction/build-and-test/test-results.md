# Test Results: Watch settings help text (#139)

Run on 2026-11-20/21, branch `feat/watch-settings-help` (uncommitted work on top of `073f51d`), WSL2 with JDK 21 and the Android SDK.

## Build

| Build | Command | Result |
|---|---|---|
| Debug + unit tests + lint | `scripts/test-all.sh` (ends with `gradlew test lint`) | BUILD SUCCESSFUL, exit 0 |
| Release, branch | `android/gradlew -p android :app:assembleRelease` | Success, `app-release-unsigned.apk` 3,658,535 bytes |
| Release, `main` baseline | the same build in a temporary worktree of `main` at `073f51d` | Success, 3,628,643 bytes |

## Tests

| Suite | Total | Passed | Failed | Skipped |
|---|---|---|---|---|
| Android JVM, debug (`testDebugUnitTest`) | 768 | 764 | 0 | 4 |
| Android JVM, release (`testReleaseUnitTest`) | 768 | 764 | 0 | 4 |
| of which the two changed classes (`WatchSettingsUiTest` 16, `WatchSettingsScreenTest` 25) | 41 | 41 | 0 | 0 |
| Watchapp C unit tests (13 suites) | all | all | 0 | 0 |
| Rule schema validation and rule-workbench regression | all | all | 0 | 0 |
| Catalog validator / route-capture / store graphics Python tests | 10 / 37 / 12 | all | 0 | 0 |
| GitHub workflows YAML parse | 6 files | 6 | 0 | 0 |

The 4 skipped JVM tests are the existing opt-in ones: `StoreScreenshotsTest` without `PEBBLENTN_STORE_SCREENSHOTS_DIR`. They were skipped before this change too. The new tests are 15 (753 → 768).

Other checks inside `scripts/test-all.sh`, all passing:
- spec asset integrity (`validate-spec-assets.sh`);
- protocol constants up to date;
- version sync;
- store graphics `--check`, using the pebble tool's Python with Pillow;
- Android lint with 0 errors.

Coverage: the express scope has no coverage floor, and the project has no coverage tool configured (`code-quality-assessment.md`). Every functional requirement has a test (see `cross-unit-traceability.md`).

## Security and privacy checks (NFR5)

| Check | Result |
|---|---|
| `git diff main --stat` on AndroidManifest, version catalog, app `build.gradle.kts` | no change |
| New network or storage API use in changed Kotlin | none |
| `@` or URLs in new strings | none |

## Failure details

None.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| NFR1 | requirements.md NFR1 | Row read as one item (title, value, help); click label unchanged; strings are resources | Merged-semantics test and click-label test pass | `WatchSettingsScreenTest` (25/25) | build-and-test | Met |
| NFR2 | requirements.md NFR2 | Help not cut off at font scale 2.0, 360 dp | Font-scale test passes; no `maxLines` on help | `WatchSettingsScreenTest` | build-and-test | Met |
| NFR3 | requirements.md NFR3 + Testing Contract scope floor | Existing suite green | 768/768 non-skipped JVM tests pass; watch, rule and tool suites pass | `scripts/test-all.sh` exit 0 | build-and-test | Met |
| NFR4-lint | requirements.md NFR4 | Lint passes, no new warnings in changed files | 0 errors; 72 warnings, all older and none in changed files | `gradlew lint` in `test-all.sh`; code-summary.md | build-and-test | Met |
| NFR4-suite | requirements.md NFR4 | `scripts/test-all.sh` passes | exit 0, "All tests finished." | test-all log | build-and-test | Met |
| NFR4-size | requirements.md NFR4 | Release APK grows < 51,200 bytes | +29,892 bytes (3,658,535 − 3,628,643) | assembleRelease on branch and `main` | build-and-test | Met |
| NFR5 | requirements.md NFR5 | No change to what the app reads, stores or sends | No manifest, dependency, network or storage change | security checks above | build-and-test | Met |
| TC-strategy | Testing Contract (minimal) | One test per requirement; happy path per component | 15 new tests covering FR1–FR5 and NFR1–NFR2; FR6/FR7 checked by review and spec tools | `cross-unit-traceability.md` | build-and-test | Met |
| FR7.3 | requirements.md FR7.3 | `validate-spec-assets.sh` exits 0 | passes | `test-all.sh` "Spec asset integrity" | build-and-test | Met |
| FR7.4 | requirements.md FR7.4 | Store graphics `--check` passes | passes | `test-all.sh` "Play store graphics" | build-and-test | Met |
| CI | Testing Contract | No CI-before-merge obligation for express | — | The plan runs no CI Pipeline | — | N/A |

Not automated: the manual check on a real phone with TalkBack and the largest font (`integration-test-instructions.md`). The Robolectric tests above cover the same targets. The phone check is recommended before the Play release.
