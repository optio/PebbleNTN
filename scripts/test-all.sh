#!/usr/bin/env bash
# Run all offline validation: spec assets, rules, Android unit tests + lint.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Spec asset integrity"
"$REPO_ROOT/scripts/validate-spec-assets.sh"

echo "==> Generated protocol constants up to date"
"$REPO_ROOT/scripts/generate-protocol.sh" --check

echo "==> Version sync (VERSION vs gradle/package.json/main.c)"
"$REPO_ROOT/scripts/version.sh" --check

echo "==> Watchapp C unit tests"
"$REPO_ROOT/scripts/test-watchapp-unit.sh"

echo "==> Rule schema validation"
"$REPO_ROOT/scripts/validate-rules.sh"

echo "==> Rule regression (rule-workbench)"
python3 "$REPO_ROOT/tools/rule-workbench/workbench.py" regression

echo "==> Route-capture harness unit tests"
python3 -m unittest discover -s "$REPO_ROOT/tools/route-capture/tests"

echo "==> GitHub workflows parse as YAML"
if python3 -c "import yaml" 2>/dev/null; then
  python3 - "$REPO_ROOT"/.github/workflows/*.yml <<'PY'
import sys
import yaml
for path in sys.argv[1:]:
    with open(path, encoding="utf-8") as handle:
        yaml.safe_load(handle)
    print(f"ok {path}")
PY
else
  echo "SKIP: PyYAML is not installed for python3; the workflows were not parsed."
fi

# Play store graphics (REQ-ANDROID-022): Pillow is needed. The system Python usually has none, so
# fall back to the pebble tool's Python (named on the first line of the `pebble` launcher).
echo "==> Play store graphics (sizes and ratios)"
graphics_python=""
for candidate in python3 "$(head -1 "$(command -v pebble 2>/dev/null || echo /dev/null)" 2>/dev/null | sed -n 's/^#!//p')"; do
  if [ -n "$candidate" ] && "$candidate" -c "import PIL" 2>/dev/null; then
    graphics_python="$candidate"
    break
  fi
done
if [ -n "$graphics_python" ]; then
  "$graphics_python" -m unittest discover -s "$REPO_ROOT/tools" -p 'test_play_store_graphics.py'
  "$graphics_python" "$REPO_ROOT/tools/play_store_graphics.py" --check
else
  echo "SKIP: no Python with Pillow found (python3 or the pebble tool's); the store graphics were not checked."
fi

echo "==> Android unit tests + lint"
"$REPO_ROOT/android/gradlew" -p "$REPO_ROOT/android" test lint

echo "All tests finished."
