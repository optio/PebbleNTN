# Requirements — Shortcuts: touch gestures and UP/DOWN buttons on the navigation screen (#12)

## Sources

- Initial description: [desc] "Work on GitHub issue #12 (Touch support: configurable gestures on touchscreen Pebbles). Challenge the requirements based on how the app evolved since the issue was created."
- Workflow-selected scope: [scope] express
- GitHub issue #12, its body as edited on 2026-10-23
- The code knowledge base for `watchapp/` and `requirements/REQ-WATCH.md` (`aidlc/spaces/default/codekb/PebbleNTN/`, focused scan at 3727ddc)
- The emery SDK 4.33.1 header `pebble.h` (tap and swipe recognizers, `touch_service_*`, `window_set_touch_bridge_disabled`)
- Answers Q1–Q5 in `requirements-analysis-questions.md`

## Intent Analysis

- **Request type:** a new watchapp feature, brownfield. Only the watchapp changes; there is no protocol or phone-app change.
- **What changed since the issue was written:**
  - Touch browsing in the settings menus shipped with #53 (REQ-WATCH-021), so it leaves #12.
  - The ETA label style (#13) and the early warning (REQ-WATCH-022) are new settings.
  - The navigation screen keeps the system touch bridge off (#53), so today it ignores touch entirely.
- **Corrections to the issue:**
  - The firmware maps swipe right to BACK, not swipe left.
  - The "RGB background" isn't a setting; the RGB setting is the backlight colour (Q4).
  - "Switch ETA / IN" is renamed, because "ETA / IN" is now a label style.
- **Answers that change the issue:**
  - Five gestures, not seven [Q1].
  - Touch is off by default [Q2].
  - The vibration action switches only the New instruction pattern [Q3].
  - Two more actions [Q4].
  - UP and DOWN get the same choice of actions on every watch [Q5].
- **Scope estimate:** about 4 files changed (`main.c`, `settings_window.c`, `theme.c/.h`), a new pure unit with its host test, REQ-WATCH-024, the screenshot tool row indices, and the docs.
- **Requirements depth:** Minimal. The feature is a settings submenu, one input handler and a short notice.

## Functional Requirements

### FR1 — A "Shortcuts" settings submenu [Q5]

- The main settings menu SHALL get a **Shortcuts** row on every watch, just above About (About stays last).
- The Shortcuts window SHALL list:
  - **Up button** and **Down button**, on every watch;
  - on a watch with touch, a **Touch** row (On / Off) and, while Touch is On, **Swipe left**, **Swipe right**, **Swipe up**, **Swipe down** and **Tap** [Q1].
- The touch rows SHALL be hidden on watches without touch, and while `touch_service_is_enabled()` is false. In the current targets only emery has touch.
- Selecting a shortcut row SHALL open an action list showing the current action as "Selected", like the other pickers.
- Every choice SHALL apply at once and persist across launches.
- **Pass/fail:**
  - basalt and aplite show Shortcuts with exactly the two button rows;
  - emery shows Touch Off on a fresh install with no gesture rows, and the five gesture rows appear after turning it On.

### FR2 — Defaults [Q2]

- **Touch:** Off.
- **Up button, Down button, Swipe up, Swipe down, Tap:** None.
- **Swipe left and Swipe right:** "ETA: arrival time / time left". They only take effect once the user turns Touch on.
- A watch that has none of the new values stored SHALL behave exactly as today: no touch reactions, and UP and DOWN do nothing.
- **Pass/fail:** on a fresh install, UP, DOWN and every touch on the navigation screen change nothing.

### FR3 — Actions [Q3, Q4]

Each shortcut is set to one of these actions:

| Action | Effect (the same setter and save as the settings menu) | Notice |
|---|---|---|
| None | nothing | none |
| ETA: arrival time / time left | toggles the ETA mode | "Arrival time" / "Time left" |
| ETA label style | toggles "ETA at / ETA in" ↔ "ETA / IN" | the new style's name |
| Next colour / Previous colour | the next or previous accent, wrapping. Black-and-white watches cycle their Black/White pair | the colour's name |
| Invert colours | toggles Invert | "Light on dark" / "Dark on light" |
| Swap arrow side | toggles the arrow side | "Arrow left" / "Arrow right" |
| Next glyph pack | Classic → Bold → Outline → Classic | "Classic glyphs" / "Bold glyphs" / "Outline glyphs" |
| Vibration on / off | the New instruction pattern: a non-Off pattern becomes Off; Off becomes the last non-Off pattern (Single if there was none), which then plays as a preview. The early warning and the phone's pulse are untouched [Q3] | "Vibration off" / "Vibration: {pattern}" |
| Early warning: next distance / previous distance | steps Off → 10 → 25 → 50 → 100 → 200 m → Off, wrapping (shown in the chosen units) | "Early warning off" / "Early warning {distance}" |
| Backlight now | turns the light on now (`light_enable_interaction`) | none |
| Next backlight colour | the next backlight colour, wrapping; only offered on RGB-backlight watches (emery) [Q4] | the colour's name |
| Units | metric ↔ imperial | the units' name |
| Open settings | the same as SELECT | none |

- The navigation screen SHALL redraw at once, through the same path as returning from Settings.
- **Pass/fail:** each action, triggered once, changes exactly its own setting, and the setting's menu row afterwards shows the new value.

### FR4 — Change notice

- Every action except None, Backlight now and Open settings SHALL show its notice text for about 1.5 seconds. The text is a small banner over the navigation screen, in plain text without arrows.
- The notice SHALL NOT replace or hide the instruction for longer than that.
- A second action within the 1.5 seconds SHALL replace the text and restart the timer.
- **Pass/fail:** a screenshot within 1.5 s shows the banner; a screenshot after 2 s shows none.

### FR5 — Input handling on the navigation screen

- **Buttons:** UP and DOWN single presses on the navigation window SHALL run their shortcuts. SELECT still opens Settings, and BACK still closes the app.
- **Touch:**
  - Swipes and taps SHALL be recognized with the firmware's own swipe and tap recognizers, attached to the navigation window.
  - The system touch bridge stays off there, so no touch can close the app.
  - Gestures SHALL run only while the watchapp's Touch is On and `touch_service_is_enabled()` is true. This follows the watch's own touch setting, as for the menus (REQ-WATCH-021).
- **Pass/fail:**
  - On the emery emulator, with Touch On, `touch_emulator.py swipe` left/right toggles the ETA and a tap with Tap = Invert inverts the colours.
  - With Touch Off, the same gestures change nothing.

### FR6 — Specs and tools

- Add **REQ-WATCH-024** to `requirements/REQ-WATCH.md` and resync `MANIFEST.sha256.json`.
- Update the row indices in `watchapp/tools/capture_screenshots.py` for the new Shortcuts row.
- Note in `docs/IMPLEMENTATION_STATUS.md` that #12 replaces the issue's original assumptions as listed in Intent Analysis.

## Non-Functional Requirements

- **Testability:** the action list, the wrap-around cycling, the vibration toggle and the notice texts SHALL live in a pure unit (for example `watchapp/src/c/shortcuts.c`). It is host-tested in `watchapp/tests/` like `early_warning`, with the happy path plus at least two edge cases (wrap-around at both ends, and the vibration toggle starting from Off with no stored pattern).
- **Compatibility:**
  - The watchapp SHALL build for aplite, basalt, chalk, diorite and emery. Touch code is compiled only where the touch API exists.
  - The new persisted values use keys 19 and up, with no change to keys 1–18.
- **Memory:** the feature SHALL fit aplite's heap. Button shortcuts exist there; touch code is not compiled in.

## Constraints

- No protocol change, and no phone-app change. The release goes through the existing pipeline, then the Pebble store.
- The SDK has no double tap or long press recognizer. They are out of scope [Q1].

## Assumptions

- `touch_service_is_enabled()` returns false when touch is turned off in the watch's system settings. The emulator can't show this, because it has no Settings app. It is checked on a real Pebble Time 2 after release.
- One shortcut action per gesture or button is enough. There are no per-screen variations, such as different actions on the idle screen and during a route.

## Out of Scope

- Double tap and long press [Q1].
- Long presses on UP and DOWN.
- flint and gabbro targets (a separate issue, as #12 already said).
- Touch browsing in Settings (done in #53).

## Open Questions

None.
