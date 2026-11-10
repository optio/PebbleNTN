# Cross-Unit Traceability: #134 (+ #136)

Source: `aidlc engine sensor-traceability --stage build-and-test`. This is an express (zero-Unit) run, so every ID is built in the code-generation stage.

**Verdict: FAIL, 1 uncovered (FR6.1).** The failure was accepted by the person at the halt-and-ask (see `test-results.md`, "Accepted failure").

| ID | Covered | Owning stage | Target |
|---|---|---|---|
| FR1.1 | yes | code-generation | `rules/bundled/osmand/en.json` (and the other 8 languages) |
| FR1.2 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/core/NavigationInstruction.kt` |
| FR1.3 | yes | code-generation | `rules/fixtures/osmand.json` |
| FR1.4 | yes | code-generation | `rules/bundled/osmand/de.json` |
| FR1.4a | yes | code-generation | `rules/fixtures/osmand.json` |
| FR1.5 | yes | code-generation | `construction/code-generation/osmand-strings.md` |
| FR2.1 | yes | code-generation | `rules/bundled/osmand/fr.json` |
| FR2.2 | yes | code-generation | `rules/bundled/osmand/de.json` |
| FR2.3 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/OsmandRulesRegressionTest.kt` |
| FR2.4 | yes | code-generation | `rules/fixtures/google-maps.json` |
| FR3.1 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/rules/ArrivalClock.kt` |
| FR3.2 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/rules/RuleEngine.kt` |
| FR3.3 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/ArrivalClockTest.kt` |
| FR3.4 | yes | code-generation | `rules/bundled/google-maps/en.json` |
| FR3.5 | yes | code-generation | `schemas/ruleset.schema.json` |
| FR4.1 | yes | code-generation | `rules/fixtures/osmand.json` |
| FR4.2 | yes | code-generation | `tools/rule-workbench/workbench.py` |
| FR4.3 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/ArrivalClockTest.kt` |
| FR4.4 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/GoogleMapsRulesRegressionTest.kt` |
| FR5.1 | yes | build-and-test | `construction/build-and-test/test-results.md` (OsmAnd en/fr emulator run) |
| FR5.2 | yes | code-generation | `rules/fixtures/README.md` |
| FR6.1 | **no** | code-generation | `android/app/build.gradle.kts`: the line is present, but the bundle has no debug symbols (stripped prebuilt libraries); accepted failure |
| FR6.2 | yes | code-generation | `android/app/build.gradle.kts` (APK byte-identical) |
| FR7.1 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt` |
| FR7.2 | yes | code-generation | `watchapp/src/c/settings_sync.c` |
| FR7.3 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt` |
| FR7.4 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/core/WatchSettingsSync.kt` |
| FR7.5 | yes | code-generation | `tools/rule-workbench/gen_localized_google_maps.py` |
| FR7.6 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/export/ExportBuilder.kt` |
| NFR1 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/FixtureTextFitTest.kt` |
| NFR2 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` |
| NFR3 | yes | code-generation | `android/app/src/main/java/com/pebblentn/app/rules/RuleValidator.kt` |
| NFR4 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/RulesetVersionsTest.kt` |
| NFR5 | yes | code-generation | `scripts/test-all.sh` |
| NFR6 | yes | code-generation | `rules/fixtures/README.md` |
| NFR7 | yes | code-generation | `android/app/src/test/java/com/pebblentn/app/rules/GoogleMapsRulesRegressionTest.kt` |

**Uncovered:** FR6.1. This finding is surfaced at the approval gate.
