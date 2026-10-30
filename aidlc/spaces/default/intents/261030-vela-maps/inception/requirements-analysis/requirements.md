# Requirements — Vela Maps support (#120)

## Sources

- Initial description: [desc] "Work on GitHub issue #120: add Vela Maps (app.vela) support with the catalog entry and English rules from the issue, plus synthetic fixtures; also add Vela Maps to the route-capture test suite that runs routes in different navigation apps to find unrecognised notifications, if possible"
- Workflow-selected scope: [scope] express
- Issue #120: the proposed catalog entry and English rules, by the issue reporter
- The person's added request: "Regarding #120 , also add if possible Vela Maps to the test suite that can run different routes in different apps to find unrecognised notifications"
- Answers Q1–Q4 in `requirements-analysis-questions.md`
- The code knowledge base (focused scan at c7b6ba0) and `inception/reverse-engineering/developer-scan.md`, which also read Vela's public source for its notification format and intents

## Intent Analysis

- **Request type:** a new supported navigation app (rule data and catalog), a small Play-view change, and route-capture harness support. No watch or protocol change.
- **Why:** Vela Maps users get turn-by-turn on their Pebble. The route-capture suite finds Vela notifications the rules miss, and gains a US imperial route for later runs of the other apps.
- **Requirements depth:** Minimal.

## Functional Requirements

### FR1 — Catalog entry and visibility

- `rules/catalog/navigation-apps.json` gets `vela-maps`:
  - display name "Vela Maps", package `app.vela`;
  - website `https://github.com/PimpinPumpkin/Vela`;
  - `playListed: false`, `hasOfficialRules: true`, `captureAvailable: true`, `defaultEnabled: true`.
- The manifest `<queries>` declares `app.vela`.
- **Outside Google Play:** Vela Maps is listed like the other apps (installed or "Also supported"), with its rules and Packages links and the link to its website.
- **Pass/fail:** catalog tests and `validate_catalog.py` pass, with 28 packages.

### FR2 — On Google Play: hidden but working, as MicroG Maps [Q1]

- On a Google Play install, Vela Maps isn't shown anywhere in the app (no row, no website link, no Packages link).
- Its notifications are still processed with its own rules. This needs `forGooglePlay()` to keep an unlisted entry **without** `rulesFrom` for matching, enablement (on by default) and the listener allowlist, while leaving it out of everything the user sees.
- MicroG Maps' Play behaviour (joining Google Maps) is unchanged.
- **Pass/fail:** JVM tests on the Play view:
  - Vela isn't among the visible apps and has no website link;
  - a Vela notification matches its rules;
  - `app.vela` is in the allowlist;
  - the existing MicroG Maps Play tests stay green.

### FR3 — English rules, with the gaps fixed [Q2]

`rules/bundled/vela-maps/en.json` starts from the issue's rules. Every rule lists exactly `["app.vela"]`, and the rules also cover:
- **Bare-instruction titles** (no `In <distance> ·` prefix, sent when the distance is 0): the same maneuver arrows, without a distance.
- **Exits:** "Take the … exit" and ramps go to the exit rule, not the roundabout rule. The direction follows the wording ("exit left" / "ramp left" → slight left; otherwise slight right).
- **Roundabouts:** "At the roundabout, …", "Enter the roundabout" and "take the Nth exit" at a roundabout give the roundabout arrow, ahead of the turn rules.
- **U-turns:** right or left from the wording; left when it doesn't say.
- **"Bear left" / "Bear right"** → slight left / right.
- **Arrival:** "Arrived", "Your destination is …" and "Arrive at …" give the arrival arrow, with or without a distance.
- **ETA:** `secondaryText` is the clock from "Arrive 12:45" / "Arrive 12:45 PM" in the text, as for the other apps.
- **The "Navigating" card** (no instruction) stays unmatched, so nothing wrong reaches the watch.
- No rule outranks another by id order alone: overlapping rules have distinct priorities.
- **Pass/fail:**
  - `validate-rules.sh` passes;
  - synthetic fixtures in `rules/fixtures/vela-maps.json` cover every rule above (feet/miles and metres/km, 12- and 24-hour clocks, "Navigating" with `matched: false`);
  - a `VelaMapsRulesRegressionTest` checks maneuver, distance, primary text and `secondaryText` for each fixture;
  - the workbench `regression` app list includes `vela-maps`.

### FR4 — Vela Maps in the route-capture suite [Q3, Q4]

- `tools/route-capture` gets a Vela Maps app driver:
  - install from Vela's GitHub releases (the stable x86_64 APK, skipping the `canary` pre-release);
  - start navigation through `google.navigation:` or `geo:`, then press Start if a preview shows;
  - a scenario plan running the existing European routes in English (metric).
- **US imperial route:** a new US route is added to the route set with an en-US scenario (imperial units). It's included in Vela's plan, and **available to the other apps' plans** for later runs, but only run for Vela now.
- The README lists Vela and the new route.
- The harness's own tests (`tools/route-capture/tests`) pass.
- **Pass/fail:**
  - the harness tests are green;
  - a run on the Windows emulator at Build and Test, attempted for Vela only, reports its captured notifications and any unrecognised ones. If the run can't complete (e.g. no online routing in the emulator), the reason is recorded and the code still ships.

### FR5 — Docs, counts and release

- **Counts and lists:** the catalog/package counts in tests (`test_validate_catalog.py`, `NavigationAppCatalogTest`, `SetupProgressTest`) are updated.
- **Docs:**
  - the README and the fastlane full description list Vela Maps;
  - `docs/IMPLEMENTATION_STATUS.md` gets an entry;
  - protected files are resynced in `MANIFEST.sha256.json`.
- **Release:** merging releases v0.0.81 through the normal release workflow; the Play bundle follows through `play-release`.

## Non-Functional Requirements

- **NFR1 — Compatibility:** every other app's matching and Play behaviour is unchanged; the full `scripts/test-all.sh` stays green.
- **NFR2 — Privacy and Play policy:** on Play installs nothing about Vela is shown and no outside download is linked. No new permission and no network use in the app.
- **NFR3 — Evidence:** the rules are marked as based on the reporter's proposal and Vela's source (synthetic fixtures), to be checked against real captures (route capture, or a diagnostics export from the reporter).

## Constraints

- The rules are English only. Vela translates its notifications into about 24 languages, so other languages need real captures.
- Route capture runs only in the Windows emulator, never inside WSL.

## Assumptions

- Vela's English instruction wording comes from its routing engine (Valhalla, OSRM or GraphHopper), so the fixtures use wording typical for those engines.

## Out of Scope

- Rules for other languages.
- Running the new US route for the other apps (it's available for later runs).

## Open Questions

None.
