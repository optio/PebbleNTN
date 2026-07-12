#!/usr/bin/env bash
# Validate all bundled/example rulesets against schemas/ruleset.schema.json.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 "$REPO_ROOT/scripts/validate_catalog.py"
python3 "$REPO_ROOT/scripts/validate_rules.py" "$@"
