#!/usr/bin/env bash
# Build everything: the Android debug APKs and the Pebble watchapp (.pbw).
#
# Prints an explicit artifact report at the end: the absolute path of every artifact that was
# produced, or FAILED for the ones that were not. Exits non-zero if any artifact is missing.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

APP_APK="$REPO_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
FIXTURES_APK="$REPO_ROOT/android/fixture-publisher/build/outputs/apk/debug/fixture-publisher-debug.apk"
WATCHAPP_PBW="$REPO_ROOT/watchapp/build/watchapp.pbw"

# Remove the previous outputs first, so a stale artifact from an earlier run can never be reported
# as if this build had produced it.
rm -f "$APP_APK" "$FIXTURES_APK" "$WATCHAPP_PBW"

failures=0

echo "==> Android debug APKs"
if ! "$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/android" assembleDebug; then
  echo "ERROR: Gradle assembleDebug failed." >&2
  failures=$((failures + 1))
fi

echo
echo "==> Watchapp"
if ! "$REPO_ROOT/scripts/build-watchapp.sh"; then
  echo "ERROR: watchapp build failed." >&2
  failures=$((failures + 1))
fi

# --- Artifact report ----------------------------------------------------------------------------
report() {
  local label="$1" path="$2"
  if [[ -f "$path" ]]; then
    local size
    size="$(du -h "$path" | cut -f1)"
    printf '  OK      %-22s %s (%s)\n' "$label" "$path" "$size"
  else
    printf '  FAILED  %-22s not produced (expected at %s)\n' "$label" "$path"
    failures=$((failures + 1))
  fi
}

echo
echo "Artifacts:"
report "phone app (debug)"   "$APP_APK"
report "fixture publisher"   "$FIXTURES_APK"
report "watchapp"            "$WATCHAPP_PBW"

echo
if [[ "$failures" -eq 0 ]]; then
  echo "All builds finished successfully."
  echo "Install the phone app with: ./scripts/install-android-debug.sh"
  exit 0
fi

echo "Build FAILED — see the errors above. (Missing toolchain? Run ./scripts/bootstrap.sh)"
exit 1
