#include "about_window.h"

#include "about_text.h"
#include "generated/protocol.h"

// Black on white whatever the theme: the install QR needs a white quiet zone for a phone camera to
// read it (as on the connection-hint screen, REQ-WATCH-017), and one look for the whole page reads
// better than a themed top half and a white bottom.

static Window *s_window;
static ScrollLayer *s_scroll;
static TextLayer *s_texts[4];
static BitmapLayer *s_qr_layer;
static GBitmap *s_qr;
static AboutInfo s_info;
static char s_details[80];

// Adds a centred, word-wrapped text block at `*y` and moves `*y` below it.
static TextLayer *add_text(int index, const char *text, const char *font_key, int16_t x,
                           int16_t w, int16_t *y) {
  const GFont font = fonts_get_system_font(font_key);
  const GSize size = graphics_text_layout_get_content_size(
      text, font, GRect(0, 0, w, 2000), GTextOverflowModeWordWrap, GTextAlignmentCenter);
  TextLayer *layer = text_layer_create(GRect(x, *y, w, size.h + 6));
  text_layer_set_text(layer, text);
  text_layer_set_font(layer, font);
  text_layer_set_text_alignment(layer, GTextAlignmentCenter);
  text_layer_set_overflow_mode(layer, GTextOverflowModeWordWrap);
  text_layer_set_background_color(layer, GColorClear);
  text_layer_set_text_color(layer, GColorBlack);
  scroll_layer_add_child(s_scroll, text_layer_get_layer(layer));
  s_texts[index] = layer;
  *y += size.h + 6;
  return layer;
}

static void about_load(Window *window) {
  window_set_background_color(window, GColorWhite);
  Layer *root = window_get_root_layer(window);
  const GRect bounds = layer_get_bounds(root);

  s_scroll = scroll_layer_create(bounds);
  scroll_layer_set_click_config_onto_window(s_scroll, window);
  scroll_layer_set_shadow_hidden(s_scroll, true);
  layer_add_child(root, scroll_layer_get_layer(s_scroll));

  // A round screen is narrower towards the top and bottom, so keep text off the bezel there.
  const int16_t inset = PBL_IF_ROUND_ELSE(22, 4);
  const int16_t w = bounds.size.w - 2 * inset;
  int16_t y = PBL_IF_ROUND_ELSE(16, 0);

  about_details(s_details, sizeof(s_details), s_info.app_version, PBNTN_PROTOCOL_MAJOR,
                PBNTN_PROTOCOL_MINOR,
                about_phone_status(connection_service_peek_pebble_app_connection(),
                                   s_info.heard_from_phone));
  add_text(0, "PebbleNTN", FONT_KEY_GOTHIC_24_BOLD, inset, w, &y);
  add_text(1, s_details, FONT_KEY_GOTHIC_18, inset, w, &y);
  y += 4;
  add_text(2, "Install phone app", FONT_KEY_GOTHIC_18_BOLD, inset, w, &y);

  s_qr = gbitmap_create_with_resource(RESOURCE_ID_CONNECT_QR);
  const GSize qr = s_qr ? gbitmap_get_bounds(s_qr).size : GSize(0, 0);
  s_qr_layer = bitmap_layer_create(GRect((bounds.size.w - qr.w) / 2, y, qr.w, qr.h));
  bitmap_layer_set_bitmap(s_qr_layer, s_qr);
  scroll_layer_add_child(s_scroll, bitmap_layer_get_layer(s_qr_layer));
  y += qr.h + 4;

  // For anyone who cannot scan: the same link as text.
  add_text(3, "github.com/optio/\nPebbleNTN/releases", FONT_KEY_GOTHIC_14, inset, w, &y);
  y += PBL_IF_ROUND_ELSE(30, 4);

  scroll_layer_set_content_size(s_scroll, GSize(bounds.size.w, y));
}

static void about_unload(Window *window) {
  for (unsigned i = 0; i < ARRAY_LENGTH(s_texts); i++) {
    text_layer_destroy(s_texts[i]);
    s_texts[i] = NULL;
  }
  bitmap_layer_destroy(s_qr_layer);
  if (s_qr != NULL) {
    gbitmap_destroy(s_qr);
    s_qr = NULL;
  }
  scroll_layer_destroy(s_scroll);
  window_destroy(s_window);
  s_window = NULL;
}

void about_window_push(const AboutInfo *info) {
  s_info = *info;
  s_window = window_create();
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = about_load,
    .unload = about_unload,
  });
  window_stack_push(s_window, true);
}
