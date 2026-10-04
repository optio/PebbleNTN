#!/usr/bin/env bash
# Rebuild device/routecap-locale.dex from SetSystemLocale.java (needs a JDK and the SDK's d8).
# The dex is committed so runs need no build step; rebuild only when the Java source changes.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
D8="$(ls -d "$SDK"/build-tools/*/d8 | sort -V | tail -1)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
javac --release 11 -d "$TMP" "$HERE/SetSystemLocale.java"
"$D8" --min-api 26 --output "$TMP" "$TMP/SetSystemLocale.class"
cp "$TMP/classes.dex" "$HERE/routecap-locale.dex"
echo "built $HERE/routecap-locale.dex"
