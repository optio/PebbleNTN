# Deployment Log — Vela Maps support (v0.0.81)

## Decision

Q1: A. Push, PR, merge, which publishes v0.0.81.

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-30 | Commit 75c2fe6 on `feat/vela-maps`. Pre-commit passed. |
| 2026-10-30 | PR https://github.com/optio/PebbleNTN/pull/122: checks passed (build, validate, Analyze, CodeQL). Merged as 9e00fbb; closes #120. |
| 2026-10-30 23:13 | **Release job failed** at "Install Pebble SDK": the new pebble-tool manages the SDK's virtual environment with `uv` ("uv not found"). Nothing was tagged or published. |
| 2026-10-30 | PR https://github.com/optio/PebbleNTN/pull/123 installs `uv` next to `pebble-tool` in `release.yml` and `watchapp.yml`. The watchapp build on the PR installed SDK 4.33.1. Merged as 0951615. |
| 2026-10-30 23:32:34 | Release **v0.0.81** published: APK and PBW, versionCode 81, targetSdk 36, signed with the release key `75:EF:A3:EA:…`. |
| 2026-10-30 | `play-release` for `v0.0.81` (run 41145119836) succeeded. AAB signed with the upload key (SHA-1 `BC:DA:23:74:…`). Copied to `C:\Users\sacha\Downloads\pebblentn-0.0.81.aab`. |

## Marketplace Changelog (v0.0.81)

- New: Vela Maps support (English), with turn arrows, distances and arrival time on your watch.
- Vela Maps' turn instructions were checked on routes in seven European cities and in Austin, Texas.

## Rollback

From a branch, open a PR with `git revert -m 1 9e00fbb`. Merging it publishes a release without Vela Maps. The CI fix (0951615) should stay.
