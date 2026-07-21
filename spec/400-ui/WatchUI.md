# Watch UI Specification

## States

- Starting
- No active navigation
- Active instruction
- Arriving
- Navigation finished
- Phone disconnected
- Stale instruction
- Protocol incompatible

## Active layout

- A coloured **panel** across the top holds the maneuver arrow and the distance side by side. The
  arrow occupies one top corner (top-right by default; the corner is a user setting, REQ-WATCH-011)
  and the distance sits opposite it, with its unit stacked under the number. The panel is sized to
  hug the arrow rather than a fixed fraction of the screen, so little vertical space is wasted.
- A **status strip** below the panel carries the watch clock on the left and the arrival estimate on
  the right — the watch stays a watch while navigating. The arrival estimate has two display modes
  (REQ-WATCH-014), selectable on the watch and defaulting to arrival time: **arrival time** shows the
  clock time of arrival with a small "ETA" label (e.g. "ETA 14:35"), while **time to arrival** shows
  the remaining duration with an "IN" label (e.g. "IN 0:25"), computed on the watch and recounted
  each minute — from the arrival epoch when the phone sent one, otherwise by comparing the
  arrival-time string against the watch's own clock (wrapping over midnight), which is what lets the
  countdown work with rulesets that extract no epoch. An arrival string that is not a clock time
  falls back to the "ETA" display. When the phone has stopped updating, the clock is replaced by a
  STALE marker.
- The lower area shows the **primary text** (road or concise instruction), auto-fit to the largest
  font that fits rather than truncated.
- No map. Distance and road text drive their own font size (no fixed truncation).

### Screen-size adaptation (REQ-WATCH-013)

The layout is detected at runtime from the screen width. The 144-wide models (aplite/basalt/diorite)
and round chalk use the compact layout; Pebble Time 2 (emery, 200×228) uses larger strip fonts, a
larger maneuver arrow (an emery-only bundled resource) and a proportionally taller panel. No user
setting selects this — it is automatic.

## Distance formatting

Phone sends meters. Watch formats using user/unit preference communicated by phone:
- metric: `850 m`, `1.2 km`;
- imperial future extension.
Initial release may be metric-only if explicitly shown in settings and protocol keeps room for unit flags.

## Vibration

Vibrate only when maneuver identity changes or an explicit threshold event is sent. Do not vibrate on every distance update.

An on-watch setting (REQ-WATCH-016) lets the user choose to vibrate when a new instruction is parsed,
with a **pattern** (Off — the default — / Single / Double / Triple / Long) and a **strength**
(Light / Medium — the default — / Strong). Strength is realised as pulse length because the SDK
exposes no vibration amplitude control. It fires only on genuinely new information (a changed maneuver
or primary text), never on a resend as the distance counts down, and — when set to a non-Off pattern —
supersedes the phone's simple maneuver-change pulse. Changing either row in the settings menu plays
the selected buzz as a preview.

## Backlight

An on-watch setting (REQ-WATCH-015) can keep the backlight lit while the watchapp is the foreground
app. It defaults to **Watch default** (no additional backlight; the watch's own automatic behaviour
is untouched) and offers three intensity levels. Because the SDK exposes no backlight brightness
control, the levels are expressed as how long the light is held after activity: **Low** and **Medium**
light it briefly and for longer respectively after each navigation update, and **High** keeps it
steady-on for the whole session. A non-default level overrides the watch's automatic control and the
phone's per-update light request; switching back to Watch default (or leaving the app) releases
control back to the watch.

## Stale data

If state age exceeds configured threshold, retain the instruction but visibly mark it stale. Never advance a maneuver locally based only on elapsed time.
