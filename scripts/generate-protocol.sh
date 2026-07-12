#!/usr/bin/env bash
# Regenerate the read-only protocol constants (Kotlin + C) from the single JSON definition.
# Pass --check to verify committed generated files are current (used by CI / test-all.sh).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 "$REPO_ROOT/protocol/check_consistency.py"
python3 "$REPO_ROOT/protocol/generate.py" "$@"
