#!/usr/bin/env python3
"""Generate localized Google Maps rulesets (rules/bundled/google-maps/<lang>.json).

Google Maps navigation notifications are localized, but the bundled rules only shipped English, so
non-English users matched nothing (diagnosed from a real Italian capture, 2026-07). This emits one
ruleset per language from a compact keyword table below, mirroring the structure of the hand-authored
en.json: the same priority ladder (arrive > roundabout > u-turn > sharp > slight > keep > turn >
continue) and the same output extractors — only the condition regexes change per language.

Design notes:
- Turn L/R is the catch-all (priority 100) and matches the localized "to the <side>" phrase, which
  avoids matching a road name that merely contains the bare direction word.
- Sharp / slight / keep are refinements above it; they match a modifier keyword AND the bare
  direction, order-independently (lookaheads), because phrasings vary ("scherpe bocht naar rechts"
  vs "rechts aanhouden"). If one misfires it degrades to a plain turn — still the right side.
- ARRIVE matches the TITLE only. Google Maps puts the ETA in subText on every notification, often
  with a localized "arrive" word, so matching combinedText would classify every turn as ARRIVE.
- ARRIVE sits at the BOTTOM of the ladder (40), unlike hand-authored en.json where it is on top.
  The localized arrive patterns are prefix stems (see below) matched against a title that also
  carries the destination road name, so they fire on ordinary road names: "Links abbiegen auf
  Zielstattstraße" (Munich), "Tournez à gauche sur Rue de l'Arrivée" (Paris). Tightening the stem
  cannot fix this — "Arrivée" is a well-formed inflection of the French stem — so instead a title
  carrying an explicit maneuver resolves as that maneuver, and only titles with no maneuver word at
  all fall through to ARRIVE. Real arrival phrasings ("Ziel erreicht", "Sei arrivato a
  destinazione") carry no maneuver word, so they still reach it.
- The ETA (secondaryText) is captured as the last clock time in subText, language-independently.

These are authored from documented Google Maps phrasings and MUST be validated against real captures
per language (especially sharp/slight/keep); run `share logs` from a device in that language.
"""

import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = REPO_ROOT / "rules" / "bundled" / "google-maps"
PACKAGE = "com.google.android.apps.maps"
RULESET_DATE = "2026.07.5"

# Per language: the regex fragments. `turn_*` are full "to the side" phrases; `right`/`left` are the
# bare direction tokens used by the sharp/slight/keep refinements; the rest are keyword alternations.
LANGS = {
    "it": {
        "name": "Italian",
        "turn_right": r"\ba\s+destra\b",
        "turn_left": r"\ba\s+sinistra\b",
        "right": r"destra",
        "left": r"sinistra",
        "roundabout": r"rotonda|rotatoria",
        "uturn": r"inversione|inverti\s+la\s+marcia",
        "sharp": r"netta|brusca|decisa|stretta|secca",
        "slight": r"leggermente",
        "keep": r"tieni|mantieni|mantenere",
        "straight": r"sempre\s+dritto|prosegui|continua|vai\s+dritto|dritto|diritto",
        # Prefix stems (no trailing \b): match arrivo / arrivato / arrivata / giunto.
        "arrive": r"arriv|giunt|destinazione",
    },
    "fr": {
        "name": "French",
        "turn_right": r"à\s+droite\b",
        "turn_left": r"à\s+gauche\b",
        "right": r"droite",
        "left": r"gauche",
        "roundabout": r"rond-?point|giratoire",
        "uturn": r"demi-tour|faites\s+demi-tour",
        "sharp": r"serré|serrée|franchement|prononcé|prononcée",
        "slight": r"légèrement|legerement",
        "keep": r"serrez|restez|maintenez",
        "straight": r"tout\s+droit|continuez|poursuivez|dirigez-vous|prenez\s+la\s+direction",
        "arrive": r"arriv|destination",
    },
    "es": {
        "name": "Spanish",
        "turn_right": r"\ba\s+la\s+derecha\b",
        "turn_left": r"\ba\s+la\s+izquierda\b",
        "right": r"derecha",
        "left": r"izquierda",
        "roundabout": r"rotonda|glorieta",
        "uturn": r"cambio\s+de\s+sentido|cambia\s+de\s+sentido|media\s+vuelta",
        "sharp": r"bruscamente|brusca|cerrada|cerrado|pronunciad",
        "slight": r"ligeramente|leve",
        "keep": r"mantente|mantén|manten|sigue\s+por\s+la",
        "straight": r"todo\s+recto|sigue\s+recto|recto|continúa|continua|dirígete|dirigete|ve\s+hacia",
        "arrive": r"llega|destino",
    },
    "de": {
        "name": "German",
        "turn_right": r"\brechts\b",
        "turn_left": r"\blinks\b",
        "right": r"rechts",
        "left": r"links",
        "roundabout": r"kreisverkehr|kreisel",
        "uturn": r"wenden|u-?turn|umkehren",
        "sharp": r"scharf",
        "slight": r"leicht",
        "keep": r"halten|halte",
        "straight": r"geradeaus|weiter\s+geradeaus|weiter\s+auf|richtung|immer\s+geradeaus",
        "arrive": r"ziel|angekommen|erreicht",
    },
    "nl": {
        "name": "Dutch",
        "turn_right": r"\brechtsaf\b",
        "turn_left": r"\blinksaf\b",
        "right": r"rechts",
        "left": r"links",
        "roundabout": r"rotonde",
        "uturn": r"keer\s+om|omkeren|u-?bocht",
        "sharp": r"scherp",
        "slight": r"flauwe|licht",
        "keep": r"aanhouden|houd",
        "straight": r"rechtdoor|vervolg|volg|ga\s+richting|rijd\s+door|neem\s+de",
        "arrive": r"aangekomen|bestemming",
    },
}

# Clock time at the end of subText — the arrival ETA, language-independent.
ETA_PATTERN = r"(?i)(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)\s*$"


def both(mods: str, direction: str) -> str:
    """Order-independent 'contains a modifier AND the direction' regex."""
    return rf"(?i)(?=.*\b(?:{mods})\b)(?=.*\b(?:{direction})\b)"


def output(maneuver: str, with_distance: bool = True) -> dict:
    out = {"maneuver": {"type": "literal", "value": maneuver}}
    if with_distance:
        out["distanceMeters"] = {"type": "distance", "field": "combinedText"}
    out["primaryText"] = {"type": "firstNonEmpty", "fields": ["title", "text"]}
    out["secondaryText"] = {"type": "regexCapture", "field": "subText", "pattern": ETA_PATTERN, "group": 1}
    return out


def rule(rid, priority, field, value, maneuver, with_distance=True, comment=None):
    r = {
        "id": rid,
        "enabled": True,
        "priority": priority,
        "packageNames": [PACKAGE],
        "locales": [rid.rsplit("-", 1)[1]],
    }
    if comment:
        r["comment"] = comment
    r["conditions"] = [{"field": field, "operator": "regex", "value": value}]
    r["output"] = output(maneuver, with_distance)
    return r


def build(lang: str, k: dict) -> dict:
    # Listed in evaluation (descending priority) order.
    rules = [
        rule(f"google-maps-roundabout-{lang}", 190, "combinedText", rf"(?i)\b(?:{k['roundabout']})\b", "ROUNDABOUT"),
        rule(f"google-maps-uturn-{lang}", 180, "combinedText", rf"(?i)\b(?:{k['uturn']})\b", "UTURN_LEFT"),
        rule(f"google-maps-sharp-right-{lang}", 170, "combinedText", both(k["sharp"], k["right"]), "SHARP_RIGHT"),
        rule(f"google-maps-sharp-left-{lang}", 170, "combinedText", both(k["sharp"], k["left"]), "SHARP_LEFT"),
        rule(f"google-maps-slight-right-{lang}", 160, "combinedText", both(k["slight"], k["right"]), "SLIGHT_RIGHT"),
        rule(f"google-maps-slight-left-{lang}", 160, "combinedText", both(k["slight"], k["left"]), "SLIGHT_LEFT"),
        rule(f"google-maps-keep-right-{lang}", 120, "combinedText", both(k["keep"], k["right"]), "SLIGHT_RIGHT"),
        rule(f"google-maps-keep-left-{lang}", 120, "combinedText", both(k["keep"], k["left"]), "SLIGHT_LEFT"),
        rule(f"google-maps-turn-right-{lang}", 100, "combinedText", rf"(?i){k['turn_right']}", "RIGHT"),
        rule(f"google-maps-turn-left-{lang}", 100, "combinedText", rf"(?i){k['turn_left']}", "LEFT"),
        rule(f"google-maps-continue-{lang}", 50, "combinedText", rf"(?i)\b(?:{k['straight']})\b", "STRAIGHT"),
        rule(f"google-maps-arrive-{lang}", 40, "title", rf"(?i)\b(?:{k['arrive']})", "ARRIVE",
             with_distance=False,
             comment="Title only: the ETA in subText often carries the localized 'arrive' word, so "
                     "matching combinedText would classify every turn as ARRIVE. Lowest priority: "
                     "the pattern is a prefix stem and the title also carries the destination road "
                     "name, so it fires on road names such as Zielstattstraße or Rue de l'Arrivée. "
                     "A title with an explicit maneuver must resolve as that maneuver; only a title "
                     "with no maneuver word falls through to ARRIVE."),
    ]
    return {
        "schemaVersion": 1,
        "rulesetVersion": f"google-maps-{lang}-{RULESET_DATE}",
        "minimumAppVersionCode": 1,
        "createdAt": "2026-07-28T00:00:00Z",
        "publisher": "PebbleNTN maintainers",
        "rules": rules,
    }


def main() -> None:
    for lang, k in LANGS.items():
        path = OUT_DIR / f"{lang}.json"
        path.write_text(json.dumps(build(lang, k), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {path.relative_to(REPO_ROOT)} ({len(build(lang, k)['rules'])} rules)")


if __name__ == "__main__":
    main()
