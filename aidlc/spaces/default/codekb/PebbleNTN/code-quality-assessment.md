# Code Quality Assessment

## Test Coverage

| Area | Tests | Framework |
|---|---|---|
| Android app | 66 test sources under `android/app/src/test/` covering core, data (including the Room v1-v8 migration test), notification, icon, rules (per-app regression suites), export, protocol, pebble controller, system, ui view models, update | JUnit 4 + Robolectric + coroutines-test + Compose UI test |
| Android instrumented | `ManualOpenWithAutoLaunchOffTest` | AndroidJUnit4 |
| Watchapp | 7 host tests (about_text, early_warning, eta_text, glance_text, panel_layout, phone_version, stops_text) | plain C, `-Werror` |
| Rules | fixtures through `workbench.py regression` and Kotlin `*RulesRegressionTest` | Python + JUnit |
| Tools | `tools/route-capture/tests/test_routecap.py` | unittest |

Coverage measurement: **none configured** (no JaCoCo or Kover, no gcov). Untested by design: `PebbleWatchTransport` (needs a bound companion) and the Pebble API parts of `main.c`, `settings_window.c` and `about_window.c`, which are verified in the emulator per the REQ acceptance criteria. `UpdateCheckRepository` has 7 Robolectric tests (throttle, force, failure retention, persistence, auto-check default off). No test covers last-check staleness, because the repository does not expose it.

## Linting and Static Analysis

- Android Lint (`./gradlew lint`) in CI, pre-commit and release verify; default configuration with no baseline file.
- CodeQL java-kotlin on push, PR and weekly.
- No ktlint, detekt or formatter check. C warnings are enforced only in host tests.

## CI/CD

| Workflow | Trigger | Gates |
|---|---|---|
| android.yml | push/PR on `android/**` | protocol `--check`, `test`, `lint`, `assembleDebug`, APK artifact |
| watchapp.yml | push/PR on `watchapp/**` | protocol `--check`, `pebble build`, PBW artifact |
| rules.yml | push/PR on rules, schemas, examples | spec-asset integrity, schema validation, rule regression |
| codeql.yml | push/PR on main, weekly | CodeQL analysis |
| release.yml | push to main (not docs/tools/md) or manual | verify job (spec assets, protocol, rules, version sync, tests, lint), then version bump, changelog, signed or debug APK, PBW, SHA256SUMS, tag `v<version>`, GitHub release |
| play-release.yml | manual | signed AAB from a protected environment |

`.githooks/pre-commit` runs secrets/keystore scanning, MANIFEST integrity, protocol `--check`, version sync, rules, syntax checks and Android tests + lint. Watchapp host tests run only in `scripts/test-all.sh`, not in any CI workflow.

## Documentation Quality

Good. There is a requirement-ID discipline (REQ-* with acceptance criteria), a MANIFEST-protected spec set, KDoc on public Kotlin APIs, a rationale header on every C module, an implementation status log per change, and human build guides (`docs/FOR-HUMAN.md`).

## Quick Ratings

| Criterion | Rating | Evidence |
|---|---|---|
| Naming clarity | good | descriptive Kotlin and C names, REQ cross-references |
| Function size | fair | most functions are small; `panel_update_proc`, `strip_update_proc` and `DashboardScreen` are long |
| Error handling | good | best-effort network returns null, transport failures are caught and logged, persist errors are logged; watch tuple types are not validated in `render_navigation` |
| Test presence | good | pure logic is tested on both devices; Pebble UI is emulator-verified only |
| Duplication | fair | three near-identical switch tables in `maneuver_resource_id`; repeated SharedPreferences repository boilerplate |
| Dead code | good | none found; datastore in `:app` is the one unused declared dependency |

## Technical Debt

- `watchapp/src/c/main.c` (1,124 lines) mixes decoding, rendering, timers and glance. `settings_window.c` is 741 lines.
- Tuple type checks are missing in `render_navigation` and `handle_stopped` (CodingStandards: "Validate every tuple and type").
- Phone-wide flags have no single application point: `NoActiveNavigation` is sent with `flags = 0`, and compatibility errors carry no flags.
- `PebbleNtnDatabase` KDoc is stale (lists v1-v5; the schema is v8).
- `scripts/__pycache__/changelog.cpython-312.pyc` is committed against `.gitignore`.
- `rules/test-cases/` is documented but has no tracked content.
- `play-release.yml` swallows the target-SDK gate failure (`|| echo "TODO(M10) ..."`).
- No coverage tooling, so the org-default 80% floor (non-express scopes) cannot be measured.
- Pebble SDK version unpinned in CI (`pebble sdk install latest`).
- A committed binary `.dex` helper sits in `tools/route-capture/device/`.
