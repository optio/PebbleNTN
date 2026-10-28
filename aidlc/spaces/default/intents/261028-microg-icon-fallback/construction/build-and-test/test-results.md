# Test Results — MicroG arrows and user-added package names (v0.0.79)

| Check | Result |
|---|---|
| `scripts/test-all.sh`, rerun in this stage | exit 0, "All tests finished." |
| Android JVM tests (debug) | 589 tests, 4 skipped (opt-in store renderer), 0 failures (537 before this work) |
| Arrow fallback (FR1–FR2) | 4 in NavigationAppCatalogTest, 5 in IconResourcePackagesTest, 5 in AppIconReferencesTest |
| User-added packages (FR4–FR7) | CustomPackageValidatorTest (8), CustomPackagesRepositoryTest (5), EffectiveCatalogTest (13), AppPackagesUiTest (8 Compose), ExportBuilderTest (+3), DiagnosticExporterTest (+1) |
| Case-sensitive names (revision 2) | `com.baidu.BaiduMap` is stored with its capitals and matches; a name differing only in case is refused |
| `:app:assembleDebug`, spec assets, lint | OK; no new lint issues |
| Version | still 0.0.78; becomes 0.0.79 on merge |

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`:
- FR1, FR2 and FR4–FR7 are covered;
- FR3 (ship as v0.0.79) is reported uncovered on purpose, because it's delivered by the release at Deployment Execution.

## Not Covered

- **A real MicroG Maps install:** the arrows, and a user-added package on a device.
- **Android package visibility:** user-added packages outside the manifest may not be seen as installed, or have readable arrow images. Text rules still apply.
