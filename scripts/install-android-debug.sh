#!/usr/bin/env bash
# Build and install the PebbleNTN debug APK onto a connected device/emulator, then launch it.
#
# Usage: install-android-debug.sh [--fixtures] [--no-launch]
#   --fixtures    also install the fixture publisher (synthetic navigation notifications)
#   --no-launch   install only; do not start the app
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib/android-sdk.sh
source "$REPO_ROOT/scripts/lib/android-sdk.sh"

APP_ID="com.pebblentn.app"
install_fixtures=0
launch=1
for arg in "$@"; do
  case "$arg" in
    --fixtures)  install_fixtures=1 ;;
    --no-launch) launch=0 ;;
    *) echo "ERROR: unknown option '$arg' (see --help in the header)." >&2; exit 2 ;;
  esac
done

# adb usually is not on PATH; fall back to <sdk>/platform-tools/adb.
if ! ADB="$(find_sdk_tool adb platform-tools "$REPO_ROOT")"; then
  echo "ERROR: 'adb' not found — not on PATH, and no Android SDK found via ANDROID_HOME," >&2
  echo "       ANDROID_SDK_ROOT, or android/local.properties (sdk.dir)." >&2
  echo "       Install it with:  sdkmanager 'platform-tools'      (see ./scripts/bootstrap.sh)" >&2
  exit 1
fi

devices="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$devices" ]]; then
  echo "ERROR: no connected device/emulator ('$ADB devices' shows none)." >&2
  echo "       Start one with:  ./scripts/run-android-emulator.sh" >&2
  echo "       Or plug in a phone with USB debugging enabled." >&2
  exit 1
fi
if [[ "$(wc -l <<<"$devices")" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
  echo "Note: several devices are connected; Gradle installs to all of them."
  echo "      Set ANDROID_SERIAL=<serial> to target just one:"
  sed 's/^/        /' <<<"$devices"
fi

cat <<EOF

If the app crashes ("PebbleNTN keeps stopping"), read the stack trace with:

  $ADB logcat -b crash -v time          # last crash (add -d to dump and exit)
  $ADB logcat --pid=\$($ADB shell pidof -s $APP_ID)   # live log of the running app

EOF

targets=(:app:installDebug)
[[ "$install_fixtures" -eq 1 ]] && targets+=(:fixture-publisher:installDebug)

"$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/android" "${targets[@]}"

echo
echo "Installed: $APP_ID (debug)"
[[ "$install_fixtures" -eq 1 ]] && echo "Installed: com.pebblentn.fixtures (fixture publisher)"

if [[ "$launch" -eq 1 ]]; then
  echo "Launching $APP_ID …"
  "$ADB" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  echo "Grant notification access when the app asks for it."
else
  echo "Launch it with: $ADB shell monkey -p $APP_ID -c android.intent.category.LAUNCHER 1"
fi
