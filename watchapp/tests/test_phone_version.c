// Host-compiled unit tests for recording the last connected phone app version (REQ-WATCH-020, #103).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/phone_version.h"

#include <stdio.h>
#include <string.h>

static int failures;

// One recorded update: `changed` is whether the caller would persist, `stored` what is kept.
static void expect(const char *what, bool changed, bool want_changed, const char *stored,
                   const char *want_stored) {
  if (changed != want_changed || strcmp(stored, want_stored) != 0) {
    printf("  FAIL  %s: expected %s \"%s\", got %s \"%s\"\n", what,
           want_changed ? "write" : "no write", want_stored, changed ? "write" : "no write", stored);
    failures++;
  } else {
    printf("  ok    %s: %s \"%s\"\n", what, changed ? "write" : "no write", stored);
  }
}

int main(void) {
  char stored[PHONE_VERSION_MAX + 1] = "";
  bool changed;

  printf("phone_version_update\n");
  changed = phone_version_update(stored, sizeof(stored), "0.0.67");
  expect("first version recorded", changed, true, stored, "0.0.67");
  changed = phone_version_update(stored, sizeof(stored), "0.0.67");
  expect("same version again is not rewritten", changed, false, stored, "0.0.67");
  changed = phone_version_update(stored, sizeof(stored), "0.0.70");
  expect("new version recorded", changed, true, stored, "0.0.70");
  changed = phone_version_update(stored, sizeof(stored), NULL);
  expect("no version (NULL) keeps the stored one", changed, false, stored, "0.0.70");
  changed = phone_version_update(stored, sizeof(stored), "");
  expect("empty version keeps the stored one", changed, false, stored, "0.0.70");

  printf("phone_version_update — length cap\n");
  changed = phone_version_update(stored, sizeof(stored), "0.0.67-very-long-bld");
  expect("20-byte version cut to 16", changed, true, stored, "0.0.67-very-long");
  if (strlen(stored) != PHONE_VERSION_MAX) {
    printf("  FAIL  cut value is %u bytes, expected %d\n", (unsigned)strlen(stored), PHONE_VERSION_MAX);
    failures++;
  }
  changed = phone_version_update(stored, sizeof(stored), "0.0.67-very-long-bld");
  expect("cut value seen again is not rewritten", changed, false, stored, "0.0.67-very-long");
  changed = phone_version_update(stored, sizeof(stored), "0.0.67-very-long");
  expect("the 16-byte prefix itself is unchanged", changed, false, stored, "0.0.67-very-long");

  // A buffer smaller than the cap still never overflows and stays terminated.
  char small[5] = "";
  changed = phone_version_update(small, sizeof(small), "0.0.67");
  expect("small buffer cut and terminated", changed, true, small, "0.0.");
  changed = phone_version_update(small, 0, "0.0.67");
  expect("zero-length buffer is left alone", changed, false, small, "0.0.");

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  printf("\nAll phone_version assertions passed.\n");
  return 0;
}
