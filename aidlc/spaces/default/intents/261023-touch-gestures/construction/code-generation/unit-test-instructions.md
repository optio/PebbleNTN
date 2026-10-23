# Unit Test Instructions — Shortcuts: touch gestures and UP/DOWN buttons (#12)

## Framework and Setup

These are plain C host tests in the watchapp's existing style. Each `watchapp/tests/test_<unit>.c` is compiled with gcc (`-std=c11 -Wall -Wextra -Werror`) together with only `watchapp/src/c/<unit>.c`. They need neither the Pebble SDK nor an emulator.

## How to Run This Work's Tests

From the repo root:

```bash
scripts/test-watchapp-unit.sh
```

The script runs every watchapp host test, including the new `test_shortcuts`. Plan Step 2 runs it before the new test exists. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test | Case | Requirement |
|---|---|---|
| test_shortcuts | `shortcut_default`: swipe left and right give ETA mode, every other slot gives None | FR2 |
| test_shortcuts | `shortcut_valid_action`: an in-range value is kept; a negative value and `ACTION_COUNT` give None | FR2 |
| test_shortcuts | `shortcut_cycle`: +1 in the middle; +1 at the last value wraps to 0; −1 at 0 wraps to the last | FR3 |
| test_shortcuts | `shortcut_vibe_toggle`: Double → Off; Off with last Triple → Triple; Off with last Off (nothing stored) → the Single fallback | FR3 |
| test_shortcuts | `shortcut_action_for_row` / `shortcut_row_for_action`: with an RGB backlight every action is offered and the mapping round-trips; without one, Next backlight colour is not offered and the count is one less | FR3 |
| test_shortcuts | `shortcut_notice`: "Vibration off", "Vibration: Double", "Early warning off", "Early warning 50 m", "Bold glyphs", the plain value for Invert; None, Backlight now and Open settings return false | FR4 |
| test_shortcuts | `shortcut_notice` with a small buffer truncates safely and NUL-terminates | FR4 |
| test_shortcuts | `shortcut_action_name` gives a non-empty label for every action | FR1 |

The input handling, the menus and the banner timing use Pebble APIs. They are checked in the emulator (plan Step 10).

## Coverage Target

Minimal strategy: one test per unit-testable requirement (FR1–FR4), and a happy path plus at least two edge cases for `shortcuts.c` (wrap-around at both ends, the vibration toggle with no stored pattern, an out-of-range stored value). Express has no line-coverage floor.

## Mocking and Test Data

No mocks. `shortcuts.c` takes plain integers and strings. The tests pass enum values and notice values such as "Double" and "50 m" directly.
