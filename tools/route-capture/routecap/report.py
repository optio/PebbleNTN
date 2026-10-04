"""Evaluate captured notifications with the bundled rules and summarise what was missed.

Uses the rule-workbench engine (tools/rule-workbench/workbench.py), the same one the fixture
regression runs, so a capture counts as recognised here exactly when a fixture with it would pass.
"""
from __future__ import annotations

import json
import re
import sys
from collections import OrderedDict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO_ROOT / "tools" / "rule-workbench"))
import workbench  # noqa: E402

HEURISTIC_KT = REPO_ROOT / "android/app/src/main/java/com/pebblentn/app/notification/ManeuverHeuristic.kt"
_CLOCK = re.compile(r"\b\d{1,2}:\d{2}\b")
_WORD = re.compile(r"[^\W\d_]+")


def _maneuver_words() -> frozenset[str]:
    """The app's own maneuver keywords, read from ManeuverHeuristic.kt so the two never drift."""
    source = HEURISTIC_KT.read_text(encoding="utf-8")
    block = source[source.index("MANEUVER_WORDS"):]
    block = block[block.index("setOf(") + len("setOf("):block.index("\n    )")]
    return frozenset(re.findall(r'"([^"]+)"', block))


def _status_card() -> re.Pattern:
    """The app's route-status-card pattern ("Rerouting..."), read from ManeuverHeuristic.kt."""
    source = HEURISTIC_KT.read_text(encoding="utf-8")
    start = source.index("statusCard")
    call = source[start:source.index("RegexOption", start)]
    return re.compile("".join(re.findall(r'"""(.*?)"""', call, re.S)), re.I)  # parts joined by +


MANEUVER_WORDS = _maneuver_words()
_STATUS = _status_card()


_DISTANCE = r"^\s*\d+(?:[.,]\d+)?\s*(?:km|m|mi|ft|pi|yd|公尺|公里|米|千米)"
_DISTANCE_TITLE = re.compile(_DISTANCE + r"\s*[·•]\s*\S", re.I)
_DISTANCE_ONLY = re.compile(_DISTANCE + r"\s*$", re.I)


def is_instruction(capture: dict) -> bool:
    """Navigation has really started: a direction-like card, or one whose title leads with a
    distance and an instruction ("200 m • Avancez"), or is only a distance above a road line
    (Organic Maps / CoMaps: "1.1 km" / "Glinkastraße"), whatever its words."""
    title = (capture.get("title") or "").replace("\u00a0", " ")
    return (looks_like_direction(capture) or bool(_DISTANCE_TITLE.search(title))
            or (bool(_DISTANCE_ONLY.search(title)) and bool((capture.get("text") or "").strip())))


def looks_like_direction(capture: dict) -> bool:
    """Mirror of the app's ManeuverHeuristic: an unmatched card counts as a missing rule only if it
    plausibly carries a turn instruction (an ETA clock time or a maneuver word, and not a route
    status card such as "Rerouting..."); otherwise it is "not a direction", like in Debug history."""
    text = " ".join(capture.get(f) or "" for f in ("title", "text", "bigText", "subText"))
    if not text.strip() or _STATUS.search(text):
        return False
    return bool(_CLOCK.search(text)) or any(w in MANEUVER_WORDS for w in _WORD.findall(text.lower()))


def catalog() -> dict:
    return json.loads((REPO_ROOT / "rules" / "catalog" / "navigation-apps.json").read_text())


def app_for_package(package: str) -> dict | None:
    return next((a for a in catalog()["apps"] if package in a["packageNames"]), None)


def bundled_rules(app_id: str) -> list:
    rules = []
    for path in sorted((REPO_ROOT / "rules" / "bundled" / app_id).glob("*.json")):
        rules.extend(workbench.load(str(path)).get("rules", []))
    return rules


def snapshot(capture: dict) -> dict:
    return {"packageName": capture["packageName"], **{f: capture.get(f) for f in ("title", "text", "subText", "bigText")}}


def instruction(text: str) -> str:
    """The instruction words of a step, with the road name cut off: words up to the first capitalised
    word after the first one ('Vire à esquerda na Rue de l'Hôpital' -> 'Vire à esquerda na …'). Road
    names start with a capital in the languages Google Maps writes; instruction words mostly don't."""
    words = text.split()
    for i, w in enumerate(words[1:], start=1):
        if w[:1].isupper():
            return " ".join(words[:i] + ["…"])
    return text


def shape(capture: dict) -> str:
    """A card's layout with numbers and the road name abstracted, so similar cards group together."""
    raw = capture.get("title") or "∅"
    head, sep, rest = raw.partition(" · ")
    raw = f"{head}{sep}{instruction(rest)}" if sep else instruction(raw)
    title = re.sub(r"\d+(?:[.,]\d+)?", "N", raw)
    text = "<text>" if capture.get("text") else "∅"
    sub = re.sub(r"\d+(?:[.,]\d+)?", "N", capture.get("subText") or "∅")
    return f"title «{title}» · text {text} · subText «{sub}» · {capture.get('template') or '-'}"


# Fallback rules match a card shape loosely (e.g. any "<distance> · …" title near the destination), so
# what they match is listed for review: an unknown phrasing must not hide behind them.
FALLBACK_RULES = ("google-maps-destination-approach-",)


# A report state's parts, each keyed by scenario id. "known" holds unmatched cards a fixture pins as
# deliberately unshown.
STATE_KEYS = ("scenarios", "shapes", "notDirections", "fallback", "known")


def _numberless(value) -> str:
    return re.sub(r"\d+(?:[.,]\d+)?", "N", (value or "").replace("\u00a0", " "))


def known_unmatched(app_id: str) -> list[dict]:
    """The snapshots of the app's `matched: false` fixtures (rules/fixtures/<app>.json): cards
    deliberately left unshown, like OsmAnd's "0 m • " at the maneuver point."""
    path = REPO_ROOT / "rules" / "fixtures" / f"{app_id}.json"
    if not path.exists():
        return []
    return [f["snapshot"] for f in json.loads(path.read_text())["fixtures"]
            if f.get("expected", {}).get("matched") is False and f.get("snapshot")]


def is_known_unmatched(capture: dict, known: list[dict]) -> bool:
    """Whether a fixture already pins this card as unmatched: every field the fixture sets equals the
    capture's, numbers aside (so "0 m • " covers "40 m • " too)."""
    return any(all(_numberless(capture.get(k)) == _numberless(v) for k, v in snap.items()) for snap in known)


def evaluate(run: dict) -> dict:
    """The run as a report state: per scenario, its counts and its unrecognised / not-a-direction
    card shapes. States merge per scenario id (see merge), so an issue can keep the latest result of
    every scenario across runs."""
    rules_by_app: dict[str, list] = {}
    known_by_app: dict[str, list] = {}
    state = {k: {} for k in STATE_KEYS}
    for sc in run["scenarios"]:
        rules = rules_by_app.setdefault(sc["app"], bundled_rules(sc["app"]))
        known = known_by_app.setdefault(sc["app"], known_unmatched(sc["app"]))
        matched = 0
        buckets = {k: OrderedDict() for k in STATE_KEYS if k != "scenarios"}
        for cap in sc["captures"]:
            result = workbench.evaluate(snapshot(cap), rules, sc["locale"])
            cap["matchedRuleId"] = result["ruleId"] if result else None
            if result:
                matched += 1
                if result["ruleId"].startswith(FALLBACK_RULES):
                    entry = buckets["fallback"].setdefault(shape(cap), {"shape": shape(cap), "count": 0, "example": snapshot(cap), "rule": result["ruleId"]})
                    entry["count"] += 1
                continue
            kind = ("notDirections" if not looks_like_direction(cap)
                    else "known" if is_known_unmatched(cap, known) else "shapes")
            bucket = buckets[kind]
            entry = bucket.setdefault(shape(cap), {"shape": shape(cap), "count": 0, "example": snapshot(cap)})
            entry["count"] += 1
        state["scenarios"][sc["id"]] = {
            "id": sc["id"], "app": sc["app"], "locale": sc["locale"], "mode": sc["mode"], "route": sc["route"],
            "appVersion": sc.get("appVersion"), "total": len(sc["captures"]), "matched": matched,
            "run": run["startedAt"], "android": run.get("android"), "image": run.get("image"),
        }
        for k, bucket in buckets.items():
            state[k][sc["id"]] = list(bucket.values())
    return state


def merge(old: dict | None, new: dict) -> dict:
    """`old` updated with `new`: scenarios in `new` replace those with the same id; others stay."""
    merged = {k: dict((old or {}).get(k, {})) for k in STATE_KEYS}
    for k in merged:
        merged[k].update(new.get(k, {}))
    return merged


def reclassify(state: dict, app_id: str) -> dict:
    """Move stored unrecognised shapes that a `matched: false` fixture now pins into "known", so a
    fixture added after a run takes effect on the issue without rerunning every scenario."""
    known = known_unmatched(app_id)
    state.setdefault("known", {})
    for sid, entries in state["shapes"].items():
        keep = []
        for e in entries:
            (state["known"].setdefault(sid, []) if is_known_unmatched(e["example"], known) else keep).append(e)
        state["shapes"][sid] = keep
    return state


def unrecognised(state: dict) -> list[dict]:
    """Unrecognised shapes across all scenarios: count, one example, and where they appeared."""
    out: "OrderedDict[str, dict]" = OrderedDict()
    for sid in sorted(state["shapes"]):
        for e in state["shapes"][sid]:
            agg = out.setdefault(e["shape"], {"shape": e["shape"], "count": 0, "example": e["example"], "scenarios": []})
            agg["count"] += e["count"]
            agg["scenarios"].append(sid)
    return list(out.values())


def markdown(app_name: str, state: dict) -> str:
    """The issue body / report: every scenario's latest result and what is still unrecognised."""
    shapes = unrecognised(state)
    lines = [
        f"Unrecognised **{app_name}** navigation notifications found by the route-capture harness (#55). "
        "Routes are synthetic, along public streets, so the examples contain no personal data. "
        "Each scenario shows its latest run.",
        "",
        "| Scenario | Language | Mode | Route | App version | Last run | Captured | Recognised |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for s in sorted(state["scenarios"].values(), key=lambda s: s["id"]):
        lines.append(f"| `{s['id']}` | {s['locale']} | {s['mode']} | {s['route']} | {s['appVersion'] or '?'} | "
                     f"{s['run']} (Android {s.get('android') or '?'}) | {s['total']} | {s['matched']} |")
    ignored = [e for sid in sorted(state["notDirections"]) for e in state["notDirections"][sid]]
    if ignored:
        titles = sorted({e["example"].get("title") or "" for e in ignored})
        lines += ["", f"Not counted: {sum(e['count'] for e in ignored)} notification(s) that aren't directions, "
                  f"by the app's own check ({', '.join('`' + t + '`' for t in titles[:5])})."]
    known = [e for sid in sorted(state.get("known", {})) for e in state["known"][sid]]
    if known:
        titles = sorted({e["example"].get("title") or "" for e in known})
        lines += ["", f"Left unshown on purpose: {sum(e['count'] for e in known)} notification(s) a rule fixture already "
                  f"pins as unmatched ({', '.join('`' + t + '`' for t in titles[:5])})."]
    fallback = [e for sid in sorted(state.get("fallback", {})) for e in state["fallback"][sid]]
    if fallback:
        lines += ["", "Recognised only by a fallback rule, worth a look (an unknown phrasing would land here too): "
                  + "; ".join(f"{e['count']}× `{e['example'].get('title')}` ({e['rule']})" for e in fallback[:10]) + "."]
    lines += ["", f"## Unrecognised card shapes ({len(shapes)})", ""]
    if not shapes:
        lines.append("None: every captured notification that looks like a direction matched a rule.")
    for u in shapes:
        ex = u["example"]
        lines += [
            f"- **{u['count']}×** {u['shape']}",
            f"  - example: title `{ex.get('title')}` · text `{ex.get('text')}` · subText `{ex.get('subText')}`",
            f"  - scenarios: {', '.join('`' + s + '`' for s in u['scenarios'])}",
        ]
    lines += ["", "Reproduce: `tools/route-capture/routecap.py run <scenario file>` (see tools/route-capture/README.md)."]
    return "\n".join(lines)
