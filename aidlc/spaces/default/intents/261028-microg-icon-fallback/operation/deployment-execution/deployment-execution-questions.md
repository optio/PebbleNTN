# Deployment Execution Questions — MicroG arrows and user-added package names (v0.0.79)

Context:
- **Release pipeline:** every push to `main` runs `release.yml`, which publishes a GitHub release with the APK and PBW.
- **What deploying means here:** merging `fix/microg-icon-fallback` into `main`, which publishes v0.0.79 (versionCode 79).
- **Google Play:** the Play bundle follows via `play-release` once the new upload key is valid (2026-10-30 00:14 UTC).

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push, open the PR, wait for CI, then merge normally, which publishes v0.0.79
B. Merge with `[skip ci]`, with no release
C. PR only
X. Other (please specify)

[Answer]: A — the person approved "move on to deployment (release v0.0.79)" at the Build and Test gate
