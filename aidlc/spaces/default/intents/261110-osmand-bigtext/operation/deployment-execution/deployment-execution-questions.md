# Deployment Execution Questions: OsmAnd street and arrival time (#134, #136)

## Question 1
How far should I take the release now? Merging to `main` makes `release.yml` publish v0.0.83: a GitHub release with the APK and `watchapp.pbw`, followed by F-Droid. The new rule packs are then served from `main`.

A. Commit on `feat/osmand-bigtext`, push, open the PR, wait for its checks, merge it, follow the release run, and then run `play-release` for `v0.0.83` (recommended: the full release as planned; you then upload the AAB to the Play closed test and the PBW to the Pebble appstore)
B. Commit, push and open the PR only; you review and merge it yourself
C. Commit on the branch only, no push yet
X. Other (please specify)

[Answer]: A **Mode:** guided
