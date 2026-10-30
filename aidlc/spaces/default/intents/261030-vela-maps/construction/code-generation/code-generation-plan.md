# Code Generation Plan — Vela Maps support (#120)

## Summary

- **Builds:**
  - the Vela Maps catalog entry and English rules (with the gaps fixed), with synthetic fixtures and a regression test;
  - a Play view that keeps an unlisted app without `rulesFrom` working but hidden;
  - a Vela Maps driver and scenario plan in the route-capture suite, plus a new US imperial route.
- **Touches:**
  - rule data: `rules/catalog/navigation-apps.json`, `rules/bundled/vela-maps/en.json`, `rules/fixtures/vela-maps.json`;
  - app: `AndroidManifest.xml`, `catalog/NavigationAppCatalog.kt` (`forGooglePlay`) and whatever reads the visible app list on Play;
  - route capture: `tools/route-capture/` (driver, `APPS`, routes, scenarios, README, tests), `tools/rule-workbench/workbench.py`;
  - tests and counts, README, the fastlane full description, `docs/IMPLEMENTATION_STATUS.md`, `MANIFEST.sha256.json`.
- **Tests:** JVM regression and Play-view tests, the rule and catalog validators, the route-capture harness tests, and the full `scripts/test-all.sh`.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR5, NFR1–NFR3). The facts are in `inception/reverse-engineering/developer-scan.md`: registration points, rule gaps (a)–(h), Vela's notification format and intents, and the route-capture structure. The emulator run is attempted at Build and Test, and the release (v0.0.81) happens at Deployment.

### Design decisions

**Play view (FR2):**
- `forGooglePlay()` keeps each `playListed: false` entry **without** `rulesFrom` as an entry of its own, flagged hidden (e.g. a transient `hiddenOnPlay`/`visible` property, never read from JSON).
- Matching, enablement (default on), the listener allowlist and rule loading use all entries.
- The UI lists (installed, "Also supported", setup progress, rules screens, links) use only visible entries.
- Entries with `rulesFrom` keep joining their root as today.
- Find every UI consumer of the catalog's app list and switch the user-facing ones to the visible list.

**Rules (FR3):**
- Keep the issue's rule ids where the rule is unchanged, and add rules for:
  - bare-instruction titles;
  - an exit rule above the roundabout rule;
  - roundabout rules above the turn rules;
  - left/right U-turns and exits;
  - "bear";
  - arrival ("Arrived", "Your destination is", "Arrive at").
- Give overlapping rules distinct priorities.
- `secondaryText` is the clock from "Arrive …"; "Navigating" stays unmatched.
- The ruleset metadata names the reporter's proposal as its source. Follow the bundled ruleset conventions (`rulesetVersion` `vela-maps-en-2026.10.30`, publisher).

**Route capture (FR4):**
- Follow the existing driver pattern (`apps.py`):
  - a `VelaMaps` app with `app.vela`;
  - a GitHub `ReleaseAsset` with an anchored `vela-maps-x86_64.apk`-style pattern that skips the `canary` pre-release (check the real asset names with `gh release view -R PimpinPumpkin/Vela` / `gh api`);
  - `start_navigation` via `google.navigation:q=lat,lon&mode=d`, falling back to `geo:`, and pressing a Start button if a preview shows.
- Add a US route to the route set with an en-US scenario (imperial units), selectable by every app's plan but included only in Vela's plan for now.
- Add the README rows. Add a harness test for the new driver and route (no device).

## Steps

- [x] 1. Add an entry to `docs/IMPLEMENTATION_STATUS.md`.
- [x] 2. Baseline: `android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.rules.*'`, `scripts/validate-rules.sh`, and the route-capture tests (`python3 -m pytest tools/route-capture/tests` or the README's command).
- [x] 3. FR1: the catalog entry and the manifest `<queries>`; update the counts and lock tables (`test_validate_catalog.py`, `NavigationAppCatalogTest`, `SetupProgressTest`). Run `scripts/validate-rules.sh`.
- [x] 4. FR2: the hidden-on-Play entry in `forGooglePlay()` and the visible list for the UI. Tests:
  - on Play, Vela isn't visible and has no website link, but a Vela notification matches and `app.vela` is allowlisted;
  - outside Play, Vela is visible with its website;
  - the MicroG Maps Play tests stay green.
- [x] 5. FR3: `rules/bundled/vela-maps/en.json` and `rules/fixtures/vela-maps.json` (every case in FR3, feet/miles and metres/km, 12/24-hour clocks, "Navigating" unmatched). Add `vela-maps` to the workbench `regression` list. Add `VelaMapsRulesRegressionTest`, checking the maneuver, distance, primary text and `secondaryText`. Run `scripts/validate-rules.sh` and the workbench regression.
- [x] 6. FR4: the route-capture driver, `APPS` entry, the US route and en-US scenario, Vela's plan, README rows, the harness test, and the "Navigating" `matched: false` fixture. Run the harness tests. Do not start an emulator in this step.
- [x] 7. FR5: the README and fastlane lists, a `MANIFEST.sha256.json` resync for changed protected files, and `scripts/validate-spec-assets.sh`.
- [x] 8. Run the full `scripts/test-all.sh` and `:app:assembleDebug`.
- [x] 9. Write `code-summary.md` and `traceability.json` (FR1–FR5 and NFR1–NFR3). Don't commit.

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
