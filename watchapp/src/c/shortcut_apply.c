#include "shortcut_apply.h"

#include "theme.h"

// Step the accent through the colours this watch can show: all of them on colour, the Black/White
// pair on black-and-white, through the same row mapping as the Colour menu.
static AccentId step_accent(int step) {
  const int row = shortcut_cycle(row_for_accent(settings_accent()), ACCENT_AVAILABLE_COUNT, step);
  return accent_for_row((uint16_t)row);
}

ShortcutResult shortcut_apply(ShortcutAction action, char *notice, size_t size) {
  ShortcutResult result = { .show_notice = false, .open_settings = false };
  char label[12];  // early-warning distance, as the Vibration menu shows it
  const char *value = NULL;

  switch (action) {
    case ACTION_ETA_MODE: {
      const EtaMode mode = (EtaMode)shortcut_cycle(settings_eta_mode(), ETA_MODE_COUNT, 1);
      settings_set_eta_mode(mode);
      value = (mode == ETA_MODE_DURATION) ? "Time left" : "Arrival time";
      break;
    }
    case ACTION_ETA_LABEL: {
      const EtaLabelStyle style =
          (EtaLabelStyle)shortcut_cycle(settings_eta_label_style(), ETA_LABEL_COUNT, 1);
      settings_set_eta_label_style(style);
      value = eta_label_style_name(style);
      break;
    }
    case ACTION_NEXT_ACCENT:
    case ACTION_PREV_ACCENT: {
      const AccentId accent = step_accent((action == ACTION_NEXT_ACCENT) ? 1 : -1);
      settings_set_accent(accent);
      value = accent_name(accent);
      break;
    }
    case ACTION_INVERT:
      settings_set_inverted(!settings_inverted());
      value = settings_inverted() ? "Light on dark" : "Dark on light";
      break;
    case ACTION_ARROW_SIDE:
      settings_set_arrow_left(!settings_arrow_left());
      value = settings_arrow_left() ? "left" : "right";
      break;
    case ACTION_NEXT_GLYPH: {
      const GlyphPack pack = (GlyphPack)shortcut_cycle(settings_glyph_pack(), GLYPH_PACK_COUNT, 1);
      settings_set_glyph_pack(pack);
      value = glyph_pack_name(pack);
      break;
    }
    case ACTION_VIBRATION: {
      // Only the New instruction pattern: the early warning and the phone's pulse are untouched.
      const VibePatternId pattern = (VibePatternId)shortcut_vibe_toggle(
          settings_vibe_pattern(), settings_vibe_last_pattern(), VIBE_PATTERN_OFF,
          VIBE_PATTERN_SINGLE);
      settings_set_vibe_pattern(pattern);
      settings_vibe_play();  // preview the pattern turned on (no-op when it turned off)
      value = vibe_pattern_name(pattern);
      break;
    }
    case ACTION_EARLY_WARN_NEXT:
    case ACTION_EARLY_WARN_PREV: {
      const int step = (action == ACTION_EARLY_WARN_NEXT) ? 1 : -1;
      const EarlyWarnDistance distance = (EarlyWarnDistance)shortcut_cycle(
          settings_early_warn_distance(), EARLY_WARN_COUNT, step);
      settings_set_early_warn_distance(distance);
      early_warn_distance_label(distance, settings_units() == UNITS_IMPERIAL, label, sizeof(label));
      value = label;
      break;
    }
    case ACTION_BACKLIGHT_NOW:
      light_enable_interaction();
      break;
    case ACTION_NEXT_BACKLIGHT_COLOUR: {
#ifdef PBL_RGB_BACKLIGHT
      const BacklightColorId colour = (BacklightColorId)shortcut_cycle(
          settings_backlight_color(), BACKLIGHT_COLOR_COUNT, 1);
      settings_set_backlight_color(colour);
      value = backlight_color_name(colour);
#endif
      break;
    }
    case ACTION_UNITS: {
      const UnitsId units = (UnitsId)shortcut_cycle(settings_units(), UNITS_COUNT, 1);
      settings_set_units(units);
      value = units_name(units);
      break;
    }
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
