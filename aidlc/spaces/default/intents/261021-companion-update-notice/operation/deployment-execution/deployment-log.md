# Deployment Log — #94

## Target

The existing release pipeline, `.github/workflows/release.yml`. It runs on every push to `main`: it verifies the commit, bumps the version, and publishes a public GitHub release with `pebble-ntn.apk`, `pebble-ntn.pbw` and `SHA256SUMS.txt`. There is no infrastructure or migration to apply.

## Decision

Q1: B, "Push, PR, merge" (deployment-execution-questions.md). The person chose to merge once CI passed.

## Timeline (UTC, 2026-10-22)

| Time | Event |
|---|---|
| — | Commits on `feat/companion-update-notice`: f29f81e (feature) and 175dac8 (AI-DLC record). The pre-commit checks passed for both. |
| — | PR https://github.com/optio/PebbleNTN/pull/107 opened. Checks passed: build (watchapp), build (android), validate (rules), Analyze (java-kotlin), CodeQL. |
| 18:13:24 | PR #107 merged (merge commit 2c98fe1). Issue #94 closed. |
| 18:13:28 | Release workflow run 39829454565 starts. |
| 18:23:04 | Release v0.0.71 published. The bump commit is a1f4764 `chore(release): v0.0.71 [skip ci]`. |

## Marketplace Changelog (v0.0.71)

- The watch now tells you when a newer phone app is available. A short notice appears once per new version: after the route if you're navigating, otherwise when you open the app.
- "No navigation detected" now mentions a pending phone app update, and the launcher shows "Update: {version}".
- The phone app reminds you to check for updates when the last check is over 30 days old, and offers weekly automatic checks.

## Rollback

- **Code:** open a PR from a branch with `git revert -m 1 2c98fe1`. Merging it publishes a new release without #94.
- **Compatibility:** the protocol change is a minor bump (1.2), so a rolled-back phone (1.1) still works with a 1.2 watchapp, and the reverse also works. The watch simply shows no update UI.
- **Stored data:** watch persist keys 17 and 18 stay unread after a rollback, which is harmless. The phone's `last_check_millis` is an existing key.
- **Store listings:** reupload the previous PBW (v0.0.70) to the Rebble store if needed.
