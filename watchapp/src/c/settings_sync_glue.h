// Settings sync with the phone app (REQ-WATCH-025, #102): the Pebble side. It keeps the settings
// revision (persist key 28), writes the settings into an outgoing message (WATCH_READY or SETTINGS)
// and applies a SETTINGS message from the phone. Which values a message may change, and whether the
// phone's or the watch's settings win, is decided by the pure settings_sync.c and revision.c.

#pragma once

#include <pebble.h>

#include "settings_sync.h"

// Load the stored revision and route every watch edit (settings_set) to settings_sync_on_watch_edit.
// `request_send` asks main.c to send a SETTINGS message when the outbox allows.
void settings_sync_init(void (*request_send)(void));

// A setting was changed on the watch (a menu, the Shortcuts door, Touch or a shortcut action): the
// revision moves on and the settings go to the phone.
void settings_sync_on_watch_edit(void);

// Append the revision, the platform, the capabilities and all 23 settings to `out`, as uint8.
void settings_sync_write(DictionaryIterator *out);

// Handle a SETTINGS message from the phone. When the phone's revision is newer, its valid values are
// stored where they differ and its revision is adopted, with no revision bump and no echo of its
// own (the caller sends one confirmation). Returns what the revision comparison decided.
SyncDecision settings_sync_apply(DictionaryIterator *in);
