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

### Companion limitation and synthesized readiness

Step 2 assumes the companion app forwards the watch's inbound `WATCH_READY` to third-party
PebbleKit listeners. The Core Devices Pebble app (`coredevices.coreapp`, the PebbleKit Android 2
companion) does **not** — its protocol runner receives our packet but never calls our listener
service — so a phone that waits for `WATCH_READY` sits on "Connecting" forever. Because the phone
has just launched our watchapp on a connected watch, it therefore **synthesizes readiness itself**
after a short settle delay (`NavigationController.scheduleAutonomousReady`), and also treats the
PebbleKit `onAppOpened` lifecycle callback (which the companion *does* forward) as a readiness
signal (`WatchListenerService.onAppOpened`). A genuine `WATCH_READY`, if it ever does arrive, is
still handled and simply re-sends the current state.

### Companion authorization (`companionApp`)

For the companion to route AppMessages between our Android package and our watchapp UUID, the
watchapp's `package.json` MUST declare the association via `companionApp.android.apps[].package =
com.pebblentn.app`. Without it the companion has no registered link between the UUID and the app and
refuses every send with `FailedDifferentAppOpen`, regardless of foreground state. The watchapp must
be (re)installed through the companion so the metadata registers.

## Launch policy

The phone calls the Pebble launch API once per detected navigation session by default (the reducer's
launch-once guard). It must not repeatedly steal focus after the user leaves, and must never
re-launch on a timer or poll. A new session may launch again.

One exception is forced by the companion: the Core Devices app refuses to deliver a state update
(`FailedDifferentAppOpen`) unless our watchapp is the foreground app on the watch. When a *pending
state update* is refused for that reason, the transport re-launches the watchapp once and retries the
send, because otherwise the update cannot reach the watch at all (`PebbleWatchTransport.send`). This
re-launch is bounded to actual state changes — it is never speculative — so during navigation it
happens at most as often as the maneuver/distance materially changes, and never while idle. See
REQ-WATCH-006.

## Navigation end

Phone sends `NAVIGATION_STOPPED` with `exitToWatchface` determined by user setting. The watch shows a brief completion state only if required for a reliable exit transition, then exits.
