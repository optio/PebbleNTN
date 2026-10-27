# Code Generation Plan — MicroG Maps as a Google Maps variant (#113)

## Summary

- **Builds:**
  - rule families: a catalog entry can reuse another entry's rules, and a rule applies to every package in its family;
  - a MicroG Maps catalog entry with a website link;
  - the Navigation apps row, hidden on Play installs;
  - the manifest query;
  - catalog validation;
  - README and specs.
- **Touches:**
  - catalog: `rules/catalog/navigation-apps.json`, `schemas/app-catalog.schema.json`;
  - Android: `catalog/`, `rules/` (`RuleEngine`, the repository or a new family resolver), `notification/` where the package gates rules or icons, `data/EnabledAppRepository`, `ui/` (Navigation apps, debug labels), `AndroidManifest.xml`, `strings.xml`;
  - tests;
  - scripts: `scripts/validate_catalog.py`;
  - docs and specs: `README.md`, `requirements/REQ-RULES.md` (and REQ-ANDROID where it describes the catalog or the Navigation apps list), `MANIFEST.sha256.json`, `docs/IMPLEMENTATION_STATUS.md`.
- **Tests:** JVM tests for the family resolution, rule matching, user rules, enablement and the UI; catalog validation; the full `scripts/test-all.sh`.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR4, NFRs). The facts list is in `inception/reverse-engineering/developer-scan-PebbleNTN.md`. Delivery is decided at Deployment: this goes into the next normal release.

### Design decisions

**Catalog (FR2):**
- **New optional fields on an app entry:**
  - `rulesFrom` (the `appId` whose rules it reuses);
  - `website` (an https URL that replaces the Google Play "Get" link);
  - `playListed` (default true; false hides the entry on Play installs).
- **The new entry:**
  ```json
  {"appId":"microg-maps","displayName":"MicroG Maps","packageNames":["app.morphe.android.apps.maps"],"rulesFrom":"google-maps","website":"https://github.com/fangkampanat/gmaps-patches","playListed":false,"hasOfficialRules":true,"captureAvailable":true,"defaultEnabled":true}
  ```
  `hasOfficialRules` / `captureAvailable` follow whatever the existing flags mean for an app whose rules come from another entry; adjust after reading `NavigationAppCatalog`.
- **Parse checks:**
  - `rulesFrom` names an existing entry, not itself and not a chain;
  - the entry it names has its own rules;
  - an entry with `rulesFrom` has no `rules/bundled/<appId>/` folder;
  - packages stay unique across entries.
- **Schema:** update the schema, the Kotlin parser and `scripts/validate_catalog.py`. The manifest `<queries>` check covers 26 packages.

**Rule family (FR1):**
- **The resolver:** a pure resolver built from the catalog, `RuleFamilies` (or functions on `NavigationAppCatalog`). It maps every package to its family:
  - the set of packages of the root app;
  - plus the packages of every entry with `rulesFrom` pointing to it.
  - A package not in the catalog is its own family.
- **`RuleEngine`:** a rule is a candidate for a snapshot when `family(snapshot.packageName)` intersects the rule's `packageNames`, instead of an exact list match. Do the same wherever rules are filtered by package:
  - `IconCandidates.forPackage`;
  - `rulesetVersionsFor`;
  - the Rules screen preview or test tools that evaluate a rule for a package.
- **Icon rendering** still uses the real posting package (`AppIconReferences`).
- **User rules** go through the same engine, so they follow automatically. Check `UserRuleRepository` and any storage keyed on the first package.

**Enablement and UI (FR2, FR3):**
- **Non-Play installs:** MicroG Maps is its own row with its own switch when installed, the stored row keyed by its package as for other apps. The "Get" or open link uses `website` when present.
- **Play installs:** entries with `playListed:false` are filtered out of Navigation apps and any app-picker UI. Their packages are enabled exactly when their `rulesFrom` root app is enabled (allowlist gate).
- **Labels:** the debug history and export label MicroG Maps events "MicroG Maps", via `entryForPackage`.
- **Strings:** all new strings are resources, with accessibility labels for the link.

**Tests and validation:**
- `NavigationAppCatalogTest`: replace "every bundled rule lists exactly its app's packages" with "every bundled rule lists exactly its root app's own packages, and an entry with `rulesFrom` has no folder". Add a lock-table row `app.morphe.android.apps.maps → microg-maps`.
- Fixtures: add a few `package-variant-app.morphe.android.apps.maps` fixtures to `rules/fixtures/google-maps.json` (one classic icon-only card, one English text card, one other language). `GoogleMapsRulesRegressionTest` must pass them unchanged.

**Docs (FR4):** add a README supported-apps item "MicroG Maps (Google Maps patched for microG; uses the Google Maps rules)", linking to the patch project. The fastlane description is unchanged.

## Steps

- [x] 1. Add an entry to `docs/IMPLEMENTATION_STATUS.md` (milestone, requirements, commands, blockers, next task).
- [x] 2. Baseline: run `android/gradlew -p android :app:testDebugUnitTest` and `scripts/validate-rules.sh`; record the results.
- [x] 3. Catalog data and schema: the new fields, the MicroG Maps entry, `scripts/validate_catalog.py`, and the manifest `<queries>` entry.
- [x] 4. Kotlin catalog: parse the new fields with validation; add the family resolver. Tests: the parser accepts and rejects (missing `rulesFrom` target, a chain, a self reference, a folder for a `rulesFrom` entry), and the family resolution covers Google Maps, MicroG Maps, CoMaps and an unknown package.
- [x] 5. Rule matching: `RuleEngine`, `IconCandidates`, `rulesetVersionsFor` and any other per-package filter use the family. Tests: a MicroG notification gives the same result as Google Maps for an icon-only card and an English and a non-English text card; a user rule for either package applies to both; an unrelated package matches nothing.
- [x] 6. Fixtures and the catalog test changes; `GoogleMapsRulesRegressionTest` and `NavigationAppCatalogTest` pass.
- [x] 7. Enablement and UI: the MicroG row with its website link on non-Play installs; hidden on Play, with its packages following Google Maps' switch; debug labels. Tests: Compose/JVM tests for both install sources, and allowlist tests on Play (Google Maps on → MicroG handled; off → ignored).
- [x] 8. README item; amend REQ-RULE-015 (and REQ-RULE-003 if needed) and the catalog / Navigation apps requirement; resync the MANIFEST; run `validate-spec-assets.sh`.
- [x] 9. Run the full `scripts/test-all.sh` and build `:app:assembleDebug`.
- [x] 10. Write `code-summary.md` and `traceability.json`. Don't commit.

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
