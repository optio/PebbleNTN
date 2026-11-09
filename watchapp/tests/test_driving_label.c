// Host-compiled unit tests for the When driving labels (REQ-WATCH-026, #132 FR2).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/driving_label.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void check(const char *what, const char *got, const char *want) {
  if (strcmp(got, want) != 0) {
    printf("  FAIL  %s: expected \"%s\", got \"%s\"\n", what, want, got);
    failures++;
  } else {
    printf("  ok    %s\n", what);
  }
}

static void check_int(const char *what, int got, int want) {
  if (got != want) {
    printf("  FAIL  %s: expected %d, got %d\n", what, want, got);
    failures++;
  } else {
    printf("  ok    %s\n", what);
  }
}

int main(void) {
  char buf[DRIVING_LABEL_LEN];

  printf("driving_summary (the main Settings row)\n");
  driving_summary(false, DRIVING_METRIC_DEFAULT, false, buf, sizeof(buf));
  check("Show", buf, "Show directions");
  driving_summary(true, DRIVING_METRIC_DEFAULT, false, buf, sizeof(buf));
  check("Hide, metric default", buf, "Hide above 25 km/h");
  driving_summary(true, DRIVING_IMPERIAL_DEFAULT, true, buf, sizeof(buf));
  check("Hide, imperial default", buf, "Hide above 15 mph");
  driving_summary(true, 10, false, buf, sizeof(buf));
  check("longest label fits", buf, "Hide above 60 km/h");

  printf("driving_label (the door and the speed list)\n");
  driving_label("Default (", ")", driving_default_index(false), false, buf, sizeof(buf));
  check("metric default row", buf, "Default (25 km/h)");
  driving_label("Default (", ")", driving_default_index(true), true, buf, sizeof(buf));
  check("imperial default row", buf, "Default (15 mph)");
  driving_label("Above ", "", 10, false, buf, sizeof(buf));
  check("Driving speed row", buf, "Above 60 km/h");
  driving_label("", "", 0, true, buf, sizeof(buf));
  check("first imperial speed", buf, "5 mph");

  printf("driving_speed (the lists)\n");
  check_int("metric first", driving_speed(0, false), 10);
  check_int("metric last", driving_speed(DRIVING_METRIC_COUNT - 1, false), 60);
  check_int("imperial last", driving_speed(DRIVING_IMPERIAL_COUNT - 1, true), 40);
  check_int("out of range reads as the default", driving_speed(DRIVING_IMPERIAL_COUNT, true), 15);
  check_int("negative reads as the default", driving_speed(-1, false), 25);

  char small[6];
  driving_label("Above ", "", 3, false, small, sizeof(small));
  check("truncated, still terminated", small, "Above");
  driving_summary(true, 3, false, NULL, 0);  // must not crash

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  printf("\nAll driving_label assertions passed.\n");
  return 0;
}
