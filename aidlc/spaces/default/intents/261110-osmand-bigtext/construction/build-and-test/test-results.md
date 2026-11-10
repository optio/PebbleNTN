# Test Results: OsmAnd street and arrival time, Directions text (#134, #136)

Date: 2026-11-10. Branch `feat/osmand-bigtext` (uncommitted working tree on top of 2afb066).

## Build

| Command | Result |
|---|---|
| `scripts/test-all.sh` (spec assets, protocol check, version sync, watch C tests, rule and catalog validation, route-capture unit tests, Android tests and lint) | exit 0, "All tests finished." |
| `scripts/generate-protocol.sh --check` (inside test-all) | up to date, protocol 1.5 |
| `scripts/build-watchapp.sh` (Code Generation run) | all five platforms built; aplite free heap 3,431 B |
| `./android/gradlew -p android assembleRelease` before and after the #136 line (Code Generation run) | byte-identical APK (sha256 ae1591a0…) |
| `./android/gradlew -p android bundleRelease` (Code Generation run) | bundle built, **no** `BUNDLE-METADATA/com.android.tools.build.debugsymbols/`; AGP: "Unable to extract native debug metadata … already been stripped" |

## Tests

| Suite | Total | Passed | Failed | Skipped |
|---|---|---|---|---|
| Android debug unit tests | 753 | 749 | 0 | 4 |
| Android release unit tests | 753 | 749 | 0 | 4 |
| Android lint | 0 errors | | | |
| Rule workbench `regression` | google-maps 206, osmand 243, vela 63, comaps 21, organic 12 | all | 0 | 0 |
| Watchapp host C tests | 13 programs | 13 | 0 | 0 |

The same 4 tests were skipped before this change. Android tests went from 703 to 753. Coverage percentages are not measured, because express sets no coverage floor.

## OsmAnd on the Android emulator (FR5.1)

The run was `tools/route-capture/routecap.py run scenarios/osmand-m3.json --only 'osmand\.(en-GB|fr-FR)\.car\.'`:
- OsmAnd 5.3.10 on Android 17, Windows emulator;
- public streets with made-up trips;
- run output in `tools/route-capture/out/20261110-192453/` (git-ignored).

| Scenario | Captured | Recognised | Unrecognised direction cards |
|---|---|---|---|
| en-GB car, London | 51 | 51 | 0 |
| fr-FR car, Paris | 33 | 27 | 0 (the other 6 are `0 m • ` cards that an existing fixture keeps unshown) |

Each distinct captured card was re-evaluated with the workbench (Europe/London). Street names are replaced by `<Street>`.

| Rule | bigText line 1 | Full | Compact | Second line / ETA |
|---|---|---|---|---|
| `osmand-turn-right-street-en` | `Turn right and go <Street> 0.3 mi` | `Turn right and go <Street>` | `<Street>` | clock / yes |
| `osmand-turn-left-street-en` | `Turn left and go <Street> 100 m` | `Turn left and go <Street>` | `<Street>` | clock / yes |
| `osmand-roundabout-street-en` | `Take 1 exit and go <Street> 1.1 km` | `Take 1 exit and go <Street>` | `1 exit and go <Street>` (accepted roundabout form) | clock / yes |
| `osmand-turn-left-en` (last step, summary only) | `250 m • 1 min • <clock> • 0 mph` | `Turn left and go` (title fallback, FR1.3) | none, so Full is sent | clock / yes |
| `osmand-turn-left-street-fr` | `Tournez à gauche <Street> 100 m` | `Tournez à gauche <Street>` | `<Street>` | clock / yes |
| `osmand-turn-right-street-fr` | `Tournez à droite <Street> 60 m` | `Tournez à droite <Street>` | `<Street>` | clock / yes |
| `osmand-turn-left-fr` (summary only) | `60 m • <1 min • <clock> • 0 km/h` | `Tournez à gauche` (title fallback) | none | clock / yes |

This confirms the layout that the en and fr rules were built from (OsmAnd's source): every card's line 1 and summary line read as designed. The emulator shut down after the run.

## Watch emulator check (FR7.2)

| Platform | Check | Result | Evidence |
|---|---|---|---|
| basalt | Navigation screen with a full road line | wraps on two lines as before | `evidence/basalt-0-nav.png` |
| basalt | Last Settings row "Directions text: Full" | as designed | `evidence/basalt-1-directions-row.png` |
| basalt | SELECT → "Compact", SELECT → "Full" | as designed | `evidence/basalt-2-compact.png`, `evidence/basalt-3-full-again.png` |
| aplite | Last row "Directions text: Full", SELECT → "Compact" | as designed | `evidence/aplite-1-directions-row.png`, `evidence/aplite-2-compact.png` |

The Pebble emulators were shut down afterwards.

## Other checks

- **NFR3:** `everyFixtureExtractsExpectedOutput` matched 243 OsmAnd fixtures in 41 ms, about 0.17 ms per notification against the 5 ms limit. `bundledRulesetIsValid` passes.
- **NFR6:** the words in the two private captures (kept outside the repo) were compared with the diff. The only hits were generic words (Continue, U-turn, maneuver names, "Library" in "No new library."). The Android manifest is unchanged.

## Not run here

- The phone and watch checks in `integration-test-instructions.md` §3 (switching the setting while navigating) need a real phone with a Pebble.
- A runtime `heap_bytes_free()` reading on aplite: the watchapp has no runtime heap log. The build report's free heap at load is the measurement REQ-WATCH-025 uses (as in #132).

## Accepted failure

FR6.1 (#136) is Not Met. The app's only native libraries (`libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`) come already stripped from their AARs, so there are no symbols to package. No fix exists in any setting, version or flag on our side. At the halt-and-ask on 2026-11-10 the person chose **Accept failure**. The `debugSymbolLevel` line stays (harmless; FR6.2 met), and #136 will be closed as not fixable from the app. No loop-back was used.

## Target Verification Matrix

| Target ID | Source | Expected | Actual | Evidence | Owning Stage | Verdict |
|---|---|---|---|---|---|---|
| TC-MINIMAL-1 | Testing Contract: one test per requirement | every FR/NFR traced | 35 of 36 IDs covered; FR6.1 not | `cross-unit-traceability.md` | build-and-test | Not Met (FR6.1) |
| TC-MINIMAL-2 | Testing Contract: happy-path test per new component | ArrivalClock, compactText, arrivalClock extractor, setting, codec, controller re-send, watch setting each tested | all present and passing | test classes above | build-and-test | Met |
| TC-FLOOR-1 | Testing Contract: existing suite green | 0 failures | 0 failures | `scripts/test-all.sh` exit 0 | build-and-test | Met |
| FR5.1 | requirements.md FR5.1 | en and fr OsmAnd captures confirm the layout | 0 unrecognised; outputs as designed | emulator section above | build-and-test | Met |
| FR6.1 | requirements.md FR6.1 | bundle carries `debugsymbols/` | no debugsymbols: the native libraries come already stripped from their AARs | bundleRelease output | build-and-test | Not Met |
| FR6.2 | requirements.md FR6.2 | release APK unchanged | byte-identical | sha256 comparison | build-and-test | Met |
| FR7.2 | requirements.md FR7.2 | watch row Full/Compact | as designed on basalt and aplite | `evidence/` | build-and-test | Met |
| NFR1 | requirements.md NFR1 | ≤ 64 / 24 bytes, fitted | `FixtureTextFitTest` passes | JVM tests | build-and-test | Met |
| NFR2 | requirements.md NFR2 | no extra sends for identical cards | controller test passes | `NavigationControllerTest` | build-and-test | Met |
| NFR3 | requirements.md NFR3 | < 5 ms per notification; validator limits | about 0.17 ms; validator passes | test XML timing | build-and-test | Met |
| NFR4 | requirements.md NFR4 | changed packs declare minimumAppVersionCode 83, bumped rulesetVersion | `RulesetVersionsTest` passes | JVM tests | build-and-test | Met |
| NFR5 | requirements.md NFR5 | full suite green | exit 0 | `scripts/test-all.sh` | build-and-test | Met |
| NFR6 | requirements.md NFR6 | no private data in the change | no hits beyond generic words | privacy check above | build-and-test | Met |
| NFR7 | requirements.md NFR7 | default Full: other apps' primaryText unchanged | regression fixtures unchanged and passing | Google Maps, Vela, CoMaps, Organic Maps regressions | build-and-test | Met |
| WATCH-HEAP | REQ-WATCH-025 / REQ-WATCH aplite budget | aplite free heap ≥ 3,282 B (≤ 200 B below 3,482 B) | 3,431 B | `scripts/build-watchapp.sh` | build-and-test | Met |
| CI | Testing Contract | CI before merge | not applicable: the plan runs no CI Pipeline stage (the repo's own CI still runs on the PR) | `aidlc-state.md` | n/a | N/A |
