#include "update_notice.h"

#include <string.h>

static bool is_empty(const char *text) {
  return text == NULL || text[0] == '\0';
}

bool update_record(char *stored, size_t stored_len, bool flag_set, const char *incoming) {
  if (stored == NULL || stored_len == 0) {
    return false;
  }
  if (!flag_set) {
    // Bit 4 clear: the phone knows of no update, so forget the recorded one (FR3.1).
    if (stored[0] == '\0') {
      return false;
    }
    stored[0] = '\0';
    return true;
  }
  if (is_empty(incoming)) {
    return false;  // the bit without a version: keep what is recorded
  }
  size_t cap = stored_len - 1;
  if (cap > UPDATE_VERSION_MAX) {
    cap = UPDATE_VERSION_MAX;
  }
  size_t n = strlen(incoming);
  if (n > cap) {
    n = cap;
  }
  // Compare the capped value, so a long version seen again is recognised as unchanged.
  if (strncmp(stored, incoming, n) == 0 && stored[n] == '\0') {
    return false;
  }
  memcpy(stored, incoming, n);
  stored[n] = '\0';
  return true;
}

UpdateNoticeAction update_notice_action(const char *latest, const char *shown, bool navigating,
                                        bool main_on_top) {
  if (is_empty(latest)) {
    return UPDATE_NOTICE_NONE;
  }
  if (shown != NULL && strcmp(latest, shown) == 0) {
    return UPDATE_NOTICE_NONE;  // once per version (FR4.3)
  }
  if (navigating || !main_on_top) {
    return UPDATE_NOTICE_DEFER;  // never over a route or another screen (FR4.4)
  }
  return UPDATE_NOTICE_SHOW;
}

const char *update_idle_text(const char *latest) {
  return is_empty(latest) ? IDLE_TEXT : UPDATE_IDLE_TEXT;
}
