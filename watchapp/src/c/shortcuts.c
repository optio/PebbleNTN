#include "shortcuts.h"

#include <stdio.h>
#include <string.h>

// Name tables rather than switches: a table is about half the code, and code is heap on aplite.
static const char *const kPatternNames[VIBE_PATTERN_COUNT] = {
  "Short pulse", "Single", "Double", "Triple", "Long", "Off",
};

const char *vibe_pattern_name(VibePatternId id) {
  return kPatternNames[((unsigned)id < VIBE_PATTERN_COUNT) ? id : VIBE_PATTERN_SILENT];
}

VibePatternId vibe_pattern_next(VibePatternId id) {
  switch (id) {
    case VIBE_PATTERN_SILENT: return VIBE_PATTERN_PULSE;
    case VIBE_PATTERN_LONG: return VIBE_PATTERN_SILENT;
    default: return ((unsigned)id < VIBE_PATTERN_LONG) ? (VibePatternId)(id + 1) : VIBE_PATTERN_SILENT;
  }
}

ShortcutAction shortcut_default(ShortcutSlot slot) {
  switch (slot) {
    case SLOT_UP_BUTTON:
    case SLOT_SWIPE_LEFT:
    case SLOT_SWIPE_RIGHT:
      return ACTION_ETA_MODE;
    case SLOT_DOWN_BUTTON:
    case SLOT_TAP:
      return ACTION_BACKLIGHT_NOW;
    default:
      return ACTION_NONE;
  }
}

static const char *const kActionNames[ACTION_COUNT] = {
  [ACTION_NONE] = "None",
  [ACTION_ETA_MODE] = "ETA: arrival time / time left",
  [ACTION_ETA_LABEL] = "ETA label style",
  [ACTION_NEXT_ACCENT] = "Next colour",
  [ACTION_PREV_ACCENT] = "Previous colour",
  [ACTION_INVERT] = "Invert colours",
  [ACTION_ARROW_SIDE] = "Swap arrow side",
  [ACTION_NEXT_GLYPH] = "Next glyph pack",
  [ACTION_VIBRATION] = "Vibration on / off",
  [ACTION_EARLY_WARN_NEXT] = "Early warning: next distance",
  [ACTION_EARLY_WARN_PREV] = "Early warning: previous distance",
  [ACTION_BACKLIGHT_NOW] = "Backlight now",
  [ACTION_NEXT_BACKLIGHT_COLOUR] = "Next backlight colour",
  [ACTION_UNITS] = "Units",
  [ACTION_OPEN_SETTINGS] = "Open settings",
};

const char *shortcut_action_name(ShortcutAction action) {
  return kActionNames[((unsigned)action < ACTION_COUNT) ? action : ACTION_NONE];
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
