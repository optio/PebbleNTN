#!/usr/bin/env bash
# Single source of truth for the app version: the VERSION file at the repo root.
#
# The version is duplicated in three places that must never disagree — the phone app reports it to
# the watch in the READY handshake, and the watch reports its own back:
#   - android/app/build.gradle.kts   versionName + versionCode
#   - watchapp/package.json          "version"
#   - watchapp/src/c/main.c          APP_VERSION_STR
#
# Usage:
#   scripts/version.sh                 print the current version
#   scripts/version.sh --check         verify every file agrees with VERSION (exit 1 if not)
#   scripts/version.sh bump patch      bump X.Y.Z -> X.Y.(Z+1) and sync every file
#   scripts/version.sh bump minor      bump X.Y.Z -> X.(Y+1).0
#   scripts/version.sh bump major      bump X.Y.Z -> (X+1).0.0
#   scripts/version.sh set 1.2.3       set an exact version and sync
#
# versionCode is derived from the semver so it is monotonic and needs no separate bookkeeping:
#   major*10000 + minor*100 + patch   (0.0.1 -> 1, 1.2.3 -> 10203)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

VERSION_FILE="VERSION"
GRADLE="android/app/build.gradle.kts"
PACKAGE_JSON="watchapp/package.json"
MAIN_C="watchapp/src/c/main.c"

current() { tr -d '[:space:]' < "$VERSION_FILE"; }

version_code() {
  local IFS=.
  read -r major minor patch <<<"$1"
  echo $((10#$major * 10000 + 10#$minor * 100 + 10#$patch))
}

valid() { [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; }

sync_files() {
  local version="$1" code
  code="$(version_code "$version")"

  printf '%s\n' "$version" > "$VERSION_FILE"
  sed -i -E "s/^(\s*versionCode = ).*/\1${code}/" "$GRADLE"
  sed -i -E "s/^(\s*versionName = ).*/\1\"${version}\"/" "$GRADLE"
  python3 - "$version" <<'PY'
import json, sys, collections
version = sys.argv[1]
path = "watchapp/package.json"
data = json.load(open(path), object_pairs_hook=collections.OrderedDict)
data["version"] = version
with open(path, "w") as f:
    json.dump(data, f, indent=2)
    f.write("\n")
PY
  sed -i -E "s/^(#define APP_VERSION_STR ).*/\1\"${version}\"/" "$MAIN_C"
}

check() {
  local version code fail=0
  version="$(current)"
  code="$(version_code "$version")"
  valid "$version" || { echo "VERSION is not semver: '$version'" >&2; exit 1; }

  grep -qE "^\s*versionName = \"${version}\"$" "$GRADLE" ||
    { echo "MISMATCH $GRADLE versionName (expected $version)" >&2; fail=1; }
  grep -qE "^\s*versionCode = ${code}$" "$GRADLE" ||
    { echo "MISMATCH $GRADLE versionCode (expected $code)" >&2; fail=1; }
  [[ "$(python3 -c 'import json;print(json.load(open("watchapp/package.json"))["version"])')" == "$version" ]] ||
    { echo "MISMATCH $PACKAGE_JSON version (expected $version)" >&2; fail=1; }
  grep -qE "^#define APP_VERSION_STR \"${version}\"$" "$MAIN_C" ||
    { echo "MISMATCH $MAIN_C APP_VERSION_STR (expected $version)" >&2; fail=1; }

  if [[ "$fail" -ne 0 ]]; then
    echo "Version files are out of sync. Fix with: ./scripts/version.sh set $version" >&2
    exit 1
  fi
  echo "Version OK: $version (versionCode $code)"
}

case "${1:-print}" in
  print) current ;;
  --check | check) check ;;
  set)
    [[ $# -eq 2 ]] || { echo "usage: version.sh set X.Y.Z" >&2; exit 2; }
    valid "$2" || { echo "not semver: $2" >&2; exit 2; }
    sync_files "$2"
    echo "$2"
    ;;
  bump)
    level="${2:-patch}"
    IFS=. read -r major minor patch <<<"$(current)"
    case "$level" in
      major) major=$((major + 1)); minor=0; patch=0 ;;
      minor) minor=$((minor + 1)); patch=0 ;;
      patch) patch=$((patch + 1)) ;;
      *) echo "usage: version.sh bump [major|minor|patch]" >&2; exit 2 ;;
    esac
    next="${major}.${minor}.${patch}"
    sync_files "$next"
    echo "$next"
    ;;
  *)
    echo "usage: version.sh [print|--check|bump LEVEL|set X.Y.Z]" >&2
    exit 2
    ;;
esac
