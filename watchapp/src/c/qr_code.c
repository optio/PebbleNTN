#include "qr_code.h"

GSize qr_code_size(const GBitmap *modules) {
  if (modules == NULL) {
    return GSizeZero;
  }
  const GSize n = gbitmap_get_bounds(modules).size;
  return GSize(n.w * QR_MODULE_PX, n.h * QR_MODULE_PX);
}

// The resource is a plain 1-bit bitmap (package.json): one bit per module, least significant bit
// first, a set bit white. Each run of dark modules in a row is filled as one rectangle.
void qr_code_draw(GContext *ctx, const GBitmap *modules, GPoint origin) {
  if (modules == NULL || gbitmap_get_format(modules) != GBitmapFormat1Bit) {
    return;
  }
  const GSize n = gbitmap_get_bounds(modules).size;
  const uint8_t *data = gbitmap_get_data(modules);
  const uint16_t stride = gbitmap_get_bytes_per_row(modules);
  graphics_context_set_fill_color(ctx, GColorBlack);
  for (int16_t y = 0; y < n.h; y++) {
    const uint8_t *row = data + y * stride;
    int16_t x = 0;
    while (x < n.w) {
      if ((row[x >> 3] >> (x & 7)) & 1) {
        x++;  // white module: the background shows through
        continue;
      }
      const int16_t start = x;
      while (x < n.w && !((row[x >> 3] >> (x & 7)) & 1)) {
        x++;
      }
      graphics_fill_rect(ctx,
                         GRect(origin.x + start * QR_MODULE_PX, origin.y + y * QR_MODULE_PX,
                               (x - start) * QR_MODULE_PX, QR_MODULE_PX),
                         0, GCornerNone);
    }
  }
}
