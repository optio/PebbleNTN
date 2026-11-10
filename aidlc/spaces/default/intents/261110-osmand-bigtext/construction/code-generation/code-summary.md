# Code Summary: #134 OsmAnd street and arrival time, Directions text setting (+ #136)

Branch `feat/osmand-bigtext`, created from `main` at 2afb066. Nothing is committed yet.

## What was built

- **Rule engine:** a new `compactText` rule output and a new `arrivalClock` extractor. `ArrivalClock.kt` turns `11:00`, `12:12 PM` or `下午 3:05` into the next occurrence of that time, with a 60 s grace and whole minutes. It is shared with `DrivingSpeedEstimator`. `RuleEngine` takes `zone: () -> ZoneId`, so the phone's current time zone is used. The schema, validator, `RulesetModel`, `SnapshotFields` and the Python workbench all support the new output and extractor.
- **OsmAnd (9 languages):**
  - Full road line: bigText line 1 without the distance.
  - Compact road line: the street.
  - Second line: the arrival clock, plus `etaEpochSeconds`. German no longer shows the remaining distance there.
  - The instruction regexes are built from OsmAnd's `route_*` strings (see `osmand-strings.md`).
- **Google Maps (10 packs) and Vela en:** primaryText is unchanged. They get a compact form (the street after the last "onto/toward" word) and the arrival-clock ETA. Organic Maps and CoMaps are unchanged, since their text is already just the street.
- **Directions text setting (Full = 0, the default; Compact = 1):**
  - Protocol 1.5, key 44, persist key 32. `ProtocolCodec` sends `compactText ?: primaryText`.
  - `NavigationController` re-sends the current card when the setting changes.
  - Phone UI row, watch Settings row (the last row) and About text.
  - The diagnostics export moves to schema 7, with `metadata.watchSettings` and `watchOutput.compactText`.
  - A watchapp older than 1.5 keeps the phone's value (FR7.4).
- **#136:** `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` in `android/app/build.gradle.kts`.
- **Specs and records:**
  - New requirements: REQ-RULE-018 and 019, REQ-WATCH-027, REQ-ANDROID-025.
  - Updated: RuleEngine, RulesJSON, Protocol, Pebble, WatchUI, AndroidUI and ExportFormat docs; `MANIFEST.sha256.json`; `docs/IMPLEMENTATION_STATUS.md`.
  - Changed packs set `minimumAppVersionCode` 83 and `rulesetVersion` `*-2026.11.10`.

## Deviations from the plan

1. **German road line on Full.** Today the German `-street` rules send only the street. Following FR1.1, Full now sends the whole line 1 (e.g. "rechts abbiegen, dann B 1"); Compact sends the street as before.
2. **Roundabout compact forms.** A capture can only take one unbroken stretch of text, so en, es, it and pt-BR read like "2 exit and go Main St". nl, fr, de and pl are clean.
3. **Extra fix in both engines:** the leading-distance strip knows the Chinese units, and the workbench normalises Unicode spaces the way `SnapshotFields` does.
4. **Aplite heap.** To keep 3416 B free (3431 B on the build report):
   - the Directions text names became a header macro;
   - the aplite About page leaves out the Directions note;
   - `ABOUT_DETAILS_LEN` went from 128 to 112.
5. **No manifest tool exists**, so the hashes reported by `validate-spec-assets.sh` were copied into `MANIFEST.sha256.json` by hand.
6. **Setting values.** The protocol JSON has no section for them, so they live in `WatchSettingsModel.DirectionsText` and `watchapp/src/c/directions_text.h`.

## Tests

- **Baseline** `scripts/test-all.sh` on 2afb066: green. 703 JVM tests; workbench google-maps 189, osmand 195, vela 60, comaps 21, organic 12; 13 watch test programs.
- **Final** `scripts/test-all.sh`: exit 0. 753 JVM tests per variant, 0 failures, 4 skipped. Workbench google-maps 206/206, osmand 243/243, vela 63/63, comaps 21/21, organic 12/12. Watch tests OK. Lint 0 errors. All 5 watch platforms build.
- **New tests:**
  - `ArrivalClockTest` and `FixtureTextFitTest` (NFR1);
  - additions to the RuleEngine, ExtractorRunner, codec and validator, regression, ProtocolCodec, settings, mirror, NavigationController, export and UI tests;
  - the watch settings tests.
- **#136:** the release APK is byte-identical before and after (FR6.2).

**Not run (Build and Test):** FR5 OsmAnd captures on the emulator in en and fr; the Directions text row on the basalt and aplite emulators; runtime `heap_bytes_free()` on aplite.

## Open issues

- **FR6.1 cannot be met from this repo.** The only native libraries come already stripped from their AARs and no NDK is installed, so the bundle has no `debugsymbols` folder and Play's warning stays. The setting is harmless (FR6.2 holds).
- OsmAnd layouts other than nl and de come from OsmAnd's source, not from captures; FR5 checks en and fr.
- The aplite heap margin is 15 B.
