// Launcher subtitle ("app glance") text (REQ-WATCH-019, #15).
//
// While a route is active, leaving the watchapp shows a live countdown to the arrival time under the
// app name, e.g. "In 0:24 · ETA 13:24"; the launcher keeps the countdown current. Otherwise, while
// a phone app update is recorded, "Update: 0.0.71" (#94); else a short hint. Only the
// text lives here; main.c hands it to the App Glance API.
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_glance_text.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

// The subtitle when no route is active, or once the countdown expires. About 18 characters fit.
#define GLANCE_HINT "Navigate on phone"

// Writes the countdown template into `buf` and returns true when a route is active and its arrival
// time (`eta_epoch`, 0 = unknown) is still ahead of `now`. `eta_text` is the phone's arrival-time
// string ("13:24", may be empty); characters the template language treats specially are dropped.
// Returns false (and writes an empty string) when the hint should be shown instead.
bool glance_countdown_template(bool navigating, int32_t eta_epoch, int32_t now, const char *eta_text,
                               char *buf, size_t buf_len);

// The subtitle while a phone app update is recorded (REQ-WATCH-019 as amended by #94): writes
// "Update: <version>" into `buf`, with the template characters `{`, `}` and `\` dropped
// from the version. Returns false (and writes an empty string) when the version is empty or the text
// does not fit, so the caller falls back to GLANCE_HINT.
bool glance_update_subtitle(const char *version, char *buf, size_t buf_len);
