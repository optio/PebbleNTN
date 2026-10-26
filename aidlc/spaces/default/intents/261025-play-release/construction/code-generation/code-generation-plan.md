# Code Generation Plan — Prepare the first Google Play release (#114)

## Summary

- **Builds:**
  - target SDK 36 with a target-SDK gate that really fails;
  - Google Play as an install source, with no GitHub update checks or links on Play installs;
  - accurate privacy and Data safety docs;
  - a store-graphics script and its images;
  - a separate Play upload key in the Play workflow;
  - one install page (README) with the watch QR code pointing to it;
  - `docs/PLAY_CONSOLE.md`;
  - specs.
- **Touches:**
  - `android/app/build.gradle.kts` and the Android packages `update/`, `ui/`, `di/`, plus `res/values/strings.xml`, the manifest comment and their tests;
  - `.github/workflows/play-release.yml` and `release.yml`;
  - `docs/`, `fastlane/metadata/android/en-US/`, the new `tools/play_store_graphics.py`, and `README.md`;
  - `watchapp/tools/gen_connect_qr.py`, `watchapp/resources/…/connect_qr.png` and `about_window.c`;
  - `requirements/`, `spec/600-security-release/SecurityPrivacy.md` and `MANIFEST.sha256.json`.
- **Tests:** JVM tests, a graphics size check, `pebble build` with the aplite heap, an emulator render of About and the connection hint, the full `scripts/test-all.sh`, and the maintainer's Android 16 phone over wireless adb.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR8, NFRs). The facts list is in `inception/reverse-engineering/developer-scan-PebbleNTN.md`. Nothing here is released: commits are made at Deployment, all with `[skip ci]`, and `versionCode` / `versionName` stay 77 / 0.0.77.

### Design decisions

**Install source (FR2):**
- **Detection:** `InstallSource.PLAY`. `InstallSources.fromInstaller("com.android.vending")` returns PLAY.
- **Policy:** a pure policy object, `UpdatePolicy` (or functions next to `UpdateCheckRepository`), answers per source:
  - `allowsGithubCheck`, which is false for PLAY;
  - `ignoresStoredGithubVersion`, which is true for PLAY;
  - `offersOtherSources`, which is false for PLAY;
  - `showsInstallDelayNote`, which is true for FDROID and PLAY.
- **Where the policy applies:**
  - `UpdateCheckRepository.effectiveLatest` / `UpdateState` take the source and ignore the stored GitHub tag for PLAY. The watchapp-newer comparison stays.
  - `AppContainer` skips the weekly check on start for PLAY.
  - `MainActivity.checkForUpdate` and `setAutoCheckUpdates` are unreachable for PLAY, because the UI hides them, and they also guard on the policy.
- **The PLAY card and links:**
  - **Dashboard:** for PLAY there is no stale-check prompt, no weekly question, no "Check for updates now" row and no weekly toggle. The Updates section shows only the version rows.
  - **Update card:** "Update from Google Play", the Play delay line, no "Other sources", and no "uninstall first" sentence.
  - **`openUpdateSource(PLAY)`** opens `market://details?id=<packageName>`; on `ActivityNotFoundException` it falls back to the https Play URL.
  - **`DashboardScreen.sourceName(PLAY)`** = "Google Play".
- **Onboarding (FR2d):** `onboarding_disclosure_play` without the GitHub sentence, chosen in `OnboardingScreen` from the install source passed in.

**Target SDK (FR1):**
- Set `targetSdk = 36` and `playMinTargetSdk = 36`, and update the comment.
- **`play-release.yml`:** run `./gradlew verifyReleaseTargetSdk` with no `|| echo`.
- **`release.yml`:** add the same step before the build.
- Robolectric stays at sdk 34.

**Upload key (FR5):**
- `play-release.yml` decodes `PLAY_UPLOAD_KEYSTORE_BASE64` and maps `PLAY_UPLOAD_KEYSTORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS` and `PLAY_UPLOAD_KEY_PASSWORD` onto the existing `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` variables that Gradle reads.
- It fails with a clear message, naming the four secrets, when any are empty.

**Docs (FR3, FR7):**
- `PRIVACY_POLICY.md`: rewritten from the template.
  - Effective date 2026-10-26.
  - Contact: `pebblentn@mg.mijnesite.be` and `https://github.com/optio/PebbleNTN/issues`.
  - Sections: on-device processing; Network (the GitHub check on non-Play installs only, what it sends, the IP address, opt-in or manual, none on Play installs); links the user opens; email drafts and diagnostic sharing; no analytics; data retention and deletion; children; changes.
- `PLAY_DATA_SAFETY.md`: rewritten to match the policy.
- `PRIVACY_REVIEW.md`: fix the outdated line.
- Manifest INTERNET comment: updated.
- `full_description.txt`: drop "More apps can be added without a new release" and reword the Internet line for both channels.
- `PLAY_CONSOLE.md`: new, structured as FR7.
- `RELEASE_CHECKLIST.md` "Store submission": points to it.

**Graphics (FR4):**
- `tools/play_store_graphics.py` (Pillow, deterministic) produces:
  - **Phone screenshots `1.png`–`3.png`:** cropped from 1080×2280 to 1080×2160 by trimming 120 px from the top (the status bar), after checking that nothing important sits there.
  - **`4.png` and `5.png`:** 1080×2160 canvases. Each holds watch screenshots (navigation screen, settings or About, at least one colour and one round) scaled ×4 with nearest-neighbour, with a caption in a bundled or system sans font. If no font is available, the caption is left out with a warning.
  - **`featureGraphic.png`:** 1024×500, built from the icon, "PebbleNTN", the short description and a ×2 watch screenshot.
- `--check` validates every image in `images/` against Play's rules: icon 512×512; feature graphic 1024×500; screenshots with sides between 320 and 3840 px and a ratio of at most 2:1. `scripts/test-all.sh` runs `--check` when Pillow is importable, and skips with a note otherwise.

**Install page and QR (FR6):**
- **README "Distribution":**
  - Google Play ("Coming soon on Google Play");
  - GitHub releases;
  - F-Droid;
  - Obtainium;
  - the note that one signature lets users switch sources without reinstalling;
  - the watchapp from the Pebble store.
- **QR:** `QR_URL = "https://github.com/optio/PebbleNTN#distribution"` (46 bytes), version 3-L, regenerated.
- **Link text:** the About text under the QR becomes "github.com/optio/\nPebbleNTN". Check the connection-hint text too.

**Specs (FR8):**
- **Amend:**
  - REQ-ANDROID-013 (network statement);
  - REQ-ANDROID-018 and REQ-ANDROID-020 (sources, the PLAY behaviour, the acceptance tests);
  - REQ-WATCH-017 and REQ-WATCH-020 (the QR URL and the link text);
  - `SecurityPrivacy.md` (the disclosure and its Play variant).
- **Add:** REQ-ANDROID-022, Google Play distribution.
- **Then:** resync the MANIFEST and add the status entry.

## Steps

- [ ] 1. Add an entry to `docs/IMPLEMENTATION_STATUS.md` (milestone, requirements, commands, blockers, next task), as CLAUDE.md requires before coding.
- [ ] 2. Baseline: run `android/gradlew -p android :app:testDebugUnitTest` and record the result.
- [ ] 3. FR1: `targetSdk` 36, `playMinTargetSdk` 36, and the gate step in both workflows; run `verifyReleaseTargetSdk`, and check it fails at 35 (a temporary local edit, reverted).
- [ ] 4. FR2: `InstallSource.PLAY`, detection, the policy, `UpdateCheckRepository`, `AppContainer`, `MainActivity`, `DashboardScreen`, `UpdateCard` and `OnboardingScreen`, plus strings (`dashboard_update_source_play`, the Play delay line, `onboarding_disclosure_play`, the Play URLs as non-translatable strings).
- [ ] 5. FR2 tests: update `InstallSourceTest` (vending → PLAY) and `UpdateCardTest`; add policy tests and an `UpdateCheckRepository` test that a stored GitHub version is ignored for PLAY; add Compose/Robolectric tests that the dashboard shows no update controls for PLAY and that onboarding shows the Play disclosure.
- [ ] 6. FR5: the upload-key mapping and missing-secret check in `play-release.yml`.
- [ ] 7. FR3: the privacy policy, Data safety, privacy review, manifest comment and listing text.
- [ ] 8. FR4: `tools/play_store_graphics.py`; generate the images with the pebble tool's Python (Pillow); run `--check`; hook `--check` into `scripts/test-all.sh`.
- [ ] 9. FR6: README "Distribution"; `QR_URL`, regenerate `connect_qr.png` and decode it to verify; the About link text; `pebble build` and compare the aplite heap (3597 B); render About and the connection hint on the aplite and basalt emulators, then `pebble kill`.
- [ ] 10. FR7: `docs/PLAY_CONSOLE.md` and `RELEASE_CHECKLIST.md`.
- [ ] 11. FR8: amend and add the requirements, `SecurityPrivacy.md`, and resync the MANIFEST with `scripts/validate-spec-assets.sh`.
- [ ] 12. Run the full `scripts/test-all.sh`; build a debug APK for the phone check in Build and Test.
- [ ] 13. Write `code-summary.md` and `traceability.json`. Don't commit: Deployment commits with `[skip ci]`.

### Revision 1 (Request Changes: "Make sure the all screenshots are from recent version. Ok for fdroid to share those for play store")

- [x] 14. Recapture every store screenshot from the current build. F-Droid keeps sharing the same `fastlane/` images.
  - **Phone:** use the light `pebblentn-api34` AVD only, never the Android 17 Play AVD, and close it after use.
    - Install the current debug APK with installer `com.android.vending` (`adb install -i com.android.vending`), so the screens show Play mode.
    - Clean the status bar with demo mode.
    - Seed realistic state through `run-as` or the fixture publisher: a watch settings mirror for the Watch settings screen, and navigation notifications.
    - Capture at least 3 phone screens, for example the dashboard, Watch settings, the accent list with its preview, and Navigation apps.
    - Replace `phoneScreenshots/1.png`–`3.png` with them, cropped by the script.
  - **Watch:** recapture the navigation screens (basalt, chalk) and the settings menu from the current watchapp on the Pebble emulators, with no wipe, setting state by `SETTINGS` messages. Rebuild `4.png`, `5.png` and `featureGraphic.png` from them.
  - Run `play_store_graphics.py --check` and look at every image.

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
