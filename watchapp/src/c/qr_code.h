// The install-the-app QR code (REQ-WATCH-017, REQ-WATCH-020), drawn from its module bitmap.
//
// The resource holds one pixel per module (29x29, see tools/gen_connect_qr.py), 168 bytes of heap
// once loaded, and each dark module is drawn as a QR_MODULE_PX square, so the code on screen is the
// same 116x116 as before. Holding it at full size cost about 1.8 KB per copy, which the
// connection hint and About each kept: more than aplite's heap allows next to the shortcuts
// (REQ-WATCH-024, #12).

#pragma once

#include <pebble.h>

#define QR_MODULE_PX 4

// On-screen size of the code; zero when `modules` is NULL.
GSize qr_code_size(const GBitmap *modules);

// Draw the dark modules in black with the code's top-left corner at `origin`. The caller provides
// the white background (the quiet zone), as both screens already paint one.
void qr_code_draw(GContext *ctx, const GBitmap *modules, GPoint origin);
