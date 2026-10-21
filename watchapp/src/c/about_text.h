// Text for the About screen (REQ-WATCH-020, #48, #103).
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_about_text.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>

// Size of the details buffer. The worst case is about 105 bytes: a 16-byte watchapp version, a
// 16-byte phone version (PHONE_VERSION_MAX) with " (last seen)", "Protocol 99.99" and the longest
// status line, "Pebble app not connected", plus the newlines and the NUL (#103, FR2.4).
#define ABOUT_DETAILS_LEN 128

// The phone side as the watch sees it: the PebbleNTN phone app has answered since the watchapp
// opened; or only the Pebble app is connected (so PebbleNTN is missing or not running); or not even
// the Pebble app is.
const char *about_phone_status(bool pebble_app_connected, bool heard_from_phone);

// "Watchapp 0.0.68\nPhone app 0.0.67 (last seen)\nProtocol 1.1\n<phone status>" into `buf` (always
// NUL-terminated). The phone line is "Phone app: never connected" when `phone_version` is NULL or
// empty, i.e. no phone app version was ever recorded (#103).
void about_details(char *buf, size_t buf_len, const char *app_version, const char *phone_version,
                   int protocol_major, int protocol_minor, const char *phone_status);
