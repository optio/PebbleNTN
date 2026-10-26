# Build and Test Summary — Prepare the first Google Play release (#114)

## Status: Ready (to merge with `[skip ci]`)

- **Device:** the Android 16 phone check passed, with the build installed as a Play install: onboarding, the dashboard, both navigation styles, rule-editor back and a route to the watch (see test-results.md).

- **Build:** target SDK 36 passes the Play gate, which now really fails below the floor. The debug, release and bundle builds succeed, and the watchapp builds on all five platforms.
- **Tests:** `scripts/test-all.sh` passes: 500 Android tests per variant, the graphics check and its 12 tests, and the spec and workflow checks.
- **Store images:** every one comes from the current version and passes Play's size rules.
- **Traceability:** pass, FR1–FR8.

## Quality Targets

| Target | Result |
|---|---|
| Minimal strategy: one test per unit-testable requirement, plus a happy path per component | Met: FR1 gate, FR2 policy, repository and UI, FR4 check, FR6 QR decode |
| Existing suite stays green | Met |
| No release, no version bump | Met: 0.0.77, and every commit and the merge carry `[skip ci]` |

## Open Items for the Person

- **After the first internal-testing install from Play,** check the "Update from Google Play" card once the watchapp is newer than the phone app.
- **Your phone has the debug test build** (debug key). Replace it with the next release: uninstall it first.
- **Play Console steps** in `docs/PLAY_CONSOLE.md`:
  - app signing with the existing release key;
  - the upload key and its secrets;
  - the content forms;
  - the closed test with 12 testers for 14 days;
  - developer verification.
