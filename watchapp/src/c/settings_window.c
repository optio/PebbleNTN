// On-watch settings menu (REQ-WATCH-011, REQ-WATCH-012, REQ-WATCH-015, REQ-WATCH-016): appearance,
// units, backlight and vibration. Opened with SELECT from the navigation window, dismissed with
// BACK. Changes apply immediately and persist.
//
// The top menu is a short list of categories. Accent colour (a long list) and the glyph pack each
// open their own sub-window ("door"); invert, units, ETA display and the two vibration rows toggle
// in place. Backlight toggles in place too, except on RGB-backlight hardware, where it opens a door
// holding both the duration and the tint. The glyph-pack door shows a sample arrow drawn in each
// pack, so the packs can be previewed on the watch before choosing one; changing either vibration
// row plays the newly selected buzz so it can be felt before leaving the menu.

#include "settings_window.h"

#include <pebble.h>

#include "theme.h"

static void (*s_on_change)(void);

static void notify_change(void) {
  if (s_on_change) {
    s_on_change();
  }
}

// menu_cell_basic_header_draw left-aligns the title, which runs straight into the bezel on a round
// display. Centring it keeps the whole title on screen; rectangular watches keep the stock look.
static void draw_menu_header(GContext *ctx, const Layer *cell, const char *title) {
#ifdef PBL_ROUND
  const GRect bounds = layer_get_bounds(cell);
  graphics_context_set_text_color(ctx, GColorBlack);
  graphics_draw_text(ctx, title, fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD), bounds,
                     GTextOverflowModeTrailingEllipsis, GTextAlignmentCenter, NULL);
#else
  menu_cell_basic_header_draw(ctx, cell, title);
#endif
}

static uint16_t one_section(struct MenuLayer *m, void *ctx) { return 1; }

static int16_t header_height(struct MenuLayer *m, uint16_t section, void *ctx) {
  return MENU_CELL_BASIC_HEADER_HEIGHT;
}

// ===== Accent-colour sub-window =================================================================

static Window *s_colour_window;
static MenuLayer *s_colour_menu;

static uint16_t colour_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return ACCENT_AVAILABLE_COUNT;
}

static void colour_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, "Accent colour");
}

static void colour_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const AccentId accent = accent_for_row(index->row);
  const bool active = (accent == settings_accent());
  menu_cell_basic_draw(ctx, cell, accent_name(accent), active ? "Selected" : NULL, NULL);
}

static void colour_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  settings_set_accent(accent_for_row(index->row));
  menu_layer_reload_data(s_colour_menu);
  notify_change();
}

static void colour_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  s_colour_menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(s_colour_menu, NULL, (MenuLayerCallbacks){
    .get_num_sections = one_section,
    .get_num_rows = colour_num_rows,
    .get_header_height = header_height,
    .draw_header = colour_header,
    .draw_row = colour_row,
    .select_click = colour_select,
  });
  menu_layer_set_click_config_onto_window(s_colour_menu, window);
  layer_add_child(root, menu_layer_get_layer(s_colour_menu));
}

static void colour_unload(Window *window) {
  menu_layer_destroy(s_colour_menu);
  s_colour_menu = NULL;
  window_destroy(s_colour_window);
  s_colour_window = NULL;
}

static void push_colour_window(void) {
  s_colour_window = window_create();
  window_set_window_handlers(s_colour_window, (WindowHandlers){
    .load = colour_load,
    .unload = colour_unload,
  });
  window_stack_push(s_colour_window, true);
}

// ===== Glyph-pack sub-window (with preview) =====================================================

static Window *s_pack_window;
static MenuLayer *s_pack_menu;
static GBitmap *s_pack_preview[GLYPH_PACK_COUNT];

// The sample glyph previewed for each pack: a right turn reads clearly at menu-icon size.
static uint32_t pack_preview_resource(GlyphPack pack) {
  switch (pack) {
    case GLYPH_PACK_BOLD: return RESOURCE_ID_MANEUVER_BOLD_RIGHT;
    case GLYPH_PACK_OUTLINE: return RESOURCE_ID_MANEUVER_OUTLINE_RIGHT;
    case GLYPH_PACK_CLASSIC:
    default: return RESOURCE_ID_MANEUVER_RIGHT;
  }
}

static uint16_t pack_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return GLYPH_PACK_COUNT;
}

static void pack_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, "Glyph pack");
}

// Height of `text` in `font`, for stacking the pack name and its "Selected" marker.
static int16_t text_size(const char *text, GFont font, bool want_width) {
  const GSize size = graphics_text_layout_get_content_size(
      text, font, GRect(0, 0, 200, 60), GTextOverflowModeFill, GTextAlignmentLeft);
  return want_width ? size.w : size.h;
}

// A pack preview is a real maneuver glyph (48px, 64px on emery), not a menu-sized icon, so it does
// not fit the icon slot menu_cell_basic_draw reserves — passing it there drew the arrow straight
// over the pack name. The row is laid out by hand instead: the glyph gets its own column and the
// name (plus "Selected") is stacked beside it, with the cell grown to fit (see pack_cell_height).
static void pack_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const GlyphPack pack = (GlyphPack)index->row;
  const bool active = (pack == settings_glyph_pack());
  const GRect bounds = layer_get_bounds(cell);
  const bool highlighted = menu_cell_layer_is_highlighted(cell);
  const GColor fg = highlighted ? GColorWhite : GColorBlack;
  GBitmap *preview = (index->row < GLYPH_PACK_COUNT) ? s_pack_preview[index->row] : NULL;

#ifdef PBL_ROUND
  // Unfocused rows on a round display are only ~32px tall — far too short for the glyph — so there
  // the preview belongs to the focused row alone.
  if (!highlighted) {
    preview = NULL;
  }
#endif

  const char *title = glyph_pack_name(pack);
  const char *subtitle = active ? "Selected" : NULL;
  GFont title_font = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
  GFont sub_font = fonts_get_system_font(FONT_KEY_GOTHIC_18);

  const GSize glyph = (preview != NULL) ? gbitmap_get_bounds(preview).size : GSizeZero;
  const int16_t gap = (preview != NULL) ? 6 : 0;
  const int16_t title_w = text_size(title, title_font, true);
  const int16_t sub_w = (subtitle != NULL) ? text_size(subtitle, sub_font, true) : 0;
  const int16_t text_w = (title_w > sub_w) ? title_w : sub_w;

  // Left-aligned on a rectangular watch; centred as one group on a round one.
  int16_t x = PBL_IF_ROUND_ELSE((bounds.size.w - (glyph.w + gap + text_w)) / 2, 4);
  if (x < 2) {
    x = 2;
  }

  if (preview != NULL) {
    // Same palette trick as the navigation screen: recolour the 1-bit glyph to the row's foreground
    // and let the row background show through, so it works highlighted and not, on colour and on
    // black-and-white watches.
    GColor *palette = gbitmap_get_palette(preview);
    GCompOp op = GCompOpAssign;
    if (palette != NULL && gbitmap_get_format(preview) == GBitmapFormat1BitPalette) {
      palette[0] = fg;
      palette[1] = GColorClear;
      op = GCompOpSet;
    }
    graphics_context_set_compositing_mode(ctx, op);
    graphics_draw_bitmap_in_rect(ctx, preview,
                                 GRect(x, (bounds.size.h - glyph.h) / 2, glyph.w, glyph.h));
    graphics_context_set_compositing_mode(ctx, GCompOpAssign);
    x += glyph.w + gap;
  }

  const int16_t title_h = text_size(title, title_font, false);
  const int16_t sub_h = (subtitle != NULL) ? text_size(subtitle, sub_font, false) : 0;
  // Both fonts report a box taller than the visible glyphs; overlap them slightly so the marker sits
  // under the name rather than a full line below it.
  const int16_t block_h = title_h + (subtitle != NULL ? sub_h - 6 : 0);
  const int16_t y = (bounds.size.h - block_h) / 2;

  graphics_context_set_text_color(ctx, fg);
  graphics_draw_text(ctx, title, title_font, GRect(x, y, text_w + 2, title_h),
                     GTextOverflowModeTrailingEllipsis, GTextAlignmentLeft, NULL);
  if (subtitle != NULL) {
    graphics_draw_text(ctx, subtitle, sub_font, GRect(x, y + title_h - 6, text_w + 2, sub_h),
                       GTextOverflowModeTrailingEllipsis, GTextAlignmentLeft, NULL);
  }
}

// Rows have to be tall enough for a full-size maneuver glyph. Round displays keep the SDK's
// focused/unfocused heights instead, since their unfocused rows are deliberately compressed.
static int16_t pack_cell_height(struct MenuLayer *m, MenuIndex *index, void *ctx) {
#ifdef PBL_ROUND
  const MenuIndex selected = menu_layer_get_selected_index(m);
  return (menu_index_compare(&selected, index) == 0)
             ? MENU_CELL_ROUND_FOCUSED_TALL_CELL_HEIGHT
             : MENU_CELL_ROUND_UNFOCUSED_TALL_CELL_HEIGHT;
#else
  GBitmap *preview = (index->row < GLYPH_PACK_COUNT) ? s_pack_preview[index->row] : NULL;
  const int16_t needed = ((preview != NULL) ? gbitmap_get_bounds(preview).size.h : 0) + 8;
  return (needed > 44) ? needed : 44;
#endif
}

static void pack_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  settings_set_glyph_pack((GlyphPack)index->row);
  menu_layer_reload_data(s_pack_menu);
  notify_change();
}

static void pack_load(Window *window) {
  for (int i = 0; i < GLYPH_PACK_COUNT; i++) {
    s_pack_preview[i] = gbitmap_create_with_resource(pack_preview_resource((GlyphPack)i));
  }
  Layer *root = window_get_root_layer(window);
  s_pack_menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(s_pack_menu, NULL, (MenuLayerCallbacks){
    .get_num_sections = one_section,
    .get_num_rows = pack_num_rows,
    .get_cell_height = pack_cell_height,
    .get_header_height = header_height,
    .draw_header = pack_header,
    .draw_row = pack_row,
    .select_click = pack_select,
  });
  menu_layer_set_click_config_onto_window(s_pack_menu, window);
  layer_add_child(root, menu_layer_get_layer(s_pack_menu));
}

static void pack_unload(Window *window) {
  menu_layer_destroy(s_pack_menu);
  s_pack_menu = NULL;
  for (int i = 0; i < GLYPH_PACK_COUNT; i++) {
    if (s_pack_preview[i] != NULL) {
      gbitmap_destroy(s_pack_preview[i]);
      s_pack_preview[i] = NULL;
    }
  }
  window_destroy(s_pack_window);
  s_pack_window = NULL;
}

static void push_pack_window(void) {
  s_pack_window = window_create();
  window_set_window_handlers(s_pack_window, (WindowHandlers){
    .load = pack_load,
    .unload = pack_unload,
  });
  window_stack_push(s_pack_window, true);
}

// ===== Backlight sub-windows (RGB hardware only) ================================================
//
// On a watch with an RGB backlight (PBL_RGB_BACKLIGHT — Pebble Time 2) the backlight has two
// independent choices, duration and tint, so the top-level Backlight row opens its own door holding
// both instead of cycling one value in place. The tint list is long enough to warrant a second door
// of its own, mirroring the accent-colour list. On every other watch the tint cannot be changed at
// all, so neither door exists and the Backlight row keeps cycling the duration in place.
#ifdef PBL_RGB_BACKLIGHT

static Window *s_backlight_colour_window;
static MenuLayer *s_backlight_colour_menu;

static uint16_t backlight_colour_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return BACKLIGHT_COLOR_COUNT;
}

static void backlight_colour_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, "Backlight colour");
}

static void backlight_colour_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const BacklightColorId id = (BacklightColorId)index->row;
  const bool active = (id == settings_backlight_color());
  menu_cell_basic_draw(ctx, cell, backlight_color_name(id), active ? "Selected" : NULL, NULL);
}

static void backlight_colour_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  settings_set_backlight_color((BacklightColorId)index->row);
  menu_layer_reload_data(s_backlight_colour_menu);
  notify_change();  // re-engages the light so the new tint is visible immediately
}

static void backlight_colour_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  s_backlight_colour_menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(s_backlight_colour_menu, NULL, (MenuLayerCallbacks){
    .get_num_sections = one_section,
    .get_num_rows = backlight_colour_num_rows,
    .get_header_height = header_height,
    .draw_header = backlight_colour_header,
    .draw_row = backlight_colour_row,
    .select_click = backlight_colour_select,
  });
  menu_layer_set_click_config_onto_window(s_backlight_colour_menu, window);
  layer_add_child(root, menu_layer_get_layer(s_backlight_colour_menu));
}

static void backlight_colour_unload(Window *window) {
  menu_layer_destroy(s_backlight_colour_menu);
  s_backlight_colour_menu = NULL;
  window_destroy(s_backlight_colour_window);
  s_backlight_colour_window = NULL;
}

static void push_backlight_colour_window(void) {
  s_backlight_colour_window = window_create();
  window_set_window_handlers(s_backlight_colour_window, (WindowHandlers){
    .load = backlight_colour_load,
    .unload = backlight_colour_unload,
  });
  window_stack_push(s_backlight_colour_window, true);
}

#define BL_ROW_DURATION 0
#define BL_ROW_COLOUR 1
#define BL_ROW_COUNT 2

static Window *s_backlight_window;
static MenuLayer *s_backlight_menu;

static uint16_t backlight_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return BL_ROW_COUNT;
}

static void backlight_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, "Backlight");
}

static void backlight_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  switch (index->row) {
    case BL_ROW_DURATION:
      menu_cell_basic_draw(ctx, cell, "Duration", backlight_mode_name(settings_backlight()), NULL);
      break;
    default:
      menu_cell_basic_draw(ctx, cell, "Colour", backlight_color_name(settings_backlight_color()),
                           NULL);
      break;
  }
}

static void backlight_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  switch (index->row) {
    case BL_ROW_DURATION:
      settings_set_backlight((BacklightMode)((settings_backlight() + 1) % BACKLIGHT_COUNT));
      menu_layer_reload_data(s_backlight_menu);
      notify_change();
      break;
    default:
      push_backlight_colour_window();
      break;
  }
}

// Returning from the colour door: refresh the Colour row's subtitle.
static void backlight_appear(Window *window) {
  if (s_backlight_menu != NULL) {
    menu_layer_reload_data(s_backlight_menu);
  }
}

static void backlight_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  s_backlight_menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(s_backlight_menu, NULL, (MenuLayerCallbacks){
    .get_num_sections = one_section,
    .get_num_rows = backlight_num_rows,
    .get_header_height = header_height,
    .draw_header = backlight_header,
    .draw_row = backlight_row,
    .select_click = backlight_select,
  });
  menu_layer_set_click_config_onto_window(s_backlight_menu, window);
  layer_add_child(root, menu_layer_get_layer(s_backlight_menu));
}

static void backlight_unload(Window *window) {
  menu_layer_destroy(s_backlight_menu);
  s_backlight_menu = NULL;
  window_destroy(s_backlight_window);
  s_backlight_window = NULL;
}

static void push_backlight_window(void) {
  s_backlight_window = window_create();
  window_set_window_handlers(s_backlight_window, (WindowHandlers){
    .load = backlight_load,
    .unload = backlight_unload,
    .appear = backlight_appear,
  });
  window_stack_push(s_backlight_window, true);
}

#endif  // PBL_RGB_BACKLIGHT

// ===== Top-level settings menu ==================================================================

#define ROW_BACKLIGHT 0
#define ROW_VIBE 1
#define ROW_VIBE_INTENSITY 2
#define ROW_COLOUR 3
#define ROW_INVERT 4
#define ROW_ETA 5
#define ROW_ARROW 6
#define ROW_GLYPH 7
#define ROW_UNITS 8
#define MAIN_ROW_COUNT 9

static Window *s_window;
static MenuLayer *s_menu;

static uint16_t main_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return MAIN_ROW_COUNT;
}

static void main_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, "Settings");
}

static void main_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  switch (index->row) {
    case ROW_COLOUR:
      menu_cell_basic_draw(ctx, cell, "Colour", accent_name(settings_accent()), NULL);
      break;
    case ROW_GLYPH:
      menu_cell_basic_draw(ctx, cell, "Glyph pack", glyph_pack_name(settings_glyph_pack()), NULL);
      break;
    case ROW_ARROW:
      menu_cell_basic_draw(ctx, cell, "Arrow side",
                           settings_arrow_left() ? "Left (distance right)" : "Right (distance left)",
                           NULL);
      break;
    case ROW_INVERT:
      menu_cell_basic_draw(ctx, cell, "Invert",
                           settings_inverted() ? "Light on dark" : "Dark on light", NULL);
      break;
    case ROW_UNITS:
      menu_cell_basic_draw(ctx, cell, "Distance units", units_name(settings_units()), NULL);
      break;
    case ROW_ETA:
      menu_cell_basic_draw(ctx, cell, "ETA display", eta_mode_name(settings_eta_mode()), NULL);
      break;
    case ROW_BACKLIGHT:
      menu_cell_basic_draw(ctx, cell, "Backlight", backlight_mode_name(settings_backlight()), NULL);
      break;
    case ROW_VIBE:
      menu_cell_basic_draw(ctx, cell, "Vibrate on update", vibe_pattern_name(settings_vibe_pattern()),
                           NULL);
      break;
    default:
      menu_cell_basic_draw(ctx, cell, "Vibration strength",
                           vibe_intensity_name(settings_vibe_intensity()), NULL);
      break;
  }
}

static void main_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  switch (index->row) {
    case ROW_COLOUR:
      push_colour_window();
      break;
    case ROW_GLYPH:
      push_pack_window();
      break;
    case ROW_ARROW:
      settings_set_arrow_left(!settings_arrow_left());
      menu_layer_reload_data(s_menu);
      notify_change();
      break;
    case ROW_INVERT:
      settings_set_inverted(!settings_inverted());
      menu_layer_reload_data(s_menu);
      notify_change();
      break;
    case ROW_UNITS:
      settings_set_units((UnitsId)((settings_units() + 1) % UNITS_COUNT));
      menu_layer_reload_data(s_menu);
      notify_change();
      break;
    case ROW_ETA:
      settings_set_eta_mode((EtaMode)((settings_eta_mode() + 1) % ETA_MODE_COUNT));
      menu_layer_reload_data(s_menu);
      notify_change();
      break;
    case ROW_BACKLIGHT:
      // RGB hardware has a tint to pick as well, so the row opens a door; otherwise duration is the
      // only choice and cycles in place.
#ifdef PBL_RGB_BACKLIGHT
      push_backlight_window();
#else
      settings_set_backlight((BacklightMode)((settings_backlight() + 1) % BACKLIGHT_COUNT));
      menu_layer_reload_data(s_menu);
      notify_change();
#endif
      break;
    case ROW_VIBE:
      settings_set_vibe_pattern((VibePatternId)((settings_vibe_pattern() + 1) % VIBE_PATTERN_COUNT));
      menu_layer_reload_data(s_menu);
      settings_vibe_play();  // preview the newly selected pattern (no-op when Off)
      notify_change();
      break;
    default:
      settings_set_vibe_intensity(
          (VibeIntensity)((settings_vibe_intensity() + 1) % VIBE_INTENSITY_COUNT));
      menu_layer_reload_data(s_menu);
      settings_vibe_play();  // preview the newly selected intensity (no-op when pattern is Off)
      notify_change();
      break;
  }
}

static void main_appear(Window *window) {
  // Returning from the Colour / Glyph-pack door: refresh those rows' subtitles.
  if (s_menu != NULL) {
    menu_layer_reload_data(s_menu);
  }
}

static void main_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  s_menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(s_menu, NULL, (MenuLayerCallbacks){
    .get_num_sections = one_section,
    .get_num_rows = main_num_rows,
    .get_header_height = header_height,
    .draw_header = main_header,
    .draw_row = main_row,
    .select_click = main_select,
  });
  menu_layer_set_click_config_onto_window(s_menu, window);
  layer_add_child(root, menu_layer_get_layer(s_menu));
}

static void main_unload(Window *window) {
  menu_layer_destroy(s_menu);
  s_menu = NULL;
  window_destroy(s_window);
  s_window = NULL;
}

void settings_window_push(void (*on_change)(void)) {
  s_on_change = on_change;
  s_window = window_create();
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = main_load,
    .appear = main_appear,
    .unload = main_unload,
  });
  window_stack_push(s_window, true);
}
