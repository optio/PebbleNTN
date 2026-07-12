#!/usr/bin/env bash
# Launch an Android emulator AVD for manual/instrumented testing.
# Usage: run-android-emulator.sh [avd-name]
set -euo pipefail

AVD_NAME="${1:-pebblentn-api34}"

if ! command -v emulator >/dev/null 2>&1; then
  echo "ERROR: 'emulator' not found. Install Android SDK emulator and put it on PATH." >&2
  exit 1
fi

if ! emulator -list-avds | grep -qx "$AVD_NAME"; then
  echo "ERROR: AVD '$AVD_NAME' not found. Available AVDs:" >&2
  emulator -list-avds >&2 || true
  echo "Create one with avdmanager, e.g.:" >&2
  echo "  sdkmanager 'system-images;android-34;google_apis;x86_64'" >&2
  echo "  avdmanager create avd -n $AVD_NAME -k 'system-images;android-34;google_apis;x86_64'" >&2
  exit 1
fi

exec emulator -avd "$AVD_NAME"
