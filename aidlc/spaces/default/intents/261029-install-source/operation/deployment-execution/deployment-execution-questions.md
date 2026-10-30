# Deployment Execution Questions — install/update source (#119) and a second MicroG Maps package (#113)

Context:
- **Release pipeline:** every push to `main` runs `release.yml`, which publishes a GitHub release with the APK and PBW.
- **What deploying means here:** merging a feature branch into `main`, which publishes v0.0.80 (versionCode 80).
- **Google Play:** the Play bundle follows via `play-release` for `v0.0.80`.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit on a branch, push, open the PR, wait for CI, then merge normally, which publishes v0.0.80
B. Merge with `[skip ci]`, with no release (so #120 can ship in the same release)
C. PR only
X. Other (please specify)

[Answer]: A
