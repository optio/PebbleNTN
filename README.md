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
