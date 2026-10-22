# Business Overview

## Purpose

PebbleNTN ("Pebble Notification To Navigation") puts turn-by-turn directions from a phone's navigation app on a Pebble smartwatch. The Android companion reads navigation notifications on the phone (Google Maps, OsmAnd, Organic Maps and CoMaps have official rules). It turns them into one normalized instruction (maneuver, distance, road, ETA) with declarative rules and sends that state to a C watchapp over Pebble AppMessage. Users then see the next turn without reaching for the phone.

## Users and Journeys

- **Driver, cyclist or walker**: installs the companion APK and the watchapp, grants notification access, and navigates. The watchapp auto-launches (configurable) and shows the arrow, distance, road, clock and ETA.
- **Rule maintainer or contributor**: inspects debug history, writes or copies user rules, and shares redacted diagnostics to help support new apps and languages (tools: rule workbench, route-capture harness).
- **Maintainer or releaser**: every push to `main` releases an APK and PBW on GitHub. F-Droid and the Rebble app store redistribute them.

## Key Functionality

- Privacy-first notification capture: package allowlist before content is read, minimal snapshot, local processing only, no automatic upload (REQ-SEC-*).
- Declarative, layered rule engine (user over downloaded over bundled), with regex time budgets.
- Deterministic session reducer: dedupe, quantization, launch-once, READY handshake, stale detection.
- Watch rendering for five platforms (rectangular, round, 200x228), with glyph packs, themes, backlight, vibration, early warning, an About screen and an app-glance subtitle.
- Opt-in GitHub update check (off by default), with a dashboard prompt when a newer APK exists (REQ-ANDROID-013). A stale-check item comes first when the last check is over 30 days old (REQ-ANDROID-018, #94).
- Phone app update notice on the watch (#94, REQ-WATCH-023): a once-per-version full-screen notice, update idle text and an "Update: <version>" launcher subtitle while the phone reports a newer phone app.
- Diagnostics export (rules only, privacy-safe, full) through the share sheet.

## Distribution Channels

| Channel | What | How updates arrive | Evidence |
|---|---|---|---|
| GitHub Releases | APK + PBW on every push to `main` | manual download; the dashboard card links to `releases/latest` | README.md Distribution, `.github/workflows/release.yml`, strings `update_releases_url` |
| F-Droid | APK (`com.pebblentn.app`) | F-Droid client; builds from tags (`UpdateCheckMode: Tags`, `Binaries:` = the GitHub APK), so it lags a release | README.md, `fdroid/metadata/com.pebblentn.app.yml` |
| Obtainium | APK, tracking GitHub releases | Obtainium installs each release | README.md |
| Rebble app store | watchapp only | the watch auto-updates | strings `watchapp_store_url` |
| Play Store | not in scope | none | README.md |

Because the watchapp auto-updates from the store while the phone app needs a manual update, the watch is usually ahead of the phone. Both share one version line (see [dependencies.md](dependencies.md)).

## Business Context for the Active Intent (261022-version-compare-update)

#94 tells users about a phone update only when the phone's own GitHub check finds one, and that check is opt-in. This intent adds an update signal that needs no internet check: a watchapp version higher than the connected phone app version means an update is available. On the watch, that reuses the #94 notice, idle text and subtitle. On the phone, it reuses the "Update available" card. The card also shows where the app was installed from ("Update from {source}"), warns that F-Droid may need about a day, and lets the user pick another source. The flows are in [architecture.md](architecture.md), and gaps and risks are in [code-quality-assessment.md](code-quality-assessment.md).

## Glossary

- **Companion app**: the PebbleNTN Android app (not the Pebble/Core Devices mobile app, which PebbleKit 2 routes through).
- **App glance**: the watch launcher's subtitle under the app name.
- **READY handshake**: `WATCH_READY` from the watch, answered with the current state.
- **Install source**: the installer package Android records for the app (F-Droid client, Obtainium, package installer/browser).
