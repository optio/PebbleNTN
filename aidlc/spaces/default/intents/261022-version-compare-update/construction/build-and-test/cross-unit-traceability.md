# Cross-Unit Traceability — Version-compare update detection

## Verdict

PASS: all 26 requirement IDs are covered, and none is uncovered. FR3.5 is covered as not applicable: it is an accepted limitation with no code. This is zero-Unit work, so stage-level code-generation owns every element.

## Coverage

| ID | Covered | Owning stage | Target file |
|---|---|---|---|
| FR1.1 | yes | code-generation | watchapp/src/c/version_compare.c |
| FR1.2 | yes | code-generation | watchapp/tests/test_version_compare.c |
| FR1.3 | yes | code-generation | watchapp/tests/test_version_compare.c |
| FR1.4 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt |
| FR2.1 | yes | code-generation | watchapp/src/c/main.c |
| FR2.2 | yes | code-generation | watchapp/src/c/version_compare.c |
| FR2.3 | yes | code-generation | watchapp/src/c/main.c |
| FR2.4 | yes | code-generation | watchapp/src/c/main.c |
| FR3.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt |
| FR3.2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt |
| FR3.3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt |
| FR3.4 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt |
| FR3.5 | N/A | code-generation | accepted limitation: the Core Devices app never delivers WATCH_READY; FR2 covers it |
| FR4.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt |
| FR4.2 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt |
| FR5.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/dashboard/DashboardScreen.kt |
| FR5.2 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt |
| FR5.3 | yes | code-generation | android/app/src/main/res/values/strings.xml |
| FR5.4 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdateCardTest.kt |
| FR5.5 | yes | code-generation | android/app/src/main/res/values/strings.xml |
| FR6.1 | yes | code-generation | requirements/REQ-ANDROID.md |
| NFR1 | yes | code-generation | protocol/protocol-definition.json (unchanged) |
| NFR2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt |
| NFR3 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/update/UpdateCheckRepositoryTest.kt |
| NFR4 | yes | code-generation | construction/code-generation/emulator-basalt-1-notice.png |
| NFR5 | yes | code-generation | scripts/test-all.sh |

## Uncovered

None.
