# Unit Test Instructions — Watchapp version in the dashboard status

## Framework and Setup

These are plain JUnit 4 JVM tests of a pure Kotlin function, with no Robolectric and no Compose. They run through Gradle. Prerequisites: JDK 21 and the Android SDK.

## How to Run This Work's Tests

From the repo root:

```bash
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.dashboard.WatchAppVersionRowTest'
```

Before the new test exists, plan Step 2 runs the same command with `--tests 'com.pebblentn.app.ui.dashboard.UpdateCardTest'` to confirm the runner works. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test | Case | Requirement |
|---|---|---|
| WatchAppVersionRowTest | `0.0.73` → "0.0.73 (last seen)" | FR1.2 |
| WatchAppVersionRowTest | null and blank (`""`, `"  "`) → "Not received yet" | FR1.3 |
| WatchAppVersionRowTest | `0.0.72-debug` → "0.0.72-debug (last seen)", shown as stored | FR1.2 |

The row's position, its live update and its layout go through Compose. They are checked by the build and on a device, not by unit tests.

## Coverage Target

Minimal strategy: one test per unit-testable requirement (FR1.2, FR1.3) and a happy path for the one new unit (`watchAppVersionText`). There is no line-coverage floor for the express scope.

## Mocking and Test Data

No mocks. The test passes `{ "$it (last seen)" }` and `"Not received yet"` as the formatting inputs, plus the literal versions above.
