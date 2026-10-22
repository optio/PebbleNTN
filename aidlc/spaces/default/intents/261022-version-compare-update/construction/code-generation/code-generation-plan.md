# Code Generation Plan — Phone app update detected by version comparison

## Summary

- Builds: a shared strict version rule; the watch shows the #94 update UI when it is newer than the phone app; the phone remembers the watch's version (best effort); install-source detection and an "Update from {source}" card with "Other sources" and the F-Droid delay line
- Touches: `watchapp/src/c/version_compare.c/.h` (new), `main.c`; Android `update/` (AppVersions, UpdateCheckRepository, new InstallSource), `protocol/ProtocolCodec.kt`, `pebble/NavigationController.kt`, `di/AppContainer.kt`, `ui/MainActivity.kt`, `ui/dashboard/` (DashboardScreen, new UpdateCard), `strings.xml`, requirements and spec files
- Tests: about 30 new assertions: host C `test_version_compare.c`, plus JVM tests in AppVersionsTest, ProtocolCodecTest, UpdateCheckRepositoryTest, NavigationControllerTest, and the new InstallSourceTest and UpdateCardTest; emulator screenshots on 4 watch platforms

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR6, NFR1–NFR5). The code knowledge base is at `aidlc/spaces/default/codekb/PebbleNTN/`.

Design decisions:
- **Watch version rule (FR1):** a new pure C unit `version_compare.c/.h`. Each host test links exactly one unit, so this unit also holds the effective-version choice.
  - `bool version_parse(const char *v, ...)`: strict parse with an optional `v`/`V`, then 1–9 digits per part, `.`-separated, no empty part and no suffix.
  - `bool version_newer(const char *a, const char *b)`: true only when both parse and `a > b`, with missing parts counting as 0.
  - `const char *version_effective_update(const char *reported, const char *own, const char *phone)`: returns `reported` (key 14, may be "") or `own`, whichever is the higher available update. `own` counts only when `version_newer(own, phone)`. It returns "" when there is no update.
- **Watch wiring (FR2):** `main.c` keeps `s_latest_phone_version` (key 17) as "what the phone reported", unchanged. A helper `effective_update()` computes the version from `s_latest_phone_version`, `APP_VERSION_STR` and `s_phone_version` (key 16), and it feeds `maybe_show_update_notice`, `update_idle_text(...)` and `glance_update_subtitle(...)`. No new persist key; key 18 (shown) works as before. `record_phone_version` runs before the notice decision on every message.
- **Phone version rule (FR1):** `AppVersions.parseStrict(v): List<Int>?` and `AppVersions.isNewerStrict(current, candidate)`. The existing tolerant `compare`/`isNewer` are unchanged; they are still used for GitHub tags.
- **Phone watch version (FR3):**
  - `ProtocolCodec.watchAppVersion(message): String?` reads key 11 from a WATCH_READY.
  - `NavigationController` gets a constructor parameter `onWatchAppVersion: (String) -> Unit = {}`, called from its inbound handler for a genuine WATCH_READY that carries the key. The self-made ready events don't call it.
  - `UpdateCheckRepository.recordWatchAppVersion(v)` stores it in prefs as `watch_app_version`, written only on change.
  - `UpdateState` gains `watchAppVersion: String?`. `updateAvailable` becomes "GitHub newer OR watch strictly newer", and the new `effectiveLatest: String?` is the higher of the two.
  - In AppContainer, `latestPhoneUpdate` and the dashboard use `effectiveLatest`.
- **Install source (FR4):** `update/InstallSource.kt`:
  - `enum class InstallSource { GITHUB, FDROID, OBTAINIUM }`;
  - a pure `InstallSources.fromInstaller(pkg: String?)`;
  - `InstallSources.detect(context)` (getInstallSourceInfo; any exception → GITHUB).
  
  Detection runs once in AppContainer. Destinations:
  - GitHub opens `update_releases_url`;
  - F-Droid opens `update_fdroid_url`;
  - Obtainium launches the app (`getLaunchIntentForPackage` for `dev.imranr.obtainium` and its `.fdroid` variant), and otherwise opens `update_obtainium_url`.
- **Card (FR5):** a pure `ui/dashboard/UpdateCard.kt` with `updateCardModel(source)` → (main source, other sources in order, show F-Droid delay). DashboardScreen shows the PromptCard as follows:
  - **Text:** a source-neutral body that keeps the uninstall advice, plus the F-Droid delay line when the source is F-Droid.
  - **Main action:** "Update from %1$s".
  - **Secondary action:** "Other sources", which opens an AlertDialog listing the other two sources. The pick is used once and nothing is stored.
  
  MainActivity's `openUpdateSource(source)` replaces `openUrl(update_releases_url)`.

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

## Steps

- [x] **Step 1: Project structure.** No protocol change (NFR1): `protocol/protocol-definition.json` and the generated files stay as they are. Create the empty new files: `watchapp/src/c/version_compare.c/.h`, `android/.../update/InstallSource.kt`, `android/.../ui/dashboard/UpdateCard.kt`.
- [x] **Step 2: Verify the test runners.** Check that `scripts/test-watchapp-unit.sh` picks up a new `watchapp/tests/test_<unit>.c` paired with `src/c/<unit>.c`. Run the scoped Gradle command from unit-test-instructions.md with the existing test classes; both runners must work before the first new test.
- [x] **Step 3: Data model: implement (FR3.1, FR3.4).** `UpdateState.watchAppVersion`, `updateAvailable` (GitHub newer or watch strictly newer), `effectiveLatest`.
- [x] **Step 4: Repository: implement (FR3.1–FR3.4, NFR3).** `UpdateCheckRepository.recordWatchAppVersion(v)`, with prefs key `watch_app_version` loaded at start and written only on change. `checkForUpdate` keeps the stored watch version in the new state.
- [x] **Step 5: Repository and data model: tests (FR3, NFR2, NFR3).** UpdateCheckRepositoryTest:
  - a watch version newer than the current version → update, with `effectiveLatest` = the watch version, and no fetch;
  - same value again → no write;
  - an unparsable watch version → no update;
  - GitHub `0.0.73` and watch `0.0.72` → `0.0.73`;
  - GitHub `0.0.72` and watch `0.0.74` → `0.0.74`.
- [x] **Step 6: Business logic, watch: implement (FR1, FR2.2).** `version_compare.c/.h` with `version_parse`, `version_newer` and `version_effective_update`. Pure C with no Pebble includes.
- [x] **Step 7: Business logic, watch: tests (FR1, FR2.1, FR2.2).** `watchapp/tests/test_version_compare.c`:
  - `0.0.10` > `0.0.9`, and `1.2` = `1.2.0`;
  - `v0.0.71` parses, while `0.0.71-debug`, `0.0.x`, `0..1`, `""` and NULL don't;
  - a 10-digit part is rejected;
  - effective update: own newer → own; reported higher → reported; own not newer → reported or ""; phone unparsable → reported only.
- [x] **Step 8: Business logic, phone: implement (FR1, FR4.1, FR5).**
  - `AppVersions.parseStrict` and `isNewerStrict`.
  - `InstallSource` and `InstallSources.fromInstaller` (F-Droid: `org.fdroid.fdroid`, `org.fdroid.basic`, `com.looker.droidify`, `com.machiav3lli.fdroid`; Obtainium: `dev.imranr.obtainium` and `dev.imranr.obtainium.*`; else GitHub), plus `detect(context)`, which logs a failure and falls back to GitHub.
  - `UpdateCard.updateCardModel(source)`.
- [x] **Step 9: Business logic, phone: tests (FR1, FR4.1, FR5.2, FR5.4).**
  - **AppVersionsTest:** the strict cases, mirroring Step 7; the existing tolerant cases stay unchanged.
  - **InstallSourceTest:** each package name; `null`; `com.android.packageinstaller`; `com.android.vending` → GitHub.
  - **UpdateCardTest:** the main source, the other two sources in a stable order, and the delay line only for F-Droid.
- [x] **Step 10: API / protocol edge: implement (FR3.1, FR3.2).**
  - `ProtocolCodec.watchAppVersion(message)`.
  - `NavigationController` takes `onWatchAppVersion` and calls it for a received WATCH_READY that carries key 11. The self-made ready events don't call it.
  - In AppContainer, wire it to `updateCheckRepository.recordWatchAppVersion`, and make `latestPhoneUpdate` use `effectiveLatest`. The install source is detected once.
- [x] **Step 11: API / protocol edge: tests (FR3.1, FR3.2).**
  - **ProtocolCodecTest:** a WATCH_READY with key 11 → `"0.0.72"`; without the key → null; any other event → null.
  - **NavigationControllerTest:** an inbound WATCH_READY with the key → the callback gets the version; the self-made ready event → no call.
- [x] **Step 12: Frontend, watch: implement (FR2.1, FR2.3, FR2.4).** In `main.c`:
  - an `effective_update()` helper, used by `maybe_show_update_notice` (decision, shown copy, key 18), by the idle text and by the glance subtitle;
  - make sure `record_phone_version` runs before the notice decision on every message;
  - update the comments.
- [x] **Step 13: Frontend, phone: implement (FR4.2, FR5).**
  - **DashboardScreen card:**
    - text: the source-neutral body with the uninstall advice, plus the F-Droid delay line;
    - "Update from %1$s" button;
    - "Other sources" secondary button that opens an AlertDialog listing the other two sources.
  - **MainActivity:** `openUpdateSource(source)` with the three destinations, including the Obtainium launch intent and its website fallback.
  - **`strings.xml`:** add `dashboard_update_from`, `dashboard_update_other_sources`, `dashboard_update_pick_source`, `dashboard_update_fdroid_delay` ("F-Droid may take about a day to offer this update.") and the source names. Make `dashboard_update_body` source-neutral and keep its uninstall advice. Add `update_fdroid_url` and `update_obtainium_url`, both `translatable="false"`. Remove `dashboard_update_download` if nothing uses it any more.
- [x] **Step 14: Frontend: build and check in the emulator (NFR4).** Build the watchapp. On basalt, chalk, emery and diorite, use `pebble send-app-message` to send a NO_ACTIVE_NAVIGATION with key 11 set below the watch's own version and bit 4 clear. Check that:
  - the notice appears once;
  - the idle text shows the update;
  - the launcher subtitle reads `Update: <watch version>`;
  - with key 11 equal to the watch's version, there is no update UI.
  
  Save screenshots as `emulator-<platform>-<what>.png` in this folder and close the emulator. Build the debug APK. The card is checked on a device later (no Android emulator in WSL).
- [x] **Step 15: Specification (FR6.1).**
  - `requirements/REQ-WATCH.md`: amend REQ-WATCH-023.
  - `requirements/REQ-ANDROID.md`: add REQ-ANDROID-019 (the watch's version, best effort) and REQ-ANDROID-020 (install source and the update card).
  - `spec/200-architecture/Protocol.md`: note that the phone reads key 11 from WATCH_READY.
  - Update `MANIFEST.sha256.json` for any listed file that changed, and run `scripts/validate-spec-assets.sh`.
- [x] **Step 16: Documentation and traceability.** Add an entry to `docs/IMPLEMENTATION_STATUS.md`, then run the full `scripts/test-all.sh` (NFR5).

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1.1–FR1.4 version rule | 6, 7, 8, 9 |
| FR2.1 watch newer than phone | 7, 12, 14 |
| FR2.2 effective update version | 6, 7, 12 |
| FR2.3 notice, idle text, subtitle | 12, 14 |
| FR2.4 no new persist key | 6, 12 |
| FR3.1 read and store the watch version | 4, 5, 10, 11 |
| FR3.2 self-made readiness ignored | 10, 11 |
| FR3.3–FR3.4 phone update from the watch version | 3, 4, 5, 10 |
| FR3.5 Core Devices accepted | (no code) |
| FR4.1 install source mapping | 8, 9 |
| FR4.2 destinations | 13 |
| FR5.1–FR5.5 card | 8, 9, 13 |
| FR6.1 specs | 15 |
| NFR1 no protocol change | 1, 16 |
| NFR2 no network | 5, 16 |
| NFR3 flash wear | 5, 12 |
| NFR4 layout | 14 |
| NFR5 tests | 2, 5, 7, 9, 11, 16 |
