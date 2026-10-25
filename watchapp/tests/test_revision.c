// Host-compiled unit tests for the settings revision (REQ-WATCH-025, #102): the wrap-around "newer"
// rule and the next revision. Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator.

#include "../src/c/revision.h"

#include <stdio.h>

static int failures;

static void expect_bool(const char *what, bool got, bool want) {
  if (got != want) {
    printf("  FAIL  %s: expected %s\n", what, want ? "true" : "false");
    failures++;
  } else {
    printf("  ok    %s == %s\n", what, got ? "true" : "false");
  }
}

static void expect_int(const char *what, int got, int want) {
  if (got != want) {
    printf("  FAIL  %s: expected %d, got %d\n", what, want, got);
    failures++;
  } else {
    printf("  ok    %s == %d\n", what, got);
  }
}

int main(void) {
  printf("next (FR2)\n");
  expect_int("0 -> 1", revision_next(0), 1);
  expect_int("42 -> 43", revision_next(42), 43);
  expect_int("99 wraps to 0", revision_next(99), 0);
  expect_int("out of range restarts at 0", revision_next(100), 0);
  expect_int("negative restarts at 0", revision_next(-1), 0);

  printf("newer (FR3)\n");
  expect_bool("1 ahead is newer", revision_newer(11, 10), true);
  expect_bool("49 ahead is newer", revision_newer(59, 10), true);
  expect_bool("equal is not newer", revision_newer(10, 10), false);
  expect_bool("50 apart is not newer", revision_newer(60, 10), false);
  expect_bool("50 apart is not newer the other way either", revision_newer(10, 60), false);
  expect_bool("51 ahead is behind", revision_newer(61, 10), false);
  expect_bool("99 ahead is 1 behind", revision_newer(9, 10), false);
  expect_bool("rollover: 0 is newer than 99", revision_newer(0, 99), true);
  expect_bool("rollover: 3 is newer than 97", revision_newer(3, 97), true);
  expect_bool("rollover: 99 is not newer than 0", revision_newer(99, 0), false);
  expect_bool("out of range is never newer", revision_newer(100, 10), false);
  expect_bool("nothing is newer than out of range", revision_newer(10, -1), false);

  printf("valid\n");
  expect_bool("0", revision_valid(0), true);
  expect_bool("99", revision_valid(99), true);
  expect_bool("100", revision_valid(100), false);
  expect_bool("-1", revision_valid(-1), false);

  if (failures != 0) {
    printf("\n%d assertion(s) FAILED\n", failures);
    return 1;
  }
  printf("\nAll revision assertions passed.\n");
  return 0;
}
