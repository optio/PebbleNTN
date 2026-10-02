#pragma once

#include <pebble.h>

// What the About screen shows that only main.c knows (REQ-WATCH-020).
typedef struct {
  const char *app_version;
  bool heard_from_phone;  // the PebbleNTN phone app has answered since the watchapp opened
} AboutInfo;

// About (#48): versions, the phone connection, and the install QR (#14). Scrolls with UP/DOWN.
void about_window_push(const AboutInfo *info);
