# Unit Test Instructions — install/update source (#119) and a second MicroG Maps package (#113)

## Framework and Setup

JUnit 4 JVM tests through Gradle (JDK 21, `source ~/.local/pebblentn-env.sh`), and Robolectric Compose tests for the dashboard. The classification and row text are pure functions over plain values, so no `PackageManager` shadowing is needed.

## How to Run This Work's Tests

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.update.*' --tests 'com.pebblentn.app.export.*' --tests 'com.pebblentn.app.ui.dashboard.*' --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.rules.*'
scripts/validate-rules.sh
scripts/validate-spec-assets.sh
scripts/test-all.sh
```

## Tests

| Test | Cases | Requirement |
|---|---|---|
| `InstallRecords.classify` / `from` | Play; each F-Droid client; Obtainium and a suffixed Obtainium; a Play look-alike → other; null, both package installers, the shell → manual; `com.aurora.store` → other with its name; `updatedSinceInstall` equal / differing; failed lookup → unknown with nulls | FR1 |
| `phoneAppVersionText` | each kind × first install / update; other shows the package; unknown shows only the version | FR2 |
| `DashboardUpdatesScreenTest` (updated) | the row on Play; on GitHub; on GitHub while the stale card shows; the check row no longer shows "Version …" | FR2 |
| `ExportBuilderTest` / `DiagnosticExporterTest` | the install section in all three modes; nulls for unknown; `build` and `buildCapped` both carry it; schema 5 | FR3 |
| Catalog / families / custom packages | the second package belongs to MicroG Maps; root is Google Maps; a notification from it matches a Google Maps rule; refused as a user package; hidden on Play | FR4 |
| `validate-rules.sh`, `validate-spec-assets.sh` | catalog valid; MANIFEST hashes match | FR4, FR5 |
| Existing update-card, policy and source tests | unchanged and green | NFR2 |

## Coverage Target

Minimal strategy: one test per requirement, a happy path per component, and at least two edge cases for each new function.

## Mocking and Test Data

Plain values for the install record; the real bundled catalog for the MicroG tests; the existing fake exporter inputs.
