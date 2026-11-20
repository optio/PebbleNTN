# Code Generation Plan: Watch settings help text (#139)

## Summary

- Builds: a short help line under every Watch settings row, a fuller explanation at the top of each value list, descriptions for the 15 shortcut actions, and the Directions note moved into row help
- Touches: `ui/watchsettings/` (WatchSettingsUi, WatchSettingsScreen, OptionListScreen, DrivingSection), `res/values/strings.xml`, their two test files, REQ-ANDROID-021/-025, AndroidUI.md, MANIFEST.sha256.json, the store screenshot
- Tests: about 9 new JVM/Robolectric tests (help mappings, row help, value-list explanation, shortcut descriptions, capability hiding, units, large font), with the two existing note assertions updated

## Context

- Zero-Unit express work, so there is one implementation pass. Requirements: `inception/requirements-analysis/requirements.md` (FR1–FR7, NFR1–NFR5). Code knowledge base: `aidlc/spaces/default/codekb/PebbleNTN/`.
- Brownfield: files are modified in place. The work happens on a new branch `feat/watch-settings-help`, because every push to `main` releases. Nothing is committed in this stage.
- The app is Kotlin with Jetpack Compose (Material 3), JUnit 4 and Robolectric. Strings are English only, in `android/app/src/main/res/values/strings.xml`.

## Design decisions

- **D1 Help mappings.** Add `WatchSettingsUi.help(setting): @StringRes Int` (short line) and `WatchSettingsUi.details(setting): @StringRes Int` (fuller explanation), both as exhaustive `when` over `WatchSetting`, like `title()` (FR1.4, FR2.3). The driving speed settings share one help string. `details()` covers every setting, including the driving ones, so the mapping stays exhaustive; the driving rows simply never show it.
- **D2 Unit-aware text (FR5.3).** Where the explanation names a distance (early warning), use a second string with an imperial variant, chosen with `imperial = model.units == 1`, the same way `valueLabels` chooses. Only `details(setting, imperial)` takes the flag.
- **D3 Row layout (FR1, A2).** `SettingRow` keeps one `ListItem`. `supportingContent` becomes a `Column` with the value (`bodyMedium`) and the help line under it (`bodySmall`, `onSurfaceVariant`). TalkBack therefore reads title, value and help as one clickable node (NFR1). There is no `maxLines`, so text wraps at large fonts (NFR2).
- **D4 Value list (FR2).** `OptionListScreen` gets a first `item(key = "details")` with the fuller explanation, `bodyMedium`, 16 dp padding, above the preview and the queued card.
- **D5 Shortcut descriptions (FR3).** Add a string array `watch_values_shortcut_descriptions`, 15 items indexed by action value, the same order as `watch_values_shortcut`. `WatchSettingsUi.valueDescriptions(setting): @ArrayRes Int?` returns it for the seven shortcut slots and `null` for every other setting (FR3.3). `OptionRow` shows the description as `supportingContent` when it isn't null. The list of offered values is unchanged, so Next backlight colour stays hidden without an RGB backlight (FR3.2).
- **D6 Existing notes (FR4).**
  - Remove `note = R.string.watch_directions_text_explanation` from the Directions section and delete that string. Its meaning moves into `watch_setting_directions_text_help` and its details.
  - Keep `WatchSettingsSection.note` and `SettingsSection`'s note slot for future use; no section uses them now.
  - In `DrivingSection`, add the short help line under the switch's headline (`supportingContent`) and under "Driving speed", before the "Above …" value. `watch_driving_explanation` stays under the card unchanged (FR4.2).
- **D7 Wording source (FR6).** Each help and details text is written from the watchapp's behaviour in `watchapp/src/c/` and the existing docs and checked against it. Examples:
  - backlight levels: Low is 3 s on each update, Medium 10 s, High stays on until the app closes;
  - New instruction vibration: on a new instruction; Early warning: when the distance drops below the threshold, once or on every update;
  - each shortcut action's effect, from `shortcuts.c`.
  
  The code summary records a table "setting → watch behaviour checked in <file>". Text that names gestures appears only on the gesture and Touch rows (FR5.2).

## Steps

### Step 1: Branch and baseline
- [x] Create and switch to branch `feat/watch-settings-help` from `main`.
- [x] Verify the test runner and record the baseline. Run the unit-scoped command from `unit-test-instructions.md` on the two existing watchsettings test classes; it must pass before any change.

### Step 2: Business logic: help mappings (FR1.4, FR2.3, FR3, FR5.3)
- [x] In `WatchSettingsUi.kt`, add:
  - `help(setting)` (D1);
  - `details(setting, imperial)` (D1, D2);
  - `valueDescriptions(setting)` (D5).
- [x] In `strings.xml`, add next to the existing Watch settings block:
  - one `watch_setting_<name>_help` per setting, 26 strings with the driving speeds shared;
  - one `watch_setting_<name>_details` per setting, plus `_details_imperial` where a distance is named;
  - `watch_values_shortcut_descriptions`, 15 items.
  
  The wording follows D7 and FR6.2: plain, second person, short sentences.
- [x] Remove the Directions section `note` from `sections()` and delete `watch_directions_text_explanation` (D6, FR4.1).

### Step 3: Business logic tests (test-after)
In `WatchSettingsUiTest.kt`:
- [x] Every `WatchSetting` maps to a non-zero help resource and a non-zero details resource, so no entry is missing (FR1.4, FR2.3).
- [x] `valueDescriptions` is non-null for exactly the seven shortcut slots and null otherwise. The descriptions array has the same length as `watch_values_shortcut` (FR3.1, FR3.3). This needs the Robolectric resources, as the existing resource tests use.
- [x] `details(EARLY_WARN_DISTANCE, imperial = true)` differs from the metric one (FR5.3).
- [x] The Directions section has no note any more. This updates the existing assertion at line 110 (FR4.1).
- [x] Run the command from `unit-test-instructions.md` for `WatchSettingsUiTest`, and it is green.

### Step 4: Frontend: row help, value-list explanation, shortcut descriptions, driving rows (FR1, FR2, FR3, FR4)
- [x] `WatchSettingsScreen.kt`: `SettingRow`'s supporting content shows the value, then the help line (D3).
- [x] `OptionListScreen.kt`: add the details item first (D4), and show shortcut descriptions in `OptionRow` (D5).
- [x] `DrivingSection.kt`: add the short help lines under the switch and the speed row. The section explanation stays (D6, FR4.2, FR4.3).
- [x] Add test tags for the new texts where a test needs them to find the node (`WatchSettingsTags` beside `DrivingSectionTags`).

### Step 5: Frontend tests (test-after)
In `WatchSettingsScreenTest.kt`, using Robolectric Compose with the existing fixtures:
- [x] A READY colour watch shows the help line of a representative row from each section, for example Colour, Directions text, ETA display and Backlight duration, together with its value (FR1.1).
- [x] A watch without touch shows no visible help mentioning a swipe or a tap, and no gesture rows. A watch without an RGB backlight shows no Backlight colour row or help (FR5.1, FR5.2).
- [x] The Directions note text is gone, and the When driving paragraph is still present. This updates the existing assertion at line 145. The driving switch and slider show their help lines (FR4).
- [x] `OptionListScreen` for Backlight duration shows the details text, which mentions battery. For Colour it shows the details with the preview (FR2.1).
- [x] `OptionListScreen` for Up button shows a description under each offered action. On a watch without an RGB backlight, Next backlight colour and its description are absent. Backlight duration shows no value descriptions (FR3).
- [x] At font scale 2.0 on `w360dp`, a row's help text is displayed in full and not cut off (`assertIsDisplayed` on the full text, no ellipsis) (NFR2).
- [x] TalkBack check: the row's merged semantics contain the title, value and help text, and its click label is still "Change <setting>" (NFR1).
- [x] Run the command from `unit-test-instructions.md` for `WatchSettingsScreenTest`, and it is green.

### Step 6: Documentation and specs (FR7.1–FR7.3)
- [x] `requirements/REQ-ANDROID.md`:
  - REQ-ANDROID-021 Screen: rows show a short help line under the value; the value list starts with a fuller explanation; the shortcut actions carry descriptions. The Acceptance names the new tests.
  - REQ-ANDROID-025 Screen: replace the "Compact shows only…" line with the row help.
  - REQ-ANDROID-024: unchanged except that the switch and slider rows have help lines.
- [x] `spec/400-ui/AndroidUI.md`, Watch settings: describe the same layout and drop the quoted Directions line.
- [x] If `docs/PebbleNTN-Consolidated-Spec.md` is generated from those sources, regenerate it with the repository script. Otherwise leave it.
- [x] Update `MANIFEST.sha256.json` with the repository tool or script for the hash-protected files that changed. Run `scripts/validate-spec-assets.sh`, and it exits 0.
- [x] `docs/IMPLEMENTATION_STATUS.md`: add the #139 entry (milestone, requirements, commands run, blockers, next task), as CLAUDE.md requires.

### Step 7: Store screenshot (FR7.4)
- [x] Regenerate `tools/play-store-sources/phone/2-watch-settings.png` with `PEBBLENTN_STORE_SCREENSHOTS_DIR=$PWD/tools/play-store-sources/phone android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.StoreScreenshotsTest'`. Keep only the files that really changed: 2-watch-settings, and 3-accent-colour, since the details text now appears on the value list. Revert any other file that changed only byte-wise.
- [x] Run `python3 tools/play_store_graphics.py` to refresh `fastlane/metadata/android/en-US/images/phoneScreenshots/`, then `--check`, which passes.
- [x] Look at the regenerated 2-watch-settings image: help lines are visible under the rows and nothing overlaps.

### Step 8: Build configuration and full checks
- [x] No Gradle or dependency change is expected. Confirm with `git diff --stat` that only the planned files changed.
- [x] Run `./gradlew lint` on the module (`android/gradlew -p android :app:lintDebug`) with no new warnings in changed files (NFR4).

### Step 9: Traceability
- [x] Write `code-summary.md`, including the wording check table (D7, FR6.1), and `traceability.json` for FR1.1–FR7.4 and NFR1–NFR5.

## Traceability (plan step → requirement)

| Step | Requirements |
|---|---|
| 2, 3 | FR1.4, FR2.3, FR3.1, FR3.3, FR4.1, FR5.3, FR6 |
| 4, 5 | FR1.1–FR1.3, FR2.1, FR2.2, FR3.1–FR3.3, FR4.1–FR4.4, FR5.1, FR5.2, NFR1, NFR2, NFR3 |
| 6 | FR7.1–FR7.3 |
| 7 | FR7.4 |
| 8 | NFR4, NFR5 |

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

Layers not applicable here: data model / database, repository / data access and API / endpoint. Nothing changes in persistence, sync or the protocol, so those layers are omitted, as the methodology allows.
