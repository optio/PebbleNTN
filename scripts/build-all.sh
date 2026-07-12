#!/usr/bin/env bash
# Build everything: Android debug APK and the Pebble watchapp.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Android debug APK"
"$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/android" assembleDebug

echo "==> Watchapp"
"$REPO_ROOT/scripts/build-watchapp.sh"

echo "All builds finished."
