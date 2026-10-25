# Deployment Log — Settings sync with the phone app (#102)

## Target

The existing release pipeline, `.github/workflows/release.yml`. Every push to `main` publishes a GitHub release with the APK, the PBW and `SHA256SUMS.txt`. The PBW then goes to the Pebble store by hand.

## Decision

Q1: B, "go ahead and deploy so that github actions create a new release".

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-25 | Before release: the person tested a debug APK and PBW on their phone and watch, and asked for new defaults, About first and larger, and the Watch settings row first. These were built, tested, and installed again. |
| 2026-10-25 | Commits on `feat/settings-sync`: fecc4c5 (feature) and the AI-DLC record. Pre-commit passed. |
| 2026-10-25 | PR https://github.com/optio/PebbleNTN/pull/112 opened. Checks passed: build (watchapp, android), validate, Analyze (java-kotlin), CodeQL. |
| 2026-10-25 | PR #112 merged (merge commit efd4c1a). Issue #102 is closed by the PR. |
| 2026-10-25 22:22:07 | Release **v0.0.77** published. |

Both the watchapp and the phone app changed, so the PBW must be uploaded to the Pebble store.

## Marketplace Changelog (v0.0.77)

- Change the watchapp's settings from the phone: the PebbleNTN app has a new Watch settings screen, kept in sync both ways with the watch, with a preview of how each look will appear.
- Vibration for a new instruction now has a real silent Off, which is the new default; the phone's short buzz is still available as "Short pulse".
- New defaults: early warning at 25 m on every update; on the Pebble Time 2 touch is on, with Tap and Down lighting the backlight and Up and the side swipes switching the ETA.
- About is now the first Settings row, in larger text.

## Rollback

From a branch, open a PR with `git revert -m 1 efd4c1a`. Merging it publishes a release without this change. Protocol 1.3 keys are ignored by older versions, and stored settings keep their keys, so there's no stored data to undo; key 28 (the settings revision) is simply left unused.
