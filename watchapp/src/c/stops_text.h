// Stop-count formatting for public-transit legs (REQ-WATCH-018).
//
// A TRANSIT state carries `stopsRemaining` (protocol 1.1) instead of a distance, and the watch shows
// it where the distance normally goes: a big number with a small unit, like "5" + "stops".
//
// Pure function with no Pebble dependencies, so it compiles on the host and is unit-tested there
// (watchapp/tests/test_stops_text.c).

#pragma once

#include <stddef.h>
#include <stdint.h>

// Writes the count into `value` and "stop"/"stops" into `unit`. A negative count (absent) leaves both
// empty, so the caller can fall back to the distance.
void stops_format(int32_t stops, char *value, size_t value_len, char *unit, size_t unit_len);
