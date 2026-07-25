#!/usr/bin/env python3
"""Validate the bundled navigation-app catalog against schemas/app-catalog.schema.json."""
from __future__ import annotations

import json
import re
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

    if not check_manifest_queries():
        return 1

    print("Catalog validation OK.")
    return 0


# The bundled catalog and the Android manifest's <queries> must agree: on Android 11+ a package that
# is not declared in <queries> is invisible to getPackageInfo, so a catalog app missing there is
# never discovered as installed and its notifications are silently dropped.
MANIFEST_PATH = REPO_ROOT / "android" / "app" / "src" / "main" / "AndroidManifest.xml"
# Our own debug fixture publisher is visible via shared signing, so it does not need a <queries> row.
QUERIES_EXEMPT = {"com.pebblentn.fixturepublisher"}


def check_manifest_queries() -> bool:
    catalog_path = REPO_ROOT / "rules" / "catalog" / "navigation-apps.json"
    if not MANIFEST_PATH.exists() or not catalog_path.exists():
        return True  # Android app not present in this checkout; nothing to cross-check.

    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    catalog_packages = {
        pkg
        for app in catalog.get("apps", [])
        for pkg in app.get("packageNames", [])
    } - QUERIES_EXEMPT

    manifest = MANIFEST_PATH.read_text(encoding="utf-8")
    declared = set(re.findall(r'<package\s+android:name="([^"]+)"', manifest))

    missing = sorted(catalog_packages - declared)
    if missing:
        print("Catalog validation FAILED: packages missing from AndroidManifest <queries>:", file=sys.stderr)
        for pkg in missing:
            print(f"  MISSING  {pkg}", file=sys.stderr)
        print("  Add each to android/app/src/main/AndroidManifest.xml so it is detectable.", file=sys.stderr)
        return False
    print(f"OK       AndroidManifest <queries> covers all {len(catalog_packages)} catalog packages.")
    return True


if __name__ == "__main__":
    raise SystemExit(main())
