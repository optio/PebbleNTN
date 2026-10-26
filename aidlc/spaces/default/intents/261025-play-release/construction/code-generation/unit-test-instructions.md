# Unit Test Instructions — Prepare the first Google Play release (#114)

## Framework and Setup

- **Phone:** JUnit 4 JVM tests through Gradle (JDK 21, Android SDK). Compose UI tests use the project's existing Robolectric setup (sdk 34).
- **Graphics:** `tools/play_store_graphics.py --check`. It needs Pillow, which the pebble tool's Python has; `scripts/test-all.sh` skips the check with a note when Pillow is missing.
- **Watch:** no new host tests. The QR is checked by decoding the regenerated image and by `pebble build`.

## How to Run This Work's Tests

From the repo root:

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.update.*' --tests 'com.pebblentn.app.ui.*'
android/gradlew -p android :app:verifyReleaseTargetSdk
<pebble-tool-python> tools/play_store_graphics.py --check
scripts/test-all.sh
```

## Tests

| Test | Cases | Requirement |
|---|---|---|
| InstallSourceTest (updated) | `com.android.vending` → PLAY; null → GITHUB; F-Droid clients → FDROID; Obtainium → OBTAINIUM; an unknown installer → GITHUB | FR2 |
| Update policy test (new) | PLAY: no GitHub check, stored version ignored, no other sources, delay note; GITHUB / FDROID / OBTAINIUM unchanged | FR2 |
| UpdateCheckRepository test (new or extended) | stored GitHub `latest_version` newer: available for GITHUB, not for PLAY; watchapp newer: available for PLAY too | FR2b, FR2c |
| UpdateCardTest (extended) | PLAY card: "Update from Google Play", delay line, no other sources | FR2c |
| Dashboard Compose test (new or extended) | PLAY: no stale prompt, no "Check for updates now", no weekly toggle; GITHUB keeps them | FR2a |
| Onboarding Compose test (new or extended) | PLAY shows the disclosure without "GitHub"; GITHUB shows today's text | FR2d |
| verifyReleaseTargetSdk | passes at 36; fails when targetSdk < floor (manual, reverted) | FR1 |
| play_store_graphics.py --check | icon 512×512; feature graphic 1024×500; every screenshot within 320–3840 px with a ratio of at most 2:1; at least 2 phone screenshots | FR4 |
| Screenshot recapture (revision 1) | every phone and watch image comes from the current build: the phone shots are from the debug APK installed as a Play install (no GitHub update controls visible), and the watch shots show About as the first Settings row; `--check` passes | FR4 |
| QR decode | `connect_qr.png` decodes to `https://github.com/optio/PebbleNTN#distribution` and is 29×29 modules | FR6 |

## Coverage Target

Minimal strategy: one test per unit-testable requirement (FR1, FR2, FR4, FR6), a happy path per new component, and at least two edge cases for the install-source detection. Express has no line-coverage floor.

## Mocking and Test Data

The existing fakes for the update repository, SharedPreferences through Robolectric, and installer package names as plain strings.
