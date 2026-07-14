// PebbleNTN watchapp renderer (M7, M12 appearance).
//
// Responsibilities (spec/200-architecture/Pebble.md, REQ-WATCH-001..011):
//   - open AppMessage and send WATCH_READY with protocol + app version;
//   - render the current normalized navigation state (maneuver bitmap, distance, ETA, road text);
//   - show explicit connection/stale/no-navigation/compatibility states;
//   - vibrate on maneuver change and enable backlight when the phone requests it;
//   - return to the watchface on navigation stop when commanded;
//   - offer on-watch appearance settings (theme, units) via SELECT.
//
// Layout: the maneuver arrow and the distance sit side by side in a coloured panel, so the road
// name gets the whole lower half and rarely has to be truncated; its font shrinks to fit.
//
// The watch never parses Android notification text (REQ-WATCH-001); it only decodes the AppMessage
// tuples defined by the generated, read-only protocol header (AGENTS.md rule 5).

#include <pebble.h>

#include "generated/protocol.h"
#include "settings_window.h"
#include "theme.h"

_Static_assert(PBNTN_PROTOCOL_MAJOR == 1, "unexpected protocol major");

#define APP_VERSION_STR "0.0.2"
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

static char s_primary_buf[PRIMARY_TEXT_MAX + 1];
static char s_secondary_buf[SECONDARY_TEXT_MAX + 1];
static char s_message_buf[32];

// --- Maneuver bitmaps -------------------------------------------------------------------------

static uint32_t maneuver_resource_id(int maneuver) {
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

static GBitmap *maneuver_bitmap(int maneuver) {
  if (maneuver < 0 || maneuver > PBNTN_MANEUVER_ARRIVE) {
    maneuver = PBNTN_MANEUVER_UNKNOWN;
  }
  if (s_maneuver_bitmaps[maneuver] == NULL) {
    s_maneuver_bitmaps[maneuver] = gbitmap_create_with_resource(maneuver_resource_id(maneuver));
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

  graphics_context_set_fill_color(ctx, theme.panel_bg);
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);

  // Maneuver glyph on the right, distance on the left — side by side, so the road name below gets
  // the whole lower half of the screen. The glyph column is a third of the width; the distance gets
  // the remaining two thirds, which is what lets "12.3" stay large.
  //
  // graphics_draw_bitmap_in_rect crops (and tiles) rather than scales, so the glyph must be drawn
  // at its own size, centred in the column — anything else silently cuts the arrow in half.
  const int16_t column = bounds.size.w / 3;
  GBitmap *bmp = maneuver_bitmap(s_maneuver);
  if (bmp != NULL) {
    const GSize glyph_size = gbitmap_get_bounds(bmp).size;
    const GRect glyph_rect = GRect(
        bounds.size.w - column + (column - glyph_size.w) / 2,
        (bounds.size.h - glyph_size.h) / 2,
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
  const GRect value_box = GRect(4, 0, bounds.size.w - column - 8, bounds.size.h - 20);
  GFont value_font = fit_font(value, value_box, GTextOverflowModeFill, kValueFonts, ARRAY_LENGTH(kValueFonts));

  graphics_context_set_text_color(ctx, theme.panel_fg);
  graphics_draw_text(ctx, value, value_font, value_box, GTextOverflowModeFill, GTextAlignmentLeft, NULL);
  graphics_draw_text(ctx, unit, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD),
                     GRect(6, bounds.size.h - 24, bounds.size.w - column - 10, 22),
                     GTextOverflowModeFill, GTextAlignmentLeft, NULL);
}

// The strip carries the context the panel has no room for: the current time on the left (so the
// watch stays a watch while navigating) and the arrival time on the right. A stale state — the
// phone has stopped updating us — replaces the clock, because that is the one thing the driver
// must not miss.
static void strip_update_proc(Layer *layer, GContext *ctx) {
  const Theme theme = theme_current();
  const GRect bounds = layer_get_bounds(layer);

  graphics_context_set_fill_color(ctx, theme.strip_bg);
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);
  graphics_context_set_text_color(ctx, theme.strip_fg);

  GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD);
  const GRect left = GRect(6, -3, bounds.size.w / 2, bounds.size.h);
  const GRect right = GRect(bounds.size.w / 2, -3, bounds.size.w / 2 - 6, bounds.size.h);

  if (s_flags & PBNTN_FLAG_STATE_IS_STALE_MASK) {
    graphics_draw_text(ctx, "STALE", font, left, GTextOverflowModeFill, GTextAlignmentLeft, NULL);
  } else {
    char clock[8];
    const time_t now = time(NULL);
    strftime(clock, sizeof(clock), clock_is_24h_style() ? "%H:%M" : "%I:%M", localtime(&now));
    graphics_draw_text(ctx, clock, font, left, GTextOverflowModeFill, GTextAlignmentLeft, NULL);
  }

  if (s_secondary_buf[0] != '\0') {
    char eta[SECONDARY_TEXT_MAX + 8];
    snprintf(eta, sizeof(eta), "ETA %s", s_secondary_buf);
    graphics_draw_text(ctx, eta, font, right, GTextOverflowModeFill, GTextAlignmentRight, NULL);
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
  const GRect box = GRect(4, 0, bounds.size.w - 8, bounds.size.h);
  GFont font = fit_font(s_primary_buf, box, GTextOverflowModeWordWrap, kRoadFonts, ARRAY_LENGTH(kRoadFonts));

  graphics_context_set_text_color(ctx, theme.road_fg);
  graphics_draw_text(ctx, s_primary_buf, font, box, GTextOverflowModeWordWrap, GTextAlignmentLeft, NULL);
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
  Tuple *flags_t = dict_find(iter, PBNTN_KEY_FLAGS);

  s_maneuver = maneuver_t ? maneuver_t->value->int32 : PBNTN_MANEUVER_UNKNOWN;
  s_distance_meters = distance_t ? distance_t->value->int32 : -1;
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

  const int16_t panel_h = PBL_IF_ROUND_ELSE(78, 70);
  const int16_t strip_h = 22;

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
