#include "theme.h"

#define PERSIST_KEY_ACCENT 1
#define PERSIST_KEY_UNITS 2
#define PERSIST_KEY_INVERT 3
#define PERSIST_KEY_GLYPH_PACK 4
#define PERSIST_KEY_ARROW_LEFT 5

static AccentId s_accent = ACCENT_GREEN;
static bool s_inverted = false;
static UnitsId s_units = UNITS_METRIC;
static GlyphPack s_glyph_pack = GLYPH_PACK_CLASSIC;
static bool s_arrow_left = false;

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

const char *glyph_pack_name(GlyphPack id) {
  switch (id) {
    case GLYPH_PACK_BOLD: return "Bold";
    case GLYPH_PACK_OUTLINE: return "Outline";
    case GLYPH_PACK_CLASSIC:
    default: return "Classic";
  }
}

AccentId settings_accent(void) { return s_accent; }
bool settings_inverted(void) { return s_inverted; }
UnitsId settings_units(void) { return s_units; }
GlyphPack settings_glyph_pack(void) { return s_glyph_pack; }
bool settings_arrow_left(void) { return s_arrow_left; }

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
}
