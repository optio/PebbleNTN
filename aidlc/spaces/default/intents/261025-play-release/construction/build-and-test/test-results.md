# Test Results — Prepare the first Google Play release (#114)

## Build

| Check | Result |
|---|---|
| `verifyReleaseTargetSdk` | passes at 36; fails at 35 (temporary edit, reverted) |
| `assembleDebug`, `assembleRelease`, `bundleRelease` | succeed; the merged manifest has `targetSdkVersion="36"` |
| `pebble build`, all five platforms | succeeds; aplite 3615 B free at load |
| versionCode / versionName | unchanged, 77 / 0.0.77 |

## Unit Tests (this work)

| Suite | Result |
|---|---|
| InstallSourceTest (5), UpdatePolicyTest (3, new), UpdateCheckRepositoryTest (21, 4 PLAY cases), UpdateCardTest (5), UpdatePromptTest (5) | pass |
| DashboardUpdatesScreenTest (6, new), OnboardingScreenTest (3, new) | pass |
| StoreScreenshotsTest (4, new) | opt-in renderer, skipped unless `PEBBLENTN_STORE_SCREENSHOTS_DIR` is set |
| `tools/test_play_store_graphics.py` (12) | pass |

## Full Suite

`scripts/test-all.sh`, rerun in this stage: exit 0, `All tests finished.`

| Check | Result |
|---|---|
| Android JVM tests, debug and release | 504 each: 500 passed, 4 skipped (the opt-in renderer), 0 failed |
| Lint | passes (warnings only) |
| Spec assets, protocol, version sync, workflow YAML parse | OK |
| Watch host suites, rules, regressions, route capture | pass |
| `play_store_graphics.py --check` | Play store images OK |

## Store Images

| File | Size |
|---|---|
| `phoneScreenshots/1.png`–`4.png` | 1080×2160: dashboard, Watch settings, colour list, navigation apps (rendered from the current app as a Play install) |
| `phoneScreenshots/5.png`, `6.png` | 1080×2160: watch navigation screens, and the Settings menu with About first (from the current watchapp) |
| `featureGraphic.png` | 1024×500 |
| `icon.png` | 512×512 (unchanged) |

## Traceability

`aidlc engine sensor-traceability --stage build-and-test`: pass, no uncovered IDs (FR1–FR8).

## Device Check (2026-10-26, requested at the gate: "Run check now")

The maintainer's Galaxy S23 on Android 16 ran the #114 debug build, installed over wireless adb with `adb install -i com.android.vending`. `dumpsys` showed installer `com.android.vending`, targetSdk 36 and 0.0.77.

| Check | Result |
|---|---|
| Onboarding disclosure | pass: the Play variant ("Google Play delivers the updates… makes no network connections of its own") |
| Dashboard Updates section on a Play install | pass: only "0.0.77, updated by Google Play"; no check row, no weekly toggle, no stale prompt |
| Screen bottom, gesture navigation | pass: clears the gesture bar |
| Screen bottom, 3-button navigation (temporarily switched, then restored) | pass: the last card clears the button bar |
| Rule editor, back with unsaved changes (predictive back, target 36) | pass: "Discard your changes?" dialog; the test copy of the rule was deleted afterwards |
| Navigation session to the watch (Google Maps, PebbleKit 2 companion) | pass: the watch showed the route, and the dashboard's "Last sent to watch" read "Straight on · Head toward Kroonstraat · ETA 20:11"; watchapp 0.0.77 reported |

## Not Covered

- **The "Update from Google Play" card on a device:** it shows only when the watchapp is newer than the phone app, and both are 0.0.77. It is JVM-tested, and gets checked after the first internal-testing install.
- **The phone store screenshots are rendered, not device captures,** so there is no system status bar.
