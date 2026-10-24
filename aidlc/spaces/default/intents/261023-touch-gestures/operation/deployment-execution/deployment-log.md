# Deployment Log — Shortcuts: touch gestures and UP/DOWN buttons (#12)

## Target

The existing release pipeline, `.github/workflows/release.yml`. Every push to `main` publishes a GitHub release with the APK, the PBW and `SHA256SUMS.txt`. The PBW then goes to the Pebble store by hand.

## Decision

Q1: B, "Push, PR, merge".

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-23 | Commits on `feat/touch-gestures`: 68ebc8c (feature) and 784b7a2 (AI-DLC record). Pre-commit passed. |
| 2026-10-23 | PR https://github.com/optio/PebbleNTN/pull/111 opened. Checks passed: build, Analyze (java-kotlin), CodeQL. |
| 2026-10-24 00:00:13 | PR #111 merged (merge commit 44f1feb). Issue #12 is closed by the PR. |
| 2026-10-24 00:09:33 | Release **v0.0.76** published. |

This release changes the watchapp, so the PBW must be uploaded to the Pebble store; the phone app is unchanged apart from the version.

## Marketplace Changelog (v0.0.76)

- New Settings → Shortcuts: put an action on the UP and DOWN buttons, for example switch the ETA, invert the colours, change the arrow side or glyphs, turn vibration on or off, or light the backlight.
- On the Pebble Time 2, swipes and a tap can run shortcuts too. Touch is off until you turn it on, and it follows the watch's own touch setting.
- A short banner tells you what changed.

## Rollback

From a branch, open a PR with `git revert -m 1 44f1feb`. Merging it publishes a release without this change. The new settings (keys 19–27) are ignored by older versions, so there's no stored data to undo.
