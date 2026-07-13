#!/usr/bin/env bash
# Launch an Android emulator AVD for manual/instrumented testing.
# Usage: run-android-emulator.sh [avd-name]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AVD_NAME="${1:-pebblentn-api34}"
SYSTEM_IMAGE="system-images;android-34;google_apis;x86_64"

# Locate the SDK: env vars first, then android/local.properties.
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_root" && -f "$REPO_ROOT/android/local.properties" ]]; then
  sdk_root="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$REPO_ROOT/android/local.properties" | tail -n1)"
fi

# The 'emulator' binary lives in $SDK/emulator and is often not on PATH.
if command -v emulator >/dev/null 2>&1; then
  EMULATOR="$(command -v emulator)"
elif [[ -n "$sdk_root" && -x "$sdk_root/emulator/emulator" ]]; then
  EMULATOR="$sdk_root/emulator/emulator"
else
  echo "ERROR: 'emulator' not found. Install it with:" >&2
  echo "  sdkmanager 'emulator' '$SYSTEM_IMAGE'" >&2
  exit 1
fi

if ! "$EMULATOR" -list-avds | grep -qx "$AVD_NAME"; then
  echo "ERROR: AVD '$AVD_NAME' not found. Available AVDs:" >&2
  "$EMULATOR" -list-avds >&2 || true
  echo "Create one with:" >&2
  echo "  sdkmanager 'emulator' '$SYSTEM_IMAGE'" >&2
  echo "  avdmanager create avd -n $AVD_NAME -k '$SYSTEM_IMAGE'" >&2
  exit 1
fi

# Hardware acceleration needs /dev/kvm (Linux). Warn early — without it the emulator is unusable.
if [[ "$(uname -s)" == "Linux" && ! -w /dev/kvm ]]; then
  echo "WARNING: /dev/kvm is not writable — the emulator will be extremely slow or refuse to start." >&2
  echo "  sudo usermod -aG kvm \$USER   # then re-login (WSL: 'wsl --shutdown' from Windows)" >&2
fi

exec "$EMULATOR" -avd "$AVD_NAME" "${@:2}"
