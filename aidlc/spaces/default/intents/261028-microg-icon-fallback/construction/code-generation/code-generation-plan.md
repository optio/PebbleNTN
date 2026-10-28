# Code Generation Plan — MicroG Maps turn arrows on icon-only cards (#113 follow-up)

## Summary

- **Builds:** an explicit family root package in `RuleFamilies`, and a fallback drawable lookup under the root package name in `AppIconReferences`, for `rulesFrom` entries only.
- **Touches:** `catalog/NavigationAppCatalog.kt` (`RuleFamilies`, `ruleFamilies()`, `forGooglePlay()`), `notification/icon/AppIconReferences.kt` (and `LargeIconRecognizer.kt` / `AppContainer.kt` for wiring), their tests, `docs/IMPLEMENTATION_STATUS.md`.
- **Tests:** JVM tests for the root, the packages-to-try function and the lookup with an injectable identifier resolver; the full `scripts/test-all.sh`.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR3). The facts are in `inception/reverse-engineering/developer-scan-PebbleNTN.md`. The release (v0.0.79) happens at Deployment.

### Design decisions

**Root package (FR1):**
- `RuleFamilies` gains `resourceFallbackFor(pkg): String?`. It returns the root app's first package, only when `pkg` belongs to an entry with `rulesFrom` and differs from that root; otherwise null.
- **Outside Play:** built from the `rulesFrom` target's first package.
- **`forGooglePlay()`:** the merged entry's first package. A package merged in from a `playListed:false` entry keeps its fallback to that first package.
- `familyOf` / `applies` are unchanged; existing tests stay green.

**Lookup (FR2):**
- A pure function `resourcePackagesToTry(postingPackage, families)` returns `[posting] + listOfNotNull(fallback)`.
- `AppIconReferences.render` keeps opening only `createPackageContext(postingPackage)`. For each candidate name, it tries `getIdentifier(name, "drawable", p)` for each `p` in that list, and the first non-zero wins.
- The identifier lookup goes through a small injectable interface or lambda (default: the real `Resources.getIdentifier`), so tests can fake hits and misses.
- `AppIconReferences` receives the families, or the fallback function, from `AppContainer`. It keeps its cache key and still caches empty results.

## Steps

- [ ] 1. Add an entry to `docs/IMPLEMENTATION_STATUS.md`.
- [ ] 2. Baseline: run `android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.catalog.*' --tests 'com.pebblentn.app.notification.*'`.
- [ ] 3. FR1: the root and `resourceFallbackFor` in `RuleFamilies`, for normal and Play catalogs. Tests: MicroG → Google Maps outside and on Play, also with MicroG listed first in a test catalog; Google Maps, CoMaps and Organic Maps variants and unknown packages → null.
- [ ] 4. FR2: `resourcePackagesToTry` and the lookup seam in `AppIconReferences`; wire it in `AppContainer`. Tests:
  - the list for MicroG, Google Maps, CoMaps and an unknown package;
  - lookup: a hit only under the root resolves the maneuver;
  - a hit under the posting package never queries the root;
  - no hit gives today's empty result.
- [ ] 5. Run the full `scripts/test-all.sh` and `:app:assembleDebug`.
- [ ] 6. Write `code-summary.md` and `traceability.json` (FR1–FR3 and the NFRs). Don't commit.

### Revision 1 — user-added package names (FR4–FR7, added at the Build and Test gate)

**Design:**
- **Storage:** a `CustomPackagesRepository` (SharedPreferences or the existing settings store, keyed per `appId`) holds the user-added packages.
- **Validation:** a pure `CustomPackageValidator` checks:
  - the Android package-name format;
  - trims and lower-cases the input;
  - uniqueness against every built-in catalog package and every user-added package of any app.
- **Effective catalog:** `AppContainer` builds the catalog that feeds families, enablement, the listener allowlist and the debug labels by appending each app's user-added packages after its built-in ones, before the Play view and `ruleFamilies()`. User-added packages therefore join the family, follow the app's switch, and get the FR2 fallback (the root is the app's first built-in package). Changes take effect without a restart, by rebuilding or observing the catalog where the app already reacts to catalog or enablement changes.
- **UI:**
  - a **Packages** link next to "View its rules" on every rules-app row, installed or "Also supported";
  - it opens a Packages screen: built-in packages locked, user-added ones with remove, an add field with inline validation messages;
  - strings are resources, with accessibility labels;
  - on Play installs it shows on listed apps only.
- **Export:**
  - each exported event carries the posting package and `customPackage: true|false`;
  - the export header lists the user-added packages per app.
  - Keep the existing export redaction rules.

- [ ] 7. FR5: validator and repository with JVM tests (format, cross-app duplicate with built-in, cross-app duplicate with user-added, same-app duplicate, trim/lowercase, add/remove/persist).
- [ ] 8. FR6: the effective catalog in `AppContainer`, with JVM tests: a user-added Google Maps package matches like Google Maps, is ignored when the app is off, and no longer matches after removal; the FR2 fallback applies to it.
- [ ] 9. FR4 and FR5 UI: the Packages link and screen, with Compose tests for an installed app, an "Also supported" app, a Play install without MicroG Maps, add/remove, and the validation message.
- [ ] 10. FR7: export changes, with a test for the custom flag on a user-added package, no flag for a built-in one, and the header list.
- [ ] 11. Specs: amend the catalog / Navigation apps requirement (REQ-ANDROID-023) and the debug-export requirement; resync the MANIFEST; update `docs/IMPLEMENTATION_STATUS.md`. Run the full `scripts/test-all.sh` and `:app:assembleDebug`; update `code-summary.md` and `traceability.json` (FR1–FR7).

### Revision 2 (Request Changes: "Ok for 1 case sensitive, about 2 it might be easier to remove any hiding if it's the google play release unless you thing it can endangor the google play releass")

**Decision:** Play hiding of MicroG Maps stays. Showing or linking a patched Google Maps in the Play build risks Google Play's intellectual-property policy. The package name on Google Maps' Packages screen on Play is left as is.

- [x] 12. Package names are case-sensitive. User-added names are stored exactly as typed, after trimming only; no lower-casing. The duplicate check against all built-in and user-added packages stays case-insensitive. Update `CustomPackageValidator`, the repository and `EffectiveCatalog` where they lower-case. Update FR5 wording in REQ-ANDROID-023 and resync the MANIFEST. Tests: `com.baidu.BaiduMap` is stored with its capitals; a notification from it matches; `com.baidu.baidumap` is rejected as a duplicate. Run the full `scripts/test-all.sh`, then update `code-summary.md`.

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
