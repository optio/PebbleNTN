// Settings revisions (REQ-WATCH-025, #102): a counter that each edit on the watch, or on the phone,
// moves on by one, so the two sides can tell whose settings are newer.
//
// Revisions run 0..99 and wrap from 99 to 0. `a` is newer than `b` when it is 1 to 49 steps ahead
// of it; 0 steps is the same revision, and 50 or more counts as not newer, so between two sides
// that are exactly 50 apart neither is newer (the watch then keeps its own settings).
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_revision.c). The phone mirrors this in WatchSettingsSync.kt.

#pragma once

#include <stdbool.h>

#define REVISION_MODULO 100

// Whether `rev` is a revision at all (0..99).
bool revision_valid(int rev);

// Whether revision `a` is newer than `b`; false when either is out of range.
bool revision_newer(int a, int b);

// The revision after `rev`: 99 is followed by 0; an out-of-range `rev` restarts at 0.
int revision_next(int rev);
