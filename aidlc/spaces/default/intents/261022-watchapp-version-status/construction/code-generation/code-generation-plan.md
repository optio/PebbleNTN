# Code Generation Plan — Watchapp version in the dashboard status

## Summary

- Builds: a "Watchapp version" row under "Watch" in the dashboard Status section, showing "{version} (last seen)" or "Not received yet", and tighter spacing between the Status info rows
- Touches: `ui/dashboard/WatchAppVersionRow.kt` (new pure helper), `ui/dashboard/DashboardScreen.kt`, `ui/MainActivity.kt`, `res/values/strings.xml`, `requirements/REQ-ANDROID.md`
- Tests: 3 JVM unit tests in a new `WatchAppVersionRowTest`

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1, FR2, NFR1–NFR3). The code knowledge base is at `aidlc/spaces/default/codekb/PebbleNTN/`.

Design decisions:
- **Pure helper:** `ui/dashboard/WatchAppVersionRow.kt` holds `watchAppVersionText(version: String?, lastSeen: (String) -> String, none: String): String`. It returns `lastSeen(version)` for a non-blank version (shown as stored) and `none` otherwise, so it is JVM-testable without Compose. This follows the existing `UpdateCard`, `UpdatePrompt` and `SetupProgress` pattern.
- **Screen:** `DashboardScreen` gets a new defaulted `watchAppVersion: String? = null` parameter. The Status section adds an `InfoRow(title = "Watchapp version", value = watchAppVersionText(...))` directly after the "Watch" row.
- **Data:** `MainActivity` passes `updateState.watchAppVersion` from the `updateCheckRepository.state` it already collects, so the row updates live (FR1.4). No new storage (NFR1).
- **Strings:** `dashboard_watchapp_version` ("Watchapp version"), `dashboard_watchapp_version_seen` ("%1$s (last seen)") and `dashboard_watchapp_version_none` ("Not received yet"), placed beside the `dashboard_watch*` group.

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

There is no data model, repository or API change: `UpdateState.watchAppVersion` already exists (NFR1). Those layers are left out.

- [x] **Step 1: Project structure.** Create the empty `android/app/src/main/java/com/pebblentn/app/ui/dashboard/WatchAppVersionRow.kt`.
- [x] **Step 2: Verify the test runner.** Run the scoped Gradle command from unit-test-instructions.md with the existing `UpdateCardTest`, and confirm it passes before the new test.
- [x] **Step 3: Business logic: implement (FR1.2, FR1.3).** `watchAppVersionText(version, lastSeen, none)`: a non-blank version → `lastSeen(version)`, shown as stored; null or blank → `none`. Give it a short KDoc saying that "last seen" means the value is the last version received and is never cleared.
- [x] **Step 4: Business logic: tests (FR1.2, FR1.3).** `WatchAppVersionRowTest`:
  - `0.0.73` → "0.0.73 (last seen)";
  - null and blank → "Not received yet";
  - `0.0.72-debug` → shown as stored.
- [x] **Step 5: Frontend: implement (FR1.1, FR1.4, FR1.5).**
  - Add the three strings to `strings.xml`.
  - `DashboardScreen`: add the `watchAppVersion: String? = null` parameter and the `InfoRow` directly after the "Watch" row.
  - `MainActivity`: pass `updateState.watchAppVersion`.
  - **Tighter info rows (the person's request, 2026-10-22):** the gaps between "Watch", "Last navigation notification" and "Last sent to watch" are too big; reduce them to make room for the new row. `InfoRow` (DashboardScreen.kt) uses Material 3 `ListItem`, which has a tall minimum height and wide vertical padding. Replace it with a compact layout: a `Column` with the overline in `labelMedium` and the value in `bodyLarge`, about 16 dp horizontal and 6 dp vertical padding, and the same section colours, `dimmed` alpha and 2-line ellipsis. All `InfoRow` uses get the tighter spacing; the toggle rows and the access chip stay unchanged.
- [x] **Step 6: Frontend: build.** Run `:app:assembleDebug` and lint. The row layout is checked later on a device (no Android emulator in WSL, NFR3).
- [x] **Step 7: Specification (FR2.1).** In `requirements/REQ-ANDROID.md` REQ-ANDROID-019, add that the dashboard Status section shows the last seen watchapp version, or "Not received yet". Update the `MANIFEST.sha256.json` hash if the file is listed, then run `scripts/validate-spec-assets.sh`.
- [x] **Step 8: Documentation and traceability.** Add an entry to `docs/IMPLEMENTATION_STATUS.md`, then run the full `scripts/test-all.sh` (NFR2).

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1.1 row under "Watch" | 5, 6 |
| FR1.2 "{version} (last seen)" | 3, 4 |
| FR1.3 "Not received yet" | 3, 4 |
| FR1.4 live update | 5 |
| FR1.5 string resources | 5 |
| FR2.1 spec | 7 |
| NFR1 no new storage | 5 |
| NFR2 tests | 2, 4, 8 |
| NFR3 layout | 6 |
