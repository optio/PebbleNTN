# Test Results — MicroG Maps as a Google Maps variant (#113)

## Build

`:app:assembleDebug` succeeds, and the APK contains the `microg-maps` catalog entry. The version is unchanged at 0.0.77.

## Unit Tests (this work)

| Suite | Result |
|---|---|
| NavigationAppCatalogTest (21, 11 new) | pass |
| RuleFamiliesMatchingTest (12, new) | pass: every Google Maps fixture gives an identical result as MicroG |
| MicrogMapsEnablementTest (5, new) | pass |
| MicrogMapsNavigationAppsTest (5, new) | pass |
| SetupProgressTest (updated) | pass |
| `scripts/test_validate_catalog.py` (10, new) | pass |

## Full Suite

`scripts/test-all.sh`, rerun in this stage: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Android JVM tests, debug and release | 537 each: 533 passed, 4 skipped (the opt-in store renderer), 0 failed |
| Catalog | "AndroidManifest `<queries>` covers all 26 catalog packages" |
| Rule regressions | google-maps 195/195 (4 MicroG fixtures) |
| Spec assets, lint, workflows, store images | OK |

## Unchanged by Design

- No file in `rules/bundled/` changed (`git diff --stat` empty).
- `fastlane/.../full_description.txt` is unchanged.

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: pass, FR1–FR4.

## Not Covered

A real MicroG Maps install: no device has it, and the WSL emulator can't run. The icon-only card's turn arrow under the renamed package is unverified. The maintainer follows up on #113.
