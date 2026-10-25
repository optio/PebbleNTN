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

#include "early_warning.h"
#include "settings_sync.h"
#include "shortcuts.h"

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

// How the tag before the arrival readout is written (REQ-WATCH-014, #13). LABELLED, the default,
// reads as a phrase ("ETA at 17:45", "ETA in 0:25"); COMPACT keeps the short tags ("ETA 17:45",
// "IN 0:25"). If the labelled tag does not fit beside the clock, the strip uses the compact one.
typedef enum {
  ETA_LABEL_LABELLED = 0,
  ETA_LABEL_COMPACT = 1,
  ETA_LABEL_COUNT = 2,
} EtaLabelStyle;

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
  BACKLIGHT_LOW = 1,     // ~3 s after each update
  BACKLIGHT_MEDIUM = 2,  // ~10 s after each update
  BACKLIGHT_HIGH = 3,    // steady-on until the app closes
  BACKLIGHT_COUNT = 4,
} BacklightMode;

// Backlight tint, offered only on RGB-backlight hardware (PBL_RGB_BACKLIGHT — Pebble Time 2 /
// emery). DEFAULT keeps the user's own backlight colour; every other entry tints the LED while the
// watchapp is foregrounded. The system resets the tint automatically on app exit or when a
// notification preempts the app, so no teardown is needed. On non-RGB hardware the tint calls are
// SDK no-ops and the menu does not offer the setting at all.
typedef enum {
  BACKLIGHT_COLOR_DEFAULT = 0,  // default: the user's own backlight colour
  BACKLIGHT_COLOR_WHITE = 1,
  BACKLIGHT_COLOR_WARM = 2,     // warm white — easier on night vision than pure white
  BACKLIGHT_COLOR_RED = 3,
  BACKLIGHT_COLOR_ORANGE = 4,
  BACKLIGHT_COLOR_YELLOW = 5,
  BACKLIGHT_COLOR_GREEN = 6,
  BACKLIGHT_COLOR_CYAN = 7,
  BACKLIGHT_COLOR_BLUE = 8,
  BACKLIGHT_COLOR_PURPLE = 9,
  BACKLIGHT_COLOR_PINK = 10,
  BACKLIGHT_COLOR_COUNT = 11,
} BacklightColorId;

// The shape of the vibration played when a new navigation instruction is parsed (VibePatternId,
// REQ-WATCH-016) lives in shortcuts.h with its menu labels, so both are host-tested.

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
// The accent's own colour and the text colour that reads on it, for previewing a colour in the
// accent menu (#81).
GColor accent_swatch(AccentId id);
GColor accent_swatch_text(AccentId id);
const char *units_name(UnitsId id);
const char *glyph_pack_name(GlyphPack id);
const char *eta_mode_name(EtaMode id);
const char *eta_label_style_name(EtaLabelStyle id);
// Menu label for a backlight level. The label spells out what the level actually does ("3s after
// update", "Until app closes") rather than a bare Low/Medium/High, which says nothing on its own.
const char *backlight_mode_name(BacklightMode id);
const char *backlight_color_name(BacklightColorId id);
const char *vibe_intensity_name(VibeIntensity id);

// Persisted settings, one value per SettingId (settings_sync.h). Load once at startup.
//
// settings_get() reads a value. settings_store() checks a value against its range (an out-of-range
// one becomes the default, as the old per-setting setters did), saves it only when it changed (flash
// wear) and says whether it did. settings_set() is the watch's own edit: a store that, when it
// changed something, also calls the edit handler, which bumps the settings revision and sends the
// settings to the phone (REQ-WATCH-025, #102). A phone edit is applied with settings_store(), so it
// is never echoed back as a watch edit.
int settings_get(SettingId id);
bool settings_store(SettingId id, int value);
void settings_set(SettingId id, int value);
void settings_set_edit_handler(void (*on_edit)(void));
void settings_load(void);

// The getters and setters by name, as the menus, the shortcuts and the renderer use them.
#define settings_accent() ((AccentId)settings_get(SET_ACCENT))
#define settings_inverted() ((bool)settings_get(SET_INVERT))
#define settings_units() ((UnitsId)settings_get(SET_UNITS))
#define settings_glyph_pack() ((GlyphPack)settings_get(SET_GLYPH_PACK))
#define settings_eta_mode() ((EtaMode)settings_get(SET_ETA_MODE))
#define settings_eta_label_style() ((EtaLabelStyle)settings_get(SET_ETA_LABEL))
#define settings_backlight() ((BacklightMode)settings_get(SET_BACKLIGHT))
#define settings_backlight_color() ((BacklightColorId)settings_get(SET_BACKLIGHT_COLOR))
#define settings_vibe_pattern() ((VibePatternId)settings_get(SET_VIBE_PATTERN))
#define settings_vibe_intensity() ((VibeIntensity)settings_get(SET_VIBE_INTENSITY))
// Early-warning vibration (REQ-WATCH-022): the distance threshold, whether it repeats, and its own
// pattern and strength, separate from the new-instruction buzz so the two can be told apart.
#define settings_early_warn_distance() ((EarlyWarnDistance)settings_get(SET_WARN_DISTANCE))
#define settings_early_warn_repeat() ((EarlyWarnRepeat)settings_get(SET_WARN_REPEAT))
#define settings_early_warn_pattern() ((VibePatternId)settings_get(SET_WARN_PATTERN))
#define settings_early_warn_intensity() ((VibeIntensity)settings_get(SET_WARN_INTENSITY))
// Whether the maneuver arrow sits in the top-LEFT corner (with the distance on the right); the
// default is the arrow on the right and the distance on the left.
#define settings_arrow_left() ((bool)settings_get(SET_ARROW_LEFT))
// Shortcuts (REQ-WATCH-024, #12): whether the navigation screen reacts to touch (Off by default),
// and the action of each button and gesture slot.
#define settings_touch_enabled() ((bool)settings_get(SET_TOUCH))
#define settings_shortcut(slot) ((ShortcutAction)settings_get((SettingId)(SET_SHORTCUT_FIRST + (slot))))

#define settings_set_accent(v) settings_set(SET_ACCENT, (v))
#define settings_set_inverted(v) settings_set(SET_INVERT, (v))
#define settings_set_units(v) settings_set(SET_UNITS, (v))
#define settings_set_glyph_pack(v) settings_set(SET_GLYPH_PACK, (v))
#define settings_set_arrow_left(v) settings_set(SET_ARROW_LEFT, (v))
#define settings_set_eta_mode(v) settings_set(SET_ETA_MODE, (v))
#define settings_set_eta_label_style(v) settings_set(SET_ETA_LABEL, (v))
#define settings_set_backlight(v) settings_set(SET_BACKLIGHT, (v))
#define settings_set_backlight_color(v) settings_set(SET_BACKLIGHT_COLOR, (v))
#define settings_set_vibe_pattern(v) settings_set(SET_VIBE_PATTERN, (v))
#define settings_set_vibe_intensity(v) settings_set(SET_VIBE_INTENSITY, (v))
#define settings_set_early_warn_distance(v) settings_set(SET_WARN_DISTANCE, (v))
#define settings_set_early_warn_repeat(v) settings_set(SET_WARN_REPEAT, (v))
#define settings_set_early_warn_pattern(v) settings_set(SET_WARN_PATTERN, (v))
#define settings_set_early_warn_intensity(v) settings_set(SET_WARN_INTENSITY, (v))
#define settings_set_touch_enabled(v) settings_set(SET_TOUCH, (v))
#define settings_set_shortcut(slot, v) settings_set((SettingId)(SET_SHORTCUT_FIRST + (slot)), (v))

// The last New instruction pattern that wasn't Off, which the Vibration shortcut turns back on
// (REQ-WATCH-024): Short pulse until another one is chosen. Not synced (persist key 27).
VibePatternId settings_vibe_last_pattern(void);

// Play the vibration selected for a newly parsed instruction, built from the current pattern and
// intensity settings (REQ-WATCH-016): the pattern itself, the phone's short pulse for Short pulse,
// nothing for Off. Shared by the navigation path (fired on new info) and the settings menu (fired
// as a preview when the user changes either row).
void settings_vibe_play(void);

// Play the early-warning buzz from its own pattern and intensity (REQ-WATCH-022). No-op when the
// distance or the pattern is Off. Fired by the navigation path and as the menu's preview.
void settings_early_warn_play(void);

// How long (ms) the backlight is held lit after activity for the current backlight mode; 0 means
// keep it on with no timeout (steady-on) and BACKLIGHT_OFF also returns 0 (no forcing).
uint32_t settings_backlight_hold_ms(void);

// Push the selected tint to the backlight LED (REQ-WATCH-015). Called whenever the app engages the
// light and when the colour setting changes. BACKLIGHT_COLOR_DEFAULT restores the user's own colour
// via light_set_system_color(). A no-op in effect on watches without an RGB backlight, where the
// SDK's tint calls compile to nothing.
void settings_apply_backlight_color(void);
