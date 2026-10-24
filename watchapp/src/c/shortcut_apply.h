// Running a shortcut on the navigation screen (REQ-WATCH-024, #12): the action goes through the same
// setters, and so the same save, as the settings menu. The pure parts (cycling, the vibration
// toggle, the notice text) live in shortcuts.c; this is the Pebble side that applies them.

#pragma once

#include <pebble.h>

#include "shortcuts.h"

// Whether this watch has an RGB backlight, which is when Next backlight colour is offered. The
// same compile-time condition as the Backlight colour menu (settings_window.c).
#ifdef PBL_RGB_BACKLIGHT
#define SHORTCUT_RGB_BACKLIGHT true
#else
#define SHORTCUT_RGB_BACKLIGHT false
#endif

typedef struct {
  bool show_notice;    // `notice` holds the text to show for about 1.5 s
  bool open_settings;  // the caller opens Settings, as SELECT does
} ShortcutResult;

// Apply `action`: change its setting and save it, and write the notice text into `notice`. The
// caller redraws the navigation screen afterwards, as when returning from Settings.
ShortcutResult shortcut_apply(ShortcutAction action, char *notice, size_t size);
