# Developer Code Scan: Vela Maps (#120)

Focused scan at c7b6ba0 (v0.0.80) for intent `261030-vela-maps`: add Vela Maps (`app.vela`) as a navigation app with English rules and synthetic fixtures, and add it to the route-capture suite.

## Developer Code Scan Results

### Scan Coverage
- **Analyzed deeply**:
  - rules/catalog/
  - rules/bundled/
  - rules/fixtures/
  - rules/README.md
  - tools/route-capture/
  - android/app/src/test/java/com/pebblentn/app/rules/
  - android/app/src/test/java/com/pebblentn/app/catalog/
  - scripts/validate_catalog.py
  - scripts/validate_rules.py
  - scripts/test_validate_catalog.py
  - android/app/src/main/AndroidManifest.xml
  - requirements/REQ-RULES.md
- **Skimmed only**: `android/app/src/main/java/com/pebblentn/app/rules/` (RuleEngine, ConditionEvaluator, ExtractorRunner, DistanceParser, SafeRegex, SnapshotFields, RuleRepository, RuleValidator, RulesetModel); `android/app/src/main/java/com/pebblentn/app/catalog/NavigationAppCatalog.kt` (`forGooglePlay`, `parse`); `android/app/src/main/java/com/pebblentn/app/ui/apps/NavigationAppsScreen.kt` (website link); `android/app/src/main/java/com/pebblentn/app/notification/ManeuverHeuristic.kt`; `android/app/src/main/java/com/pebblentn/app/core/Maneuver.kt`; `android/app/build.gradle.kts` (rule-data staging); `android/app/src/test/java/com/pebblentn/app/ui/dashboard/SetupProgressTest.kt`; `schemas/ruleset.schema.json`; `schemas/app-catalog.schema.json`; `tools/rule-workbench/workbench.py`; `scripts/validate-rules.sh`; `README.md`; `fastlane/metadata/android/en-US/full_description.txt`; `MANIFEST.sha256.json` (file list); `AGENTS.md` (rule list); git history of earlier app additions (bf6d68c, b3d9932, faf1e71). Outside the repository, read-only: GitHub issue #120 with its comments, and the public Vela repository (`PimpinPumpkin/Vela`: `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `NavigationService.kt`, `ui/Format.kt`, `values/strings.xml`, the releases list).
- **Left out**: `tools/route-capture/out/` (run output, git-ignored), `__pycache__/`, `tools/route-capture/routes/cache/` (fetched OSM tracks, listed only), the prebuilt `device/*.dex` and `device/icon-probe/*.apk` binaries, Android `build/` and `.gradle/`, and the AI-DLC install.

### Packages Found
- `rules/` — data — JSON — catalog (20 entries, 28 packages, 27 of them queried), bundled rulesets for 4 apps (google-maps 10 files, osmand 9, comaps 1, organic-maps 1), fixtures per app (google-maps 189, osmand 195, comaps 21, organic-maps 12)
- `tools/route-capture/` — CLI — Python 3.10+ (stdlib) — emulator route-replay harness: `routecap.py` plus `routecap/{apps,geo,host,issues,notifications,report,ui}.py`, 7 European routes, 6 scenario plans, `tests/test_routecap.py`
- `android/app/src/test/.../rules/` — tests — Kotlin — 15 files, one `<App>RulesRegressionTest` per app with rules
- `android/app/src/test/.../catalog/` — tests — Kotlin — `NavigationAppCatalogTest`, `EffectiveCatalogTest`, `CustomPackageValidatorTest`
- `scripts/validate_*.py` — validators — Python + jsonschema

### Build System
- **Type**: Gradle (Android), plain Python scripts (rules, tools)
- **Config Files**: `android/app/build.gradle.kts` (`syncRuleData` stages `rules/catalog/*.json` and `rules/bundled/**/*.json` as assets and test resources; `syncTestFixtures` stages `rules/fixtures/**/*.json` as test resources only), `scripts/validate-rules.sh`
- **Build Dependencies**: a new `rules/bundled/vela-maps/en.json` and `rules/fixtures/vela-maps.json` are picked up with no build change (recursive globs).

### APIs Discovered
- Rule JSON contract — `schemas/ruleset.schema.json` — 10 condition operators, 9 extractor types, 6 output fields
- Catalog JSON contract — `schemas/app-catalog.schema.json` — 10 app fields (`rulesFrom`, `website`, `playListed` optional)
- route-capture CLI — `routecap.py` — `setup`, `fetch-routes`, `install`, `run`, `report`
- Vela intents (external, from its manifest) — VIEW `geo:`, VIEW `google.navigation:`, Google Maps https links, car-app NAVIGATE `geo:`

### Frameworks & Libraries
- kotlinx-serialization — 1.7.3 — strict rule and catalog parsing (unknown keys rejected)
- java.util.regex via `SafeRegex` — JDK — rule regexes with a 50 ms budget, 1024-char pattern cap
- jsonschema — unpinned — `Draft202012Validator` in both validators
- Python `re` — stdlib — workbench engine subset used by route-capture

### Test Coverage
- **Test Directories**: `android/app/src/test/java/com/pebblentn/app/{rules,catalog}/`, `scripts/test_validate_catalog.py`, `tools/route-capture/tests/`
- **Test Frameworks**: JUnit 4, Python unittest
- **Coverage Config**: absent

### Code Quality Indicators
- **Linting**: Android Lint only; none for the Python tools
- **CI/CD**: `rules.yml` (schema, regression), `android.yml` (JVM tests); `scripts/test-all.sh` runs the route-capture unit tests locally
- **Documentation**: good; `rules/fixtures/README.md` records what each real capture taught, `tools/route-capture/README.md` documents each app driver

### Technical Debt Signals
- `tools/rule-workbench/workbench.py:394` hard-codes the regression app list `("google-maps", "comaps", "organic-maps", "osmand")`; a new app is skipped by `regression` until it is added there.
- `tools/route-capture/routecap/apps.py:256` `APPS` is a hand-kept tuple of drivers; the README "Apps" and "Plans" tables repeat it.
- Counts pinned by hand: `scripts/test_validate_catalog.py:70-72` (27 queried packages), `NavigationAppCatalogTest.kt:283` (27 on Play), `SetupProgressTest.kt:30` and `:34` (the ordered list of apps with directions).
- `OrganicMapsRulesRegressionTest` declares `Expected.secondaryText` but never asserts it, and `OsmandRulesRegressionTest` has no ETA check; `GoogleMapsRulesRegressionTest:135-136` and `ComapsRulesRegressionTest:114` assert the ETA line.
- The workbench `field_value` does not map Unicode spaces to ASCII as Kotlin `SnapshotFields.normalizeSpaces` does; Python's Unicode-aware `\s` hides the difference for `\s` patterns but not for a literal space.
- `schemas/app-catalog.schema.json` describes `playListed: false` only for an app with `rulesFrom` ("Its packages then follow the switch of the rulesFrom app"); Vela would be the first unlisted app without `rulesFrom`.

## Registration points for a new navigation app

What adding `vela-maps` touches (pattern from b3d9932 Organic Maps rules, faf1e71 capture-only apps, bf6d68c MicroG Maps):

| # | File | Change | Enforced by |
|---|---|---|---|
| 1 | `rules/catalog/navigation-apps.json` | new entry (alphabetical position is free; `pebblentn-fixtures` is last) | `validate_catalog.py` schema, `NavigationAppCatalog.parse` |
| 2 | `android/app/src/main/AndroidManifest.xml:18-50` | `<package android:name="app.vela" />` in `<queries>` | `validate_catalog.py check_manifest_queries` (fails otherwise) |
| 3 | `rules/bundled/vela-maps/en.json` | ruleset; every rule's `packageNames` must equal `["app.vela"]` | `NavigationAppCatalogTest.everyBundledRuleCoversExactlyItsRootAppsOwnPackages` (folder name = `appId`), `captureOnlyMatchesTheBundledRules` (`hasOfficialRules: true`), `validate_rules.py` |
| 4 | `rules/fixtures/vela-maps.json` | fixtures (`note`, `fixtures[]`) | AGENTS.md rule 14, REQ-RULE-012 |
| 5 | `android/app/src/test/.../rules/VelaMapsRulesRegressionTest.kt` (new) | copy of the per-app regression test; should assert `secondaryText` and `primaryText` | — |
| 6 | `tools/rule-workbench/workbench.py:394` | add `"vela-maps"` to the regression tuple | — |
| 7 | `android/app/src/test/.../catalog/NavigationAppCatalogTest.kt:45-70` | lock-table row `"app.vela" to "vela-maps"`; optionally assert Vela is absent on Play | — |
| 8 | `scripts/test_validate_catalog.py:70-72` | 27 → 28 queried packages (test name and comment) | the test itself |
| 9 | `android/app/src/test/.../ui/dashboard/SetupProgressTest.kt:30` | `withDirections` becomes `comaps, microg-maps, organic-maps, osmand, vela-maps`; the Play list at `:34` stays | the test itself |
| 10 | `NavigationAppCatalogTest.kt:283` | stays 27: on Play an unlisted app without `rulesFrom` is dropped entirely (`NavigationAppCatalog.kt:125`, `forGooglePlay`), so `app.vela` is not on the Play allowlist; the comment "nothing is lost" becomes inaccurate | the test itself |
| 11 | `README.md:20-21`, `fastlane/metadata/android/en-US/full_description.txt:7-10` | list Vela Maps (optional; README lists MicroG Maps, fastlane lists every app with rules) | — |
| 12 | `requirements/REQ-RULES.md` | no requirement lists apps; REQ-RULE-013 allows evidence-backed rules for other apps. A new REQ is optional; `REQ-RULES.md` and `README.md` are MANIFEST-protected, so editing either needs its `MANIFEST.sha256.json` hash updated by hand | `scripts/validate-spec-assets.sh`, pre-commit |
| 13 | `rules/fixtures/README.md` | optional Vela section (provenance: synthetic, derived from Vela's source strings) | — |
| 14 | `docs/IMPLEMENTATION_STATUS.md` | status entry (CLAUDE.md) | — |

Not needed: the Android UI is catalog-driven (display name, Get link through `website`, languages from the ruleset, no per-app strings or icons); `RuleRepository` loads `rules/bundled/**` recursively; no main-source Kotlin names any navigation app; `RulesetVersionsTest` is synthetic.

## Issue #120 rules against the schema and the engine

**Schema and parser**: the proposal validates as written. Operators `equalsIgnoreCase` and `regex`, extractors `literal`, `distance`, `regexCapture` (with `group`) and `firstNonEmpty` are all in the schema and in `RulesetModel`; every maneuver literal (`ARRIVE`, `RIGHT`, `LEFT`, `SLIGHT_*`, `SHARP_*`, `UTURN_LEFT`, `ROUNDABOUT`, `STRAIGHT`, `UNKNOWN`) is a `Maneuver` token; ids match `^[a-z0-9]+(?:-[a-z0-9]+)*$`. Not run (no programs run in this scan), checked by reading.

**What Vela really posts** (`NavigationService.buildNotification`, `values/strings.xml:857-861, 1117`, `ui/Format.kt:36-45`):
- title: `Arrived`; `Navigating` while the maneuver text is empty; `In %1$s · %2$s` while the distance to the next maneuver is above 0; otherwise the bare maneuver text.
- distance: `"<n> ft"` (10/50 ft steps, never 0) or `"%.1f mi"` in imperial, `"<n> m"` or `"%.1f km"` in metric; `Locale.US` decimals and an ASCII space, so `DistanceParser` reads all four.
- text: `"<duration> · <distance> · Arrive <time>"`, then optionally ` · To <stop>` and ` · <n> min faster available` or ` · faster route available`; empty on arrival. `<time>` is `DateFormat.SHORT` ("12:45" or "12:45 PM", possibly with U+202F, which `SnapshotFields` turns into a space).
- maneuver text: the routing engine's instruction string; the repository mentions Valhalla, OSRM and GraphHopper, so the phrasing depends on the selected router.
- large icon: a bitmap drawn at runtime (`NavGlyphs.bitmap`), not an app drawable, so `iconDrawable` matching (#74) cannot help.
- 24 `values-*` folders: Vela localizes the title prefix and the ETA word, so English rules match only an English device (the rules' `locales: ["en"]` agrees).

**Rule-quality problems** (evaluation order is priority descending, then id ascending, `RuleEngine.kt:24`):
1. **Bare titles lose their arrow.** At distance 0 the title is just the instruction ("Turn right onto Main St"), and every maneuver rule requires `^In\s+`, so it falls to `vela-bare-maneuver-en` (UNKNOWN). Dropping the `^In` condition from the maneuver rules (distance and road then come out empty or whole) would keep the arrow.
2. **Motorway exits as roundabouts.** `vela-in-distance-exit-en` and `vela-in-distance-roundabout-en` both have priority 150; the exit rule goes first by id but only matches `take (the )?exit`, `exit highway`, `take the ramp`. A Valhalla-style "Take the I 35 South exit toward Dallas" fails it and then matches the roundabout's `take the .+ exit`, giving ROUNDABOUT.
3. **Exits and ramps are always right.** The exit rule outputs SLIGHT_RIGHT for "Take the exit on the left" / "Take the ramp on the left".
4. **U-turns are always left**: `UTURN_LEFT` for "Make a right U-turn"; `UTURN_RIGHT` exists.
5. **Turn rules outrank the roundabout**: `turn right` (160) also matches a roundabout instruction phrased "At the roundabout, turn right" (OSRM style), giving RIGHT.
6. **No arrival with a distance.** "In 500 ft · Your destination is on the right" / "Arrive at …" end in the generic rule (UNKNOWN); Google Maps' and OsmAnd's English rules map destination phrasing to ARRIVE. `Arrive` in the bare rule also gives UNKNOWN.
7. **Missing verbs**: "Bear left/right" (Valhalla), "Turn slight …" is covered, "Keep straight" is STRAIGHT; "Stay"/"Exit the roundabout" fall to the generic rule.
8. **`head|continue|merge` → STRAIGHT**: consistent with the bundled Google Maps `merge` rules (STRAIGHT), but `\bhead\b` / `\bcontinue\b` also match road names ("Continue Ave"); low risk at priority 140.
9. **`·` separator**: Vela's format string has a space on both sides, so `(?i)^In\s+.+?\s·\s*(.+)$` works; `Navigating` matches no rule (as intended, but see the route-capture note below).
10. **secondaryText**: only the ETA clock ("12:45" / "12:45 PM"), the same convention as Google Maps (`google-maps-*` rules put the time in `secondaryText`, none set `etaEpochSeconds`). With a stop ahead the clock is the stop's ETA. The fastlane text says "no ETA" for the other open-source apps; Vela would have one.
11. **Duplication**: the `primaryText`/`secondaryText` extractors are repeated in 12 rules; harmless, but a pattern change must be made 12 times.

**Fixture format** (`rules/fixtures/<appId>.json`, staged as `/fixtures/<appId>.json`): `{"note", "fixtures": [{name, source: capture|synthetic, locale, packageName, snapshot: {title, text, subText, bigText, iconDrawable}, comment?, expected: {matched, maneuver, distanceMeters, ruleId, primaryText, secondaryText, stopsRemaining, noDistance, noPrimaryText}}]}`. Run by the Kotlin `*RulesRegressionTest` (per app, hand-written) and by `workbench.py regression` / `test`. A `matched: false` fixture also tells route-capture that a card is left unshown on purpose (`report.known_unmatched`), for example `Navigating`.

## Route-capture suite

- **How it works**: `run <plan>` boots an Android 17 Play emulator (on Windows from WSL), and per scenario (`app` × `locale` × `mode` × `route`) provisions maps, sets permissions and the system language, puts the GPS at the origin, starts navigation through an app driver, dismisses first-run dialogs by text or resource id (`apps.py:16-27`), feeds one GPS fix per second along a committed OSM track, and keeps every distinct notification of `app.package` from `dumpsys notification --noredact`.
- **"Unrecognised"**: `report.evaluate` runs each card through the workbench engine with `rules/bundled/<appId>/*.json`. A card no rule matches that looks like a direction (`ManeuverHeuristic` words, read from the Kotlin source, or a clock time, and not a status card) is "unrecognised" and grouped by shape; matches with an UNKNOWN maneuver are listed as "fallback" for review; cards pinned by a `matched: false` fixture are "known". With `--publish`, one GitHub issue per app (label `route-capture`) is created or merged per scenario.
- **Drivers today** (`apps.py:256`): Google Maps (`google.navigation:` deep link), OsmAnd (pushed `.obf` map, `google.navigation:`, Start button by id), Organic Maps and CoMaps (`om://` / `cm://route`, in-app map downloads, positive buttons by id). Install sources: `Preinstalled`, `FDroid`, `ReleaseAsset` (GitHub/Codeberg API, regex on the asset name).
- **What Vela needs**: a `VelaMaps(App)` with `app_id = "vela-maps"`, `package = "app.vela"`, `source = ReleaseAsset("https://api.github.com/repos/PimpinPumpkin/Vela/releases?per_page=5", r"^vela-maps-x86_64\.apk$")` (the list starts with a `canary` pre-release whose assets are `vela-maps-canary-*`, and the latest stable `v0.5.2677` has per-ABI APKs; the anchored pattern skips canary); a `start_navigation` sending `google.navigation:q=<lat>,<lon>&mode=…` (or `geo:`) to `app.vela`, which its manifest accepts; an `APPS` entry; a scenario plan such as `scenarios/vela-maps-m1.json` (English first: en-GB London, en-US needs a new US route plus `fetch-routes`); the README "Apps" and "Plans" rows; and a `matched: false` fixture for `Navigating` if it shows a clock (its text carries "Arrive <time>", so `looks_like_direction` counts it as missing).
- **Unknown until tried on the emulator**: whether the deep link starts guidance directly or opens a preview (its Start button id and first-run dialogs, which need `FIRST_RUN_IDS`/labels), whether `mode` is honoured, whether Vela needs offline data or works with online routing in the emulator, which router it uses by default (it decides the instruction wording), and whether it runs on the Android 17 Play image. `is_instruction` accepts Vela cards through the clock in the text (its distance-led check wants a title starting with a number, not `In`). The harness must not be run inside WSL (memory: the Android 17 image crashed WSL).

## Handoff Summary
- **Intent-relevant finding**: Vela is a 4-place registration (catalog, manifest `<queries>`, `rules/bundled/vela-maps/en.json`, `rules/fixtures/vela-maps.json`) plus a new `VelaMapsRulesRegressionTest`, the workbench regression tuple (`workbench.py:394`) and three hand-pinned test values (`test_validate_catalog.py:72` 27→28, `SetupProgressTest.kt:30`, `NavigationAppCatalogTest.kt` lock table); the issue's rules validate against the schema, and Vela's own source (`NavigationService.kt`, `strings.xml:859-861`, `Format.kt:36-45`) confirms the title and text formats, but the rules drop the arrow on bare-instruction titles, can classify motorway exits as ROUNDABOUT, always give SLIGHT_RIGHT for exits and UTURN_LEFT for U-turns, and have no ARRIVE for an approaching destination.
- **Risks / follow-up**: `playListed: false` without `rulesFrom` hides Vela entirely on a Google Play install (no allowlist, notifications ignored; first such entry, `NavigationAppCatalog.kt:125`); all fixtures are synthetic (no real capture; instruction wording depends on Vela's router), so `source: synthetic` and a request for a diagnostics export from the reporter (offered on #120) stand; README and REQ-RULES edits need `MANIFEST.sha256.json` hashes; the route-capture driver's start flow and dialogs are unverified until run on the Windows emulator.
