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
- Watch rendering for five platforms (rectangular, round, 200x228), with glyph packs, themes, backlight, vibration, early warning, an About screen and an app-glance subtitle. On Pebble Time 2 (emery, the only touch watch built for) the settings menus, their sub-lists and About respond to touch through the system touch bridge (REQ-WATCH-021, #53); the navigation screen opts out of the bridge and has no gestures yet (#12).
- Opt-in GitHub update check (off by default), with a dashboard prompt when a newer APK exists (REQ-ANDROID-013). A stale-check item comes first when the last check is over 30 days old (REQ-ANDROID-018, #94).
- Phone app update notice on the watch (#94, REQ-WATCH-023): a once-per-version full-screen notice, update idle text and an "Update: <version>" launcher subtitle while the phone reports a newer phone app.
- Version-compare update detection (v0.0.73, REQ-ANDROID-019/020, REQ-WATCH-023): the watch compares its own version with the phone's (`version_compare.c`, strict parse), and the phone compares the watchapp version from a genuine `WATCH_READY` (stored as `watch_app_version`) with its own. Either signal raises the update notice or card with no internet check. Forwarding of the watch's `WATCH_READY` through the Core Devices app is confirmed working (v0.0.74, with `companionApp` declared), so the phone half is not limited to other companions. The card says "Update from {source}" for the detected install source (GitHub, F-Droid, Obtainium), adds the F-Droid delay line, and offers "Other sources".
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

## Business Context for the Active Intent (261023-touch-gestures)

GitHub issue #12 (opened 2026-08-09, body last edited 2026-10-23) asks for configurable gestures on the navigation screen of touchscreen Pebbles: a Touch submenu (Touch on/off; swipe left, right, up and down; tap; double tap; long press), each gesture mapped to an action that changes a watch setting (ETA mode, accent colour, invert, arrow side, glyph pack, vibration, early warning, units), lights the backlight or opens Settings, with a short "what changed" notice. It ships as a watchapp release only, with no phone-app change. The users are drivers, cyclists and walkers wearing a Pebble Time 2 (emery), the only touch watch among the five build targets.

The watchapp has moved on since the issue was opened (v0.0.75 at 3727ddc). Touch browsing of the settings menus has already shipped (#53, REQ-WATCH-021). Settings were added: the ETA label style (#13), the early-warning vibration (REQ-WATCH-022), the About row (REQ-WATCH-020) and colour previews (#81). Persist keys 11-18 were taken, and the main menu now has ten rows and ends with About. What this means for each part of #12 is listed in [code-quality-assessment.md](code-quality-assessment.md) ("Touch gestures findings"); the input and settings surfaces are in [api-documentation.md](api-documentation.md).

## Earlier Intent Context (261022-update-by-version-compar, shipped in v0.0.75)

The person wants version compare (a watchapp version higher than the companion app version) to be the **default** way an update is detected. The online GitHub check stays as an optional extra: a manual "Check for updates" or the opt-in weekly check. With v0.0.74 it was confirmed on a real phone with the Core Devices app that the watch's `WATCH_READY` (key 11) does reach `WatchListenerService`: the dashboard "Watchapp version" row shows it. The earlier "Core Devices does not forward watch messages" statement is wrong and is being corrected (the likely real cause was the missing `companionApp` declaration in `watchapp/package.json`, since added).

Version compare already works and is ungated on both devices (the card, bit 4/key 14, the watch notice, idle text and glance appear with no online check ever run). What still makes the online check look like the default is the #94 stale prompt: "Check for updates" tops the dashboard on every fresh install (never checked) and every 30 days, followed by the weekly-check question, and several texts say updates come from GitHub only. Detail is in [architecture.md](architecture.md) and [code-quality-assessment.md](code-quality-assessment.md).

Previous intent (`261022-watchapp-version-status`, shipped in v0.0.74): a read-only "Watchapp version (last seen)" row in the dashboard Status section, showing the stored watchapp version or "Not received yet". It is what showed that forwarding works.

Earlier intent (`261022-version-compare-update`, shipped): #94 told users about a phone update only when the opt-in GitHub check found one. Version compare added a signal that needs no internet check (a watchapp version higher than the phone app version), reusing the #94 watch notice and the phone "Update available" card, with install-source awareness on the card.

## Glossary

- **Companion app**: the PebbleNTN Android app (not the Pebble/Core Devices mobile app, which PebbleKit 2 routes through).
- **App glance**: the watch launcher's subtitle under the app name.
- **READY handshake**: `WATCH_READY` from the watch, answered with the current state.
- **Install source**: the installer package Android records for the app (F-Droid client, Obtainium, package installer/browser), mapped by `InstallSources.fromInstaller`.
- **Touch bridge**: the firmware's mapping of touches to button presses on a window, which an app opts into with `app_touch_navigation_enable(true)` and turns off per window with `window_set_touch_bridge_disabled(window, true)`. With the bridge off, touches go to the app's own gesture recognizers.
- **Watchapp version (last seen)**: the `APP_VERSION` from the last genuine `WATCH_READY` the phone received, persisted as `watch_app_version`; null when none ever arrived.
