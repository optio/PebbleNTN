# Business Overview

## Purpose

PebbleNTN ("Pebble Notification To Navigation") shows turn-by-turn directions from a phone's navigation app on a Pebble smartwatch. The Android companion app reads the navigation app's ongoing notification, turns it into a normalized instruction with declarative rules, and sends it to a C watchapp. The watchapp draws the maneuver arrow, the distance, the road line and an ETA strip. No navigation app integration or account is needed: the notification is the only source.

Distribution: GitHub releases (APK + PBW), F-Droid (reproducible build), Obtainium, Google Play (in preparation), and the Rebble Pebble app store for the watchapp. The current version is 0.0.82 (`VERSION`).

## Users and Value

| Actor | Need | How PebbleNTN serves it |
|---|---|---|
| Driver, cyclist or walker with a Pebble | See the next turn without taking out the phone | Arrow, distance, road and ETA on the wrist; auto-launch when navigation starts; hide while driving (#132) |
| User of a non-English phone | Directions in their language | Localized rulesets per app and language (rules picked by phone locale) |
| Power user | Support an app or phrasing that is missing | Rule editor (simple and expert JSON), preview against captured notifications, share rules and logs |
| Rule maintainer | Add and fix rules safely | Fixtures, two regression engines (Kotlin and Python), rule-workbench, route-capture harness, diagnostics exports |

## Supported Navigation Apps

The catalog is defined once in `rules/catalog/navigation-apps.json`; the full list is in [component-inventory.md](component-inventory.md#navigation-app-catalog). Apps with official rules:

| App | Languages with rules | Notification shape the rules read |
|---|---|---|
| Google Maps (and MicroG Maps through `rulesFrom`) | en, de, es, fr, it, nl, pl, pt, zh, plus language-agnostic `any` | Instruction in `title`/`text`, ETA clock in `subText` ("Arrive 23:51"), turn icon via `iconDrawable` |
| OsmAnd | en, de, es, fr, it, nl, pl, pt, zh | `title` = `<distance> • <instruction>`; expanded `bigText` line 1 = instruction plus next road and its length, line 2 = `<remaining> • <duration> • <arrival HH:MM> • <speed>` |
| Organic Maps, CoMaps | language-agnostic | Distance and road in `title`, arrow only in the large icon |
| Vela Maps | en | `title` = `In <distance> · <instruction>`, `text` = `<duration> · <distance> · Arrive <time>` |

The other catalog apps (Waze, HERE WeGo, Sygic and more) are capture-only: notifications are recorded for diagnostics but no rule shows them on the watch.

## Key Business Capabilities

1. **Privacy-first capture**: only allowlisted packages are read, and only selected fields; nothing is uploaded automatically (REQ-SEC-003, AGENTS.md rules 7-9).
2. **Declarative rules**: rule data cannot execute code; user rules override downloaded rules, which override bundled rules (REQ-RULE-001..004).
3. **Session sync**: one current state per session; launch once; resend on watch READY; stale marker; stop and exit (REQ-WATCH, REQ-ANDROID).
4. **Watch customization**: accent colours, glyph packs, units, left/right arrows, ETA as arrival time or time left, shortcuts, early warning, synced both ways (REQ-WATCH-014, REQ-ANDROID-021).
5. **When driving**: estimates speed from Google Maps' notification progress and hides the watchapp above a set speed (#132).
6. **Diagnostics**: debug history with rule traces, three export modes (full, privacy-safe, rules-only), Sharesheet.
7. **Updates**: offline watchapp-versus-phone version check, optional GitHub check, install-source-aware update card.

## Business Rules Relevant to the ETA and Road Line (#134 context)

- The watch's status strip shows `secondaryText` as the arrival time and can show "time left". It uses `etaEpochSeconds` when sent, else it parses an `HH:MM` out of `secondaryText` (REQ-WATCH-014).
- No bundled rule extracts `etaEpochSeconds` today, so the countdown always comes from the clock string, and the app glance countdown (`glance_text.c`) never runs.
- An ETA missing from one card is carried from the last card of the same session for up to 5 minutes (REQ-ANDROID-014).
- The road line drops a leading `<distance> •` (REQ-RULE-017). The watch fits 64 bytes of road text and 24 bytes of secondary text.
- Rule changes need sanitized fixtures and regression tests (REQ-RULE-012, AGENTS.md rule 14).

## Glossary

| Term | Meaning |
|---|---|
| Snapshot | The minimal immutable record of one eligible notification (`NotificationSnapshot`) |
| Instruction | Normalized rule output: maneuver, distance, primary/secondary text, ETA epoch, stops |
| Ruleset / rule pack | One JSON file `rules/bundled/<app>/<language>.json` |
| Rule family | Packages that share rules, through catalog `rulesFrom` (#113) |
| Fixture | Sanitized snapshot plus expected output, under `rules/fixtures/` |
| Capture / synthetic | Fixture provenance: from a real notification, or authored from documented phrasing |
