# Code Generation Plan — #94 Phone app update notice on the watch

## Summary

- Builds: protocol 1.2 (update flag bit 4 + `LATEST_PHONE_VERSION` key 14); the phone sends them; the watch shows a once-per-version notice, update idle text and launcher subtitle; the phone dashboard gets a stale-check item and a weekly-check question
- Touches: protocol/ + examples/, generated constants, android (ProtocolCodec, NavigationController, AppContainer, UpdateCheckRepository, DashboardScreen, MainActivity, strings), watchapp/src/c (new update_notice + update_notice_window, glance_text, main.c), watchapp/tests, requirements, spec Protocol.md, MANIFEST, docs/IMPLEMENTATION_STATUS.md
- Tests: about 25 new assertions in 4 test files: 2 host C tests (new test_update_notice.c, extended test_glance_text.c) and 3 JVM test classes (ProtocolCodecTest, UpdateCheckRepositoryTest, new UpdatePromptTest), plus emulator screenshots on basalt, chalk, emery and diorite

## Context

Zero-Unit express work, brownfield. Inputs: `inception/requirements-analysis/requirements.md` (FR1–FR8, NFR1–NFR5) and the code knowledge base at `aidlc/spaces/default/codekb/PebbleNTN/`.

Design decisions:
- **Phone side, the codec stays pure.** `ProtocolCodec.encodeState` gets one more parameter, `latestPhoneVersion: String?`. When it is non-null, the codec ORs `PHONE_UPDATE_AVAILABLE_MASK` into `FLAGS`, strips a leading `v`, and adds key 14. `NavigationController` takes a `latestPhoneUpdate: () -> String?` provider (default `{ null }`) and calls it on each send. `AppContainer` wires the provider to `updateCheckRepository.state.value`: the latest version when `updateAvailable`, otherwise null. The reducer is not changed.
- **Watch side, the pure logic goes in a new Pebble-free unit `update_notice.c`, which gets its own test twin.** It holds the update record, the show/defer decision and the idle text. `scripts/test-watchapp-unit.sh` builds each test against only its own unit, so this unit does not reuse `phone_version.c` and has its own small capped copy.
- **The subtitle text goes in `glance_text.c`**, so the escaping of `{`, `}` and `\` stays in one place.
- **The notice window is a new Pebble UI module, `update_notice_window.c`**, styled like the About screen. It has no host test, like about_window.c, and is checked in the emulator.
- **Persist keys:** 17 (`PERSIST_KEY_LATEST_PHONE_VERSION`) and 18 (`PERSIST_KEY_UPDATE_NOTICE_SHOWN`) live in main.c, next to key 16. theme.c's note on the keys in use is extended.
- **The notice opens only when the main window is on top.** If Settings or About is open, it waits for the next idle or stop message. It is marked shown when it opens.
- **Dashboard logic is a pure helper `UpdatePrompt`**, like `SetupProgress`. It decides between NONE, STALE_CHECK and WEEKLY_QUESTION and gets a JVM test, since the project has no Compose UI tests. The "check finished from the stale item" state lives in `MainActivity` (rememberSaveable) and is not persisted. It comes back only through the stale item, which takes another 30 days (FR7.3).
- **While stale, the "Check for updates now" row leaves the Updates section and shows as the top card** (FR7.2).

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

Layers that do not apply: there is no database or data-model layer. The "repository" layer is `UpdateCheckRepository`. The "API" layer is the AppMessage protocol: the definition and the codec. On the watch, persistence goes through Pebble `persist_*` in main.c and is checked in the emulator, not by host tests.

## Steps

- [x] **Step 1: Project structure and production configuration (FR1.1).**
  - `protocol/protocol-definition.json`: add `"LATEST_PHONE_VERSION": 14` to keys and `"PHONE_UPDATE_AVAILABLE": 4` to flagBits, and bump the minor version to 2.
  - `examples/protocol-definition.json`: add the same key and version.
  - Run `scripts/generate-protocol.sh` to regenerate `Protocol.kt` and `protocol.h`. Never edit them by hand.
- [x] **Step 2: Runner check (contract runner step).** Before the first test, run `scripts/test-watchapp-unit.sh` and `android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.protocol.ProtocolCodecTest' --tests 'com.pebblentn.app.update.UpdateCheckRepositoryTest'` on the regenerated tree, and record both as green (baseline). Traces: NFR5.
- [x] **Step 3: Repository: `UpdateCheckRepository` (FR7.1).**
  - Add `lastCheckMillis: StateFlow<Long>` (0 = never; updated when a check records its attempt) and `fun isCheckStale(now: Long = this.now()): Boolean` (true when never checked or `now - last > STALE_AFTER_MS`).
  - Add `STALE_AFTER_MS = 30 days`. The KDoc cites REQ-ANDROID-018 and #94.
- [x] **Step 4: Repository: write and run its tests.** Extend `UpdateCheckRepositoryTest` with these cases:
  - never checked → stale;
  - exactly 30 days → not stale;
  - 30 days + 1 ms → stale;
  - right after a forced check → not stale, and `lastCheckMillis` is updated;
  - a failed check also clears staleness.

  Run the scoped Gradle command. Traces: FR7.1, NFR2.
- [x] **Step 5: Business logic, phone: `UpdatePrompt` (FR7.2, FR7.3).** New pure `ui/dashboard/UpdatePrompt.kt`: `enum class UpdatePrompt { NONE, STALE_CHECK, WEEKLY_QUESTION }` and `fun updatePrompt(stale: Boolean, autoCheckEnabled: Boolean, staleCheckFinished: Boolean): UpdatePrompt`. The question wins only when `staleCheckFinished && !autoCheckEnabled`; otherwise STALE_CHECK when stale, else NONE.
- [x] **Step 6: Business logic, phone: write and run `UpdatePromptTest`** (stale → STALE_CHECK; check finished with weekly off → WEEKLY_QUESTION; weekly on → never WEEKLY_QUESTION; not stale and nothing pending → NONE). Traces: FR7.2, FR7.3.
- [x] **Step 7: Business logic, watch: `update_notice` (FR3.1, FR4.1, FR4.3, FR4.4, FR5.1).** New Pebble-free `watchapp/src/c/update_notice.h/.c`:
  - `#define UPDATE_VERSION_MAX 16`, `#define UPDATE_NOTICE_TEXT "A phone app update is available. Open the mobile app to find the update."`, `#define UPDATE_IDLE_TEXT "No navigation detected. Phone app update available: open the mobile app."`, `#define IDLE_TEXT "No navigation detected. If navigating, open PebbleNTN on your phone."`;
  - `bool update_record(char *stored, size_t len, bool flag_set, const char *incoming)`: when the flag is clear, empty `stored`; when the flag is set and `incoming` is non-empty, store a copy capped at 16 bytes and always NUL-terminated; when the flag is set and `incoming` is NULL or empty, leave `stored` unchanged. Returns true only when `stored` changed;
  - `UpdateNoticeAction update_notice_action(const char *latest, const char *shown, bool navigating, bool main_on_top)`: NONE when `latest` is empty or equals `shown`; DEFER while `navigating` or when the main window is not on top; otherwise SHOW;
  - `const char *update_idle_text(const char *latest)`: the update text when `latest` is non-empty, else the usual text;
  - the header cites REQ-WATCH-023 and #94.
- [x] **Step 8: Business logic, watch: write and run `tests/test_update_notice.c`.** Cases:
  - record first version (true);
  - same again (false);
  - flag clear clears (true), then clear again (false);
  - flag set with missing or empty version leaves it unchanged (false);
  - a 20-byte version is cut to 16 bytes;
  - action SHOW for a new version, NONE once shown, SHOW again for a newer version;
  - DEFER while navigating and while the main window is not on top;
  - NONE with no update;
  - idle text for both states.

  Run `scripts/test-watchapp-unit.sh`. Traces: FR3.1, FR4.1, FR4.3, FR4.4, FR5.1, NFR3.
- [x] **Step 9: API: codec and controller (FR2.1, FR2.2, FR2.3).**
  - `ProtocolCodec.encodeState(state, flags, appVersion, latestPhoneVersion: String? = null)`: when the version is non-null and not blank, OR in `Protocol.Flags.PHONE_UPDATE_AVAILABLE_MASK` (generated name, confirmed after Step 1) for all three states and put `LATEST_PHONE_VERSION` with the leading `v` removed. `encodeCompatibilityError` is unchanged.
  - `NavigationController`: a new constructor parameter `latestPhoneUpdate: () -> String? = { null }`, passed through on every `SendState`.
  - `AppContainer`: `latestPhoneUpdate = { updateCheckRepository.state.value.let { if (it.updateAvailable) it.latestVersion else null } }`.
- [x] **Step 10: API: write and run the extended `ProtocolCodecTest`.** Cases:
  - update `v0.0.71` on NO_ACTIVE_NAVIGATION, NAVIGATION_UPDATE and NAVIGATION_STOPPED sets bit 4 with key 14 = `0.0.71`, and existing bits are kept;
  - no update means bit 4 is clear and key 14 is absent;
  - the compatibility error has no key 14.

  Run the scoped Gradle command. Traces: FR2.1, FR2.2, FR2.3, NFR1.
- [x] **Step 11: Frontend, watch: launcher subtitle (FR6.1–FR6.3).**
  - `glance_text.h/.c`: add `bool glance_update_subtitle(const char *version, char *buf, size_t len)`, which writes `Update phone app to <version>` with `{`, `}` and `\` removed and returns false when the version is empty or the text doesn't fit.
  - `main.c` `glance_reload_callback`: while a countdown is active, add the countdown slice with its expiry, then a fallback slice (update text when recorded, else `GLANCE_HINT`). With no countdown, add only that fallback slice.
- [x] **Step 12: Frontend, watch: write and run the extended `tests/test_glance_text.c`** (update text for `0.0.71`, braces and backslash dropped, empty version → false, small buffer → false and empty, and the existing countdown cases unchanged). Run `scripts/test-watchapp-unit.sh`. Traces: FR6.1, FR6.2, FR6.3.
- [x] **Step 13: Frontend, watch: the notice and wiring (FR3.2, FR4.1–FR4.4, FR5.1).**
  - New `update_notice_window.h/.c`:
    - `update_notice_window_push(void (*on_close)(void))` shows `UPDATE_NOTICE_TEXT` in a centred TextLayer, inset for chalk's round screen, with About's colours and theme;
    - a 10 s `app_timer` closes it;
    - a click config that sends UP, SELECT, DOWN and BACK to close only;
    - no vibration and no light.
  - `main.c`:
    - add keys 17 and 18, `s_latest_phone_version` and `s_update_notice_shown`, loaded at init;
    - `record_update(iter)` runs only when the message carries `FLAGS`, and writes key 17 on change;
    - for `NO_ACTIVE_NAVIGATION`, show `update_idle_text(s_latest_phone_version)`;
    - after `NO_ACTIVE_NAVIGATION`, and after `NAVIGATION_STOPPED` when the app does not exit to the watchface, call `maybe_show_update_notice()`;
    - when it returns SHOW, push the notice, copy the version into `s_update_notice_shown` and persist key 18 (log a failed write with `APP_LOG`).
  - `theme.c`: extend the persist-key note to 16–18.
- [x] **Step 14: Frontend, phone: dashboard (FR7.2–FR7.4).**
  - `DashboardScreen`:
    - new parameters `updatePrompt: UpdatePrompt`, `onStaleCheck`, `onEnableWeeklyChecks` and `onDismissWeeklyQuestion`;
    - for STALE_CHECK, a PromptCard is the first item in the column, above the setup checklist;
    - for WEEKLY_QUESTION, a card with **Turn on** and **Not now**, which adds a second action to `PromptCard` or a small two-button variant;
    - while STALE_CHECK, the "Check for updates now" row is hidden from the Updates section.
  - `MainActivity`:
    - collect `lastCheckMillis`;
    - `staleCheckFinished` is set true after a check started from the stale card finishes, whatever the outcome;
    - **Turn on** calls `setAutoCheckEnabled(true)` and clears the flag, and **Not now** clears it.
  - `strings.xml`: the new strings, with no hard-coded text.
- [x] **Step 15: Frontend: build and check in the emulator (NFR4).**
  - Build the watchapp. On basalt, chalk, emery and diorite, simulate messages with `pebble send-app-message` (flags bit 4 + key 14) and check:
    - the notice appears, closes after 10 s and closes on a button;
    - after a relaunch there is no second notice;
    - a new version brings the notice back;
    - the notice waits during a route and appears on stop;
    - the update idle text shows and fits the bezel;
    - the launcher subtitle reads `Update phone app to 0.0.71` on basalt, chalk and emery.
  - Save screenshots and close the emulator after use.
  - Phone: build the debug APK. The dashboard is not checked in an Android emulator in WSL (it crashed WSL before); the Compose change is covered by the pure `UpdatePrompt` tests and a compile, and an on-device check is left as a follow-up.
- [x] **Step 16: Specification (FR8.1).**
  - `requirements/REQ-WATCH.md`: add REQ-WATCH-023, amend REQ-WATCH-019 and drop the "no new protocol key" note from REQ-WATCH-020.
  - `requirements/REQ-ANDROID.md`: add REQ-ANDROID-017 and REQ-ANDROID-018.
  - `spec/200-architecture/Protocol.md`: 1.2, bit 4, key 14.
  - Resync `MANIFEST.sha256.json` and run `scripts/validate-spec-assets.sh`.
- [x] **Step 17: Documentation and traceability.** Add a #94 entry to `docs/IMPLEMENTATION_STATUS.md` (milestone, requirements, commands run, blockers, next task). Then run the full `scripts/test-all.sh`: protocol consistency, generated constants, version sync, watch units, Android `test lint` (NFR5).
- [x] **Step 18: Loop-back 1: a shorter launcher subtitle (FR6.1, NFR4).** Build and Test found `Update phone app to {version}` cut off after about 16 characters in the launcher. The person chose `Update: {version}` instead (2026-10-22). They asked that opening the watchapp still gives a clear message about the phone app update; the notice (FR4) and the update idle text (FR5) already do this and stay unchanged.
  - `watchapp/src/c/glance_text.c` and `.h`: the template becomes `Update: %s`, and the comments are updated.
  - `watchapp/tests/test_glance_text.c`: expect `Update: 0.0.71`; keep the template-character and small-buffer cases.
  - `requirements/REQ-WATCH.md` REQ-WATCH-020 and `requirements.md` FR6: the subtitle text becomes `Update: {version}`. `code-summary.md` is updated to match.
  - Run the scoped C tests and `scripts/test-all.sh`. Take new launcher screenshots on basalt, chalk, emery and diorite (`emulator-<platform>-7-launcher-subtitle.png`, replacing the old ones), check that the full version is readable, and close the emulator.

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1.1 protocol 1.2, bit 4, key 14 | 1, 16, 17 |
| FR2.1 bit 4 on every state message | 9, 10 |
| FR2.2 key 14 without leading `v` | 9, 10 |
| FR2.3 compatibility error unchanged | 9, 10 |
| FR3.1 record or clear latest version | 7, 8, 13 |
| FR3.2 persist keys 17, 18 | 13, 15 |
| FR4.1 notice text, no vibration | 7, 13, 15 |
| FR4.2 10 s or any button | 13, 15 |
| FR4.3 once per version | 7, 8, 13, 15 |
| FR4.4 defer during a route | 7, 8, 13, 15 |
| FR5.1 idle text | 7, 8, 13, 15 |
| FR6.1–FR6.3 launcher subtitle | 11, 12, 15, 18 |
| FR7.1 lastCheckMillis / isCheckStale | 3, 4 |
| FR7.2 stale item first | 5, 6, 14 |
| FR7.3 weekly question | 5, 6, 14 |
| FR7.4 string resources | 14 |
| FR8.1 spec and manifest | 16 |
| NFR1 compatibility | 9, 10, 17 |
| NFR2 no new network access | 4, 17 |
| NFR3 flash wear | 8, 13 |
| NFR4 layout on 4 platforms | 15, 18 |
| NFR5 tests green | 2, 4, 6, 8, 10, 12, 17 |
