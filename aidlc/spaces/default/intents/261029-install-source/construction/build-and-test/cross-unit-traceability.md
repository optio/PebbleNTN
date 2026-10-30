# Cross-Unit Traceability — install/update source (#119) and a second MicroG Maps package (#113)

## Verdict

FR1–FR5 and NFR1–NFR3 are covered by stage-level code-generation (zero-Unit work). The v0.0.80 release named in FR5 happens at Deployment Execution.

## Coverage

| ID | Covered | Owning stage | Target file |
|---|---|---|---|
| FR1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/update/InstallRecord.kt |
| FR2 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/dashboard/PhoneAppVersionRow.kt |
| FR3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/export/ExportBuilder.kt |
| FR4 | yes | code-generation | rules/catalog/navigation-apps.json |
| FR5 | yes | code-generation | requirements/REQ-DEBUG.md |
| NFR1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/update/InstallRecord.kt |
| NFR2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/update/InstallSourceTest.kt |
| NFR3 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/update/InstallRecordTest.kt |

## Uncovered

None.
