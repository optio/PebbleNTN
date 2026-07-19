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
  the remaining duration with an "IN" label (e.g. "IN 0:25"), computed on the watch from the arrival
  epoch and recounted each minute. When the phone has stopped updating, the clock is replaced by a
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

## Stale data

If state age exceeds configured threshold, retain the instruction but visibly mark it stale. Never advance a maneuver locally based only on elapsed time.
