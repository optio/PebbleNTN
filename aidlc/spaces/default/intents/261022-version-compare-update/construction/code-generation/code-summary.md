# Code Summary — Phone app update detected by version comparison

All 16 plan steps are done and ticked in `code-generation-plan.md`. There is no protocol change: `protocol/protocol-definition.json` and the generated files are untouched. Nothing was committed.

## What was built, step by step

1. **Structure.** Created `watchapp/src/c/version_compare.{h,c}`, `update/InstallSource.kt` and `ui/dashboard/UpdateCard.kt`.
2. **Runners.** `scripts/test-watchapp-unit.sh` pairs each `tests/test_<unit>.c` with `src/c/<unit>.c`. Both it and the scoped Gradle command (with the existing classes) were green before any new test was written.
3. **Data model.** `UpdateState` has a new `watchAppVersion`. `updateAvailable` is now "GitHub newer (tolerant) OR watch strictly newer". The new `effectiveLatest` is the higher of the two, or null.
4. **Repository.** `UpdateCheckRepository.recordWatchAppVersion(v)` trims the version and ignores a blank one. It writes the `watch_app_version` pref only when the value changes, and returns true only then. The value is loaded at start, and `checkForUpdate` keeps it (`_state.update { copy(...) }`).
5. **Repository tests.** Five new tests in `UpdateCheckRepositoryTest`:
   - a newer watch version gives an update with no fetch, and survives a new instance;
   - the same value again causes no write;
   - an unparsable or equal version gives no update;
   - GitHub `v0.0.73` and watch `0.0.72` give `v0.0.73`;
   - GitHub `0.0.72` and watch `0.0.74` give `0.0.74`.
6. **Watch rule.** `version_parse`, `version_newer` and `version_effective_update`, in pure C11 with no Pebble includes.
7. **Watch tests.** `watchapp/tests/test_version_compare.c` has 38 assertions covering parse, compare and the effective update version.
8. **Phone rule, source and card model.**
   - `AppVersions.parseStrict` and `isNewerStrict`. The tolerant `compare` now shares `compareParts` and behaves as before.
   - `InstallSource` and `InstallSources.fromInstaller`. `detect(context)` logs a failed lookup and returns GitHub.
   - `updateCardModel(source)`.
9. **Phone tests.**
   - `AppVersionsTest`: 3 new tests that mirror the C cases.
   - `InstallSourceTest`: 3 tests (new).
   - `UpdateCardTest`: 3 tests (new).
10. **Protocol edge.**
    - `ProtocolCodec.watchAppVersion(message)`.
    - `NavigationController(onWatchAppVersion = …)` is called from `handleInbound` before the ready event is dispatched. A callback failure is logged.
    - `AppContainer` wires the callback to `recordWatchAppVersion` and uses `effectiveLatest` for `latestPhoneUpdate`. It also detects `installSource` once.
11. **Edge tests.**
    - `ProtocolCodecTest`: 3 new tests (with key 11; without it or blank; another event).
    - `NavigationControllerTest`: 2 new tests. A genuine WATCH_READY reports the version. The autonomous READY and `appOpenedReadyMessage` report none.
12. **Watch UI.** `main.c` has a new `effective_update()`. It feeds `maybe_show_update_notice`, which uses it for the decision and for the key 18 copy. It also feeds the NO_ACTIVE_NAVIGATION idle text and the glance subtitle. `record_phone_version` and `record_update` still run first on every message (a comment now says so). The header comments are updated. No persist key was added.
13. **Phone UI.**
    - `DashboardScreen`: the `onDownloadUpdate` parameter is replaced by `installSource` and `onOpenUpdateSource`. A new private `UpdateAvailableCard` shows the source-neutral body, plus the F-Droid delay line, a "Update from %1$s" button, and an "Other sources" button. That button opens an `AlertDialog` that lists the other two sources, with Cancel.
    - `PromptCard`'s two-button row is now a `FlowRow`, so the buttons wrap on narrow screens. This also applies to the weekly-question card.
    - `MainActivity.openUpdateSource(source)` handles the three destinations. Obtainium opens the app's launch intent for `dev.imranr.obtainium` or its `.fdroid` variant, and otherwise its website. The dashboard gets `effectiveLatest`.
    - `strings.xml`: added `dashboard_update_from`, `_other_sources`, `_pick_source`, `_fdroid_delay` and the three `dashboard_update_source_*` names. Added `update_fdroid_url` and `update_obtainium_url` (`translatable="false"`). The body is now source-neutral and keeps the uninstall advice. `dashboard_update_download` was removed because nothing used it.
14. **Emulator.** The watchapp was built and checked on 4 platforms (see the evidence below). The debug APK was built.
15. **Specs.**
    - `REQ-WATCH.md`: REQ-WATCH-023 is amended.
    - `REQ-ANDROID.md`: REQ-ANDROID-019 and REQ-ANDROID-020 are new.
    - `Protocol.md`: the WATCH_READY line notes that it carries the version.
    - `MANIFEST.sha256.json`: the three hashes are resynced, and `validate-spec-assets.sh` is OK (34 files).
16. **Docs.** A new top entry in `docs/IMPLEMENTATION_STATUS.md`. `scripts/test-all.sh` exits 0.

## Files

Created:
- `watchapp/src/c/version_compare.h`, `watchapp/src/c/version_compare.c`, `watchapp/tests/test_version_compare.c`
- `android/app/src/main/java/com/pebblentn/app/update/InstallSource.kt`
- `android/app/src/main/java/com/pebblentn/app/ui/dashboard/UpdateCard.kt`
- `android/app/src/test/java/com/pebblentn/app/update/InstallSourceTest.kt`
- `android/app/src/test/java/com/pebblentn/app/ui/dashboard/UpdateCardTest.kt`

Changed:
- `watchapp/src/c/main.c`
- `android/app/src/main/java/com/pebblentn/app/update/AppVersions.kt`, `update/UpdateCheckRepository.kt`
- `android/app/src/main/java/com/pebblentn/app/protocol/ProtocolCodec.kt`
- `android/app/src/main/java/com/pebblentn/app/pebble/NavigationController.kt`
- `android/app/src/main/java/com/pebblentn/app/di/AppContainer.kt`
- `android/app/src/main/java/com/pebblentn/app/ui/MainActivity.kt`, `ui/dashboard/DashboardScreen.kt`
- `android/app/src/main/res/values/strings.xml`, `android/app/src/main/AndroidManifest.xml`
- Tests: `AppVersionsTest.kt`, `UpdateCheckRepositoryTest.kt`, `ProtocolCodecTest.kt`, `NavigationControllerTest.kt`
- `requirements/REQ-WATCH.md`, `requirements/REQ-ANDROID.md`, `spec/200-architecture/Protocol.md`, `MANIFEST.sha256.json`, `docs/IMPLEMENTATION_STATUS.md`

## Test results

- **Host C** (`scripts/test-watchapp-unit.sh`): 9 test programs, all OK. `test_version_compare` has 38 assertions. The suite prints 182 `ok` lines in total.
- **JVM, scoped command** from unit-test-instructions.md, all green:

  | Test class | Tests | New |
  |---|---|---|
  | `AppVersionsTest` | 9 | 3 |
  | `UpdateCheckRepositoryTest` | 17 | 5 |
  | `InstallSourceTest` | 3 | 3 (new class) |
  | `ProtocolCodecTest` | 19 | 3 |
  | `NavigationControllerTest` | 18 | 2 |
  | `UpdateCardTest` | 3 | 3 (new class) |

  That makes 19 new JVM tests.
- **Full `scripts/test-all.sh`:** exit 0. The debug unit tests count 421 tests in 68 classes, with 0 failures, errors or skips (402 before this work). Lint and spec, rules and catalog validation all pass.
- **`:app:assembleDebug :app:lintDebug`:** green, and lint reports nothing new for the touched files.

## Emulator evidence

Pebble emulator only. `pebble wipe` ran first. The run used the watch at 0.0.71 and sent NO_ACTIVE_NAVIGATION with flags = 0 (bit 4 clear) and key 11 = `0.0.70`. The screenshots are in this folder:

| Screenshot (`emulator-<p>-…`, p = basalt, chalk, emery, diorite) | What it shows |
|---|---|
| `-1-notice.png` | The full-screen notice "A phone app update is available. Open the mobile app to find the update." |
| `-2-idle-update.png` | After BACK closed the notice: "No navigation detected. Phone app update available: open the mobile app." |
| `-3-no-second-notice.png` | The same message sent again: the idle update text, with no second notice (once per version). |
| `-4-launcher-subtitle.png` | Launcher, PebbleNTN highlighted: `Update: 0.0.71` (the watch's own version, since bit 4 was never set). |
| `-5-equal-idle-usual.png` | The app reopened with key 11 = `0.0.71`: "No navigation detected. If navigating, open PebbleNTN on your phone." |
| `-6-equal-launcher-hint.png` | Launcher subtitle back to "Navigate on phone" (no update). |

The text stays inside the bezel on chalk. After the run, `pebble kill` was used and no `qemu-pebble` or `pypkjs` process remains.

## Deviations from the plan

1. **The strict `AppVersions` functions were written in Step 3, not Step 8.** `UpdateState` (Step 3) depends on `isNewerStrict`. Their tests stayed in Step 9.
2. **`AndroidManifest.xml` changed, which the plan did not list.** It now has `<queries>` entries for `dev.imranr.obtainium` and `dev.imranr.obtainium.fdroid`. Without them, `getLaunchIntentForPackage` returns null on Android 11+ (targetSdk 35), and FR4.2's "launch Obtainium" could never work. `validate_catalog.py` only checks for missing catalog packages, so the extra entries pass.
3. **`version_parse(const char *v)` only validates.** It returns no parts. `version_newer` walks both strings part by part, so the C rule has no maximum number of parts, the same as the phone's `parseStrict`.
4. **`recordWatchAppVersion` returns a `Boolean` (written or not).** On Android, `SharedPreferences` listeners don't fire for a same-value write, so the return value is how the "same value again → no write" test (NFR3) proves it.
5. **`PromptCard`'s action row is now a `FlowRow`.** "Update from Obtainium" and "Other sources" may not fit on one line on narrow phones (NFR4). The weekly-question card gets the same wrapping.
6. **`pebble wipe` before the emulator run.** The #94 checks had left key 18 = `0.0.71` in the emulator's storage, and that would have hidden the notice.
7. **Extra test cases beyond the plan's list:**
   - blank version (codec and repository);
   - `dev.imranr.obtainiumx` is not Obtainium;
   - Chrome and an empty installer name map to GitHub;
   - the Obtainium card model;
   - in C: a trailing dot, a space, a sign, a bare `v`, and an unparsable reported version being kept.

## Open concerns

- **The update card is not checked on a device.** Its layout, the "Other sources" dialog and the three destinations need a real phone, because an Android emulator crashes WSL. The card model is unit tested, and the APK builds and passes lint.
- **The F-Droid package page is assumed.** `https://f-droid.org/packages/com.pebblentn.app/` works only once the app is published on F-Droid. The requirements accept this.
- **Phone side with the Core Devices app.** WATCH_READY never reaches the phone there, so `recordWatchAppVersion` is never called (accepted, FR3.5). The watch side covers this case.
- **Effective version format on the phone.** When the GitHub tag wins, `effectiveLatest` keeps its `v` (for example `v0.0.73`), as `latestVersion` did before. The codec strips the `v` for key 14, and the card shows the tag as before.
- **Unparsable reported version on the watch.** If key 14 is unparsable (for example `0.0.73-rc`) and the watch's own version is also newer, the watch keeps the phone's reported version, because the two can't be compared. This is documented in `version_compare.h` and tested.
- **The phone-to-watch path was not run end to end on real hardware.** The watch was driven with `pebble send-app-message`.
