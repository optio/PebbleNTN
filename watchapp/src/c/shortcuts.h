// Shortcuts on the navigation screen (REQ-WATCH-024, #12): what UP, DOWN and, on a touch watch, each
// swipe and the tap do. This module holds the slots, the actions with their menu labels, the
// defaults, the wrap-around cycling and the notice text shown after an action, kept apart from the
// persisted settings and the Pebble UI (shortcut_apply.c runs an action through the settings).
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_shortcuts.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>

// Where a shortcut is triggered from. The order is also the order of the persist keys (theme.c).
typedef enum {
  SLOT_UP_BUTTON = 0,
  SLOT_DOWN_BUTTON,
  SLOT_SWIPE_LEFT,
  SLOT_SWIPE_RIGHT,
  SLOT_SWIPE_UP,
  SLOT_SWIPE_DOWN,
  SLOT_TAP,
  SLOT_COUNT,
} ShortcutSlot;

// What a shortcut does. The values are stored one byte per slot, so they are fixed once released:
// add new actions before ACTION_COUNT, never in between.
typedef enum {
  ACTION_NONE = 0,
  ACTION_ETA_MODE,               // arrival time <-> time left
  ACTION_ETA_LABEL,              // "ETA at / ETA in" <-> "ETA / IN"
  ACTION_NEXT_ACCENT,
  ACTION_PREV_ACCENT,
  ACTION_INVERT,
  ACTION_ARROW_SIDE,
  ACTION_NEXT_GLYPH,
  ACTION_VIBRATION,              // the New instruction pattern on / off
  ACTION_EARLY_WARN_NEXT,
  ACTION_EARLY_WARN_PREV,
  ACTION_BACKLIGHT_NOW,
  ACTION_NEXT_BACKLIGHT_COLOUR,  // offered only on RGB-backlight watches
  ACTION_UNITS,
  ACTION_OPEN_SETTINGS,
  ACTION_COUNT,
} ShortcutAction;

// Shape of a vibration (REQ-WATCH-016): the New instruction buzz and the early warning (which never
// uses PULSE or SILENT). The stored values are fixed. PULSE is the phone's single short buzz on a
// turn change; it was labelled "Off" before #102, when the watch had no true Off. SILENT is that
// true Off, and the default: no buzz, whatever the phone asks for (REQ-WATCH-025, #102).
typedef enum {
  VIBE_PATTERN_PULSE = 0,   // the phone's short pulse on a maneuver change
  VIBE_PATTERN_SINGLE = 1,  // one pulse
  VIBE_PATTERN_DOUBLE = 2,  // two pulses
  VIBE_PATTERN_TRIPLE = 3,  // three pulses
  VIBE_PATTERN_LONG = 4,    // one sustained pulse
  VIBE_PATTERN_SILENT = 5,  // default, Off: no vibration for a new instruction
  VIBE_PATTERN_COUNT = 6,
} VibePatternId;

// Menu label of a pattern: "Short pulse", "Single", …, "Off" for SILENT and for an
// out-of-range value.
const char *vibe_pattern_name(VibePatternId id);

// The New instruction pattern after `id` in the menu's order, which lists Off first: Off, Short
// pulse, Single, Double, Triple, Long, then Off again.
VibePatternId vibe_pattern_next(VibePatternId id);

// The action a slot has before the user picks one: Up and the left and right swipes toggle the ETA,
// Down and Tap turn the backlight on, the up and down swipes do nothing. Touch starts On.
ShortcutAction shortcut_default(ShortcutSlot slot);

// Menu label for an action ("ETA: arrival time / time left"); "None" for an out-of-range value.
const char *shortcut_action_name(ShortcutAction action);

// The action list as the menu offers it. Without an RGB backlight, Next backlight colour is left
// out, so the list is one row shorter and the rows after it move up one.
int shortcut_offered_count(bool rgb_backlight);
ShortcutAction shortcut_action_for_row(int row, bool rgb_backlight);
// The row of `action` in that list; 0 (None) for an action the list does not offer.
int shortcut_row_for_action(ShortcutAction action, bool rgb_backlight);

// Step `current` by `step` (+1 or -1) through 0..count-1, wrapping at both ends. 0 when count < 1.
int shortcut_cycle(int current, int count, int step);

// The vibration toggle: a pattern that isn't `off` turns off; `off` turns back on to `last`, or to
// `fallback` when `last` is `off` too. With silent Off (#102) `off` is VIBE_PATTERN_SILENT and both
// `last` and `fallback` are a pattern that buzzes, Short pulse included.
int shortcut_vibe_toggle(int current, int last, int off, int fallback);

// A stored byte as an action: kept when in range, ACTION_NONE otherwise.
ShortcutAction shortcut_valid_action(int stored);

// Write the notice shown after `action` changed its setting, given the new value as the menu
// spells it: "Vibration: Double", "Early warning 50 m", "Bold glyphs", "Arrow left", or the value
// itself ("Time left", "Blue"). A value of "Off" (as vibe_pattern_name and early_warn_distance_label
// spell it) reads "Vibration off" / "Early warning off". The arrow side takes "left" or "right".
// Returns false, writing nothing, for None, Backlight now and Open settings, which show no notice,
// and for a missing buffer or value. A small buffer truncates; the text is always NUL-terminated.
bool shortcut_notice(char *buf, size_t size, ShortcutAction action, const char *value);
