# Cross-Unit Traceability: "When driving" (#132)

Source: `aidlc engine sensor-traceability --stage build-and-test` (zero-Unit express work; everything built in Code Generation).

**Verdict: PASS.** 39 requirement IDs traced, 0 uncovered. FR9 is traced through its sub-requirement FR9.1 and through `NavigationControllerTest` (relaunch after 2 minutes below the threshold).

| ID | Covered | Built in (Code Generation) |
|---|---|---|
| FR1.1 | yes | `android/app/src/main/java/com/pebblentn/app/core/WatchSettingsModel.kt` |
| FR1.2 | yes | `watchapp/src/c/settings_sync.c` |
| FR1.3 | yes | `watchapp/src/c/driving_label.h` |
| FR1.4 | yes | `android/app/src/test/java/com/pebblentn/app/core/WatchSettingsModelTest.kt` |
| FR1.5 | yes | `watchapp/tests/test_settings_sync.c` |
| FR2.1 | yes | `watchapp/src/c/settings_window.c` |
| FR2.2 | yes | `watchapp/src/c/settings_window.c` |
| FR2.3 | yes | `watchapp/tests/test_driving_label.c` |
| FR2.4 | yes | `watchapp/src/c/about_text.h` |
| FR3.1 | yes | `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/DrivingSection.kt` |
| FR3.2 | yes | `android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt` |
| FR3.3 | yes | `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt` |
| FR3.4 | yes | `android/app/src/main/res/values/strings.xml` |
| FR3.5 | yes | `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreen.kt` |
| FR4 | yes | `android/app/src/main/java/com/pebblentn/app/core/WatchSettingsSync.kt` |
| FR5.1 | yes | `android/app/src/main/java/com/pebblentn/app/core/DrivingSpeedEstimator.kt` |
| FR5.2 | yes | `android/app/src/test/java/com/pebblentn/app/core/DrivingSpeedEstimatorTest.kt` |
| FR5.3 | yes | `android/app/src/main/java/com/pebblentn/app/notification/DebugCaptureProcessor.kt` |
| FR5.4 | yes | `android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt` |
| FR6 | yes | `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` |
| FR7 | yes | `android/app/src/main/java/com/pebblentn/app/core/DrivingPolicy.kt` |
| FR8.1 | yes | `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt` |
| FR8.2 | yes | `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt` |
| FR8.3 | yes | `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` |
| FR8.4 | yes | `spec/200-architecture/Pebble.md` |
| FR9.1 | yes | `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` |
| FR10 | yes | `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt` |
| FR11 | yes | `android/app/src/test/java/com/pebblentn/app/core/DrivingPolicyTest.kt` |
| FR12 | yes | `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` |
| FR13 | yes | `android/app/src/main/java/com/pebblentn/app/ui/dashboard/DrivingStatusLine.kt` |
| FR14.1 | yes | `android/app/src/main/java/com/pebblentn/app/data/DrivingLogRepository.kt` |
| FR14.2 | yes | `android/app/src/main/java/com/pebblentn/app/export/ExportBuilder.kt` |
| FR14.3 | yes | `spec/300-data/ExportFormat.md` |
| NFR1 | yes | `android/app/src/main/java/com/pebblentn/app/core/DrivingSpeedEstimator.kt` |
| NFR2 | yes | `android/app/src/test/java/com/pebblentn/app/core/DrivingPolicyTest.kt` |
| NFR3 | yes | `android/app/src/main/java/com/pebblentn/app/core/DrivingPolicy.kt` |
| NFR4 | yes | `requirements/REQ-WATCH.md` |
| NFR5 | yes | `android/app/src/test/java/com/pebblentn/app/protocol/ProtocolCodecTest.kt` |
| NFR6 | yes | `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` |

Uncovered elements: none.
