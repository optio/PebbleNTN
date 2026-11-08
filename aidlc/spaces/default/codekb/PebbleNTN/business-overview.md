# Business Overview

## Purpose

PebbleNTN ("Pebble Notification To Navigation") puts turn-by-turn directions from a phone's navigation app on a Pebble smartwatch. The Android companion reads navigation notifications on the phone (Google Maps, OsmAnd, Organic Maps, CoMaps and Vela Maps have official rules). It turns them into one normalized instruction (maneuver, distance, road, ETA) with declarative rules and sends that state to a C watchapp over Pebble AppMessage. Users then see the next turn without reaching for the phone.

## Users and Journeys

- **Driver, cyclist or walker**: installs the companion APK and the watchapp, grants notification access, and navigates. The watchapp auto-launches (configurable) and shows the arrow, distance, road, clock and ETA.
- **Rule maintainer or contributor**: inspects debug history, writes or copies user rules, and shares redacted diagnostics to help support new apps and languages (tools: rule workbench, route-capture harness).
- **Maintainer or releaser**: every push to `main` releases an APK and PBW on GitHub. F-Droid and the Rebble app store redistribute them.

## Key Functionality

- Privacy-first notification capture: package allowlist before content is read, minimal snapshot, local processing only, no automatic upload (REQ-SEC-*).
- Declarative, layered rule engine (user over downloaded over bundled), with regex time budgets.
- Deterministic session reducer: dedupe, quantization, launch-once, READY handshake, stale detection.
- Watch rendering for five platforms (rectangular, round, 200x228), with glyph packs, themes, backlight, vibration, early warning, an About screen and an app-glance subtitle. On Pebble Time 2 (emery, the only touch watch built for) the settings menus, their sub-lists and About respond to touch through the system touch bridge (REQ-WATCH-021, #53); the navigation screen opts out of the bridge and runs the user's Shortcuts: UP/DOWN on every watch, and swipes and a tap on emery while Touch is on (REQ-WATCH-024, #12).
- Watch settings sync (#102, protocol 1.3, shipped in v0.0.77; checked at 61787f5): the watch owns 23 settings and sends them with a 0-99 revision, its platform and capabilities; the phone's **Watch settings** screen mirrors them, edits them, and queues edits until the watchapp is open (REQ-WATCH-025, REQ-ANDROID-021). The phone's own `WatchSettings` (auto-launch plus three fixed flags) is separate.
- Opt-in GitHub update check (off by default), with a dashboard prompt when a newer APK exists (REQ-ANDROID-013). A stale-check item comes first when the last check is over 30 days old (REQ-ANDROID-018, #94).
- Phone app update notice on the watch (#94, REQ-WATCH-023): a once-per-version full-screen notice, update idle text and an "Update: <version>" launcher subtitle while the phone reports a newer phone app.
- Version-compare update detection (v0.0.73, REQ-ANDROID-019/020, REQ-WATCH-023): the watch compares its own version with the phone's (`version_compare.c`, strict parse), and the phone compares the watchapp version from a genuine `WATCH_READY` (stored as `watch_app_version`) with its own. Either signal raises the update notice or card with no internet check. The card says "Update from {source}" for the detected install source, adds the F-Droid delay line, and offers "Other sources".
- Diagnostics export (rules only, privacy-safe, full) through the share sheet. Export schema 5 at 61787f5 (metadata: versions, locale, user-added packages, install record). It carries notification events only: nothing the phone decides about the watch (sends, launches) is logged.
- Install source on the dashboard (#119, shipped in v0.0.80): a "Phone app version" row with the install record.
- Supported navigation apps come from one bundled catalog (`rules/catalog/navigation-apps.json`). An app may publish under several package names (alternative builds, #54); all of them share one switch on the Navigation apps screen and one set of official rules.

## Distribution Channels

| Channel | What | How updates arrive | Evidence |
|---|---|---|---|
| GitHub Releases | APK + PBW on every push to `main` | manual download; the dashboard card's "Update from GitHub" opens `releases/latest` (F-Droid and Obtainium have their own destinations since v0.0.73) | README.md Distribution, `.github/workflows/release.yml`, strings `update_releases_url` |
| F-Droid | APK (`com.pebblentn.app`) | F-Droid client; builds from tags (`UpdateCheckMode: Tags`, `Binaries:` = the GitHub APK), so it lags a release | README.md, `fdroid/metadata/com.pebblentn.app.yml` |
| Obtainium | APK, tracking GitHub releases | Obtainium installs each release | README.md |
| Rebble app store | watchapp only | the watch auto-updates | strings `watchapp_store_url` |
| Google Play | prepared by #114; the app detects a Play install as `PLAY` and its update UI follows `UpdatePolicy` (REQ-ANDROID-022) | Play's own updates only | issue #114; `.github/workflows/play-release.yml` |

Because the watchapp auto-updates from the store while the phone app needs a manual update, the watch is usually ahead of the phone. Both share one version line (see [dependencies.md](dependencies.md)).

## Business Context for the Active Intent (261108-hide-when-driving)

GitHub issue #132 "When driving" asks that the watch stop showing directions while the user drives, so a driver does not look at the wrist; directions return when the user walks or cycles again. The design from the issue's "Proposed design" comment, as given in the brief:

- A new **synced watch setting**, editable in the watchapp's settings menu and on the phone's Watch settings screen: **When driving**: Show directions (default) / Hide directions, and **Driving speed**: default 25 km/h or 15 mph, 10-60 km/h or 5-40 mph in steps of 5 (the unit follows the watch's distance units).
- The **phone estimates speed** from what Google Maps already posts: its Android 16 `ProgressStyle` notification's progress / progress max (route metres travelled and total), the `subText` "Arrive h:mm" ETA, and the countdown of the distance over time.
- Above the threshold for about 30 s the phone **closes the watchapp and stops vibrations**; below the threshold minus a margin for about 2 min it **relaunches** it. A **manual open** by the user keeps the watchapp open for the rest of that trip.
- The **diagnostics export logs** speed estimates and hide/show decisions.

The users are drivers who wear the watch while navigating by car and also navigate on foot or by bike with the same phone. What exists today and what the change touches: [code-quality-assessment.md](code-quality-assessment.md), "Hide when driving findings"; the setting surfaces in [api-documentation.md](api-documentation.md).

## Earlier Intent Context (261030-vela-maps, shipped in v0.0.81, not rescanned)

GitHub issue #120 (reported on app version 0.0.79) asked for Vela Maps (`app.vela`, https://github.com/PimpinPumpkin/Vela, GPL-3.0): an Android maps and turn-by-turn app "without Google Play Services", built on open map data, distributed through its GitHub releases. The reporter proposed a catalog entry (`playListed: false`, `website` to the repository, official rules, default enabled) and English rules for its notification: title `In 500 ft · Turn right onto Main St`, the bare instruction, `Navigating` or `Arrived`; text `12 min · 3.4 mi · Arrive 12:45`. The person also asked to add Vela to the route-capture suite (`tools/route-capture/`). REQ-ANDROID-023 now names `vela-maps` as a `playListed: false` app without `rulesFrom` that keeps its own switch on Google Play. Detail: [code-quality-assessment.md](code-quality-assessment.md), "Vela Maps findings".

## Earlier Intent Context (261029-install-source, shipped in v0.0.80, not rescanned)

GitHub issue #119 asks the app to say where it was installed or last updated from: a "Phone app version" row for every source ("updated by Google Play", "updated by F-Droid", "installed manually"), and an install section in every debug export (mapped source, installing, initiating and originating packages, first install and last update times, whether updated since install). Shipped as REQ-DEBUG-015 (schema 5). Detail: [code-quality-assessment.md](code-quality-assessment.md), "Install source findings".

## Earlier Intent Context (261028-microg-icon-fallback, shipped in v0.0.79, not rescanned)

A follow-up to #113. Google Maps' classic card carries the turn only in its large icon, which PebbleNTN recognises by loading Google Maps' own `maneuver_*` drawables by name from the posting app. The fix retries the lookup with the Google Maps package name in the same MicroG Maps package context. Detail: [code-quality-assessment.md](code-quality-assessment.md), "MicroG icon fallback findings".

## Earlier Intent Context (261026-microg-maps, shipped in v0.0.78)

GitHub issue #113 asked for MicroG Maps (Google Maps re-packaged by the "Google Maps for MicroG-RE" Morphe patch, package `app.morphe.android.apps.maps`). It shipped as its own catalog entry using the Google Maps rules (`rulesFrom`). Detail: [code-quality-assessment.md](code-quality-assessment.md), "MicroG Maps findings".

## Earlier Intent Context (261025-play-release, merged at 1c7f4c1)

GitHub issue #114 prepared the first Google Play release of the companion app: target SDK 36, no self-updates on a Play install, privacy policy and Data safety, store listing, reviewer access and a Play Console doc; Play App Signing, developer verification and the closed test are manual. Findings as they were at 6dec020: [code-quality-assessment.md](code-quality-assessment.md), "Play release findings".

## Earlier Intent Context (261025-settings-sync, shipped in v0.0.77)

GitHub issue #102 asked for the watchapp's settings to appear in the Android companion app and stay in sync both ways, with the watch as source of truth, a wrap-at-99 revision, settings in `WATCH_READY`, a queue for edits made while the watchapp is closed, and per-value preview images. It shipped as protocol 1.3, REQ-WATCH-025 and REQ-ANDROID-021; the current design is in [architecture.md](architecture.md) and [api-documentation.md](api-documentation.md).

## Earlier Intent Context (261023-touch-gestures, shipped in v0.0.76)

GitHub issue #12 asked for configurable gestures on the navigation screen of touchscreen Pebbles. It shipped as Settings > Shortcuts (UP/DOWN on every watch, Touch and five gestures on emery), the pure `shortcuts.c` unit and `shortcut_apply.c` (REQ-WATCH-024).

## Earlier Intent Context (261022-update-by-version-compar, shipped in v0.0.75)

The person wanted version compare to be the default way an update is detected, with the online GitHub check as an optional extra. Earlier intents in that line: `261022-watchapp-version-status` (v0.0.74, the "Watchapp version (last seen)" row, which showed that the Core Devices app forwards `WATCH_READY`) and `261022-version-compare-update` (version compare itself).

## Glossary

- **Companion app**: the PebbleNTN Android app (not the Pebble/Core Devices mobile app, which PebbleKit 2 routes through).
- **App glance**: the watch launcher's subtitle under the app name.
- **READY handshake**: `WATCH_READY` from the watch, answered with the current state. The phone also makes up a READY itself (1.5 s after its own launch, and on the companion's "app opened" callback); those carry no app version and no settings.
- **Install source**: the installer package Android records for the app, mapped by `InstallSources.fromInstaller` to GITHUB, FDROID, OBTAINIUM or PLAY.
- **Installing / initiating / originating package** (#119): the app that installed the current version, the one that asked for the install, and where the APK came from.
- **Play install**: an install (or update) made by the Google Play Store; it may not update itself or point to updates outside Play.
- **Touch bridge**: the firmware's mapping of touches to button presses on a window (`app_touch_navigation_enable`, `window_set_touch_bridge_disabled`).
- **Settings revision** (#102): a number from 0 to 99 that each side moves on by one per local settings edit, wrapping to 0; `a` is newer than `b` when 1 to 49 steps ahead.
- **Settings mirror** (#102): the phone's copy of the watch's 23 settings, with the phone's revision and the edits the watch has not confirmed (pending).
- **Watchapp open** (phone view): the watchapp sent its settings in this session and has not closed (`onAppClosed`) or failed a send since; only then do phone edits go out.
- **Shortcut**: an action bound to UP, DOWN or, on emery, a swipe or tap on the navigation screen (REQ-WATCH-024).
- **Watchapp version (last seen)**: the `APP_VERSION` from the last genuine `WATCH_READY` the phone received, persisted as `watch_app_version`.
- **Catalog entry**: one supported navigation app in `rules/catalog/navigation-apps.json`. Settings, enablement and the Navigation apps screen work per entry.
- **Package variant**: another Android package name of the same navigation app; listed in the app's catalog entry and behaves like the main package.
- **Rule family** (#113): the packages that share one app's rules (`RuleFamilies`).
- **Unlisted app** (`playListed: false`): an entry hidden on Google Play installs.
- **Vela Maps** (#120): an open-source navigation app, package `app.vela`, released on GitHub only.
- **Unrecognised card** (route-capture): a captured notification no bundled rule matches that looks like a direction.
- **MicroG Maps**: Google Maps re-packaged to run with microG; package `app.morphe.android.apps.maps` (#113).
- **ProgressStyle notification** (#132): Android 16's progress-tracker notification template; Google Maps posts its trip as `EXTRA_PROGRESS` / `EXTRA_PROGRESS_MAX`, which PebbleNTN already captures as `progress` / `progressMax` in the snapshot but uses for nothing.
- **Driving mode / hidden** (#132, proposed): the phone's decision that the user is driving, after which it closes the watchapp until the speed stays low again or the user opens it by hand.
- **Manual open** (#132, proposed): the user opening the watchapp while it is hidden, which keeps it open for the rest of the trip.
