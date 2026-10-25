#include "shortcut_apply.h"

#include "theme.h"

// Step the accent through the colours this watch can show: all of them on colour, the Black/White
// pair on black-and-white, through the same row mapping as the Colour menu.
static AccentId step_accent(int step) {
  const int row = shortcut_cycle(row_for_accent(settings_accent()), ACCENT_AVAILABLE_COUNT, step);
  return accent_for_row((uint16_t)row);
}

// Step a setting by `step` through all its values, wrapping, save it as a watch edit, and return
// the new value. One helper for every cycling action is less code, which is heap on aplite (#102).
static int step_setting(SettingId id, int step) {
  const int value = shortcut_cycle(settings_get(id), sync_value_count(id), step);
  settings_set(id, value);
  return value;
}

ShortcutResult shortcut_apply(ShortcutAction action, char *notice, size_t size) {
  ShortcutResult result = { .show_notice = false, .open_settings = false };
  char label[12];  // early-warning distance, as the Vibration menu shows it
  const char *value = NULL;

  switch (action) {
    case ACTION_ETA_MODE:
      value = (step_setting(SET_ETA_MODE, 1) == ETA_MODE_DURATION) ? "Time left" : "Arrival time";
      break;
    case ACTION_ETA_LABEL:
      value = eta_label_style_name((EtaLabelStyle)step_setting(SET_ETA_LABEL, 1));
      break;
    case ACTION_NEXT_ACCENT:
    case ACTION_PREV_ACCENT: {
      const AccentId accent = step_accent((action == ACTION_NEXT_ACCENT) ? 1 : -1);
      settings_set_accent(accent);
      value = accent_name(accent);
      break;
    }
    case ACTION_INVERT:
      value = step_setting(SET_INVERT, 1) ? "Light on dark" : "Dark on light";
      break;
    case ACTION_ARROW_SIDE:
      value = step_setting(SET_ARROW_LEFT, 1) ? "left" : "right";
      break;
    case ACTION_NEXT_GLYPH:
      value = glyph_pack_name((GlyphPack)step_setting(SET_GLYPH_PACK, 1));
      break;
    case ACTION_VIBRATION: {
      // Only the New instruction pattern: the early warning is untouched. Off is silent; turning it
      // back on restores the last pattern, Short pulse included (REQ-WATCH-025, #102).
      const VibePatternId pattern = (VibePatternId)shortcut_vibe_toggle(
          settings_vibe_pattern(), settings_vibe_last_pattern(), VIBE_PATTERN_SILENT,
          VIBE_PATTERN_PULSE);
      settings_set_vibe_pattern(pattern);
      settings_vibe_play();  // preview the pattern turned on (none when it turned off)
      value = vibe_pattern_name(pattern);
      break;
    }
    case ACTION_EARLY_WARN_NEXT:
    case ACTION_EARLY_WARN_PREV: {
      const int distance = step_setting(SET_WARN_DISTANCE, (action == ACTION_EARLY_WARN_NEXT) ? 1 : -1);
      early_warn_distance_label((EarlyWarnDistance)distance, settings_units() == UNITS_IMPERIAL,
                                label, sizeof(label));
      value = label;
      break;
    }
    case ACTION_BACKLIGHT_NOW:
      light_enable_interaction();
      break;
    case ACTION_NEXT_BACKLIGHT_COLOUR:
#ifdef PBL_RGB_BACKLIGHT
      value = backlight_color_name((BacklightColorId)step_setting(SET_BACKLIGHT_COLOR, 1));
#endif
      break;
    case ACTION_UNITS:
      value = units_name((UnitsId)step_setting(SET_UNITS, 1));
      break;
    case ACTION_OPEN_SETTINGS:
      result.open_settings = true;
      break;
    case ACTION_NONE:
    default:
      break;
  }

  // No value means nothing changed that a notice could name (None, Backlight now, Open settings,
  // or the backlight colour on a watch without an RGB backlight).
  result.show_notice = (value != NULL) && shortcut_notice(notice, size, action, value);
  return result;
}
