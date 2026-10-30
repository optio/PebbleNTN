# Code Summary — Vela Maps support (#120)

Testing contract: test-after, Minimal strategy, express scope. Each layer was implemented, then its tests were written and run. Nothing is committed.

## What was built

### FR1 — Catalog entry
- `rules/catalog/navigation-apps.json` has a `vela-maps` entry:
  - `app.vela`, with its GitHub website;
  - `playListed: false`, official rules, capture available, on by default.
- `AndroidManifest.xml` declares `app.vela` in `<queries>`, so the catalog now has 28 packages.
- The count and lock tests are updated: `test_validate_catalog.py`, `NavigationAppCatalogTest`, `SetupProgressTest`.

### FR2 — Hidden on Google Play, still working
- `NavigationAppCatalog.forGooglePlay()` now treats an unlisted entry without `rulesFrom` like this:
  - it stays an entry of its own, with its website removed;
  - its id goes into the transient `hiddenAppIds`.
- `visibleApps` and `isVisible` serve the UI:
  - `supportedNotInstalled` (the "Also supported" list);
  - `EnabledAppRepository.observeEnablement` (the Navigation apps rows and the dashboard counts);
  - the Packages screen (`MainActivity`);
  - the Rules screen's official groups (`groupOfficialRules`, now `internal`).
- Matching, enablement (on by default on discovery), the listener allowlist and discovery keep using every entry.
- MicroG Maps still joins Google Maps on Play.
- REQ-ANDROID-023, `spec/200-architecture/Android.md` and `schemas/app-catalog.schema.json` describe the new case.

### FR3 — English rules
- `rules/bundled/vela-maps/en.json` has 29 rules, `vela-maps-en-2026.10.30`. Its publisher and comments name the #120 reporter's proposal.
- Changes from the issue's rules:
  - **Bare-instruction titles:** a `vela-bare-*` rule for each maneuver, with no distance.
  - **Arrival:** approaching-arrival rules (`vela-in-distance-arrive-en`, `vela-bare-arrive-en`).
  - **Exits:** left and right exit rules, ranked above the roundabout rule. They never take a roundabout exit or an ordinal exit.
  - **Roundabouts:** rules covering the OSRM, Valhalla and GraphHopper wording, ranked above the turn rules.
  - **U-turns:** right and left (left when the wording names no side).
  - **"Bear":** gives a slight left or right.
  - **Road names:** maneuver words are read only at the start of the instruction, so a road name (`Bear Creek Rd`) never decides the arrow.
  - **Priorities:** every rule has its own priority.
- `secondaryText` is the clock from "Arrive …", in 12-hour and 24-hour forms.
- "Navigating" stays unmatched.
- `rules/fixtures/vela-maps.json` has 50 synthetic fixtures:
  - feet/miles and metres/km;
  - 12-hour and 24-hour clocks;
  - a stop ahead and a faster-route suffix;
  - three "matched: false" cards.
- `vela-maps` is in the workbench `regression` list.
- `rules/fixtures/README.md` has a Vela section.

### FR4 — Route capture
- `routecap/apps.py`:
  - `ReleaseAsset(stable_only=…)`;
  - a `VelaMaps` driver that installs the newest stable release. It takes the x86_64 APK, or the release's single APK, and skips the nightlies and the canary.
  - The driver sends `google.navigation:`, then falls back to `geo:`.
  - "Get started" is a first-run label.
- A new `austin-downtown` route, with car, bike and foot tracks fetched with `./routecap.py fetch-routes`.
- A new `vela-maps-m1` plan: the seven European routes in en-GB, then Austin in en-US.
- README rows for the app, the plan and the routes.
- 10 new harness tests.

### FR5 — Docs
- README line.
- `MANIFEST.sha256.json` is resynced for `README.md`, `requirements/REQ-ANDROID.md` and `spec/200-architecture/Android.md`.
- `docs/IMPLEMENTATION_STATUS.md` has an entry.

## Deviations
- **Fastlane full description not changed.** It is the shared Google Play listing (`docs/PLAY_CONSOLE.md`). REQ-ANDROID-023 says the shared store description does not name an app that is hidden on Play, and NFR2 says Play shows nothing about Vela. MicroG Maps is absent from it for the same reason. Listing Vela there needs a human decision.
- **Stable release instead of the nightly.** The driver installs the newest **stable** release (today v0.5.2621, a single `vela-maps-v0.5.2621.apk`). The scan expected the per-ABI x86_64 APK of v0.5.2677, but that release is a nightly pre-release.
- **Specs updated.** REQ-ANDROID-023 and the Android architecture spec now cover an app that is unlisted on Play and has no `rulesFrom`, so their hashes were resynced.

## Test results
- **Baseline** (catalog, rules, ui): 252 tests, 0 failures, 4 skipped.
- **After** (catalog, rules, ui, data): 355 tests, 0 failures, 4 skipped.
- **`scripts/test-all.sh`:** exit 0.
  - Debug and release unit tests: 630 each, 0 failures, 4 skipped.
  - Lint: 0 errors, no new warnings.
- **Other checks:**
  - workbench regression: vela-maps 50/50, the other apps unchanged;
  - route-capture tests: 37 OK;
  - `validate-rules.sh` OK;
  - `validate-spec-assets.sh` OK (34 files);
  - `:app:assembleDebug` OK.

## For Build and Test
Run on the Windows emulator, never inside WSL, with the emulator booted:

```bash
cd tools/route-capture
./routecap.py install vela-maps
./routecap.py run scenarios/vela-maps-m1.json --only 'london|austin'
./routecap.py run scenarios/vela-maps-m1.json --publish
```

These are still unknown until that run:
- whether Vela's link starts guidance or only opens a preview;
- whether Vela honours `mode`;
- which router writes the instructions;
- whether Vela routes online in the emulator;
- whether the stable APK runs on the x86_64 Android 17 image.
