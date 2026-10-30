# Test Results — install/update source (#119) and a second MicroG Maps package (#113)

| Check | Result |
|---|---|
| `scripts/test-all.sh`, rerun in this stage | exit 0 |
| Android JVM tests (debug) | 611 tests, 4 skipped (opt-in store renderer), 0 failures (589 before this work) |
| Install record (FR1) | InstallRecordTest (7) |
| Row (FR2) | PhoneAppVersionRowTest (5), DashboardUpdatesScreenTest (2 updated, 2 added) |
| Export (FR3) | ExportBuilderTest (+3, schema 5), DiagnosticExporterTest (+2) |
| MicroG package (FR4) | NavigationAppCatalogTest, RuleFamiliesMatchingTest, EffectiveCatalogTest, MicrogMapsEnablementTest updated; `test_validate_catalog.py` 27 packages |
| `validate-rules.sh`, `validate-spec-assets.sh` | OK; manifest `<queries>` covers all 27 catalog packages; 34 spec files match |
| `:app:assembleDebug`, lint | OK; no new lint issues |

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: FR1–FR5 and NFR1–NFR3 are covered, with nothing uncovered.

## Not Covered

- **On a device:** the row and the export. What the F-Droid privileged extension and other stores report as installer is still unverified.
- **The ungoogled MicroG Maps build itself.**
