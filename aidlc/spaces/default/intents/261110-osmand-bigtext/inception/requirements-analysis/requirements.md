# Requirements: OsmAnd street and arrival time, arrival-clock ETA (#134)

## Intent analysis

For OsmAnd the watch shows less than the notification holds. The title carries the instruction without the street, and most OsmAnd rules read only the title, so in eight of nine languages the road line reads `Sla rechtsaf en volg` (turn right and follow…) with nothing after it and the second line stays empty. The expanded text (`bigText`) has what is missing: line 1 names the street, line 2 has the remaining distance, time left, the arrival clock and the speed. German already reads it, but puts the remaining distance where the watch expects a time ("ETA 3,3 km").

Goal: every OsmAnd language shows the street on the road line (in the full instruction by default, or the street alone with a new Full / Compact setting that covers every app) and the arrival time on the second line, the same way Google Maps does. Both apps send an arrival epoch to the watch, so the launcher countdown and the exact time-left readout work. [desc] (issue #134 and its decision comment)

Type: enhancement to bundled rules, one new rule-engine extractor, one new rule output (`compactText`) and one new synced setting. Scope: phone (rule engine, rule packs, fixtures, both regression engines, rule specs, settings, sender), protocol 1.5 (one setting key) and watchapp (one settings row). Complexity: standard.

## Functional requirements

### FR1. Road line from OsmAnd's expanded text (all languages)

Each matching rule produces two forms of the road line [Q1, Q6]: the **full** form (`primaryText`, the instruction with the street) and the **compact** form (new output `compactText`, the street only). Which one reaches the watch is chosen by the Directions text setting (FR7); Full is the default.

- **FR1.1** For each OsmAnd language (en, fr, es, it, nl, pt, pl, zh, de), when `bigText` line 1 names a street:
  - `primaryText` (full) is line 1 without the trailing next-step distance (`215 m`, `1,2 km`);
  - `compactText` (compact) is only the street.
  - Given nl `bigText` line 1 `Sla rechtsaf en volg Voorbeeldstraat 215 m`, when the turn-right rule matches, then `primaryText` is `Sla rechtsaf en volg Voorbeeldstraat` and `compactText` is `Voorbeeldstraat`.
  - Given de `bigText` line 1 `rechts abbiegen, dann B 1 Beispielstraße 400 m`, then `primaryText` is `rechts abbiegen, dann B 1 Beispielstraße` (as today) and `compactText` is `B 1 Beispielstraße`.
- **FR1.2** Continue steps (`Volg <Street>` in nl and the equivalent per language): full `Volg Voorbeeldstraat`, compact `Voorbeeldstraat`.
- **FR1.3** When `bigText` has no instruction line (near the destination it holds only the summary line), or line 1 names no street (`rechts abbiegen, dann 9 m`), `primaryText` falls back to today's behaviour (the title with its leading distance stripped) and no `compactText` is produced, so Compact shows the full text.
- **FR1.4** German keeps its current full text; it gains the compact form like every other language.
- **FR1.4a** Roundabout steps: the watch has no separate field for the exit number, so the compact form keeps OsmAnd's exit phrase in front of the street (de `2 Ausfahrt L 33 Beispielweg`; the equivalent short exit phrase in each other language).
- **FR1.5** Each language's street pattern is built from OsmAnd's own instruction strings for that language (its `res/values-<lang>/strings.xml`), as #61 and #57 did, not copied from German. The arrow (`maneuver`) and distance (`distanceMeters`) outputs of every existing rule stay unchanged.

### FR2. Second line: arrival time

- **FR2.1** For every OsmAnd language, `secondaryText` is the arrival clock from `bigText`'s summary line (`2,79 km • 42 min • 11:00 • 142 min/km` → `11:00`), whether the summary is line 2 or the only line.
- **FR2.2** German OsmAnd stops sending the remaining distance as `secondaryText` and sends the arrival clock instead.
- **FR2.3** When no arrival clock can be found, `secondaryText` is absent (not a distance or other value).
- **FR2.4** Google Maps keeps its current `secondaryText` (the `Arrive 12:12` clock).

### FR3. Arrival epoch for the watch (`etaEpochSeconds`)

- **FR3.1** A new rule extractor turns an arrival clock captured from a notification field into an epoch in seconds [Q2]: the next occurrence of that wall-clock time in the phone's time zone, at or after the notification's post time. Arrival clocks earlier than now are taken as the next day (a drive past midnight).
- **FR3.2** The extractor accepts 24-hour (`11:00`, `9:05`) and 12-hour (`12:12 PM`, `9:05 am`) clocks. Text it cannot read yields no value (the rule still matches; `etaEpochSeconds` is just absent).
- **FR3.3** The value is whole minutes (seconds = 0), so a notification whose arrival clock has not changed produces the same instruction and is not resent to the watch.
- **FR3.4** Every OsmAnd rule (all 9 languages) and every Google Maps rule that shows an arrival clock sets `etaEpochSeconds` with this extractor. For the generated Google Maps languages (it, fr, es, de, nl, pt, pl) the generator is changed and the packs regenerated; en, zh and any are edited by hand.
- **FR3.5** The extractor is part of the rule format everywhere it is defined: the rule schema, the Kotlin model, runner and validator, the Python workbench engine and validator, and the rule-format spec (`REQ-RULES.md`, `RuleEngine.md`). The protected schema's hash in `MANIFEST.sha256.json` is updated through the repository's tool.

### FR4. Fixtures and regression tests

- **FR4.1** `rules/fixtures/osmand.json` gets synthetic fixtures for every language that follow the captured layout (`title` = `distance • instruction`, `bigText` = instruction line with street and distance, then the summary line), with made-up street names and no real places or times from the private capture. Each language covers: a turn with a street, a continue step, a summary-only card (fallback), and a line 1 without a street.
- **FR4.2** Fixtures can state the expected `secondaryText` and `etaEpochSeconds`, and both regression engines (Kotlin `OsmandRulesRegressionTest` and the Python workbench) check them, along with `primaryText`. The expected epoch is checked against a fixed post time and time zone so the tests are deterministic.
- **FR4.3** Unit tests for the arrival-clock extractor cover: a 24-hour clock later today, a clock earlier than now (next day), a 12-hour PM clock, `12:xx AM`, an unreadable value, and a time zone other than UTC.
- **FR4.4** Google Maps fixtures gain the expected `etaEpochSeconds` for the cards that carry an arrival clock.

### FR5. Verification on the emulator [Q3]

- **FR5.1** During Build and Test, OsmAnd runs on the Windows Android emulator with `tools/route-capture` in English and French, and the captured notifications confirm the `title` / `bigText` layout the new patterns assume. Any difference becomes a pattern fix plus a synthetic fixture.
- **FR5.2** Captures stay private: no real street names, places or captured files are committed or posted publicly; only synthetic fixtures derived from their structure.

### FR7. Directions text setting: Full / Compact [Q5, Q6, Q7]

- **FR7.1** A new setting "Directions text" with two values: **Full** (default) sends `primaryText`; **Compact** sends `compactText` when the rule produced one, else `primaryText`. With the default, what the watch shows is unchanged from today for every app except where FR1 adds a street OsmAnd did not show before.
- **FR7.2** The setting is in the watchapp's settings menu and in the phone app's Watch settings, and the two stay in sync, like the other watch settings: a new protocol setting key in protocol 1.5, persisted on the watch, mirrored on the phone, with the setting count raised by one. On the watch it is one row that toggles Full ↔ Compact in place (no sub-menu), on every platform including aplite.
- **FR7.3** The phone applies the choice when it builds the instruction it sends, so the watchapp only stores and shows the setting. Changing it re-sends the current instruction in the new form within one send (no wait for the next notification).
- **FR7.4** With a watchapp older than protocol 1.5, the phone still offers the setting in its own Watch settings and applies it; only the watch menu row is missing.
- **FR7.5** All apps get a compact form in this PR where their notification has more than the street [Q7]:
  - **Google Maps**, every language: from `Turn right onto Example Street` the compact form is `Example Street` (the street after the language's "onto/toward/on" word; for the generated languages through the generator). Cards without a street keep only the full form.
  - **Vela Maps** (en): `Turn right onto Main St` → `Main St`.
  - **Organic Maps and CoMaps** already show only the street (the notification has no instruction words), so their full and compact forms are the same and their rules do not change.
- **FR7.6** The setting appears in the diagnostics export with the other watch settings, and the watch's About/help text names it.

### FR6. Play debug symbols (#136) [Q4]

- **FR6.1** The release build includes native debug symbols in the App Bundle (`ndk { debugSymbolLevel = "SYMBOL_TABLE" }`), so the bundle carries `BUNDLE-METADATA/com.android.tools.build.debugsymbols/`.
- **FR6.2** The release APK used for GitHub and F-Droid is unchanged by FR6.1 (F-Droid's reproducible build still matches).

## Non-functional requirements

- **NFR1 Fit:** the road line sent (full or compact) stays within the watch's 64-byte limit and `secondaryText` within 24 bytes (`ProtocolCodec` limits); longer text is fitted by the existing truncation, not dropped.
- **NFR7 Default unchanged:** with Directions text at its default (Full), every existing fixture's `primaryText` and every send for Google Maps, Vela, Organic Maps and CoMaps is the same as before (verified by the existing fixtures and a controller test).
- **NFR2 No extra sends:** with an unchanged instruction, distance bucket and arrival clock, consecutive notifications produce no new send to the watch (verified by a reducer or controller test with two identical cards a few seconds apart).
- **NFR3 Rule cost:** the new and changed regexes keep the ruleset validator's limits (pattern length, no catastrophic backtracking per the existing checks); each OsmAnd notification is still matched in under 5 ms on the JVM test (existing performance check, if present, stays green).
- **NFR4 Compatibility:** rule packs that use the new extractor declare a `minimumAppVersionCode` of the release that ships it, so older apps fetching remote rules skip them instead of failing; bundled packs bump `rulesetVersion`.
- **NFR5 Regression:** the full test suite (`test-all.sh`: Kotlin unit tests, Python workbench, watchapp tests) passes, and every existing fixture keeps its expected arrow and distance.
- **NFR6 Privacy:** no data from the private diagnostics export (street names, places, times, email) appears in commits, fixtures, issues or PR text.

## Constraints

- Rule packs are chosen by the phone's language, not OsmAnd's; a phone in one language with OsmAnd in another is not covered (unchanged behaviour).
- Every rule change needs fixtures and regression tests in both the Kotlin and Python engines (AGENTS.md).
- Generated Google Maps packs are changed through `tools/rule-workbench/gen_localized_google_maps.py`, never by hand.
- No code is committed straight to main (every push releases); the work goes to branch `feat/osmand-bigtext` and a PR.
- No heavy emulator in WSL; the Windows emulator via `adb.exe` is used and closed after use.

## Assumptions

- OsmAnd's summary line has the same order in every language (remaining distance • time left • arrival clock • speed), because it is built from one format with localized units. To confirm against OsmAnd's source and the en/fr emulator runs (FR5.1). [assumption]
- OsmAnd formats the arrival clock per the phone's 24-hour setting; both forms are handled by FR3.2. [assumption]
- The street in line 1 is followed by the next-step distance and nothing else, as in the nl and de captures. [assumption]
- The watch needs no change for the second line or the ETA: it already shows `secondaryText` and uses `etaEpochSeconds` when present (`main.c` `format_eta_readout`, `glance_text.c`). Its only change is the Directions text settings row (FR7.2).

## Out of scope

- Arrow or distance changes for OsmAnd.
- Showing time left or remaining distance as a separate watch field.
- A third, in-between Directions text level (short turn word + street) [Q6].
- The `0 m •` card with an empty instruction (stays unmatched).
- Matching rules by OsmAnd's own language instead of the phone's.
- "When driving" speed support for OsmAnd (#135).
- The Play listing text (#124).

## Open questions

- None blocking. The assumptions above are checked during Code Generation (OsmAnd source) and Build and Test (emulator runs).

## Sources

- Initial description: "Implement GitHub issue #134 (OsmAnd: show the street and arrival time from the expanded notification text, all languages; arrival-clock ETA for OsmAnd and Google Maps) as described in the issue and its decision comment, on branch feat/osmand-bigtext" [desc]
- Workflow-selected scope: express [scope]
- Answers Q1–Q7 in `requirements-analysis-questions.md` [Q1] [Q2] [Q3] [Q4] [Q5] [Q6] [Q7]; Q1 changed and Q5–Q7 added at the requirements approval (full instruction by default, with a setting)
- Code knowledge base `aidlc/spaces/default/codekb/PebbleNTN/` and `inception/reverse-engineering/developer-scan.md` (rule engine, ETA path, test gaps)
