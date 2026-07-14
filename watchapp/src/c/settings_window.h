// On-watch settings menu (REQ-WATCH-011): theme + units. Opened with SELECT from the navigation
// window, dismissed with BACK. Changes apply immediately and persist.

#pragma once

// Push the settings menu. `on_change` is called whenever a setting changes so the navigation
// window can re-apply the theme and redraw.
void settings_window_push(void (*on_change)(void));
