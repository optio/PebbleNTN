# Unit Test Instructions — #103 About screen: last connected phone app version

## Framework and Setup

- Host-compiled C unit tests, no framework: each `watchapp/tests/test_<unit>.c` has a `main` with a small `check()` helper and returns non-zero on failure.
- `scripts/test-watchapp-unit.sh` compiles each test against only its matching unit (`watchapp/src/c/<unit>.c`) with `gcc -std=c11 -Wall -Wextra -Werror`, and fails if a test has no matching unit. No Pebble SDK or emulator is needed.
- Prerequisite: `gcc` (or `CC=<compiler>`) on PATH.

## How to Run This Work's Tests

Scoped to the two units this work touches (exact files, not the whole suite):

```bash
gcc -std=c11 -Wall -Wextra -Werror -o /tmp/test_phone_version watchapp/tests/test_phone_version.c watchapp/src/c/phone_version.c && /tmp/test_phone_version
gcc -std=c11 -Wall -Wextra -Werror -o /tmp/test_about_text watchapp/tests/test_about_text.c watchapp/src/c/about_text.c && /tmp/test_about_text
```

Run both from the repo root. The runner check before the first test (plan Step 1) runs `scripts/test-watchapp-unit.sh` once on the unchanged code as a baseline. Build and Test runs the full `scripts/test-all.sh`.

## Tests

| Test file | Cases | Requirements |
|---|---|---|
| watchapp/tests/test_phone_version.c (new) | first version recorded; same version unchanged (no write); new version recorded; NULL and empty incoming unchanged; 20-byte version cut to 16 bytes and NUL-terminated; truncated value seen again unchanged | FR1.1, FR1.3, FR1.4, NFR2 |
| watchapp/tests/test_about_text.c (extended) | existing phone-status cases kept; details with a phone version (`Watchapp …` / `Phone app … (last seen)` / `Protocol …` / status); never connected for NULL and empty; worst case (16 + 16 bytes, longest status) fits `ABOUT_DETAILS_LEN` untruncated; small buffer truncated but NUL-terminated | FR2.1, FR2.4, NFR4 |

About 12 new or changed assertions. Persistence (FR1.2), the snapshot on open (FR2.2) and the subtitle (FR2.3) go through Pebble APIs and are checked in the emulator (plan Step 7), not by host tests.

## Coverage Target

Minimal strategy: at least one test per host-testable requirement (FR1.1, FR1.3, FR1.4, FR2.1, FR2.4) and a happy path per unit (`phone_version`, `about_text`). No line-coverage floor applies to the express scope.

## Mocking and Test Data

No mocks: both units are pure. The test data is inline string literals (`"0.0.67"`, `"0.0.70"`, a 20-byte `"0.0.67-very-long-bld"`, `""`, `NULL`).
