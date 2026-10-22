# Unit Test Instructions — Phone app update detected by version comparison

## Framework and Setup

- **Watch (host C):** `watchapp/tests/test_version_compare.c` has a `main` with a small `check()` helper and returns non-zero on failure. It compiles against only `watchapp/src/c/version_compare.c`, with `gcc -std=c11 -Wall -Wextra -Werror`. No Pebble SDK is needed. Prerequisite: `gcc` on PATH.
- **Phone (JVM):** JUnit 4 with Robolectric where a Context is needed (`UpdateCheckRepositoryTest`). The other new tests are plain JVM tests of pure functions. They run through Gradle. Prerequisites: JDK 21 and the Android SDK.

## How to Run This Work's Tests

Scoped to the units this work touches. Run from the repo root. Create the git-ignored `watchapp/build/` folder first if it is missing.

```bash
gcc -std=c11 -Wall -Wextra -Werror -o watchapp/build/test_version_compare watchapp/tests/test_version_compare.c watchapp/src/c/version_compare.c && watchapp/build/test_version_compare
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.update.AppVersionsTest' --tests 'com.pebblentn.app.update.UpdateCheckRepositoryTest' --tests 'com.pebblentn.app.update.InstallSourceTest' --tests 'com.pebblentn.app.protocol.ProtocolCodecTest' --tests 'com.pebblentn.app.pebble.NavigationControllerTest' --tests 'com.pebblentn.app.ui.dashboard.UpdateCardTest'
```

Plan Step 2 checks the runners before the first new test. It runs `scripts/test-watchapp-unit.sh` and the Gradle command with the test classes that already exist. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test file | Cases | Requirements |
|---|---|---|
| watchapp/tests/test_version_compare.c (new) | `0.0.10` > `0.0.9`; `1.2` = `1.2.0`; `v0.0.71` parses; `0.0.71-debug`, `0.0.x`, `0..1`, `""`, NULL and a 10-digit part are rejected; never "newer" when one side is unparsable; effective update: own newer → own, reported higher → reported, own not newer → reported or "", phone unparsable → reported only | FR1.1–FR1.3, FR2.1, FR2.2 |
| AppVersionsTest (extended) | the same strict cases on the phone; the existing tolerant cases unchanged | FR1.4 |
| UpdateCheckRepositoryTest (extended) | watch newer → update to the watch version with no fetch; same value again → no write; an unparsable watch version → no update; GitHub `0.0.73` and watch `0.0.72` → `0.0.73`; GitHub `0.0.72` and watch `0.0.74` → `0.0.74` | FR3.1, FR3.3, FR3.4, NFR2, NFR3 |
| InstallSourceTest (new) | each F-Droid and Obtainium package name; `null`, `com.android.packageinstaller` and `com.android.vending` → GitHub | FR4.1 |
| ProtocolCodecTest (extended) | WATCH_READY with key 11 → the version; without it → null; another event → null | FR3.1 |
| NavigationControllerTest (extended) | an inbound WATCH_READY with key 11 → the callback gets the version; the self-made ready event → no call | FR3.1, FR3.2 |
| UpdateCardTest (new) | the main source and the other two sources for each source; the F-Droid delay line only for F-Droid | FR5.2, FR5.4 |

That is about 30 new assertions. The parts that go through Pebble or Compose APIs are checked in the emulator or by compiling, not by unit tests:
- the watch's notice, idle text and subtitle from the effective version (FR2.3, plan Step 14);
- the card layout and the dialog (FR5.1, FR5.3);
- the source destinations (FR4.2).

## Coverage Target

Minimal strategy: at least one test per unit-testable requirement (FR1.1–FR1.4, FR2.1, FR2.2, FR3.1–FR3.4, FR4.1, FR5.2, FR5.4) and a happy path per unit (`version_compare`, `AppVersions`, `UpdateCheckRepository`, `InstallSources`, `ProtocolCodec`, `NavigationController`, `UpdateCard`). There is no line-coverage floor for the express scope.

## Mocking and Test Data

- `version_compare`, `InstallSources.fromInstaller`, `AppVersions` and `UpdateCard` are pure: no mocks, inline literals.
- `UpdateCheckRepositoryTest` keeps its `FakeFetcher` (counting calls, to prove no fetch) and its injected `now` clock.
- `NavigationControllerTest` uses its existing fake transport and a recording lambda for `onWatchAppVersion`.
