#include "settings_window.h"

#include <pebble.h>

#include "theme.h"

#define SECTION_COLOUR 0
#define SECTION_DISPLAY 1
#define SECTION_UNITS 2
#define SECTION_COUNT 3

static Window *s_window;
static MenuLayer *s_menu;
static void (*s_on_change)(void);

static uint16_t num_sections(struct MenuLayer *m, void *ctx) { return SECTION_COUNT; }

static uint16_t num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  switch (section) {
    case SECTION_COLOUR: return ACCENT_AVAILABLE_COUNT;
    case SECTION_DISPLAY: return 1; // invert
    default: return UNITS_COUNT;
  }
}

static int16_t header_height(struct MenuLayer *m, uint16_t section, void *ctx) {
  return MENU_CELL_BASIC_HEADER_HEIGHT;
}

static void draw_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  const char *title = "Distance units";
  if (section == SECTION_COLOUR) {
    title = "Accent colour";
  } else if (section == SECTION_DISPLAY) {
    title = "Display";
  }
  menu_cell_basic_header_draw(ctx, cell, title);
}

static void draw_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  switch (index->section) {
    case SECTION_COLOUR: {
      const AccentId accent = accent_for_row(index->row);
      const bool active = (accent == settings_accent());
      menu_cell_basic_draw(ctx, cell, accent_name(accent), active ? "Selected" : NULL, NULL);
      break;
    }
    case SECTION_DISPLAY:
      menu_cell_basic_draw(ctx, cell, "Invert",
                           settings_inverted() ? "Light on dark" : "Dark on light", NULL);
      break;
    default: {
      const UnitsId units = (UnitsId)index->row;
      const bool active = (units == settings_units());
      menu_cell_basic_draw(ctx, cell, units_name(units), active ? "Selected" : NULL, NULL);
      break;
    }
  }
}

static void select_row(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  switch (index->section) {
    case SECTION_COLOUR:
      settings_set_accent(accent_for_row(index->row));
      break;
    case SECTION_DISPLAY:
      settings_set_inverted(!settings_inverted());
      break;
    default:
      settings_set_units((UnitsId)index->row);
      break;
  }
  menu_layer_reload_data(s_menu);
  if (s_on_change) {
    s_on_change();
  }
}

static void window_load(Window *window) {
  Layer *root = window_get_root_layer(window);

  s_menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(s_menu, NULL, (MenuLayerCallbacks){
    .get_num_sections = num_sections,
    .get_num_rows = num_rows,
    .get_header_height = header_height,
    .draw_header = draw_header,
    .draw_row = draw_row,
    .select_click = select_row,
  });
  menu_layer_set_click_config_onto_window(s_menu, window);
  layer_add_child(root, menu_layer_get_layer(s_menu));
}

static void window_unload(Window *window) {
  menu_layer_destroy(s_menu);
  s_menu = NULL;
  window_destroy(s_window);
  s_window = NULL;
}

void settings_window_push(void (*on_change)(void)) {
  s_on_change = on_change;
  s_window = window_create();
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = window_load,
    .unload = window_unload,
  });
  window_stack_push(s_window, true);
}
