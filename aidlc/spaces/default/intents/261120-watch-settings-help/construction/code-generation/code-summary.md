# Code Summary: Watch settings help text (#139)

Express scope, zero-Unit, Minimal test strategy, test-after. Branch `feat/watch-settings-help` from `main` (`073f51d`). Nothing is committed.

## Files created/modified

| File | Change |
|---|---|
| `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUi.kt` | `help(setting)`, `details(setting, imperial)` and `valueDescriptions(setting)`, each an exhaustive mapping. The Directions section's `note` is removed. |
| `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreen.kt` | `SettingRow` shows the value, then the help line, in one `ListItem`. New `WatchSettingsTags` (row, help, details). KDoc updated. |
| `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/OptionListScreen.kt` | A first `details` item with the fuller explanation. `OptionRow` shows the shortcut action description as `supportingContent`. |
| `android/app/src/main/java/com/pebblentn/app/ui/watchsettings/DrivingSection.kt` | Help lines under the switch and under "Driving speed". Two new tags. The When driving paragraph is unchanged. |
| `android/app/src/main/res/values/strings.xml` | 26 `_help` strings, 28 `_details` strings (27 settings plus the early-warning imperial variant) and the 15-item array `watch_values_shortcut_descriptions`. `watch_directions_text_explanation` is deleted. |
| `android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsUiTest.kt` | Now runs on Robolectric. 4 new tests, and the Directions note test is updated. |
| `android/app/src/test/java/com/pebblentn/app/ui/watchsettings/WatchSettingsScreenTest.kt` | 11 new tests, and the Directions assertion is updated. |
| `requirements/REQ-ANDROID.md` | REQ-ANDROID-021: Screen gets a **Help (#139)** paragraph, and Acceptance names the new tests. REQ-ANDROID-024: help lines on the switch and the speed. REQ-ANDROID-025: the quoted line is replaced by the row help. |
| `spec/400-ui/AndroidUI.md` | Watch settings: the quoted Directions line is dropped. Three bullets describe the row help, the value-list explanation and the shortcut descriptions. |
| `MANIFEST.sha256.json` | New hashes for the two spec files. |
| `tools/play-store-sources/phone/2-watch-settings.png`, `3-accent-colour.png` | Re-rendered by `StoreScreenshotsTest`. |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/2.png`, `3.png` | Refreshed by `tools/play_store_graphics.py`. |
| `docs/IMPLEMENTATION_STATUS.md` | New #139 entry. |

New string resources: 54 strings plus 1 string array of 15 items, so 55 resources in total. One string was removed.

## Key implementation decisions

- **One clickable node per row (D3, NFR1).** The help line is a second `Text` in the row's `supportingContent`, so `clickable` merges title, value and help. The click label "Change <setting>" is unchanged, and no `maxLines` is set, so text wraps at a large font size (NFR2).
- **Exhaustive `when` mappings (D1).** `details()` also covers the three driving settings, which never open a list. That keeps the compile-time guarantee for every `WatchSetting`. The two driving speeds share one help line, and each has its own details text with its own default (25 km/h or 15 mph).
- **Units (D2, FR5.3).** Only the early-warning distance names a distance, so it is the only text with an `_imperial` variant (25 m or 80 ft). It is chosen with `imperial = model.units == 1`, the same way `valueLabels` chooses.
- **Shortcut descriptions (D5).** `valueDescriptions` returns the array for `setting.isShortcut`, which covers exactly the seven slots and not Touch. Descriptions are looked up by value, so the existing capability filter still hides Next backlight colour and its description without an RGB backlight.
- **No help about missing features (FR5.2).** Only the Touch and gesture rows mention touch, swipes or taps, and those rows only exist on a touch watch. The action descriptions are shown on the button lists too, so none of them mentions a gesture. The Units help and details leave out the driving speed, because When driving needs protocol 1.4. The Colour explanation says "Only the colours your watch can show are listed" instead of naming the black-and-white case.
- **`WatchSettingsUiTest` runs on Robolectric**, so the help texts and arrays resolve the way `DisplayLabelsTest` resolves its labels. The existing tests in the class are unchanged.
- **Large-font test** uses `@GraphicsMode(NATIVE)`, as `StoreScreenshotsTest` does, so the text layout is real. It checks `hasVisualOverflow == false`, more than one line, the last line ending at the full text length, and the right edge inside the screen.

## Wording check (setting → watch behaviour → source file)

| Setting / text | What the help says | Watch behaviour checked | Source |
|---|---|---|---|
| Colour | Colours the panel with the arrow and the distance. The ETA strip takes a matching shade, panel text stays readable, and only colours the watch can show are listed. | `panel_bg = accent`, `panel_fg = legible_over(accent)`, `strip_bg = shade_of(accent)`. A B/W watch lists only Black and White. | `watchapp/src/c/theme.c` (theme, `accent_for_row`) |
| Invert | Directions text dark on light or light on dark. Default Dark on light. | `road_bg/road_fg` flip, and only the road area changes. Default 0 = Dark on light. | `theme.c`, `settings_sync.c` defaults |
| Glyph pack | Arrow style. Classic default, Bold thick solid, Outline hollow. | Pack chooses the resource set only. Images checked in `resources/images/bold/` and `outline/`. Default Classic. | `main.c` (`maneuver_resource_id`), REQ-WATCH-012 |
| Arrow side | Arrow right or left, distance on the other side. Default Right. | `arrow_left` places the glyph and right-aligns the distance. Value labels "Right (distance left)". | `main.c` panel layout, `settings_window.c` |
| Directions text | Full is the whole instruction, Compact only the street. Compact falls back to full without a street. Default Full. | The phone sends `compactText` when the rule produced one, else `primaryText`. | `directions_text.h`, REQ-ANDROID-025 |
| ETA display | Arrival clock time or time left. Default Arrival time. | `ETA_MODE_DURATION` shows minutes until arrival. | `main.c` (`settings_eta_mode`), `eta_text.c` |
| ETA label | "ETA at / ETA in" or "ETA / IN". It follows ETA display. | `eta_label(duration, labelled)` returns "ETA at"/"ETA in" or "ETA"/"IN". | `eta_text.c`, `theme.c` (`eta_label_style_name`) |
| Distance units | Every distance on the watch, including the early warning. Default km/m. | `units_name`, `early_warn_distance_label(..., imperial)`. | `theme.c`, `early_warning.c` |
| Backlight duration | Watch default leaves it to the watch. Low 3 s and Medium 10 s on each update. High stays on until the app closes. More time, more battery. | `settings_backlight_hold_ms`: 3000, 10000, 0 (steady). `apply_backlight()` runs on every state update. OFF means no forcing, and the phone's light request is honoured. | `theme.c`, `main.c` lines 794–845, 917–924 |
| Backlight colour | Tints the light when PebbleNTN lights it (Low/Medium/High). Watch default keeps the watch's colour. | `settings_apply_backlight_color()` is called in `apply_backlight()`, only when the duration is not Watch default. DEFAULT means `light_set_system_color()`. Only with `PBL_RGB_BACKLIGHT`. | `theme.c` lines 177–214, `main.c` line 837 |
| New instruction pattern | Off default, no buzz. Short pulse buzzes briefly when the next turn changes. Single to Long buzz each time the instruction changes, at the chosen strength. | Off ignores everything. PULSE gives `vibes_short_pulse()` on a maneuver change (the phone flag, default on). Other patterns run `settings_vibe_play()` on `new_info` (maneuver or road text changed). | `main.c` lines 885–907, `settings_sync.c` defaults |
| New instruction strength | Stronger means a longer buzz. Medium default. It does not change Short pulse. | `vibe_pulse_ms`: 100, 220 or 400 ms ("a longer buzz is what reads as stronger"). PULSE always calls `vibes_short_pulse()`. | `theme.c` lines 295–346 |
| Early warning distance | Buzzes when the next turn is within the distance. Default 25 m / 80 ft. Follows units. Off hides the other rows. | `early_warn_should_fire`, thresholds 10/25/50/100/200 m, feet table 30/80/160/330/650. Default index 2. The phone hides the rows while it is 0. | `early_warning.c`, `settings_sync.c`, `WatchSettingsUi.sections` |
| Early warning repeat | Once per instruction, or again whenever the distance changes inside the range (default). A new instruction's own buzz counts as the warning. | REPEAT_EVERY fires when `distance != warned_at_m`. Once fires a single time. `new_info && new_info_buzzed` records the warning without firing. Default 1. | `early_warning.c` |
| Early warning pattern / strength | Different feel from the new-instruction buzz. Double / Medium default. | Separate `settings_early_warn_play()` with its own pattern and strength. Pattern list starts at Single (firstValue 1). Defaults Double and 1. | `theme.c` line 348, `settings_sync.c`, `WatchSettingsModel.kt` |
| Touch | Swipes and taps on the navigation screen run the chosen actions. Touch must also be on in the watch's own settings. Default on. | `run_touch_shortcut` requires `settings_touch_enabled() && touch_service_is_enabled()`. Default 1. | `main.c` lines 1262–1286 |
| Up / Down button | Action for a press on the navigation screen. Most changes show a notice. Middle button still opens settings and Back still closes the app. Defaults: ETA toggle / Backlight now. | `shortcut_default`, `shortcut_notice` (none for None, Backlight now, Open settings). SELECT and BACK unchanged per REQ-WATCH-024. | `shortcuts.c`, REQ-WATCH-024 |
| Swipe left/right/up/down, Tap | Action for the gesture. Works while Touch is on. Defaults: ETA toggle on left/right, nothing on up/down, Backlight now on Tap. | `shortcut_default` and the gesture gate above. | `shortcuts.c`, `main.c` |
| Hide directions when driving (switch) | Closes the watchapp while you drive and brings it back when you slow down. | The phone closes the watchapp above the speed and reopens it after slowing down. Same meaning as the kept `watch_driving_explanation`. | REQ-ANDROID-024, `strings.xml` (`watch_driving_explanation`) |
| Driving speed | Above it you count as driving. Follows Distance units. Defaults 25 km/h / 15 mph. | Metric or imperial speed per units. Defaults `DRIVING_METRIC_DEFAULT` / `IMPERIAL_DEFAULT`. | `settings_sync.c`, `WatchSettingsUi.drivingDefaultValue` |
| Action: None | Does nothing. | `ACTION_NONE`: no change, no notice. | `shortcut_apply.c` |
| Action: ETA toggle | Switches between arrival time and time left. | `step_setting(SET_ETA_MODE, 1)`. | `shortcut_apply.c` |
| Action: ETA label style | Switches between the two labels. | `step_setting(SET_ETA_LABEL, 1)`. | `shortcut_apply.c` |
| Action: Next / Previous colour | Next or previous colour the watch can show. | `step_accent(±1)` over `ACCENT_AVAILABLE_COUNT` (B/W: Black, White). | `shortcut_apply.c` |
| Action: Invert colours | Switches the directions text dark/light. | `step_setting(SET_INVERT, 1)`. | `shortcut_apply.c` |
| Action: Swap arrow side | Moves the arrow, and the distance with it. | `step_setting(SET_ARROW_LEFT, 1)`. | `shortcut_apply.c` |
| Action: Next glyph pack | Classic, Bold, then Outline. | `step_setting(SET_GLYPH_PACK, 1)`, wrapping. | `shortcut_apply.c` |
| Action: Vibration on / off | Turns the new-instruction buzz off, or back on with the last pattern. Early warning untouched. | `shortcut_vibe_toggle` with `settings_vibe_last_pattern()`. Comment: "the early warning is untouched". | `shortcut_apply.c`, `shortcuts.c` |
| Action: Early warning next / previous | Steps the distance up (after the longest, Off) or down (after Off, the longest). | `step_setting(SET_WARN_DISTANCE, ±1)` with `shortcut_cycle` wrapping over Off..200 m. | `shortcut_apply.c`, `shortcuts.c` |
| Action: Backlight now | Turns on the backlight for a moment. | `light_enable_interaction()`. | `shortcut_apply.c` |
| Action: Next backlight colour | Next backlight colour. Offered only with an RGB backlight. | `step_setting(SET_BACKLIGHT_COLOR, 1)` under `PBL_RGB_BACKLIGHT`. `shortcut_offered_count`. | `shortcut_apply.c`, `shortcuts.c` |
| Action: Units | Switches between kilometres and miles. | `step_setting(SET_UNITS, 1)`. | `shortcut_apply.c` |
| Action: Open settings | Opens the settings menu. | `result.open_settings = true`. | `shortcut_apply.c` |

Found while checking: `spec/400-ui/WatchUI.md` still calls the early warning "Off — the default". The watchapp's fresh-install default has been 25 m since #102 (`settings_sync.c` `kDefaults`), and the help follows the code. The stale spec line is out of scope here and is noted as a follow-up.

## Test coverage summary

Baseline (Step 1, before any change):
`android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.watchsettings.WatchSettingsUiTest' --tests 'com.pebblentn.app.ui.watchsettings.WatchSettingsScreenTest'`: BUILD SUCCESSFUL, WatchSettingsUiTest 12/12, WatchSettingsScreenTest 14/14.

Tests added in `WatchSettingsUiTest` (Step 3; `--tests '...WatchSettingsUiTest'` gave 16/16, 0 failures):
- `everySettingHasAShortHelpLineAndAFullerExplanation` (FR1.1 length ≤ 90, FR1.4, FR2.3)
- `onlyTheShortcutSlotsDescribeTheirValuesOnePerAction` (FR3.1, FR3.3)
- `theEarlyWarningExplanationFollowsTheUnits` (FR5.3)
- `onlyTheTouchRowsMentionTouchGestures` (FR5.2)
- updated: `theDirectionsSectionIsOfferedOnEveryWatchWithoutANote` (FR4.1)

Tests added in `WatchSettingsScreenTest` (Step 5; `--tests '...WatchSettingsScreenTest'` gave 25/25, 0 failures):
- `everyRowShowsItsValueAndItsHelpLine` (FR1.1–FR1.3)
- `aWatchWithoutTouchOrAnRgbBacklightShowsNoHelpAboutThem` (FR5.1, FR5.2)
- `theDrivingRowsHaveHelpAndTheDrivingParagraphStays` (FR4.2–FR4.4)
- `theBacklightListExplainsTheBatteryCostAboveTheValues` (FR2.1, FR2.2)
- `theColourListExplainsAboveThePreview` (FR2.1)
- `withMilesTheEarlyWarningExplanationIsInFeet` (FR5.3)
- `theUpButtonListDescribesEveryAction` (FR3.1)
- `withoutAnRgbBacklightTheBacklightColourActionAndItsDescriptionAreAbsent` (FR3.2)
- `aNonShortcutListHasNoValueDescriptions` (FR3.3)
- `atTwiceTheFontSizeTheHelpWrapsInFull` (NFR2)
- `aRowIsOneAccessibleItemWithTitleValueAndHelp` (NFR1)
- updated: `directionsTextShowsForA13WatchappWithItsValueAndExplanation` (FR4.1: the old note is absent, the row help is present)

Other commands:
- Final scoped run of both classes (the baseline command): BUILD SUCCESSFUL, 16 + 25 = 41 tests, 0 failures.
- Full debug JVM suite `android/gradlew -p android :app:testDebugUnitTest`: BUILD SUCCESSFUL, 768 tests, 0 failures, 0 errors, 4 skipped. That is the 753 of the last recorded run plus the 15 new tests, so nothing regressed (NFR3).
- `PEBBLENTN_STORE_SCREENSHOTS_DIR=$PWD/tools/play-store-sources/phone android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.StoreScreenshotsTest'`: BUILD SUCCESSFUL. Only `2-watch-settings.png` and `3-accent-colour.png` changed; no other PNG changed, so nothing was reverted.
- `tools/play_store_graphics.py` and `--check`: run with the pebble tool's Python (`/home/sacha/.local/share/uv/tools/pebble-tool/bin/python`), as the script's docstring prescribes, because the system `python3` has no Pillow. Result: "Play store images OK", exit 0. Only `phoneScreenshots/2.png` and `3.png` changed.
- `scripts/validate-spec-assets.sh`: "Spec asset validation OK (34 files)."
- `android/gradlew -p android :app:lintDebug`: BUILD SUCCESSFUL, 0 errors, 72 warnings, none in a changed Kotlin file or in the Watch settings block of `strings.xml` (all existing ones: PluralsCandidate, UnusedResources and others at strings.xml lines 18–255).
- `git diff --stat`: 14 planned files, no Gradle or dependency change.

## Tests written but not executed

None. Every test written was run and passed. `scripts/test-all.sh` (with the watch host tests and the workbench) and the release APK size measurement were not run. The plan places them in Build and Test.

## Deviations from the plan

- **Step 7, Pillow.** The plan names `python3 tools/play_store_graphics.py`. The system `python3` lacks Pillow, so the script was run with the pebble tool's Python, which the script itself names for this purpose. Nothing was installed.
- **Step 6, consolidated spec.** `docs/PebbleNTN-Consolidated-Spec.md` has no generator script in the repository and does not quote the changed sections, so it was left as it is, as the plan allows.
- **Tests.** There are 15 new tests where the instructions estimate about 11. The extra four tighten FR5.2 (a text scan for touch words on the always-shown rows), FR5.3 (feet shown on screen), FR2.1 (the explanation above the colour preview) and FR3.3 (a plain row's semantics). `WatchSettingsUiTest` now runs on Robolectric, as Step 3 foresaw for the resource checks.
- **Value style.** The row's value uses `bodyMedium` explicitly, the style `ListItem` already gave it, so its look is unchanged.
