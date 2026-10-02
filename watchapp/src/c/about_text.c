#include "about_text.h"

#include <stdio.h>

const char *about_phone_status(bool pebble_app_connected, bool heard_from_phone) {
  if (heard_from_phone) {
    return "Phone app connected";
  }
  return pebble_app_connected ? "Phone app not answering" : "Pebble app not connected";
}

void about_details(char *buf, size_t buf_len, const char *app_version, int protocol_major,
                   int protocol_minor, const char *phone_status) {
  if (buf_len == 0) {
    return;
  }
  snprintf(buf, buf_len, "Version %s\nProtocol %d.%d\n%s", app_version ? app_version : "?",
           protocol_major, protocol_minor, phone_status ? phone_status : "");
}
