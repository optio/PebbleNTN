# Deployment Execution Questions — Vela Maps support (#120)

Context:
- **Release pipeline:** every push to `main` runs `release.yml`, which publishes a GitHub release with the APK and PBW.
- **What deploying means here:** merging a feature branch into `main`, which publishes v0.0.81 (versionCode 81) and closes #120.
- **Google Play:** the Play bundle follows via `play-release` for `v0.0.81`.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit on a branch, push, open the PR, wait for CI, then merge normally, which publishes v0.0.81
B. Merge with `[skip ci]`, with no release
C. PR only
X. Other (please specify)

[Answer]: A
