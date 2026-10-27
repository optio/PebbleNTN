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

    if not check_catalog_consistency():
        return 1

    if not check_manifest_queries():
        return 1

    print("Catalog validation OK.")
    return 0


CATALOG_PATH = REPO_ROOT / "rules" / "catalog" / "navigation-apps.json"
BUNDLED_DIR = REPO_ROOT / "rules" / "bundled"


def rule_family_errors(apps: list[dict], bundled_app_dirs: set[str]) -> list[str]:
    """What the schema cannot express about rule families (#113), mirroring NavigationAppCatalog.parse.

    `rulesFrom` names another entry that has its own rules and no `rulesFrom` itself (no chains), and
    an entry using another's rules has no `rules/bundled/<appId>/` folder of its own. A package belongs
    to exactly one entry, so every package has exactly one rule family.
    """
    errors = []
    by_id = {app.get("appId"): app for app in apps}
    seen_packages: dict[str, str] = {}
    for app in apps:
        app_id = app.get("appId")
        for pkg in app.get("packageNames", []):
            if pkg in seen_packages:
                errors.append(f"{pkg} is claimed by both {seen_packages[pkg]} and {app_id}")
            seen_packages[pkg] = app_id
        source = app.get("rulesFrom")
        if source is None:
            continue
        target = by_id.get(source)
        if source == app_id:
            errors.append(f"{app_id}: rulesFrom names itself")
        elif target is None:
            errors.append(f"{app_id}: rulesFrom names unknown app {source}")
        elif target.get("rulesFrom") is not None:
            errors.append(f"{app_id}: rulesFrom {source} uses another app's rules itself (no chains)")
        elif not target.get("hasOfficialRules"):
            errors.append(f"{app_id}: rulesFrom {source} has no rules of its own")
        if app_id in bundled_app_dirs:
            errors.append(f"{app_id}: uses {source}'s rules, so rules/bundled/{app_id}/ must not exist")
    return errors


def check_catalog_consistency() -> bool:
    if not CATALOG_PATH.exists():
        return True
    apps = json.loads(CATALOG_PATH.read_text(encoding="utf-8")).get("apps", [])
    bundled = {p.name for p in BUNDLED_DIR.iterdir() if p.is_dir()} if BUNDLED_DIR.exists() else set()
    errors = rule_family_errors(apps, bundled)
    if errors:
        print("Catalog validation FAILED: rule families are inconsistent:", file=sys.stderr)
        for error in errors:
            print(f"  INVALID  {error}", file=sys.stderr)
        return False
    families = sum(1 for app in apps if app.get("rulesFrom"))
    print(f"OK       rule families consistent ({families} app(s) use another app's rules).")
    return True


# The bundled catalog and the Android manifest's <queries> must agree: on Android 11+ a package that
# is not declared in <queries> is invisible to getPackageInfo, so a catalog app missing there is
# never discovered as installed and its notifications are silently dropped.
MANIFEST_PATH = REPO_ROOT / "android" / "app" / "src" / "main" / "AndroidManifest.xml"
# Our own debug fixture publisher is visible via shared signing, so it does not need a <queries> row.
QUERIES_EXEMPT = {"com.pebblentn.fixturepublisher"}


def queried_catalog_packages(apps: list[dict]) -> set[str]:
    """The catalog packages that must be declared in the manifest <queries>."""
    return {pkg for app in apps for pkg in app.get("packageNames", [])} - QUERIES_EXEMPT


def missing_manifest_packages(apps: list[dict], manifest: str) -> list[str]:
    """Catalog packages the manifest <queries> does not declare, sorted."""
    declared = set(re.findall(r'<package\s+android:name="([^"]+)"', manifest))
    return sorted(queried_catalog_packages(apps) - declared)


def check_manifest_queries() -> bool:
    if not MANIFEST_PATH.exists() or not CATALOG_PATH.exists():
        return True  # Android app not present in this checkout; nothing to cross-check.

    apps = json.loads(CATALOG_PATH.read_text(encoding="utf-8")).get("apps", [])
    catalog_packages = queried_catalog_packages(apps)
    missing = missing_manifest_packages(apps, MANIFEST_PATH.read_text(encoding="utf-8"))
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
