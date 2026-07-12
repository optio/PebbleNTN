#!/usr/bin/env python3
"""Ensure protocol/protocol-definition.json stays consistent with the spec example asset.

examples/protocol-definition.json is part of the specification contract (integrity-protected by
MANIFEST.sha256.json) and must not be edited. The authoritative generator input
protocol/protocol-definition.json extends it (flag bits, maneuvers, error codes) but MUST agree on
every shared section: protocolMajor/Minor, keys, events. This guards against silent drift.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
AUTHORITATIVE = REPO_ROOT / "protocol" / "protocol-definition.json"
EXAMPLE = REPO_ROOT / "examples" / "protocol-definition.json"


def main() -> int:
    auth = json.loads(AUTHORITATIVE.read_text(encoding="utf-8"))
    example = json.loads(EXAMPLE.read_text(encoding="utf-8"))

    problems: list[str] = []
    for field in ("protocolMajor", "protocolMinor", "keys", "events"):
        if auth.get(field) != example.get(field):
            problems.append(
                f"'{field}' differs:\n  authoritative={auth.get(field)!r}\n  example={example.get(field)!r}"
            )

    if problems:
        print("Protocol definition INCONSISTENT with examples/protocol-definition.json:", file=sys.stderr)
        for p in problems:
            print("  - " + p, file=sys.stderr)
        return 1

    print("Protocol definition consistent with examples/protocol-definition.json.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
