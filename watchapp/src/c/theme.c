#include "theme.h"

#define PERSIST_KEY_ACCENT 1
#define PERSIST_KEY_UNITS 2
#define PERSIST_KEY_INVERT 3
#define PERSIST_KEY_GLYPH_PACK 4
#define PERSIST_KEY_ARROW_LEFT 5
#define PERSIST_KEY_ETA_MODE 6
#define PERSIST_KEY_BACKLIGHT 7
#define PERSIST_KEY_VIBE_PATTERN 8
#define PERSIST_KEY_VIBE_INTENSITY 9
#define PERSIST_KEY_BACKLIGHT_COLOR 10

static AccentId s_accent = ACCENT_GREEN;
static bool s_inverted = false;
static UnitsId s_units = UNITS_METRIC;
static GlyphPack s_glyph_pack = GLYPH_PACK_CLASSIC;
static bool s_arrow_left = false;
static EtaMode s_eta_mode = ETA_MODE_ARRIVAL;
static BacklightMode s_backlight = BACKLIGHT_OFF;
static BacklightColorId s_backlight_color = BACKLIGHT_COLOR_DEFAULT;
static VibePatternId s_vibe_pattern = VIBE_PATTERN_OFF;
static VibeIntensity s_vibe_intensity = VIBE_INTENSITY_MEDIUM;

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

const char *accent_name(AccentId id) {
  switch (id) {
    case ACCENT_GREEN: return "Green";
    case ACCENT_BLUE: return "Blue";
    case ACCENT_RED: return "Red";
    case ACCENT_ORANGE: return "Orange";
    case ACCENT_PURPLE: return "Purple";
    case ACCENT_TEAL: return "Teal";
    case ACCENT_PINK: return "Pink";
    case ACCENT_YELLOW: return "Yellow";
    case ACCENT_CYAN: return "Cyan";
    case ACCENT_LIME: return "Lime";
    case ACCENT_TAN: return "Tan";
    case ACCENT_NAVY: return "Navy";
    case ACCENT_AMBER: return "Amber";
    case ACCENT_GRAY: return "Grey";
    case ACCENT_WHITE: return "White";
    case ACCENT_BLACK:
    default: return "Black";
  }
}

const char *units_name(UnitsId id) {
  return (id == UNITS_IMPERIAL) ? "Miles / feet" : "Kilometres / metres";
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

const char *backlight_color_name(BacklightColorId id) {
  switch (id) {
    case BACKLIGHT_COLOR_WHITE: return "White";
    case BACKLIGHT_COLOR_WARM: return "Warm white";
    case BACKLIGHT_COLOR_RED: return "Red";
    case BACKLIGHT_COLOR_ORANGE: return "Orange";
    case BACKLIGHT_COLOR_YELLOW: return "Yellow";
    case BACKLIGHT_COLOR_GREEN: return "Green";
    case BACKLIGHT_COLOR_CYAN: return "Cyan";
    case BACKLIGHT_COLOR_BLUE: return "Blue";
    case BACKLIGHT_COLOR_PURPLE: return "Purple";
    case BACKLIGHT_COLOR_PINK: return "Pink";
    case BACKLIGHT_COLOR_DEFAULT:
    default: return "Watch default";
  }
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

const char *vibe_pattern_name(VibePatternId id) {
  switch (id) {
    case VIBE_PATTERN_SINGLE: return "Single";
    case VIBE_PATTERN_DOUBLE: return "Double";
    case VIBE_PATTERN_TRIPLE: return "Triple";
    case VIBE_PATTERN_LONG: return "Long";
    case VIBE_PATTERN_OFF:
    default: return "Off";
  }
}

const char *vibe_intensity_name(VibeIntensity id) {
  switch (id) {
    case VIBE_INTENSITY_LIGHT: return "Light";
    case VIBE_INTENSITY_STRONG: return "Strong";
    case VIBE_INTENSITY_MEDIUM:
    default: return "Medium";
  }
}

AccentId settings_accent(void) { return s_accent; }
bool settings_inverted(void) { return s_inverted; }
UnitsId settings_units(void) { return s_units; }
GlyphPack settings_glyph_pack(void) { return s_glyph_pack; }
bool settings_arrow_left(void) { return s_arrow_left; }
EtaMode settings_eta_mode(void) { return s_eta_mode; }
BacklightMode settings_backlight(void) { return s_backlight; }
BacklightColorId settings_backlight_color(void) { return s_backlight_color; }
VibePatternId settings_vibe_pattern(void) { return s_vibe_pattern; }
VibeIntensity settings_vibe_intensity(void) { return s_vibe_intensity; }

void settings_set_accent(AccentId id) {
  s_accent = (id < ACCENT_COUNT) ? id : ACCENT_GREEN;
  persist_write_int(PERSIST_KEY_ACCENT, s_accent);
}

void settings_set_inverted(bool inverted) {
  s_inverted = inverted;
  persist_write_bool(PERSIST_KEY_INVERT, s_inverted);
}

void settings_set_units(UnitsId id) {
  s_units = (id < UNITS_COUNT) ? id : UNITS_METRIC;
  persist_write_int(PERSIST_KEY_UNITS, s_units);
}

void settings_set_glyph_pack(GlyphPack id) {
  s_glyph_pack = (id < GLYPH_PACK_COUNT) ? id : GLYPH_PACK_CLASSIC;
  persist_write_int(PERSIST_KEY_GLYPH_PACK, s_glyph_pack);
}

void settings_set_arrow_left(bool arrow_left) {
  s_arrow_left = arrow_left;
  persist_write_bool(PERSIST_KEY_ARROW_LEFT, s_arrow_left);
}

void settings_set_eta_mode(EtaMode id) {
  s_eta_mode = (id < ETA_MODE_COUNT) ? id : ETA_MODE_ARRIVAL;
  persist_write_int(PERSIST_KEY_ETA_MODE, s_eta_mode);
}

void settings_set_backlight(BacklightMode id) {
  s_backlight = (id < BACKLIGHT_COUNT) ? id : BACKLIGHT_OFF;
  persist_write_int(PERSIST_KEY_BACKLIGHT, s_backlight);
}

void settings_set_backlight_color(BacklightColorId id) {
  s_backlight_color = (id < BACKLIGHT_COLOR_COUNT) ? id : BACKLIGHT_COLOR_DEFAULT;
  persist_write_int(PERSIST_KEY_BACKLIGHT_COLOR, s_backlight_color);
  settings_apply_backlight_color();  // apply live so the choice is visible while the light is on
}

void settings_set_vibe_pattern(VibePatternId id) {
  s_vibe_pattern = (id < VIBE_PATTERN_COUNT) ? id : VIBE_PATTERN_OFF;
  persist_write_int(PERSIST_KEY_VIBE_PATTERN, s_vibe_pattern);
}

void settings_set_vibe_intensity(VibeIntensity id) {
  s_vibe_intensity = (id < VIBE_INTENSITY_COUNT) ? id : VIBE_INTENSITY_MEDIUM;
  persist_write_int(PERSIST_KEY_VIBE_INTENSITY, s_vibe_intensity);
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

void settings_vibe_play(void) {
  if (s_vibe_pattern == VIBE_PATTERN_OFF) {
    return;
  }
  const uint32_t pulse = vibe_pulse_ms(s_vibe_intensity);
  const uint32_t gap = 140;
  // Alternating on/off segments starting with a pulse; TRIPLE needs the most (pulse,gap,pulse,gap,
  // pulse) = 5. Static so the buffer outlives the enqueue call.
  static uint32_t segments[5];
  uint32_t count = 0;
  switch (s_vibe_pattern) {
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

void settings_load(void) {
  // Default to an accent this watch can actually show.
  s_accent = PBL_IF_COLOR_ELSE(ACCENT_GREEN, ACCENT_BLACK);
  if (persist_exists(PERSIST_KEY_ACCENT)) {
    const int stored = persist_read_int(PERSIST_KEY_ACCENT);
    if (stored >= 0 && stored < ACCENT_COUNT) {
      s_accent = (AccentId)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_INVERT)) {
    s_inverted = persist_read_bool(PERSIST_KEY_INVERT);
  }
  if (persist_exists(PERSIST_KEY_UNITS)) {
    const int stored = persist_read_int(PERSIST_KEY_UNITS);
    if (stored >= 0 && stored < UNITS_COUNT) {
      s_units = (UnitsId)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_GLYPH_PACK)) {
    const int stored = persist_read_int(PERSIST_KEY_GLYPH_PACK);
    if (stored >= 0 && stored < GLYPH_PACK_COUNT) {
      s_glyph_pack = (GlyphPack)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_ARROW_LEFT)) {
    s_arrow_left = persist_read_bool(PERSIST_KEY_ARROW_LEFT);
  }
  if (persist_exists(PERSIST_KEY_ETA_MODE)) {
    const int stored = persist_read_int(PERSIST_KEY_ETA_MODE);
    if (stored >= 0 && stored < ETA_MODE_COUNT) {
      s_eta_mode = (EtaMode)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_BACKLIGHT)) {
    const int stored = persist_read_int(PERSIST_KEY_BACKLIGHT);
    if (stored >= 0 && stored < BACKLIGHT_COUNT) {
      s_backlight = (BacklightMode)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_BACKLIGHT_COLOR)) {
    const int stored = persist_read_int(PERSIST_KEY_BACKLIGHT_COLOR);
    if (stored >= 0 && stored < BACKLIGHT_COLOR_COUNT) {
      s_backlight_color = (BacklightColorId)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_VIBE_PATTERN)) {
    const int stored = persist_read_int(PERSIST_KEY_VIBE_PATTERN);
    if (stored >= 0 && stored < VIBE_PATTERN_COUNT) {
      s_vibe_pattern = (VibePatternId)stored;
    }
  }
  if (persist_exists(PERSIST_KEY_VIBE_INTENSITY)) {
    const int stored = persist_read_int(PERSIST_KEY_VIBE_INTENSITY);
    if (stored >= 0 && stored < VIBE_INTENSITY_COUNT) {
      s_vibe_intensity = (VibeIntensity)stored;
    }
  }
}
