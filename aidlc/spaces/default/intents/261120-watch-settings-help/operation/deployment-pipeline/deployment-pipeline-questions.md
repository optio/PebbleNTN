# Deployment Pipeline Questions: Watch settings help text (#139)

The project's pipeline already exists and is reused unchanged:
- every push to `main` runs `.github/workflows/release.yml`: validation, Android tests and lint, version bump, changelog, F-Droid changelog, signed APK and watchapp PBW, and the GitHub release;
- F-Droid builds the release tag about a day later;
- `.github/workflows/play-release.yml` builds the Play AAB from a tag on demand.

This change is phone-only. There is no watchapp, protocol or rule-pack change, so no `minimumAppVersionCode` or rule rollout is involved. Strategy, promotion gates and rollback follow from that, so only one choice is open.

## Q1. [Release engineer] Should this release (0.0.84) also go to the Google Play closed test?

A. Yes: after the GitHub release, run `play-release` for `v0.0.84` and upload the AAB to the closed testing track (Alpha), so testers get the help texts. This is recommended: it replaces 0.0.83 if that isn't uploaded yet, and a new upload doesn't reset the 14-day clock.
B. No: GitHub, F-Droid and Obtainium only for now.
X. Other (please specify)

[Answer]: A **Mode:** guided
