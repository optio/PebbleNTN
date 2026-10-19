#include "early_warning.h"

#include <stdio.h>

int32_t early_warn_threshold_m(EarlyWarnDistance id) {
  switch (id) {
    case EARLY_WARN_10M: return 10;
    case EARLY_WARN_25M: return 25;
    case EARLY_WARN_50M: return 50;
    case EARLY_WARN_100M: return 100;
    case EARLY_WARN_200M: return 200;
    case EARLY_WARN_OFF:
    default: return 0;
  }
}

// Round feet for each threshold: 10 m is 32.8 ft, which reads as "30 ft". A table rather than a
// conversion, so every label is a value a person would actually say.
static int32_t threshold_ft(EarlyWarnDistance id) {
  switch (id) {
    case EARLY_WARN_10M: return 30;
    case EARLY_WARN_25M: return 80;
    case EARLY_WARN_50M: return 160;
    case EARLY_WARN_100M: return 330;
    case EARLY_WARN_200M: return 650;
    case EARLY_WARN_OFF:
    default: return 0;
  }
}

void early_warn_distance_label(EarlyWarnDistance id, bool imperial, char *buf, size_t len) {
  if (len == 0) {
    return;
  }
  const int32_t meters = early_warn_threshold_m(id);
  if (meters == 0) {
    snprintf(buf, len, "%s", "Off");
  } else if (imperial) {
    snprintf(buf, len, "%d ft", (int)threshold_ft(id));  // %d as in main.c; Pebble's printf is minimal
  } else {
    snprintf(buf, len, "%d m", (int)meters);
  }
}

const char *early_warn_repeat_name(EarlyWarnRepeat id) {
  return (id == EARLY_WARN_REPEAT_EVERY) ? "Every update" : "Once";
}

void early_warn_reset(EarlyWarnState *state) {
  state->warned_at_m = -1;
}

bool early_warn_should_fire(EarlyWarnState *state, EarlyWarnDistance threshold,
                            EarlyWarnRepeat repeat, bool new_info, bool new_info_buzzed,
                            int32_t distance_m, bool transit) {
  if (new_info) {
    early_warn_reset(state);
  }
  const int32_t threshold_m = early_warn_threshold_m(threshold);
  if (threshold_m == 0 || transit || distance_m < 0 || distance_m > threshold_m) {
    return false;
  }
  if (new_info && new_info_buzzed) {
    // Already inside the threshold when the instruction arrives: the new-instruction buzz is the
    // warning. Recording it stops a resend at this distance from buzzing a second time.
    state->warned_at_m = distance_m;
    return false;
  }
  if (repeat == EARLY_WARN_REPEAT_EVERY) {
    if (state->warned_at_m == distance_m) {
      return false;  // a resend of the distance already warned about
    }
  } else if (state->warned_at_m >= 0) {
    return false;  // Once: this instruction has had its warning
  }
  state->warned_at_m = distance_m;
  return true;
}
