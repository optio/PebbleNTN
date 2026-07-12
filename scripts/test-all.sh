#!/usr/bin/env bash
# Run all offline validation: spec assets, rules, Android unit tests + lint.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Spec asset integrity"
"$REPO_ROOT/scripts/validate-spec-assets.sh"

echo "==> Rule schema validation"
"$REPO_ROOT/scripts/validate-rules.sh"

echo "==> Android unit tests + lint"
"$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/android" test lint

echo "All tests finished."
