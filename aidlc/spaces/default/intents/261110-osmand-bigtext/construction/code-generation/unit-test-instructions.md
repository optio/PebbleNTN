# Unit Test Instructions: #134

## Framework and setup

- Phone: JUnit 4 + kotlinx.serialization under `android/app/src/test/`, Gradle (JDK 21, Android SDK as in the build environment). Fixtures and bundled rules are test resources (`/fixtures/*.json`, `/rules/bundled/...`).
- Rule workbench: Python 3 standard library, `tools/rule-workbench/workbench.py` (run with `python3 -I`).
- Watchapp: the C unit tests under `watchapp/tests/` (host compiler, as `test-all.sh` runs them).

## Commands (scoped to this change)

Run from `android/`:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.pebblentn.app.rules.ArrivalClockTest' \
  --tests 'com.pebblentn.app.rules.RuleEngineTest' \
  --tests 'com.pebblentn.app.rules.ExtractorRunnerTest' \
  --tests 'com.pebblentn.app.rules.RulesetCodecTest' \
  --tests 'com.pebblentn.app.rules.RuleValidatorTest' \
  --tests 'com.pebblentn.app.rules.OsmandRulesRegressionTest' \
  --tests 'com.pebblentn.app.rules.GoogleMapsRulesRegressionTest' \
  --tests 'com.pebblentn.app.rules.VelaMapsRulesRegressionTest' \
  --tests 'com.pebblentn.app.rules.RulesetVersionsTest' \
  --tests 'com.pebblentn.app.core.DrivingSpeedEstimatorTest' \
  --tests 'com.pebblentn.app.core.WatchSettings*' \
  --tests 'com.pebblentn.app.data.WatchSettingsMirror*' \
  --tests 'com.pebblentn.app.protocol.ProtocolCodecTest' \
  --tests 'com.pebblentn.app.pebble.NavigationControllerTest'
```

Rule workbench fixtures (from the repo root; use the exact sub-command the workbench README documents for running all fixtures):

```bash
python3 -I tools/rule-workbench/workbench.py test rules/fixtures/osmand.json rules/fixtures/google-maps.json rules/fixtures/vela-maps.json
```

Watchapp settings tests: the `watchapp/tests/` target that `test-all.sh` builds for `test_settings_sync` and the theme value names.

Baseline before any change and final check: `./test-all.sh` (whole suite, run once at Step 1 and once at Step 14, not per step).

## New and extended tests (Minimal strategy: one per requirement, happy path per component)

| Test | Covers |
|---|---|
| `ArrivalClockTest` (new): later today, next day, 12 h PM, 12 AM, unreadable, Europe/Brussels incl. DST day, whole minutes | FR3.1–FR3.3, FR4.3 |
| `RuleEngineTest`: compactText assembled; arrivalClock ETA with fixed now/zone; unreadable clock → null ETA, rule still matches | FR3.1, FR3.2, FR1 |
| `ExtractorRunnerTest`: arrivalClock with/without pattern | FR3.2 |
| `RulesetCodecTest`, `RuleValidatorTest`: new output and extractor round-trip and validate | FR3.5 |
| `OsmandRulesRegressionTest` + fixtures (9 languages: street turn, continue, summary-only, no street, roundabout) | FR1.1–FR1.5, FR2.1–FR2.3, FR3.4, FR4.1, FR4.2 |
| `GoogleMapsRulesRegressionTest`, `VelaMapsRulesRegressionTest` + fixtures: compactText, ETA; primaryText unchanged | FR2.4, FR3.4, FR4.4, FR7.5, NFR7 |
| Byte-fit check over fixtures (both forms) | NFR1 |
| `ProtocolCodecTest`: Full / Compact / Compact fallback | FR7.1 |
| Settings model / mirror: key 44, pre-1.5 READY keeps phone value | FR7.2, FR7.4 |
| `NavigationControllerTest`: re-send on Directions text change, none on unrelated change, one send for identical cards, defaults unchanged | FR7.3, NFR2, NFR7 |
| Watch `test_settings_sync` / theme names | FR7.2 |
| Workbench fixture run | FR4.2 (Python engine) |

## Coverage target

No line-coverage floor for express; every requirement has at least one test above and the existing suite stays green (NFR5).

## Mocking and test data

- Time: tests pass `nowEpochSeconds` and a fixed `ZoneId` to `RuleEngine`; fixtures carry `nowEpochSeconds` and `timeZone`. No test reads the system clock or zone.
- Fixtures are synthetic: made-up streets (e.g. `Voorbeeldstraat`, `Beispielweg`, `Example Street`) and times. Nothing from the private diagnostics export.
- `NavigationControllerTest` uses the existing `FakeWatchTransport` and settings mirror fakes.

## Baseline

Recorded at Step 1 (test counts and pass/fail of `./test-all.sh` on `main` at `2afb066`).

- `./scripts/test-all.sh` (the repo's full suite; there is no root `test-all.sh`): exit 0, "All tests finished."
- Android `:app:testDebugUnitTest`: 703 tests, 0 failures, 0 errors, 4 skipped (`:app:testReleaseUnitTest` and `lint` also green).
- Rule workbench `python3 tools/rule-workbench/workbench.py regression` (the documented all-fixtures command): google-maps 189/189, comaps 21/21, organic-maps 12/12, osmand 195/195, vela-maps 60/60.
- Watchapp `./scripts/test-watchapp-unit.sh`: 13 test programs, all OK.
