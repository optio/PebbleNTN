# Business Overview

## Purpose

PebbleNTN ("Pebble Notification To Navigation") shows turn-by-turn directions from a phone's navigation app on a Pebble smartwatch. The Android companion app reads the navigation app's ongoing notification, turns it into a small normalized state (maneuver, distance, road, arrival time) and sends it to a Pebble watchapp, which renders it. The watch never parses notifications and never computes routes (spec/000-overview.md).

## Users and Value

- **Driver, cyclist or walker** wearing a Pebble: sees the next turn without reaching for the phone.
- **Rule maintainer / contributor**: writes declarative JSON rules that teach the app a new navigation app or language, checked by fixtures and regression tests.
- **Maintainer / releaser**: ships an APK (GitHub Releases, F-Droid, Obtainium) and a PBW (Rebble app store) from every push to `main`.

## Key Functionality

| Area | What it does | Where |
|---|---|---|
| Notification capture | Listens to allowlisted navigation apps only; builds a minimal immutable snapshot | android/app notification/, core/AppAllowlist.kt |
| Rule extraction | Declarative, layered (official, user) rules turn a snapshot into an instruction | android/app rules/, rules/bundled/ |
| Session sync | A pure reducer keeps one latest state, launches the watchapp once per session, retries sends | core/NavigationSessionReducer.kt, pebble/NavigationController.kt |
| Watch rendering | Maneuver glyph, distance, clock + ETA strip, road text; stale, connecting, no-navigation and incompatible states | watchapp/src/c/main.c |
| On-watch settings | Colour, invert, units, glyph pack, arrow side, ETA mode and label, backlight, vibration, early warning, About | watchapp/src/c/settings_window.c, theme.c |
| About screen | Watchapp version, protocol version, phone link status, install QR | watchapp/src/c/about_window.c, about_text.c (REQ-WATCH-020) |
| Diagnostics | Debug history, redacted or full export through the share sheet | android/app data/, export/, ui/debug, ui/share |
| Updates | Release check against GitHub; signed (Ed25519) official ruleset updates | android/app update/ |

Supported apps today: Google Maps (official rules in 9 languages), OsmAnd, Organic Maps and CoMaps (rules/bundled/), with capture-only entries for more (rules/catalog/navigation-apps.json).

## Business Rules That Shape the Code

- Privacy first: notification text is never inspected before the package allowlist passes, disabled apps are never stored, nothing is uploaded automatically (AGENTS.md rules 7-9; spec/600-security-release/SecurityPrivacy.md).
- Remote rules are data only, never executable code (AGENTS.md rule 6).
- The protocol is generated from one JSON definition and never hand-edited (AGENTS.md rule 5).
- Spec and requirement files are integrity-protected (MANIFEST.sha256.json).
- Every behaviour change needs automated tests and cites requirement IDs (AGENTS.md rules 4 and commit discipline).

## Current Intent Context

Intent `261020-about-companion-version` (GitHub #103): the About screen should also show the last connected companion (phone) app version. See [api-documentation.md](api-documentation.md) for why this needs no protocol change and [code-quality-assessment.md](code-quality-assessment.md) for the constraints it touches.
