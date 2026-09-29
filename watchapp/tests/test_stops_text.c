// Host-compiled unit tests for the transit stop-count formatting (REQ-WATCH-018).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/stops_text.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void check(int32_t stops, const char *want_value, const char *want_unit) {
  char value[12];
  char unit[8];
  stops_format(stops, value, sizeof(value), unit, sizeof(unit));
  if (strcmp(value, want_value) != 0 || strcmp(unit, want_unit) != 0) {
    printf("  FAIL  stops_format(%ld): expected \"%s\" \"%s\", got \"%s\" \"%s\"\n",
           (long)stops, want_value, want_unit, value, unit);
    failures++;
  } else {
    printf("  ok    stops_format(%ld) == \"%s\" \"%s\"\n", (long)stops, value, unit);
  }
}

int main(void) {
  printf("stops_format\n");
  check(5, "5", "stops");
  check(1, "1", "stop");      // singular
  check(0, "0", "stops");     // at the station: still a count, not absent
  check(12, "12", "stops");
  check(-1, "", "");          // absent: the caller falls back to the distance

  printf("stops_format — small buffers never overflow\n");
  char value[3];
  char unit[4];
  stops_format(12345, value, sizeof(value), unit, sizeof(unit));
  if (strlen(value) >= sizeof(value) || strlen(unit) >= sizeof(unit)) {
    printf("  FAIL  truncation\n");
    failures++;
  } else {
    printf("  ok    truncated to \"%s\" \"%s\"\n", value, unit);
  }

  if (failures != 0) {
    printf("\n%d assertion(s) FAILED\n", failures);
    return 1;
  }
  printf("\nAll stops_text assertions passed.\n");
  return 0;
}
