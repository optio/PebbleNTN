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

## REQ-WATCH-011 — Appearance settings
The watchapp SHALL provide on-watch settings for accent colour, inverted display, distance units and glyph pack, applied immediately and persisted across launches. The colour list SHALL be reduced to the colours the watch can display: 16 accents on colour watches (e.g. Pebble Time 2), black/white on black-and-white watches (e.g. Pebble 2 Duo), so both polarities and their inverses remain reachable on every model. Accent colour and glyph pack, each being a list rather than a toggle, SHALL each open their own sub-menu rather than sharing the top settings screen.

**Acceptance:** Emulator verification on a colour platform (basalt) and a black-and-white platform (diorite).

## REQ-WATCH-012 — Glyph packs
The watchapp SHALL provide built-in maneuver glyph packs selectable on the watch, defaulting to the Classic pack. The glyph-pack menu SHALL let the user preview each pack on the watch before selecting it. Packs are a render-only choice: the phone SHALL continue to send only the maneuver code, and pack selection SHALL NOT change the protocol. An unclassified maneuver (UNKNOWN) SHALL render a question-mark fallback glyph.

**Acceptance:** Emulator verification that switching packs re-renders the current maneuver, that the preview shows each pack, and that UNKNOWN renders the "?" glyph.
