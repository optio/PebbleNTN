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

See REQ-WATCH-016 for the on-watch vibration setting (pattern and strength) layered on top of this.

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
The watchapp SHALL provide an on-watch setting selecting how the status strip renders the arrival estimate, applied immediately and persisted across launches. It SHALL offer two modes and default to the arrival-time mode: **arrival time** displays the phone-provided arrival clock time labelled "ETA" (the original behaviour), and **time to arrival** displays the remaining duration until arrival in `H:MM` form labelled "IN", computed on the watch and recomputed at least once per minute. This is a render-only choice: mode selection SHALL NOT change the protocol; the phone SHALL continue to send the arrival-time string, and the arrival epoch (`etaEpochSeconds`) whenever a rule extracts one.

The countdown SHALL be derived in this order:
1. from the arrival epoch when the phone sent one, to the exact second (rounded up to the next whole minute);
2. otherwise from the arrival-time string, when that string contains a wall-clock time, by comparing it against the watch's own clock — both being wall-clock time in the phone's timezone. The comparison SHALL wrap over midnight, so an arrival past midnight reads as minutes away rather than a negative or a whole day. Only a genuine clock time SHALL be accepted: a value shaped like a time but out of range (`25:00`) SHALL NOT be reinterpreted as some other time.

The string is a normalized field the phone chose to send, not notification text, so deriving from it does not weaken REQ-WATCH-001. When neither source yields a time — the mode is time-to-arrival, no epoch was sent, and the arrival-time string is absent or is not a clock time — the watch SHALL fall back to displaying the arrival-time string under the "ETA" label rather than leaving the strip blank.

**Acceptance:** Emulator verification that the setting toggles the strip between "ETA 14:35" and an "IN H:MM" countdown, that the countdown updates as the clock advances, that it is derived from the arrival-time string when no epoch is sent (including 12-hour strings such as "2:35 PM" and arrivals across midnight), and that a non-time arrival string falls back to the arrival-time display. Host unit tests cover the string parsing and the midnight wrap.

## REQ-WATCH-015 — Backlight setting
The watchapp SHALL provide an on-watch setting that keeps the backlight lit while the watchapp is the foreground app, applied immediately and persisted across launches. It SHALL default to **Watch default** — no additional backlight, leaving the watch's own automatic light behaviour untouched — and offer three further intensity levels. Because the Pebble SDK exposes no backlight *brightness* control to apps (only on/off via `light_enable`), the three levels SHALL express intensity as how long the light is held engaged around navigation activity: **Low** and **Medium** light the backlight for a short and a longer period respectively after each navigation update, and **High** keeps it steady-on for the whole session. The setting SHALL override the watch's automatic control only while a non-default level is selected, and SHALL release control back to the watch (`light_enable(false)`) when switched back to Watch default or when the app exits. While a non-default level is selected it supersedes the phone's per-update backlight request (REQ-WATCH is otherwise unchanged when the setting is at its default). This is a watch-side render/behaviour choice: it SHALL NOT change the protocol.

Each level's menu label SHALL state what the level does rather than only how strong it is, so the choice is self-explanatory on the watch: "Watch default", "Low - 3s on update", "Medium - 10s on update", "High - until app closes".

**Backlight colour (RGB hardware only).** On watches whose backlight LED is colour-capable — identified at compile time by `PBL_RGB_BACKLIGHT`, currently Pebble Time 2 — the watchapp SHALL additionally offer a backlight **colour**, applied immediately and persisted across launches. It SHALL default to **Watch default** (the user's own backlight colour, restored with `light_set_system_color()`) and offer a list of tints applied with `light_set_color_rgb888()`, chosen over `light_set_color()` because `GColor` carries only 2 bits per channel. The tint SHALL be pushed to the LED whenever the watchapp engages the light and whenever the colour setting changes. No teardown is required: the system resets the tint when the app exits or a notification preempts it.

On hardware without an RGB backlight the colour setting SHALL NOT be offered at all, and the Backlight row SHALL keep cycling the duration in place. Where the colour setting exists, the Backlight row SHALL instead open a sub-window holding both the **Duration** and the **Colour**, the latter opening a colour list of its own.

**Acceptance:** Emulator verification that Watch default leaves the light unforced, that High keeps the backlight on for the whole session, that Low/Medium light it around updates and release it after their hold, and that switching back to Watch default releases the light. For the colour setting: emulator verification on an RGB platform (emery) that the Backlight row opens a Duration + Colour sub-window, that the colour list marks and applies the selection, and that both survive an app relaunch; and on a non-RGB platform (basalt) that the Backlight row still cycles the duration in place and offers no colour. The rendered LED tint itself is observable only on hardware.

## REQ-WATCH-016 — Vibration setting
The watchapp SHALL provide on-watch settings that vibrate the watch when a new navigation instruction is parsed, applied immediately and persisted across launches: a **pattern** row and a **strength** row. The pattern row SHALL default to **Off** (no watch-initiated vibration) and offer Single, Double, Triple and Long patterns, differing in the number/shape of pulses. The strength row SHALL offer Light, Medium (default) and Strong; because the Pebble SDK exposes no vibration *amplitude* control, strength SHALL be expressed as pulse length (a longer buzz reads as stronger), realised with a custom vibration pattern. Vibration SHALL fire only on genuinely new information — a changed maneuver or changed primary text — never on a plain resend of the same instruction as the distance counts down (REQ-WATCH-009). When the pattern is a non-Off value it supersedes the phone's simple maneuver-change pulse; when it is Off, the original maneuver-change vibration (gated by the phone's request) stands. Changing either row in the menu SHALL play the newly selected buzz as a preview. This is a watch-side behaviour choice: it SHALL NOT change the protocol.

**Acceptance:** Emulator verification that Off preserves the original maneuver-change pulse, that each pattern plays its number of pulses on a new instruction, that Light/Medium/Strong produce progressively longer buzzes, that a resend of the same instruction does not vibrate, and that changing a row previews the buzz.
