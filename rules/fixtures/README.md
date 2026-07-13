# rules/fixtures

Sanitized notification fixtures with expected extraction results, used by the rule regression
tests (`GoogleMapsRulesRegressionTest`).

## Provenance

Each fixture carries a `source` field:

- **`capture`** — derived from a **real** Google Maps capture (Android 16, Google Maps
  `ProgressStyle` navigation notification, 2026-07-13). Street names and destinations are replaced
  with placeholders; the *field structure* is exactly as captured.
- **`synthetic`** — authored from the documented notification shape (maneuver phrase in the title,
  distance in the text). No real capture backs these.

## What the first real capture taught us (2026-07-13)

The capture invalidated an assumption baked into the synthetic fixtures, and the bundled ruleset had
a matching bug:

1. **Google Maps puts the ETA in `subText` — "Arrive 23:51" — on every navigation notification.**
   The `google-maps-arrive-en` rule matched `/arrive|arriving|destination/` against `combinedText`
   (which includes `subText`) at the highest priority, so **every turn was classified as ARRIVE**.
   Fixed: ARRIVE now matches on `title` only, and never the bare word "arrive". The
   `eta-subtext-must-not-mean-arrive` fixture and `etaInSubTextNeverProducesArrive` test pin this.
   Every fixture that plausibly carries an ETA now includes one — the synthetic fixtures did not,
   which is precisely why the tests missed the bug.
2. **"Head toward <road>" is the step Maps opens a route with**, and it was unmatched: the continue
   rule only knew `head (straight|north|…|on)`. It now also accepts `toward(s)` and `onto`.
3. **"Starting navigation…" carries no maneuver.** It is deliberately left unmatched (fixture
   `capture-starting-navigation` asserts this): mapping it to a maneuver would put a false arrow on
   the watch.

## Known gaps

- Non-English locales (nl/fr/de …) — the bundled ruleset covers **English only**. Still a documented
  gap; no captures yet.
- Distance: the captured notifications carry no distance ("Head toward X" has no "in 200 m"), so the
  watch shows no distance for those steps. Whether Maps supplies distance in another field on
  Android 16's `ProgressStyle` template is not yet established.
- `subText` ETA is captured but not yet surfaced to the watch (`NavigationInstruction` has
  `secondaryText`/`etaEpochSeconds`; no rule fills them and the watchapp does not render them).
- Roundabout/u-turn/merge phrasing across app versions is still synthetic-only.
