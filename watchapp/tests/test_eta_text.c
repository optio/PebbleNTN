// Host-compiled unit tests for the watch's arrival-string parsing (REQ-WATCH-014).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/eta_text.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void check_int(const char *what, int actual, int expected) {
  if (actual != expected) {
    printf("  FAIL  %s: expected %d, got %d\n", what, expected, actual);
    failures++;
  } else {
    printf("  ok    %s == %d\n", what, actual);
  }
}

static void check_parse(const char *text, int expected) {
  char label[96];
  snprintf(label, sizeof(label), "parse(\"%s\")", text);
  check_int(label, eta_parse_clock_minutes(text), expected);
}

int main(void) {
  printf("eta_parse_clock_minutes — 24-hour\n");
  check_parse("14:35", 14 * 60 + 35);
  check_parse("00:00", 0);
  check_parse("23:59", 23 * 60 + 59);
  check_parse("9:05", 9 * 60 + 5);

  printf("eta_parse_clock_minutes — 12-hour suffix\n");
  check_parse("2:35 PM", 14 * 60 + 35);
  check_parse("2:35pm", 14 * 60 + 35);
  check_parse("2:35 AM", 2 * 60 + 35);
  check_parse("12:05 AM", 5);            // midnight hour rebases to 0
  check_parse("12:05 PM", 12 * 60 + 5);  // noon hour stays 12

  printf("eta_parse_clock_minutes — embedded in longer text\n");
  check_parse("arrive 14:35", 14 * 60 + 35);
  check_parse("ETA 7:45 PM", 19 * 60 + 45);

  printf("eta_parse_clock_minutes — rejected\n");
  check_parse("", -1);
  check_parse("Turn right", -1);
  check_parse("500 m", -1);       // digits, but no clock time
  check_parse("in 500 m", -1);    // digits mid-string, still no clock time
  check_parse("25:00", -1);       // impossible hour — must NOT match the "5:00" inside it
  check_parse("12:75", -1);       // impossible minute
  check_parse("1234:56", -1);     // too many hour digits
  check_parse("14:3", -1);        // truncated minute
  check_int("parse(NULL)", eta_parse_clock_minutes(NULL), -1);

  printf("eta_minutes_until\n");
  check_int("12:35 -> 13:00", eta_minutes_until(13 * 60, 12 * 60 + 35), 25);
  check_int("same minute", eta_minutes_until(12 * 60, 12 * 60), 0);
  check_int("one minute out", eta_minutes_until(12 * 60 + 1, 12 * 60), 1);
  // Arrival after midnight must read as minutes away, not a negative or a whole day.
  check_int("23:50 -> 00:20 (wraps)", eta_minutes_until(20, 23 * 60 + 50), 30);
  check_int("23:59 -> 00:00 (wraps)", eta_minutes_until(0, 23 * 60 + 59), 1);
  check_int("longest wrap", eta_minutes_until(12 * 60 + 34, 12 * 60 + 35), 24 * 60 - 1);

  if (failures != 0) {
    printf("\n%d assertion(s) FAILED\n", failures);
    return 1;
  }
  printf("\nAll eta_text assertions passed.\n");
  return 0;
}
