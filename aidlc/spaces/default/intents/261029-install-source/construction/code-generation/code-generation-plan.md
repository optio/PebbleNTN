# Code Generation Plan — install/update source (#119) and a second MicroG Maps package (#113)

## Summary

- **Builds:**
  - a once-per-start install record next to the existing `InstallSource`;
  - a "Phone app version" row for every source;
  - an `install` section in every debug export (schema 5);
  - `org.ungoogled.android.apps.maps.microg` as a second built-in MicroG Maps package.
- **Touches:** `update/InstallSource.kt` (or a new `update/InstallRecord.kt`), `di/AppContainer.kt`, `ui/MainActivity.kt`, `ui/dashboard/DashboardScreen.kt` (plus a pure row-text helper), `export/ExportBuilder.kt`, `export/DiagnosticExporter.kt`, `res/values/strings.xml`, `rules/catalog/navigation-apps.json`, `AndroidManifest.xml`, the requirements, spec and privacy docs, `MANIFEST.sha256.json`, `docs/IMPLEMENTATION_STATUS.md`, and their tests.
- **Tests:** JVM tests for the classification, the row text, the export section and the catalog; updated dashboard Compose tests; the full `scripts/test-all.sh`.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR5, NFR1–NFR3). The facts are in `inception/reverse-engineering/developer-scan.md`. The release (v0.0.80) happens at Deployment.

### Design decisions

**Install record (FR1):**
- `data class InstallRecord(kind, installingPackage, initiatingPackage, originatingPackage, firstInstallTime: Long?, lastUpdateTime: Long?)`, with `updatedSinceInstall: Boolean?` derived from the two times.
- `enum class InstallKind { PLAY, FDROID, OBTAINIUM, MANUAL, OTHER, UNKNOWN }`, exported in lower case.
- A pure `InstallRecords.classify(installingPackage)`:
  - reuses the existing package sets for Play, F-Droid and Obtainium;
  - `MANUAL` for null, `com.google.android.packageinstaller`, `com.android.packageinstaller` and `com.android.shell`;
  - `OTHER` for everything else.
- A pure `InstallRecords.from(...)` builds the record from plain values; `detect(context)` reads `getInstallSourceInfo` and `getPackageInfo(packageName, 0)` in `runCatching`. A failure gives `UNKNOWN` with null fields and is logged with Timber.
- `AppContainer` computes the record once (`installRecord`). The existing `installSource` stays as it is, so update destinations don't change.

**Row (FR2):**
- A pure `phoneAppVersionText(version, record)` returns a string-resource id plus arguments:
  - `installed by %2$s` / `updated by %2$s` with the source name (Google Play, F-Droid, Obtainium) or the raw package for `OTHER`;
  - `installed manually` / `updated manually`;
  - the bare version for `UNKNOWN`.
- `DashboardScreen` gets an `installRecord` parameter (defaulted), and the Updates section starts with `InfoRow(dashboard_app_version, …)` for every source, the stale state included.
- The "Check for updates now" row's summary changes from "Version %1$s" to a short hint resource ("Compare with the latest GitHub release").
- The Play-only string is replaced by the generic strings.

**Export (FR3):**
- `ExportMetadata.install: ExportInstall` with the seven fields: times as ISO 8601 UTC strings, nulls written as `null`.
- `SCHEMA_VERSION = 5`, with a KDoc history line.
- `DiagnosticExporter` takes `installRecord: () -> InstallRecord` and passes it to both `build` and `buildCapped`.

**MicroG package (FR4):**
- Append `org.ungoogled.android.apps.maps.microg` to the `microg-maps` entry's `packageNames`, add a `<package>` in the manifest `<queries>`, and check the docs that list MicroG Maps' package (README, rules README).

## Steps

- [x] 1. Add an entry to `docs/IMPLEMENTATION_STATUS.md`.
- [x] 2. Baseline: `android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.update.*' --tests 'com.pebblentn.app.export.*' --tests 'com.pebblentn.app.ui.dashboard.*' --tests 'com.pebblentn.app.catalog.*'`.
- [x] 3. FR1: `InstallRecord`, `InstallKind`, `classify`, `from` and `detect`; wire `installRecord` in `AppContainer`. Tests:
  - each Play, F-Droid and Obtainium package (an Obtainium suffix included);
  - a look-alike of `com.android.vending` → OTHER;
  - null, both package installers and the shell → MANUAL;
  - `com.aurora.store` → OTHER with its name;
  - `updatedSinceInstall` for equal and differing times;
  - a failed lookup → UNKNOWN with nulls.
- [x] 4. FR2: strings, `phoneAppVersionText`, the row in `DashboardScreen`, `MainActivity` wiring. Tests:
  - the text for each kind, first install and update, and UNKNOWN;
  - update `DashboardUpdatesScreenTest`: the row on Play, on GitHub, and on GitHub while the stale card is shown; the check row no longer says "Version …".
- [x] 5. FR3: `ExportInstall`, schema 5, `DiagnosticExporter` wiring in both paths, `AppContainer`. Tests:
  - the section in each of the three modes;
  - the nulls for an UNKNOWN record;
  - `build` and `buildCapped` both carry it;
  - the schema assertion goes to 5.
- [x] 6. FR4: the catalog entry and the manifest `<queries>` entry; check the docs naming MicroG Maps' package. Tests:
  - the package belongs to `microg-maps`, and its family root is `com.google.android.apps.maps`;
  - a notification from it matches a Google Maps rule;
  - adding it as a user package is refused as a duplicate;
  - Play hides it.
  Run `scripts/validate-rules.sh` (the catalog validator).
- [x] 7. FR5:
  - amend REQ-ANDROID-020 (classification and row), REQ-DEBUG-014 (or add REQ-DEBUG-015) for the install section and schema 5, and REQ-ANDROID-023 (the second package);
  - add the "Install (schema 5)" section to `spec/300-data/ExportFormat.md` and fix its "(2)" wording;
  - add the Updates row to `spec/400-ui/AndroidUI.md` if it lists the Updates controls;
  - add a privacy-docs line about the export's install section (`docs/PRIVACY_POLICY.md`, `docs/PLAY_DATA_SAFETY.md` where they list export contents);
  - resync `MANIFEST.sha256.json` for every protected file changed, and run `scripts/validate-spec-assets.sh`.
- [x] 8. Run the full `scripts/test-all.sh` and `:app:assembleDebug`.
- [x] 9. Write `code-summary.md` and `traceability.json` (FR1–FR5 and NFR1–NFR3). Don't commit.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "express",
  "test_strategy": "minimal",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "minimal",
    "strategy_volume": [
      "One verifiable test per requirement at the narrowest effective level.",
      "At least one happy-path unit test per component.",
      "Unit tests are the default; a bugfix/security scope floor may require an integration or E2E regression when that is the narrowest level that reproduces the defect."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:c8f84e35190e007d9863fad236154d0269ab9bf633fc2a3ff106ed965ba4a9b9",
  "contract_sha256": "sha256:bb76c1b22f8307be6e5780d50cabab891684449d5b164bd0e825e9e4e5c61e5b"
}
```
