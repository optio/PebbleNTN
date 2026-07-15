#!/usr/bin/env bash
# Report presence of the required toolchain and, for anything that is missing, print the exact
# commands to install it on this machine.
#
# This script never installs or modifies anything itself — it only tells you what to run.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

REQUIRED_JDK_MAJOR=21

ok=0
missing_list=()

note()    { printf '  %s\n' "$*"; }
present() { printf '  ok      %s\n' "$*"; ok=$((ok + 1)); }
absent()  { printf '  MISSING %s\n' "$1"; missing_list+=("$2"); }

# --- Which package manager should the hints use? -----------------------------------------------
PKG="none"
case "$(uname -s)" in
  Darwin) command -v brew    >/dev/null 2>&1 && PKG="brew" ;;
  Linux)
    if   command -v apt-get >/dev/null 2>&1; then PKG="apt"
    elif command -v dnf     >/dev/null 2>&1; then PKG="dnf"
    elif command -v pacman  >/dev/null 2>&1; then PKG="pacman"
    elif command -v zypper  >/dev/null 2>&1; then PKG="zypper"
    fi
    ;;
esac

# --- Checks ------------------------------------------------------------------------------------
echo "PebbleNTN toolchain check:"

# JDK: must exist *and* be >= 21 (the Android build targets JDK 21).
if command -v java >/dev/null 2>&1; then
  java_major="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n1)"
  if [[ -n "$java_major" && "$java_major" -ge "$REQUIRED_JDK_MAJOR" ]]; then
    present "JDK $java_major ($(command -v java))"
  else
    absent "JDK $REQUIRED_JDK_MAJOR+ (found ${java_major:-unknown} at $(command -v java))" jdk
  fi
else
  absent "JDK $REQUIRED_JDK_MAJOR+ (command: java)" jdk
fi

if command -v python3 >/dev/null 2>&1; then
  present "Python 3 ($(command -v python3))"
else
  absent "Python 3 (command: python3)" python3
fi

if command -v pebble >/dev/null 2>&1; then
  present "Pebble CLI ($(command -v pebble))"
else
  absent "Pebble CLI (command: pebble) — only needed to build the watchapp" pebble
fi

if command -v sha256sum >/dev/null 2>&1; then
  present "sha256sum ($(command -v sha256sum))"
else
  absent "sha256sum (command: sha256sum)" sha256sum
fi

if command -v jq >/dev/null 2>&1; then
  present "jq ($(command -v jq))"
else
  absent "jq (command: jq)" jq
fi

# Android SDK: an env var pointing at a real directory, or android/local.properties sdk.dir.
android_sdk=""
for candidate in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
  [[ -n "$candidate" && -d "$candidate" ]] && android_sdk="$candidate" && break
done
if [[ -z "$android_sdk" && -f "$REPO_ROOT/android/local.properties" ]]; then
  sdk_dir="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$REPO_ROOT/android/local.properties" | tail -n1)"
  [[ -n "$sdk_dir" && -d "$sdk_dir" ]] && android_sdk="$sdk_dir"
fi
if [[ -n "$android_sdk" ]]; then
  present "Android SDK ($android_sdk)"
else
  absent "Android SDK (no ANDROID_HOME/ANDROID_SDK_ROOT and no android/local.properties sdk.dir)" android
fi

# Python jsonschema: used by the spec/catalog/rule validators.
if command -v python3 >/dev/null 2>&1 && python3 -c "import jsonschema" >/dev/null 2>&1; then
  present "python 'jsonschema' package"
else
  absent "python 'jsonschema' package" jsonschema
fi

# --- Install instructions for whatever is missing -----------------------------------------------
hint() {
  case "$1" in
    jdk)
      echo "JDK $REQUIRED_JDK_MAJOR (Android build)"
      case "$PKG" in
        apt)    note "sudo apt-get install -y openjdk-21-jdk" ;;
        dnf)    note "sudo dnf install -y java-21-openjdk-devel" ;;
        pacman) note "sudo pacman -S --needed jdk21-openjdk" ;;
        zypper) note "sudo zypper install -y java-21-openjdk-devel" ;;
        brew)   note "brew install --cask temurin@21" ;;
        *)      note "Install a JDK 21 build (e.g. Eclipse Temurin: https://adoptium.net/temurin/releases/)" ;;
      esac
      note "Then make sure JAVA_HOME points at it, e.g.:"
      note "  export JAVA_HOME=\"\$(dirname \"\$(dirname \"\$(readlink -f \"\$(command -v java)\")\")\")\""
      ;;
    python3)
      echo "Python 3.10+ (validators, protocol/bitmap generators)"
      case "$PKG" in
        apt)    note "sudo apt-get install -y python3 python3-pip" ;;
        dnf)    note "sudo dnf install -y python3 python3-pip" ;;
        pacman) note "sudo pacman -S --needed python python-pip" ;;
        zypper) note "sudo zypper install -y python3 python3-pip" ;;
        brew)   note "brew install python" ;;
        *)      note "Install Python 3.10+ from https://www.python.org/downloads/" ;;
      esac
      ;;
    jsonschema)
      echo "python 'jsonschema' package (schema validation in validate-rules.sh / validate-spec-assets.sh)"
      case "$PKG" in
        apt)    note "sudo apt-get install -y python3-jsonschema" ;;
        dnf)    note "sudo dnf install -y python3-jsonschema" ;;
        pacman) note "sudo pacman -S --needed python-jsonschema" ;;
        zypper) note "sudo zypper install -y python3-jsonschema" ;;
        *)      note "python3 -m pip install --user jsonschema" ;;
      esac
      note "If pip refuses (externally-managed environment / PEP 668), use a venv:"
      note "  python3 -m venv .venv && . .venv/bin/activate && pip install jsonschema"
      ;;
    pebble)
      echo "Pebble CLI + SDK (watchapp build; not needed for the Android app)"
      note "python3 -m pip install --user pebble-tool     # or: pipx install pebble-tool"
      note "pebble sdk install latest                     # downloads the Pebble SDK (4.x)"
      note "pebble sdk list                               # confirm an SDK is active"
      note "This is the same recipe CI uses — see .github/workflows/watchapp.yml."
      note "The SDK is distributed by the Rebble project; if the download fails, check their SDK docs."
      ;;
    sha256sum)
      echo "sha256sum (spec asset integrity checks)"
      case "$PKG" in
        brew) note "brew install coreutils   # then add \$(brew --prefix)/opt/coreutils/libexec/gnubin to PATH" ;;
        *)    note "Provided by GNU coreutils — install the 'coreutils' package for your distro." ;;
      esac
      ;;
    jq)
      echo "jq (JSON handling in the helper scripts)"
      case "$PKG" in
        apt)    note "sudo apt-get install -y jq" ;;
        dnf)    note "sudo dnf install -y jq" ;;
        pacman) note "sudo pacman -S --needed jq" ;;
        zypper) note "sudo zypper install -y jq" ;;
        brew)   note "brew install jq" ;;
        *)      note "Install jq: https://jqlang.github.io/jq/download/" ;;
      esac
      ;;
    android)
      echo "Android SDK (platform 36, build-tools 36.0.0, platform-tools)"
      note "Easiest: install Android Studio, which installs and manages the SDK for you."
      note "Headless alternative — the standalone command-line tools:"
      note "  1. Download 'Command line tools only' from https://developer.android.com/studio"
      note "  2. mkdir -p \$HOME/android-sdk/cmdline-tools && unzip <zip> -d \$HOME/android-sdk/cmdline-tools"
      note "     && mv \$HOME/android-sdk/cmdline-tools/cmdline-tools \$HOME/android-sdk/cmdline-tools/latest"
      note "  3. export ANDROID_HOME=\"\$HOME/android-sdk\""
      note "     export PATH=\"\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH\""
      note "  4. yes | sdkmanager --licenses"
      note "     sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'"
      note "Then point Gradle at it (git-ignored):"
      note "  echo \"sdk.dir=\$ANDROID_HOME\" > $REPO_ROOT/android/local.properties"
      ;;
  esac
}

echo
if [[ ${#missing_list[@]} -eq 0 ]]; then
  echo "Summary: $ok present, 0 missing. You're ready to run ./scripts/build-all.sh and ./scripts/test-all.sh."
  exit 0
fi

echo "Summary: $ok present, ${#missing_list[@]} missing."
echo
echo "How to install what's missing"
case "$PKG" in
  apt)    echo "(detected package manager: apt)" ;;
  dnf)    echo "(detected package manager: dnf)" ;;
  pacman) echo "(detected package manager: pacman)" ;;
  zypper) echo "(detected package manager: zypper)" ;;
  brew)   echo "(detected package manager: Homebrew)" ;;
  *)      echo "(no supported package manager detected — generic instructions below)" ;;
esac

for item in "${missing_list[@]}"; do
  echo
  hint "$item"
done

echo
echo "Re-run ./scripts/bootstrap.sh when you're done. Full guide: docs/FOR-HUMAN.md"
exit 1
