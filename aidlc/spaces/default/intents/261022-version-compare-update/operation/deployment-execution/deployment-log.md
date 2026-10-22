# Deployment Log — Version-compare update detection

## Target

The existing release pipeline, `.github/workflows/release.yml`. It runs on every push to `main`: it verifies the commit, bumps the version, and publishes a public GitHub release with `pebble-ntn.apk`, `pebble-ntn.pbw` and `SHA256SUMS.txt`. There is no infrastructure, migration or protocol change.

## Decision

Q1: B, "Push, PR, merge" (deployment-execution-questions.md). The person chose to merge once CI passed.

## Timeline (UTC, 2026-10-22)

| Time | Event |
|---|---|
| — | Commits on `feat/version-compare-update`: 1ad9ec8 (#94 deployment record), e7a85b7 (feature), c229ea2 (AI-DLC record). The pre-commit checks passed for all three. |
| 19:13:22 | Unrelated: commit 2cd9a30 "Change GitHub Sponsors username", pushed straight to `main`, released v0.0.72. |
| — | PR https://github.com/optio/PebbleNTN/pull/108 opened. Checks passed: build (watchapp), build (android), Analyze (java-kotlin), CodeQL. |
| 20:30:14 | PR #108 merged (merge commit 0ab636d). |
| 20:30:17 | Release workflow starts. |
| 20:39:36 | Release **v0.0.73** published (bump commit 8f63f3c `chore(release): v0.0.73 [skip ci]`). |

The deployment questions expected v0.0.72. It became v0.0.73 because of the unrelated v0.0.72 release above.

## Marketplace Changelog (v0.0.73)

- The watch now notices when the phone app is out of date, with no internet check: if the watchapp is newer than the phone app, it shows the phone app update notice, the idle text and the "Update: {version}" launcher subtitle.
- The phone app's update card shows where the app was installed from ("Update from F-Droid / Obtainium / GitHub"), offers other sources, and tells F-Droid users that F-Droid may take about a day to offer the update.
- With the original Pebble app, the phone also learns that the watchapp is newer and shows the update card.

## Rollback

- **Code:** in a PR from a branch, run `git revert -m 1 0ab636d`. Merging it publishes a new release without this change.
- **Compatibility:** there is no protocol change and no new watch persist key, so a rollback leaves nothing behind on the watch. The phone's `watch_app_version` preference is left unread, which is harmless.
- **Rebble store:** if needed, re-upload the previous PBW.
