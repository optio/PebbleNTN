// Horizontal placement of the maneuver arrow and the distance in the navigation panel (#80).
//
// The arrow and the distance are placed as one group with even spacing: the same space at the outer
// edges and between them. Before, the arrow sat in a fixed third and the distance hugged the opposite
// edge, so a short distance ("80 m") left a wide gap in the middle, most visibly on the 144 px Pebble 2.
//
// Pure function with no Pebble dependencies, so it compiles on the host and is unit-tested there
// (watchapp/tests/test_panel_layout.c).

#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef struct {
  int16_t glyph_x;  // left edge of the arrow
  int16_t text_x;   // left edge of the distance text block
} PanelLayout;

// `band_x`/`band_w`: the horizontal band (after any round inset); `glyph_w`/`text_w`: the arrow's and
// the widest text line's widths, 0 when absent. `arrow_left` is the arrow-side setting (REQ-WATCH-011).
// Content wider than the band is clamped to it rather than spilled.
PanelLayout panel_layout(int16_t band_x, int16_t band_w, int16_t glyph_w, int16_t text_w, bool arrow_left);
