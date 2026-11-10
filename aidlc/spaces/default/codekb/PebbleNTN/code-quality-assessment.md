# Code Quality Assessment

## Summary

| Dimension | Rating | Evidence |
|---|---|---|
| Naming clarity | good | Domain names (`NavigationSessionReducer`, `stripLeadingDistance`, `KnownEta`); rule ids encode app, maneuver, variant and language |
| Function size | good (Kotlin), fair (watch C) | Engine and reducer functions are short; `watchapp/src/c/main.c` 1,479 lines, `settings_window.c` 1,113 |
| Error handling | good | Rule errors and regex timeouts become trace entries; malformed bundled files are skipped and logged; icon errors kept as diagnostics; strict parsing |
| Test presence | good | 107 JVM test files mirror the main packages; per-app rule regression; 13 host C tests; Python tool tests |
| Duplication | fair | Arrival-clock parsing in three places (Kotlin `arrivalMillis`, watch `eta_parse_clock_minutes`, rule regexes); OsmAnd `de` repeats the same `secondaryText` regex in 29 rules; two rule engines (Kotlin, Python) by design |
| Dead code | good | 0 TODO/FIXME/HACK; no commented-out production code (AGENTS.md rule 11) |

## Testing

- **JVM unit tests** (`android/app/src/test/`, 107 files): pure-logic tests plus Robolectric for Room, view models and Compose screens. Rule regressions: `GoogleMapsRulesRegressionTest`, `OsmandRulesRegressionTest`, `VelaMapsRulesRegressionTest`, `ComapsRulesRegressionTest`, `OrganicMapsRulesRegressionTest`, each loading `rules/bundled/<app>/*.json` and `rules/fixtures/<app>.json` from staged resources.
- **Fixtures**: 477 in total (counts per app in [component-inventory.md](component-inventory.md#rule-fixtures-and-regression)); `source` marks capture or synthetic; the fixtures README records lessons from real captures.
- **Python regression**: `workbench.py regression` runs the same fixtures through a subset engine, compares `maneuver`, `distanceMeters`, `primaryText`, `secondaryText`, `stopsRemaining`, `ruleId`, `noDistance`, `noPrimaryText`.
- **Watch**: host-compiled pure units (`eta_text`, `glance_text`, `stops_text`, `panel_layout`, ...); the full watchapp builds with the Pebble SDK in CI; emulator screenshots under `screenshots/`.
- **Instrumented**: one test (`ManualOpenWithAutoLaunchOffTest`); `scripts/test_manual_open_e2e.py`.
- **Coverage**: no JaCoCo/Kover configuration and no enforced floor.

Gaps relevant to #134:
- `OsmandRulesRegressionTest.Expected` has `secondaryText` but the test never asserts it (only `maneuver`, `distanceMeters`, `ruleId`, `primaryText`).
- Neither engine's fixture contract has an `etaEpochSeconds` expectation; Kotlin tests use `nowEpochSeconds = 0`; the Python engine returns no ETA.
- OsmAnd `bigText` is covered only by 10 German fixtures; no `bigText` fixture exists for the other 8 languages.
- `DurationParser` tests cover only English units (`DistanceDurationParserTest`).

## Linting and Static Analysis

- Android Lint on every Android CI run and in `test-all.sh` (default rules, no `lint.xml`, no baseline).
- CodeQL (java-kotlin) on push, PR and weekly.
- No ktlint/detekt/spotless; formatting follows `kotlin.code.style=official`.
- C: `-Wall -Wextra -Werror` for host units only.
- Python: `py_compile` in the pre-commit hook; no linter.
- Suppressions: 1 in app code (`@SuppressLint("DiscouragedApi")` in `AppIconReferences.kt` for `getIdentifier`), 3 in vendored PebbleKit; Python `noqa: E402` for path-adjusted imports.

## CI/CD

| Workflow | Trigger | Gates |
|---|---|---|
| `android.yml` | push/PR touching `android/**` | protocol constants current, `./gradlew test`, `lint`, `assembleDebug`, APK artifact |
| `rules.yml` | push/PR touching `rules/**`, `schemas/**`, `examples/**`, `scripts/validate*` | spec asset hashes, schema validation, Python rule regression |
| `watchapp.yml` | push/PR touching `watchapp/**` | protocol constants current, Pebble SDK build |
| `codeql.yml` | push/PR to main, weekly | CodeQL analysis |
| `release.yml` | every push to main (docs, tools, md ignored) or manual | verify job (`test lint`, rules, watch) then version bump, changelog, tag, GitHub release with APK + PBW |
| `play-release.yml` | manual with tag | target SDK floor check, signed AAB artifact |

Local: `.githooks/pre-commit` (secrets, spec hashes, protocol, version sync, rules schema + regression when rules change, syntax, Android tests + lint when `android/` changes); `scripts/test-all.sh` runs everything offline.

Gap: a PR that changes only `rules/` runs `rules.yml` (Python engine) but not `android.yml`, so the authoritative Kotlin regression runs on that change only in `release.yml` after merge, or locally.

## Documentation

- `README.md` (user-facing), `AGENTS.md` (18 mandatory rules), `spec/` and `requirements/` with SHA-256 protection in `MANIFEST.sha256.json`, `docs/IMPLEMENTATION_STATUS.md` (3,245 lines), privacy, Play and release docs.
- KDoc on nearly every class and on non-obvious lines, citing REQ ids and issues.
- `rules/fixtures/README.md` is a living log of capture lessons.
- Stale references: `rules/README.md` and Gradle `syncTestFixtures` description mention `rules/test-cases/`, which does not exist; `protocol/README.md` says "Placeholder in M0".

## Technical Debt

| # | Debt | Location | Impact |
|---|---|---|---|
| 1 | No rule can turn an arrival clock into `etaEpochSeconds`; only `duration` gives a now-relative epoch | `RuleEngine.assemble`, `ExtractorRunner` | Watch countdown and glance rely on string parsing; #134 needs a fix |
| 2 | `DurationParser` units are English only | `rules/DurationParser.kt` | Non-English hour or minute words (for example Dutch `uur`, Polish `godz.`, Chinese `小時`/`分鐘`, the last seen in a Google Maps zh fixture `1 分鐘 · 450 公尺 · …`) are not recognized; German `8 Min.` and `1 h 5 min` already parse |
| 3 | OsmAnd `secondaryText` means remaining distance in `de` and is absent in 8 languages | `rules/bundled/osmand/*.json` | Watch strip labels a distance as ETA in German |
| 4 | Kotlin OsmAnd regression does not assert `secondaryText` | `OsmandRulesRegressionTest.kt` | Python-only coverage of that output |
| 5 | Kotlin regression not triggered by rules-only PRs | `.github/workflows/android.yml` paths | Late detection of engine disagreement |
| 6 | Clock parsing duplicated | `DrivingSpeedEstimator`, `eta_text.c`, rule regexes | Divergent AM/PM and `p.m.` handling (`5:46 p.m.` in en-CA fixtures) |
| 7 | Large files | `watchapp/src/c/main.c`, `settings_window.c`, `DashboardScreen.kt`, `MainActivity.kt` | Harder review |
| 8 | Tracked bytecode | `scripts/__pycache__/changelog.cpython-312.pyc` | Noise; ignored pattern bypassed |
| 9 | No coverage measurement | Gradle config | Coverage floors in the org posture cannot be verified |
| 10 | Remote official rules implemented but disabled (no keys) | `AppContainer`, `update/RuleUpdateRepository.kt` | Dormant code path |

## Security and Privacy Notes

- Notification content read only after the allowlist check; the snapshot type cannot hold intents, actions or images (REQ-SEC-003).
- Regex limits: pattern ≤ 1024 characters, input ≤ 4096, 50 ms budget, ≤ 500 rules per package.
- No secrets in the repo; release signing from CI environment secrets; pre-commit scans staged diffs for keys.
- Keys and tags of notifications are stored hashed; exports have a privacy-safe mode.
