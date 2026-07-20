# PebbleNTN — Engineering Specification

[![Android Build](https://github.com/optio/PebbleNTN/actions/workflows/android.yml/badge.svg)](https://github.com/optio/PebbleNTN/actions/workflows/android.yml) [![Watchapp Build](https://github.com/optio/PebbleNTN/actions/workflows/watchapp.yml/badge.svg)](https://github.com/optio/PebbleNTN/actions/workflows/watchapp.yml) [![Release Build](https://github.com/optio/PebbleNTN/actions/workflows/release.yml/badge.svg)](https://github.com/optio/PebbleNTN/actions/workflows/release.yml) [![CodeQL](https://github.com/optio/PebbleNTN/actions/workflows/codeql.yml/badge.svg)](https://github.com/optio/PebbleNTN/actions/workflows/codeql.yml)

**PebbleNTN** means **Pebble Notification To Navigation**.

Turn-by-turn directions from your phone's nav app, on your pebble.

PebbleNTN reads your navigation app's notifications and shows the next turn, distance, road, and ETA on your Pebble. No need to reach for your phone!

- Google Maps supported now, more coming soon
- Accent colours, light/dark, and three arrow styles
- Metric/imperial, left- or right-hand arrow
- Write your own rules, or share logs to help add new apps

Requires the companion Android app with notification access.

Don't forget to ❤️ the [PebbleNTN App](https://apps.repebble.com/ad4971e345854b909b73b1a9)

## Distribution

[PebbleNTN](https://apps.repebble.com/ad4971e345854b909b73b1a9) ships two artifacts on every successful push to `main`
(`.github/workflows/release.yml`): the Android APK (`pebble-ntn.apk`) and the Pebble
watchapp (`pebble-ntn.pbw`), attached to a GitHub Release with `SHA256SUMS.txt`.

The **[Pebble watchapp](https://apps.repebble.com/ad4971e345854b909b73b1a9)** can be installed [directly from the Pebble store](https://apps.repebble.com/ad4971e345854b909b73b1a9).
Alternativly you can download the latest `pebble-ntn.pbw` from [PebbleNTN Releases](https://github.com/optio/PebbleNTN/releases) page. Open it on your phone using the Pebble app to install the .pbw file.

**Android companion** - download `pebble-ntn.apk` from the [GitHub PebbleNTN Releases](https://github.com/optio/PebbleNTN/releases) page. ( The APK is debug-signed until the release keystore secrets are configured, so expect the unknown-sources prompt. )
- For auto-updates, add the repository's releases to **[Obtainium](https://github.com/ImranR98/Obtainium)**  (point it at this repo's [GitHub releases](https://github.com/optio/PebbleNTN/releases). Obtainium then tracks and installs each new release automatically.

Play Store distribution is currently not in scope (but is technically possible if there are enough request)

## Watch glyphs, and why notification icons are not extracted

The watch renders maneuvers from **built-in glyph packs** (Classic, Bold, Outline; selectable and
previewable on the watch), recoloured to the watch theme. It deliberately does **not** extract the
navigation app's own arrow icon from the notification and forward it. That choice is intentional:
- **Privacy / minimal snapshot** The app reads only the documented text fields of a
  notification and never its icons, PendingIntents, actions or RemoteViews. Keeping icons out of the   pipeline preserves the "no data collected" posture and the audited minimal-snapshot guarantee.
- **Store safety.** Notification-access apps are held to a high bar; not reaching into notification imagery keeps the data-handling story simple and defensible.
- **Cross-app compatibility.** Built-in glyphs render identically for every navigation app (Google
  Maps, Waze, OsmAnd, …) and every language, instead of depending on each app's private icon format. An unclassified maneuver falls back to a clear "?" glyph rather than guessing.

