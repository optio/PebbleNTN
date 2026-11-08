#include "driving_label.h"

#include <stdio.h>

int driving_default_index(bool imperial) {
  return imperial ? DRIVING_IMPERIAL_DEFAULT : DRIVING_METRIC_DEFAULT;
}

int driving_speed(int index, bool imperial) {
  const int count = imperial ? DRIVING_IMPERIAL_COUNT : DRIVING_METRIC_COUNT;
  if (index < 0 || index >= count) {
    index = driving_default_index(imperial);
  }
  return (imperial ? 5 : 10) + 5 * index;
}

void driving_label(const char *prefix, const char *suffix, int index, bool imperial, char *buf,
                   size_t len) {
  if (buf == NULL || len == 0) {
    return;
  }
  snprintf(buf, len, "%s%d %s%s", prefix ? prefix : "", driving_speed(index, imperial),
           imperial ? "mph" : "km/h", suffix ? suffix : "");
}

// Self-contained rather than built on driving_label, so aplite, which has no When driving door and
// shows only this summary, links none of the door's label code (#132 NFR4).
void driving_summary(bool hide, int index, bool imperial, char *buf, size_t len) {
  if (buf == NULL || len == 0) {
    return;
  }
  if (hide) {
    snprintf(buf, len, "Hide above %d %s", driving_speed(index, imperial), imperial ? "mph" : "km/h");
  } else {
    snprintf(buf, len, "Show directions");
  }
}
