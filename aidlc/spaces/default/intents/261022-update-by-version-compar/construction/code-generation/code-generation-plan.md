# Code Generation Plan — Version comparison as the default update check

## Summary

- Builds: a manual-check result message that combines GitHub and the watchapp version; shorter texts that present the offline check as the default; corrected Core Devices notes in code, requirements and specs
- Touches: `update/UpdateCheckMessage.kt` (new pure helper), `ui/MainActivity.kt`, `update/UpdateCheckRepository.kt` (KDoc), `di/AppContainer.kt` (KDoc), `pebble/NavigationController.kt` and `pebble/WatchListenerService.kt` (KDoc), `AndroidManifest.xml` (comment), `strings.xml`, `NavigationControllerTest.kt` (comments), `requirements/REQ-ANDROID.md`, `spec/200-architecture/Pebble.md`, `docs/IMPLEMENTATION_STATUS.md`
- Tests: about 5 JVM unit tests in a new `UpdateCheckMessageTest`

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR3, NFR1–NFR4). The code knowledge base is at `aidlc/spaces/default/codekb/PebbleNTN/`.

Design decisions:
- **Pure helper:** `update/UpdateCheckMessage.kt`:
  - `sealed interface UpdateCheckMessage { object UpToDate; object Failed; data class Available(val version: String) }`;
  - `fun updateCheckMessage(outcome: UpdateCheckOutcome, state: UpdateState): UpdateCheckMessage`. It returns `Available(effectiveLatest without a leading v/V)` whenever `state.updateAvailable`, whatever the outcome (FR1.1, FR1.2). Otherwise `Failed` for `FAILED`, else `UpToDate`.
  - `SKIPPED_NOT_DUE` never reaches the manual path (it is forced), but it maps like `UP_TO_DATE`.
- **MainActivity:** the manual check reads the outcome and then `updateCheckRepository.state.value`, maps them through the helper, and shows `update_check_available_version` ("PebbleNTN %1$s is available."), `update_check_up_to_date` or `update_check_failed`. The old `update_check_available` string goes if nothing else uses it.
- **Strings (FR2.1, FR2.2):** replace the network sentence in `onboarding_disclosure`, and replace `dashboard_auto_update_hint`, with the approved shorter texts. The stale item and weekly question strings are unchanged (FR2.3).
- **Corrections (FR3):** comments, KDoc, REQ-ANDROID-013/018/019, `Pebble.md` and the status doc only. No behaviour change: the made-up readiness stays as a fallback.

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

## Steps

There is no data model, repository logic, API or protocol change (NFR1). Those layers are left out apart from comments.

- [x] **Step 1: Project structure.** Create the empty `android/app/src/main/java/com/pebblentn/app/update/UpdateCheckMessage.kt`.
- [x] **Step 2: Verify the test runner.** Run the scoped Gradle command from unit-test-instructions.md with the existing `UpdateCheckRepositoryTest`, and confirm it passes.
- [x] **Step 3: Business logic: implement (FR1.1, FR1.2).** Add `UpdateCheckMessage` and `updateCheckMessage(outcome, state)` as described in Context, with a short KDoc.
- [x] **Step 4: Business logic: tests (FR1.1–FR1.3).** `UpdateCheckMessageTest`:
  - GitHub up to date, watch `0.0.75` and phone `0.0.74` → Available("0.0.75");
  - both up to date → UpToDate;
  - FAILED with no watch update → Failed;
  - FAILED with a watch update → Available;
  - GitHub `v0.0.76` → Available("0.0.76").
- [x] **Step 5: Frontend: implement (FR1, FR2.1, FR2.2, NFR4).**
  - `MainActivity`'s manual check uses the helper and shows the new `update_check_available_version`.
  - In `strings.xml`: add `update_check_available_version`, replace the `onboarding_disclosure` network sentence and `dashboard_auto_update_hint` with the approved texts, and remove `update_check_available` if nothing uses it any more.
- [x] **Step 6: Comments and KDoc (FR2.4, FR3.1).**
  - Describe the GitHub check as optional and the version comparison as the default in: the `UpdateCheckRepository` and `AppContainer` KDoc, and the INTERNET comment in `AndroidManifest.xml`.
  - Correct the Core Devices statements in: the `NavigationController.scheduleAutonomousReady` KDoc, the `WatchListenerService.onAppOpened` KDoc, the `UpdateCheckRepository.recordWatchAppVersion` KDoc, and the `NavigationControllerTest` comments. The new wording: forwarding was confirmed in v0.0.74 with `companionApp` declared, and the made-up readiness stays as a fallback.
- [x] **Step 7: Build.** Run `:app:assembleDebug :app:lintDebug`.
- [x] **Step 8: Specification (FR3.2, FR3.3).**
  - `requirements/REQ-ANDROID.md`: REQ-ANDROID-013 and -018 say GitHub is an optional extra; REQ-ANDROID-019 drops the "best effort / not with the Core Devices app" limitation and describes the combined check message.
  - `spec/200-architecture/Pebble.md`: correct §"Companion limitation and synthesized readiness".
  - Resync `MANIFEST.sha256.json` for any listed file, then run `scripts/validate-spec-assets.sh`.
- [x] **Step 9: Documentation and traceability.** In `docs/IMPLEMENTATION_STATUS.md`, add a new entry and correct the open Core Devices note. Then run the full `scripts/test-all.sh` (NFR3).

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1.1–FR1.3 combined message | 3, 4, 5 |
| FR2.1–FR2.2 texts | 5 |
| FR2.3 stale item and question unchanged | (no code) |
| FR2.4 KDoc and manifest comment | 6 |
| FR3.1 code comments corrected | 6 |
| FR3.2–FR3.3 specs | 8, 9 |
| NFR1 no behaviour change beyond FR1 | 5, 6, 9 |
| NFR2 no new network access | 5 |
| NFR3 tests | 2, 4, 9 |
| NFR4 string resources | 5 |
