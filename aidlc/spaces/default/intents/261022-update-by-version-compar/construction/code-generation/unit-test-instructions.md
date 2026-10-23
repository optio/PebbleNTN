# Unit Test Instructions — Version comparison as the default update check

## Framework and Setup

These are plain JUnit 4 JVM tests of a pure Kotlin function. They run through Gradle and need JDK 21 and the Android SDK.

## How to Run This Work's Tests

From the repo root:

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.update.UpdateCheckMessageTest'
```

Plan Step 2 checks the runner with `--tests 'com.pebblentn.app.update.UpdateCheckRepositoryTest'` before the new test exists. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test | Case | Requirement |
|---|---|---|
| UpdateCheckMessageTest | GitHub up to date, watch `0.0.75` newer than phone `0.0.74` → Available("0.0.75") | FR1.1 |
| UpdateCheckMessageTest | Neither source newer → UpToDate | FR1.1 |
| UpdateCheckMessageTest | FAILED, no watch update → Failed | FR1.1 |
| UpdateCheckMessageTest | FAILED, watch update → Available | FR1.2 |
| UpdateCheckMessageTest | GitHub `v0.0.76` → Available("0.0.76") | FR1.1 |

The texts and comments are checked by the build, lint and review. The texts are also checked on a device.

## Coverage Target

Minimal strategy: one test per unit-testable requirement (FR1.1, FR1.2) and a happy path for `updateCheckMessage`. Express has no line-coverage floor.

## Mocking and Test Data

No mocks. The tests build `UpdateState(currentVersion, latestVersion, watchAppVersion = …)` directly and pass `UpdateCheckOutcome` values.
