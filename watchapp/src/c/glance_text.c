#include "glance_text.h"

#include <stdio.h>

// Copy `text` into `out` (`out_len` > 0 bytes, always terminated) without the characters the
// template language treats specially: braces open an expression and a backslash escapes. Neither
// belongs in a clock time or a version, so they are dropped rather than break the template.
// Returns the copied length.
static size_t template_safe_copy(const char *text, char *out, size_t out_len) {
  size_t n = 0;
  for (const char *c = text ? text : ""; *c != '\0' && n < out_len - 1; c++) {
    if (*c != '{' && *c != '}' && *c != '\\') {
      out[n++] = *c;
    }
  }
  out[n] = '\0';
  return n;
}

bool glance_countdown_template(bool navigating, int32_t eta_epoch, int32_t now, const char *eta_text,
                               char *buf, size_t buf_len) {
  if (buf_len == 0) {
    return false;
  }
  buf[0] = '\0';
  if (!navigating || eta_epoch <= 0 || eta_epoch <= now) {
    return false;
  }

  char eta[24];
  const size_t n = template_safe_copy(eta_text, eta, sizeof(eta));

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

bool glance_update_subtitle(const char *version, char *buf, size_t buf_len) {
  if (buf_len == 0) {
    return false;
  }
  buf[0] = '\0';
  char safe[24];
  if (template_safe_copy(version, safe, sizeof(safe)) == 0) {
    return false;
  }
  const int written = snprintf(buf, buf_len, "Update: %s", safe);
  if (written < 0 || (size_t)written >= buf_len) {
    buf[0] = '\0';  // a cut-off version would be misleading; show the hint instead
    return false;
  }
  return true;
}
