// PebbleNTN watchapp — M0 scaffold.
//
// Responsibilities filled in later milestones (see spec/200-architecture/Pebble.md):
//   - M1/M7: open AppMessage, send WATCH_READY with protocol + app version, decode current state.
//   - M7:    bitmap maneuver renderer, distance/road layout, stale/no-navigation/compat states.
//
// This M0 version only proves the toolchain: it creates a window with a placeholder label and
// opens the AppMessage inbox/outbox so the handshake can be layered on without restructuring.
// Protocol key/event constants are intentionally NOT defined here; they are emitted into a
// read-only generated header in M1 (AGENTS.md rule 5) and included by the renderer/transport.

#include <pebble.h>

#include "generated/protocol.h"

// Compile-time sanity: the generated protocol header must define the handshake version.
_Static_assert(PBNTN_PROTOCOL_MAJOR == 1, "unexpected protocol major");

static Window *s_window;
static TextLayer *s_status_layer;

static void inbox_received_handler(DictionaryIterator *iter, void *context) {
  // M1/M7: dispatch normalized navigation state to the renderer.
}

static void window_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(root);

  s_status_layer = text_layer_create(GRect(0, bounds.size.h / 2 - 20, bounds.size.w, 40));
  text_layer_set_text(s_status_layer, "PebbleNTN");
  text_layer_set_text_alignment(s_status_layer, GTextAlignmentCenter);
  text_layer_set_font(s_status_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  layer_add_child(root, text_layer_get_layer(s_status_layer));
}

static void window_unload(Window *window) {
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
}

static void deinit(void) {
  window_destroy(s_window);
}

int main(void) {
  init();
  app_event_loop();
  deinit();
  return 0;
}
