# Deployment Pipeline Questions: OsmAnd street and arrival time (#134, #136)

The project's pipeline already exists and is reused unchanged:
- every push to `main` runs `.github/workflows/release.yml`: validation, Android tests and lint, version bump, changelog, F-Droid changelog, signed APK and watchapp PBW, GitHub release;
- F-Droid builds the release tag about a day later;
- `.github/workflows/play-release.yml` builds the Play AAB from a tag on demand;
- the remote rule packs are served from `main`. The changed packs declare `minimumAppVersionCode` 83, so 0.0.82 apps keep their current rules.

Strategy, promotion gates and rollback follow from that, so only one choice is open.

## Question 1
The Google Play app is in closed testing (Alpha). Should this release (0.0.83) also go to the Play closed test?

A. Yes: after the GitHub release, run `play-release` for `v0.0.83` and upload the AAB to the closed testing track, so testers get the OsmAnd road line, the arrival times and the Directions text setting (recommended: it replaces 0.0.82 if that is not uploaded yet, and a new upload does not reset the 14-day clock)
B. No: GitHub, F-Droid and Obtainium only for now
X. Other (please specify)

[Answer]: A **Mode:** guided
