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

// How the status strip renders the arrival estimate (REQ-WATCH-014). Purely a render choice: the
// phone always sends both the arrival-time string and the arrival epoch; this only picks which the
// watch shows. ARRIVAL is the default and matches the original behaviour (the clock time of arrival,
// e.g. "17:45"); DURATION shows the remaining time until arrival (e.g. "0:25"), recomputed on the
// watch each minute.
typedef enum {
  ETA_MODE_ARRIVAL = 0,
  ETA_MODE_DURATION = 1,
  ETA_MODE_COUNT = 2,
} EtaMode;

// Built-in maneuver glyph packs (REQ-WATCH-012). Purely a render choice: the phone always sends the
// same maneuver code; the pack only selects which bundled bitmap the watch draws for it.
typedef enum {
  GLYPH_PACK_CLASSIC = 0,
  GLYPH_PACK_BOLD = 1,
  GLYPH_PACK_OUTLINE = 2,
  GLYPH_PACK_COUNT = 3,
} GlyphPack;

// How aggressively the watchapp keeps the backlight lit while it is the foreground app
// (REQ-WATCH-015). The Pebble SDK exposes no backlight *brightness* control to apps (only on/off via
// light_enable), so "intensity" is expressed as how much of the time the light is engaged, which is
// the honest proxy the hardware allows: OFF leaves the watch's own automatic behaviour untouched;
// the three levels turn the backlight on around navigation activity, from a brief assist (LOW) up to
// steady-on for the whole session (HIGH). OFF is the default — no additional backlight.
typedef enum {
  BACKLIGHT_OFF = 0,     // default: watch's own light settings, no forcing
  BACKLIGHT_LOW = 1,     // brief boost on each update
  BACKLIGHT_MEDIUM = 2,  // longer hold on each update
  BACKLIGHT_HIGH = 3,    // steady-on while the app is active
  BACKLIGHT_COUNT = 4,
} BacklightMode;

// Shape of the vibration played when a new navigation instruction is parsed (REQ-WATCH-016). OFF is
// the default (no watch-initiated vibration; the phone's maneuver-change request still applies). The
// other patterns differ in how many pulses are played.
typedef enum {
  VIBE_PATTERN_OFF = 0,     // default: no watch-initiated vibration
  VIBE_PATTERN_SINGLE = 1,  // one pulse
  VIBE_PATTERN_DOUBLE = 2,  // two pulses
  VIBE_PATTERN_TRIPLE = 3,  // three pulses
  VIBE_PATTERN_LONG = 4,    // one sustained pulse
  VIBE_PATTERN_COUNT = 5,
} VibePatternId;

// Vibration intensity (REQ-WATCH-016). The Pebble SDK exposes no vibration *amplitude* control, so
// intensity is expressed as pulse length: a longer buzz reads as stronger. This is the honest proxy
// the hardware allows and is genuinely different per level.
typedef enum {
  VIBE_INTENSITY_LIGHT = 0,
  VIBE_INTENSITY_MEDIUM = 1,
  VIBE_INTENSITY_STRONG = 2,
  VIBE_INTENSITY_COUNT = 3,
} VibeIntensity;

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
const char *eta_mode_name(EtaMode id);
const char *backlight_mode_name(BacklightMode id);
const char *vibe_pattern_name(VibePatternId id);
const char *vibe_intensity_name(VibeIntensity id);

// Persisted settings. Load once at startup; each setter saves.
AccentId settings_accent(void);
bool settings_inverted(void);
UnitsId settings_units(void);
GlyphPack settings_glyph_pack(void);
EtaMode settings_eta_mode(void);
BacklightMode settings_backlight(void);
VibePatternId settings_vibe_pattern(void);
VibeIntensity settings_vibe_intensity(void);
// Whether the maneuver arrow sits in the top-LEFT corner (with the distance on the right); the
// default is the arrow on the right and the distance on the left.
bool settings_arrow_left(void);
void settings_set_accent(AccentId id);
void settings_set_inverted(bool inverted);
void settings_set_units(UnitsId id);
void settings_set_glyph_pack(GlyphPack id);
void settings_set_arrow_left(bool arrow_left);
void settings_set_eta_mode(EtaMode id);
void settings_set_backlight(BacklightMode id);
void settings_set_vibe_pattern(VibePatternId id);
void settings_set_vibe_intensity(VibeIntensity id);
void settings_load(void);

// Play the vibration selected for a newly parsed instruction, built from the current pattern and
// intensity settings (REQ-WATCH-016). No-op when the pattern is OFF. Shared by the navigation path
// (fired on new info) and the settings menu (fired as a preview when the user changes either row).
void settings_vibe_play(void);

// How long (ms) the backlight is held lit after activity for the current backlight mode; 0 means
// keep it on with no timeout (steady-on) and BACKLIGHT_OFF also returns 0 (no forcing).
uint32_t settings_backlight_hold_ms(void);
