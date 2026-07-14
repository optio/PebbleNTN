# PebbleNTN — Engineering Specification

**PebbleNTN** means **Pebble Notification To Navigation**.

This repository specification is an implementation contract for an AI coding agent, principally Claude Code or Codex. It defines an Android companion application that reads selected navigation-app notifications, translates them with declarative rules, and sends normalized turn instructions to a Pebble watchapp.

## Product scope

- Android-only companion application.
- Pebble C watchapp.
- Google Maps rules in the first usable release.
- Other navigation apps may be selected for debug capture before rules exist.
- Notification content is processed only after the posting package passes the enabled-app allowlist.
- Material 3 Android UI with restrained Pebble-inspired styling.
- Declarative JSON rules, simple editor, expert JSON editor, preview, imports and exports.
- Bundled official rules; architecture supports signed official remote updates managed by the project maintainers.
- Debug history and privacy-safe diagnostic export.
- Automatic one-time watchapp launch when navigation starts, configurable by the user.
- Automatic return to watchface when navigation ends, configurable by the user.
- MIT licensed, GitHub-hosted monorepo.

## Normative language

`SHALL`, `SHALL NOT`, `SHOULD`, and `MAY` are normative. Requirement IDs are binding. When prose conflicts with a numbered requirement, the numbered requirement wins.

## Target baseline

- Android language: Kotlin.
- Android UI: Jetpack Compose and Material 3.
- Android minimum SDK: API 31 (Android 12).
- Compile SDK: latest stable installed SDK; baseline 36.
- Target SDK: at least the current Google Play submission requirement; baseline 35 and verified by release automation.
- Persistence: Room.
- JSON: kotlinx.serialization.
- Dependency injection: explicit constructor injection; no Hilt or Dagger.
- Watchapp: Pebble SDK, C, bitmap maneuver resources.
- Build: Gradle Wrapper for Android and `pebble` CLI for watchapp.
- CI: GitHub Actions.
- License: MIT.

## Distribution

PebbleNTN ships two artifacts on every successful push to `main`
(`.github/workflows/release.yml`): the Android APK (`pebble-ntn.apk`) and the Pebble
watchapp (`pebble-ntn.pbw`), attached to a GitHub Release with `SHA256SUMS.txt`.

- **Android companion** — install `pebble-ntn.apk` from the GitHub Releases page. For
  hands-off updates, add the repository's releases to **[Obtainium](https://github.com/ImranR98/Obtainium)**
  (point it at this repo's GitHub releases); Obtainium then tracks and installs each new
  release automatically. The APK is debug-signed until the release keystore secrets are
  configured, so expect the unknown-sources prompt.
- **Pebble watchapp** — install `pebble-ntn.pbw` from the same release through the
  Pebble / **Rebble** mobile app, or side-load it with `pebble install --phone <IP>`. The
  watchapp is also intended for listing on the **[RePebble appstore](https://apps.rebble.io/)**
  so it can be discovered and updated like any other watchapp.

Play Store distribution uses a separately signed AAB produced by the manually triggered
`play-release.yml` workflow.

## Watch glyphs, and why notification icons are not extracted

The watch renders maneuvers from **built-in glyph packs** (Classic, Bold, Outline; selectable and
previewable on the watch), recoloured to the watch theme. It deliberately does **not** extract the
navigation app's own arrow icon from the notification and forward it. That choice is intentional:

- **Privacy / minimal snapshot (REQ-SEC-003).** The app reads only the documented text fields of a
  notification and never its icons, PendingIntents, actions or RemoteViews. Keeping icons out of the
  pipeline preserves the "no data collected" posture and the audited minimal-snapshot guarantee.
- **Store safety.** Notification-access apps are held to a high bar; not reaching into notification
  imagery keeps the data-handling story simple and defensible.
- **Cross-app compatibility.** Built-in glyphs render identically for every navigation app (Google
  Maps, Waze, OsmAnd, …) and every language, instead of depending on each app's private icon format.

An unclassified maneuver falls back to a clear "?" glyph rather than guessing.

## Read order for an AI coding agent

1. `AGENTS.md`
2. `spec/000-overview.md`
3. `requirements/`
4. `spec/200-architecture/`
5. `spec/300-data/`
6. `spec/400-ui/`
7. `spec/500-testing/`
8. `spec/600-security-release/`
9. `spec/800-roadmap/Milestones.md`

No follow-up product questions are required. Where implementation details remain open, this specification supplies a default.
