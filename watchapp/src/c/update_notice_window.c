#include "update_notice_window.h"

#include "update_notice.h"

// How long the notice stays up without a button press (FR4.2).
#define UPDATE_NOTICE_TIMEOUT_MS 10000

static Window *s_window;
static TextLayer *s_text;
static AppTimer *s_timer;
static void (*s_on_close)(void);
static bool s_open;  // on the window stack (push until unload)

static void close_notice(void) {
  if (s_open) {
    window_stack_remove(s_window, true);
  }
}

static void timeout_callback(void *data) {
  s_timer = NULL;
  close_notice();
}

// Every button only closes the notice: the handlers are on this window, so the press is consumed
// here and never acts on the navigation screen behind it (FR4.2).
static void any_click_handler(ClickRecognizerRef recognizer, void *context) {
  close_notice();
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_UP, any_click_handler);
  window_single_click_subscribe(BUTTON_ID_SELECT, any_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, any_click_handler);
  window_single_click_subscribe(BUTTON_ID_BACK, any_click_handler);
}

// Largest font in which the notice fits `box`, so it reads at a glance on every screen size.
static GFont fit_font(const char *text, GRect box) {
  static const char *const kFonts[] = {
    FONT_KEY_GOTHIC_28_BOLD,
    FONT_KEY_GOTHIC_24_BOLD,
    FONT_KEY_GOTHIC_18_BOLD,
  };
  for (unsigned i = 0; i < ARRAY_LENGTH(kFonts); i++) {
    const GFont font = fonts_get_system_font(kFonts[i]);
    const GSize size = graphics_text_layout_get_content_size(
        text, font, box, GTextOverflowModeWordWrap, GTextAlignmentCenter);
    if (size.h <= box.size.h && size.w <= box.size.w) {
      return font;
    }
  }
  return fonts_get_system_font(kFonts[ARRAY_LENGTH(kFonts) - 1]);
}

static void notice_load(Window *window) {
  window_set_background_color(window, GColorWhite);
  Layer *root = window_get_root_layer(window);
  const GRect bounds = layer_get_bounds(root);

  // A round screen is narrower towards the top and bottom, so keep the text off the bezel there.
  const int16_t inset_x = PBL_IF_ROUND_ELSE(22, 6);
  const int16_t inset_y = PBL_IF_ROUND_ELSE(24, 8);
  const GRect box = GRect(inset_x, inset_y, bounds.size.w - 2 * inset_x,
                          bounds.size.h - 2 * inset_y);
  const GFont font = fit_font(UPDATE_NOTICE_TEXT, box);
  const GSize size = graphics_text_layout_get_content_size(
      UPDATE_NOTICE_TEXT, font, box, GTextOverflowModeWordWrap, GTextAlignmentCenter);
  // Centre the block vertically; the extra 6 px keeps the last line's descenders in the layer.
  const int16_t h = size.h + 6;
  const int16_t y = (bounds.size.h - h) / 2;

  s_text = text_layer_create(GRect(box.origin.x, y, box.size.w, h));
  text_layer_set_text(s_text, UPDATE_NOTICE_TEXT);
  text_layer_set_font(s_text, font);
  text_layer_set_text_alignment(s_text, GTextAlignmentCenter);
  text_layer_set_overflow_mode(s_text, GTextOverflowModeWordWrap);
  text_layer_set_background_color(s_text, GColorClear);
  text_layer_set_text_color(s_text, GColorBlack);
  layer_add_child(root, text_layer_get_layer(s_text));

  s_timer = app_timer_register(UPDATE_NOTICE_TIMEOUT_MS, timeout_callback, NULL);
}

// The window is created once and reused if a newer version brings the notice back in the same
// session; only its layer and timer go here, and the window itself at deinit.
static void notice_unload(Window *window) {
  if (s_timer != NULL) {
    app_timer_cancel(s_timer);
    s_timer = NULL;
  }
  text_layer_destroy(s_text);
  s_text = NULL;
  s_open = false;
  void (*on_close)(void) = s_on_close;
  s_on_close = NULL;
  if (on_close != NULL) {
    on_close();
  }
}

bool update_notice_window_push(void (*on_close)(void)) {
  if (s_open) {
    return false;  // already open
  }
  if (s_window == NULL) {
    s_window = window_create();
    if (s_window == NULL) {
      APP_LOG(APP_LOG_LEVEL_ERROR, "update notice window not created");
      return false;
    }
    window_set_window_handlers(s_window, (WindowHandlers){
      .load = notice_load,
      .unload = notice_unload,
    });
    window_set_click_config_provider(s_window, click_config_provider);
  }
  s_on_close = on_close;
  s_open = true;
  window_stack_push(s_window, true);
  return true;
}

bool update_notice_window_is_open(void) {
  return s_open;
}

void update_notice_window_deinit(void) {
  if (s_window != NULL) {
    window_destroy(s_window);
    s_window = NULL;
  }
}
