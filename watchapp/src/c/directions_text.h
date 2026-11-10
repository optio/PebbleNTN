// The Directions text setting (REQ-WATCH-027, #134, protocol 1.5): which road line the phone sends.
//
// Full (the default) is the navigation app's instruction with the street, Compact only the street
// when the app gives one. The phone applies the choice when it builds the instruction, so the watch
// only stores the value, syncs it and shows it in Settings. The phone mirrors the values in
// WatchSettingsModel.kt (DirectionsText).
//
// Header only: a macro rather than a function, because a function costs aplite heap it does not
// have to spare (REQ-WATCH-025). Host-tested in watchapp/tests/test_settings_sync.c.

#pragma once

#define DIRECTIONS_TEXT_FULL 0
#define DIRECTIONS_TEXT_COMPACT 1
#define DIRECTIONS_TEXT_COUNT 2

// The Settings row's value: "Full" or "Compact". Any other value reads as Full, the default.
#define DIRECTIONS_TEXT_NAME(value) (((value) == DIRECTIONS_TEXT_COMPACT) ? "Compact" : "Full")
