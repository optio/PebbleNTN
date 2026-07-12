#!/usr/bin/env python3
"""Validate the bundled navigation-app catalog against schemas/app-catalog.schema.json."""
from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    from jsonschema import Draft202012Validator
except ImportError:  # pragma: no cover
    print("ERROR: python 'jsonschema' package is required (pip install jsonschema).", file=sys.stderr)
    sys.exit(2)

REPO_ROOT = Path(__file__).resolve().parent.parent
SCHEMA_PATH = REPO_ROOT / "schemas" / "app-catalog.schema.json"
CATALOG_GLOB = "rules/catalog/*.json"


def main() -> int:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    validator = Draft202012Validator(schema)

    targets = sorted(REPO_ROOT.glob(CATALOG_GLOB))
    if not targets:
        print("No catalog files found to validate.")
        return 0

    failed = False
    for path in targets:
        rel = path.relative_to(REPO_ROOT)
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            print(f"INVALID  {rel}: not valid JSON: {exc}", file=sys.stderr)
            failed = True
            continue
        errors = sorted(validator.iter_errors(data), key=lambda e: list(e.path))
        if errors:
            failed = True
            for err in errors:
                location = "/".join(str(p) for p in err.path) or "<root>"
                print(f"INVALID  {rel} at {location}: {err.message}", file=sys.stderr)
        else:
            print(f"OK       {rel} ({len(data.get('apps', []))} app(s))")

    if failed:
        print("Catalog validation FAILED.", file=sys.stderr)
        return 1
    print("Catalog validation OK.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
