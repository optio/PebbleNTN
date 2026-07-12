// PebbleNTN watchapp renderer (M7).
//
// Responsibilities (spec/200-architecture/Pebble.md, REQ-WATCH-001..010):
//   - open AppMessage and send WATCH_READY with protocol + app version;
//   - render the current normalized navigation state (maneuver bitmap, distance, road text);
//   - show explicit connection/stale/no-navigation/compatibility states;
//   - vibrate on maneuver change and enable backlight when the phone requests it;
//   - return to the watchface on navigation stop when commanded.
//
// The watch never parses Android notification text (REQ-WATCH-001); it only decodes the AppMessage
// tuples defined by the generated, read-only protocol header (AGENTS.md rule 5).

#include <pebble.h>

#include "generated/protocol.h"

_Static_assert(PBNTN_PROTOCOL_MAJOR == 1, "unexpected protocol major");

#define APP_VERSION_STR "0.0.1"
#define PRIMARY_TEXT_MAX 64

static Window *s_window;
static BitmapLayer *s_maneuver_layer;
static TextLayer *s_distance_layer;
static TextLayer *s_primary_layer;
static TextLayer *s_status_layer;

static GBitmap *s_maneuver_bitmaps[12];
static int s_last_maneuver = -1;

static char s_distance_buf[24];
static char s_primary_buf[PRIMARY_TEXT_MAX + 1];
static char s_status_buf[32];

// --- Maneuver bitmap lookup -------------------------------------------------------------------

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

// --- Rendering --------------------------------------------------------------------------------

static void set_distance_text(int32_t meters) {
  if (meters < 0) {
    s_distance_buf[0] = '\0';
  } else if (meters >= 1000) {
    snprintf(s_distance_buf, sizeof(s_distance_buf), "%d.%d km", (int)(meters / 1000), (int)((meters % 1000) / 100));
  } else {
    snprintf(s_distance_buf, sizeof(s_distance_buf), "%d m", (int)meters);
  }
  text_layer_set_text(s_distance_layer, s_distance_buf);
}

static void show_message(const char *message) {
  bitmap_layer_set_bitmap(s_maneuver_layer, maneuver_bitmap(PBNTN_MANEUVER_UNKNOWN));
  s_distance_buf[0] = '\0';
  text_layer_set_text(s_distance_layer, s_distance_buf);
  s_primary_buf[0] = '\0';
  text_layer_set_text(s_primary_layer, s_primary_buf);
  snprintf(s_status_buf, sizeof(s_status_buf), "%s", message);
  text_layer_set_text(s_status_layer, s_status_buf);
}

static void render_navigation(DictionaryIterator *iter) {
  Tuple *maneuver_t = dict_find(iter, PBNTN_KEY_MANEUVER);
  Tuple *distance_t = dict_find(iter, PBNTN_KEY_DISTANCE_METERS);
  Tuple *primary_t = dict_find(iter, PBNTN_KEY_PRIMARY_TEXT);
  Tuple *flags_t = dict_find(iter, PBNTN_KEY_FLAGS);

  int maneuver = maneuver_t ? maneuver_t->value->int32 : PBNTN_MANEUVER_UNKNOWN;
  int32_t flags = flags_t ? flags_t->value->int32 : 0;

  bitmap_layer_set_bitmap(s_maneuver_layer, maneuver_bitmap(maneuver));
  set_distance_text(distance_t ? distance_t->value->int32 : -1);

  if (primary_t && primary_t->length > 0) {
    strncpy(s_primary_buf, primary_t->value->cstring, PRIMARY_TEXT_MAX);
    s_primary_buf[PRIMARY_TEXT_MAX] = '\0';
  } else {
    s_primary_buf[0] = '\0';
  }
  text_layer_set_text(s_primary_layer, s_primary_buf);

  // Status line: stale marker or clear.
  if (flags & PBNTN_FLAG_STATE_IS_STALE_MASK) {
    snprintf(s_status_buf, sizeof(s_status_buf), "stale");
  } else {
    s_status_buf[0] = '\0';
  }
  text_layer_set_text(s_status_layer, s_status_buf);

  // Vibrate only on a maneuver change, and only when requested (REQ-WATCH-009).
  if ((flags & PBNTN_FLAG_VIBRATE_ON_MANEUVER_CHANGE_MASK) && maneuver != s_last_maneuver) {
    vibes_short_pulse();
  }
  if (flags & PBNTN_FLAG_ACTIVATE_BACKLIGHT_MASK) {
    light_enable_interaction();
  }
  s_last_maneuver = maneuver;
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

static void window_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(root);

  const int16_t bmp = 52;
  s_maneuver_layer = bitmap_layer_create(GRect((bounds.size.w - bmp) / 2, 6, bmp, bmp));
  bitmap_layer_set_compositing_mode(s_maneuver_layer, GCompOpSet);
  layer_add_child(root, bitmap_layer_get_layer(s_maneuver_layer));

  s_distance_layer = text_layer_create(GRect(0, 60, bounds.size.w, 34));
  text_layer_set_font(s_distance_layer, fonts_get_system_font(FONT_KEY_BITHAM_30_BLACK));
  text_layer_set_text_alignment(s_distance_layer, GTextAlignmentCenter);
  layer_add_child(root, text_layer_get_layer(s_distance_layer));

  s_primary_layer = text_layer_create(GRect(4, 98, bounds.size.w - 8, 44));
  text_layer_set_font(s_primary_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_text_alignment(s_primary_layer, GTextAlignmentCenter);
  text_layer_set_overflow_mode(s_primary_layer, GTextOverflowModeTrailingEllipsis);
  layer_add_child(root, text_layer_get_layer(s_primary_layer));

  s_status_layer = text_layer_create(GRect(0, bounds.size.h - 20, bounds.size.w, 18));
  text_layer_set_font(s_status_layer, fonts_get_system_font(FONT_KEY_GOTHIC_14));
  text_layer_set_text_alignment(s_status_layer, GTextAlignmentCenter);
  layer_add_child(root, text_layer_get_layer(s_status_layer));

  show_message("Connecting");
}

static void window_unload(Window *window) {
  bitmap_layer_destroy(s_maneuver_layer);
  text_layer_destroy(s_distance_layer);
  text_layer_destroy(s_primary_layer);
  text_layer_destroy(s_status_layer);
}

static void init(void) {
  app_message_register_inbox_received(inbox_received_handler);
  app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());

  s_window = window_create();
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = window_load,
    .unload = window_unload,
  });
  window_stack_push(s_window, true);

  // Handshake: tell the phone we are ready and our protocol/app version (REQ-WATCH-003).
  send_ready();
}

static void deinit(void) {
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
