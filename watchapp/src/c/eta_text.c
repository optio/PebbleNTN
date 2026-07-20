#include "eta_text.h"

#include <stddef.h>  // NULL — pebble.h provides it on the watch, but the host build is standalone

#define MINUTES_PER_DAY (24 * 60)

static int is_digit(char c) { return c >= '0' && c <= '9'; }

int eta_parse_clock_minutes(const char *text) {
  if (text == NULL) {
    return -1;
  }
  // Scan for the first "H:MM" / "HH:MM". Text that is not a clock time is skipped, so a string like
  // "arrive 14:35" still yields a time — but the whole digit run is skipped as a unit. Scanning one
  // character at a time would let "25:00" match the "5:00" inside it and report an arrival that was
  // never written, so an out-of-range candidate is rejected outright instead.
  for (const char *p = text; *p != '\0';) {
    if (!is_digit(*p)) {
      p++;
      continue;
    }
    const char *digits = p;
    int digit_count = 0;
    while (is_digit(*p)) {
      p++;
      digit_count++;
    }
    if (digit_count > 2 || *p != ':' || !is_digit(p[1]) || !is_digit(p[2])) {
      continue;  // not a clock time; `p` already sits past the digit run
    }

    int hour = digits[0] - '0';
    if (digit_count == 2) {
      hour = hour * 10 + (digits[1] - '0');
    }
    const int minute = (p[1] - '0') * 10 + (p[2] - '0');
    if (hour > 23 || minute > 59) {
      return -1;  // shaped like a time but out of range — not something to guess at
    }

    const char *suffix = p + 3;
    while (*suffix == ' ') {
      suffix++;
    }
    // A 12-hour suffix rebases the hour; 12 AM is midnight and 12 PM is noon. The result cannot
    // exceed 23:59, since the largest rebase is 11 PM.
    if (*suffix == 'A' || *suffix == 'a') {
      if (hour == 12) {
        hour = 0;
      }
    } else if (*suffix == 'P' || *suffix == 'p') {
      if (hour < 12) {
        hour += 12;
      }
    }
    return hour * 60 + minute;
  }
  return -1;
}

int eta_minutes_until(int target_minutes, int now_minutes) {
  int delta = target_minutes - now_minutes;
  if (delta < 0) {
    delta += MINUTES_PER_DAY;  // arrival is on the other side of midnight
  }
  return delta;
}
