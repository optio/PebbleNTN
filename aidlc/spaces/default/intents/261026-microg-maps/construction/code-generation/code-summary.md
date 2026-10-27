# Code Summary — MicroG Maps as a Google Maps variant (#113)

Express scope, zero Units, test-after (Testing Contract `sha256:bb76c1b2…`). Nothing is committed;
`versionCode` / `versionName` stay 77 / 0.0.77. No rule file and no rule package list changed.

## Files

**Catalog data and contracts**
- `rules/catalog/navigation-apps.json`: new entry `microg-maps` ("MicroG Maps",
  `app.morphe.android.apps.maps`, `rulesFrom: google-maps`, `website:
  https://github.com/fangkampanat/gmaps-patches`, `playListed: false`, official rules, capture,
  default enabled), placed right after Google Maps (FR2).
- `schemas/app-catalog.schema.json`: optional `rulesFrom` (appId pattern), `website` (https only),
  `playListed` (boolean, default true) (FR2).
- `android/app/src/main/AndroidManifest.xml`: `<queries>` gets `app.morphe.android.apps.maps`
  (26 navigation packages) (FR2).

**Android app**
- `catalog/NavigationAppCatalog.kt`: the three fields on `NavigationAppEntry` (+ `rulesAppId`);
  parse checks (`rulesFrom` not itself, an existing app, not a chain, an app with its own rules;
  `website` https); `ruleFamilies()`; `forGooglePlay()` (unlisted entries dropped, their packages
  appended to their `rulesFrom` entry, every website cleared); the new pure `RuleFamilies`
  resolver (`familyOf`, `applies`, `NONE`, `of`) (FR1, FR2, FR3).
- `rules/RuleEngine.kt`: `families` constructor parameter (default `RuleFamilies.NONE`) and
  `appliesTo(rule, packageName)`, used for rule selection (FR1).
- `rules/RuleRepository.kt`: `rulesetVersionsFor(…, families)` and an `AssetRuleRepository`
  `families` parameter, so MicroG events record the Google Maps ruleset versions (FR1).
- `notification/icon/IconCandidates.kt`, `notification/icon/LargeIconRecognizer.kt`: the icon table
  comes from the family's rules; the drawables are still rendered from the posting package by
  `AppIconReferences` (unchanged) (FR1).
- `data/RuleOverrides.kt`: the takes-over check uses `engine.appliesTo` (FR1, user rules).
- `di/AppContainer.kt`: install source detected first; `catalog` is `forGooglePlay()` on
  `InstallSource.PLAY`; one family-aware `RuleEngine` for the listener and the rule preview;
  families passed to the asset repository and the icon recognizer (FR1, FR3).
- `ui/rules/RulesViewModel.kt`: override analysis with a family-aware engine (FR1).
- `data/AppEnablement.kt`, `data/EnabledAppRepository.kt`: `rulesAppId` on each row; KDoc on how
  Play enablement follows the Play view (FR2, FR3). Enablement logic itself is unchanged.
- `ui/apps/NavigationAppsScreen.kt`: languages and "View its rules" use `rulesAppId`; an
  uninstalled app with a `website` opens it through the new `onOpenWebsite`, with the accessibility
  action "Open the … website"; every other app keeps the Google Play link (FR2).
- `ui/MainActivity.kt`: `onOpenWebsite = ::openUrl` (FR2).
- `res/values/strings.xml`: `navigation_apps_get_website` (FR2).

**Tooling**
- `scripts/validate_catalog.py`: `rule_family_errors` (unknown, self, chain, target without rules,
  a folder for a `rulesFrom` app, duplicate packages) and testable `missing_manifest_packages`;
  prints "covers all 26 catalog packages" (FR2).
- `scripts/test_validate_catalog.py` (new, 10 tests) and a `scripts/test-all.sh` step running it.
- `tools/rule-workbench/workbench.py`: `rule_families` / `rule_applies` mirror the Kotlin
  families, so the workbench regression runs the MicroG fixtures too (FR1).

**Tests**
- `catalog/NavigationAppCatalogTest.kt`: lock-table row `app.morphe.android.apps.maps →
  microg-maps`; `captureOnlyMatchesTheBundledRules` judges by family; renamed
  `everyBundledRuleCoversExactlyItsRootAppsOwnPackages` (+ no folder for a `rulesFrom` app); 11 new
  tests (MicroG entry, rejections, defaults, families, Play view).
- `rules/RuleFamiliesMatchingTest.kt` (new, 12): icon-only, English and French cards and every
  Google Maps fixture give the same result for MicroG; no bundled rule lists the MicroG package;
  unrelated and unknown packages match nothing; user rules both ways; override analysis; icon
  table; ruleset versions.
- `rules/GoogleMapsRulesRegressionTest.kt`: engine built with the catalog's families.
- `rules/fixtures/google-maps.json`: four `package-variant-app.morphe.android.apps.maps-*`
  fixtures (icon card, English, French, "Starting navigation…" no match).
- `data/MicrogMapsEnablementTest.kt` (new, 5): own row and switch, independent switches; on Play
  shown as Google Maps, Google Maps on → handled with `google-maps-turn-right-fr`, off → ignored.
- `ui/apps/MicrogMapsNavigationAppsTest.kt` (new, 5, Robolectric Compose): GitHub website link and
  label, others on Google Play, installed row shows/opens the Google Maps rules, debug labels, Play
  shows no MicroG row or link.
- `ui/dashboard/SetupProgressTest.kt`: "Also supported" now lists `microg-maps` among the apps with
  directions on non-Play installs, and not on Play.

**Docs and specs**
- `README.md`: separate item "MicroG Maps (Google Maps patched for microG), with the Google Maps
  rules", linking to the patch project (FR4). `fastlane/…/full_description.txt` unchanged.
- `requirements/REQ-RULES.md`: REQ-RULE-015 amended (rule families, acceptance).
  REQ-RULE-003 needed no change (a rule still lists one or more exact packages).
- `requirements/REQ-ANDROID.md`: new REQ-ANDROID-023 (app variants: `rulesFrom`, `website`,
  `playListed`, behaviour per install source).
- `spec/200-architecture/Android.md` (catalog section), `rules/README.md`.
- `MANIFEST.sha256.json`: README, REQ-ANDROID, REQ-RULES and Android.md resynced;
  `scripts/validate-spec-assets.sh` passes (34 files).
- `docs/IMPLEMENTATION_STATUS.md`: new entry.

## Decisions

- **Field names:** `rulesFrom` (as planned and as named in FR1), `website`, `playListed`. They say
  what each does at the place it is read; `playListed` defaults to true so no other entry changes.
- **One Play switch point:** instead of filtering in each screen, `AppContainer` gives the whole app
  the catalog's Google Play view. Enablement, Navigation apps, "Also supported", the install
  receiver and the debug labels then need no Play branch, and MicroG Maps' package simply belongs to
  the Google Maps row on Play (its row is created when only MicroG Maps is installed). On Play a
  MicroG event is labelled "Google Maps", so the patched app is never named there.
- **Families everywhere a rule meets a package:** engine, icon table, ruleset versions, overrides,
  preview. `RuleFamilies.NONE` keeps every other caller (and the remote-ruleset test vectors in
  `RuleUpdateRepository`) exactly as before.
- **`hasOfficialRules: true` for MicroG Maps:** it gets official rules through its family, so it is
  not capture-only; the catalog test now checks this by family.
- **A family is the whole app**, as Q1 option A says: for OsmAnd, Organic Maps and CoMaps nothing
  changes for bundled rules (they list every package), but a user rule listing one of their packages
  now applies to all packages of that app.

## Coverage

Minimal strategy: one or more tests per requirement (FR1: `RuleFamiliesMatchingTest`, catalog
family tests, regression fixtures; FR2: catalog parser tests, Compose test, validator tests; FR3:
`MicrogMapsEnablementTest`, Play Compose test, Play catalog view test; FR4:
`validate-spec-assets.sh`), happy path per new component, two or more edge cases for the parser and
the resolver. No line-coverage floor in express.

Results: unit tests 537 (debug) and 537 (release), 0 failures, 4 skipped (baseline 504/0/4);
`scripts/test-all.sh` exit 0 (lint 0 errors; the warnings on changed files are older ones); workbench regression google-maps
195/195; catalog validator 10/10; `:app:assembleDebug` builds and the APK carries the new catalog.

## Tests not run

- No emulator (no KVM in WSL) and no phone: nothing on a device. Not verified: the Navigation apps
  screen on a real GitHub and Play install, opening the website in a browser, and a real MicroG Maps
  notification.
- `connectedDebugAndroidTest` and `scripts/build-watchapp.sh` were not run (no watch-side change).

## Deviations

- `scripts/test_validate_catalog.py` and its `test-all.sh` step were added so the validator's
  failure cases are tested without editing real files.
- `tools/rule-workbench/workbench.py` changed (not named in the plan): without it the workbench
  regression in `test-all.sh` fails on the MicroG fixtures.
- `ui/dashboard/SetupProgressTest.kt` was updated: its expected "Also supported" list gains MicroG
  Maps on non-Play installs, which is the intended change.
- `spec/200-architecture/Android.md` and `rules/README.md` updated for consistency (Android.md is
  MANIFEST-protected, resynced).
- `tools/rule-workbench/gen_localized_google_maps.py` unchanged: with families, regeneration no longer
  needs to know about MicroG Maps.

## Concerns

- **Icon-only cards under the renamed package.** `AppIconReferences` still calls
  `createPackageContext("app.morphe.android.apps.maps")` and `getIdentifier("maneuver_…",
  "drawable", "app.morphe.android.apps.maps")`. If the Morphe patch renamed only the manifest
  package and the resource table still uses `com.google.android.apps.maps`, `getIdentifier` returns
  0, no reference renders, and the classic card shows distance, road and ETA with the fallback
  arrow (text cards are unaffected). Only a device with MicroG Maps can tell; a fallback (retrying
  under the root app's first package name inside the same package context) would be a small,
  separate change once a debug export shows it is needed.
- The Play build's manifest also declares the MicroG package in `<queries>` (one manifest for every
  source). It is invisible to users and needed for the Play behaviour of FR3.
- A user rule for OsmAnd, Organic Maps or CoMaps that lists only one package now applies to that
  app's other packages (Q1 A); bundled behaviour is unchanged.
