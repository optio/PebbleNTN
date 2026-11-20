# Cross-Unit Traceability: Watch settings help text (#139)

Source: `aidlc engine sensor-traceability --stage build-and-test`.

**Verdict: PASS.** 28 of 28 IDs are covered and none are uncovered.

This is zero-Unit express work, so every ID is owned by the stage-level Code Generation. NFR4 was Deferred at Code Generation and is now verified here; its target points at `test-results.md`.

| ID | Covered | Owning stage | Target file |
|---|---|---|---|
| FR1.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreen.kt |
| FR1.2 | yes | code-generation | android/app/src/main/res/values/strings.xml |
| FR1.3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreen.kt |
| FR1.4 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt |
| FR2.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/OptionListScreen.kt |
| FR2.2 | yes | code-generation | android/app/src/main/res/values/strings.xml |
| FR2.3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt |
| FR3.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/OptionListScreen.kt |
| FR3.2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt |
| FR3.3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt |
| FR4.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt |
| FR4.2 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/DrivingSection.kt |
| FR4.3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/DrivingSection.kt |
| FR4.4 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt |
| FR5.1 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreen.kt |
| FR5.2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUiTest.kt |
| FR5.3 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt |
| FR6.1 | yes | code-generation | aidlc/spaces/default/intents/261120-watch-settings-help/construction/code-generation/code-summary.md |
| FR6.2 | yes | code-generation | android/app/src/main/res/values/strings.xml |
| FR7.1 | yes | code-generation | requirements/REQ-ANDROID.md |
| FR7.2 | yes | code-generation | spec/400-ui/AndroidUI.md |
| FR7.3 | yes | code-generation | MANIFEST.sha256.json |
| FR7.4 | yes | code-generation | tools/play-store-sources/phone/2-watch-settings.png |
| NFR1 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt |
| NFR2 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt |
| NFR3 | yes | code-generation | android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt |
| NFR4 | yes | build-and-test | aidlc/spaces/default/intents/261120-watch-settings-help/construction/build-and-test/test-results.md |
| NFR5 | yes | code-generation | android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt |

Uncovered elements: none.
