#pragma once

#include <pebble.h>

// The phone app update notice (REQ-WATCH-023, #94): a full-screen UPDATE_NOTICE_TEXT, styled like
// About (black on white whatever the theme). It closes after 10 seconds, or at once on any button;
// a press only closes it and never reaches the screen behind. No vibration and no backlight.
// `on_close` (may be NULL) runs once the notice has left the screen. Returns true when the notice
// was pushed, false when it is already open or could not be created.
bool update_notice_window_push(void (*on_close)(void));

// Whether the notice is open now.
bool update_notice_window_is_open(void);

// Free the notice window; call once as the app exits.
void update_notice_window_deinit(void);
