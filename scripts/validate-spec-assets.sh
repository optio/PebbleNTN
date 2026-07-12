#!/usr/bin/env bash
# Verify that spec/requirement/example/schema assets match their recorded SHA-256 hashes.
# The manifest (MANIFEST.sha256.json) is the integrity record for the specification contract;
# a mismatch means a protected asset was changed and must be reviewed, not silently accepted.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

MANIFEST="MANIFEST.sha256.json"
if [[ ! -f "$MANIFEST" ]]; then
  echo "ERROR: $MANIFEST not found" >&2
  exit 1
fi

fail=0
count=0
while IFS=$'\t' read -r path expected; do
  count=$((count + 1))
  if [[ ! -f "$path" ]]; then
    echo "MISSING  $path" >&2
    fail=1
    continue
  fi
  actual="$(sha256sum "$path" | awk '{print $1}')"
  if [[ "$actual" != "$expected" ]]; then
    echo "MISMATCH $path" >&2
    echo "         expected $expected" >&2
    echo "         actual   $actual" >&2
    fail=1
  fi
done < <(jq -r 'to_entries[] | "\(.key)\t\(.value)"' "$MANIFEST")

if [[ "$fail" -ne 0 ]]; then
  echo "Spec asset validation FAILED." >&2
  exit 1
fi
echo "Spec asset validation OK ($count files)."
