#!/usr/bin/env python3
"""Validate PebbleNTN ruleset JSON files against schemas/ruleset.schema.json.

M0 scope: JSON well-formedness + JSON Schema (draft 2020-12) validation of every discovered
ruleset. Canonicalization, operator/extractor semantics and fixture regression are added by the
Kotlin rule engine in M4; this script keeps the bundled/example rulesets schema-valid until then.
"""
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
SCHEMA_PATH = REPO_ROOT / "schemas" / "ruleset.schema.json"

# Directories/files that contain full rulesets to validate.
RULESET_GLOBS = [
    "examples/example-ruleset.json",
    "rules/bundled/**/*.json",
]


def discover_targets() -> list[Path]:
    targets: list[Path] = []
    for pattern in RULESET_GLOBS:
        if "*" in pattern:
            targets.extend(sorted(REPO_ROOT.glob(pattern)))
        else:
            p = REPO_ROOT / pattern
            if p.exists():
                targets.append(p)
    return targets


def main() -> int:
    if not SCHEMA_PATH.exists():
        print(f"ERROR: schema not found at {SCHEMA_PATH}", file=sys.stderr)
        return 2

    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    validator = Draft202012Validator(schema)

    targets = discover_targets()
    if not targets:
        print("No ruleset files found to validate.")
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
            rule_count = len(data.get("rules", []))
            print(f"OK       {rel} ({rule_count} rule(s))")

    if failed:
        print("Rule validation FAILED.", file=sys.stderr)
        return 1
    print("Rule validation OK.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
