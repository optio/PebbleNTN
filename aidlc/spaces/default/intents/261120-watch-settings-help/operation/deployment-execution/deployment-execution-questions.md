# Deployment Execution Questions: Watch settings help text (#139)

Pre-deployment checks:
- Build and Test passed: `scripts/test-all.sh` green, 768 JVM tests, release APK +29,892 bytes.
- No database migration is needed (nothing stored changes) and no dependent service is involved.
- No deployment window is needed: users update when their store or Obtainium offers 0.0.84.

## Q1. [Release engineer] How far should I take the release now?

Merging to `main` makes `release.yml` publish v0.0.84: a GitHub release with the APK and `watchapp.pbw`, followed by F-Droid.

A. Commit on `feat/watch-settings-help`, push, open the PR, wait for its checks, merge it, follow the release run, then run `play-release` for `v0.0.84`. This is recommended: it is the full release as planned. You then upload the AAB to the Play closed test and, optionally, the PBW to the Pebble appstore.
B. Commit, push and open the PR only. You review and merge it yourself.
C. Commit on the branch only, with no push yet.
X. Other (please specify)

[Answer]: A **Mode:** guided
