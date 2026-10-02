// Host-compiled unit tests for the About screen text (REQ-WATCH-020).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/about_text.h"

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

int main(void) {
  printf("about_phone_status\n");
  check("phone app answered", about_phone_status(true, true), "Phone app connected");
  check("answered wins even if the link just dropped", about_phone_status(false, true), "Phone app connected");
  check("Pebble app only", about_phone_status(true, false), "Phone app not answering");
  check("nothing connected", about_phone_status(false, false), "Pebble app not connected");

  printf("about_details\n");
  char buf[80];
  about_details(buf, sizeof(buf), "0.0.48", 1, 1, "Phone app connected");
  check("all lines", buf, "Version 0.0.48\nProtocol 1.1\nPhone app connected");
  about_details(buf, sizeof(buf), NULL, 1, 2, NULL);
  check("missing values", buf, "Version ?\nProtocol 1.2\n");
  char small[12];
  about_details(small, sizeof(small), "0.0.48", 1, 1, "Phone app connected");
  check("truncated, still terminated", small, "Version 0.0");

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  printf("\nAll about_text assertions passed.\n");
  return 0;
}
