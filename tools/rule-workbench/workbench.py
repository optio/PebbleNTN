#!/usr/bin/env python3
"""PebbleNTN rule-workbench — a terminal tool for rule maintainers (REQ-DEBUG-008).

Subcommands:
  inspect   <ruleset.json>                 Summarize a ruleset.
  validate  <ruleset.json>                 Validate against schemas/ruleset.schema.json.
  diff      <a.json> <b.json>              Show added/removed/changed rules by id.
  scaffold  <id> --package <pkg>           Print a new-rule JSON template.
  sanitize  <export.json> [-o out.json]    Redact a full-diagnostic export's notification text.
  promote   <rule.json> <ruleset.json>     Insert/replace a rule in a ruleset (canonical order).
  test      <ruleset.json> <fixtures.json> Run fixtures through the engine subset; report pass/fail.
  regression                               Run the bundled Google Maps ruleset against its fixtures.

NOTE: `test` uses a documented *subset* re-implementation of the rule engine (regex + the operators
and extractors used by the bundled rules) for quick local iteration. The authoritative engine is the
Kotlin RuleEngine (GoogleMapsRulesRegressionTest); keep the two in sync.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SCHEMA = REPO_ROOT / "schemas" / "ruleset.schema.json"

# Maneuver/structural keywords kept by sanitize (mirrors the Kotlin Redactor).
KEEP = {
    "turn", "left", "right", "slight", "slightly", "sharp", "keep", "roundabout", "exit",
    "continue", "straight", "uturn", "u", "merge", "head", "go", "north", "south", "east", "west",
    "arrive", "arriving", "destination", "onto", "on", "in", "at", "take", "the", "toward",
    "towards", "now", "km", "m", "mi", "ft", "min", "mins", "minute", "minutes", "sec", "secs",
    "s", "h", "hr", "st", "nd", "rd", "th",
}
PLACEHOLDER = "▮"


def load(path: str) -> dict:
    return json.loads(Path(path).read_text(encoding="utf-8"))


# --- inspect / validate / diff -----------------------------------------------------------------

def cmd_inspect(args) -> int:
    ruleset = load(args.ruleset)
    rules = ruleset.get("rules", [])
    print(f"rulesetVersion: {ruleset.get('rulesetVersion')}")
    print(f"publisher:      {ruleset.get('publisher')}")
    print(f"rules:          {len(rules)}")
    for rule in rules:
        maneuver = (rule.get("output", {}).get("maneuver", {}) or {}).get("value", "-")
        pkgs = ",".join(rule.get("packageNames", []))
        locales = ",".join(rule.get("locales", [])) or "*"
        print(f"  [{rule.get('priority'):>4}] {rule.get('id'):40} {locales:8} {maneuver:14} {pkgs}")
    return 0


def cmd_validate(args) -> int:
    try:
        from jsonschema import Draft202012Validator
    except ImportError:
        print("ERROR: pip install jsonschema", file=sys.stderr)
        return 2
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
    validator = Draft202012Validator(schema)
    data = load(args.ruleset)
    errors = sorted(validator.iter_errors(data), key=lambda e: list(e.path))
    if errors:
        for err in errors:
            loc = "/".join(str(p) for p in err.path) or "<root>"
            print(f"INVALID at {loc}: {err.message}", file=sys.stderr)
        return 1
    print(f"OK ({len(data.get('rules', []))} rules)")
    return 0


def cmd_diff(args) -> int:
    a = {r["id"]: r for r in load(args.a).get("rules", [])}
    b = {r["id"]: r for r in load(args.b).get("rules", [])}
    for rid in sorted(set(a) | set(b)):
        if rid not in a:
            print(f"+ {rid}")
        elif rid not in b:
            print(f"- {rid}")
        elif a[rid] != b[rid]:
            print(f"~ {rid}")
    return 0


def cmd_scaffold(args) -> int:
    rule = {
        "id": args.id,
        "enabled": True,
        "priority": 100,
        "packageNames": [args.package],
        "locales": ["en"],
        "conditions": [{"field": "combinedText", "operator": "regex", "value": "(?i)\\bturn\\s+right\\b"}],
        "output": {
            "maneuver": {"type": "literal", "value": "RIGHT"},
            "distanceMeters": {"type": "distance", "field": "combinedText"},
            "primaryText": {"type": "firstNonEmpty", "fields": ["title", "text"]},
        },
    }
    print(json.dumps(rule, indent=2))
    return 0


# --- sanitize ----------------------------------------------------------------------------------

def redact_text(value):
    if not isinstance(value, str):
        return value
    return re.sub(r"[^\W\d_]+", lambda m: m.group(0) if m.group(0).lower() in KEEP else PLACEHOLDER, value, flags=re.UNICODE)


def cmd_sanitize(args) -> int:
    data = load(args.export)
    text_fields = ("title", "text", "subText", "bigText", "summaryText", "infoText")
    for event in data.get("events", []):
        snap = event.get("snapshot")
        if isinstance(snap, dict):
            for field in text_fields:
                if field in snap:
                    snap[field] = redact_text(snap[field])
    out = json.dumps(data, indent=2, ensure_ascii=False)
    if args.out:
        Path(args.out).write_text(out + "\n", encoding="utf-8")
        print(f"wrote {args.out}")
    else:
        print(out)
    return 0


# --- promote -----------------------------------------------------------------------------------

def cmd_promote(args) -> int:
    rule = load(args.rule)
    ruleset = load(args.ruleset)
    rules = [r for r in ruleset.get("rules", []) if r.get("id") != rule.get("id")]
    rules.append(rule)
    rules.sort(key=lambda r: (r.get("packageNames", [""])[0], -r.get("priority", 0), r.get("id", "")))
    ruleset["rules"] = rules
    print(json.dumps(ruleset, indent=2, ensure_ascii=False))
    return 0


# --- test / regression (subset engine) ---------------------------------------------------------

def combined_text(snap: dict) -> str:
    parts = [snap.get(f) for f in ("title", "text", "bigText", "subText", "summaryText", "infoText")]
    return " ".join(p for p in parts if p)


def field_value(snap: dict, name: str):
    if name == "combinedText":
        return combined_text(snap)
    return snap.get(name)


def eval_condition(cond: dict, snap: dict) -> bool:
    value = field_value(snap, cond["field"])
    op = cond["operator"]
    target = cond.get("value")
    if op == "exists":
        return bool(value)
    if op == "notExists":
        return not value
    if value is None:
        return False
    if op == "equals":
        return value == target
    if op == "equalsIgnoreCase":
        return value.lower() == (target or "").lower()
    if op == "contains":
        return target in value
    if op == "containsIgnoreCase":
        return (target or "").lower() in value.lower()
    if op == "startsWith":
        return value.startswith(target or "")
    if op == "endsWith":
        return value.endswith(target or "")
    if op == "in":
        return value in cond.get("values", [])
    if op == "regex":
        return re.search(target, value[:4096]) is not None
    return False


def parse_distance(text: str):
    # The unit is mandatory — see DistanceParser.kt. A bare number is usually the ETA hour, a road
    # number or an exit number, not a distance.
    m = re.search(r"(\d+(?:[.,]\d+)?)\s*(km|mi|ft|m)\b", text, re.IGNORECASE)
    if not m:
        return None
    num = float(m.group(1).replace(",", "."))
    unit = m.group(2).lower()
    meters = {"km": num * 1000, "mi": num * 1609.344, "ft": num * 0.3048}.get(unit, num)
    return max(0, round(meters))


def run_extractor(ext: dict, snap: dict):
    if ext is None:
        return None
    t = ext["type"]
    if t == "literal":
        return ext["value"]
    if t == "fieldCopy":
        return field_value(snap, ext["field"]) or None
    if t == "firstNonEmpty":
        for f in ext["fields"]:
            v = field_value(snap, f)
            if v:
                return v
        return None
    if t == "distance":
        v = field_value(snap, ext["field"])
        return parse_distance(v) if v else None
    if t == "regexCapture":
        v = field_value(snap, ext["field"])
        if not v:
            return None
        m = re.search(ext["pattern"], v)
        if not m:
            return None
        try:
            return m.group(ext.get("group", 1))
        except (IndexError, re.error):
            return None
    if t == "maneuverMap":
        v = field_value(snap, ext["field"])
        mapping = ext.get("mapping", {})
        return mapping.get(v, ext.get("default"))
    return None


def evaluate(snap: dict, rules: list, locale: str):
    ordered = sorted(rules, key=lambda r: (-r.get("priority", 0), r.get("id", "")))
    for rule in ordered:
        if snap.get("packageName") not in rule.get("packageNames", []):
            continue
        if not rule.get("enabled", True):
            continue
        locs = rule.get("locales", [])
        if locs and (locale or "").split("-")[0] not in locs:
            continue
        if all(eval_condition(c, snap) for c in rule.get("conditions", [])):
            out = rule.get("output", {})
            return {
                "ruleId": rule["id"],
                "maneuver": run_extractor(out.get("maneuver"), snap) or "UNKNOWN",
                "distanceMeters": run_extractor(out.get("distanceMeters"), snap),
                "primaryText": run_extractor(out.get("primaryText"), snap),
                "secondaryText": run_extractor(out.get("secondaryText"), snap),
            }
    return None


def check_fixture(result: dict | None, expected: dict) -> str | None:
    """Return None when the result matches `expected`, else a short reason.

    Mirrors GoogleMapsRulesRegressionTest so the two engines are held to the same contract:
    `matched: false` asserts that *nothing* matches (a notification carrying no maneuver, such as
    "Starting navigation…", must not put an arrow on the watch), and `ruleId` pins which rule wins,
    not merely the maneuver it produced.
    """
    if not expected.get("matched", True):
        return None if result is None else f"expected no match, got {result['ruleId']}"
    if result is None:
        return "expected a match, got none"
    for field in ("maneuver", "distanceMeters", "secondaryText", "ruleId"):
        if field in expected and result.get(field) != expected[field]:
            return f"{field}: expected {expected[field]!r}, got {result.get(field)!r}"
    return None


def run_fixtures(rules: list, fixtures: list) -> int:
    failures = 0
    for fx in fixtures:
        snap = dict(fx["snapshot"])
        snap["packageName"] = fx["packageName"]
        result = evaluate(snap, rules, fx.get("locale", "en"))
        reason = check_fixture(result, fx["expected"])
        if reason is None:
            print(f"  PASS  {fx['name']:32} -> {result}")
        else:
            failures += 1
            print(f"  FAIL  {fx['name']:32} -> {reason}")
    total = len(fixtures)
    print(f"{total - failures}/{total} passed")
    return 1 if failures else 0


def cmd_test(args) -> int:
    rules = load(args.ruleset).get("rules", [])
    fixtures = load(args.fixtures).get("fixtures", [])
    return run_fixtures(rules, fixtures)


def cmd_regression(args) -> int:
    rules = load(str(REPO_ROOT / "rules" / "bundled" / "google-maps.json")).get("rules", [])
    fixtures = load(str(REPO_ROOT / "rules" / "fixtures" / "google-maps.json")).get("fixtures", [])
    print("Google Maps regression:")
    return run_fixtures(rules, fixtures)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("inspect"); p.add_argument("ruleset"); p.set_defaults(func=cmd_inspect)
    p = sub.add_parser("validate"); p.add_argument("ruleset"); p.set_defaults(func=cmd_validate)
    p = sub.add_parser("diff"); p.add_argument("a"); p.add_argument("b"); p.set_defaults(func=cmd_diff)
    p = sub.add_parser("scaffold"); p.add_argument("id"); p.add_argument("--package", required=True); p.set_defaults(func=cmd_scaffold)
    p = sub.add_parser("sanitize"); p.add_argument("export"); p.add_argument("-o", "--out"); p.set_defaults(func=cmd_sanitize)
    p = sub.add_parser("promote"); p.add_argument("rule"); p.add_argument("ruleset"); p.set_defaults(func=cmd_promote)
    p = sub.add_parser("test"); p.add_argument("ruleset"); p.add_argument("fixtures"); p.set_defaults(func=cmd_test)
    p = sub.add_parser("regression"); p.set_defaults(func=cmd_regression)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
