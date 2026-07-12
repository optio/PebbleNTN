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

## Play Store

- MIT license applies to source; privacy policy remains a separate product document.
- Complete Data Safety accurately.
- Justify notification access as core functionality.
- Provide prominent disclosure.
- Build an Android App Bundle.
- Use Play App Signing and protect upload key.
- Release automation verifies target SDK policy at release time.
