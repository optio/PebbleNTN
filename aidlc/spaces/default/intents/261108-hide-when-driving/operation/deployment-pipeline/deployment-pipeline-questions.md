# Deployment Pipeline Questions: "When driving" (#132)

The project's pipeline already exists and is reused unchanged: every push to `main` runs `.github/workflows/release.yml` (validation, Android tests and lint, version bump, changelog, F-Droid changelog, signed APK and watchapp PBW, GitHub release), F-Droid builds the release tag about a day later, and `.github/workflows/play-release.yml` builds the Play AAB from a tag on demand. Strategy, promotion gates and rollback follow from that, so only one choice is open.

## Question 1
The Google Play app is in closed testing (Alpha) with testers being recruited. Should this release also go to the Play closed test?

A. Yes: after the GitHub release, run `play-release` for the new tag and upload the AAB to the closed testing track, so the testers get "When driving" too (recommended: testers are on Play and this is a feature they can try; a new upload does not reset the 14-day clock)
B. No: GitHub, F-Droid and Obtainium only; the Play closed test stays on 0.0.81 for now
X. Other (please specify)

[Answer]: A
