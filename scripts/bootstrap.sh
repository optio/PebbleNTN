#!/usr/bin/env bash
# Report presence of the required toolchain and set up local prerequisites.
# Does not install SDKs automatically; it tells the developer what is missing.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

ok=0
missing=0

check() {
  local name="$1" cmd="$2"
  if command -v "$cmd" >/dev/null 2>&1; then
    echo "  ok      $name ($(command -v "$cmd"))"
    ok=$((ok + 1))
  else
    echo "  MISSING $name (command: $cmd)"
    missing=$((missing + 1))
  fi
}

echo "PebbleNTN toolchain check:"
check "JDK (java)"        java
check "Python 3"         python3
check "Pebble CLI"       pebble
check "sha256sum"        sha256sum
check "jq"               jq

echo
if [[ -n "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]]; then
  echo "  ok      Android SDK (ANDROID_HOME/ANDROID_SDK_ROOT set)"
else
  echo "  MISSING Android SDK (set ANDROID_HOME or create android/local.properties with sdk.dir)"
  missing=$((missing + 1))
fi

echo
echo "python 'jsonschema' package:"
if python3 -c "import jsonschema" >/dev/null 2>&1; then
  echo "  ok      jsonschema importable"
else
  echo "  MISSING jsonschema — install with: python3 -m pip install jsonschema"
  missing=$((missing + 1))
fi

echo
echo "Summary: $ok present, $missing missing."
[[ "$missing" -eq 0 ]] || echo "Install the missing tools before running build-all.sh / test-all.sh."
