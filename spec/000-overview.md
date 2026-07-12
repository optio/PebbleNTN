# 000 — Consolidated Technical Overview

## Problem

Phone navigation apps expose upcoming turns in ongoing notifications. PebbleNTN converts those notifications into a compact, normalized navigation state and presents it on a Pebble watch.

## System boundary

```text
Navigation app
  -> Android NotificationListenerService
  -> package allowlist
  -> immutable notification snapshot
  -> declarative rule engine
  -> normalized navigation state
  -> state repository and debug history
  -> Pebble transport
  -> Pebble watchapp renderer
```

The watch does not parse Android notifications and does not calculate routes.

## Core principles

- Event-driven, no polling service.
- Privacy by early package filtering.
- Declarative and testable rule extraction.
- Last-known-state synchronization instead of replaying stale instructions.
- Watch transport isolated behind an interface.
- Rules and protocol versioned independently.
- Development workflows work from the terminal.
- Real Google Maps captures and synthetic fixtures are both first-class test inputs.

## Product defaults

- Supported app definitions are bundled.
- Installed defined navigation apps are enabled by default.
- Users may disable any app.
- Google Maps has official parsing rules initially.
- Organic Maps, OsmAnd, HERE WeGo, and Waze may be listed for capture-only mode when installed; no correct extraction is claimed until official rules exist.
- Debug retention defaults to 500 eligible notification events.
- Automatic watchapp launch on navigation-session start is enabled.
- Automatic return to watchface when navigation ends is enabled.
- Automatic remote rule checks are disabled in the initial public version.
- Raw content export is opt-in and preceded by an explicit privacy warning.
