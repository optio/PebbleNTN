// Labels of the When driving settings (REQ-WATCH-026, #132): the driving speed lists and the menu
// texts built from them ("Hide above 25 km/h", "Default (15 mph)").
//
// The speeds are stored as list indices, one list per unit: 10, 15, ... 60 km/h (11 values) and 5,
// 10, ... 40 mph (8 values). The unit in use is the Distance units setting; switching units switches
// lists and converts neither value. The phone mirrors this in WatchSettingsModel.kt (DrivingSpeeds).
//
// Pure functions with no Pebble dependencies, so they compile on the host and are unit-tested there
// (watchapp/tests/test_driving_label.c).

#pragma once

#include <stdbool.h>
#include <stddef.h>

#define DRIVING_METRIC_COUNT 11
#define DRIVING_IMPERIAL_COUNT 8
#define DRIVING_METRIC_DEFAULT 3    // 25 km/h
#define DRIVING_IMPERIAL_DEFAULT 2  // 15 mph

// Longest label: "Hide above 60 km/h" or "Default (25 km/h)", plus the NUL.
#define DRIVING_LABEL_LEN 20

// The default list index of a unit: 25 km/h, or 15 mph when `imperial`.
int driving_default_index(bool imperial);

// The speed list index `index` stands for, in km/h or (when `imperial`) mph. An index outside the
// unit's list reads as its default.
int driving_speed(int index, bool imperial);

// "<prefix><speed> km/h<suffix>" (or mph) into `buf`, always NUL-terminated:
// ("", "") -> "25 km/h", ("Above ", "") -> "Above 25 km/h", ("Default (", ")") -> "Default (25 km/h)".
void driving_label(const char *prefix, const char *suffix, int index, bool imperial, char *buf,
                   size_t len);

// The main Settings row's value: "Show directions", or "Hide above 25 km/h" (#132 FR2).
void driving_summary(bool hide, int index, bool imperial, char *buf, size_t len);
