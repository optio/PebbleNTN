# Deployment Execution Questions — MicroG Maps as a Google Maps variant (#113)

Context:
- **Release pipeline:** every push to `main` runs `.github/workflows/release.yml`, which publishes a public GitHub release with the APK and the PBW, unless the commit message contains `[skip ci]`.
- **What a release would ship:** #114 (Play preparation, targetSdk 36, the new QR) and #113 (MicroG Maps) together, as v0.0.78. The watchapp changed in #114 (QR, About text), so the PBW also goes to the Pebble store by hand.
- **Checks:** `scripts/test-all.sh` passes locally.

## Q1. [Release engineer] How far should I take the deployment?

A. Commit, push, open the pull request, wait for CI to pass, then merge it normally, which publishes v0.0.78
B. Commit, push, open the pull request and merge it with `[skip ci]`, with no release yet
C. Commit, push and open the pull request, then stop: you merge
X. Other (please specify)

[Answer]: A — "If A will also make the play release action create a valid google play version then Aversion" (after the release, the play-release workflow is run for v0.0.78 and its AAB verified)
