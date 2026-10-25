// On-watch settings menu (REQ-WATCH-011, REQ-WATCH-012, REQ-WATCH-015, REQ-WATCH-016,
// REQ-WATCH-022, REQ-WATCH-024): appearance, units, backlight, vibration and shortcuts. Opened with
// SELECT from the navigation window, dismissed with BACK. Changes apply immediately and persist.
//
// The top menu is a short list of categories. Accent colour (a long list), the glyph pack and
// vibration each open their own sub-window ("door"); invert, units and ETA display toggle in place.
// Backlight toggles in place too, except on RGB-backlight hardware, where it opens a door holding
// both the duration and the tint. The glyph-pack door shows a sample arrow drawn in each pack, so the
// packs can be previewed on the watch before choosing one; the vibration door holds the
// new-instruction buzz and the early warning, and changing a pattern, strength or warning distance
// plays the newly selected buzz so it can be felt before leaving the menu. The shortcuts door lists
// UP, DOWN and, on a touch watch, Touch and the gestures; each opens one shared action list.

#include "settings_window.h"

#include <pebble.h>

#include "shortcut_apply.h"
#include "theme.h"

static void (*s_on_change)(void);

static void notify_change(void) {
  if (s_on_change) {
    s_on_change();
  }
}

// Step `id` to its next value, wrapping, as a watch edit (a row that cycles in place).
static void cycle_setting(SettingId id) {
  settings_set(id, (settings_get(id) + 1) % sync_value_count(id));
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

// Every menu here fills its window and is driven by the window's buttons. Each passes its callbacks
// as a static const table: building them as a literal in every load function took about 750 bytes
// more code, which on aplite is app RAM the shortcuts (REQ-WATCH-024) need.
static MenuLayer *menu_create(Window *window, const MenuLayerCallbacks *callbacks) {
  Layer *root = window_get_root_layer(window);
  MenuLayer *menu = menu_layer_create(layer_get_bounds(root));
  menu_layer_set_callbacks(menu, NULL, *callbacks);
  menu_layer_set_click_config_onto_window(menu, window);
  layer_add_child(root, menu_layer_get_layer(menu));
  return menu;
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

// The highlighted row is drawn in its own colour (#81), so scrolling the list previews each accent.
// Colour watches only: on black-and-white the list is just Black and White, and the stock highlight
// keeps the focused row distinguishable.
static void colour_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const AccentId accent = accent_for_row(index->row);
  const bool active = (accent == settings_accent());
#ifdef PBL_COLOR
  if (menu_cell_layer_is_highlighted(cell)) {
    const GRect bounds = layer_get_bounds(cell);
    graphics_context_set_fill_color(ctx, accent_swatch(accent));
    graphics_fill_rect(ctx, bounds, 0, GCornerNone);
    if (accent == ACCENT_WHITE) {
      // White on the white list would hide the focus; frame it.
      graphics_context_set_stroke_color(ctx, GColorBlack);
      graphics_context_set_stroke_width(ctx, 2);
      graphics_draw_rect(ctx, grect_inset(bounds, GEdgeInsets(1)));
    }
    graphics_context_set_text_color(ctx, accent_swatch_text(accent));
  }
#endif
  menu_cell_basic_draw(ctx, cell, accent_name(accent), active ? "Selected" : NULL, NULL);
}

static void colour_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  settings_set_accent(accent_for_row(index->row));
  menu_layer_reload_data(s_colour_menu);
  notify_change();
}

static const MenuLayerCallbacks kColourCallbacks = {
  .get_num_sections = one_section,
  .get_num_rows = colour_num_rows,
  .get_header_height = header_height,
  .draw_header = colour_header,
  .draw_row = colour_row,
  .select_click = colour_select,
};

static void colour_load(Window *window) {
  s_colour_menu = menu_create(window, &kColourCallbacks);
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

// The preview glyphs' size: the maneuver bitmaps are 48 px, 64 px on emery (package.json).
#if defined(PBL_PLATFORM_EMERY)
#define PACK_GLYPH_SIZE 64
#else
#define PACK_GLYPH_SIZE 48
#endif

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
//
// The glyph is loaded for the draw and released after it, so the door holds at most one preview at
// a time: holding all three kept this door within a few bytes of aplite's heap limit (#102).
static void pack_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const GlyphPack pack = (GlyphPack)index->row;
  const bool active = (pack == settings_glyph_pack());
  const GRect bounds = layer_get_bounds(cell);
  const bool highlighted = menu_cell_layer_is_highlighted(cell);
  const GColor fg = highlighted ? GColorWhite : GColorBlack;
  GBitmap *preview = NULL;

  // Unfocused rows on a round display are only ~32px tall — far too short for the glyph — so there
  // the preview belongs to the focused row alone.
  if (PBL_IF_ROUND_ELSE(highlighted, true)) {
    preview = gbitmap_create_with_resource(pack_preview_resource(pack));
  }

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
    gbitmap_destroy(preview);
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
  return PACK_GLYPH_SIZE + 8;  // never under the 44 px the text alone needs
#endif
}

static void pack_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  settings_set_glyph_pack((GlyphPack)index->row);
  menu_layer_reload_data(s_pack_menu);
  notify_change();
}

static const MenuLayerCallbacks kPackCallbacks = {
  .get_num_sections = one_section,
  .get_num_rows = pack_num_rows,
  .get_cell_height = pack_cell_height,
  .get_header_height = header_height,
  .draw_header = pack_header,
  .draw_row = pack_row,
  .select_click = pack_select,
};

static void pack_load(Window *window) {
  s_pack_menu = menu_create(window, &kPackCallbacks);
}

static void pack_unload(Window *window) {
  menu_layer_destroy(s_pack_menu);
  s_pack_menu = NULL;
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

static const MenuLayerCallbacks kBacklightColourCallbacks = {
  .get_num_sections = one_section,
  .get_num_rows = backlight_colour_num_rows,
  .get_header_height = header_height,
  .draw_header = backlight_colour_header,
  .draw_row = backlight_colour_row,
  .select_click = backlight_colour_select,
};

static void backlight_colour_load(Window *window) {
  s_backlight_colour_menu = menu_create(window, &kBacklightColourCallbacks);
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

static const MenuLayerCallbacks kBacklightCallbacks = {
  .get_num_sections = one_section,
  .get_num_rows = backlight_num_rows,
  .get_header_height = header_height,
  .draw_header = backlight_header,
  .draw_row = backlight_row,
  .select_click = backlight_select,
};

static void backlight_load(Window *window) {
  s_backlight_menu = menu_create(window, &kBacklightCallbacks);
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

// ===== Vibration sub-window =====================================================================
//
// Two sections: the buzz for a new instruction (REQ-WATCH-016) and the early warning as the
// maneuver comes within a chosen distance (REQ-WATCH-022). Every row cycles in place. The early
// warning's repeat, pattern and strength rows only appear once a distance is chosen.

#define VIBE_SECTION_NEW 0
#define VIBE_SECTION_WARN 1
#define VIBE_SECTION_COUNT 2

#define NEW_ROW_PATTERN 0
#define NEW_ROW_STRENGTH 1
#define NEW_ROW_COUNT 2

#define WARN_ROW_DISTANCE 0
#define WARN_ROW_REPEAT 1
#define WARN_ROW_PATTERN 2
#define WARN_ROW_STRENGTH 3
#define WARN_ROW_COUNT 4

static Window *s_vibe_window;
static MenuLayer *s_vibe_menu;

static void early_warn_label(char *buf, size_t len) {
  early_warn_distance_label(settings_early_warn_distance(), settings_units() == UNITS_IMPERIAL, buf,
                            len);
}

static uint16_t vibe_num_sections(struct MenuLayer *m, void *ctx) { return VIBE_SECTION_COUNT; }

static uint16_t vibe_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  if (section == VIBE_SECTION_NEW) {
    return NEW_ROW_COUNT;
  }
  return (settings_early_warn_distance() == EARLY_WARN_OFF) ? 1 : WARN_ROW_COUNT;
}

static void vibe_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, (section == VIBE_SECTION_NEW) ? "New instruction" : "Early warning");
}

static void vibe_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  // Both sections share the Pattern and Strength rows; the early warning adds Distance and Repeat.
  const bool warn = index->section != VIBE_SECTION_NEW;
  const int row = warn ? index->row : index->row + WARN_ROW_PATTERN;
  char label[12];
  const char *title = "Strength";
  const char *value;
  switch (row) {
    case WARN_ROW_DISTANCE:
      title = "Distance";
      early_warn_label(label, sizeof(label));
      value = label;
      break;
    case WARN_ROW_REPEAT:
      title = "Repeat";
      value = early_warn_repeat_name(settings_early_warn_repeat());
      break;
    case WARN_ROW_PATTERN:
      title = "Pattern";
      value = vibe_pattern_name(warn ? settings_early_warn_pattern() : settings_vibe_pattern());
      break;
    default:
      value = vibe_intensity_name(warn ? settings_early_warn_intensity() : settings_vibe_intensity());
      break;
  }
  menu_cell_basic_draw(ctx, cell, title, value, NULL);
}

// The early warning has no Off or Short pulse pattern (an Off distance is its Off), so it cycles
// through Single to Long only.
static VibePatternId next_audible_pattern(VibePatternId id) {
  return (id >= VIBE_PATTERN_SINGLE && id < VIBE_PATTERN_LONG) ? (VibePatternId)(id + 1)
                                                               : VIBE_PATTERN_SINGLE;
}

// The setting each early-warning row cycles, in row order.
static const uint8_t kWarnRowSettings[WARN_ROW_COUNT] = {
  SET_WARN_DISTANCE, SET_WARN_REPEAT, SET_WARN_PATTERN, SET_WARN_INTENSITY,
};

static void vibe_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  if (index->section == VIBE_SECTION_NEW) {
    if (index->row == NEW_ROW_PATTERN) {
      // Off, Short pulse, Single, … Long, then Off again (#102).
      settings_set_vibe_pattern(vibe_pattern_next(settings_vibe_pattern()));
    } else {
      cycle_setting(SET_VIBE_INTENSITY);
    }
    menu_layer_reload_data(s_vibe_menu);
    settings_vibe_play();  // preview the newly selected buzz (none when the pattern is Off)
    notify_change();
    return;
  }
  if (index->row == WARN_ROW_PATTERN) {
    settings_set_early_warn_pattern(next_audible_pattern(settings_early_warn_pattern()));
  } else {
    cycle_setting((SettingId)kWarnRowSettings[index->row]);
  }
  menu_layer_reload_data(s_vibe_menu);
  if (index->row != WARN_ROW_REPEAT) {
    settings_early_warn_play();  // preview the warning buzz (no-op when the distance is Off)
  }
  notify_change();
}

static const MenuLayerCallbacks kVibeCallbacks = {
  .get_num_sections = vibe_num_sections,
  .get_num_rows = vibe_num_rows,
  .get_header_height = header_height,
  .draw_header = vibe_header,
  .draw_row = vibe_row,
  .select_click = vibe_select,
};

static void vibe_load(Window *window) {
  s_vibe_menu = menu_create(window, &kVibeCallbacks);
}

static void vibe_unload(Window *window) {
  menu_layer_destroy(s_vibe_menu);
  s_vibe_menu = NULL;
  window_destroy(s_vibe_window);
  s_vibe_window = NULL;
}

static void push_vibe_window(void) {
  s_vibe_window = window_create();
  window_set_window_handlers(s_vibe_window, (WindowHandlers){
    .load = vibe_load,
    .unload = vibe_unload,
  });
  window_stack_push(s_vibe_window, true);
}

// ===== Shortcuts sub-windows (REQ-WATCH-024, #12) ===============================================
//
// The door lists the button shortcuts on every watch and, on a touch watch while the watch delivers
// touch, a Touch section: the Touch row toggles in place and, while it is On, the five gesture rows
// follow. Every shortcut row opens the same action list for its slot.

static const char *slot_name(ShortcutSlot slot) {
  switch (slot) {
    case SLOT_UP_BUTTON: return "Up button";
    case SLOT_DOWN_BUTTON: return "Down button";
    case SLOT_SWIPE_LEFT: return "Swipe left";
    case SLOT_SWIPE_RIGHT: return "Swipe right";
    case SLOT_SWIPE_UP: return "Swipe up";
    case SLOT_SWIPE_DOWN: return "Swipe down";
    case SLOT_TAP:
    default: return "Tap";
  }
}

static Window *s_action_window;
static MenuLayer *s_action_menu;
static ShortcutSlot s_action_slot;

static uint16_t action_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return (uint16_t)shortcut_offered_count(SHORTCUT_RGB_BACKLIGHT);
}

static void action_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, slot_name(s_action_slot));
}

// Several labels ("Early warning: previous distance") are longer than a menu title, so the rows are
// laid out by hand: the label wraps onto a second line, with "Selected" under it, and the cell grows
// to fit (see action_cell_height).
#define ACTION_SIDE PBL_IF_ROUND_ELSE(16, 5)

static int16_t action_label_height(const char *label, int16_t cell_w) {
  return graphics_text_layout_get_content_size(
             label, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD),
             GRect(0, 0, cell_w - 2 * ACTION_SIDE, 60), GTextOverflowModeWordWrap,
             PBL_IF_ROUND_ELSE(GTextAlignmentCenter, GTextAlignmentLeft))
      .h;
}

static int16_t action_cell_height(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  const ShortcutAction action = shortcut_action_for_row(index->row, SHORTCUT_RGB_BACKLIGHT);
  const int16_t w = layer_get_bounds(menu_layer_get_layer(m)).size.w;
  const bool active = (action == settings_shortcut(s_action_slot));
  const int16_t needed = action_label_height(shortcut_action_name(action), w) + (active ? 16 : 0) + 8;
  return (needed > 36) ? needed : 36;
}

static void action_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const ShortcutAction action = shortcut_action_for_row(index->row, SHORTCUT_RGB_BACKLIGHT);
  const bool active = (action == settings_shortcut(s_action_slot));
  const GRect bounds = layer_get_bounds(cell);
  const char *label = shortcut_action_name(action);
  const int16_t label_h = action_label_height(label, bounds.size.w);
  const int16_t block_h = label_h + (active ? 16 : 0);
  const int16_t y = (bounds.size.h - block_h) / 2 - 2;
  const int16_t text_w = bounds.size.w - 2 * ACTION_SIDE;
  const GTextAlignment align = PBL_IF_ROUND_ELSE(GTextAlignmentCenter, GTextAlignmentLeft);

  graphics_context_set_text_color(ctx, menu_cell_layer_is_highlighted(cell) ? GColorWhite
                                                                             : GColorBlack);
  graphics_draw_text(ctx, label, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD),
                     GRect(ACTION_SIDE, y, text_w, label_h), GTextOverflowModeWordWrap, align,
                     NULL);
  if (active) {
    graphics_draw_text(ctx, "Selected", fonts_get_system_font(FONT_KEY_GOTHIC_14),
                       GRect(ACTION_SIDE, y + label_h - 2, text_w, 18),
                       GTextOverflowModeTrailingEllipsis, align, NULL);
  }
}

static void action_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  settings_set_shortcut(s_action_slot, shortcut_action_for_row(index->row, SHORTCUT_RGB_BACKLIGHT));
  window_stack_pop(true);  // back to the shortcuts door, which shows the new choice
}

static const MenuLayerCallbacks kActionCallbacks = {
  .get_num_sections = one_section,
  .get_num_rows = action_num_rows,
  .get_cell_height = action_cell_height,
  .get_header_height = header_height,
  .draw_header = action_header,
  .draw_row = action_row,
  .select_click = action_select,
};

static void action_load(Window *window) {
  s_action_menu = menu_create(window, &kActionCallbacks);
}

static void action_unload(Window *window) {
  menu_layer_destroy(s_action_menu);
  s_action_menu = NULL;
  window_destroy(s_action_window);
  s_action_window = NULL;
}

static void push_action_window(ShortcutSlot slot) {
  s_action_slot = slot;
  s_action_window = window_create();
  window_set_window_handlers(s_action_window, (WindowHandlers){
    .load = action_load,
    .unload = action_unload,
  });
  window_stack_push(s_action_window, true);
}

#define SHORTCUT_SECTION_BUTTONS 0
#define SHORTCUT_SECTION_TOUCH 1

// Touch row first, then the gestures in ShortcutSlot order.
#define TOUCH_ROW_SWITCH 0
#define TOUCH_GESTURE_COUNT (SLOT_COUNT - SLOT_SWIPE_LEFT)

static Window *s_shortcuts_window;
static MenuLayer *s_shortcuts_menu;

// Whether the Touch section is shown: only a touch watch has one, and only while the watch delivers
// touch to apps.
static bool touch_available(void) {
#if defined(PBL_PLATFORM_EMERY)
  return touch_service_is_enabled();
#else
  return false;
#endif
}

static uint16_t shortcuts_num_sections(struct MenuLayer *m, void *ctx) {
  return touch_available() ? 2 : 1;
}

static uint16_t shortcuts_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  if (section == SHORTCUT_SECTION_BUTTONS) {
    return 2;
  }
  return settings_touch_enabled() ? 1 + TOUCH_GESTURE_COUNT : 1;
}

static void shortcuts_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, (section == SHORTCUT_SECTION_BUTTONS) ? "Buttons" : "Touch");
}

// The slot a row stands for; SLOT_COUNT for the Touch row.
static ShortcutSlot shortcuts_slot(const MenuIndex *index) {
  if (index->section == SHORTCUT_SECTION_BUTTONS) {
    return (index->row == 0) ? SLOT_UP_BUTTON : SLOT_DOWN_BUTTON;
  }
  if (index->row == TOUCH_ROW_SWITCH) {
    return SLOT_COUNT;
  }
  return (ShortcutSlot)(SLOT_SWIPE_LEFT + index->row - 1);
}

static void shortcuts_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  const ShortcutSlot slot = shortcuts_slot(index);
  if (slot == SLOT_COUNT) {
    menu_cell_basic_draw(ctx, cell, "Touch", settings_touch_enabled() ? "On" : "Off", NULL);
    return;
  }
  menu_cell_basic_draw(ctx, cell, slot_name(slot), shortcut_action_name(settings_shortcut(slot)),
                       NULL);
}

static void shortcuts_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  const ShortcutSlot slot = shortcuts_slot(index);
  if (slot == SLOT_COUNT) {
    settings_set_touch_enabled(!settings_touch_enabled());
    menu_layer_reload_data(s_shortcuts_menu);
    return;
  }
  push_action_window(slot);
}

static const MenuLayerCallbacks kShortcutsCallbacks = {
  .get_num_sections = shortcuts_num_sections,
  .get_num_rows = shortcuts_num_rows,
  .get_header_height = header_height,
  .draw_header = shortcuts_header,
  .draw_row = shortcuts_row,
  .select_click = shortcuts_select,
};

// The door's menu exists only while the door is on screen. The action list is the third window
// deep, and on aplite it scrolls only with the door's menu freed behind it (#102), so the menu is
// built on appear, on the row it was left on, and destroyed when the action list covers it.
static MenuIndex s_shortcuts_selected;

static void shortcuts_appear(Window *window) {
  if (s_shortcuts_menu == NULL) {
    s_shortcuts_menu = menu_create(window, &kShortcutsCallbacks);
    menu_layer_set_selected_index(s_shortcuts_menu, s_shortcuts_selected, MenuRowAlignCenter, false);
  }
}

static void shortcuts_disappear(Window *window) {
  if (s_shortcuts_menu != NULL) {
    s_shortcuts_selected = menu_layer_get_selected_index(s_shortcuts_menu);
    menu_layer_destroy(s_shortcuts_menu);
    s_shortcuts_menu = NULL;
  }
}

static void shortcuts_unload(Window *window) {
  shortcuts_disappear(window);
  window_destroy(s_shortcuts_window);
  s_shortcuts_window = NULL;
}

static void push_shortcuts_window(void) {
  s_shortcuts_selected = (MenuIndex){0, 0};
  s_shortcuts_window = window_create();
  window_set_window_handlers(s_shortcuts_window, (WindowHandlers){
    .appear = shortcuts_appear,
    .disappear = shortcuts_disappear,
    .unload = shortcuts_unload,
  });
  window_stack_push(s_shortcuts_window, true);
}

// ===== Top-level settings menu ==================================================================

#define ROW_ABOUT 0  // About comes first (REQ-WATCH-020)
#define ROW_BACKLIGHT 1
#define ROW_VIBE 2
#define ROW_COLOUR 3
#define ROW_INVERT 4
#define ROW_ETA 5
#define ROW_ETA_LABEL 6
#define ROW_ARROW 7
#define ROW_GLYPH 8
#define ROW_UNITS 9
#define ROW_SHORTCUTS 10
#define MAIN_ROW_COUNT 11

static Window *s_window;
static MenuLayer *s_menu;
static AboutInfo s_about;

static uint16_t main_num_rows(struct MenuLayer *m, uint16_t section, void *ctx) {
  return MAIN_ROW_COUNT;
}

static void main_header(GContext *ctx, const Layer *cell, uint16_t section, void *data) {
  draw_menu_header(ctx, cell, "Settings");
}

// Row titles, in row order. The rows are drawn and selected through tables rather than a case per
// row, which is less code, and code is heap on aplite (#102).
static const char *const kMainTitles[MAIN_ROW_COUNT] = {
  [ROW_ABOUT] = "About",
  [ROW_BACKLIGHT] = "Backlight",
  [ROW_VIBE] = "Vibration",
  [ROW_COLOUR] = "Colour",
  [ROW_INVERT] = "Invert",
  [ROW_ETA] = "ETA display",
  [ROW_ETA_LABEL] = "ETA label",
  [ROW_ARROW] = "Arrow side",
  [ROW_GLYPH] = "Glyph pack",
  [ROW_UNITS] = "Distance units",
  [ROW_SHORTCUTS] = "Shortcuts",
};

// The setting each row cycles in place, plus one; 0 for a row that opens a door. Backlight opens a
// door on RGB hardware, which has a tint to pick as well; elsewhere duration is the only choice.
static const uint8_t kMainCycles[MAIN_ROW_COUNT] = {
#ifndef PBL_RGB_BACKLIGHT
  [ROW_BACKLIGHT] = SET_BACKLIGHT + 1,
#endif
  [ROW_INVERT] = SET_INVERT + 1,
  [ROW_ETA] = SET_ETA_MODE + 1,
  [ROW_ETA_LABEL] = SET_ETA_LABEL + 1,
  [ROW_ARROW] = SET_ARROW_LEFT + 1,
  [ROW_UNITS] = SET_UNITS + 1,
};

static void main_row(GContext *ctx, const Layer *cell, MenuIndex *index, void *data) {
  char summary[80];
  const char *value = summary;
  switch (index->row) {
    case ROW_COLOUR: value = accent_name(settings_accent()); break;
    case ROW_GLYPH: value = glyph_pack_name(settings_glyph_pack()); break;
    case ROW_ARROW:
      value = settings_arrow_left() ? "Left (distance right)" : "Right (distance left)";
      break;
    case ROW_INVERT: value = settings_inverted() ? "Light on dark" : "Dark on light"; break;
    case ROW_UNITS: value = units_name(settings_units()); break;
    case ROW_ETA: value = eta_mode_name(settings_eta_mode()); break;
    case ROW_ETA_LABEL: value = eta_label_style_name(settings_eta_label_style()); break;
    case ROW_ABOUT: value = s_about.app_version; break;
    case ROW_BACKLIGHT: value = backlight_mode_name(settings_backlight()); break;
    case ROW_SHORTCUTS:
      // The button shortcuts, which every watch has: "Up: None / Down: Invert colours".
      snprintf(summary, sizeof(summary), "Up: %s / Down: %s",
               shortcut_action_name(settings_shortcut(SLOT_UP_BUTTON)),
               shortcut_action_name(settings_shortcut(SLOT_DOWN_BUTTON)));
      break;
    default: {
      // Both halves of the door in one line: "Double / Warn 50 m", "Off / Warn off".
      char warn[12];
      early_warn_label(warn, sizeof(warn));
      snprintf(summary, sizeof(summary), "%s / Warn %s", vibe_pattern_name(settings_vibe_pattern()),
               (settings_early_warn_distance() == EARLY_WARN_OFF) ? "off" : warn);
      break;
    }
  }
  menu_cell_basic_draw(ctx, cell, kMainTitles[index->row], value, NULL);
}

static void main_select(struct MenuLayer *m, MenuIndex *index, void *ctx) {
  const uint8_t cycles = kMainCycles[index->row];
  if (cycles != 0) {
    cycle_setting((SettingId)(cycles - 1));
    menu_layer_reload_data(s_menu);
    notify_change();
    return;
  }
  switch (index->row) {
    case ROW_COLOUR:
      push_colour_window();
      break;
    case ROW_GLYPH:
      push_pack_window();
      break;
    case ROW_ABOUT:
      about_window_push(&s_about);
      break;
    case ROW_SHORTCUTS:
      push_shortcuts_window();
      break;
#ifdef PBL_RGB_BACKLIGHT
    case ROW_BACKLIGHT:
      push_backlight_window();
      break;
#endif
    default:
      push_vibe_window();
      break;
  }
}

static void main_appear(Window *window) {
  // Returning from the Colour / Glyph-pack / Vibration / Shortcuts door: refresh those rows'
  // subtitles.
  if (s_menu != NULL) {
    menu_layer_reload_data(s_menu);
  }
}

static const MenuLayerCallbacks kMainCallbacks = {
  .get_num_sections = one_section,
  .get_num_rows = main_num_rows,
  .get_header_height = header_height,
  .draw_header = main_header,
  .draw_row = main_row,
  .select_click = main_select,
};

static void main_load(Window *window) {
  s_menu = menu_create(window, &kMainCallbacks);
}

static void main_unload(Window *window) {
  menu_layer_destroy(s_menu);
  s_menu = NULL;
  window_destroy(s_window);
  s_window = NULL;
}

void settings_window_refresh(void) {
  if (s_window == NULL) {
    return;  // Settings is not open
  }
  // Any open door closes: some doors' rows depend on the values that changed (the early warning
  // rows, the Touch rows), and the top menu shows every new value and reopens any door. One rule
  // for all doors keeps this small on aplite, where code is heap.
  Window *top;
  while ((top = window_stack_get_top_window()) != s_window && top != NULL) {
    window_stack_pop(false);
  }
  menu_layer_reload_data(s_menu);
}

void settings_window_push(void (*on_change)(void), const AboutInfo *about) {
  s_on_change = on_change;
  s_about = *about;
  s_window = window_create();
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = main_load,
    .appear = main_appear,
    .unload = main_unload,
  });
  window_stack_push(s_window, true);
}
