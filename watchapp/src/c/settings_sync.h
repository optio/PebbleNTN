// Settings sync with the phone app (REQ-WATCH-025, #102): the pure part.
//
// The 23 watch settings the phone app can show and change are one table, indexed by SettingId:
// each has a value range, a default, the capability it needs and the persist key it is stored
// under. The same order is the order of the SETTING_* protocol keys (protocol 1.3), so the protocol
// key of a setting is PBNTN_KEY_SETTING_ACCENT + its id. theme.c stores the values in this order and
// settings_sync_glue.c writes and reads the messages; this unit decides what a message may change.
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_settings_sync.c). The phone mirrors the table in WatchSettingsModel.kt.

#pragma once

#include <stdbool.h>
#include <stdint.h>

// The synced settings, in protocol key and persist order. The values are stored as enum indices
// (theme.h, early_warning.h, shortcuts.h), the bools as 0/1.
typedef enum {
  SET_ACCENT = 0,
  SET_UNITS,
  SET_INVERT,
  SET_GLYPH_PACK,
  SET_ARROW_LEFT,
  SET_ETA_MODE,
  SET_BACKLIGHT,
  SET_VIBE_PATTERN,
  SET_VIBE_INTENSITY,
  SET_BACKLIGHT_COLOR,
  SET_ETA_LABEL,
  SET_WARN_DISTANCE,
  SET_WARN_REPEAT,
  SET_WARN_PATTERN,
  SET_WARN_INTENSITY,
  SET_TOUCH,
  SET_SHORTCUT_FIRST,  // one per ShortcutSlot, in slot order
  SET_COUNT = SET_SHORTCUT_FIRST + 7,
} SettingId;

// Every capability at once: a value is then checked against its range only, as for a value read
// back from this watch's own storage.
#define SYNC_CAPS_ALL 0xFFu

// What a SETTINGS message from the phone leads to.
typedef enum {
  SYNC_APPLY_PHONE = 0,  // the phone's revision is newer: apply its values and adopt its revision
  SYNC_KEEP_WATCH,       // the watch's settings stand (newer, 50 apart, equal but different)
  SYNC_SAME,             // same revision and the same values: nothing to do
} SyncDecision;

// The capability bits (PBNTN_CAP_*_MASK) and the platform value (PBNTN_PLATFORM_*) of the build
// this is compiled into; 0 and PBNTN_PLATFORM_UNKNOWN on the host.
uint8_t sync_build_caps(void);
uint8_t sync_build_platform(void);

// Whether `value` is one `id` can take on a watch with `caps`: in range, and supported there. A
// black-and-white watch takes only the Black and White accents; Backlight colour and the Next
// backlight colour action need an RGB backlight; Touch and the gesture slots need touch.
bool sync_valid(SettingId id, int32_t value, uint32_t caps);

// How many values `id` has (its enum's *_COUNT); a menu row cycles through 0..count-1. 0 for an
// unknown id.
int sync_value_count(SettingId id);

// The value `id` has before anyone picks one; the accent depends on whether the watch has colour.
int sync_default(SettingId id, uint32_t caps);

// Whether `id` is stored as a bool (persist_write_bool) rather than an int.
bool sync_is_bool(SettingId id);

// The persist key `id` is stored under: 1-15 for the first fifteen, then 19 (Touch) and 20-26 (the
// shortcut slots). Keys 16-18 and 27 are not settings the phone sees.
uint32_t sync_persist_key(SettingId id);

// Compare the watch's revision with the one a phone SETTINGS message carries. `differs` is whether
// any valid value in the message differs from the watch's own. A missing or out-of-range phone
// revision keeps the watch's settings.
SyncDecision sync_decide(int watch_rev, int phone_rev, bool differs);
