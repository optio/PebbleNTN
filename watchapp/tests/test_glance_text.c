// Host-compiled unit tests for the launcher subtitle text (REQ-WATCH-019, amended by #94).
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

static void check_update(const char *what, const char *version, size_t len, bool want_ok,
                         const char *want) {
  char buf[48];
  bool ok = glance_update_subtitle(version, buf, len);
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

  // #94 (FR6): the subtitle while a phone app update is recorded and no countdown applies.
  printf("glance_update_subtitle\n");
  check_update("update recorded", "0.0.71", 48, true, "Update: 0.0.71");
  check_update("template characters dropped", "0.0.{7}1\\", 48, true, "Update: 0.0.71");
  check_update("empty version: hint", "", 48, false, "");
  check_update("NULL version: hint", NULL, 48, false, "");
  check_update("only template characters: hint", "{}\\", 48, false, "");
  // "Update: 0.0.71" is 14 characters, so it needs 15 bytes with the terminator.
  check_update("exact fit", "0.0.71", 15, true, "Update: 0.0.71");
  check_update("small buffer: hint, never a cut version", "0.0.71", 14, false, "");

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  return 0;
}
