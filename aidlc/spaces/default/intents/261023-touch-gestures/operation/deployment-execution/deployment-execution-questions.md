# Deployment Execution Questions — Shortcuts (#12)

Context:
- **Release pipeline:** every push to `main` runs `.github/workflows/release.yml`, which publishes a public GitHub release with the APK and the PBW.
- **What deploying means here:** merging branch `feat/touch-gestures` into `main`. The merge would publish v0.0.76. This release changes the watchapp, so its PBW also goes to the Pebble store by hand.
- **Checks:** `scripts/test-all.sh` passes locally, and all five watch platforms build.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push the branch and open a pull request, then stop: you review and merge (the merge triggers the release)
B. Commit, push, open the pull request, wait for CI to pass, then merge it, which publishes the next public release (v0.0.76)
C. Commit locally only: don't push yet
X. Other (please specify)

[Answer]: B
