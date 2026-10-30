# Unit Test Instructions — Vela Maps support (#120)

## Framework and Setup

JUnit 4 JVM tests through Gradle (JDK 21, `source ~/.local/pebblentn-env.sh`), the Python rule and catalog validators, the rule workbench regression, and the route-capture harness's own Python tests. No device or emulator in this stage.

## How to Run This Work's Tests

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.rules.*' --tests 'com.pebblentn.app.ui.*'
scripts/validate-rules.sh
scripts/validate-spec-assets.sh
python3 -m pytest tools/route-capture/tests   # or the command in tools/route-capture/README.md
scripts/test-all.sh
```

## Tests

| Test | Cases | Requirement |
|---|---|---|
| Catalog tests, `validate_catalog.py`, `test_validate_catalog.py` | `vela-maps` entry, `app.vela` in `<queries>`, 28 packages, rule-folder/appId lock | FR1 |
| Play view (`NavigationAppCatalogTest` or a new test) | on Play: Vela hidden, no website link, still matched and allowlisted; outside Play: visible with website; MicroG Maps unchanged | FR2 |
| `VelaMapsRulesRegressionTest` + `rules/fixtures/vela-maps.json` | turns, slight/sharp, bear, U-turn left/right, roundabout (incl. "At the roundabout, turn right"), exit left/right (incl. "Take the I 35 South exit"), head/continue, bare-instruction titles, arrival (3 wordings), generic fallback, ft/mi and m/km, 12/24-hour ETA, "Navigating" unmatched | FR3 |
| Route-capture harness tests | the Vela driver (package, release asset pattern skips `canary`, start intent), the US route and en-US scenario load, Vela's plan includes it | FR4 |
| `validate-spec-assets.sh` | MANIFEST hashes match | FR5 |
| Existing suites | unchanged and green | NFR1 |

## Coverage Target

Minimal strategy: one test per requirement, a happy path per component, and at least two edge cases for each new function or rule group.

## Mocking and Test Data

Synthetic Vela notifications based on Vela's source format and typical routing-engine wording; the real bundled catalog; no network in tests.
