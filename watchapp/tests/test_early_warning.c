// Host-compiled unit tests for the early-warning vibration decision (REQ-WATCH-022).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/early_warning.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void expect(const char *what, bool got, bool want) {
  if (got != want) {
    printf("  FAIL  %s: expected %s, got %s\n", what, want ? "buzz" : "quiet", got ? "buzz" : "quiet");
    failures++;
  } else {
    printf("  ok    %s: %s\n", what, got ? "buzz" : "quiet");
  }
}

static void expect_label(EarlyWarnDistance id, bool imperial, const char *want) {
  char buf[12];
  early_warn_distance_label(id, imperial, buf, sizeof(buf));
  if (strcmp(buf, want) != 0) {
    printf("  FAIL  label(%d, %s): expected \"%s\", got \"%s\"\n", (int)id,
           imperial ? "imperial" : "metric", want, buf);
    failures++;
  } else {
    printf("  ok    label(%d, %s) == \"%s\"\n", (int)id, imperial ? "imperial" : "metric", buf);
  }
}

// One navigation update at `distance`; `new_info` marks a new instruction.
static bool update(EarlyWarnState *s, EarlyWarnDistance t, EarlyWarnRepeat r, bool new_info,
                   int32_t distance) {
  return early_warn_should_fire(s, t, r, new_info, false, distance, false);
}

int main(void) {
  EarlyWarnState s;

  printf("thresholds and labels\n");
  expect("Off has no threshold", early_warn_threshold_m(EARLY_WARN_OFF) == 0, true);
  expect("out-of-range id has no threshold", early_warn_threshold_m(EARLY_WARN_COUNT) == 0, true);
  expect("50 m threshold", early_warn_threshold_m(EARLY_WARN_50M) == 50, true);
  expect_label(EARLY_WARN_OFF, false, "Off");
  expect_label(EARLY_WARN_OFF, true, "Off");
  expect_label(EARLY_WARN_10M, false, "10 m");
  expect_label(EARLY_WARN_200M, false, "200 m");
  expect_label(EARLY_WARN_10M, true, "30 ft");
  expect_label(EARLY_WARN_50M, true, "160 ft");
  expect_label(EARLY_WARN_200M, true, "650 ft");

  printf("Off never buzzes\n");
  early_warn_reset(&s);
  expect("Off at 0 m", update(&s, EARLY_WARN_OFF, EARLY_WARN_REPEAT_EVERY, true, 0), false);

  printf("Once: buzz on crossing, then quiet until the next instruction\n");
  early_warn_reset(&s);
  expect("new instruction at 300 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, true, 300), false);
  expect("60 m (above)", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 60), false);
  expect("50 m (at threshold)", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 50), true);
  expect("40 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 40), false);
  expect("0 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 0), false);
  expect("next instruction at 200 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, true, 200), false);
  expect("next instruction reaches 30 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 30), true);

  printf("Every update: buzz on each new distance under the threshold\n");
  early_warn_reset(&s);
  expect("new instruction at 120 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, true, 120), false);
  expect("50 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 50), true);
  expect("resend at 50 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 50), false);
  expect("40 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 40), true);
  expect("30 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 30), true);
  expect("0 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 0), true);

  printf("Once: a resend before crossing doesn't consume the warning\n");
  early_warn_reset(&s);
  expect("new instruction at 80 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, true, 80), false);
  expect("resend at 80 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 80), false);
  expect("20 m", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 20), true);

  printf("new instruction already inside the threshold\n");
  early_warn_reset(&s);
  expect("new-instruction buzz takes precedence",
         early_warn_should_fire(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, true, true, 30, false),
         false);
  expect("Once: no second buzz on the next update",
         update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, false, 20), false);
  early_warn_reset(&s);
  expect("no new-instruction buzz: the warning plays",
         early_warn_should_fire(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_ONCE, true, false, 30, false),
         true);
  early_warn_reset(&s);
  expect("Every: new-instruction buzz takes precedence",
         early_warn_should_fire(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, true, true, 30, false),
         false);
  expect("Every: resend at that distance stays quiet",
         update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 30), false);
  expect("Every: the next distance buzzes", update(&s, EARLY_WARN_50M, EARLY_WARN_REPEAT_EVERY, false, 20), true);

  printf("no distance to warn about\n");
  early_warn_reset(&s);
  expect("unknown distance", update(&s, EARLY_WARN_200M, EARLY_WARN_REPEAT_EVERY, true, -1), false);
  expect("transit ride",
         early_warn_should_fire(&s, EARLY_WARN_200M, EARLY_WARN_REPEAT_EVERY, false, false, 0, true),
         false);

  if (failures != 0) {
    printf("\n%d assertion(s) FAILED\n", failures);
    return 1;
  }
  printf("\nAll early_warning assertions passed.\n");
  return 0;
}
