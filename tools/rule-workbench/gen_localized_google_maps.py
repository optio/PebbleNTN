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
RULESET_DATE = "2026.10.1"
# Languages added later carry their own date, so regenerating doesn't bump the others' versions.
RULESET_DATES = {"pt": "2026.10.4", "pl": "2026.10.4"}

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
        "straight": r"sempre\s+dritto|prosegui|procedi|continua|vai\s+dritto|dritto|diritto",
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
    # pt and pl (#66): the turn / continue / slight phrasings are captured by the route-capture harness
    # (Google Maps 26.14, Android 17, 2026-10-04): "Vire à esquerda na …", "Siga em direção à …",
    # "Continue para …", "Curva suave à direita para permanecer na …"; "Skręć w lewo w …",
    # "Skręć łagodnie w prawo, pozostając na …", "Kontynuuj wzdłuż …", "Kieruj się w stronę …".
    # The others follow Google Maps' usual wording and need captures.
    # Patterns that start or end with a non-ASCII letter are marked RAW (used as written): Java's \b
    # is ASCII-only on the JVM but Unicode-aware on Android and in Python, so \b next to ł or ć would
    # behave differently between the engines.
    "pt": {
        "name": "Portuguese",
        "turn_right": r"(?:vire|dobre|curva)\s+à\s+direita\b",
        "turn_left": r"(?:vire|dobre|curva)\s+à\s+esquerda\b",
        "right": r"direita",
        "left": r"esquerda",
        "roundabout": r"RAW:\b(?:rotunda|rotat\S*ria)\b",
        "uturn": r"retorno|invers\S*o\s+de\s+marcha",
        "sharp": r"acentuad[ao]|fechad[ao]|bruscamente",
        "slight": r"suave|ligeiramente|levemente",
        "keep": r"mantenha-se|mantenha|permane\S*a|fique",
        "straight": r"continue|siga|prossiga|em\s+frente",
        "arrive": r"cheg|destino",
    },
    "pl": {
        "name": "Polish",
        "turn_right": r"\bskr\S*\s+w\s+prawo\b",
        "turn_left": r"\bskr\S*\s+w\s+lewo\b",
        "right": r"prawo|prawej|prawa",
        "left": r"lewo|lewej|lewa",
        "roundabout": r"rondo|rondzie|ronda",
        "uturn": r"RAW:\bzawr",
        "sharp": r"ostro",
        "slight": r"RAW:(?:lekko\b|agodnie\b)",
        "keep": r"trzymaj|zjed\S*\s+na",
        "straight": r"kontynuuj|prosto|kieruj",
        "arrive": r"dotar|celu|miejsce\s+docelowe",
    },
}

# Clock time at the end of subText — the arrival ETA, language-independent.
ETA_PATTERN = r"(?i)(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)\s*$"


def kw(pattern: str) -> str:
    """A keyword alternation wrapped in word boundaries, unless marked RAW: (used as written)."""
    return pattern[4:] if pattern.startswith("RAW:") else rf"\b(?:{pattern})\b"


def both(mods: str, direction: str) -> str:
    """Order-independent 'contains a modifier AND the direction' regex."""
    return rf"(?i)(?=.*{kw(mods)})(?=.*\b(?:{direction})\b)"


def output(maneuver: str, with_distance: bool = True) -> dict:
    out = {"maneuver": {"type": "literal", "value": maneuver}}
    if with_distance:
        out["distanceMeters"] = {"type": "distance", "field": "combinedText"}
    # The title may lead with the distance ("70 m · ..."), which the watch already shows (#26).
    out["primaryText"] = {"type": "firstNonEmpty", "fields": ["title", "text"], "stripLeadingDistance": True}
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
        rule(f"google-maps-roundabout-{lang}", 190, "combinedText", rf"(?i){kw(k['roundabout'])}", "ROUNDABOUT"),
        rule(f"google-maps-uturn-{lang}", 180, "combinedText", rf"(?i){kw(k['uturn'])}", "UTURN_LEFT"),
        rule(f"google-maps-sharp-right-{lang}", 170, "combinedText", both(k["sharp"], k["right"]), "SHARP_RIGHT"),
        rule(f"google-maps-sharp-left-{lang}", 170, "combinedText", both(k["sharp"], k["left"]), "SHARP_LEFT"),
        rule(f"google-maps-slight-right-{lang}", 160, "combinedText", both(k["slight"], k["right"]), "SLIGHT_RIGHT"),
        rule(f"google-maps-slight-left-{lang}", 160, "combinedText", both(k["slight"], k["left"]), "SLIGHT_LEFT"),
        rule(f"google-maps-keep-right-{lang}", 120, "combinedText", both(k["keep"], k["right"]), "SLIGHT_RIGHT"),
        rule(f"google-maps-keep-left-{lang}", 120, "combinedText", both(k["keep"], k["left"]), "SLIGHT_LEFT"),
        rule(f"google-maps-turn-right-{lang}", 100, "combinedText", rf"(?i){k['turn_right']}", "RIGHT"),
        rule(f"google-maps-turn-left-{lang}", 100, "combinedText", rf"(?i){k['turn_left']}", "LEFT"),
        rule(f"google-maps-continue-{lang}", 50, "combinedText", rf"(?i){kw(k['straight'])}", "STRAIGHT"),
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
        "rulesetVersion": f"google-maps-{lang}-{RULESET_DATES.get(lang, RULESET_DATE)}",
        "minimumAppVersionCode": 1,
        "createdAt": "2026-07-28T00:00:00Z",
        "publisher": "PebbleNTN maintainers",
        "rules": rules,
    }


def main() -> None:
    import sys
    if len(sys.argv) > 1:  # no options: running it always regenerates every language
        print(__doc__)
        return
    for lang, k in LANGS.items():
        path = OUT_DIR / f"{lang}.json"
        path.write_text(json.dumps(build(lang, k), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {path.relative_to(REPO_ROOT)} ({len(build(lang, k)['rules'])} rules)")


if __name__ == "__main__":
    main()
