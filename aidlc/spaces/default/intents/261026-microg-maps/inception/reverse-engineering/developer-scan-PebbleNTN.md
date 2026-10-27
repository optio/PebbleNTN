## Developer Code Scan Results

Repo `PebbleNTN`, intent `261026-microg-maps` (GitHub issue #113, "Compatibility with Microg Maps"), focused scan at 1c7f4c12d94dc7dab324eb4fd89180f25061eede, depth Minimal. Decision taken: MicroG Maps (`app.morphe.android.apps.maps`, Google Maps repackaged by the "Google Maps for MicroG-RE" Morphe patch) becomes one more package of the existing `google-maps` catalog entry, so every Google Maps rule applies to it. There is no custom-package feature. The maintainer's reply on #113 says the same: "make it an alias to Google Maps".

### Scan Coverage
- **Analyzed deeply**:
  - rules/catalog/navigation-apps.json
  - rules/bundled/google-maps/ (all 10 files: package lists, rule counts, `rulesetVersion`)
  - rules/fixtures/google-maps.json (package names, fixture count)
  - rules/fixtures/README.md
  - rules/README.md
  - schemas/app-catalog.schema.json
  - android/app/src/main/AndroidManifest.xml (`<queries>`)
  - android/app/src/main/java/com/pebblentn/app/catalog/
  - android/app/src/main/java/com/pebblentn/app/rules/RuleEngine.kt
  - android/app/src/main/java/com/pebblentn/app/rules/RuleRepository.kt
  - android/app/src/main/java/com/pebblentn/app/notification/NotificationDispatcher.kt
  - android/app/src/main/java/com/pebblentn/app/notification/NavigationNotificationListenerService.kt
  - android/app/src/main/java/com/pebblentn/app/notification/NotificationSnapshotFactory.kt
  - android/app/src/main/java/com/pebblentn/app/notification/icon/IconCandidates.kt
  - android/app/src/main/java/com/pebblentn/app/notification/icon/AppIconReferences.kt
  - android/app/src/main/java/com/pebblentn/app/notification/icon/LargeIconRecognizer.kt
  - android/app/src/test/java/com/pebblentn/app/catalog/
  - android/app/src/test/java/com/pebblentn/app/rules/GoogleMapsRulesRegressionTest.kt
  - android/app/src/test/java/com/pebblentn/app/rules/RulesetVersionsTest.kt
  - scripts/validate_catalog.py
  - scripts/validate_rules.py
  - scripts/validate-rules.sh
  - scripts/validate-spec-assets.sh
  - tools/rule-workbench/gen_localized_google_maps.py
- **Skimmed only**:
  - schemas/ruleset.schema.json (the `packageNames` block only)
  - the rest of android/app/src/main/java/com/pebblentn/app/rules/ (grep for package handling: `RulesetCodec`, `RuleValidator`, `RulePreviewService`)
  - the rest of android/app/src/main/java/com/pebblentn/app/notification/ (file list)
  - android/app/src/test/java/com/pebblentn/app/notification/ and the other files of android/app/src/test/java/com/pebblentn/app/rules/ (file list and a package-name grep)
  - rules/bundled/osmand/, rules/bundled/comaps/, rules/bundled/organic-maps/, rules/fixtures/{osmand,comaps,organic-maps}.json (package counts only)
  - requirements/REQ-RULES.md, requirements/REQ-ANDROID.md (package and catalog requirements by grep)
  - tools/rule-workbench/workbench.py, tools/route-capture/ (package-name grep)
  - scripts/test-all.sh (which validators it runs)
  - Outside the snapshot, read only for the touchpoints: MANIFEST.sha256.json (its key list), android/app/build.gradle.kts (`syncRuleData`, `syncTestFixtures`), android/app/src/main/java/com/pebblentn/app/data/EnabledAppRepository.kt, android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt, android/app/src/main/java/com/pebblentn/app/ui/rules/RulesViewModel.kt, android/app/src/main/java/com/pebblentn/app/ui/apps/NavigationAppsScreen.kt, android/app/src/main/java/com/pebblentn/app/ui/format/DisplayLabels.kt, AGENTS.md (rule 14), README.md, fastlane/metadata/android/en-US/full_description.txt, the #54 commit d73d903
- **Left out**:
  - scripts/__pycache__/
  - tools/__pycache__/
  - tools/rule-workbench/__pycache__/
  - tools/route-capture/__pycache__/
  - tools/route-capture/out/ (git-ignored)
  - the AI-DLC install (.claude/, aidlc/)

### Packages Found
- `com.pebblentn.app.catalog` — Model — Kotlin — `NavigationAppEntry`, `NavigationAppCatalog` (parse, `entryForPackage`, `allPackageNames`), `supportedNotInstalled`
- `com.pebblentn.app.rules` — Service (pure) — Kotlin — rule engine, layered rules, asset repository, codec, validator
- `com.pebblentn.app.notification` (+ `icon/`) — Controller/Service — Kotlin — listener, dispatcher (privacy gate), snapshot factory, rule-driven large-icon recognition
- `rules/` — data — JSON — catalog (19 entries, 26 package names), bundled rulesets per app and language, fixtures
- `schemas/` — data — JSON Schema — ruleset and catalog contracts
- `scripts/` — tooling — Python/Bash — catalog, rule and spec-asset validation
- `tools/rule-workbench/` — tooling — Python — workbench and the localized Google Maps generator

### Build System
- **Type**: Gradle (Android); Python and Bash scripts for data validation
- **Config Files**: android/app/build.gradle.kts (`syncRuleData` stages `rules/catalog` and `rules/bundled` as assets and test resources, lines 12-23; `syncTestFixtures` stages `rules/fixtures` for unit tests only, lines 27-34)
- **Build Dependencies**: `:app` assets ← `rules/catalog`, `rules/bundled`; `:app` unit tests ← `rules/fixtures`; `scripts/validate-rules.sh` → `validate_catalog.py` → `validate_rules.py`; `scripts/test-all.sh` → `validate-spec-assets.sh`, `validate-rules.sh`

### APIs Discovered
- Data contract — `schemas/app-catalog.schema.json` — one app object: `appId`, `displayName`, `packageNames` (1-20 strings), `hasOfficialRules`, `captureAvailable`, `defaultEnabled`, optional `channelHints`; `additionalProperties: false`, so there is no alias field
- Data contract — `schemas/ruleset.schema.json:134-141` — every rule's `packageNames`: 1-20 strings (also enforced in Kotlin, `RuleValidator.kt:45`)
- Kotlin — `NavigationAppCatalog.entryForPackage(packageName)` / `allPackageNames` (catalog/NavigationAppCatalog.kt:39-43)
- Kotlin — `RuleEngine.evaluate(snapshot, rules, locale, now)` (rules/RuleEngine.kt:76-113)
- Kotlin — `IconCandidates.forPackage(rules, packageName)`, `AppIconReferences.references(packageName, candidates)`, `LargeIconRecognizer.recognize(packageName, notification)`
- Android — `<queries>` package visibility list (AndroidManifest.xml:18-48)

### Frameworks & Libraries
- kotlinx-serialization-json — 1.7.3 — strict catalog and rule parsing (`ignoreUnknownKeys = false`)
- jsonschema (Python) — unpinned — catalog and rule schema validation
- JUnit 4 — 4.13.2 — catalog and regression tests
(Full list unchanged; see the store's dependencies.md.)

### Test Coverage
- **Test Directories**: android/app/src/test/java/com/pebblentn/app/{catalog,rules,notification}/; fixtures from rules/fixtures/ (staged as `/fixtures/...`)
- **Test Frameworks**: JUnit 4 (catalog and regression tests are plain JVM tests, reading classpath resources)
- **Coverage Config**: absent

### Code Quality Indicators
- **Linting**: Android Lint (unchanged); schema validation through `scripts/validate-rules.sh`
- **CI/CD**: `rules.yml` runs spec-asset integrity, schema validation and rule regression on rule changes (from the store; not rescanned)
- **Documentation**: KDoc on every class scanned, rationale comments citing issues (#54, #74, #83, #84); `rules/README.md` and `rules/fixtures/README.md` up to date except the `test-cases/` mention

### Technical Debt Signals
- The package list of an app lives in four places that must agree: the catalog entry, every rule of `rules/bundled/<appId>/`, the manifest `<queries>`, and the fixtures. Two checks guard it (a JVM test and a Python script); nothing generates them from one source.
- `tools/rule-workbench/gen_localized_google_maps.py:37` hard-codes one `PACKAGE`; running it rewrites the 7 generated languages with only `com.google.android.apps.maps` (line 186).
- `GoogleMapsRulesRegressionTest` hard-codes `com.google.android.apps.maps` in two tests (lines 156, 171); only the fixture-driven test reads the package from the fixture.
- `rules/README.md` mentions `rules/test-cases/`, which has no tracked content (known from the store).

## Facts relevant to #113

1. **How an app declares its packages.** `rules/catalog/navigation-apps.json:4-13`: `google-maps` has one package, `com.google.android.apps.maps` (line 8). Apps with several packages already exist: OsmAnd (`:27-32`, 4), Organic Maps (`:41-42`, 2) and CoMaps (`:51-56`, 4: `app.comaps.google`, `app.comaps.fdroid`, `app.comaps`, `app.comaps.huawei`). The catalog has 19 entries and 26 package names. `NavigationAppEntry.packageNames` is a list (catalog/NavigationAppCatalog.kt:22); `parse` rejects a package claimed by two apps (`:64-67`). So "a Google Maps variant" means appending `app.morphe.android.apps.maps` to the `google-maps` entry's `packageNames`, the same shape as #54's alternative builds.
2. **No alias mechanism: every rule must list the package.** `RuleEngine.evaluate` skips any rule whose own `packageNames` does not contain the snapshot's package (rules/RuleEngine.kt:86); `AssetRuleRepository` flattens the bundled files without consulting the catalog (rules/RuleRepository.kt:22-45); `IconCandidates.forPackage` filters the same way (notification/icon/IconCandidates.kt:21). Nothing maps a catalog package to another app's rules.
3. **The rule count.** All 10 files in `rules/bundled/google-maps/` list exactly `["com.google.android.apps.maps"]` in every rule: 150 rules (any 2, de 13, en 45, es 13, fr 13, it 13, nl 13, pl 13, pt 13, zh 12). Each needs `app.morphe.android.apps.maps` added. The schema and validator allow up to 20 packages per rule (`schemas/ruleset.schema.json:134-141`, `RuleValidator.kt:45`).
4. **The test that forces it.** `NavigationAppCatalogTest.everyBundledRuleCoversExactlyItsAppsPackages` (android/app/src/test/java/com/pebblentn/app/catalog/NavigationAppCatalogTest.kt:102-121) requires every rule in `rules/bundled/<appId>/` to list exactly the catalog entry's package set. Adding the package to the catalog alone fails this test; adding it to the rules alone fails it too. `captureOnlyMatchesTheBundledRules` (`:82-94`) stays green. `commonNavigationAppsAreDetectedAndCaptureOnly` (`:39-76`) is the package-to-appId lock table where the new package should get a row (`"app.morphe.android.apps.maps" to "google-maps"`).
5. **Manifest `<queries>`.** android/app/src/main/AndroidManifest.xml:18-48 lists the 25 navigation packages (Google Maps at line 19) plus two Obtainium packages; the comment at lines 14-17 says an app missing there is never discovered. `scripts/validate_catalog.py:67-90` fails when a catalog package is missing from `<queries>` (fixture publisher exempt, line 64) and prints "AndroidManifest <queries> covers all 25 catalog packages." (line 89); with the new package the count becomes 26. It runs from `scripts/validate-rules.sh:6`, so through `test-all.sh` and CI's rules job, not through the Gradle tests.
6. **Enablement is per app, not per package.** `EnabledAppRepository.syncInstalledApps` creates one settings row per catalog app when any of its packages is installed, stored under `packageNames.first()`; `refreshCache` enables every package of an enabled app (data/EnabledAppRepository.kt:48-80, outside the snapshot). MicroG Maps therefore shares the Google Maps switch and the Navigation apps row, the allowlist gate (`NotificationDispatcher.kt:26-30`) admits it, and an existing user's row is untouched. A phone with only MicroG Maps installed gets a Google Maps row created on discovery (REQ-ANDROID-004).
7. **Keep `com.google.android.apps.maps` first.** Several places read `packageNames.first()`: the settings row (`EnabledAppRepository.kt:56`), the Navigation apps "Get" link to Play (`NavigationAppsScreen.kt:117`), the Rules screen grouping (`RulesViewModel.kt:204-205`), the canonical rule order (`RulesetCodec.kt:56-60`) and user-rule storage (`UserRuleRepository.kt:52`). Appending the new package keeps them all as they are. The app icon picks the first installed package (`NavigationAppsScreen.kt:213-219`), so a MicroG-only phone shows the MicroG icon. Display names come from `entryForPackage` (`DisplayLabels.kt:114`), so the debug history shows "Google Maps" for MicroG events.
8. **Icons are looked up in the source package by name.** For the classic card (`google-maps-icon-step`, any.json, #74), `RuleDrivenLargeIconRecognizer.recognize` gets drawable candidates for the notification's package from the rules (notification/icon/LargeIconRecognizer.kt:29-34) and `AppIconReferences.render` opens that package (`createPackageContext(packageName)`, AppIconReferences.kt:36) and calls `getIdentifier(name, "drawable", packageName)` (line 38); the doc at line 16 says the package must be in `<queries>`. Nothing is keyed on the Google Maps package in code; once the rules list the MicroG package, its icons are tried with the MicroG package name. Whether `getIdentifier` finds `maneuver_*` under the renamed package depends on how the Morphe patch renames it (the resource table may keep `com.google.android.apps.maps`). That is unverified here; without a match, the card still shows distance, road and ETA with the fallback arrow (#83 comment in any.json).
9. **Regression tests and fixtures.** `rules/fixtures/google-maps.json` has 185 fixtures, all `com.google.android.apps.maps`. `GoogleMapsRulesRegressionTest` reads the package of each fixture (`GoogleMapsRulesRegressionTest.kt:94-104`), so new fixtures with `"packageName": "app.morphe.android.apps.maps"` run through the same engine with no code change. The #54 precedent adds one synthetic `package-variant-<package>` fixture per new package (commit d73d903, `rules/fixtures/comaps.json`). AGENTS.md rule 14 requires fixtures and regression tests for any rule change.
10. **Ruleset versions.** Each file carries `rulesetVersion` `google-maps-<lang>-<date>` (any 2026.10.16, en and zh 2026.10.19, de/es/fr/it/nl/pl/pt 2026.10.4). Nothing in the app compares versions of bundled rulesets: `rulesetVersionsFor` only records which rulesets hold a rule for the event's package (RuleRepository.kt:68-69, tested by RulesetVersionsTest.kt:34-40), so MicroG events will record the Google Maps versions once the rules list the package. The #54 change added packages to every OsmAnd ruleset without bumping `rulesetVersion` (commit d73d903). A bump is optional: it would only make the diagnostics show which build first matched MicroG.
11. **The generator.** `tools/rule-workbench/gen_localized_google_maps.py` emits it, fr, es, de, nl, pt and pl with `"packageNames": [PACKAGE]` (line 186, `PACKAGE` at line 37) and has no options (lines 234-242). It must take the package list too (or read the catalog), or a later regeneration silently drops MicroG and the catalog test fails.
12. **Protection.** `MANIFEST.sha256.json` protects `requirements/*.md`, `schemas/ruleset.schema.json` and `examples/*`, not `rules/`, the catalog, `schemas/app-catalog.schema.json` or the manifest. Adding the package touches no protected file. REQ-RULE-003 ("one or more exact package names", REQ-RULES.md:9-10), REQ-RULE-013 (Google Maps first, :39-40) and REQ-RULE-015 (rules selected by `packageNames`, never filename, :45-46) already allow it; no requirement lists supported packages. A new requirement or an amended REQ-RULE text would need the MANIFEST hash in the same commit (`scripts/validate-spec-assets.sh`).
13. **Other mentions (shallow).** The route-capture harness and icon probe know only the Google Maps package (`tools/route-capture/routecap/apps.py:116`, `device/icon-probe/AndroidManifest.xml:10`, `ProbeListener.java:27`); they are maintainer tools for the emulator and need no change. README.md:20 and `fastlane/metadata/android/en-US/full_description.txt:7` list Google Maps without variants; mentioning MicroG Maps there is a documentation choice.

## Handoff Summary
- **Intent-relevant finding**: there is no alias mechanism. Supporting MicroG Maps as a Google Maps variant means appending `app.morphe.android.apps.maps` to the `google-maps` catalog entry (rules/catalog/navigation-apps.json:7-9), to all 150 rules in the 10 `rules/bundled/google-maps/*.json` files (forced by NavigationAppCatalogTest.kt:102-121), and to the manifest `<queries>` (AndroidManifest.xml:18-48, checked by scripts/validate_catalog.py:67-90), plus at least one `package-variant` fixture in rules/fixtures/google-maps.json and a row in the package lock table (NavigationAppCatalogTest.kt:45-70). This is exactly the #54 pattern (commit d73d903). No Kotlin production code needs to change: the engine, the allowlist, enablement and icon lookup all work from the package lists.
- **Risks / follow-up**: (1) keep `com.google.android.apps.maps` first in every list (settings row, "Get" link, grouping, canonical order); (2) update `gen_localized_google_maps.py` so regeneration keeps the new package; (3) icon matching under the renamed package is unverified (`getIdentifier` with `app.morphe.android.apps.maps`) and needs a device check or a fallback to the catalog's first package; without it the classic card shows the fallback arrow; (4) `rulesetVersion` bumps are optional (#54 did none); (5) user rules already stored in Room list only the Google Maps package and will not apply to MicroG; (6) the validate_catalog count message moves from 25 to 26.
