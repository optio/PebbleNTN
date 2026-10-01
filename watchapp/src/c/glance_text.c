#include "glance_text.h"

#include <stdio.h>

bool glance_countdown_template(bool navigating, int32_t eta_epoch, int32_t now, const char *eta_text,
                               char *buf, size_t buf_len) {
  if (buf_len == 0) {
    return false;
  }
  buf[0] = '\0';
  if (!navigating || eta_epoch <= 0 || eta_epoch <= now) {
    return false;
  }

  // The arrival-time text goes into a template string, where braces open an expression and a
  // backslash escapes; neither belongs in a clock time, so drop them rather than break the template.
  char eta[24];
  size_t n = 0;
  for (const char *c = eta_text ? eta_text : ""; *c != '\0' && n < sizeof(eta) - 1; c++) {
    if (*c != '{' && *c != '}' && *c != '\\') {
      eta[n++] = *c;
    }
  }
  eta[n] = '\0';

  // "%H:%0M" gives "0:24", the same style as the watch's own "IN 0:25".
  int written;
  if (n > 0) {
    written = snprintf(buf, buf_len, "In {time_until(%ld)|format('%%H:%%0M')} \xC2\xB7 ETA %s",
                       (long)eta_epoch, eta);
  } else {
    written = snprintf(buf, buf_len, "In {time_until(%ld)|format('%%H:%%0M')}", (long)eta_epoch);
  }
  if (written < 0 || (size_t)written >= buf_len) {
    buf[0] = '\0';  // a cut-off template would be invalid; show the hint instead
    return false;
  }
  return true;
}
