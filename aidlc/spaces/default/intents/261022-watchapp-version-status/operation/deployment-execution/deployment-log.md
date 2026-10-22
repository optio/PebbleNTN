# Deployment Log — Watchapp version in the dashboard status

## Target

The existing release pipeline, `.github/workflows/release.yml`: every push to `main` publishes a GitHub release with the APK, the PBW and `SHA256SUMS.txt`. There is no infrastructure, migration or protocol change.

## Decision

Q1: B, "Push, PR, merge". The person chose to merge once CI passed.

## Timeline (UTC, 2026-10-22)

| Time | Event |
|---|---|
| — | Commits on `feat/watchapp-version-status`: 41e1277 (feature) and e22fa16 (AI-DLC record). Pre-commit passed. |
| — | PR https://github.com/optio/PebbleNTN/pull/109 opened. Checks passed: build (android), Analyze (java-kotlin), CodeQL. |
| 22:02:32 | PR #109 merged (merge commit bb2d910). |
| 22:02:35 | Release workflow starts. |
| 22:12:29 | Release **v0.0.74** published. |

Only the Android app changed. The PBW is republished as 0.0.74 because both apps share one version number, but its watch code is the same as v0.0.73, so no Rebble store upload is needed.

## Marketplace Changelog (v0.0.74, phone app)

- The dashboard's Status section shows the watchapp version the phone last received ("0.0.74 (last seen)"), or "Not received yet". This helps check whether your Pebble app passes watch messages on to PebbleNTN.
- The Status rows are now closer together.

## Rollback

In a PR from a branch, run `git revert -m 1 bb2d910`. Merging it publishes a new release without the row. No stored data is involved.
