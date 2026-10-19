// Early-warning vibration decision (REQ-WATCH-022).
//
// When the user picks a distance threshold, the watch buzzes as the next maneuver comes within that
// distance: once per instruction, or on every distance update at or under it. This module holds the
// thresholds, their menu labels and the decision itself, kept apart from the vibration motor and the
// persisted settings.
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_early_warning.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

// The distance choices offered in the menu. OFF is the default: no early warning at all.
typedef enum {
  EARLY_WARN_OFF = 0,
  EARLY_WARN_10M = 1,
  EARLY_WARN_25M = 2,
  EARLY_WARN_50M = 3,
  EARLY_WARN_100M = 4,
  EARLY_WARN_200M = 5,
  EARLY_WARN_COUNT = 6,
} EarlyWarnDistance;

// Whether the warning fires once per instruction or on every distance update under the threshold.
typedef enum {
  EARLY_WARN_REPEAT_ONCE = 0,   // default
  EARLY_WARN_REPEAT_EVERY = 1,
  EARLY_WARN_REPEAT_COUNT = 2,
} EarlyWarnRepeat;

// Threshold in metres; 0 for OFF (or an out-of-range id).
int32_t early_warn_threshold_m(EarlyWarnDistance id);

// Menu label for a threshold: "Off", "50 m", or in imperial the nearest round feet ("160 ft"). The
// threshold itself stays in metres; the feet value is only how it reads.
void early_warn_distance_label(EarlyWarnDistance id, bool imperial, char *buf, size_t len);

const char *early_warn_repeat_name(EarlyWarnRepeat id);

// Per-instruction memory: the distance the warning last fired at, or -1 while it has not fired for
// the current instruction.
typedef struct {
  int32_t warned_at_m;
} EarlyWarnState;

void early_warn_reset(EarlyWarnState *state);

// Decide whether this navigation update plays the early-warning buzz, and update `state`.
//   new_info          the update is a new instruction (changed maneuver or primary text): re-arms.
//   new_info_buzzed   a new-instruction buzz is already playing for this update. It takes precedence
//                     and counts as the warning, so the two never stack.
//   distance_m        distance to the maneuver; negative when unknown.
//   transit           a transit ride, which has no turn distance to warn about (REQ-WATCH-018).
// A plain resend of the same distance never buzzes again.
bool early_warn_should_fire(EarlyWarnState *state, EarlyWarnDistance threshold,
                            EarlyWarnRepeat repeat, bool new_info, bool new_info_buzzed,
                            int32_t distance_m, bool transit);
