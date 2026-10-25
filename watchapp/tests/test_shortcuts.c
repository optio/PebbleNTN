// Host-compiled unit tests for the navigation-screen shortcuts (REQ-WATCH-024, #12): the defaults,
// the action list with and without an RGB backlight, the wrap-around cycling, the vibration toggle
// and the notice texts. Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/shortcuts.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void expect_int(const char *what, int got, int want) {
  if (got != want) {
    printf("  FAIL  %s: expected %d, got %d\n", what, want, got);
    failures++;
  } else {
    printf("  ok    %s == %d\n", what, got);
  }
}

static void expect_true(const char *what, bool got) {
  if (!got) {
    printf("  FAIL  %s\n", what);
    failures++;
  } else {
    printf("  ok    %s\n", what);
  }
}

// `action` with `value` shows the notice `want`.
static void expect_notice(ShortcutAction action, const char *value, const char *want) {
  char buf[32] = "unchanged";
  const bool shown = shortcut_notice(buf, sizeof(buf), action, value);
  if (!shown || strcmp(buf, want) != 0) {
    printf("  FAIL  notice(%d, \"%s\"): expected \"%s\", got %s \"%s\"\n", (int)action, value, want,
           shown ? "shown" : "not shown", buf);
    failures++;
  } else {
    printf("  ok    notice(%d, \"%s\") == \"%s\"\n", (int)action, value, buf);
  }
}

// `action` shows no notice, and leaves the buffer alone.
static void expect_no_notice(ShortcutAction action) {
  char buf[32] = "unchanged";
  const bool shown = shortcut_notice(buf, sizeof(buf), action, "Blue");
  if (shown || strcmp(buf, "unchanged") != 0) {
    printf("  FAIL  notice(%d): expected none, got \"%s\"\n", (int)action, buf);
    failures++;
  } else {
    printf("  ok    notice(%d): none\n", (int)action);
  }
}

int main(void) {
  printf("defaults (FR2)\n");
  expect_int("swipe left", shortcut_default(SLOT_SWIPE_LEFT), ACTION_ETA_MODE);
  expect_int("swipe right", shortcut_default(SLOT_SWIPE_RIGHT), ACTION_ETA_MODE);
  expect_int("up button", shortcut_default(SLOT_UP_BUTTON), ACTION_ETA_MODE);
  expect_int("down button", shortcut_default(SLOT_DOWN_BUTTON), ACTION_BACKLIGHT_NOW);
  expect_int("swipe up", shortcut_default(SLOT_SWIPE_UP), ACTION_NONE);
  expect_int("swipe down", shortcut_default(SLOT_SWIPE_DOWN), ACTION_NONE);
  expect_int("tap", shortcut_default(SLOT_TAP), ACTION_BACKLIGHT_NOW);

  printf("stored values (FR2)\n");
  expect_int("in range kept", shortcut_valid_action(ACTION_INVERT), ACTION_INVERT);
  expect_int("last action kept", shortcut_valid_action(ACTION_OPEN_SETTINGS), ACTION_OPEN_SETTINGS);
  expect_int("negative is None", shortcut_valid_action(-1), ACTION_NONE);
  expect_int("ACTION_COUNT is None", shortcut_valid_action(ACTION_COUNT), ACTION_NONE);
  expect_int("255 is None", shortcut_valid_action(255), ACTION_NONE);

  printf("cycling (FR3)\n");
  expect_int("+1 in the middle", shortcut_cycle(1, 3, 1), 2);
  expect_int("+1 at the last wraps to 0", shortcut_cycle(2, 3, 1), 0);
  expect_int("-1 at 0 wraps to the last", shortcut_cycle(0, 6, -1), 5);
  expect_int("-1 in the middle", shortcut_cycle(3, 6, -1), 2);
  expect_int("black-and-white pair: +1 at White wraps to Black", shortcut_cycle(1, 2, 1), 0);
  expect_int("no values", shortcut_cycle(0, 0, 1), 0);

  printf("vibration toggle with silent Off (FR3, #102 FR8)\n");
  // As shortcut_apply calls it: Off is VIBE_PATTERN_SILENT, the fallback is Short pulse.
  expect_int("Double turns off",
             shortcut_vibe_toggle(VIBE_PATTERN_DOUBLE, VIBE_PATTERN_DOUBLE, VIBE_PATTERN_SILENT,
                                  VIBE_PATTERN_PULSE),
             VIBE_PATTERN_SILENT);
  expect_int("Short pulse turns off",
             shortcut_vibe_toggle(VIBE_PATTERN_PULSE, VIBE_PATTERN_PULSE, VIBE_PATTERN_SILENT,
                                  VIBE_PATTERN_PULSE),
             VIBE_PATTERN_SILENT);
  expect_int("Off with last Triple turns Triple on",
             shortcut_vibe_toggle(VIBE_PATTERN_SILENT, VIBE_PATTERN_TRIPLE, VIBE_PATTERN_SILENT,
                                  VIBE_PATTERN_PULSE),
             VIBE_PATTERN_TRIPLE);
  expect_int("Off with Short pulse last turns Short pulse on",
             shortcut_vibe_toggle(VIBE_PATTERN_SILENT, VIBE_PATTERN_PULSE, VIBE_PATTERN_SILENT,
                                  VIBE_PATTERN_PULSE),
             VIBE_PATTERN_PULSE);
  expect_int("Off with no last pattern falls back to Short pulse",
             shortcut_vibe_toggle(VIBE_PATTERN_SILENT, VIBE_PATTERN_SILENT, VIBE_PATTERN_SILENT,
                                  VIBE_PATTERN_PULSE),
             VIBE_PATTERN_PULSE);

  printf("vibration patterns (#102 FR8)\n");
  expect_true("0 is Short pulse",
              strcmp(vibe_pattern_name(VIBE_PATTERN_PULSE), "Short pulse") == 0);
  expect_true("5 is Off", strcmp(vibe_pattern_name(VIBE_PATTERN_SILENT), "Off") == 0);
  expect_true("Double", strcmp(vibe_pattern_name(VIBE_PATTERN_DOUBLE), "Double") == 0);
  expect_true("out of range reads Off", strcmp(vibe_pattern_name((VibePatternId)9), "Off") == 0);
  expect_int("stored values are fixed: Off is 5", VIBE_PATTERN_SILENT, 5);
  expect_int("menu order: Off then Short pulse", vibe_pattern_next(VIBE_PATTERN_SILENT),
             VIBE_PATTERN_PULSE);
  expect_int("menu order: Short pulse then Single", vibe_pattern_next(VIBE_PATTERN_PULSE),
             VIBE_PATTERN_SINGLE);
  expect_int("menu order: Long wraps to Off", vibe_pattern_next(VIBE_PATTERN_LONG),
             VIBE_PATTERN_SILENT);
  expect_int("menu order: out of range goes to Off", vibe_pattern_next((VibePatternId)9),
             VIBE_PATTERN_SILENT);
  int visited = 0;
  VibePatternId walk = VIBE_PATTERN_SILENT;
  do {
    walk = vibe_pattern_next(walk);
    visited++;
  } while (walk != VIBE_PATTERN_SILENT && visited <= VIBE_PATTERN_COUNT);
  expect_int("the menu visits every pattern once", visited, VIBE_PATTERN_COUNT);
  // The shortcut notice for Off still reads "Vibration off" (shortcut_notice matches "Off").
  expect_notice(ACTION_VIBRATION, vibe_pattern_name(VIBE_PATTERN_SILENT), "Vibration off");

  printf("action list (FR3)\n");
  expect_int("RGB backlight: every action offered", shortcut_offered_count(true), ACTION_COUNT);
  expect_int("no RGB backlight: one less", shortcut_offered_count(false), ACTION_COUNT - 1);
  bool round_trip = true;
  bool colour_offered = false;
  for (int row = 0; row < shortcut_offered_count(true); row++) {
    const ShortcutAction action = shortcut_action_for_row(row, true);
    round_trip = round_trip && shortcut_row_for_action(action, true) == row;
    colour_offered = colour_offered || action == ACTION_NEXT_BACKLIGHT_COLOUR;
  }
  expect_true("RGB backlight: rows round-trip", round_trip);
  expect_true("RGB backlight: Next backlight colour offered", colour_offered);
  round_trip = true;
  colour_offered = false;
  for (int row = 0; row < shortcut_offered_count(false); row++) {
    const ShortcutAction action = shortcut_action_for_row(row, false);
    round_trip = round_trip && shortcut_row_for_action(action, false) == row;
    colour_offered = colour_offered || action == ACTION_NEXT_BACKLIGHT_COLOUR;
  }
  expect_true("no RGB backlight: rows round-trip", round_trip);
  expect_true("no RGB backlight: Next backlight colour not offered", !colour_offered);
  expect_int("no RGB backlight: last row is Open settings",
             shortcut_action_for_row(shortcut_offered_count(false) - 1, false), ACTION_OPEN_SETTINGS);
  expect_int("no RGB backlight: Units moves up one",
             shortcut_row_for_action(ACTION_UNITS, false), ACTION_UNITS - 1);
  expect_int("row past the end is None", shortcut_action_for_row(ACTION_COUNT, true), ACTION_NONE);

  printf("notices (FR4)\n");
  expect_notice(ACTION_VIBRATION, "Off", "Vibration off");
  expect_notice(ACTION_VIBRATION, "Double", "Vibration: Double");
  expect_notice(ACTION_EARLY_WARN_NEXT, "Off", "Early warning off");
  expect_notice(ACTION_EARLY_WARN_PREV, "50 m", "Early warning 50 m");
  expect_notice(ACTION_NEXT_GLYPH, "Bold", "Bold glyphs");
  expect_notice(ACTION_ARROW_SIDE, "left", "Arrow left");
  expect_notice(ACTION_INVERT, "Light on dark", "Light on dark");
  expect_notice(ACTION_ETA_MODE, "Time left", "Time left");
  expect_no_notice(ACTION_NONE);
  expect_no_notice(ACTION_BACKLIGHT_NOW);
  expect_no_notice(ACTION_OPEN_SETTINGS);
  expect_no_notice(ACTION_COUNT);

  printf("notice in a small buffer (FR4)\n");
  char small[8];
  memset(small, 'x', sizeof(small));
  expect_true("still shown", shortcut_notice(small, sizeof(small), ACTION_VIBRATION, "Double"));
  expect_true("truncated and NUL-terminated", strcmp(small, "Vibrati") == 0);
  expect_true("no buffer: not shown", !shortcut_notice(NULL, 0, ACTION_INVERT, "Light on dark"));

  printf("action names (FR1)\n");
  bool named = true;
  for (int action = 0; action < ACTION_COUNT; action++) {
    const char *name = shortcut_action_name((ShortcutAction)action);
    named = named && name != NULL && name[0] != '\0';
  }
  expect_true("every action has a label", named);
  expect_true("ETA mode label", strcmp(shortcut_action_name(ACTION_ETA_MODE),
                                       "ETA: arrival time / time left") == 0);

  if (failures != 0) {
    printf("\n%d assertion(s) FAILED\n", failures);
    return 1;
  }
  printf("\nAll shortcuts assertions passed.\n");
  return 0;
}
