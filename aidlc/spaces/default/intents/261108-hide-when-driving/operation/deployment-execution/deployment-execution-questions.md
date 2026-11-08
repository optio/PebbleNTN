# Deployment Execution Questions: "When driving" (#132)

## Question 1
How far should I take the release now? Merging to `main` makes `release.yml` publish v0.0.82 (GitHub release with APK and `watchapp.pbw`; F-Droid follows).

A. Commit on `feat/hide-when-driving`, push, open the PR, wait for its checks and merge it, then follow the release run and run `play-release` for `v0.0.82` (recommended: the full release as planned; you then upload the AAB to the Play closed test and the PBW to the Pebble appstore)
B. Commit, push and open the PR only; you review and merge it yourself
C. Commit on the branch only, no push yet
X. Other (please specify)

[Answer]: A
