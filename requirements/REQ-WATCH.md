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
The phone SHALL NOT relaunch the watchapp merely to bring it to the foreground during the same session after the user leaves it, and SHALL NOT relaunch it on a timer or poll. It MAY re-launch the watchapp when a companion app refuses to deliver a pending state update unless the watchapp is the foreground app on the watch — notably the Core Devices Pebble app, which returns `FailedDifferentAppOpen` in that case — because otherwise the update cannot reach the watch at all. Such a re-launch SHALL be driven only by an actual pending state update (never speculatively), so it occurs at most as often as the navigation state materially changes. See `spec/200-architecture/Pebble.md` → Launch policy.

## REQ-WATCH-007 — Navigation stop
On navigation stop, the watch SHALL return to the watchface by default. This SHALL be user-configurable.

## REQ-WATCH-008 — Low power
The watchapp SHALL avoid polling, continuous animation and unnecessary redraws.

## REQ-WATCH-009 — Vibration
The watch SHALL not vibrate on every distance update; maneuver-change vibration is configurable.

## REQ-WATCH-010 — Compatibility
Protocol incompatibility SHALL produce an explicit state rather than silent failure.

## REQ-WATCH-011 — Appearance settings
The watchapp SHALL provide on-watch settings for accent colour, inverted display, distance units, glyph pack and arrow corner, applied immediately and persisted across launches. The arrow-corner setting SHALL choose which top corner holds the maneuver arrow (top-right by default), placing the distance in the opposite corner. The colour list SHALL be reduced to the colours the watch can display: 16 accents on colour watches (e.g. Pebble Time 2), black/white on black-and-white watches (e.g. Pebble 2 Duo), so both polarities and their inverses remain reachable on every model. Accent colour and glyph pack, each being a list rather than a toggle, SHALL each open their own sub-menu rather than sharing the top settings screen.

**Acceptance:** Emulator verification on a colour platform (basalt), a black-and-white platform (diorite) and the large-screen platform (emery / Pebble Time 2).

## REQ-WATCH-013 — Screen-size adaptation
The watchapp SHALL detect the watch's screen size at runtime and adapt its layout without a user setting: the 144-wide models (aplite/basalt/diorite) and the round chalk display use the compact layout, while Pebble Time 2 (emery, 200×228) uses a larger layout — bigger status-strip fonts, a larger maneuver arrow (bundled as an emery-only resource so the smaller models never carry it) and a maneuver panel sized to hug the arrow rather than a fixed fraction of the screen. Adaptation SHALL keep every supported model rendering correctly.

**Acceptance:** Emulator verification that a 144-wide platform and emery each render the compact and large layouts respectively.

## REQ-WATCH-012 — Glyph packs
The watchapp SHALL provide built-in maneuver glyph packs selectable on the watch, defaulting to the Classic pack. The glyph-pack menu SHALL let the user preview each pack on the watch before selecting it. Packs are a render-only choice: the phone SHALL continue to send only the maneuver code, and pack selection SHALL NOT change the protocol. An unclassified maneuver (UNKNOWN) SHALL render a question-mark fallback glyph.

**Acceptance:** Emulator verification that switching packs re-renders the current maneuver, that the preview shows each pack, and that UNKNOWN renders the "?" glyph.

## REQ-WATCH-014 — ETA display mode
The watchapp SHALL provide an on-watch setting selecting how the status strip renders the arrival estimate, applied immediately and persisted across launches. It SHALL offer two modes and default to the arrival-time mode: **arrival time** displays the phone-provided arrival clock time labelled "ETA" (the original behaviour), and **time to arrival** displays the remaining duration until arrival in `H:MM` form labelled "IN", computed on the watch from the arrival epoch (`etaEpochSeconds`) and recomputed at least once per minute. This is a render-only choice: pack selection SHALL NOT change the protocol, and the phone SHALL continue to send both the arrival-time string and the arrival epoch. When the selected mode is time-to-arrival but the phone sent no arrival epoch, the watch SHALL fall back to the arrival-time string rather than leaving the strip blank.

**Acceptance:** Emulator verification that the setting toggles the strip between "ETA 14:35" and an "IN H:MM" countdown, that the countdown updates as the clock advances, and that with no arrival epoch the time-to-arrival mode falls back to the arrival-time string.
