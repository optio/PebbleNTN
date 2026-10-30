# Cross-Unit Traceability — Vela Maps support (#120)

## Verdict

FR1–FR5 and NFR1–NFR3 are covered by stage-level code-generation (zero-Unit work). FR5's fastlane listing was left out on purpose, as approved at the Code Generation gate; the v0.0.81 release happens at Deployment Execution.

## Coverage

| ID | Covered | Owning stage | Target file |
|---|---|---|---|
| FR1 | yes | code-generation | rules/catalog/navigation-apps.json |
| FR2 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/catalog/NavigationAppCatalog.kt |
| FR3 | yes | code-generation | rules/bundled/vela-maps/en.json |
| FR4 | yes | code-generation | tools/route-capture/routecap/apps.py |
| FR5 | yes | code-generation | README.md |
| NFR1 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/data/MicrogMapsEnablementTest.kt |
| NFR2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/apps/VelaMapsNavigationAppsTest.kt |
| NFR3 | yes | code-generation | rules/fixtures/vela-maps.json |

## Uncovered

None.
