# Deployment Execution Questions — Prepare the first Google Play release (#114)

Context:
- **Release pipeline:** every push to `main` runs `.github/workflows/release.yml`, which publishes a public GitHub release, unless the commit message contains `[skip ci]`.
- **What deploying means here:** merging `feat/play-release` into `main` with no release and no version bump. The changes ship with the next normal release.
- **Checks:** `scripts/test-all.sh` passes locally, and the Android 16 phone check passed. `[skip ci]` also skips the PR checks, which is why the suite ran locally.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push the branch and open a pull request, then stop
B. Commit, push, open the pull request and merge it, with `[skip ci]` in every commit message and in the merge commit, so nothing is released
C. Commit locally only: don't push yet
X. Other (please specify)

[Answer]: B — "work on #114 but with [skip ci] commit and merge messages so that this does not yet trigger a new release or new version"
