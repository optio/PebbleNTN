#!/usr/bin/env bash
# Compile and run the watchapp's host-testable C units.
#
# The watchapp itself is cross-compiled for ARM and normally only verifiable in the emulator, so
# pure logic (no Pebble APIs) is kept in its own translation units and tested here with the host
# compiler — fast, and it needs neither the SDK nor an emulator.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

CC="${CC:-gcc}"
status=0

for test_src in "$REPO_ROOT"/watchapp/tests/test_*.c; do
  name="$(basename "$test_src" .c)"
  # Each test compiles against the unit it covers: test_<unit>.c <-> src/c/<unit>.c
  unit="$REPO_ROOT/watchapp/src/c/${name#test_}.c"
  if [[ ! -f "$unit" ]]; then
    echo "ERROR: $test_src has no matching unit at $unit" >&2
    exit 1
  fi
  # A unit that calls another pure unit names it in its test, on a line "// Links: <unit> ...".
  links=()
  for dep in $(sed -n 's|^// Links:||p' "$test_src"); do
    links+=("$REPO_ROOT/watchapp/src/c/$dep.c")
  done
  echo "==> $name"
  "$CC" -std=c11 -Wall -Wextra -Werror -o "$BUILD_DIR/$name" "$test_src" "$unit" "${links[@]}"
  "$BUILD_DIR/$name" || status=1
done

if [[ "$status" -ne 0 ]]; then
  echo "Watchapp unit tests FAILED." >&2
  exit 1
fi
echo "Watchapp unit tests OK."
