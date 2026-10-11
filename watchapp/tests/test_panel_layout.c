// Host-compiled unit tests for the arrow/distance placement in the navigation panel (#80).
// Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/panel_layout.h"

#include <stdio.h>

static int failures;

static void check(const char *name, PanelLayout got, int want_glyph_x, int want_text_x) {
  if (got.glyph_x != want_glyph_x || got.text_x != want_text_x) {
    printf("  FAIL  %s: expected glyph %d text %d, got glyph %d text %d\n", name, want_glyph_x, want_text_x,
           got.glyph_x, got.text_x);
    failures++;
  } else {
    printf("  ok    %s: glyph %d text %d\n", name, got.glyph_x, got.text_x);
  }
}

int main(void) {
  printf("panel_layout\n");
  // Pebble 2 (144 px), a 48 px arrow and "80 m" (~45 px): 17 px at each edge and in between, where
  // the old layout left a gap of over 40 px in the middle.
  check("short distance, arrow left", panel_layout(0, 144, 48, 45, true), 17, 82);
  check("short distance, arrow right", panel_layout(0, 144, 48, 45, false), 79, 17);
  // A long value ("12.3" in the big font) uses most of the width; the spaces shrink evenly.
  check("long distance", panel_layout(0, 144, 48, 87, true), 3, 54);
  // Round watch: the band is inset; the layout stays inside it.
  check("inset band", panel_layout(10, 160, 48, 52, true), 30, 98);
  // Only the arrow (no distance) or only the distance (no arrow bitmap): centred.
  check("arrow only", panel_layout(0, 144, 48, 0, true), 48, 48);
  check("text only", panel_layout(0, 144, 0, 60, false), 42, 42);
  // Too wide: no negative spacing, nothing starts before the band.
  check("overfull", panel_layout(0, 100, 60, 60, true), 0, 60);
  check("overfull single", panel_layout(5, 40, 60, 0, true), 5, 5);

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  return 0;
}
