# Cross-Unit Traceability — MicroG arrows and user-added package names (v0.0.79)

## Verdict

FR1, FR2 and FR4–FR7 are covered by stage-level code-generation (zero-Unit work). FR3 is a release requirement, delivered at Deployment Execution.

## Coverage

| ID | Covered | Owning stage | Target file |
|---|---|---|---|
| FR1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/catalog/NavigationAppCatalog.kt |
| FR2 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/notification/icon/AppIconReferences.kt |
| FR3 | at deployment | deployment-execution | (release v0.0.79) |
| FR4 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/apps/NavigationAppsScreen.kt |
| FR5 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/catalog/CustomPackageValidator.kt |
| FR6 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/catalog/EffectiveCatalog.kt |
| FR7 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/export/ExportBuilder.kt |

## Uncovered

FR3 is pending until Deployment Execution, by design.
