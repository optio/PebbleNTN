#!/usr/bin/env bash
# Point git at the repo's tracked hooks (.githooks/). Run once per clone.
#
# Hooks live in the repo so they are reviewed like any other code; git needs to be told to use them
# because it defaults to .git/hooks, which is not tracked.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

chmod +x .githooks/*
git config core.hooksPath .githooks

echo "Git hooks installed (core.hooksPath = .githooks)."
echo
echo "pre-commit runs: secret scan, spec-asset integrity, generated-protocol check,"
echo "rule schema + regression, shell/YAML/Python/JSON syntax, and — when android/ is"
echo "touched — unit tests + lint."
echo
echo "  git commit --no-verify     skip the hook"
echo "  PEBBLENTN_SKIP_GRADLE=1    skip only the slow Android step"
echo
echo "Uninstall with: git config --unset core.hooksPath"
