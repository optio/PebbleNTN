# Deployment Execution Questions — #94

Context:
- **How the project ships:** every push to `main` runs `.github/workflows/release.yml`. It verifies the commit, bumps the version and publishes a public GitHub release with the APK and PBW. From there the PBW goes to the Rebble store, and the APK to F-Droid and Obtainium.
- **What deploying #94 means:** getting branch `feat/companion-update-notice` merged into `main`. The branch holds the #103 deployment record commit and the uncommitted #94 change, and the merge would publish v0.0.71.
- **Pre-deployment checks:** `scripts/test-all.sh` passes locally.
- **Nothing else to deploy:** there are no migrations and no dependent services.
- **Compatibility:** the protocol goes to 1.2, a minor bump, so the phone and the watch can update in either order.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push the branch and open a pull request that closes #94, then stop: you review and merge (the merge triggers the release)
B. Commit, push, open the pull request, wait for CI to pass, then merge it, which publishes the next public release (v0.0.71)
C. Commit locally only: don't push yet
X. Other (please specify)

[Answer]: B
