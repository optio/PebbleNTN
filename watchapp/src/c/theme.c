#include "theme.h"

// Persist keys 1–15 and 19–26 hold the synced settings, one per SettingId (sync_persist_key). Keys
// 16–18 are taken by main.c: 16 the last connected phone app version (#103), 17 the latest phone
// app version on offer and 18 the version the update notice was shown for (#94). 27 is the last New
// instruction pattern that wasn't Off (REQ-WATCH-024) and 28 the settings revision
// (settings_sync_glue.c, REQ-WATCH-025). The next key is 29.
#define PERSIST_KEY_VIBE_LAST 27

// settings_sync.c keeps a value range per setting; these are the enums it stands for.
_Static_assert(ACCENT_COUNT == 16 && UNITS_COUNT == 2 && GLYPH_PACK_COUNT == 3 &&
                   ETA_MODE_COUNT == 2 && BACKLIGHT_COUNT == 4 && VIBE_PATTERN_COUNT == 6 &&
                   VIBE_INTENSITY_COUNT == 3 && BACKLIGHT_COLOR_COUNT == 11 &&
                   ETA_LABEL_COUNT == 2 && EARLY_WARN_COUNT == 6 && EARLY_WARN_REPEAT_COUNT == 2,
               "settings_sync.c kRules matches the setting enums");

// Every synced setting, as its enum index (bools as 0/1); settings_load fills in the defaults.
static uint8_t s_values[SET_COUNT];
// The last New instruction pattern that buzzed (0–4); Short pulse until another is chosen.
static VibePatternId s_vibe_last = VIBE_PATTERN_PULSE;
static void (*s_on_edit)(void);

#define s_accent ((AccentId)s_values[SET_ACCENT])
#define s_inverted (s_values[SET_INVERT] != 0)
#define s_backlight ((BacklightMode)s_values[SET_BACKLIGHT])
#define s_backlight_color ((BacklightColorId)s_values[SET_BACKLIGHT_COLOR])

// On a black-and-white watch the accent list is just {Black, White}; the row indices are offset so
// the menu never shows a colour the display cannot render.
AccentId accent_for_row(uint16_t row) {
#ifdef PBL_COLOR
  return (row < ACCENT_COUNT) ? (AccentId)row : ACCENT_GREEN;
#else
  return (row == 0) ? ACCENT_BLACK : ACCENT_WHITE;
#endif
}

uint16_t row_for_accent(AccentId id) {
#ifdef PBL_COLOR
  return (uint16_t)id;
#else
  return (id == ACCENT_WHITE) ? 1 : 0;
#endif
}

static GColor accent_color(AccentId id) {
#ifdef PBL_COLOR
  switch (id) {
    case ACCENT_GREEN: return GColorIslamicGreen;
    case ACCENT_BLUE: return GColorCobaltBlue;
    case ACCENT_RED: return GColorDarkCandyAppleRed;
    case ACCENT_ORANGE: return GColorOrange;
    case ACCENT_PURPLE: return GColorImperialPurple;
    case ACCENT_TEAL: return GColorTiffanyBlue;
    case ACCENT_PINK: return GColorShockingPink;
    case ACCENT_YELLOW: return GColorYellow;
    case ACCENT_CYAN: return GColorCyan;
    case ACCENT_LIME: return GColorGreen;
    case ACCENT_TAN: return GColorWindsorTan;
    case ACCENT_NAVY: return GColorOxfordBlue;
    case ACCENT_AMBER: return GColorChromeYellow;
    case ACCENT_GRAY: return GColorLightGray;
    case ACCENT_WHITE: return GColorWhite;
    case ACCENT_BLACK:
    default: return GColorBlack;
  }
#else
  return (id == ACCENT_WHITE) ? GColorWhite : GColorBlack;
#endif
}

// Whatever the accent, the panel text and glyph must stay readable on top of it. This is a
// perceived-luminance test rather than gcolor_legible_over(), which picks black over mid-brightness
// colours like Islamic green — legible in principle, but the wrong call for a glanceable arrow.
static GColor legible_over(GColor background) {
#ifdef PBL_COLOR
  // Channels are 2-bit (0..3); weight them the way the eye does and scale to 0..255.
  const uint16_t r = background.r * 85;
  const uint16_t g = background.g * 85;
  const uint16_t b = background.b * 85;
  const uint16_t luminance = (r * 77 + g * 151 + b * 28) / 256;
  return (luminance >= 160) ? GColorBlack : GColorWhite;
#else
  return gcolor_equal(background, GColorWhite) ? GColorBlack : GColorWhite;
#endif
}

GColor accent_swatch(AccentId id) {
  return accent_color(id);
}

GColor accent_swatch_text(AccentId id) {
  return legible_over(accent_color(id));
}

// The status strip is a shade of the accent, so the panel reads as one block. On a colour watch
// that means one step darker per channel; on black-and-white there is nothing to darken.
static GColor shade_of(GColor color) {
#ifdef PBL_COLOR
  GColor8 shaded = color;
  shaded.r = (shaded.r > 0) ? shaded.r - 1 : 0;
  shaded.g = (shaded.g > 0) ? shaded.g - 1 : 0;
  shaded.b = (shaded.b > 0) ? shaded.b - 1 : 0;
  return shaded;
#else
  return color;
#endif
}

Theme theme_current(void) {
  const GColor accent = accent_color(s_accent);
  Theme t;
  t.panel_bg = accent;
  t.panel_fg = legible_over(accent);
  t.strip_bg = shade_of(accent);
  t.strip_fg = legible_over(t.strip_bg);
  // Invert flips the road area: black-on-white becomes white-on-black.
  t.road_bg = s_inverted ? GColorBlack : GColorWhite;
  t.road_fg = s_inverted ? GColorWhite : GColorBlack;
  return t;
}

// Name tables rather than switches: a table is about half the code, and code is heap on aplite.
static const char *const kAccentNames[ACCENT_COUNT] = {
  "Green", "Blue", "Red", "Orange", "Purple", "Teal", "Pink", "Yellow",
  "Cyan", "Lime", "Tan", "Navy", "Amber", "Grey", "Black", "White",
};

const char *accent_name(AccentId id) {
  return kAccentNames[(id < ACCENT_COUNT) ? id : ACCENT_BLACK];
}

const char *units_name(UnitsId id) {
  return (id == UNITS_IMPERIAL) ? "Miles / feet" : "Kilometres / metres";
}

// The setting shows the style itself, as the two tags it produces.
const char *eta_label_style_name(EtaLabelStyle id) {
  return (id == ETA_LABEL_COMPACT) ? "ETA / IN" : "ETA at / ETA in";
}

const char *eta_mode_name(EtaMode id) {
  return (id == ETA_MODE_DURATION) ? "Time to arrival" : "Arrival time";
}

const char *glyph_pack_name(GlyphPack id) {
  switch (id) {
    case GLYPH_PACK_BOLD: return "Bold";
    case GLYPH_PACK_OUTLINE: return "Outline";
    case GLYPH_PACK_CLASSIC:
    default: return "Classic";
  }
}

// The level names say what the level does, not just how strong it is: "Low" alone tells the user
// nothing, whereas "Low - 3s on update" is the whole setting in one line.
const char *backlight_mode_name(BacklightMode id) {
  switch (id) {
    case BACKLIGHT_LOW: return "Low - 3s on update";
    case BACKLIGHT_MEDIUM: return "Medium - 10s on update";
    case BACKLIGHT_HIGH: return "High - until app closes";
    case BACKLIGHT_OFF:
    default: return "Watch default";
  }
}

static const char *const kBacklightColorNames[BACKLIGHT_COLOR_COUNT] = {
  "Watch default", "White", "Warm white", "Red", "Orange", "Yellow",
  "Green", "Cyan", "Blue", "Purple", "Pink",
};

const char *backlight_color_name(BacklightColorId id) {
  return kBacklightColorNames[(id < BACKLIGHT_COLOR_COUNT) ? id : BACKLIGHT_COLOR_DEFAULT];
}

// The tint helpers exist only where the hardware does. On other watches the SDK's light_set_color*
// entry points are argument-discarding no-op macros, so an unguarded call would leave this table
// unreferenced and the call site evaluating to a bare `0` — two compiler warnings for code that
// cannot do anything.
#ifdef PBL_RGB_BACKLIGHT

// Packed 0x00RRGGBB tints. light_set_color_rgb888() is used rather than light_set_color() because
// GColor carries only 2 bits per channel, which is too coarse for a warm white to read as anything
// other than yellow on the LED.
static uint32_t backlight_color_rgb(BacklightColorId id) {
  switch (id) {
    case BACKLIGHT_COLOR_WHITE: return 0xFFFFFF;
    case BACKLIGHT_COLOR_WARM: return 0xFFB86B;
    case BACKLIGHT_COLOR_RED: return 0xFF0000;
    case BACKLIGHT_COLOR_ORANGE: return 0xFF7000;
    case BACKLIGHT_COLOR_YELLOW: return 0xFFFF00;
    case BACKLIGHT_COLOR_GREEN: return 0x00FF00;
    case BACKLIGHT_COLOR_CYAN: return 0x00FFFF;
    case BACKLIGHT_COLOR_BLUE: return 0x0060FF;
    case BACKLIGHT_COLOR_PURPLE: return 0x9000FF;
    case BACKLIGHT_COLOR_PINK: return 0xFF00A0;
    case BACKLIGHT_COLOR_DEFAULT:
    default: return 0xFFFFFF;  // unused: DEFAULT is handled by light_set_system_color()
  }
}

void settings_apply_backlight_color(void) {
  if (s_backlight_color == BACKLIGHT_COLOR_DEFAULT) {
    light_set_system_color();
  } else {
    light_set_color_rgb888(backlight_color_rgb(s_backlight_color));
  }
}

#else  // !PBL_RGB_BACKLIGHT

void settings_apply_backlight_color(void) {}

#endif

const char *vibe_intensity_name(VibeIntensity id) {
  switch (id) {
    case VIBE_INTENSITY_LIGHT: return "Light";
    case VIBE_INTENSITY_STRONG: return "Strong";
    case VIBE_INTENSITY_MEDIUM:
    default: return "Medium";
  }
}

int settings_get(SettingId id) {
  return ((unsigned)id < SET_COUNT) ? s_values[id] : 0;
}

VibePatternId settings_vibe_last_pattern(void) {
  return s_vibe_last;
}

void settings_set_edit_handler(void (*on_edit)(void)) {
  s_on_edit = on_edit;
}

// Remember `id` as the pattern the Vibration shortcut turns back on. Written only when it changes:
// the menu row writes the pattern on every step through the list.
static void record_vibe_last(VibePatternId id) {
  if (id == s_vibe_last && persist_exists(PERSIST_KEY_VIBE_LAST)) {
    return;
  }
  s_vibe_last = id;
  persist_write_int(PERSIST_KEY_VIBE_LAST, s_vibe_last);
}

bool settings_store(SettingId id, int value) {
  if ((unsigned)id >= SET_COUNT) {
    return false;
  }
  if (!sync_valid(id, value, SYNC_CAPS_ALL)) {
    value = sync_default(id, sync_build_caps());
  }
  const uint8_t previous = s_values[id];
  if (value == previous) {
    return false;  // unchanged: no flash write
  }
  s_values[id] = (uint8_t)value;
  const uint32_t key = sync_persist_key(id);
  if (sync_is_bool(id)) {
    persist_write_bool(key, value != 0);
  } else {
    persist_write_int(key, value);
  }
  if (id == SET_BACKLIGHT_COLOR) {
    settings_apply_backlight_color();  // apply live so the choice is visible while the light is on
  } else if (id == SET_VIBE_PATTERN) {
    // Turning it off keeps the pattern it had, which also covers a pattern stored before key 27.
    if (value != VIBE_PATTERN_SILENT) {
      record_vibe_last((VibePatternId)value);
    } else {
      record_vibe_last((VibePatternId)previous);
    }
  }
  return true;
}

void settings_set(SettingId id, int value) {
  if (settings_store(id, value) && s_on_edit != NULL) {
    s_on_edit();
  }
}

uint32_t settings_backlight_hold_ms(void) {
  switch (s_backlight) {
    case BACKLIGHT_LOW: return 3000;     // brief assist around each update
    case BACKLIGHT_MEDIUM: return 10000; // longer hold
    case BACKLIGHT_HIGH: return 0;       // 0 => steady-on, no timeout
    case BACKLIGHT_OFF:
    default: return 0;                   // not forced at all (caller checks the mode)
  }
}

// A pulse length (ms) per intensity. The motor's amplitude is not app-controllable, so a longer
// buzz is what reads as "stronger" — this is the honest, genuinely distinct proxy per level.
static uint32_t vibe_pulse_ms(VibeIntensity intensity) {
  switch (intensity) {
    case VIBE_INTENSITY_LIGHT: return 100;
    case VIBE_INTENSITY_STRONG: return 400;
    case VIBE_INTENSITY_MEDIUM:
    default: return 220;
  }
}

static void vibe_play(VibePatternId id, VibeIntensity intensity) {
  if (id == VIBE_PATTERN_SILENT) {
    return;
  }
  if (id == VIBE_PATTERN_PULSE) {
    vibes_short_pulse();  // the phone's own buzz, as the navigation path plays it
    return;
  }
  const uint32_t pulse = vibe_pulse_ms(intensity);
  const uint32_t gap = 140;
  // Alternating on/off segments starting with a pulse; TRIPLE needs the most (pulse,gap,pulse,gap,
  // pulse) = 5. Static so the buffer outlives the enqueue call.
  static uint32_t segments[5];
  uint32_t count = 0;
  switch (id) {
    case VIBE_PATTERN_DOUBLE:
      segments[0] = pulse; segments[1] = gap; segments[2] = pulse;
      count = 3;
      break;
    case VIBE_PATTERN_TRIPLE:
      segments[0] = pulse; segments[1] = gap; segments[2] = pulse;
      segments[3] = gap;   segments[4] = pulse;
      count = 5;
      break;
    case VIBE_PATTERN_LONG:
      segments[0] = pulse * 2;  // one sustained buzz
      count = 1;
      break;
    case VIBE_PATTERN_SINGLE:
    default:
      segments[0] = pulse;
      count = 1;
      break;
  }
  VibePattern pattern = { .durations = segments, .num_segments = count };
  vibes_enqueue_custom_pattern(pattern);
}

void settings_vibe_play(void) {
  vibe_play(settings_vibe_pattern(), settings_vibe_intensity());
}

void settings_early_warn_play(void) {
  if (settings_early_warn_distance() == EARLY_WARN_OFF) {
    return;
  }
  vibe_play(settings_early_warn_pattern(), settings_early_warn_intensity());
}

// A setting with nothing stored, or a stored value out of range, keeps its default. The range is
// this watch's whole range, as before the table: a stored colour accent stays on a black-and-white
// watch, where it draws as Black.
void settings_load(void) {
  const uint32_t caps = sync_build_caps();
  for (int i = 0; i < SET_COUNT; i++) {
    const SettingId id = (SettingId)i;
    const uint32_t key = sync_persist_key(id);
    int value = sync_default(id, caps);
    if (persist_exists(key)) {
      const int stored = sync_is_bool(id) ? persist_read_bool(key) : persist_read_int(key);
      if (sync_valid(id, stored, SYNC_CAPS_ALL)) {
        value = stored;
      }
    }
    s_values[id] = (uint8_t)value;
  }
  if (persist_exists(PERSIST_KEY_VIBE_LAST)) {
    const int stored = persist_read_int(PERSIST_KEY_VIBE_LAST);
    if (stored >= VIBE_PATTERN_PULSE && stored < VIBE_PATTERN_SILENT) {
      s_vibe_last = (VibePatternId)stored;
    }
  }
}
