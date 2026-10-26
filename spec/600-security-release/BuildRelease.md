# Build, CI and Release

## Terminal tools

- JDK 21 unless the chosen stable Android Gradle Plugin requires another supported LTS.
- Android SDK command-line tools.
- Gradle Wrapper.
- adb, emulator, sdkmanager, avdmanager.
- Pebble SDK and `pebble` CLI.
- Git and GitHub CLI optional.
- Python only for support scripts where Kotlin/shell is not suitable.

## Root commands

```bash
./scripts/bootstrap.sh
./scripts/build-all.sh
./scripts/test-all.sh
./scripts/run-android-emulator.sh
./scripts/install-android-debug.sh
./scripts/build-watchapp.sh
./scripts/validate-rules.sh
```

## Artifacts

- Android debug APK.
- Android release AAB.
- Optional signed release APK for direct testing.
- Pebble `.pbw`.
- Official rules JSON and signature.
- Rule regression report.
- SBOM and dependency license report where practical.

## GitHub Actions

Workflows:
- `android.yml`: unit tests, lint, debug APK.
- `watchapp.yml`: watch build.
- `rules.yml`: schema, canonicalization, fixtures, regression.
- `release.yml`: signed AAB using GitHub environment secrets, PBW, checksums and release notes.
- `codeql.yml` or equivalent static analysis.

Pull requests cannot merge unless all required checks pass.

## Distribution

- **Android companion:** the GitHub Release APK (`pebble-ntn.apk`). For hands-off updates users add
  the repository's releases to Obtainium, which tracks and installs each new release, or install
  from F-Droid, which verifies the reproducible build and ships our signature. The APK is
  debug-signed until release keystore secrets are set. Google Play gets an App Bundle of the same
  app (REQ-ANDROID-022); a Play install is detected at runtime and updates from Play only. The
  README's "Distribution" section lists every source and is where the watch's install QR points.
- **Watchapp:** the GitHub Release `.pbw` (`pebble-ntn.pbw`), installable through the Rebble / Pebble
  mobile app or side-loaded with `pebble install --phone`, and intended for listing on the RePebble
  appstore.

## Play Store

- MIT license applies to source; privacy policy remains a separate product document.
- Complete Data Safety accurately.
- Justify notification access as core functionality.
- Provide prominent disclosure.
- Build an Android App Bundle.
- Use Play App Signing with the existing release key as the app signing key, so every source ships
  one signature, and a separate upload key, held only in the `PLAY_UPLOAD_*` secrets of the
  protected `release` environment; protect and back up both keys offline.
- Build the bundle with the manual `play-release.yml` workflow for a release tag; it attaches the
  AAB to the run, and the maintainer uploads it in Play Console.
- Release automation verifies target SDK policy at release time: `verifyReleaseTargetSdk` runs in
  `release.yml` and `play-release.yml` and fails the job when targetSdk is below Play's floor.
- Every Play Console answer and step is in `docs/PLAY_CONSOLE.md` (REQ-ANDROID-022).
