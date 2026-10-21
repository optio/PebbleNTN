# Deployment Execution Questions — #103

Context: this project ships through its existing pipeline. Every push to `main` runs `.github/workflows/release.yml`, which verifies the commit, bumps the version and publishes a public GitHub release (APK + PBW). From there the PBW goes to the Rebble store and the APK to F-Droid and Obtainium. So "deploying" #103 means getting branch `feat/about-companion-version` (2 commits: the AI-DLC workspace shell, then the #103 change) merged into `main`. Pre-deployment checks pass locally (`scripts/test-all.sh` green). There are no migrations and no dependent services.

## Q1. [Release engineer] How far should I take the deployment?

A. Push the branch and open a pull request that closes #103, then stop: you review and merge (the merge triggers the release)
B. Push, open the pull request, wait for CI to pass, then merge it, which publishes the next public release (v0.0.69)
C. Don't push yet: keep everything local on the branch
X. Other (please specify)

[Answer]: B

## Q2. [Release engineer] PR #106 is green, but my merge was blocked by this session's permission settings. How do you want to merge it?

Context: https://github.com/optio/PebbleNTN/pull/106 passed build, CodeQL and Analyze. The merge publishes v0.0.69.

A. I merged it myself (for example with `! gh pr merge 106 --merge --delete-branch`): continue from there
B. Leave the PR open for now; record the deployment as pending the merge
X. Other (please specify)

[Answer]: X. Other: on 2026-10-21 the person asked "Ok merge all open pr's", and PR #106 was merged at their request (merge commit 8ba0c47).
