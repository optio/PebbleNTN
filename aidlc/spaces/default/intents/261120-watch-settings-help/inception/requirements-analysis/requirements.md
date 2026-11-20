# Requirements: Watch settings help text (#139)

## Intent Analysis

- **Request:** "implement issue #139". GitHub issue #139 asks for clear help text for each setting in the phone app's Watch settings screen, so that users understand what each setting means and what changing it does.
- **Goal:** a user who opens Watch settings can tell, without trying it on the watch, what each setting changes, when it matters, and what it costs (battery, distraction, dependencies), so fewer settings are misread or set by trial and error.
- **Type:** enhancement to an existing screen (brownfield). The change is phone-only: Android UI, strings and docs. No protocol or watchapp change.
- **Scope:** one component, the Watch settings screen (`ui/watchsettings/`: `WatchSettingsUi.kt`, `WatchSettingsScreen.kt`, `OptionListScreen.kt`, `DrivingSection.kt`, and the strings in `res/values/strings.xml`), plus the REQ-ANDROID-021 requirement, the Android UI spec, the spec manifest and the Play Store screenshot of this screen.
- **Complexity:** simple. Depth: Minimal.

Decisions from the questions file (`requirements-analysis-questions.md`):
- Q1: both a short line under each row and a fuller explanation at the top of the value list.
- Q2: per-value descriptions only for the shortcut actions.
- Q3: the Directions note becomes that row's help.
- Q5: When driving keeps its paragraph as a section note, which is an exception to Q3, and its rows get short lines.
- Q4: update the spec, the manifest and the store screenshot.

## Functional Requirements

### FR1: Short help line under every setting row

- **FR1.1** Every setting row on the Watch settings screen shows a short help line (at most about 90 characters, one sentence) below its current value. This covers every row in Appearance, Directions, Arrival time, Distance, Backlight, Vibration, Early warning, Shortcuts and When driving (the switch and the speed slider). The value stays where it is today.
- **FR1.2** The help line says what the setting changes on the watch: what you see or feel.
- **FR1.3** The help line is always visible. No tap, expand or icon is needed to read it.
- **FR1.4** Every `WatchSetting` the screen can show maps to a short help string. The mapping is an exhaustive `when` over `WatchSetting`, like `WatchSettingsUi.title`, so a new setting fails to compile until it has help. The two driving speed settings share one help string, as they share one title.

**Acceptance:**
- Given a watch whose settings have loaded, when the Watch settings screen is shown, then every visible row shows its title, its current value and its short help line.
- Given any `WatchSetting`, when the help mapping is asked for it, then a non-empty string resource is returned. A unit test covers all entries.

### FR2: Fuller explanation on the value list

- **FR2.1** The value list opened from a row shows a fuller explanation (two to four sentences) at the top, above the preview, if there is one, and above the values. It covers what the setting does, when it matters, and its impact or trade-off where there is one. Examples: battery for the backlight duration, how the new-instruction and early-warning vibrations relate, that the gestures need Touch on, that the early-warning distance follows Distance units.
- **FR2.2** Where the default isn't obvious, the fuller explanation states it. Examples: New instruction pattern is Off by default; Early warning distance is 25 m (80 ft) by default.
- **FR2.3** Every setting that opens a value list has a fuller explanation. The mapping is exhaustive, as in FR1.4. The When driving switch and slider open no value list, so they need none (see FR4).

**Acceptance:**
- Given any setting that opens a value list, when the list is shown, then the fuller explanation is shown above the values. A test checks one visual setting (with a preview) and one non-visual setting.
- Given the Backlight duration list, when it is shown, then the explanation mentions the battery impact.

### FR3: Descriptions for the shortcut actions

- **FR3.1** On the value list of each shortcut slot (Up button, Down button, Swipe left/right/up/down, Tap), every action shows a short description under its name that says what the action does on the watch. The 15 actions run from None to Open settings. Example: "ETA label style": switches the arrival label between "ETA at / ETA in" and "ETA / IN".
- **FR3.2** Descriptions are shown only for the actions the watch offers. Next backlight colour stays hidden without an RGB backlight, as today.
- **FR3.3** No other setting's values get descriptions. Their names stay as they are.

**Acceptance:**
- Given the Up button value list, when it is shown, then each offered action shows its name and its description.
- Given a watch without an RGB backlight, when a shortcut list is shown, then Next backlight colour and its description are absent.
- Given a non-shortcut list such as Backlight duration, when it is shown, then its values show no descriptions.

### FR4: What happens to the existing explanations

- **FR4.1** The Directions section note ("Compact shows only the street name when the app gives one.") is removed as a section note. Its content moves into the Directions text row's short help line and fuller explanation, which together say what Full and Compact show.
- **FR4.2** The When driving paragraph (`watch_driving_explanation`) stays as the When driving section note, unchanged in meaning. This is the decided exception (Q5).
- **FR4.3** The When driving switch ("Hide directions when driving") and the speed slider ("Driving speed") each get a short help line under them, like the other rows. The slider's help says the speed follows Distance units.
- **FR4.4** The screen's intro (`watch_settings_explanation`) stays as it is.

**Acceptance:**
- Given the Watch settings screen, when it is shown, then no section note appears under the Directions card, and the When driving paragraph still appears under the When driving card.
- Given a watchapp with protocol 1.4 or later, when When driving is shown, then the switch and the slider each show their short help line.

### FR5: Help follows the connected watch

- **FR5.1** Help is shown only for rows that are shown. A setting hidden by capability or state shows no help: Backlight colour without an RGB backlight, Touch and the gestures without touch, the gestures while Touch is off, the early-warning details while the distance is Off, and When driving before protocol 1.4.
- **FR5.2** No help text names a feature the connected watch lacks. Example: the Up and Down buttons' help doesn't mention swipes. Text about a dependency appears only on the row that has it, such as the gestures needing Touch on.
- **FR5.3** Help text that mentions distances uses the units the watch is set to (metric or imperial), as the value labels do. Otherwise it is written to be unit-neutral.

**Acceptance:**
- Given a watch without touch, when the screen is shown, then no help on any visible row mentions swiping or tapping the screen.
- Given a watch set to miles, when the Early warning distance list is shown, then any distance in its explanation is in feet.

### FR6: Wording and terms

- **FR6.1** Help text uses the setting and value names as the phone shows them, and says nothing that contradicts the watchapp's behaviour (checked against `watchapp/src/c/`). Examples: the backlight level durations, which vibration fires when, what each shortcut action toggles.
- **FR6.2** The text follows the project's writing style: plain English, short sentences, second person, no jargon. Arrival time and ETA terms match the existing labels.

**Acceptance:** each help text is reviewed against the watchapp's behaviour for that setting during Code Generation, and the check is recorded in the code summary.

### FR7: Specs, manifest and store screenshot

- **FR7.1** REQ-ANDROID-021 in `requirements/REQ-ANDROID.md` is updated:
  - each row shows a short help line under its value;
  - the value list shows a fuller explanation;
  - the shortcut actions carry descriptions;
  - the Directions note becomes row help;
  - the When driving paragraph stays as its section note.
  
  Its acceptance criteria name the new tests.
- **FR7.2** The Watch settings section of `spec/400-ui/AndroidUI.md` describes the same layout.
- **FR7.3** `MANIFEST.sha256.json` is updated for every hash-protected file that changed, and `scripts/validate-spec-assets.sh` passes.
- **FR7.4** The Play Store screenshot of the screen is regenerated from the updated screen. It is `tools/play-store-sources/phone/2-watch-settings.png`, rendered by `StoreScreenshotsTest` with `PEBBLENTN_STORE_SCREENSHOTS_DIR` set. The Play images made from it are regenerated with `tools/play_store_graphics.py`, and `--check` passes.

**Acceptance:**
- `scripts/validate-spec-assets.sh` exits 0.
- The regenerated screenshot shows help lines under the rows.
- `tools/play_store_graphics.py --check` passes.

## Non-Functional Requirements

- **NFR1 Accessibility:**
  - TalkBack reads each row as one item: title, value, then help. The row's action label ("Change <setting>") still works.
  - The fuller explanation and each shortcut description are readable by TalkBack in reading order.
  - Every new string is an Android resource (AGENTS.md rules 16 and 17).
- **NFR2 Layout at large font and small screen:** at 200 % system font scale on a 360 dp wide screen:
  - no help text is cut off or overlaps another element;
  - rows grow in height instead of truncating;
  - nothing scrolls horizontally.
  
  A Compose test or Robolectric screenshot at font scale 2.0 checks this.
- **NFR3 Stable existing behaviour:**
  - the sync, the queue, the previews, capability hiding and the value selection behave exactly as before;
  - the existing JVM and Compose tests stay green, apart from assertions on the moved Directions note, which are updated to the new place.
- **NFR4 Build health:**
  - `./gradlew lint` passes with no new warnings in changed files;
  - `scripts/test-all.sh` passes;
  - the release APK size grows by less than 50 KB (strings only, no new images).
- **NFR5 Privacy:** no change to what the app reads, stores or sends.

## Constraints

- Phone-only, with no protocol or watchapp change. The watchapp's own settings menu stays as it is.
- English strings only (`res/values/strings.xml`). No translations are added.
- Hash-protected spec files must be updated with their manifest in the same commit. The pre-commit hook and CI run `validate-spec-assets.sh`.
- No code may go straight to `main`, because every push to `main` releases. The work goes on a feature branch and is merged through a PR.
- Material 3 components already in use (`ListItem`, `OutlinedCard`), with no new UI library.

## Assumptions

- **A1** The fuller explanation sits as a text block at the top of the existing value list. No new screen or dialog is added. This follows from Q1 option C, "at the top of the value list".
- **A2** The short help line goes in the row's `supportingContent` together with the current value: the value first, then the help in a smaller or muted style. This keeps one `ListItem` per row, so TalkBack reads it as one item. The exact styling is settled in Code Generation.
- **A3** The help texts are written in this change by the developer from the watchapp's code and existing docs. The user reviews them at the Code Generation gate.
- **A4** The store screenshot can be rendered by the existing Robolectric `StoreScreenshotsTest`, with no emulator.

## Out of Scope

- Help text on the watch's own settings menu (`settings_window.c`).
- Translations of any string.
- Descriptions for values other than the shortcut actions (Q2).
- Changing setting names, value names, defaults, grouping or order.
- Other screens: dashboard, onboarding, rules, debug.
- Store listing text (`fastlane/metadata`) beyond the regenerated images.

## Open Questions

- None blocking. The exact wording of each help text is produced in Code Generation and reviewed there (A3).
