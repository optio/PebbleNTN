// Watch-side appearance settings: accent colour, invert, and distance units (REQ-WATCH-011).
//
// These are *watch* settings, not phone settings: they change only how the normalized state is
// rendered, never what the phone sends. They persist across app launches.
//
// The app runs on 64-colour watches (Pebble Time 2, Time, …) and on black-and-white ones (Pebble 2
// Duo, …). One model covers both: an accent colour for the maneuver panel plus an invert flag for
// the road area. On a black-and-white watch the colour list collapses to Black and White, so
// "black-on-white", "white-on-black" and their inverses are all still reachable.

#pragma once

#include <pebble.h>

// Accent colours offered for the maneuver panel. On black-and-white watches only the last two
// (BLACK, WHITE) are offered; every other entry falls back to one of them.
typedef enum {
  ACCENT_GREEN = 0,
  ACCENT_BLUE,
  ACCENT_RED,
  ACCENT_ORANGE,
  ACCENT_PURPLE,
  ACCENT_TEAL,
  ACCENT_PINK,
  ACCENT_YELLOW,
  ACCENT_CYAN,
  ACCENT_LIME,
  ACCENT_TAN,
  ACCENT_NAVY,
  ACCENT_AMBER,
  ACCENT_GRAY,
  ACCENT_BLACK,
  ACCENT_WHITE,
  ACCENT_COUNT,
} AccentId;

// Black-and-white watches offer only the final two accents.
#define ACCENT_BW_FIRST ACCENT_BLACK
#define ACCENT_COUNT_BW 2
#define ACCENT_AVAILABLE_COUNT PBL_IF_COLOR_ELSE(ACCENT_COUNT, ACCENT_COUNT_BW)

// Map a menu row to an accent: identity on colour, offset to {BLACK, WHITE} on black-and-white.
AccentId accent_for_row(uint16_t row);
uint16_t row_for_accent(AccentId id);

typedef enum {
  UNITS_METRIC = 0,
  UNITS_IMPERIAL = 1,
  UNITS_COUNT = 2,
} UnitsId;

// Built-in maneuver glyph packs (REQ-WATCH-012). Purely a render choice: the phone always sends the
// same maneuver code; the pack only selects which bundled bitmap the watch draws for it.
typedef enum {
  GLYPH_PACK_CLASSIC = 0,
  GLYPH_PACK_BOLD = 1,
  GLYPH_PACK_OUTLINE = 2,
  GLYPH_PACK_COUNT = 3,
} GlyphPack;

typedef struct {
  GColor panel_bg;   // top panel: distance + maneuver
  GColor panel_fg;   // distance text and maneuver glyph (always legible over panel_bg)
  GColor strip_bg;   // thin status strip: ETA + stale marker
  GColor strip_fg;
  GColor road_bg;    // bottom area: road name
  GColor road_fg;
} Theme;

// Colours for the current settings, reduced to what this platform can display.
Theme theme_current(void);

const char *accent_name(AccentId id);
const char *units_name(UnitsId id);
const char *glyph_pack_name(GlyphPack id);

// Persisted settings. Load once at startup; each setter saves.
AccentId settings_accent(void);
bool settings_inverted(void);
UnitsId settings_units(void);
GlyphPack settings_glyph_pack(void);
// Whether the maneuver arrow sits in the top-LEFT corner (with the distance on the right); the
// default is the arrow on the right and the distance on the left.
bool settings_arrow_left(void);
void settings_set_accent(AccentId id);
void settings_set_inverted(bool inverted);
void settings_set_units(UnitsId id);
void settings_set_glyph_pack(GlyphPack id);
void settings_set_arrow_left(bool arrow_left);
void settings_load(void);
