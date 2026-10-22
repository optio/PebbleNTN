// Host-compiled unit tests for the phone app update notice and idle text (REQ-WATCH-023, #94).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/update_notice.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void check(const char *what, bool ok) {
  if (ok) {
    printf("  ok    %s\n", what);
  } else {
    printf("  FAIL  %s\n", what);
    failures++;
  }
}

// One recorded message: `changed` is whether the caller would persist, `stored` what is kept.
static void expect_record(const char *what, bool changed, bool want_changed, const char *stored,
                          const char *want_stored) {
  if (changed != want_changed || strcmp(stored, want_stored) != 0) {
    printf("  FAIL  %s: expected %s \"%s\", got %s \"%s\"\n", what,
           want_changed ? "write" : "no write", want_stored, changed ? "write" : "no write", stored);
    failures++;
  } else {
    printf("  ok    %s: %s \"%s\"\n", what, changed ? "write" : "no write", stored);
  }
}

static const char *action_name(UpdateNoticeAction action) {
  switch (action) {
    case UPDATE_NOTICE_NONE: return "NONE";
    case UPDATE_NOTICE_DEFER: return "DEFER";
    case UPDATE_NOTICE_SHOW: return "SHOW";
  }
  return "?";
}

static void expect_action(const char *what, UpdateNoticeAction got, UpdateNoticeAction want) {
  if (got != want) {
    printf("  FAIL  %s: expected %s, got %s\n", what, action_name(want), action_name(got));
    failures++;
  } else {
    printf("  ok    %s: %s\n", what, action_name(got));
  }
}

static void test_record(void) {
  char stored[UPDATE_VERSION_MAX + 1] = "";
  bool changed;

  printf("update_record\n");
  changed = update_record(stored, sizeof(stored), true, "0.0.71");
  expect_record("first version recorded", changed, true, stored, "0.0.71");
  changed = update_record(stored, sizeof(stored), true, "0.0.71");
  expect_record("same version again is not rewritten", changed, false, stored, "0.0.71");
  changed = update_record(stored, sizeof(stored), true, NULL);
  expect_record("flag set, no version (NULL) keeps it", changed, false, stored, "0.0.71");
  changed = update_record(stored, sizeof(stored), true, "");
  expect_record("flag set, empty version keeps it", changed, false, stored, "0.0.71");
  changed = update_record(stored, sizeof(stored), false, "0.0.71");
  expect_record("flag clear clears it", changed, true, stored, "");
  changed = update_record(stored, sizeof(stored), false, NULL);
  expect_record("flag clear again is not rewritten", changed, false, stored, "");

  printf("update_record — length cap\n");
  changed = update_record(stored, sizeof(stored), true, "0.0.71-very-long-bld");
  expect_record("20-byte version cut to 16", changed, true, stored, "0.0.71-very-long");
  check("cut value is exactly 16 bytes", strlen(stored) == UPDATE_VERSION_MAX);
  changed = update_record(stored, sizeof(stored), true, "0.0.71-very-long-bld");
  expect_record("cut value seen again is not rewritten", changed, false, stored, "0.0.71-very-long");

  char small[5] = "";
  changed = update_record(small, sizeof(small), true, "0.0.71");
  expect_record("small buffer cut and terminated", changed, true, small, "0.0.");
  changed = update_record(small, 0, false, NULL);
  expect_record("zero-length buffer is left alone", changed, false, small, "0.0.");
}

static void test_action(void) {
  printf("update_notice_action\n");
  expect_action("new version, idle, main on top",
                update_notice_action("0.0.71", "", false, true), UPDATE_NOTICE_SHOW);
  expect_action("never shown (NULL shown)",
                update_notice_action("0.0.71", NULL, false, true), UPDATE_NOTICE_SHOW);
  expect_action("already shown for this version",
                update_notice_action("0.0.71", "0.0.71", false, true), UPDATE_NOTICE_NONE);
  expect_action("a newer version shows again",
                update_notice_action("0.0.72", "0.0.71", false, true), UPDATE_NOTICE_SHOW);
  expect_action("during a route: wait",
                update_notice_action("0.0.71", "", true, true), UPDATE_NOTICE_DEFER);
  expect_action("another screen on top: wait",
                update_notice_action("0.0.71", "", false, false), UPDATE_NOTICE_DEFER);
  expect_action("shown already, even during a route",
                update_notice_action("0.0.71", "0.0.71", true, true), UPDATE_NOTICE_NONE);
  expect_action("no update recorded",
                update_notice_action("", "0.0.71", false, true), UPDATE_NOTICE_NONE);
  expect_action("no update (NULL)",
                update_notice_action(NULL, NULL, false, true), UPDATE_NOTICE_NONE);
}

static void test_idle_text(void) {
  printf("update_idle_text\n");
  check("update recorded: update text", strcmp(update_idle_text("0.0.71"), UPDATE_IDLE_TEXT) == 0);
  check("no update: usual text", strcmp(update_idle_text(""), IDLE_TEXT) == 0);
  check("no update (NULL): usual text", strcmp(update_idle_text(NULL), IDLE_TEXT) == 0);
  check("update text reads as specified",
        strcmp(UPDATE_IDLE_TEXT,
               "No navigation detected. Phone app update available: open the mobile app.") == 0);
  check("notice text reads as specified",
        strcmp(UPDATE_NOTICE_TEXT,
               "A phone app update is available. Open the mobile app to find the update.") == 0);
}

int main(void) {
  test_record();
  test_action();
  test_idle_text();

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  printf("\nAll update_notice assertions passed.\n");
  return 0;
}
