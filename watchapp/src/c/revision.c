#include "revision.h"

bool revision_valid(int rev) {
  return rev >= 0 && rev < REVISION_MODULO;
}

bool revision_newer(int a, int b) {
  if (!revision_valid(a) || !revision_valid(b)) {
    return false;
  }
  const int ahead = (a - b + REVISION_MODULO) % REVISION_MODULO;
  return ahead >= 1 && ahead < REVISION_MODULO / 2;
}

int revision_next(int rev) {
  return revision_valid(rev) ? (rev + 1) % REVISION_MODULO : 0;
}
