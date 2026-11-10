# Code Generation Plan: OsmAnd street and arrival time, Directions text setting (#134)

## Summary

- Builds: OsmAnd street/arrival rules in 9 languages, a compact road line for Google Maps and Vela, an arrival-clock ETA extractor, the synced Directions text setting (Full / Compact, protocol 1.5), and Play debug symbols (#136)
- Touches: `rules/bundled/` (osmand, google-maps, vela-maps), `rules/fixtures/`, `schemas/ruleset.schema.json`, `android/app/.../rules/`, `core/`, `protocol/`, `pebble/NavigationController.kt`, watch settings UI, `watchapp/src/c/` settings, `protocol/protocol-definition.json`, `tools/rule-workbench/`, specs and requirements, `android/app/build.gradle.kts`
- Tests: about 15 new unit tests (extractor, engine, codec, settings, controller, watch C) plus fixture-driven regression for every language in the Kotlin and Python engines

Work on branch `feat/osmand-bigtext` (created from `main` at Step 1). Never commit to `main`.

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

## Design decisions

- **Two road-line forms.** `NavigationInstruction` gains `compactText: String?`; rule output gains an optional `compactText` extractor (same extractor kinds as `primaryText`). The phone picks the form when it encodes the state: `ProtocolCodec.encodeState(..., compact = true)` sends `compactText ?: primaryText`. The reducer compares whole instructions, so both forms take part in its duplicate check.
- **Arrival clock extractor.** New extractor type `arrivalClock` with `field`, optional `pattern` and `group` (like `regexCapture`); without a pattern it takes the first `H:MM` / `HH:MM` with an optional `AM`/`PM`. `ExtractorRunner` returns the captured clock as text; `RuleEngine.assemble` turns it into an epoch for `etaEpochSeconds` only (like the `duration` special case), using `nowEpochSeconds` and a `ZoneId` given to the engine (constructor parameter, default `ZoneId.systemDefault()`; tests pass a fixed zone). Next occurrence at or after now, minus a 60 s grace so "11:00" seen at 11:00:30 is today, seconds = 0. The parsing lives in a new `ArrivalClock` object in `rules/`, and `DrivingSpeedEstimator.arrivalMillis` reuses it so there is one clock parser.
- **Directions text setting.** `WatchSetting.DIRECTIONS_TEXT(2)`: 0 Full (default), 1 Compact. Protocol 1.5: key 44 `SETTING_DIRECTIONS_TEXT`, watch persist key 32, setting count 26 → 27, `PROTOCOL_MINOR` 5. `isDriving` becomes the range of the three driving settings; a new `isDirectionsText` marks the 1.5 setting. On the watch: one row "Directions text" with value "Full" / "Compact" that toggles in place on every platform (aplite included). FR7.4: when a READY or SETTINGS from a pre-1.5 watch does not carry key 44, the phone's mirror keeps its own last value for it instead of resetting to the default, and the phone's Watch settings shows the row for every protocol ≥ 1.3 watch.
- **Re-send on change.** `NavigationController` re-encodes and sends the current navigating state when the effective Directions text value changes (from the phone UI or a watch SETTINGS message), not on unrelated setting changes. Nothing is sent while hidden for driving.
- **German keeps its full text.** The existing `-street` / `-then` rules keep `primaryText`; they gain `compactText` (street only) and change `secondaryText` to the arrival clock.
- **Generated Google Maps packs** (it, fr, es, de, nl, pt, pl) change only through `gen_localized_google_maps.py` and are regenerated; en, zh, any are edited by hand.
- **Rule-pack compatibility.** Packs using `arrivalClock` or `compactText` set `minimumAppVersionCode` to the version code of this release (83); every changed pack bumps `rulesetVersion` to `<app>-<lang>-2026.11.10`.

## Steps

### Step 1: Branch and baseline
- [x] Create `feat/osmand-bigtext` from `main`; commit nothing to `main`.
- [x] Verify the runners and record baseline counts: `./gradlew :app:testDebugUnitTest` (from `android/`), `python3 tools/rule-workbench/workbench.py test` (or the workbench's documented fixture command), watchapp `tests/` runner, and `./test-all.sh` once for the baseline. Record results in `unit-test-instructions.md`'s baseline line.
- Traces: NFR5

### Step 2: Confirm OsmAnd's strings and layout (research, no code)
- [x] From OsmAnd's source (`OsmAnd/res/values-<lang>/strings.xml` for en, fr, es, it, nl, pt, pl, zh, de, and the navigation notification builder), record per language: the instruction phrases that precede the street on `bigText` line 1 (turn, keep, sharp, slight, U-turn, continue/follow, roundabout exit), the summary-line format (order, separators, time-left units, arrival clock 24 h vs AM/PM), and confirm that line 1 is `<instruction> <street> <next-step distance>`.
- [x] Write the findings to `aidlc/spaces/default/intents/261110-osmand-bigtext/construction/code-generation/osmand-strings.md` (record only; no private capture data).
- Traces: FR1.5, assumptions in requirements

### Step 3: Data model: instruction, rule output, schema
- [x] `core/NavigationInstruction.kt`: add `compactText: String? = null` (KDoc).
- [x] `rules/RulesetModel.kt`: add `compactText` to `RuleOutput`; add `ArrivalClockExtractor(field, pattern?, group = 1)` and its serial name `arrivalClock`.
- [x] `rules/RulesetCodec.kt` (if it maps types by hand) and `rules/RuleValidator.kt`: accept and validate `arrivalClock` (field known, pattern compiles through `SafeRegex` limits, group ≥ 0) and `compactText`.
- [x] `schemas/ruleset.schema.json`: add `arrivalClock` to the extractor enum and `compactText` to `output`; update `MANIFEST.sha256.json` with the repository's manifest tool (not by hand).
- [x] `protocol/protocol-definition.json` (+ `examples/protocol-definition.json`, manifest): protocol 1.5, key 44 `SETTING_DIRECTIONS_TEXT`, values `DIRECTIONS_TEXT_FULL = 0`, `DIRECTIONS_TEXT_COMPACT = 1`; regenerate `Protocol.kt` and `watchapp/src/c/generated/protocol.h` with the protocol generator.
- Traces: FR3.5, FR7.2

### Step 4: Data model tests
- [x] `RulesetCodecTest`: a rule with `compactText` and an `arrivalClock` extractor round-trips.
- [x] `RuleValidatorTest`: `arrivalClock` with a bad regex or unknown field is rejected; a valid one passes.
- [x] Generated-protocol check (existing protocol test) passes at 1.5.
- [x] Run: `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.rules.RulesetCodecTest' --tests 'com.pebblentn.app.rules.RuleValidatorTest'`.
- Traces: FR3.5

### Step 5: Business logic: arrival clock and engine
- [x] New `rules/ArrivalClock.kt`: `parse(text): LocalTime?` (24 h and 12 h, `12:xx AM` = 0:xx, `12:xx PM` = 12:xx, rejects 25:00 / 9:7) and `nextEpochSeconds(time, nowEpochSeconds, zone): Long` (next occurrence with the 60 s grace, whole minutes, DST-safe via `ZonedDateTime`).
- [x] `rules/ExtractorRunner.kt`: run `ArrivalClockExtractor` (regex capture or first clock in the field) → `Text`.
- [x] `rules/RuleEngine.kt`: `zone: ZoneId` constructor parameter; `assemble` runs `compactText`; for `etaEpochSeconds` an `ArrivalClockExtractor` result becomes `ArrivalClock.nextEpochSeconds(...)`, an unreadable clock gives null.
- [x] `core/DrivingSpeedEstimator.kt`: `arrivalMillis` uses `ArrivalClock.parse` (behaviour unchanged; its tests stay green).
- Traces: FR3.1, FR3.2, FR3.3

### Step 6: Business logic tests
- [x] New `ArrivalClockTest`: 24 h later today; earlier than now → next day; `12:12 PM`; `12:05 AM`; unreadable text → null; non-UTC zone (Europe/Brussels) and a DST-change day; whole minutes.
- [x] `RuleEngineTest`: `compactText` is assembled; `etaEpochSeconds` from `arrivalClock` with fixed now and zone; an unreadable clock leaves the ETA null but the rule still matches.
- [x] `ExtractorRunnerTest`: `arrivalClock` with and without a pattern.
- [x] Run: `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.rules.ArrivalClockTest' --tests 'com.pebblentn.app.rules.RuleEngineTest' --tests 'com.pebblentn.app.rules.ExtractorRunnerTest' --tests 'com.pebblentn.app.core.DrivingSpeedEstimatorTest'`.
- Traces: FR4.3, FR3.1–FR3.3

### Step 7: Rule packs: OsmAnd (9 languages)
- [x] For en, fr, es, it, nl, pt, pl, zh (from Step 2's strings): add `-street` variants above each maneuver rule (as `de.json` does) that match `bigText` line 1 with a street; `primaryText` = line 1 without the trailing distance, `compactText` = the street (roundabouts: exit phrase + street, FR1.4a); continue steps (`Volg <Street>` and equivalents) likewise. Base rules keep the title fallback (FR1.3) and produce no `compactText`.
- [x] Every OsmAnd rule in all 9 languages: `secondaryText` = arrival clock from the summary line (`regexCapture`), `etaEpochSeconds` = `arrivalClock` on the same capture. `de.json`: replace the remaining-distance `secondaryText` with the arrival clock; add `compactText` to the `-street` variants.
- [x] Maneuver and distance outputs unchanged (FR1.5); arrive rules stay lowest priority; `rulesetVersion` and `minimumAppVersionCode` bumped.
- Traces: FR1.1–FR1.5, FR2.1–FR2.3, FR3.4, NFR4

### Step 8: Rule packs: Google Maps and Vela
- [x] `tools/rule-workbench/gen_localized_google_maps.py`: emit `compactText` (the street after the language's onto/toward/on word, from the existing localized templates) and `etaEpochSeconds` = `arrivalClock` on `subText`; regenerate it, fr, es, de, nl, pt, pl.
- [x] `google-maps/en.json`, `zh.json`, `any.json` by hand: same two outputs where the card carries a street or an arrival clock.
- [x] `vela-maps/en.json`: `compactText` (street after onto/on/toward) and `etaEpochSeconds` from `Arrive HH:MM` in `text`.
- [x] Organic Maps and CoMaps: unchanged (FR7.5).
- [x] Bump `rulesetVersion` / `minimumAppVersionCode` of each changed pack.
- Traces: FR2.4, FR3.4, FR7.5, NFR4, NFR7

### Step 9: Fixtures and regression engines (tests for Steps 7–8)
- [x] Fixture format: `expected.compactText`, `expected.secondaryText`, `expected.etaEpochSeconds`; fixture-level `nowEpochSeconds` and `timeZone` (defaults 0 and UTC). Document in `rules/fixtures/README.md`.
- [x] `rules/fixtures/osmand.json`: per language, synthetic fixtures with made-up streets and times following the captured layout: turn with street, continue step, summary-only card, line 1 without a street, roundabout. Update the German fixtures' expected `secondaryText`.
- [x] `rules/fixtures/google-maps.json` and `vela-maps.json`: expected `compactText` and `etaEpochSeconds` on cards with a street / arrival clock; existing `primaryText` expectations unchanged (NFR7).
- [x] Kotlin regression tests (`OsmandRulesRegressionTest`, `GoogleMapsRulesRegressionTest`, `VelaMapsRulesRegressionTest`, and Organic/CoMaps untouched): check `primaryText`, `compactText`, `secondaryText`, `etaEpochSeconds` when given, with the fixture's now and zone.
- [x] `tools/rule-workbench/workbench.py`: implement `arrivalClock` and `compactText` the same way, read the new fixture fields, and check them.
- [x] NFR1 check: a test that every fixture's sent text (both forms) fits 64 / 24 bytes after `ProtocolCodec.fitText`.
- [x] Run: `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.rules.*RulesRegressionTest' --tests 'com.pebblentn.app.rules.RulesetVersionsTest'` and the workbench fixture command.
- Traces: FR4.1, FR4.2, FR4.4, NFR1, NFR3, NFR5, NFR6

### Step 10: Settings and send path (phone)
- [x] `core/WatchSettingsModel.kt`: `DIRECTIONS_TEXT(2)`, `isDriving` limited to the three driving settings, `isDirectionsText`, `compactDirections` accessor.
- [x] `core/WatchSettingsSync.kt`, `data/WatchSettingsMirrorRepository.kt`: carry the new setting; keep the mirror's own value for key 44 when a pre-1.5 watch leaves it out (FR7.4).
- [x] `protocol/ProtocolCodec.kt`: `encodeState(..., compact: Boolean = false)` sends `compactText ?: primaryText` when compact.
- [x] `pebble/NavigationController.kt`: pass the setting into every state encode; re-send the current navigating state once when the effective value changes (FR7.3), never while hidden for driving.
- [x] Diagnostics export (`export/ExportBuilder.kt`): the setting appears with the other watch settings (FR7.6), and `compactText` is exported with the instruction.
- Traces: FR7.1–FR7.4, FR7.6, NFR2

### Step 11: Settings and send path tests
- [x] `ProtocolCodecTest`: Full sends `primaryText`; Compact sends `compactText`; Compact without `compactText` sends `primaryText`.
- [x] `WatchSettingsModelTest` / mirror test: new key 44 decoded; a pre-1.5 READY without key 44 keeps the phone's value.
- [x] `NavigationControllerTest`: changing Directions text re-sends the current instruction once in the new form; an unrelated setting change sends nothing; two identical OsmAnd cards seconds apart cause one send (NFR2); default settings send the same as before (NFR7).
- [x] Run: `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.protocol.ProtocolCodecTest' --tests 'com.pebblentn.app.core.WatchSettings*' --tests 'com.pebblentn.app.pebble.NavigationControllerTest' --tests 'com.pebblentn.app.data.WatchSettingsMirror*'`.
- Traces: FR7.1–FR7.4, NFR2, NFR7

### Step 12: Frontend: phone Watch settings and watchapp
- [x] Phone: a "Directions text" row (Full / Compact) in Watch settings with a one-line explanation ("Compact shows only the street name when the app gives one"), `strings.xml`, shown for every protocol ≥ 1.3 watch.
- [x] Watchapp: `settings_sync.c/.h` table entry (persist key 32, count 27, default Full, valid 0–1), `theme.c/.h` value names, `settings_window.c` one in-place toggle row "Directions text" on all platforms, `about_text.h` / `about_window.c` help line; protocol minor 5 in READY.
- [x] Watch tests: `watchapp/tests/` (settings sync table: new key round-trips, out-of-range → default; value names).
- [x] Phone UI: existing Watch settings UI test (if any) extended for the new row.
- [x] Run: the watchapp test command from `unit-test-instructions.md` and `./gradlew :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.watchsettings.*'`; build the watchapp for all platforms (`pebble build`) to check aplite heap.
- Traces: FR7.2, FR7.6

### Step 13: Build configuration (#136)
- [x] `android/app/build.gradle.kts` release buildType: `ndk { debugSymbolLevel = "SYMBOL_TABLE" }`.
- [ ] Verify: `./gradlew :app:bundleRelease` (unsigned or with the CI debug config) contains `BUNDLE-METADATA/com.android.tools.build.debugsymbols/`, and `:app:assembleRelease` APK contents are unchanged (compare file lists/hashes of `lib/` and `classes*.dex` before and after).
- Traces: FR6.1, FR6.2

### Step 14: Documentation and traceability
- [x] `requirements/REQ-RULES.md` (arrivalClock, compactText), `spec/200-architecture/RuleEngine.md`, `spec/300-data/RulesJSON.md`, `REQ-WATCH.md` / `REQ-ANDROID.md` (Directions text, protocol 1.5), `spec/200-architecture/Protocol.md`, `spec/200-architecture/Pebble.md`, `spec/400-ui/WatchUI.md` / `AndroidUI.md`, `spec/300-data/ExportFormat.md`; update `MANIFEST.sha256.json` with the tool.
- [x] `docs/IMPLEMENTATION_STATUS.md` per CLAUDE.md (milestone, requirements, commands, blockers, next task).
- [x] Full suite: `./test-all.sh` green.
- Traces: FR3.5, NFR5

## Requirement → step map

| Requirement | Steps |
|---|---|
| FR1.1–FR1.5 | 2, 7, 9 |
| FR2.1–FR2.4 | 7, 8, 9 |
| FR3.1–FR3.5 | 3, 4, 5, 6, 7, 8, 14 |
| FR4.1–FR4.4 | 6, 9 |
| FR5.1–FR5.2 | Build and Test (emulator runs in en and fr) |
| FR6.1–FR6.2 | 13 |
| FR7.1–FR7.6 | 3, 10, 11, 12 |
| NFR1–NFR7 | 9, 11, 1/14, 7/8 |
