# Deployment Execution Questions — Version comparison as the default update check

Context:
- **Release pipeline:** every push to `main` runs `.github/workflows/release.yml`, which publishes a public GitHub release.
- **What deploying means here:** merging branch `feat/update-by-version-compare` into `main`. The merge would publish v0.0.75.
- **Checks:** `scripts/test-all.sh` passes locally.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push the branch and open a pull request, then stop: you review and merge (the merge triggers the release)
B. Commit, push, open the pull request, wait for CI to pass, then merge it, which publishes the next public release (v0.0.75)
C. Commit locally only: don't push yet
X. Other (please specify)

[Answer]: B
