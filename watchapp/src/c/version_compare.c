#include "version_compare.h"

#include <stddef.h>
#include <stdint.h>

static bool is_digit(char c) {
  return c >= '0' && c <= '9';
}

static const char *skip_prefix(const char *v) {
  return (v[0] == 'v' || v[0] == 'V') ? v + 1 : v;
}

bool version_parse(const char *v) {
  if (v == NULL) {
    return false;
  }
  const char *p = skip_prefix(v);
  for (;;) {
    int digits = 0;
    while (is_digit(*p)) {
      digits++;
      p++;
    }
    if (digits == 0 || digits > VERSION_PART_DIGITS_MAX) {
      return false;  // empty part ("", "0..1", "1.") or a part too long to compare safely
    }
    if (*p == '\0') {
      return true;
    }
    if (*p != '.') {
      return false;  // a suffix, sign, space or other junk ("0.0.71-debug", "0.0.x")
    }
    p++;
  }
}

// The next part of an already parsed version at *p, advancing past it and its '.'; 0 at the end, so
// missing parts count as 0.
static uint32_t take_part(const char **p) {
  uint32_t n = 0;
  while (is_digit(**p)) {
    n = n * 10 + (uint32_t)(**p - '0');
    (*p)++;
  }
  if (**p == '.') {
    (*p)++;
  }
  return n;
}

bool version_newer(const char *a, const char *b) {
  if (!version_parse(a) || !version_parse(b)) {
    return false;  // an unparsable or missing version is ignored, never "newer" (FR1.3)
  }
  const char *pa = skip_prefix(a);
  const char *pb = skip_prefix(b);
  while (*pa != '\0' || *pb != '\0') {
    const uint32_t x = take_part(&pa);
    const uint32_t y = take_part(&pb);
    if (x != y) {
      return x > y;
    }
  }
  return false;  // equal
}

const char *version_effective_update(const char *reported, const char *own, const char *phone) {
  const char *rep = (reported != NULL) ? reported : "";
  const char *own_update = version_newer(own, phone) ? own : "";
  if (own_update[0] == '\0') {
    return rep;
  }
  if (rep[0] == '\0') {
    return own_update;
  }
  // Both apply: the higher one. A reported version that does not parse cannot be compared, so the
  // phone's own report is kept, as before this rule existed.
  return version_newer(own_update, rep) ? own_update : rep;
}
