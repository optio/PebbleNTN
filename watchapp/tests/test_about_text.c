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
  char buf[ABOUT_DETAILS_LEN];
  about_details(buf, sizeof(buf), "0.0.68", "0.0.67", 1, 1, "Phone app connected");
  check("all lines", buf,
        "Watchapp 0.0.68\nPhone app 0.0.67 (last seen)\nProtocol 1.1\nPhone app connected");
  about_details(buf, sizeof(buf), "0.0.68", "0.0.67", 1, 1, "Pebble app not connected");
  check("last seen while out of range", buf,
        "Watchapp 0.0.68\nPhone app 0.0.67 (last seen)\nProtocol 1.1\nPebble app not connected");
  about_details(buf, sizeof(buf), "0.0.68", NULL, 1, 1, "Pebble app not connected");
  check("never connected (no version)", buf,
        "Watchapp 0.0.68\nPhone app: never connected\nProtocol 1.1\nPebble app not connected");
  about_details(buf, sizeof(buf), "0.0.68", "", 1, 1, "Phone app not answering");
  check("never connected (empty version)", buf,
        "Watchapp 0.0.68\nPhone app: never connected\nProtocol 1.1\nPhone app not answering");
  about_details(buf, sizeof(buf), NULL, NULL, 1, 2, NULL);
  check("missing values", buf, "Watchapp ?\nPhone app: never connected\nProtocol 1.2\n");

  // Worst case (FR2.4): 16-byte versions on both sides and the longest status line all fit, uncut.
  about_details(buf, sizeof(buf), "0.0.68-abcdefghi", "0.0.67-very-long", 99, 99,
                about_phone_status(false, false));
  check("worst case fits uncut", buf,
        "Watchapp 0.0.68-abcdefghi\nPhone app 0.0.67-very-long (last seen)\nProtocol 99.99\n"
        "Pebble app not connected");

  char small[12];
  about_details(small, sizeof(small), "0.0.68", "0.0.67", 1, 1, "Phone app connected");
  check("truncated, still terminated", small, "Watchapp 0.");
  char tiny[1] = {'x'};
  about_details(tiny, sizeof(tiny), "0.0.68", "0.0.67", 1, 1, "Phone app connected");
  check("one-byte buffer is just the terminator", tiny, "");

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  printf("\nAll about_text assertions passed.\n");
  return 0;
}
