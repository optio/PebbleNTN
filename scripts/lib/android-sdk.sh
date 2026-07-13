#!/usr/bin/env bash
# Shared helpers: locate the Android SDK and its tools without requiring them on PATH.
# Source this from a script; it defines android_sdk_root and find_sdk_tool.
# shellcheck shell=bash

# android_sdk_root <repo-root> -> prints the SDK path, or nothing if not found.
# Looks at ANDROID_HOME / ANDROID_SDK_ROOT, then android/local.properties (sdk.dir).
android_sdk_root() {
  local repo_root="$1"
  local root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$root" && -f "$repo_root/android/local.properties" ]]; then
    root="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$repo_root/android/local.properties" | tail -n1)"
  fi
  [[ -n "$root" && -d "$root" ]] && printf '%s' "$root"
}

# find_sdk_tool <tool> <sdk-subdir> <repo-root> -> prints the tool path, or returns 1.
# PATH wins; otherwise falls back to <sdk>/<subdir>/<tool>.
find_sdk_tool() {
  local tool="$1" subdir="$2" repo_root="$3"
  if command -v "$tool" >/dev/null 2>&1; then
    command -v "$tool"
    return 0
  fi
  local root
  root="$(android_sdk_root "$repo_root")"
  if [[ -n "$root" && -x "$root/$subdir/$tool" ]]; then
    printf '%s' "$root/$subdir/$tool"
    return 0
  fi
  return 1
}
