# Code Generation Plan — #103 About screen: last connected phone app version

## Summary

- Builds: the watch records the phone app version from key 11, persists it (key 16), and About shows `Watchapp …` / `Phone app … (last seen)` or `Phone app: never connected`
- Touches: watchapp/src/c (new phone_version.c/.h, about_text.c/.h, about_window.c/.h, main.c, theme.c comment), watchapp/tests (new test_phone_version.c, test_about_text.c), requirements/REQ-WATCH.md, MANIFEST.sha256.json, docs/IMPLEMENTATION_STATUS.md
- Tests: 2 host C unit test files (new phone_version tests, extended about_text tests, about 12 assertions), plus emulator screenshots on basalt, chalk, emery and diorite

## Context

Zero-Unit express work, brownfield. Inputs: `inception/requirements-analysis/requirements.md` (FR1–FR3, NFR1–NFR4) and the code knowledge base at `aidlc/spaces/default/codekb/PebbleNTN/`. Watch-side only: no file under `android/` or `protocol/` and no generated constant changes (NFR1).

Design decisions:
- The version-recording rule (cap, empty, "changed?") is pure logic, so it goes into a new Pebble-free unit `phone_version.c` with a `tests/test_phone_version.c` twin. `scripts/test-watchapp-unit.sh` compiles each test against only its own unit, which is why it is not folded into main.c.
- `PHONE_VERSION_MAX = 16` bytes of text (FR1.4); `ABOUT_DETAILS_LEN = 128` covers the worst case of about 105 bytes (FR2.4).
- The persist key `PERSIST_KEY_PHONE_VERSION 16` lives in main.c, which owns the inbox; theme.c gets a one-line note that 16 is taken, so the next setting does not reuse it.
- About keeps taking its text when it opens (FR2.2): `AboutInfo` carries a pointer to main.c's stored version, and `about_load` formats it once.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "express",
  "test_strategy": "minimal",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "minimal",
    "strategy_volume": [
      "One verifiable test per requirement at the narrowest effective level.",
      "At least one happy-path unit test per component.",
      "Unit tests are the default; a bugfix/security scope floor may require an integration or E2E regression when that is the narrowest level that reproduces the defect."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:c8f84e35190e007d9863fad236154d0269ab9bf633fc2a3ff106ed965ba4a9b9",
  "contract_sha256": "sha256:bb76c1b22f8307be6e5780d50cabab891684449d5b164bd0e825e9e4e5c61e5b"
}
```

Layers that do not apply: there is no database, repository or API/endpoint layer in this watch-only change. The phone version store is persisted through Pebble `persist_*` in the shell (main.c), which is checked in the emulator, not by host tests.

## Steps

- [x] **Step 1 — Runner check (contract runner step).** Verify the host runner before the first test: run `scripts/test-watchapp-unit.sh` and record that it is green on the unchanged code (baseline). Traces: NFR4.
- [x] **Step 2 — Business logic: implement `phone_version` (FR1.1, FR1.3, FR1.4).** New `watchapp/src/c/phone_version.h/.c`, Pebble-free:
  - `#define PHONE_VERSION_MAX 16` (bytes of text, NUL excluded);
  - `bool phone_version_update(char *stored, size_t stored_len, const char *incoming)`: NULL or empty `incoming` leaves `stored` unchanged and returns false. Otherwise it copies at most `min(PHONE_VERSION_MAX, stored_len - 1)` bytes, always NUL-terminates, and returns true only when the stored text changed (so the caller persists only then).
  - Header comment cites REQ-WATCH-020 and #103.
- [x] **Step 3 — Business logic: write and run `tests/test_phone_version.c`.** Cases: first version recorded (true), same version again (false), new version (true), NULL and empty incoming (false, unchanged), 20-byte version truncated to 16 bytes and NUL-terminated, a truncated value seen again is unchanged (false). Run `scripts/test-watchapp-unit.sh`. Traces: FR1.1, FR1.3, FR1.4, NFR2.
- [x] **Step 4 — Business logic: About text (FR2.1, FR2.4).** In `about_text.h/.c`:
  - add `#define ABOUT_DETAILS_LEN 128` with a comment on the worst case;
  - change `about_details` to take `phone_version` after `app_version` and produce `Watchapp <v>\nPhone app <pv> (last seen)\nProtocol <M>.<m>\n<status>`, or `Phone app: never connected` when `phone_version` is NULL or empty;
  - keep `about_phone_status` unchanged; update the header comment.
- [x] **Step 5 — Business logic: write and run the extended `tests/test_about_text.c`.**
  - Update the existing details cases to the new format.
  - Add: never connected (NULL and empty), the full worst case (16-byte watchapp version, 16-byte phone version, `Pebble app not connected`) fits `ABOUT_DETAILS_LEN` untruncated, small-buffer truncation still NUL-terminated.
  - Run `scripts/test-watchapp-unit.sh`.

  Traces: FR2.1, FR2.4, NFR4.
- [x] **Step 6 — Frontend behavior: wire the watchapp (FR1.1, FR1.2, FR1.3, FR2.2, FR2.3).**
  - `main.c`: `#define PERSIST_KEY_PHONE_VERSION 16` (comment: keys 1–15 belong to theme.c). Add `static char s_phone_version[PHONE_VERSION_MAX + 1]` and load it at init with `persist_read_string` when `persist_exists`.
  - `main.c` `inbox_received_handler`: for any message, when `dict_find(iter, PBNTN_KEY_APP_VERSION)` is a `TUPLE_CSTRING` and `phone_version_update` returns true, call `persist_write_string`. A negative result is logged with `APP_LOG` as a warning (no silent failure; the in-memory value still shows).
  - `about_window.h`: add `const char *phone_version` to `AboutInfo`.
  - `main.c` `select_click_handler`: pass `s_phone_version`.
  - `about_window.c`: size `s_details` with `ABOUT_DETAILS_LEN` and pass `s_info.phone_version` to `about_details`. It still formats once in `about_load`, so the snapshot is taken when About opens.
  - `settings_window.c`: the About row subtitle stays `s_about.app_version` (verify, no change).
  - `theme.c`: one comment line that persist key 16 is used by main.c for the phone version.
- [x] **Step 7 — Frontend behavior: build and check in the emulator (NFR3).** Build the watchapp with the Pebble SDK.
  - On basalt, chalk, emery and diorite, open Settings → About: a fresh install shows `Phone app: never connected`.
  - Then with the phone (or the emulator's simulated message) connected, it shows `Phone app <version> (last seen)`. After an app relaunch with no phone, the version is still shown.
  - The screen scrolls to the link and stays inside the bezel on chalk.
  - Save screenshots. Close the emulator afterwards.
- [x] **Step 8 — Specification (FR3.1).** Amend REQ-WATCH-020 in `requirements/REQ-WATCH.md`:
  - list `Watchapp <version>`, the last connected phone app version shown as `Phone app <version> (last seen)` (persisted, kept after relaunch and out of range), and `Phone app: never connected`, with no protocol change;
  - acceptance: host tests of the recording rule and the new details lines, plus the emulator check above.

  Resync that file's hash in `MANIFEST.sha256.json` (sha256sum) and run `scripts/validate-spec-assets.sh`.
- [x] **Step 9 — Documentation and traceability.** Update `docs/IMPLEMENTATION_STATUS.md` with a #103 entry (milestone, requirements, commands run, blockers, next task, as CLAUDE.md requires). Run the full `scripts/test-all.sh` to confirm the existing suite stays green (NFR4) and the protocol consistency check is unchanged (NFR1).

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1.1 record key 11 | 2, 3, 6 |
| FR1.2 persist key 16, load at startup | 6, 7 |
| FR1.3 write only when changed | 2, 3, 6 |
| FR1.4 16-byte cap, empty = none | 2, 3 |
| FR2.1 line wording | 4, 5, 7 |
| FR2.2 snapshot on open | 6, 7 |
| FR2.3 About row subtitle unchanged | 6, 7 |
| FR2.4 buffer fits worst case | 4, 5 |
| FR3.1 REQ-WATCH-020 + manifest | 8 |
| NFR1 no protocol change | 6, 9 |
| NFR2 flash wear | 3 |
| NFR3 four platforms | 7 |
| NFR4 tests green | 1, 3, 5, 9 |
