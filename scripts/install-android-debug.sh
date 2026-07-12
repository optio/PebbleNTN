#!/usr/bin/env bash
# Build and install the Android debug APK onto a connected device/emulator.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: 'adb' not found. Install Android platform-tools." >&2
  exit 1
fi

if [[ -z "$(adb devices | awk 'NR>1 && $2=="device"')" ]]; then
  echo "ERROR: no connected device/emulator (check 'adb devices')." >&2
  exit 1
fi

"$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/android" installDebug
echo "Installed debug build. Launch: adb shell monkey -p com.pebblentn.app 1"
