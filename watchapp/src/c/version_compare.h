// The phone app update found by version comparison (REQ-WATCH-023, amended).
//
// Both apps share one version number, and the store updates the watchapp by itself while the phone
// app is updated by hand. So a watchapp newer than the phone app version the phone reports (key 11)
// means a newer phone app has been published, and the watch can offer the #94 update UI with no
// internet check. This module holds the strict version rule — the same as AppVersions.parseStrict
// on the phone — and the choice of the "effective update version", apart from main.c, which feeds
// it the stored versions and adds no persist key of its own.
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_version_compare.c).

#pragma once

#include <stdbool.h>

// Longest numeric part accepted, in digits: 9 always fits a uint32_t, so parsing never overflows.
#define VERSION_PART_DIGITS_MAX 9

// True when `v` is a parsable version: an optional leading 'v' or 'V', then one or more parts of
// 1 to VERSION_PART_DIGITS_MAX decimal digits separated by '.', and nothing else (no suffix, sign,
// space or empty part). NULL and "" are unparsable.
bool version_parse(const char *v);

// True only when both `a` and `b` parse and `a` is the higher version, compared part by part from
// the left with missing parts counting as 0 ("0.0.10" > "0.0.9", "1.2" equals "1.2.0").
bool version_newer(const char *a, const char *b);

// The update version the watch offers. `reported` is the version the phone reports with flag bit 4
// and key 14 ("" or NULL when none), `own` the watchapp's version, and `phone` the phone app version
// from key 11 ("" or NULL when none was ever heard). `own` counts only when version_newer(own,
// phone). Returns the higher of `reported` and a counting `own` — `reported` when the two cannot be
// compared — or "" when there is no update. The result points at one of the inputs or at "".
const char *version_effective_update(const char *reported, const char *own, const char *phone);
