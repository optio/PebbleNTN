#include "panel_layout.h"

PanelLayout panel_layout(int16_t band_x, int16_t band_w, int16_t glyph_w, int16_t text_w, bool arrow_left) {
  PanelLayout out;
  if (glyph_w <= 0 || text_w <= 0) {
    // Only one of them: centre it.
    const int16_t only = glyph_w > 0 ? glyph_w : text_w;
    const int16_t x = band_x + (only < band_w ? (band_w - only) / 2 : 0);
    out.glyph_x = x;
    out.text_x = x;
    return out;
  }
  // Three equal spaces: outer edge, between the two, other outer edge.
  int16_t space = (int16_t)((band_w - glyph_w - text_w) / 3);
  if (space < 0) {
    space = 0;
  }
  if (arrow_left) {
    out.glyph_x = band_x + space;
    out.text_x = out.glyph_x + glyph_w + space;
  } else {
    out.text_x = band_x + space;
    out.glyph_x = out.text_x + text_w + space;
  }
  return out;
}
