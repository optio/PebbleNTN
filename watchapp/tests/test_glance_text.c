// Host-compiled unit tests for the launcher subtitle text (REQ-WATCH-019).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/glance_text.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void check(const char *what, bool navigating, int32_t eta, int32_t now, const char *eta_text,
                  bool want_ok, const char *want) {
  char buf[150];
  bool ok = glance_countdown_template(navigating, eta, now, eta_text, buf, sizeof(buf));
  if (ok != want_ok || strcmp(buf, want) != 0) {
    printf("  FAIL  %s: expected %d \"%s\", got %d \"%s\"\n", what, want_ok, want, ok, buf);
    failures++;
  } else {
    printf("  ok    %s\n", what);
  }
}

int main(void) {
  printf("glance_countdown_template\n");
  check("active route: countdown and arrival time", true, 2000, 1000, "13:24", true,
        "In {time_until(2000)|format('%H:%0M')} \xC2\xB7 ETA 13:24");
  check("no arrival text: countdown only", true, 2000, 1000, "", true,
        "In {time_until(2000)|format('%H:%0M')}");
  check("NULL arrival text: countdown only", true, 2000, 1000, NULL, true,
        "In {time_until(2000)|format('%H:%0M')}");
  check("not navigating: hint", false, 2000, 1000, "13:24", false, "");
  check("no arrival epoch: hint", true, 0, 1000, "13:24", false, "");
  check("arrival time passed: hint", true, 1000, 1000, "13:24", false, "");
  check("template characters dropped", true, 2000, 1000, "{1}3:2\\4", true,
        "In {time_until(2000)|format('%H:%0M')} \xC2\xB7 ETA 13:24");

  printf("glance_countdown_template — too small a buffer gives the hint, never a cut template\n");
  char small[20];
  bool ok = glance_countdown_template(true, 2000, 1000, "13:24", small, sizeof(small));
  if (ok || small[0] != '\0') {
    printf("  FAIL  small buffer: %d \"%s\"\n", ok, small);
    failures++;
  } else {
    printf("  ok    small buffer\n");
  }

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  return 0;
}
