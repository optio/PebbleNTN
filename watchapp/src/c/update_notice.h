// The phone app update notice and idle text (REQ-WATCH-023, #94).
//
// Since protocol 1.2 the phone sets flag bit 4 (PHONE_UPDATE_AVAILABLE) on every state message while
// it knows of a newer phone app, and sends the latest version as key 14 (LATEST_PHONE_VERSION). The
// watch records that version, shows a once-per-version notice when no route is active, and swaps
// the "No navigation detected" text for an update hint. This module holds the rules — what to
// record, when to show, defer or skip the notice, and which idle text to use — apart from the
// persist storage and the window in main.c / update_notice_window.c.
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_update_notice.c). The host test builds against this unit alone, so it keeps
// its own small capped copy rather than reusing phone_version.c.

#pragma once

#include <stdbool.h>
#include <stddef.h>

// Longest latest-version text kept, in bytes (the NUL is extra). Real versions are short
// ("0.0.71"); a longer one is cut to its first UPDATE_VERSION_MAX bytes.
#define UPDATE_VERSION_MAX 16

// The full-screen notice, shown once per latest version (FR4.1).
#define UPDATE_NOTICE_TEXT "A phone app update is available. Open the mobile app to find the update."

// The "No navigation detected" screen while an update is recorded (FR5.1), and the usual text.
#define UPDATE_IDLE_TEXT "No navigation detected. Phone app update available: open the mobile app."
#define IDLE_TEXT "No navigation detected. If navigating, open PebbleNTN on your phone."

typedef enum {
  UPDATE_NOTICE_NONE = 0,  // no update recorded, or the notice was already shown for it
  UPDATE_NOTICE_DEFER,     // wait: a route is active, or another screen is on top
  UPDATE_NOTICE_SHOW,      // show it now
} UpdateNoticeAction;

// Record the update state a message carries into `stored` (a buffer of `stored_len` bytes, always
// left NUL-terminated). With `flag_set` false the recorded version is cleared. With it set and a
// non-empty `incoming`, at most min(UPDATE_VERSION_MAX, stored_len - 1) bytes are kept. With it
// set and a NULL or empty `incoming`, `stored` is left as it is. Returns true only when `stored`
// changed, so the caller persists it only then (flash wear).
bool update_record(char *stored, size_t stored_len, bool flag_set, const char *incoming);

// What to do about the notice, given the recorded `latest` version, the version it was last
// `shown` for, whether a route is active, and whether the main window is on top.
UpdateNoticeAction update_notice_action(const char *latest, const char *shown, bool navigating,
                                        bool main_on_top);

// The "No navigation detected" text: UPDATE_IDLE_TEXT while `latest` is non-empty, else IDLE_TEXT.
const char *update_idle_text(const char *latest);
