#include "settings_sync.h"

#include "generated/protocol.h"
#include "revision.h"
#include "shortcuts.h"

// One byte per setting: the number of values (low five bits) and what the setting needs. The counts
// are the *_COUNT of each enum in theme.h, early_warning.h and shortcuts.h; theme.c checks them.
#define NEEDS_RGB 0x20u
#define NEEDS_TOUCH 0x40u
#define FROM_ONE 0x80u  // value 0 is not one of its values (the early warning has no Off pattern)
#define COUNT_MASK 0x1Fu

static const uint8_t kRules[SET_COUNT] = {
  [SET_ACCENT] = 16,
  [SET_UNITS] = 2,
  [SET_INVERT] = 2,
  [SET_GLYPH_PACK] = 3,
  [SET_ARROW_LEFT] = 2,
  [SET_ETA_MODE] = 2,
  [SET_BACKLIGHT] = 4,
  [SET_VIBE_PATTERN] = 6,
  [SET_VIBE_INTENSITY] = 3,
  [SET_BACKLIGHT_COLOR] = 11 | NEEDS_RGB,
  [SET_ETA_LABEL] = 2,
  [SET_WARN_DISTANCE] = 6,
  [SET_WARN_REPEAT] = 2,
  [SET_WARN_PATTERN] = 5 | FROM_ONE,
  [SET_WARN_INTENSITY] = 3,
  [SET_TOUCH] = 2 | NEEDS_TOUCH,
  [SET_SHORTCUT_FIRST + SLOT_UP_BUTTON] = ACTION_COUNT,
  [SET_SHORTCUT_FIRST + SLOT_DOWN_BUTTON] = ACTION_COUNT,
  [SET_SHORTCUT_FIRST + SLOT_SWIPE_LEFT] = ACTION_COUNT | NEEDS_TOUCH,
  [SET_SHORTCUT_FIRST + SLOT_SWIPE_RIGHT] = ACTION_COUNT | NEEDS_TOUCH,
  [SET_SHORTCUT_FIRST + SLOT_SWIPE_UP] = ACTION_COUNT | NEEDS_TOUCH,
  [SET_SHORTCUT_FIRST + SLOT_SWIPE_DOWN] = ACTION_COUNT | NEEDS_TOUCH,
  [SET_SHORTCUT_FIRST + SLOT_TAP] = ACTION_COUNT | NEEDS_TOUCH,
};

// Defaults of a fresh install (#102 testing): New instruction vibration Off, Medium strengths, an early
// warning at 25 m on every update with a Double pattern, Touch on, Backlight now on Tap and Down, and
// the ETA toggle on Up and the left and right swipes. Everything else starts at 0.
static const uint8_t kDefaults[SET_COUNT] = {
  [SET_VIBE_PATTERN] = VIBE_PATTERN_SILENT,
  [SET_VIBE_INTENSITY] = 1,
  [SET_WARN_DISTANCE] = 2,  // EARLY_WARN_25M
  [SET_WARN_REPEAT] = 1,    // EARLY_WARN_REPEAT_EVERY
  [SET_WARN_PATTERN] = VIBE_PATTERN_DOUBLE,
  [SET_WARN_INTENSITY] = 1,
  [SET_TOUCH] = 1,
  [SET_SHORTCUT_FIRST + SLOT_UP_BUTTON] = ACTION_ETA_MODE,
  [SET_SHORTCUT_FIRST + SLOT_DOWN_BUTTON] = ACTION_BACKLIGHT_NOW,
  [SET_SHORTCUT_FIRST + SLOT_SWIPE_LEFT] = ACTION_ETA_MODE,
  [SET_SHORTCUT_FIRST + SLOT_SWIPE_RIGHT] = ACTION_ETA_MODE,
  [SET_SHORTCUT_FIRST + SLOT_TAP] = ACTION_BACKLIGHT_NOW,
};

// The accents a black-and-white watch shows: Black and White, the last two (theme.h).
#define ACCENT_BLACK_ID 14
#define ACCENT_WHITE_ID 15

_Static_assert(SET_COUNT == 23, "the protocol carries 23 settings");
_Static_assert(SLOT_COUNT == 7, "one setting per shortcut slot");
_Static_assert(PBNTN_KEY_SETTING_SHORTCUT_TAP - PBNTN_KEY_SETTING_ACCENT == SET_COUNT - 1,
               "the SETTING_* keys follow the SettingId order");

uint8_t sync_build_caps(void) {
  uint8_t caps = 0;
#ifdef PBL_COLOR
  caps |= PBNTN_CAP_COLOR_MASK;
#endif
#ifdef PBL_ROUND
  caps |= PBNTN_CAP_ROUND_MASK;
#endif
#ifdef PBL_RGB_BACKLIGHT
  caps |= PBNTN_CAP_RGB_BACKLIGHT_MASK;
#endif
#ifdef PBL_TOUCH
  caps |= PBNTN_CAP_TOUCH_MASK;
#endif
  return caps;
}

uint8_t sync_build_platform(void) {
#if defined(PBL_PLATFORM_APLITE)
  return PBNTN_PLATFORM_APLITE;
#elif defined(PBL_PLATFORM_BASALT)
  return PBNTN_PLATFORM_BASALT;
#elif defined(PBL_PLATFORM_CHALK)
  return PBNTN_PLATFORM_CHALK;
#elif defined(PBL_PLATFORM_DIORITE)
  return PBNTN_PLATFORM_DIORITE;
#elif defined(PBL_PLATFORM_EMERY)
  return PBNTN_PLATFORM_EMERY;
#else
  return PBNTN_PLATFORM_UNKNOWN;
#endif
}

bool sync_valid(SettingId id, int32_t value, uint32_t caps) {
  if ((unsigned)id >= SET_COUNT) {
    return false;
  }
  const uint8_t rule = kRules[id];
  const int32_t first = (rule & FROM_ONE) ? 1 : 0;
  if (value < first || value >= (int32_t)(rule & COUNT_MASK)) {
    return false;
  }
  if (((rule & NEEDS_RGB) && !(caps & PBNTN_CAP_RGB_BACKLIGHT_MASK)) ||
      ((rule & NEEDS_TOUCH) && !(caps & PBNTN_CAP_TOUCH_MASK))) {
    return false;
  }
  if (id == SET_ACCENT && !(caps & PBNTN_CAP_COLOR_MASK)) {
    return value == ACCENT_BLACK_ID || value == ACCENT_WHITE_ID;
  }
  if (id >= SET_SHORTCUT_FIRST && value == ACTION_NEXT_BACKLIGHT_COLOUR) {
    return (caps & PBNTN_CAP_RGB_BACKLIGHT_MASK) != 0;
  }
  return true;
}

int sync_value_count(SettingId id) {
  return ((unsigned)id < SET_COUNT) ? (int)(kRules[id] & COUNT_MASK) : 0;
}

int sync_default(SettingId id, uint32_t caps) {
  if ((unsigned)id >= SET_COUNT) {
    return 0;
  }
  if (id == SET_ACCENT) {
    return (caps & PBNTN_CAP_COLOR_MASK) ? 0 : ACCENT_BLACK_ID;
  }
  return kDefaults[id];
}

bool sync_is_bool(SettingId id) {
  return id == SET_INVERT || id == SET_ARROW_LEFT || id == SET_TOUCH;
}

uint32_t sync_persist_key(SettingId id) {
  return (id < SET_TOUCH) ? (uint32_t)id + 1 : (uint32_t)id + 4;
}

SyncDecision sync_decide(int watch_rev, int phone_rev, bool differs) {
  if (revision_newer(phone_rev, watch_rev)) {
    return SYNC_APPLY_PHONE;
  }
  if (revision_valid(phone_rev) && phone_rev == watch_rev && !differs) {
    return SYNC_SAME;
  }
  return SYNC_KEEP_WATCH;
}
