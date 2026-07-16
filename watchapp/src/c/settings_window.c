// On-watch settings menu (REQ-WATCH-011, REQ-WATCH-012): appearance + units. Opened with SELECT
// from the navigation window, dismissed with BACK. Changes apply immediately and persist.
//
// The top menu is a short list of categories. Accent colour (a long list) and the glyph pack each
// open their own sub-window ("door"); invert and units toggle in place. The glyph-pack door shows a
// sample arrow drawn in each pack, so the packs can be previewed on the watch before choosing one.

#include "settings_window.h"

#include <pebble.h>

#include "theme.h"

static void (*s_on_change)(void);

static void notify_change(void) {
  if (s_on_change) {
    s_on_change();
  }
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
  menu_cell_basic_header_draw(ctx, cell, "Accent colour");
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
  menu_cell_basic_header_draw(ctx, cell, "Glyph pack");
}

static void pack_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const GlyphPack pack = (GlyphPack)index->row;
  const bool active = (pack == settings_glyph_pack());
  GBitmap *preview = (index->row < GLYPH_PACK_COUNT) ? s_pack_preview[index->row] : NULL;
  menu_cell_basic_draw(ctx, cell, glyph_pack_name(pack), active ? "Selected" : NULL, preview);
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

// ===== Top-level settings menu ==================================================================

#define ROW_COLOUR 0
#define ROW_GLYPH 1
#define ROW_ARROW 2
#define ROW_INVERT 3
#define ROW_UNITS 4
#define MAIN_ROW_COUNT 5

static Window *s_window;
static MenuLayer *s_menu;

static uint16_t main_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return MAIN_ROW_COUNT;
}

static void main_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  menu_cell_basic_header_draw(ctx, cell, "Settings");
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
    default:
      menu_cell_basic_draw(ctx, cell, "Distance units", units_name(settings_units()), NULL);
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
    default:
      settings_set_units((UnitsId)((settings_units() + 1) % UNITS_COUNT));
      menu_layer_reload_data(s_menu);
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
