// Text for the About screen (REQ-WATCH-020, #48).
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_about_text.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>

// The phone side as the watch sees it: the PebbleNTN phone app has answered since the watchapp
// opened; or only the Pebble app is connected (so PebbleNTN is missing or not running); or not even
// the Pebble app is.
const char *about_phone_status(bool pebble_app_connected, bool heard_from_phone);

// "Version 0.0.47\nProtocol 1.1\n<phone status>" into `buf` (always NUL-terminated).
void about_details(char *buf, size_t buf_len, const char *app_version, int protocol_major,
                   int protocol_minor, const char *phone_status);
