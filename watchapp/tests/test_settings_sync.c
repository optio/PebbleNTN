// Host-compiled unit tests for the settings sync's pure part (REQ-WATCH-025, #102): whose settings
// win, which values a watch takes, the defaults and the persist keys. Run with
// scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.
//
// Links: revision

#include "../src/c/settings_sync.h"

#include "../src/c/generated/protocol.h"
#include "../src/c/shortcuts.h"

#include <stdio.h>

static int failures;

static void expect_int(const char *what, int got, int want) {
  if (got != want) {
    printf("  FAIL  %s: expected %d, got %d\n", what, want, got);
    failures++;
  } else {
    printf("  ok    %s == %d\n", what, got);
  }
}

static void expect_bool(const char *what, bool got, bool want) {
  if (got != want) {
    printf("  FAIL  %s: expected %s\n", what, want ? "true" : "false");
    failures++;
  } else {
    printf("  ok    %s == %s\n", what, got ? "true" : "false");
  }
}

#define COLOUR PBNTN_CAP_COLOR_MASK
#define RGB PBNTN_CAP_RGB_BACKLIGHT_MASK
#define TOUCH PBNTN_CAP_TOUCH_MASK
#define BASALT (COLOUR)
#define EMERY (COLOUR | RGB | TOUCH)
#define APLITE 0u

int main(void) {
  printf("decide (FR3)\n");
  expect_int("phone 1 ahead: apply", sync_decide(10, 11, true), SYNC_APPLY_PHONE);
  expect_int("phone 49 ahead: apply", sync_decide(10, 59, false), SYNC_APPLY_PHONE);
  expect_int("rollover: phone 0 after watch 99: apply", sync_decide(99, 0, true), SYNC_APPLY_PHONE);
  expect_int("watch newer: keep", sync_decide(12, 11, true), SYNC_KEEP_WATCH);
  expect_int("50 apart: the watch wins", sync_decide(10, 60, true), SYNC_KEEP_WATCH);
  expect_int("equal, values differ: the watch wins", sync_decide(10, 10, true), SYNC_KEEP_WATCH);
  expect_int("equal, same values: same", sync_decide(10, 10, false), SYNC_SAME);
  expect_int("no phone revision: keep", sync_decide(10, -1, false), SYNC_KEEP_WATCH);
  expect_int("out-of-range phone revision: keep", sync_decide(10, 150, true), SYNC_KEEP_WATCH);

  printf("valid: ranges (FR3)\n");
  expect_bool("units 1", sync_valid(SET_UNITS, 1, BASALT), true);
  expect_bool("units 2 is out of range", sync_valid(SET_UNITS, 2, BASALT), false);
  expect_bool("negative is out of range", sync_valid(SET_GLYPH_PACK, -1, BASALT), false);
  expect_bool("bool 2 is out of range", sync_valid(SET_INVERT, 2, BASALT), false);
  expect_bool("vibe pattern Off (5)", sync_valid(SET_VIBE_PATTERN, VIBE_PATTERN_SILENT, BASALT), true);
  expect_bool("vibe pattern 6", sync_valid(SET_VIBE_PATTERN, 6, BASALT), false);
  expect_bool("early warning pattern 0 (none)", sync_valid(SET_WARN_PATTERN, 0, BASALT), false);
  expect_bool("early warning pattern Long", sync_valid(SET_WARN_PATTERN, 4, BASALT), true);
  expect_bool("early warning pattern Off (5)", sync_valid(SET_WARN_PATTERN, 5, BASALT), false);
  expect_bool("shortcut action past the last", sync_valid(SET_SHORTCUT_FIRST, ACTION_COUNT, BASALT), false);
  expect_bool("unknown setting", sync_valid(SET_COUNT, 0, EMERY), false);

  printf("valid: capabilities (FR3)\n");
  expect_bool("colour accent on a colour watch", sync_valid(SET_ACCENT, 1, BASALT), true);
  expect_bool("colour accent on black-and-white", sync_valid(SET_ACCENT, 1, APLITE), false);
  expect_bool("Black on black-and-white", sync_valid(SET_ACCENT, 14, APLITE), true);
  expect_bool("White on black-and-white", sync_valid(SET_ACCENT, 15, APLITE), true);
  expect_bool("backlight colour without RGB", sync_valid(SET_BACKLIGHT_COLOR, 3, BASALT), false);
  expect_bool("backlight colour with RGB", sync_valid(SET_BACKLIGHT_COLOR, 3, EMERY), true);
  expect_bool("RGB action on Up without RGB",
              sync_valid(SET_SHORTCUT_FIRST + SLOT_UP_BUTTON, ACTION_NEXT_BACKLIGHT_COLOUR, BASALT),
              false);
  expect_bool("RGB action on Up with RGB",
              sync_valid(SET_SHORTCUT_FIRST + SLOT_UP_BUTTON, ACTION_NEXT_BACKLIGHT_COLOUR, EMERY),
              true);
  expect_bool("Touch without touch", sync_valid(SET_TOUCH, 1, BASALT), false);
  expect_bool("Touch with touch", sync_valid(SET_TOUCH, 1, EMERY), true);
  expect_bool("swipe slot without touch",
              sync_valid(SET_SHORTCUT_FIRST + SLOT_SWIPE_LEFT, ACTION_INVERT, BASALT), false);
  expect_bool("tap slot with touch", sync_valid(SET_SHORTCUT_FIRST + SLOT_TAP, ACTION_INVERT, EMERY),
              true);
  expect_bool("Down slot without touch",
              sync_valid(SET_SHORTCUT_FIRST + SLOT_DOWN_BUTTON, ACTION_INVERT, APLITE), true);
  expect_bool("every capability: range only", sync_valid(SET_ACCENT, 3, SYNC_CAPS_ALL), true);

  printf("defaults\n");
  expect_int("accent on colour: Green", sync_default(SET_ACCENT, BASALT), 0);
  expect_int("accent on black-and-white: Black", sync_default(SET_ACCENT, APLITE), 14);
  expect_int("vibe pattern: Off", sync_default(SET_VIBE_PATTERN, EMERY), VIBE_PATTERN_SILENT);
  expect_int("vibe strength: Medium", sync_default(SET_VIBE_INTENSITY, EMERY), 1);
  expect_int("early warning pattern: Double", sync_default(SET_WARN_PATTERN, EMERY), VIBE_PATTERN_DOUBLE);
  expect_int("swipe left: ETA toggle", sync_default(SET_SHORTCUT_FIRST + SLOT_SWIPE_LEFT, EMERY),
             ACTION_ETA_MODE);
  expect_int("early warning: 25 m", sync_default(SET_WARN_DISTANCE, EMERY), 2);
  expect_int("early warning: every update", sync_default(SET_WARN_REPEAT, EMERY), 1);
  expect_int("early warning strength: Medium", sync_default(SET_WARN_INTENSITY, EMERY), 1);
  expect_int("Touch: On", sync_default(SET_TOUCH, EMERY), 1);
  expect_int("Up: ETA toggle", sync_default(SET_SHORTCUT_FIRST + SLOT_UP_BUTTON, EMERY), ACTION_ETA_MODE);
  expect_int("Down: Backlight now", sync_default(SET_SHORTCUT_FIRST + SLOT_DOWN_BUTTON, EMERY),
             ACTION_BACKLIGHT_NOW);
  expect_int("Tap: Backlight now", sync_default(SET_SHORTCUT_FIRST + SLOT_TAP, EMERY),
             ACTION_BACKLIGHT_NOW);
  expect_int("swipe up: None", sync_default(SET_SHORTCUT_FIRST + SLOT_SWIPE_UP, EMERY), ACTION_NONE);
  bool defaults_valid = true;
  for (int id = 0; id < SET_COUNT; id++) {
    defaults_valid = defaults_valid && sync_valid((SettingId)id, sync_default((SettingId)id, EMERY), EMERY);
  }
  expect_bool("every default is valid on a watch with everything", defaults_valid, true);

  printf("table\n");
  expect_int("23 settings", SET_COUNT, 23);
  expect_int("accent count", sync_value_count(SET_ACCENT), 16);
  expect_int("shortcut count", sync_value_count(SET_SHORTCUT_FIRST + SLOT_TAP), ACTION_COUNT);
  expect_int("persist key of the accent", (int)sync_persist_key(SET_ACCENT), 1);
  expect_int("persist key of the last early warning setting",
             (int)sync_persist_key(SET_WARN_INTENSITY), 15);
  expect_int("persist key of Touch", (int)sync_persist_key(SET_TOUCH), 19);
  expect_int("persist key of Tap", (int)sync_persist_key(SET_SHORTCUT_FIRST + SLOT_TAP), 26);
  expect_bool("Invert is a bool", sync_is_bool(SET_INVERT), true);
  expect_bool("Units is an int", sync_is_bool(SET_UNITS), false);
  expect_int("protocol key of Tap", PBNTN_KEY_SETTING_ACCENT + SET_SHORTCUT_FIRST + SLOT_TAP,
             PBNTN_KEY_SETTING_SHORTCUT_TAP);
  expect_int("host build: no capabilities", sync_build_caps(), 0);
  expect_int("host build: unknown platform", sync_build_platform(), PBNTN_PLATFORM_UNKNOWN);

  if (failures != 0) {
    printf("\n%d assertion(s) FAILED\n", failures);
    return 1;
  }
  printf("\nAll settings_sync assertions passed.\n");
  return 0;
}
