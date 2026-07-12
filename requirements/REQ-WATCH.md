# Watch Requirements

## REQ-WATCH-001 — Renderer
The watchapp SHALL render normalized instructions and SHALL NOT parse Android notification text.

## REQ-WATCH-002 — Bitmap maneuvers
Maneuver graphics SHALL use bundled bitmap resources.

## REQ-WATCH-003 — READY
The watchapp SHALL send READY after AppMessage initialization.

## REQ-WATCH-004 — Current state
After READY, the phone SHALL send only the latest current state.

## REQ-WATCH-005 — Automatic launch
The phone SHALL automatically launch the watchapp once when a new navigation session starts by default. This SHALL be user-configurable.

## REQ-WATCH-006 — No focus stealing loop
The phone SHALL NOT repeatedly relaunch the watchapp during the same session after the user leaves it.

## REQ-WATCH-007 — Navigation stop
On navigation stop, the watch SHALL return to the watchface by default. This SHALL be user-configurable.

## REQ-WATCH-008 — Low power
The watchapp SHALL avoid polling, continuous animation and unnecessary redraws.

## REQ-WATCH-009 — Vibration
The watch SHALL not vibrate on every distance update; maneuver-change vibration is configurable.

## REQ-WATCH-010 — Compatibility
Protocol incompatibility SHALL produce an explicit state rather than silent failure.
