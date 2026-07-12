#!/usr/bin/env bash
# Build the Pebble watchapp (.pbw) with the pebble CLI.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v pebble >/dev/null 2>&1; then
  echo "ERROR: 'pebble' CLI not found. Install the Pebble SDK/tool first." >&2
  exit 1
fi

cd "$REPO_ROOT/watchapp"
pebble build
echo "Watchapp built: $REPO_ROOT/watchapp/build/watchapp.pbw"
