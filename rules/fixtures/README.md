# rules/fixtures

Sanitized notification fixtures with expected extraction results, used by the rule regression
tests (`GoogleMapsRulesRegressionTest`).

## Provenance and gaps (M6)

`google-maps.json` fixtures are **synthetic**: they were authored from the documented shape of
Google Maps navigation notifications (maneuver phrase in the title, distance in the text), **not**
from real device captures, because a device/emulator was not available in this build environment.
They contain no real destinations or personal data.

Known gaps to close when real captures are available:
- Verify exact Google Maps notification field layout (title vs text vs bigText) across app versions.
- Confirm phrasing for all maneuvers (e.g. "Make a U-turn", exit-numbered roundabouts, merges).
- Add non-English locales (nl/fr/de …) — the bundled ruleset currently covers **English only**;
  other locales are a documented gap, not yet implemented, per the "evidence supports" rule in the
  roadmap.
- Replace or supplement synthetic fixtures with sanitized real captures and re-verify expected
  output.
