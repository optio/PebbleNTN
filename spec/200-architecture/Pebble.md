# Pebble Watchapp Technical Design

## Responsibilities

- Open AppMessage inbox/outbox.
- Send READY after initialization.
- Render current normalized state.
- Display connection/stale/no-navigation states.
- Vibrate according to settings included in state or protocol capability.
- Exit to watchface on navigation end when commanded/configured.
- Persist only minimal display state needed for brief restart continuity.

## UI

Use bitmap arrows:
- straight;
- slight left/right;
- left/right;
- sharp left/right;
- U-turn left/right;
- roundabout;
- arrive;
- unknown.

Layout:
- upper half: maneuver bitmap;
- prominent distance;
- one or two lines for road/instruction;
- compact status indicator.

No continuous animation. Redraw only when state changes.

## Startup handshake

1. Watchapp opens AppMessage.
2. Watchapp sends `WATCH_READY` with protocol version and watchapp version.
3. Phone validates compatibility.
4. Phone sends the current navigation state or `NO_ACTIVE_NAVIGATION`.
5. Watch acknowledges via AppMessage transport behavior and may send an application-level state receipt if needed.

## Launch policy

The phone may call the Pebble launch API only once per detected navigation session by default. It must not repeatedly steal focus after the user leaves. A new session may launch again.

## Navigation end

Phone sends `NAVIGATION_STOPPED` with `exitToWatchface` determined by user setting. The watch shows a brief completion state only if required for a reliable exit transition, then exits.
