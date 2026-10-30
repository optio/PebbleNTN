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

## Why localized ARRIVE sits at the bottom of the ladder (2026-07-29)

The localized rulesets match ARRIVE with a *prefix stem* (`arriv`, `ziel`, `llega`, …) so that
inflections are covered, against the `title` — which also carries the destination road name. At the
top of the priority ladder that classified any road whose name begins with the stem as ARRIVE:
`Links abbiegen auf Zielstattstraße` (Munich) and `Tournez à gauche sur Rue de l'Arrivée` (Paris
15e) both showed the arrival glyph mid-route. A tighter stem cannot fix it — `Arrivée` *is* a
well-formed inflection — so ARRIVE was demoted to priority 40, below every maneuver rule. A title
carrying an explicit maneuver now resolves as that maneuver; only a title with no maneuver word
falls through to ARRIVE, which is what real arrival phrasings look like. The
`*-arrive-stem-in-road-name` fixtures and `localizedArriveRanksBelowEveryManeuverRule` pin this.
English is unaffected: `en.json` matches whole words (`arriving|arrived|destination`) and keeps
ARRIVE on top.

## What a walking capture taught us (2026-09)

A shared full-diagnostics log (en-US, Android 17: a train ride, then a walk) was 92% unmatched.
Walking navigation switches **one** notification between two layouts:

1. **An overview card between turns** (`ProgressStyle`): `Walk 7 min (500 m)` /
   `Arrive 18:57 · <destination>`, and on the last stretch `Arrive in 1 min (70 m)` /
   `<destination>`. Unmatched, the watch kept the previous turn card (`0 m`, turn right) for minutes.
   `google-maps-walk-overview-en` shows *continue toward the destination* with the ETA and
   deliberately **no distance**, because the metres are the whole remaining walk, not the distance
   to the next turn (fixtures assert this with `noDistance`). `google-maps-walk-arrive-in-en` shows
   ARRIVE with the distance to the destination, since no turns remain.
2. **Classic turn cards**: title = bare distance (`50 m`), text = instruction. These already
   matched, but the title-first road line showed the distance twice. The `*-distance-title-en`
   variants (one priority step above each base rule) take the road line from the text. Fixtures now
   pin `primaryText`, checked by both engines.

Transit legs (`Ride to <station>` / `5 stops · 10 min`, subText `Arrive 20:26`) are handled by
`google-maps-transit-ride-en` ([#20](https://github.com/optio/PebbleNTN/issues/20)): the TRANSIT
maneuver, the station as the road line, the ETA, and the stop count as `stopsRemaining` (fixtures
assert it). It ranks above `google-maps-arrive-en` because it's anchored to a leading `Ride to`,
while the arrive rule matches "destination" anywhere in the title, so a station named
"Destination Park" would otherwise read as ARRIVE. Only the English ride card has been captured.
Boarding, transfers and alighting cards, and other languages, need real captures before rules. The ETA is missing on classic turn cards, and carrying
it over from the overview card is an Android change
([#21](https://github.com/optio/PebbleNTN/issues/21)).

## Lane guidance (2026-09)

A shared en-US driving log (Android 16, Belgium) had 115 unmatched updates, all lane guidance:
`Use the left lane to merge onto <road>` and `Use the right lane to take the <road> ramp to <city>`.
`google-maps-use-lane-{left,right}-en` read these as keep left/right (SLIGHT_*), with
`*-distance-title-en` variants for the classic layout. They sit at priority 90, below every turn
rule, so a lane instruction that names an explicit turn (`Use the right lane to turn left`) still
shows that turn. A middle lane is neither left nor right, and falls through to continue.

## Known gaps

- Non-English locales — the bundle now ships Italian, French, Spanish, German and Dutch Google Maps
  rulesets alongside English (`bundled/google-maps/<lang>.json`), generated by
  `tools/rule-workbench/gen_localized_google_maps.py`. Their fixtures are **synthetic**, authored
  from documented Google Maps phrasings (no real captures yet), so the exact wording — especially
  sharp/slight/keep — should be validated against real captures per language. Other languages are
  still a gap.
- Distance: the captured notifications carry no distance ("Head toward X" has no "in 200 m"), so the
  watch shows no distance for those steps. Whether Maps supplies distance in another field on
  Android 16's `ProgressStyle` template is not yet established.
- `subText` ETA is captured but not yet surfaced to the watch (`NavigationInstruction` has
  `secondaryText`/`etaEpochSeconds`; no rule fills them and the watchapp does not render them).
- Roundabout and u-turn phrasing across app versions is still synthetic-only (merge and motorway exits are captured, #95).

## Arrows that are only an icon: `iconDrawable` (#74)

CoMaps, Organic Maps and Google Maps' classic card put the turn only in the notification's large
icon. The app compares that icon, on the phone, with the navigation app's own turn drawables and
records the matched drawable's **name** in the snapshot's `iconDrawable` field (`ic_turn_left`,
`maneuver_turn_normal_right`, …), never the image (REQ-SEC-003). Rules turn it into a maneuver with
a `maneuverMap` on `iconDrawable`; that mapping is also what tells the app which drawables to compare
with. A fixture sets `iconDrawable` in its snapshot as the app would; a snapshot without it is a card
whose icon didn't match, and keeps the rule's default (UNKNOWN).

## What two drives in the US and Taiwan taught us (#95, 2026-10)

- **Motorway steps fell into the destination fallback.** On the ProgressStyle card,
  `0.7 mi · Take exit 425 toward …` and `350 ft · Merge onto I-35E` matched no maneuver rule and
  landed in `google-maps-destination-approach-en`, which showed the arrival flag in the middle of
  the motorway. They now have their own rules (`take-exit`, `take-exit-left`, `merge`,
  `exit-parking`), and the fallback skips any step that starts with a maneuver verb, so an
  unknown verb stays unmatched (`unknown-verb-is-not-a-destination`) and shows up in diagnostics.
- **The first classic card of a step has no title.** The distance only arrives with the next
  update, but the icon is already there. `google-maps-icon-step-no-distance` uses it, without a
  distance; status cards carry no icon and still match nothing.
- **The classic card's ETA reads `<n> min · <dist> · 07:40 ETA`**, not `Arrive 07:40`. The
  English ETA pattern takes both.
- Still open: a ProgressStyle card whose title is only the destination name, without a distance,
  is left unmatched, because a bare title can be anything.

## Vela Maps: synthetic until a capture (#120, 2026-10)

- **Every `vela-maps.json` fixture is synthetic.** The card's shape comes from Vela's public
  source: title `In <distance> · <instruction>`, the bare instruction at distance 0, `Arrived`,
  and `Navigating` before the first instruction; text `<duration> · <distance> · Arrive <time>`.
  The instruction wording is Vela's router's (Valhalla, OSRM or GraphHopper), so the fixtures use
  wording typical for each.
- **The rules start from the ones proposed by the #120 reporter**, with these gaps fixed: bare
  titles kept their arrow only through a fallback (now `vela-bare-*`); `Take the I 35 South exit`
  read as a roundabout and every exit as a right one; every U-turn was a left one; roundabouts
  phrased as a turn (`At the roundabout, turn right`) read as a turn; an approaching destination
  had no arrival flag. The maneuver is read at the start of the instruction, so a road such as
  `Bear Creek Rd` never decides the arrow, and every rule has its own priority.
- **`Navigating` stays unmatched on purpose** (`navigating-no-instruction`); route capture counts
  it as known.
- Check them against real captures: a route-capture run (`vela-maps-m1`) or a diagnostics export.
