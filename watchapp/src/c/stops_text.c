#include "stops_text.h"

#include <stdio.h>

void stops_format(int32_t stops, char *value, size_t value_len, char *unit, size_t unit_len) {
  if (value_len > 0) {
    value[0] = '\0';
  }
  if (unit_len > 0) {
    unit[0] = '\0';
  }
  if (stops < 0 || value_len == 0 || unit_len == 0) {
    return;
  }
  snprintf(value, value_len, "%d", (int)stops);  // %d as in main.c; Pebble's printf is minimal
  snprintf(unit, unit_len, "%s", stops == 1 ? "stop" : "stops");
}
