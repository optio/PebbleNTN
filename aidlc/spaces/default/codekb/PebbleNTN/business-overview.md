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
- Version-compare update detection (v0.0.73, REQ-ANDROID-019/020, REQ-WATCH-023): the watch compares its own version with the phone's (`version_compare.c`, strict parse), and the phone compares the watchapp version from a genuine `WATCH_READY` (stored as `watch_app_version`) with its own. Either signal raises the update notice or card with no internet check. The card says "Update from {source}" for the detected install source (GitHub, F-Droid, Obtainium), adds the F-Droid delay line, and offers "Other sources".
- Diagnostics export (rules only, privacy-safe, full) through the share sheet.

## Distribution Channels

| Channel | What | How updates arrive | Evidence |
|---|---|---|---|
| GitHub Releases | APK + PBW on every push to `main` | manual download; the dashboard card's "Update from GitHub" opens `releases/latest` (F-Droid and Obtainium have their own destinations since v0.0.73) | README.md Distribution, `.github/workflows/release.yml`, strings `update_releases_url` |
| F-Droid | APK (`com.pebblentn.app`) | F-Droid client; builds from tags (`UpdateCheckMode: Tags`, `Binaries:` = the GitHub APK), so it lags a release | README.md, `fdroid/metadata/com.pebblentn.app.yml` |
| Obtainium | APK, tracking GitHub releases | Obtainium installs each release | README.md |
| Rebble app store | watchapp only | the watch auto-updates | strings `watchapp_store_url` |
| Play Store | not in scope | none | README.md |

Because the watchapp auto-updates from the store while the phone app needs a manual update, the watch is usually ahead of the phone. Both share one version line (see [dependencies.md](dependencies.md)).

## Business Context for the Active Intent (261022-watchapp-version-status)

The version-compare work (intent `261022-version-compare-update`) shipped in v0.0.73. Its phone half depends on the watch's real `WATCH_READY` reaching the phone, and the Core Devices companion is known not to forward it. Today the user cannot tell on the phone whether it ever arrived. This intent shows the stored watchapp version ("last seen") in the dashboard Status section, or "not received yet" when none is stored, so the user can check on the device whether the companion forwards watch-to-phone messages. It is a read-only diagnostic row: no protocol or watch change. The data path is in [architecture.md](architecture.md) and the risks are in [code-quality-assessment.md](code-quality-assessment.md).

Earlier intent (`261022-version-compare-update`, shipped): #94 told users about a phone update only when the opt-in GitHub check found one. Version compare added a signal that needs no internet check (a watchapp version higher than the phone app version), reusing the #94 watch notice and the phone "Update available" card, with install-source awareness on the card.

## Glossary

- **Companion app**: the PebbleNTN Android app (not the Pebble/Core Devices mobile app, which PebbleKit 2 routes through).
- **App glance**: the watch launcher's subtitle under the app name.
- **READY handshake**: `WATCH_READY` from the watch, answered with the current state.
- **Install source**: the installer package Android records for the app (F-Droid client, Obtainium, package installer/browser), mapped by `InstallSources.fromInstaller`.
- **Watchapp version (last seen)**: the `APP_VERSION` from the last genuine `WATCH_READY` the phone received, persisted as `watch_app_version`; null when none ever arrived.
