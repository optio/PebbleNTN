# Deployment Log — Version comparison as the default update check

## Target

The existing release pipeline, `.github/workflows/release.yml`. Every push to `main` publishes a GitHub release with the APK, the PBW and `SHA256SUMS.txt`.

## Decision

Q1: B, "Push, PR, merge".

## Timeline (UTC, 2026-10-23)

| Time | Event |
|---|---|
| — | Commits on `feat/update-by-version-compare`: 83a5a62 (feature) and b70a865 (AI-DLC record). Pre-commit passed. |
| — | PR https://github.com/optio/PebbleNTN/pull/110 opened. Checks passed: build (android), Analyze (java-kotlin), CodeQL. |
| 07:17:53 | PR #110 merged (merge commit a4daf7d). |
| 07:17:56 | Release workflow starts. |
| 07:27:48 | Release **v0.0.75** published. |

This change is Android-only. The PBW is republished as 0.0.75 with the same watch code as v0.0.73.

## Marketplace Changelog (v0.0.75, phone app)

- PebbleNTN now finds new versions offline by default: it compares the phone app with your watchapp. The GitHub check stays optional, either manually or weekly.
- "Check for updates now" reports a newer version from either source, so it no longer says "up to date" while an update is waiting.

## Rollback

From a branch, open a PR with `git revert -m 1 a4daf7d`. Merging it publishes a release without this change. There is no stored data to undo.
