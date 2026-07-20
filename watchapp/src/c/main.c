// PebbleNTN watchapp renderer (M7, M12 appearance).
//
// Responsibilities (spec/200-architecture/Pebble.md, REQ-WATCH-001..011):
//   - open AppMessage and send WATCH_READY with protocol + app version;
//   - render the current normalized navigation state (maneuver bitmap, distance, ETA, road text);
//   - show explicit connection/stale/no-navigation/compatibility states;
//   - vibrate on maneuver change and enable backlight when the phone requests it;
//   - return to the watchface on navigation stop when commanded;
//   - offer on-watch appearance settings (theme, units, ETA display) via SELECT.
//
// Layout: the maneuver arrow and the distance sit side by side in a coloured panel, so the road
// name gets the whole lower half and rarely has to be truncated; its font shrinks to fit.
//
// The watch never parses Android notification text (REQ-WATCH-001); it only decodes the AppMessage
// tuples defined by the generated, read-only protocol header (AGENTS.md rule 5).

#include <pebble.h>

#include "eta_text.h"
#include "generated/protocol.h"
#include "settings_window.h"
#include "theme.h"

_Static_assert(PBNTN_PROTOCOL_MAJOR == 1, "unexpected protocol major");

#define APP_VERSION_STR "0.0.9"
#define PRIMARY_TEXT_MAX 64
#define SECONDARY_TEXT_MAX 24

static Window *s_window;
static Layer *s_panel_layer;   // maneuver + distance
static Layer *s_strip_layer;   // ETA + stale marker
static Layer *s_road_layer;    // road name
static Layer *s_message_layer; // connecting / no navigation / arrived

static GBitmap *s_maneuver_bitmaps[12];

// Current normalized state, as sent by the phone.
static int s_maneuver = PBNTN_MANEUVER_UNKNOWN;
static int32_t s_distance_meters = -1;
static int32_t s_flags = 0;
static int s_last_maneuver = -1;
static bool s_showing_message = true;
// Arrival time as a Unix epoch second, or 0 when the phone did not send one. Used to render a live
// "time to arrival" countdown when that ETA display mode is selected (REQ-WATCH-014).
static int32_t s_eta_epoch = 0;

static char s_primary_buf[PRIMARY_TEXT_MAX + 1];
static char s_secondary_buf[SECONDARY_TEXT_MAX + 1];
static char s_message_buf[32];

// --- Maneuver bitmaps -------------------------------------------------------------------------

// Resource id for a maneuver within the selected glyph pack (REQ-WATCH-012). The pack only chooses
// which bundled bitmap is drawn; the maneuver code from the phone is unchanged.
static uint32_t maneuver_resource_id(int maneuver, GlyphPack pack) {
  switch (pack) {
    case GLYPH_PACK_BOLD:
      switch (maneuver) {
        case PBNTN_MANEUVER_STRAIGHT: return RESOURCE_ID_MANEUVER_BOLD_STRAIGHT;
        case PBNTN_MANEUVER_SLIGHT_LEFT: return RESOURCE_ID_MANEUVER_BOLD_SLIGHT_LEFT;
        case PBNTN_MANEUVER_LEFT: return RESOURCE_ID_MANEUVER_BOLD_LEFT;
        case PBNTN_MANEUVER_SHARP_LEFT: return RESOURCE_ID_MANEUVER_BOLD_SHARP_LEFT;
        case PBNTN_MANEUVER_SLIGHT_RIGHT: return RESOURCE_ID_MANEUVER_BOLD_SLIGHT_RIGHT;
        case PBNTN_MANEUVER_RIGHT: return RESOURCE_ID_MANEUVER_BOLD_RIGHT;
        case PBNTN_MANEUVER_SHARP_RIGHT: return RESOURCE_ID_MANEUVER_BOLD_SHARP_RIGHT;
        case PBNTN_MANEUVER_UTURN_LEFT: return RESOURCE_ID_MANEUVER_BOLD_UTURN_LEFT;
        case PBNTN_MANEUVER_UTURN_RIGHT: return RESOURCE_ID_MANEUVER_BOLD_UTURN_RIGHT;
        case PBNTN_MANEUVER_ROUNDABOUT: return RESOURCE_ID_MANEUVER_BOLD_ROUNDABOUT;
        case PBNTN_MANEUVER_ARRIVE: return RESOURCE_ID_MANEUVER_BOLD_ARRIVE;
        default: return RESOURCE_ID_MANEUVER_BOLD_UNKNOWN;
      }
    case GLYPH_PACK_OUTLINE:
      switch (maneuver) {
        case PBNTN_MANEUVER_STRAIGHT: return RESOURCE_ID_MANEUVER_OUTLINE_STRAIGHT;
        case PBNTN_MANEUVER_SLIGHT_LEFT: return RESOURCE_ID_MANEUVER_OUTLINE_SLIGHT_LEFT;
        case PBNTN_MANEUVER_LEFT: return RESOURCE_ID_MANEUVER_OUTLINE_LEFT;
        case PBNTN_MANEUVER_SHARP_LEFT: return RESOURCE_ID_MANEUVER_OUTLINE_SHARP_LEFT;
        case PBNTN_MANEUVER_SLIGHT_RIGHT: return RESOURCE_ID_MANEUVER_OUTLINE_SLIGHT_RIGHT;
        case PBNTN_MANEUVER_RIGHT: return RESOURCE_ID_MANEUVER_OUTLINE_RIGHT;
        case PBNTN_MANEUVER_SHARP_RIGHT: return RESOURCE_ID_MANEUVER_OUTLINE_SHARP_RIGHT;
        case PBNTN_MANEUVER_UTURN_LEFT: return RESOURCE_ID_MANEUVER_OUTLINE_UTURN_LEFT;
        case PBNTN_MANEUVER_UTURN_RIGHT: return RESOURCE_ID_MANEUVER_OUTLINE_UTURN_RIGHT;
        case PBNTN_MANEUVER_ROUNDABOUT: return RESOURCE_ID_MANEUVER_OUTLINE_ROUNDABOUT;
        case PBNTN_MANEUVER_ARRIVE: return RESOURCE_ID_MANEUVER_OUTLINE_ARRIVE;
        default: return RESOURCE_ID_MANEUVER_OUTLINE_UNKNOWN;
      }
    case GLYPH_PACK_CLASSIC:
    default:
      switch (maneuver) {
        case PBNTN_MANEUVER_STRAIGHT: return RESOURCE_ID_MANEUVER_STRAIGHT;
        case PBNTN_MANEUVER_SLIGHT_LEFT: return RESOURCE_ID_MANEUVER_SLIGHT_LEFT;
        case PBNTN_MANEUVER_LEFT: return RESOURCE_ID_MANEUVER_LEFT;
        case PBNTN_MANEUVER_SHARP_LEFT: return RESOURCE_ID_MANEUVER_SHARP_LEFT;
        case PBNTN_MANEUVER_SLIGHT_RIGHT: return RESOURCE_ID_MANEUVER_SLIGHT_RIGHT;
        case PBNTN_MANEUVER_RIGHT: return RESOURCE_ID_MANEUVER_RIGHT;
        case PBNTN_MANEUVER_SHARP_RIGHT: return RESOURCE_ID_MANEUVER_SHARP_RIGHT;
        case PBNTN_MANEUVER_UTURN_LEFT: return RESOURCE_ID_MANEUVER_UTURN_LEFT;
        case PBNTN_MANEUVER_UTURN_RIGHT: return RESOURCE_ID_MANEUVER_UTURN_RIGHT;
        case PBNTN_MANEUVER_ROUNDABOUT: return RESOURCE_ID_MANEUVER_ROUNDABOUT;
        case PBNTN_MANEUVER_ARRIVE: return RESOURCE_ID_MANEUVER_ARRIVE;
        default: return RESOURCE_ID_MANEUVER_UNKNOWN;
      }
  }
}

// The maneuver bitmaps are cached; the cache belongs to one pack. When the user switches packs the
// whole cache is dropped so the next draw loads the new pack's bitmaps.
static GlyphPack s_cached_pack = GLYPH_PACK_CLASSIC;

static GBitmap *maneuver_bitmap(int maneuver) {
  const GlyphPack pack = settings_glyph_pack();
  if (pack != s_cached_pack) {
    for (int i = 0; i < 12; i++) {
      if (s_maneuver_bitmaps[i] != NULL) {
        gbitmap_destroy(s_maneuver_bitmaps[i]);
        s_maneuver_bitmaps[i] = NULL;
      }
    }
    s_cached_pack = pack;
  }
  if (maneuver < 0 || maneuver > PBNTN_MANEUVER_ARRIVE) {
    maneuver = PBNTN_MANEUVER_UNKNOWN;
  }
  if (s_maneuver_bitmaps[maneuver] == NULL) {
    s_maneuver_bitmaps[maneuver] = gbitmap_create_with_resource(maneuver_resource_id(maneuver, pack));
  }
  return s_maneuver_bitmaps[maneuver];
}

// The glyphs ship as black-on-white images, built as 1-bit palettised bitmaps on every platform
// (see watchapp/package.json). Recolouring the palette is what lets one asset serve every theme:
// the glyph takes the panel's foreground colour and its background becomes transparent, so the
// panel shows through. This works on black-and-white watches too — there the palette entries are
// simply black and white, which is exactly what a Pebble 2 Duo needs.
static GCompOp prepare_glyph(GBitmap *bmp, Theme theme) {
  GColor *palette = gbitmap_get_palette(bmp);
  if (palette != NULL && gbitmap_get_format(bmp) == GBitmapFormat1BitPalette) {
    palette[0] = theme.panel_fg;  // set pixels: the glyph
    palette[1] = GColorClear;     // unset pixels: let the panel show through
    return GCompOpSet;
  }
  // No palette (unexpected): fall back to inverting the image on a dark panel.
  return gcolor_equal(theme.panel_bg, GColorBlack) ? GCompOpAssignInverted : GCompOpAssign;
}

// --- Round-display geometry --------------------------------------------------------------------

static int16_t s_screen_h;  // set in window_load; needed to reason about the round bezel

#ifdef PBL_ROUND
// Horizontal inset that keeps every row of the band [top, bottom) inside the round display, plus
// `margin` px of breathing room. Rows are chords of the circle, so the narrowest row in the band is
// whichever end sits further from the vertical centre; insetting to that keeps the whole band clear
// of the bezel. `top`/`bottom` are screen coordinates, not layer-relative ones.
//
// Without this the rectangular layout is drawn as-is on chalk and the bezel eats the corners: the
// distance, the maneuver arrow and the road name all lose their outer edges.
static int16_t round_inset(int16_t top, int16_t bottom, int16_t margin) {
  const int32_t r = s_screen_h / 2;
  const int32_t dy_top = r - top;
  const int32_t dy_bottom = bottom - r;
  const int32_t dy = (dy_top > dy_bottom) ? dy_top : dy_bottom;
  if (dy <= 0) {
    return margin;
  }
  if (dy >= r) {
    return (int16_t)r;
  }
  // half_width = sqrt(r^2 - dy^2), by integer search — this runs once per layer redraw.
  const int32_t target = r * r - dy * dy;
  int32_t half = 0;
  while ((half + 1) * (half + 1) <= target) {
    half++;
  }
  return (int16_t)(r - half + margin);
}
#endif

// --- Formatting -------------------------------------------------------------------------------

// Split the distance into a big number and a small unit, as on the reference layout ("0.3" + "mi").
static void format_distance(int32_t meters, char *value, size_t value_len, char *unit, size_t unit_len) {
  value[0] = '\0';
  unit[0] = '\0';
  if (meters < 0) {
    return;
  }
  if (settings_units() == UNITS_IMPERIAL) {
    const int32_t feet = (meters * 3281) / 1000;
    if (feet < 1000) {
      snprintf(value, value_len, "%d", (int)feet);
      snprintf(unit, unit_len, "ft");
    } else {
      const int32_t tenths = (meters * 10) / 1609;
      snprintf(value, value_len, "%d.%d", (int)(tenths / 10), (int)(tenths % 10));
      snprintf(unit, unit_len, "mi");
    }
  } else if (meters < 1000) {
    snprintf(value, value_len, "%d", (int)meters);
    snprintf(unit, unit_len, "m");
  } else {
    snprintf(value, value_len, "%d.%d", (int)(meters / 1000), (int)((meters % 1000) / 100));
    snprintf(unit, unit_len, "km");
  }
}

// Largest font from `candidates` whose text still fits the box. Neither the road name nor the
// distance has a bounded width, so the text drives the font instead of being truncated.
static GFont fit_font(const char *text, GRect box, GTextOverflowMode overflow,
                      const char *const *candidates, size_t count) {
  for (size_t i = 0; i < count; i++) {
    GFont font = fonts_get_system_font(candidates[i]);
    const GSize size = graphics_text_layout_get_content_size(text, font, box, overflow, GTextAlignmentLeft);
    if (size.h <= box.size.h && size.w <= box.size.w) {
      return font;
    }
  }
  return fonts_get_system_font(candidates[count - 1]);
}

// --- Layer rendering --------------------------------------------------------------------------

static void panel_update_proc(Layer *layer, GContext *ctx) {
  const Theme theme = theme_current();
  const GRect bounds = layer_get_bounds(layer);
  const int16_t w = bounds.size.w;
  const int16_t ph = bounds.size.h;
  const bool large = w >= 200;

  graphics_context_set_fill_color(ctx, theme.panel_bg);
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);

  GBitmap *bmp = maneuver_bitmap(s_maneuver);

  // The arrow and the distance share one horizontal band. On a rectangular watch that band is the
  // whole panel. On a round one the panel's top corners are outside the bezel, so the band is
  // pushed to the bottom of the panel — where the display is at its widest — and inset to the
  // circle. `content_*` is in layer coordinates; `band_*` is the same band on screen.
#ifdef PBL_ROUND
  const int16_t content_h = (bmp != NULL) ? gbitmap_get_bounds(bmp).size.h : 48;
  const int16_t content_y = ph - content_h - 6;
  const int16_t inset = round_inset(content_y, content_y + content_h, 4);
#else
  const int16_t content_h = ph;
  const int16_t content_y = 0;
  const int16_t inset = 0;
#endif

  // The maneuver glyph takes a third of the width in one corner of the band; the distance fills the
  // other two thirds (which is what lets "12.3" stay large). Which corner the arrow lives in is a
  // user setting (REQ-WATCH-011); the distance always sits opposite it, hugging the outer edge.
  const bool arrow_left = settings_arrow_left();
  const int16_t cw = w - 2 * inset;
  const int16_t column = cw / 3;
  const int16_t glyph_x = inset + (arrow_left ? 0 : (cw - column));
  const int16_t dist_x = inset + (arrow_left ? column : 0);
  const int16_t dist_w = cw - column;
  const GTextAlignment dist_align = arrow_left ? GTextAlignmentRight : GTextAlignmentLeft;

  // graphics_draw_bitmap_in_rect crops (and tiles) rather than scales, so the glyph is drawn at its
  // own size, centred in its column — anything else silently cuts the arrow in half.
  if (bmp != NULL) {
    const GSize glyph_size = gbitmap_get_bounds(bmp).size;
    // A glyph wider than its column would spill past the inset (and, on round, into the bezel).
    int16_t gx = glyph_x + (column - glyph_size.w) / 2;
    if (gx < inset) {
      gx = inset;
    } else if (gx + glyph_size.w > w - inset) {
      gx = w - inset - glyph_size.w;
    }
    const GRect glyph_rect = GRect(gx, content_y + (content_h - glyph_size.h) / 2,
                                   glyph_size.w, glyph_size.h);
    graphics_context_set_compositing_mode(ctx, prepare_glyph(bmp, theme));
    graphics_draw_bitmap_in_rect(ctx, bmp, glyph_rect);
    graphics_context_set_compositing_mode(ctx, GCompOpAssign);
  }

  char value[12];
  char unit[4];
  format_distance(s_distance_meters, value, sizeof(value), unit, sizeof(unit));
  if (value[0] == '\0') {
    return;
  }

  static const char *const kValueFonts[] = {
    FONT_KEY_BITHAM_42_BOLD,
    FONT_KEY_BITHAM_30_BLACK,
    FONT_KEY_GOTHIC_28_BOLD,
    FONT_KEY_GOTHIC_24_BOLD,
  };
  const GRect fit_box = GRect(dist_x + 6, content_y, dist_w - 12, content_h);
  GFont value_font = fit_font(value, fit_box, GTextOverflowModeFill, kValueFonts, ARRAY_LENGTH(kValueFonts));
  // Unit is bigger than before (was GOTHIC_18) and steps up again on the large display.
  GFont unit_font = fonts_get_system_font(large ? FONT_KEY_GOTHIC_28_BOLD : FONT_KEY_GOTHIC_24_BOLD);

  const int16_t value_h = graphics_text_layout_get_content_size(
      value, value_font, fit_box, GTextOverflowModeFill, dist_align).h;
  const int16_t unit_h = graphics_text_layout_get_content_size(
      unit, unit_font, fit_box, GTextOverflowModeFill, dist_align).h;

  // Stack the number and its unit as one block and centre it vertically so the number lines up with
  // the arrow, with no wasted gap. Both fonts report a box taller than the visible glyphs (roughly a
  // third is padding); subtracting that padding lets us butt the unit right under the number and
  // centre the visible block rather than the padded boxes.
  const int16_t num_pad = value_h / 3;
  const int16_t unit_pad = unit_h / 3;
  const int16_t num_visible = value_h - num_pad;
  const int16_t unit_visible = unit_h - unit_pad;
  // Centre the number+unit block, nudged so the number's own middle lands on the arrow line (the
  // unit hangs below it, filling what used to be wasted space). +4 accounts for the number sitting a
  // touch above the block centre.
  const int16_t block_top = content_y + (content_h - (num_visible + unit_visible)) / 2 + 4;
  const int16_t value_y = block_top - num_pad / 2;
  const int16_t unit_y = block_top + num_visible - unit_pad / 2;

  graphics_context_set_text_color(ctx, theme.panel_fg);
  graphics_draw_text(ctx, value, value_font, GRect(dist_x + 6, value_y, dist_w - 12, value_h),
                     GTextOverflowModeFill, dist_align, NULL);
  graphics_draw_text(ctx, unit, unit_font, GRect(dist_x + 6, unit_y, dist_w - 12, unit_h),
                     GTextOverflowModeFill, dist_align, NULL);
}

// The strip carries the context the panel has no room for: the current time on the left (so the
// watch stays a watch while navigating) and the arrival time on the right. A stale state — the
// phone has stopped updating us — replaces the clock, because that is the one thing the driver
// must not miss.
// Width of `text` in `font`, for laying strip items out next to each other.
static int16_t strip_text_width(const char *text, GFont font) {
  return graphics_text_layout_get_content_size(
             text, font, GRect(0, 0, 300, 40), GTextOverflowModeFill, GTextAlignmentLeft)
      .w;
}

// Draw `text` vertically centred within a `strip_h`-tall strip, in the horizontal span [x, x+w].
// Centring every item this way keeps the clock, the "ETA" label and the time on one line even
// though they are different font sizes.
//
// Pebble reports a text box that is taller than the visible glyphs, with most of the slack *above*
// the caps, so naive box-centring leaves the letters sitting low with a big gap on top. `top_trim`
// pulls the glyphs back up so the middle of the letters lands on the middle of the strip; it scales
// with the font because the slack grows with size.
static void draw_strip_text(GContext *ctx, const char *text, GFont font, int16_t x, int16_t w,
                            int16_t strip_h, GTextAlignment align, int16_t top_trim) {
  const GRect box = GRect(x, 0, w, strip_h);
  const GSize size = graphics_text_layout_get_content_size(text, font, box, GTextOverflowModeFill, align);
  const int16_t y = (strip_h - size.h) / 2 - top_trim;
  graphics_draw_text(ctx, text, font, GRect(x, y, w, size.h + top_trim),
                     GTextOverflowModeFill, align, NULL);
}

// Build the strip's right-hand arrival readout for the current display mode (REQ-WATCH-014). Writes
// the value into `out`, points `*label` at its small leading tag, and returns false when there is
// nothing to show. ARRIVAL mode (the default) shows the phone's arrival-time string as "ETA 17:45";
// DURATION mode shows the time left until arrival as "IN 0:25", computed from the arrival epoch and
// recomputed each minute by the strip's redraw. DURATION falls back to the arrival-time string when
// the phone sent no arrival epoch (a rule that extracts no ETA), so the strip is never left blank.
static bool format_eta_readout(char *out, size_t out_len, const char **label) {
  if (settings_eta_mode() == ETA_MODE_DURATION) {
    int32_t total_min = -1;
    if (s_eta_epoch > 0) {
      int32_t remaining = s_eta_epoch - (int32_t)time(NULL);
      if (remaining < 0) {
        remaining = 0;
      }
      // Round up to the next whole minute so "0:00" appears only once arrival is actually reached,
      // never while a fraction of the final minute is still left.
      total_min = (remaining + 59) / 60;
    } else {
      // No epoch — the bundled ruleset extracts none, so this is the common case. Derive the
      // countdown from the arrival-time string the phone always sends for display, by comparing it
      // against the watch's own clock (both are wall-clock time in the phone's timezone).
      const int target = eta_parse_clock_minutes(s_secondary_buf);
      if (target >= 0) {
        const time_t now = time(NULL);
        const struct tm *local = localtime(&now);
        total_min = eta_minutes_until(target, local->tm_hour * 60 + local->tm_min);
      }
    }
    if (total_min >= 0) {
      snprintf(out, out_len, "%d:%02d", (int)(total_min / 60), (int)(total_min % 60));
      *label = "IN";
      return true;
    }
  }
  if (s_secondary_buf[0] != '\0') {
    snprintf(out, out_len, "%s", s_secondary_buf);
    *label = "ETA";
    return true;
  }
  return false;
}

// Per-font top trims (px) that pull the glyphs up onto the strip's centre line. Tuned in the
// emulator; the slack above the caps grows with the font size.
static void strip_update_proc(Layer *layer, GContext *ctx) {
  const Theme theme = theme_current();
  const GRect bounds = layer_get_bounds(layer);
  const int16_t w = bounds.size.w;
  const int16_t h = bounds.size.h;

  graphics_context_set_fill_color(ctx, theme.strip_bg);
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);
  graphics_context_set_text_color(ctx, theme.strip_fg);

  // The clock and the arrival time are the same (large) size; the "ETA" label is smaller. Fixed
  // sizes (not per-item auto-fit) keep the two HH:MM readouts from rendering at different sizes,
  // which is what made them look misaligned. On Pebble Time 2 (emery) everything steps up a size.
  const bool large = w >= 200;
  const GFont clock_font = fonts_get_system_font(large ? FONT_KEY_GOTHIC_28_BOLD : FONT_KEY_GOTHIC_24_BOLD);
  const GFont time_font = clock_font;
  const GFont label_font = fonts_get_system_font(large ? FONT_KEY_GOTHIC_18_BOLD : FONT_KEY_GOTHIC_14_BOLD);
  const int16_t hhmm_trim = large ? 6 : 5;   // pull the glyphs onto the strip's centre line
  const int16_t label_trim = large ? 4 : 3;

  // The strip straddles the widest part of a round display, so it needs only a small inset — but
  // its lower corners still fall outside the bezel without one.
#ifdef PBL_ROUND
  const GRect frame = layer_get_frame(layer);
  const int16_t pad = round_inset(frame.origin.y, frame.origin.y + h, 4);
#else
  const int16_t pad = 6;
#endif

  // Left: clock, or STALE when the phone has stopped updating us.
  char clock[8];
  const char *left_text;
  if (s_flags & PBNTN_FLAG_STATE_IS_STALE_MASK) {
    left_text = "STALE";
  } else {
    const time_t now = time(NULL);
    strftime(clock, sizeof(clock), clock_is_24h_style() ? "%H:%M" : "%I:%M", localtime(&now));
    left_text = clock;
  }
  draw_strip_text(ctx, left_text, clock_font, pad, w / 2, h, GTextAlignmentLeft, hhmm_trim);

  // Right: the arrival estimate, large and right-aligned, with a small label just before it. Whether
  // this is the arrival time ("ETA 17:45") or the remaining duration ("IN 0:25") is a user setting.
  char eta_text[SECONDARY_TEXT_MAX + 1];
  const char *eta_label = "ETA";
  if (format_eta_readout(eta_text, sizeof(eta_text), &eta_label)) {
    const int16_t time_w = strip_text_width(eta_text, time_font);
    draw_strip_text(ctx, eta_text, time_font, w / 2, w / 2 - pad, h, GTextAlignmentRight, hhmm_trim);
    const int16_t label_right = w - pad - time_w - 4;  // 4px gap before the time
    draw_strip_text(ctx, eta_label, label_font, pad, label_right - pad, h, GTextAlignmentRight, label_trim);
  }
}

static void road_update_proc(Layer *layer, GContext *ctx) {
  const Theme theme = theme_current();
  const GRect bounds = layer_get_bounds(layer);

  graphics_context_set_fill_color(ctx, theme.road_bg);
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);
  if (s_primary_buf[0] == '\0') {
    return;
  }

  static const char *const kRoadFonts[] = {
    FONT_KEY_GOTHIC_28_BOLD,
    FONT_KEY_GOTHIC_24_BOLD,
    FONT_KEY_GOTHIC_18_BOLD,
    FONT_KEY_GOTHIC_14,
  };
  // On a round display the road area is the bottom cap of the circle: it narrows fast, so only its
  // first line is usable. Inset to that line, centre the text, and cap the box height so fit_font
  // picks a size that stays on one line instead of wrapping into the bezel.
#ifdef PBL_ROUND
  const GRect frame = layer_get_frame(layer);
  const int16_t line_h = 30;
  const int16_t road_inset = round_inset(frame.origin.y, frame.origin.y + line_h, 4);
  const GRect box = GRect(road_inset, 2, bounds.size.w - 2 * road_inset, line_h);
  const GTextAlignment align = GTextAlignmentCenter;
#else
  const GRect box = GRect(4, 0, bounds.size.w - 8, bounds.size.h);
  const GTextAlignment align = GTextAlignmentLeft;
#endif
  GFont font = fit_font(s_primary_buf, box, GTextOverflowModeWordWrap, kRoadFonts, ARRAY_LENGTH(kRoadFonts));

  graphics_context_set_text_color(ctx, theme.road_fg);
  graphics_draw_text(ctx, s_primary_buf, font, box, GTextOverflowModeWordWrap, align, NULL);
}

static void message_update_proc(Layer *layer, GContext *ctx) {
  const Theme theme = theme_current();
  const GRect bounds = layer_get_bounds(layer);

  graphics_context_set_fill_color(ctx, theme.road_bg);
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);
  graphics_context_set_text_color(ctx, theme.road_fg);
  graphics_draw_text(ctx, s_message_buf, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD),
                     GRect(4, bounds.size.h / 2 - 30, bounds.size.w - 8, 60),
                     GTextOverflowModeWordWrap, GTextAlignmentCenter, NULL);
}

// Show either the navigation layers or the full-screen message layer, never both.
static void set_message_mode(bool message) {
  s_showing_message = message;
  layer_set_hidden(s_panel_layer, message);
  layer_set_hidden(s_strip_layer, message);
  layer_set_hidden(s_road_layer, message);
  layer_set_hidden(s_message_layer, !message);
}

static void redraw_all(void) {
  layer_mark_dirty(s_panel_layer);
  layer_mark_dirty(s_strip_layer);
  layer_mark_dirty(s_road_layer);
  layer_mark_dirty(s_message_layer);
}

// Connecting / no navigation / arrived / incompatible: one large centred line, no maneuver icon
// (an icon here would be read as a maneuver).
static void show_message(const char *message) {
  snprintf(s_message_buf, sizeof(s_message_buf), "%s", message);
  set_message_mode(true);
  redraw_all();
}

static void render_navigation(DictionaryIterator *iter) {
  Tuple *maneuver_t = dict_find(iter, PBNTN_KEY_MANEUVER);
  Tuple *distance_t = dict_find(iter, PBNTN_KEY_DISTANCE_METERS);
  Tuple *primary_t = dict_find(iter, PBNTN_KEY_PRIMARY_TEXT);
  Tuple *secondary_t = dict_find(iter, PBNTN_KEY_SECONDARY_TEXT);
  Tuple *eta_epoch_t = dict_find(iter, PBNTN_KEY_ETA_EPOCH_SECONDS);
  Tuple *flags_t = dict_find(iter, PBNTN_KEY_FLAGS);

  s_maneuver = maneuver_t ? maneuver_t->value->int32 : PBNTN_MANEUVER_UNKNOWN;
  s_distance_meters = distance_t ? distance_t->value->int32 : -1;
  // The phone omits the ETA epoch when a rule extracts no arrival time; 0 means "unknown".
  s_eta_epoch = eta_epoch_t ? eta_epoch_t->value->int32 : 0;
  s_flags = flags_t ? flags_t->value->int32 : 0;

  if (primary_t && primary_t->length > 0) {
    strncpy(s_primary_buf, primary_t->value->cstring, PRIMARY_TEXT_MAX);
    s_primary_buf[PRIMARY_TEXT_MAX] = '\0';
  } else {
    s_primary_buf[0] = '\0';
  }
  if (secondary_t && secondary_t->length > 0) {
    strncpy(s_secondary_buf, secondary_t->value->cstring, SECONDARY_TEXT_MAX);
    s_secondary_buf[SECONDARY_TEXT_MAX] = '\0';
  } else {
    s_secondary_buf[0] = '\0';
  }

  set_message_mode(false);
  redraw_all();

  // Vibrate only on a maneuver change, and only when requested (REQ-WATCH-009).
  if ((s_flags & PBNTN_FLAG_VIBRATE_ON_MANEUVER_CHANGE_MASK) && s_maneuver != s_last_maneuver) {
    vibes_short_pulse();
  }
  if (s_flags & PBNTN_FLAG_ACTIVATE_BACKLIGHT_MASK) {
    light_enable_interaction();
  }
  s_last_maneuver = s_maneuver;
}

static void handle_stopped(DictionaryIterator *iter) {
  Tuple *flags_t = dict_find(iter, PBNTN_KEY_FLAGS);
  int32_t flags = flags_t ? flags_t->value->int32 : 0;
  if (flags & PBNTN_FLAG_EXIT_TO_WATCHFACE_ON_STOP_MASK) {
    window_stack_pop_all(true); // return to the watchface (REQ-WATCH-007)
  } else {
    show_message("Arrived");
  }
}

// --- AppMessage -------------------------------------------------------------------------------

static void send_ready(void) {
  DictionaryIterator *out;
  if (app_message_outbox_begin(&out) != APP_MSG_OK) {
    return;
  }
  dict_write_int32(out, PBNTN_KEY_EVENT, PBNTN_EVENT_WATCH_READY);
  dict_write_int32(out, PBNTN_KEY_PROTOCOL_MAJOR, PBNTN_PROTOCOL_MAJOR);
  dict_write_int32(out, PBNTN_KEY_PROTOCOL_MINOR, PBNTN_PROTOCOL_MINOR);
  dict_write_cstring(out, PBNTN_KEY_APP_VERSION, APP_VERSION_STR);
  app_message_outbox_send();
}

static void inbox_received_handler(DictionaryIterator *iter, void *context) {
  Tuple *event_t = dict_find(iter, PBNTN_KEY_EVENT);
  if (!event_t) {
    return;
  }
  switch (event_t->value->int32) {
    case PBNTN_EVENT_NAVIGATION_UPDATE:
      render_navigation(iter);
      break;
    case PBNTN_EVENT_NAVIGATION_STOPPED:
      handle_stopped(iter);
      break;
    case PBNTN_EVENT_NO_ACTIVE_NAVIGATION:
      s_last_maneuver = -1;
      show_message("No navigation");
      break;
    case PBNTN_EVENT_PHONE_COMPATIBILITY_ERROR:
      show_message("Update phone app");
      break;
    default:
      break;
  }
}

// --- Window ------------------------------------------------------------------------------------

static void on_settings_changed(void) {
  window_set_background_color(s_window, theme_current().road_bg);
  redraw_all();
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  settings_window_push(on_settings_changed);
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
}

static void window_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  const GRect bounds = layer_get_bounds(root);
  const int16_t w = bounds.size.w;
  const int16_t h = bounds.size.h;

  // Pebble Time 2 (emery, 200x228) has far more screen than the 144-wide models, so the maneuver
  // panel and the clock strip both grow there.
  // Panel height hugs the tallest element it holds — the maneuver arrow (64px on emery, 48px
  // elsewhere) plus a little breathing room — rather than a fixed fraction of the screen. On the
  // Pebble Time 2 the old 104px panel left ~20px of dead space above and below the arrow; sizing to
  // the arrow trims that and hands the space to the road name below.
  const bool large = w >= 200;
  // Round watches get a taller panel: its arrow/distance band has to sit low enough to clear the
  // bezel (see panel_update_proc), so the panel needs the extra height above that band.
  const int16_t panel_h = large ? 80 : PBL_IF_ROUND_ELSE(92, 70);
  const int16_t strip_h = large ? 40 : 28;

  s_screen_h = h;

  s_panel_layer = layer_create(GRect(0, 0, w, panel_h));
  layer_set_update_proc(s_panel_layer, panel_update_proc);
  layer_add_child(root, s_panel_layer);

  s_strip_layer = layer_create(GRect(0, panel_h, w, strip_h));
  layer_set_update_proc(s_strip_layer, strip_update_proc);
  layer_add_child(root, s_strip_layer);

  s_road_layer = layer_create(GRect(0, panel_h + strip_h, w, h - panel_h - strip_h));
  layer_set_update_proc(s_road_layer, road_update_proc);
  layer_add_child(root, s_road_layer);

  s_message_layer = layer_create(bounds);
  layer_set_update_proc(s_message_layer, message_update_proc);
  layer_add_child(root, s_message_layer);

  window_set_background_color(window, theme_current().road_bg);
  show_message("Connecting");
}

static void window_unload(Window *window) {
  layer_destroy(s_panel_layer);
  layer_destroy(s_strip_layer);
  layer_destroy(s_road_layer);
  layer_destroy(s_message_layer);
}

// Keeps the clock in the status strip honest.
static void tick_handler(struct tm *tick_time, TimeUnits units_changed) {
  layer_mark_dirty(s_strip_layer);
}

static void init(void) {
  settings_load();

  app_message_register_inbox_received(inbox_received_handler);
  app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());

  s_window = window_create();
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = window_load,
    .unload = window_unload,
  });
  window_set_click_config_provider(s_window, click_config_provider);
  window_stack_push(s_window, true);

  tick_timer_service_subscribe(MINUTE_UNIT, tick_handler);

  // Handshake: tell the phone we are ready and our protocol/app version (REQ-WATCH-003).
  send_ready();
}

static void deinit(void) {
  tick_timer_service_unsubscribe();
  for (unsigned i = 0; i < ARRAY_LENGTH(s_maneuver_bitmaps); i++) {
    if (s_maneuver_bitmaps[i]) {
      gbitmap_destroy(s_maneuver_bitmaps[i]);
    }
  }
  window_destroy(s_window);
}

int main(void) {
  init();
  app_event_loop();
  deinit();
  return 0;
}
