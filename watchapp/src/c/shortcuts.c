#include "shortcuts.h"

#include <stdio.h>
#include <string.h>

ShortcutAction shortcut_default(ShortcutSlot slot) {
  switch (slot) {
    case SLOT_SWIPE_LEFT:
    case SLOT_SWIPE_RIGHT:
      return ACTION_ETA_MODE;
    default:
      return ACTION_NONE;
  }
}

const char *shortcut_action_name(ShortcutAction action) {
  switch (action) {
    case ACTION_ETA_MODE: return "ETA: arrival time / time left";
    case ACTION_ETA_LABEL: return "ETA label style";
    case ACTION_NEXT_ACCENT: return "Next colour";
    case ACTION_PREV_ACCENT: return "Previous colour";
    case ACTION_INVERT: return "Invert colours";
    case ACTION_ARROW_SIDE: return "Swap arrow side";
    case ACTION_NEXT_GLYPH: return "Next glyph pack";
    case ACTION_VIBRATION: return "Vibration on / off";
    case ACTION_EARLY_WARN_NEXT: return "Early warning: next distance";
    case ACTION_EARLY_WARN_PREV: return "Early warning: previous distance";
    case ACTION_BACKLIGHT_NOW: return "Backlight now";
    case ACTION_NEXT_BACKLIGHT_COLOUR: return "Next backlight colour";
    case ACTION_UNITS: return "Units";
    case ACTION_OPEN_SETTINGS: return "Open settings";
    case ACTION_NONE:
    default: return "None";
  }
}

int shortcut_offered_count(bool rgb_backlight) {
  return rgb_backlight ? ACTION_COUNT : ACTION_COUNT - 1;
}

// The list is the actions in their stored order; without an RGB backlight the rows from Next
// backlight colour on move up one.
ShortcutAction shortcut_action_for_row(int row, bool rgb_backlight) {
  if (row < 0 || row >= shortcut_offered_count(rgb_backlight)) {
    return ACTION_NONE;
  }
  if (!rgb_backlight && row >= ACTION_NEXT_BACKLIGHT_COLOUR) {
    row++;
  }
  return (ShortcutAction)row;
}

int shortcut_row_for_action(ShortcutAction action, bool rgb_backlight) {
  if (action <= ACTION_NONE || action >= ACTION_COUNT) {
    return 0;
  }
  if (rgb_backlight) {
    return (int)action;
  }
  if (action == ACTION_NEXT_BACKLIGHT_COLOUR) {
    return 0;
  }
  return (action > ACTION_NEXT_BACKLIGHT_COLOUR) ? (int)action - 1 : (int)action;
}

int shortcut_cycle(int current, int count, int step) {
  if (count < 1) {
    return 0;
  }
  // Normalise both so the result lands in range whatever the inputs.
  const int base = ((current % count) + count) % count;
  const int delta = ((step % count) + count) % count;
  return (base + delta) % count;
}

int shortcut_vibe_toggle(int current, int last, int off, int fallback) {
  if (current != off) {
    return off;
  }
  return (last != off) ? last : fallback;
}

ShortcutAction shortcut_valid_action(int stored) {
  return (stored >= 0 && stored < ACTION_COUNT) ? (ShortcutAction)stored : ACTION_NONE;
}

bool shortcut_notice(char *buf, size_t size, ShortcutAction action, const char *value) {
  if (buf == NULL || size == 0 || value == NULL) {
    return false;
  }
  const bool off = strcmp(value, "Off") == 0;
  switch (action) {
    case ACTION_NONE:
    case ACTION_BACKLIGHT_NOW:
    case ACTION_OPEN_SETTINGS:
      return false;
    case ACTION_VIBRATION:
      if (off) {
        snprintf(buf, size, "%s", "Vibration off");
      } else {
        snprintf(buf, size, "Vibration: %s", value);
      }
      return true;
    case ACTION_EARLY_WARN_NEXT:
    case ACTION_EARLY_WARN_PREV:
      if (off) {
        snprintf(buf, size, "%s", "Early warning off");
      } else {
        snprintf(buf, size, "Early warning %s", value);
      }
      return true;
    case ACTION_NEXT_GLYPH:
      snprintf(buf, size, "%s glyphs", value);
      return true;
    case ACTION_ARROW_SIDE:
      snprintf(buf, size, "Arrow %s", value);
      return true;
    default:
      if (action >= ACTION_COUNT) {
        return false;
      }
      snprintf(buf, size, "%s", value);
      return true;
  }
}
