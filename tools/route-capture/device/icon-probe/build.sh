#!/usr/bin/env bash
# Build icon-probe.apk (#59) from the SDK tools directly: no Gradle. Needs a JDK and the SDK's
# build-tools and android-36 platform. The APK isn't committed; build it when needed.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/.local/android-sdk}}"
BT="$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)"
JAR="$SDK/platforms/android-36/android.jar"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
javac --release 17 -classpath "$JAR" -d "$TMP/classes" $(find "$HERE/src" -name '*.java')
"$BT/d8" --min-api 26 --lib "$JAR" --output "$TMP" $(find "$TMP/classes" -name '*.class')
"$BT/aapt2" link -o "$TMP/base.apk" -I "$JAR" --manifest "$HERE/AndroidManifest.xml"
python3 -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1], 'a').write(sys.argv[2], 'classes.dex')" "$TMP/base.apk" "$TMP/classes.dex"
"$BT/zipalign" -f 4 "$TMP/base.apk" "$TMP/aligned.apk"
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --out "$HERE/icon-probe.apk" "$TMP/aligned.apk"
echo "built $HERE/icon-probe.apk"
