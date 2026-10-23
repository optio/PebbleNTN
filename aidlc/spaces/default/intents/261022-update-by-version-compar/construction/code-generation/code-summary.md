# Code Summary — Version comparison as the default update check

All 9 plan steps are done and ticked. Android app, specs and docs only. No protocol, storage or watch change.

## Per step

| Step | What was built | Result |
|---|---|---|
| 1 | Created `update/UpdateCheckMessage.kt` (package line only) | Done |
| 2 | Ran the scoped runner check on `UpdateCheckRepositoryTest` | 17 tests, 0 failures |
| 3 | `sealed interface UpdateCheckMessage { UpToDate; Failed; Available(version) }` and the pure `updateCheckMessage(outcome, state)`. It returns `Available` whenever `state.updateAvailable`, with `effectiveLatest` trimmed and any leading `v`/`V` removed, whatever the outcome. Otherwise it returns `Failed` for `FAILED`, else `UpToDate` (so `SKIPPED_NOT_DUE` maps like `UP_TO_DATE`) | Done |
| 4 | `UpdateCheckMessageTest`: the 5 planned cases | 5 tests, 0 failures |
| 5 | `MainActivity.checkForUpdate` reads the outcome, then `updateCheckRepository.state.value`, maps them through the helper and shows the toast with `getString(...)`. Strings: new `update_check_available_version` ("PebbleNTN %1$s is available."); the approved FR2.1 sentence replaces the disclosure's network sentence; `dashboard_auto_update_hint` gets the approved FR2.2 text; the now-unused `update_check_available` is removed (nothing else referenced it) | Done |
| 6 | KDoc/comments: the `UpdateCheckRepository` class KDoc, the `AppContainer` property KDoc and the manifest INTERNET comment now call the GitHub check optional and the version comparison the default. The Core Devices statements are corrected in the `scheduleAutonomousReady` KDoc, the `WatchListenerService.onAppOpened` KDoc, the `recordWatchAppVersion` KDoc and two `NavigationControllerTest` comments: forwarding was confirmed in v0.0.74 with `companionApp` declared, and the synthesized readiness stays as a fallback | Done |
| 7 | `:app:assembleDebug :app:lintDebug` | BUILD SUCCESSFUL |
| 8 | REQ-ANDROID-013 (default offline detection, GitHub as an optional extra, manual message combined); REQ-ANDROID-018 (a lead sentence: these are the optional GitHub check's checks, and the default is REQ-ANDROID-019); REQ-ANDROID-019 (the "best effort / not with Core Devices" sentence is replaced; a paragraph for the combined manual message; the acceptance gets the message-choice tests). `Pebble.md` §"Companion limitation and synthesized readiness" rewritten (the heading is kept). `MANIFEST.sha256.json` resynced for both files | `validate-spec-assets.sh`: OK (34 files) |
| 9 | `docs/IMPLEMENTATION_STATUS.md`: new top entry (2026-10-23), and the open Core Devices note in the 2026-10-22 "comparing versions" entry is struck through and corrected | `scripts/test-all.sh` exit 0 |

## Files changed

- `android/app/src/main/java/com/pebblentn/app/update/UpdateCheckMessage.kt` (new)
- `android/app/src/test/java/com/pebblentn/app/update/UpdateCheckMessageTest.kt` (new)
- `android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt`
- `android/app/src/main/res/values/strings.xml`
- `android/app/src/main/java/com/pebblentn/app/update/UpdateCheckRepository.kt` (KDoc only)
- `android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt` (KDoc only)
- `android/app/src/main/AndroidManifest.xml` (comment only)
- `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt` (KDoc only)
- `android/app/src/main/java/com/pebblentn/app/pebble/WatchListenerService.kt` (KDoc only)
- `android/app/src/test/java/com/pebblentn/app/pebble/NavigationControllerTest.kt` (comments only)
- `requirements/REQ-ANDROID.md`
- `spec/200-architecture/Pebble.md`
- `MANIFEST.sha256.json`
- `docs/IMPLEMENTATION_STATUS.md`

## Test results

- Runner check: `UpdateCheckRepositoryTest`: 17 passed.
- New: `UpdateCheckMessageTest`: 5 passed, 0 failed, 0 skipped.
- Full `scripts/test-all.sh`: exit 0. It covers spec assets, protocol, version sync, watchapp C unit tests (no failures), and Android test + lint. The JVM debug unit tests total 429 (424 before + 5 new), all green.
- `:app:assembleDebug :app:lintDebug`: successful.

## Deviations

- `WatchListenerService.appOpenedReadyMessage` KDoc (in the companion object) was also corrected. It called the synthetic READY "the only readiness signal", which is no longer true; it is now "the fallback readiness signal" and says it carries no version. This is comment only, in the spirit of FR3.1, but the plan did not list this spot.
- REQ-ANDROID-019's acceptance line was extended with the message-choice tests rather than adding a second acceptance block.
- `MANIFEST.sha256.json` was updated by hand with `sha256sum` values, because the repo has no resync script. `validate-spec-assets.sh` confirms it.

## Open concerns

- The texts and the toast were not checked on a device (no Android emulator in WSL). The next atomic task in the status doc covers this.
- REQ-ANDROID-018 still ends with "the onboarding disclosure is unchanged (#94)". That is true for #94, but this intent changes the disclosure (FR2.1). It was left as historical context for #94; a reviewer may prefer to reword it.
- Older status entries (2026-09-25, 2026-07-15) still describe the Core Devices limitation as it was then. They were left as history; only the open note was corrected.
- No Android emulator or AVD was started, and no commits were made.
