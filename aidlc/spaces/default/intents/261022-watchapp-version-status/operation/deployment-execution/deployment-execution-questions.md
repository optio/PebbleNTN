# Deployment Execution Questions — Watchapp version in the dashboard status

Context:
- **Release pipeline:** every push to `main` runs `.github/workflows/release.yml`, which publishes a public GitHub release (APK and PBW).
- **What deploying means:** merge branch `feat/watchapp-version-status` into `main`. The merge would publish v0.0.74.
- **Checks:** `scripts/test-all.sh` passes locally.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push the branch and open a pull request, then stop: you review and merge (the merge triggers the release)
B. Commit, push, open the pull request, wait for CI to pass, then merge it, which publishes the next public release (v0.0.74)
C. Commit locally only: don't push yet
X. Other (please specify)

[Answer]: B
