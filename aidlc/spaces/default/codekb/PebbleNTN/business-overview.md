# Business Overview

## Purpose

**PebbleNTN** (Pebble Notification To Navigation) shows turn-by-turn directions from a phone navigation app on a Pebble smartwatch. The Android companion app reads the navigation app's notifications on the phone, turns them into a normalized instruction (turn arrow, distance, road, arrival time) with declarative rules, and sends that to a PebbleNTN watchapp over Bluetooth. Nothing is uploaded: processing is on the device, and diagnostics leave the phone only when the user exports them.

Current release: 0.0.83 (versionCode 83, protocol 1.5). Distributed through GitHub Releases, F-Droid (reproducible build), Google Play and the Rebble/Pebble app store (watchapp).

## Users and Value

| User | Value |
|---|---|
| Pebble owner who navigates by car, bike, foot or transit | Next turn, distance, road and ETA on the wrist without taking out the phone |
| User of a less common navigation app | Writes own rules, or shares redacted diagnostics so maintainers can add support |
| Maintainer / rule author | Rule workbench, fixtures, route capture harness to add apps and languages safely |

## Key Functionality

- **Notification-to-instruction**: privacy gate (master switch, per-app allowlist) before any content is read; rule engine per app and language. Supported: Google Maps (and MicroG Maps), OsmAnd (9 languages), Vela Maps, Organic Maps, CoMaps; 21 catalog entries in total, others capture-only.
- **Watch session**: automatic watchapp launch, deduplicated updates, stale marking, stop handling, app glance countdown.
- **Watch settings** (REQ-ANDROID-021, #102): the watchapp's own settings (appearance, Directions text, arrival time, units, backlight, vibration, early warning, shortcuts, When driving) are shown and changed on the phone and synced both ways with revisions; previews of visual values.
- **When driving** (#132): hide the watchapp above a chosen speed.
- **Directions text** (#134): Full instruction or Compact (street only).
- **Rules**: bundled official rules, user rules with editor and preview, rule review on update.
- **Debug and diagnostics**: debug history with traces; full, privacy-safe and rules-only exports.
- **Updates**: offline version comparison between phone app and watchapp, optional GitHub check.

## Business Domain Terms

| Term | Meaning |
|---|---|
| Instruction | Normalized maneuver + distance + primary (road) text + secondary text + ETA + stops |
| Rule / ruleset | Declarative JSON matching a notification and extracting the instruction |
| Watch settings | Settings stored on the watch, mirrored and edited on the phone |
| Capabilities | What a watch can do (colour, round, RGB backlight, touch); decides which settings are offered |
| Protocol minor | Watchapp feature level: 1.3 settings sync, 1.4 When driving, 1.5 Directions text |

## Active Intent Context (issue #139)

Issue #139 asks for clear help text for each setting on the phone's Watch settings screen. Today the screen shows only a title and the current value per row; only Directions text and When driving carry an explanation. See [component-inventory.md](component-inventory.md#watch-settings-screen) and [architecture.md](architecture.md#interaction-diagrams).
