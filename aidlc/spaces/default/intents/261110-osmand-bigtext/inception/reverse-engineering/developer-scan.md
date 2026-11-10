# Developer Code Scan: PebbleNTN (full rescan)

Commit scanned: `2afb066a6453db76d188fd00b51f4fbdd9d2d930` (v0.0.82). Intent: `osmand-bigtext` (GitHub #134).

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - `android/app/src/main/java/com/pebblentn/app/rules/` (all 12 files: model, engine, extractors, parsers, safe regex, validator, codec, repository, preview)
  - `android/app/src/main/java/com/pebblentn/app/notification/` (snapshot, snapshot factory, dispatcher, capture processor, listener service)
  - `android/app/src/main/java/com/pebblentn/app/core/NavigationInstruction.kt`, `Maneuver.kt`, `NavigationSessionReducer.kt`, `NavigationState.kt`, `DistanceQuantizer.kt`, `DrivingSpeedEstimator.kt` (arrival-clock parsing)
  - `android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt` (encode path), `di/AppContainer.kt` (wiring)
  - `rules/bundled/` (all 22 rulesets summarized by rule, outputs and extractor types; `osmand/en.json` and `osmand/de.json` read rule by rule)
  - `rules/fixtures/README.md`, `rules/fixtures/osmand.json` (structure, coverage by locale/source/bigText), `rules/fixtures/google-maps.json` (subText ETA formats per locale)
  - `rules/catalog/navigation-apps.json`, `rules/README.md`, `schemas/ruleset.schema.json` (extractor enum)
  - `android/app/src/test/java/com/pebblentn/app/rules/OsmandRulesRegressionTest.kt`, `GoogleMapsRulesRegressionTest.kt` (harness)
  - `tools/rule-workbench/workbench.py` (Python engine subset, regression), `tools/rule-workbench/gen_localized_google_maps.py` (generator scope)
  - `scripts/validate_rules.py`, `scripts/test-all.sh`, `scripts/test-watchapp-unit.sh`, `.githooks/pre-commit`, `.github/workflows/*.yml` (triggers and steps)
  - `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `android/settings.gradle.kts`, `android/build.gradle.kts`, wrapper properties
  - `watchapp/src/c/eta_text.c`, ETA strip and message handling in `watchapp/src/c/main.c`, `watchapp/package.json`
  - `protocol/protocol-definition.json`, `MANIFEST.sha256.json` (protected files), `AGENTS.md`, `requirements/REQ-RULES.md`, `spec/200-architecture/RuleEngine.md`, REQ-WATCH-014 and REQ-ANDROID-014
- **Skimmed only**:
  - Rest of `android/app/src/main/java/com/pebblentn/app/` (catalog, data, data/db, export, update, system, ui, pebble, notification/icon, remaining core): first KDoc line and declarations of every file, line counts
  - `android/app/src/test/` beyond the rules harnesses (file list), `android/app/src/androidTest/`, `android/app/schemas/room/`
  - `android/pebblekit/` (vendored PebbleKit 2: file list and README), `android/fixture-publisher/` (README)
  - `watchapp/src/c/` other units and `watchapp/tests/` (file list, line counts, ETA grep), `watchapp/tools/`, `watchapp/resources/`
  - `tools/route-capture/` (README and file list), `tools/play_store_graphics.py`, `tools/play-store-sources/`
  - `spec/`, `requirements/`, `docs/` (headings and sizes), `README.md` head, `examples/`, `fastlane/`, `fdroid/`, `screenshots/`
- **Left out**:
  - `android/app/build/`, `android/*/build/`, `android/.gradle/` (build output and Gradle cache)
  - `watchapp/build/` (Pebble build output, gitignored)
  - `tools/route-capture/out/` (run output, gitignored), `tools/route-capture/routes/cache/` contents (fetched route tracks)
  - `__pycache__/` folders (Python caches)
  - AI-DLC install and workspace: `.claude/`, `aidlc/`, `.mcp.json`, `aidlc.settings.json`, AI-DLC block of `.gitignore`

### Packages Found
- `android/app` (`com.pebblentn.app`) — Android application — Kotlin — companion app: notification listener, rule engine, session reducer, PebbleKit transport, Compose UI, Room history
- `android/pebblekit` (`io.rebble.pebblekit2`) — Android library (vendored) — Kotlin + AIDL — PebbleKit Android 2 client
- `android/fixture-publisher` (`com.pebblentn.fixturepublisher`) — debug Android app — Kotlin — posts synthetic navigation notifications
- `watchapp` — Pebble watchapp (SDK 3) — C — renders maneuver, distance, road, ETA strip; on-watch settings
- `protocol` — generator — Python — emits `Protocol.kt` and `watchapp/src/c/generated/protocol.h` from `protocol-definition.json`
- `rules` — data — JSON — bundled rulesets (5 apps, 22 files), catalog, fixtures
- `tools/rule-workbench` — maintainer CLI — Python — rule inspect/validate/test/regression, localized Google Maps generator
- `tools/route-capture` — maintainer harness — Python — emulator route drives, capture vs rules, GitHub issue reports
- `scripts` — build/test/release scripts — Bash + Python

### Build System
- **Type**: Gradle 8.13 (Kotlin DSL, version catalog) for Android; Pebble SDK `waf` (`wscript`) for the watchapp; Bash/Python scripts orchestrate
- **Config Files**: `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `android/gradle.properties`, `watchapp/wscript`, `watchapp/package.json`, `VERSION`
- **Build Dependencies**: `:app` → `:pebblekit`; `:app` assets and unit-test resources ← `rules/catalog` + `rules/bundled` (Gradle `syncRuleData`); unit-test resources ← `rules/fixtures` (`syncTestFixtures`); `:app` + `watchapp` ← `protocol/protocol-definition.json` (generated constants)

### APIs Discovered
- Android NotificationListenerService — `notification/NavigationNotificationListenerService.kt` — 2 callbacks (posted, removed)
- Pebble AppMessage protocol v1.4 — `protocol/protocol-definition.json` — 44 keys, 7 events, 5 flag bits, 13 maneuver codes
- Declarative ruleset JSON (schema v1) — `schemas/ruleset.schema.json` — 10 operators, 9 extractor types, 6 output fields
- Ruleset signed update envelope (Ed25519; disabled, no keys) — `update/SignedRuleset.kt`, `update/RulesetSignatureVerifier.kt`
- GitHub releases feed (optional update check) — `update/ReleaseFetcher.kt`
- Diagnostics export JSON (3 modes) — `export/ExportBuilder.kt`
- Android Sharesheet / content sharing — `export/DiagnosticShareManager.kt`

### Frameworks & Libraries
- Kotlin — 2.3.10 — language; Android Gradle Plugin — 8.13.2; KSP — 2.3.10
- Jetpack Compose BOM — 2024.12.01 — UI; Material 3; Navigation Compose — 2.8.5
- Room — 2.7.2 — debug history, user rules, official rulesets, state cache (schema v8)
- DataStore Preferences — 1.1.1 — settings
- kotlinx.serialization JSON — 1.7.3 — rules, snapshots, exports; kotlinx.coroutines — 1.9.0
- Timber — 5.0.1 — logging
- PebbleKit Android 2 (vendored) with kermit 2.0.4, androidx.annotation 1.9.1
- Android SDK: minSdk 31, compile/target 36; JVM 21
- Pebble SDK 3 (C), platforms aplite, basalt, chalk, diorite, emery
- Python 3.10+/3.12 with `jsonschema` (validators, workbench)

### Test Coverage
- **Test Directories**: `android/app/src/test/` (107 files, JVM + Robolectric), `android/app/src/androidTest/` (1 instrumented test), `watchapp/tests/` (13 host C tests), `tools/route-capture/tests/`, `scripts/test_*.py`, `tools/test_play_store_graphics.py`, `rules/fixtures/` (fixture data)
- **Test Frameworks**: JUnit 4.13.2, Robolectric 4.14.1, kotlinx-coroutines-test, Compose UI test, Room testing; host `gcc -std=c11 -Werror`; Python `unittest`; rule-workbench `regression`
- **Coverage Config**: absent (no JaCoCo/Kover; no coverage floor enforced)

### Code Quality Indicators
- **Linting**: Android Lint (`./gradlew lint`, default config, no `lint.xml`); C built with `-Wall -Wextra -Werror` in host tests; no ktlint/detekt, no Python linter
- **CI/CD**: `.github/workflows/android.yml` (protocol check, test, lint, assembleDebug), `rules.yml` (spec assets, schema, Python regression), `watchapp.yml` (Pebble SDK build), `codeql.yml`, `release.yml` (verify then tag, APK + PBW), `play-release.yml` (manual signed AAB); `.githooks/pre-commit`
- **Documentation**: strong: README, `AGENTS.md` contract, `spec/` and `requirements/` (hash-protected), `rules/fixtures/README.md` lessons log, KDoc on nearly every class citing REQ ids and issues; `docs/IMPLEMENTATION_STATUS.md` (3,245 lines)

### Technical Debt Signals
- No rule fills `etaEpochSeconds`; the engine converts only a `duration` extractor to an epoch (`RuleEngine.assemble`), and no extractor turns an `HH:MM` clock into one (`ExtractorRunner.kt`).
- `DurationParser` knows English units only (`h|hr|hrs|hour|hours`, `min…`, `s…`): `DurationParser.kt:10-12`.
- OsmAnd `secondaryText` semantics differ by language: German rules send the remaining distance (`3,3 km`) into the field the watch labels "ETA"; the other 8 OsmAnd languages send nothing (`rules/bundled/osmand/de.json`, `watchapp/src/c/main.c:417-455`).
- `OsmandRulesRegressionTest` declares `Expected.secondaryText` but never asserts it; only the Python workbench checks it.
- Python workbench `evaluate` returns no `etaEpochSeconds` and has no `duration` extractor, so the two engines diverge for ETA.
- A rules-only change does not run `android.yml` (path filter `android/**`); Kotlin regression runs on that PR only through `release.yml` after merge.
- Arrival-clock parsing exists twice (`DrivingSpeedEstimator.arrivalMillis`, watch `eta_parse_clock_minutes`) and is not reusable by the rule engine.
- Large files: `watchapp/src/c/main.c` (1,479 lines), `settings_window.c` (1,113), `DashboardScreen.kt` (673), `MainActivity.kt` (586).
- `scripts/__pycache__/changelog.cpython-312.pyc` is tracked despite `.gitignore`.
- `rules/README.md` and the Gradle `syncTestFixtures` description mention `rules/test-cases/`, which does not exist.
- No coverage tooling despite the org posture's coverage floors for non-express scopes.
- 0 TODO/FIXME/HACK markers; 4 suppressions (1 in app, 3 in vendored PebbleKit).

## Handoff Summary
- **Intent-relevant finding**: OsmAnd's expanded `bigText` already has the shape #134 needs: line 1 `<instruction>, then <road> <length>`, line 2 `<remaining> • <duration> • <HH:MM arrival> • <speed>` (fixtures `de-capture-*`, `rules/fixtures/osmand.json:382-391`). Only `osmand/de.json` reads it (29 rules: `-street`/`-then` variants for the road, `secondaryText` = remaining distance). The other 8 languages (`en, fr, es, it, nl, pt, pl, zh`, 12 rules each) read only `title`. For the ETA epoch, `RuleEngine.assemble` (`RuleEngine.kt:136-141`) treats a `duration` result as now plus seconds and any other `Num` as an absolute epoch, but `regexCapture` returns text, so a clock string never becomes an epoch. Two paths: (a) localize `DurationParser` hour/minute units and point `etaEpochSeconds` at a `duration` extractor, or (b) add an arrival-clock extractor, reusing `DrivingSpeedEstimator.arrivalMillis` logic. Path (b) changes the schema enum (`schemas/ruleset.schema.json`, hash-protected by `MANIFEST.sha256.json`), `RulesetModel`, `ExtractorRunner`, `RuleValidator`, the Python workbench, and needs a time zone injected into the engine.
- **Risks / follow-up**: (1) A duration-derived epoch changes every second, so `NavigationSessionReducer`'s dedup (`lastSentInstruction != instruction`) would resend on every notification; a minute-aligned clock epoch would not. (2) Rules are chosen by phone locale (`Locale.getDefault()` in `AppContainer`), not by OsmAnd's own language. (3) Localized Google Maps `it/fr/es/de/nl/pt/pl` are generated by `tools/rule-workbench/gen_localized_google_maps.py`, so change the generator and regenerate; `en`, `zh`, `any` are hand-authored. (4) Every rule change needs fixtures and regression in both engines (AGENTS.md rule 14), and the OsmAnd Kotlin test should start asserting `secondaryText` (and ETA). (5) Watch `SECONDARY_TEXT_MAX` is 24 bytes and primary is 64 (`ProtocolCodec.MAX_*_BYTES`). (6) Bump each touched ruleset's `rulesetVersion`.
