# Deployment Log — Prepare the first Google Play release (#114)

## Target

`main`, merged with no release. Every commit and the merge commit carry `[skip ci]`, so `.github/workflows/release.yml` and the other workflows did not run. The version stays 0.0.77, and the changes ship with the next normal release.

## Decision

Q1: B, "work on #114 but with [skip ci] commit and merge messages so that this does not yet trigger a new release or new version".

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-26 | Phone check on Android 16, with the build installed as a Play install: passed (see `construction/build-and-test/test-results.md`). |
| 2026-10-26 | Commits on `feat/play-release`: eb9b240 (feature) and 4de79f3 (AI-DLC record), both `[skip ci]`. Pre-commit passed (secrets, spec integrity, protocol, version 0.0.77, syntax, unit tests, lint). |
| 2026-10-26 | PR https://github.com/optio/PebbleNTN/pull/115 opened. PR checks were skipped by `[skip ci]`; the full suite ran locally. |
| 2026-10-26 | PR #115 merged as faf8cb1, "Merge pull request #115 from optio/feat/play-release [skip ci]". Issue #114 closed. |
| 2026-10-26 | No workflow run on faf8cb1. The latest release is still v0.0.77. |

## Rollback

From a branch, open a PR with `git revert -m 1 faf8cb1`, with `[skip ci]` in the message to keep it release-free. Nothing was published, so no user is affected until the next release.
