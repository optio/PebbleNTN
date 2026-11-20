# Code Quality Assessment

## Tests

| Area | Tests | Framework |
|---|---|---|
| Android JVM | 106 files, 753 `@Test` (4 skipped by assumption, incl. store screenshots) | JUnit 4, Robolectric, Compose UI test |
| Android instrumented | 1 (`ManualOpenWithAutoLaunchOffTest`) | AndroidX test |
| Watch | 13 host C programs | plain asserts via `scripts/test-watchapp-unit.sh` |
| Rules | Workbench regression over 5 fixture sets; Kotlin `*RulesRegressionTest` | Python, JUnit |
| Tools | `test_validate_catalog.py`, `test_routecap.py`, `test_play_store_graphics.py`, `test_manual_open_e2e.py` | unittest |

- **Coverage**: no coverage tool or floor configured.
- **Watch settings**: `WatchSettingsUiTest` (12 pure tests) and `WatchSettingsScreenTest` (14 Robolectric tests) cover states, capability-hidden rows, early warning rows, option lists, previews, Directions text note, When driving section. Screen tests locate nodes by exact English text, so copy changes require test edits.
- **Single entry**: `scripts/test-all.sh` runs spec/protocol/version/rule checks, workbench regression, Python tests, workflow YAML parse, store graphics check, watch host tests and `gradlew test lint`.

## Linting and Static Analysis

- Android Lint (`./gradlew lint`) with defaults; no `lint.xml`, no baseline. `kotlin.code.style=official`. No ktlint or detekt.
- CodeQL java-kotlin on PRs, main and weekly.
- Protocol constants `--check`; spec asset hash check; rule schema validation.

## CI/CD

| Workflow | Trigger | Does |
|---|---|---|
| `android.yml` | push main / PR on `android/**` | protocol check, test, lint, assembleDebug |
| `watchapp.yml` | push main / PR on watch paths | protocol check, pebble build |
| `rules.yml` | push main / PR on rule paths | spec assets, rule schemas, workbench regression |
| `codeql.yml` | push / PR / weekly | CodeQL |
| `release.yml` | push main (ignores `**/*.md`, `docs/**`, `fdroid/**`, `tools/**`; `[skip ci]` skips) / manual | spec assets, protocol, rules, version sync, test + lint, version bump, changelog, signed APK + pbw, GitHub release, F-Droid changelog |
| `play-release.yml` | manual | signed AAB for Play |

Pre-commit hook (`.githooks/pre-commit`): secrets, spec hashes, protocol, version sync, rules, syntax, Gradle test + lint when `android/` changes.

## Documentation

Good: README, AGENTS.md contract, 20 spec files, 5 requirement files with REQ ids, IMPLEMENTATION_STATUS per change, privacy and store docs. KDoc on almost every Kotlin declaration referencing REQ ids and issues. 34 spec files are hash-protected (`MANIFEST.sha256.json`).

## Quick Ratings

| Criterion | Rating | Note |
|---|---|---|
| Naming clarity | good | Domain names, KDoc everywhere |
| Function size | fair | Mostly small; large Compose screens and C windows |
| Error handling | good | Explicit results, retries at transport, validation at rule boundary |
| Test presence | good | Every component has tests; no coverage figure |
| Duplication | fair | Watch C and phone strings duplicate setting names; sync rule implemented on both sides by design |
| Dead code | good | 0 TODO/FIXME/HACK; one deliberate `@SuppressLint` |

## Technical Debt

- No coverage measurement or floor.
- English-only UI strings (`values/` only); no translation workflow.
- Large files: `watchapp/src/c/main.c` (1,479), `settings_window.c` (1,120), `ui/dashboard/DashboardScreen.kt` (673), `ui/MainActivity.kt` (586), `pebble/NavigationController.kt` (518).
- Tracked bytecode file `scripts/__pycache__/changelog.cpython-312.pyc` despite the ignore rule.
- Compose BOM and AndroidX pins about a year old.
- Watch settings: no per-setting help on phone or watch (issue #139, phone side); text-based UI tests are brittle to copy edits.
