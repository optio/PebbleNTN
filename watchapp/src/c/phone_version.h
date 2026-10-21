// The last connected phone app version, as the watch records it (REQ-WATCH-020, #103).
//
// The phone sends its own version as key 11 (APP_VERSION) with every message; the watch keeps the
// latest one so About can show it after a relaunch and while the phone is out of range. This module
// holds the recording rule — the length cap, "empty means none", and "did it change?" — apart from
// the persist storage in main.c, which writes only when this says the text changed (flash wear).
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_phone_version.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>

// Longest phone version kept, in bytes of text (the NUL is extra). Real versions are short
// ("0.0.68"); a longer one is cut to its first PHONE_VERSION_MAX bytes.
#define PHONE_VERSION_MAX 16

// Record `incoming` into `stored` (a buffer of `stored_len` bytes, always left NUL-terminated).
// A NULL or empty `incoming` is no version: `stored` is left as it is. Otherwise at most
// min(PHONE_VERSION_MAX, stored_len - 1) bytes are kept. Returns true only when the stored text
// changed, so the caller persists it only then.
bool phone_version_update(char *stored, size_t stored_len, const char *incoming);
