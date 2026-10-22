# Deployment Execution Questions — Version-compare update detection

Context:
- **How releases work:** every push to `main` runs `.github/workflows/release.yml`. The workflow verifies the commit, bumps the version, and publishes a public GitHub release with the APK and the PBW.
- **What deploying means here:** merging branch `feat/version-compare-update` into `main`. The branch holds the #94 deployment record commit and this work, which is still uncommitted. The merge would publish v0.0.72.
- **Checks:** `scripts/test-all.sh` passes locally. There are no migrations and no protocol change.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push the branch and open a pull request, then stop: you review and merge (the merge triggers the release)
B. Commit, push, open the pull request, wait for CI to pass, then merge it, which publishes the next public release (v0.0.72)
C. Commit locally only: don't push yet
X. Other (please specify)

[Answer]: B
