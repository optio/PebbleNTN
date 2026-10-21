# Code Summary — #103 About screen: last connected phone app version

## Files Created / Modified

| File | Change |
|---|---|
| watchapp/src/c/phone_version.h, phone_version.c | New Pebble-free unit: `PHONE_VERSION_MAX 16`, `phone_version_update()` (cap at 16 bytes, NULL/empty = no change, true only when the text changed) |
| watchapp/src/c/main.c | `PERSIST_KEY_PHONE_VERSION 16`, `s_phone_version`; `load_phone_version()` at init; `record_phone_version()` on every inbound message, persisting only on change, with `APP_LOG` warnings on read/write failure; passes `.phone_version` to `AboutInfo` |
| watchapp/src/c/about_text.h, about_text.c | `ABOUT_DETAILS_LEN 128`; `about_details()` takes `phone_version` and writes `Watchapp …` / `Phone app … (last seen)` or `Phone app: never connected` / `Protocol …` / status |
| watchapp/src/c/about_window.h, about_window.c | `AboutInfo.phone_version`; `s_details[ABOUT_DETAILS_LEN]`; formatted once in `about_load` (snapshot on open) |
| watchapp/src/c/theme.c | Comment: persist key 16 is taken, next setting uses 17 |
| watchapp/tests/test_phone_version.c | New, 10 cases |
| watchapp/tests/test_about_text.c | Existing details cases moved to the new format; never connected, last seen out of range, worst case fits, truncation, 1-byte buffer |
| requirements/REQ-WATCH.md | REQ-WATCH-020 amended with acceptance criteria |
| MANIFEST.sha256.json | REQ-WATCH.md hash resynced |
| docs/IMPLEMENTATION_STATUS.md | #103 entry |

No file under `android/` or `protocol/` changed (NFR1).

## Key Implementation Decisions

- The capped value is compared, so a long version seen again causes no further flash write (FR1.3, NFR2).
- Key 11 is recorded from any event before the event switch; only a non-empty `TUPLE_CSTRING` counts (FR1.1).
- Persist failures are logged and not fatal: the version stays in memory for this session (construction error-handling rule).
- The About row subtitle was verified unchanged (`s_about.app_version`, FR2.3).

## Test Coverage

- Host: `scripts/test-watchapp-unit.sh` green (`All phone_version assertions passed.`, `All about_text assertions passed.`).
- Full suite: `scripts/test-all.sh` exit 0 (spec assets, protocol consistency, version, watch units, catalog/rules, 185/21/12/195 rule & regression tests, route-capture, Android `BUILD SUCCESSFUL`).
- Watch build: `scripts/build-watchapp.sh` succeeds for emery, diorite, chalk, basalt, aplite.
- Emulator (key 11 sent with `pebble send-app-message`): on basalt, chalk, emery and diorite the screen shows never connected and last seen, scrolls to the link, keeps the version after a relaunch with no phone, and stays inside the bezel on chalk. Basalt only: no refresh while open, the new version shows after reopening, and the row subtitle is `0.0.68`. Screenshots `emulator-*.png` in this folder.

## Tests Written but Not Executed

- None of the written tests were left unexecuted.
- Not verified: a real phone and watch end to end (key 11 came from the emulator's message tool, not the Android app). This is left for an on-wrist check.

## Deviations from the Plan

- The `test-all.sh` output was sent to a gitignored log file and read from there (since deleted), not read straight from the command result.
- Two scratch screenshots (`emulator-basalt-check.png`, `emulator-diorite-check.png`) are left in this folder.
- The published `screenshots/settings-about-*.png` still show the old `Version …` layout. They were not regenerated because the capture script rewrites the whole published set.
- On 144-px-wide screens and chalk, the two `Phone app …` lines wrap at a word boundary. Nothing is cut.
