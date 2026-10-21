#include "phone_version.h"

#include <string.h>

bool phone_version_update(char *stored, size_t stored_len, const char *incoming) {
  if (stored == NULL || stored_len == 0 || incoming == NULL || incoming[0] == '\0') {
    return false;
  }
  size_t cap = stored_len - 1;
  if (cap > PHONE_VERSION_MAX) {
    cap = PHONE_VERSION_MAX;
  }
  size_t n = strlen(incoming);
  if (n > cap) {
    n = cap;
  }
  // Compare the capped value, so a long version seen again is recognised as unchanged (FR1.3).
  if (strncmp(stored, incoming, n) == 0 && stored[n] == '\0') {
    return false;
  }
  memcpy(stored, incoming, n);
  stored[n] = '\0';
  return true;
}
