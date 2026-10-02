// Deriving a "time to arrival" countdown from the arrival-time string (REQ-WATCH-014).
//
// The phone sends the arrival estimate two ways: as `etaEpochSeconds` (exact, but only when a rule
// extracts it — the bundled Google Maps ruleset does not) and as the pre-formatted arrival-time
// string it always sends for display. When the epoch is absent the watch can still count down, by
// comparing that string against its own clock: both are wall-clock time in the same timezone,
// because the watch takes its time from the phone.
//
// These are pure functions with no Pebble dependencies, so they compile on the host and are
// unit-tested there (watchapp/tests/test_eta_text.c). Nothing here inspects notification text — it
// reads only the normalized field the phone chose to send (REQ-WATCH-001).

#pragma once

#include <stdbool.h>

// Minutes since midnight for the first wall-clock time found in `text`, or -1 when it holds none.
// Accepts "14:35", "2:35 PM" and "2:35pm"; a 12-hour suffix is applied when present.
int eta_parse_clock_minutes(const char *text);

// Whole minutes from `now_minutes` to `target_minutes` (both minutes since midnight). Wraps over
// midnight, so an arrival shortly after 00:00 reads as minutes away rather than a day.
int eta_minutes_until(int target_minutes, int now_minutes);

// The small tag before the arrival readout (REQ-WATCH-014, #13). The labelled style reads as a
// phrase, "ETA at 17:45" / "ETA in 0:25"; the compact style is "ETA 17:45" / "IN 0:25".
const char *eta_label(bool duration, bool labelled);

// The labelled tag split over two lines, for a strip too narrow for it on one line: "ETA" over
// "at" / "in". No wider than the compact tag.
void eta_label_lines(bool duration, const char **first, const char **second);
