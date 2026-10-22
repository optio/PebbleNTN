// Host-compiled unit tests for the strict version rule and the effective update version
// (REQ-WATCH-023, amended). Run with scripts/test-watchapp-unit.sh — no Pebble SDK or emulator needed.

#include "../src/c/version_compare.h"

#include <stdio.h>
#include <string.h>

static int failures;

static void check(const char *what, bool ok) {
  printf("  %s  %s\n", ok ? "ok  " : "FAIL", what);
  if (!ok) {
    failures++;
  }
}

static void check_str(const char *what, const char *got, const char *want) {
  const bool ok = got != NULL && strcmp(got, want) == 0;
  printf("  %s  %s: \"%s\"\n", ok ? "ok  " : "FAIL", what, got != NULL ? got : "(null)");
  if (!ok) {
    printf("        expected \"%s\"\n", want);
    failures++;
  }
}

int main(void) {
  printf("version_parse\n");
  check("0.0.71 parses", version_parse("0.0.71"));
  check("v0.0.71 parses", version_parse("v0.0.71"));
  check("V1 parses", version_parse("V1"));
  check("a 9-digit part parses", version_parse("0.0.999999999"));
  check("0.0.71-debug is rejected", !version_parse("0.0.71-debug"));
  check("0.0.x is rejected", !version_parse("0.0.x"));
  check("0..1 is rejected", !version_parse("0..1"));
  check("a trailing dot is rejected", !version_parse("0.0."));
  check("a space is rejected", !version_parse("0.0 .1"));
  check("a sign is rejected", !version_parse("+0.0.1"));
  check("\"\" is rejected", !version_parse(""));
  check("a bare v is rejected", !version_parse("v"));
  check("NULL is rejected", !version_parse(NULL));
  check("a 10-digit part is rejected", !version_parse("0.0.1234567890"));

  printf("version_newer\n");
  check("0.0.10 > 0.0.9", version_newer("0.0.10", "0.0.9"));
  check("0.0.9 is not > 0.0.10", !version_newer("0.0.9", "0.0.10"));
  check("0.1.0 > 0.0.99", version_newer("0.1.0", "0.0.99"));
  check("1.2 = 1.2.0 (not newer)", !version_newer("1.2", "1.2.0"));
  check("1.2.0 = 1.2 (not newer)", !version_newer("1.2.0", "1.2"));
  check("1.2.1 > 1.2", version_newer("1.2.1", "1.2"));
  check("v0.0.72 > 0.0.71", version_newer("v0.0.72", "0.0.71"));
  check("equal is not newer", !version_newer("0.0.71", "0.0.71"));
  check("unparsable a is never newer", !version_newer("0.0.71-debug", "0.0.70"));
  check("unparsable b is never newer", !version_newer("0.0.72", "0.0.x"));
  check("missing b is never newer", !version_newer("0.0.72", ""));
  check("NULL b is never newer", !version_newer("0.0.72", NULL));
  check("NULL a is never newer", !version_newer(NULL, "0.0.70"));

  printf("version_effective_update\n");
  check_str("own newer, nothing reported -> own", version_effective_update("", "0.0.72", "0.0.71"),
            "0.0.72");
  check_str("NULL reported, own newer -> own", version_effective_update(NULL, "0.0.72", "0.0.71"),
            "0.0.72");
  check_str("reported higher than own -> reported",
            version_effective_update("0.0.73", "0.0.72", "0.0.71"), "0.0.73");
  check_str("own higher than reported -> own", version_effective_update("0.0.72", "0.0.74", "0.0.71"),
            "0.0.74");
  check_str("own not newer, reported -> reported",
            version_effective_update("0.0.73", "0.0.72", "0.0.72"), "0.0.73");
  check_str("own not newer, nothing reported -> \"\"",
            version_effective_update("", "0.0.72", "0.0.72"), "");
  check_str("own older than phone -> \"\"", version_effective_update("", "0.0.71", "0.0.72"), "");
  check_str("phone unparsable -> reported only",
            version_effective_update("0.0.73", "0.0.72", "0.0.71-debug"), "0.0.73");
  check_str("phone unparsable, nothing reported -> \"\"",
            version_effective_update("", "0.0.72", "0.0.x"), "");
  check_str("phone never heard -> \"\"", version_effective_update("", "0.0.72", ""), "");
  check_str("unparsable reported is kept over own",
            version_effective_update("0.0.73-rc", "0.0.72", "0.0.71"), "0.0.73-rc");

  if (failures) {
    printf("%d failure(s)\n", failures);
    return 1;
  }
  printf("\nAll version_compare assertions passed.\n");
  return 0;
}
