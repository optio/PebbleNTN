# Unit Test Instructions — #94 Phone app update notice on the watch

## Framework and Setup

- **Watch (host C):** each `watchapp/tests/test_<unit>.c` has a `main` with a small `check()` helper and returns non-zero on failure. `scripts/test-watchapp-unit.sh` compiles each test against only its matching `watchapp/src/c/<unit>.c` with `gcc -std=c11 -Wall -Wextra -Werror`. No Pebble SDK is needed. Prerequisite: `gcc` on PATH.
- **Phone (JVM):** JUnit 4 with Robolectric (`UpdateCheckRepositoryTest` uses `ApplicationProvider` for SharedPreferences), run through Gradle. Prerequisites: JDK 21 and the Android SDK, as for `scripts/test-all.sh`.

## How to Run This Work's Tests

Scoped to the units this work touches. Run from the repo root.

```bash
gcc -std=c11 -Wall -Wextra -Werror -o build/test_update_notice watchapp/tests/test_update_notice.c watchapp/src/c/update_notice.c && build/test_update_notice
gcc -std=c11 -Wall -Wextra -Werror -o build/test_glance_text watchapp/tests/test_glance_text.c watchapp/src/c/glance_text.c && build/test_glance_text
android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.protocol.ProtocolCodecTest' --tests 'com.pebblentn.app.update.UpdateCheckRepositoryTest' --tests 'com.pebblentn.app.ui.dashboard.UpdatePromptTest'
```

The `build/` output folder is git-ignored. Create it first if it is missing. The runner check before the first test (plan Step 2) runs `scripts/test-watchapp-unit.sh` and the Gradle command with the two test classes that already exist. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test file | Cases | Requirements |
|---|---|---|
| watchapp/tests/test_update_notice.c (new) | record first version; same again → no change; flag clear clears; clear again → no change; flag set with missing or empty version → unchanged; 20-byte version cut to 16 bytes; action SHOW for a new version, NONE once shown, SHOW for a newer version; DEFER while navigating and while main is not on top; NONE with no update; idle text in both states | FR3.1, FR4.1, FR4.3, FR4.4, FR5.1, NFR3 |
| watchapp/tests/test_glance_text.c (extended) | `Update: 0.0.71`; `{`, `}`, `\` dropped; empty version → false; exact fit (15 bytes) → true; small buffer (14 bytes) → false and empty; existing countdown cases unchanged | FR6.1, FR6.2, FR6.3 |
| ProtocolCodecTest (extended) | bit 4 + key 14 `0.0.71` (from `v0.0.71`) on NO_ACTIVE, UPDATE and STOPPED, with other bits kept; no update → bit clear, no key 14; compatibility error has no key 14 | FR2.1, FR2.2, FR2.3, NFR1 |
| UpdateCheckRepositoryTest (extended) | never checked → stale; exactly 30 days → not stale; 30 days + 1 ms → stale; after a forced check → not stale and `lastCheckMillis` updated; a failed check also clears staleness | FR7.1, NFR2 |
| UpdatePromptTest (new) | stale → STALE_CHECK; finished with weekly off → WEEKLY_QUESTION; weekly on → never the question; nothing pending → NONE | FR7.2, FR7.3 |

About 25 new or changed assertions. These parts go through Pebble or Compose APIs and are checked in the emulator or by a compile, not by unit tests (plan Step 15):
- watch persistence (FR3.2);
- the notice window, its timer and buttons (FR4.2);
- the glance slices (FR6.1, FR6.2);
- the dashboard layout (FR7.2, FR7.4).

## Coverage Target

Minimal strategy: at least one test per unit-testable requirement (FR2.1–FR2.3, FR3.1, FR4.1, FR4.3, FR4.4, FR5.1, FR6.1–FR6.3, FR7.1–FR7.3) and a happy path per unit (`update_notice`, `glance_text`, `ProtocolCodec`, `UpdateCheckRepository`, `UpdatePrompt`). No line-coverage floor applies to the express scope.

## Mocking and Test Data

- The C units and `UpdatePrompt` are pure: no mocks, inline literals (`"0.0.71"`, `"0.0.72"`, a 20-byte `"0.0.71-very-long-bld"`, `"0.0.{7}1\\"`, `""`, `NULL`).
- `UpdateCheckRepositoryTest` keeps its `FakeFetcher` and injected `now` clock, so the 30-day boundary is exact and nothing touches the network.
- `ProtocolCodecTest` builds `NavigationState` values directly and reads the resulting `AppMessage`.
