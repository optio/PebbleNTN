# Requirements — Phone app update detected by version comparison

## Sources

- [desc] Initial description: "Detect a phone app update without an internet check by comparing versions: the watchapp auto-updates but the phone (companion) app needs a manual update, so when the watchapp version is higher than the connected phone app version, treat it as update available (watch side: reuse the #94 notice, idle text and launcher subtitle; phone side: show the Update available card when the watch reports a higher version). Compare versions numerically part by part (0.0.9 < 0.0.10), ignore unparsable versions. If the phone app install source is detected as F-Droid, show a warning that the update may take about a day before F-Droid makes it available. The update card links to the install source and says "Update from {source}" instead of "Update now", and lets the user pick another source."
- [Q1] Phone side: A. Best effort. The phone reads the watch's version from WATCH_READY when it arrives. With the Core Devices app, only the watch shows the update (see the discussion in requirements-analysis-questions.md).
- [Q2] Sources: A. GitHub, F-Droid, Obtainium.
- [Q3] Picking another source: A. An "Other sources" button with a short list, used once.
- [Q4] F-Droid warning: B. On the card, short text: "F-Droid may take about a day to offer this update."
- Code knowledge base: `aidlc/spaces/default/codekb/PebbleNTN/`, which was refreshed by this intent's focused scan. See architecture.md "Version Exchange" and code-quality-assessment.md "Version-compare update findings".
- Earlier work: #94 (update notice, idle text, subtitle, protocol 1.2), #103 (the watch stores the phone version from key 11).

## Intent Analysis

The store updates the watchapp automatically, but users update the phone app by hand. Both apps share one version number (`scripts/version.sh`), so a watchapp that is newer than the phone app means a newer phone app has been published. Today the user only learns about it through the internet check, which is off by default. This work adds a check that needs no network.

- **Watch:** every phone message already carries the phone app's version (key 11), so the watch can compare it with its own version and show the #94 update UI.
- **Phone:** it can do the same only when the watch's WATCH_READY message reaches it. That happens with the original Pebble app, but never with the Core Devices app, and PebbleKit 2 offers no other way to learn the watchapp version.
- **Update card:** it now names the source the phone app was installed from, warns F-Droid users about the delay, and offers the other sources.

- **Type**: enhancement, multi-component (watchapp, Android app), Minimal depth. No protocol change.
- **Users**: everyone whose watchapp auto-updated ahead of the phone app, and everyone who sees the phone's update card.

## Functional Requirements

### FR1 — Version comparison rule

- **FR1.1** A version is parsable when, after an optional leading `v` or `V`, it is one or more parts separated by `.`, and every part is 1 to 9 decimal digits. Nothing else is allowed: no suffix, sign or space inside it, and no empty part. Anything else is unparsable.
- **FR1.2** Two parsable versions SHALL be compared numerically, part by part from the left, with missing parts counting as 0 (`0.0.9` < `0.0.10`, `1.2` = `1.2.0`).
- **FR1.3** A comparison involving an unparsable or missing version SHALL never report "newer". The version is ignored.
- **FR1.4** The rule SHALL be the same on the watch (a new pure C unit) and on the phone (a strict parse beside the existing tolerant `AppVersions`, whose GitHub-tag behaviour is unchanged).

Acceptance:
- Given `0.0.10` and `0.0.9`, When compared, Then `0.0.10` is newer (host C test and JVM test).
- Given `1.2` and `1.2.0`, Then they are equal.
- Given `0.0.71-debug`, `0.0.x`, `0..1`, `""` or a missing value against `0.0.70`, Then neither side is newer.

### FR2 — The watch detects that it is newer

- **FR2.1** On each inbound message that carries the phone app version (key 11), and at startup from the stored version (persist key 16, #103), the watch SHALL compare its own version (`APP_VERSION_STR`) with the phone app version under FR1. When its own version is newer, the watch SHALL treat its own version as an available phone app version.
- **FR2.2** The update version the watch uses (the "effective update version") SHALL be the higher of:
  - the version the phone reports with flag bit 4 and key 14 (#94);
  - the watch's own version when FR2.1 finds it newer.
  
  When neither applies, there is no update. Clearing bit 4 in a phone message SHALL NOT clear an update found by FR2.1.
- **FR2.3** The #94 update notice (once per version, deferred during a route), the update idle text ("No navigation detected. Phone app update available: open the mobile app.") and the launcher subtitle `Update: {version}` SHALL all use the effective update version. Their text, timing and once-per-version behaviour are unchanged.
- **FR2.4** No new persist key SHALL be added. The effective update version is derived from the stored phone version (16), the stored phone-reported version (17) and the watch's own version. The shown-version key (18) works as before.

Acceptance:
- Given the watch is `0.0.72` and the phone sends key 11 = `0.0.71` with bit 4 clear, When the watchapp opens with no route, Then the notice appears once for `0.0.72`, the idle text shows the update text, and on exit the subtitle is `Update: 0.0.72` (emulator, using `pebble send-app-message`).
- Given the phone then sends key 11 = `0.0.72`, Then the update text and subtitle are gone (no update).
- Given the watch is `0.0.72`, the phone is `0.0.71` and the phone reports bit 4 with key 14 = `0.0.73`, Then the effective update version is `0.0.73` (host test).
- Given the phone version is unparsable or was never received, Then FR2.1 finds no update (host test).

### FR3 — The phone learns the watch's version (best effort)

- **FR3.1** When a WATCH_READY message arrives that carries the watch's version (key 11), the phone SHALL read it and store it (SharedPreferences, written only on change) as the last seen watchapp version [Q1].
- **FR3.2** The made-up readiness events the phone creates itself (the timed one in NavigationController, and the one from `onAppOpened`) carry no version. They SHALL NOT change or clear the stored watchapp version.
- **FR3.3** When the stored watchapp version is newer than the phone app's own version under FR1, the phone SHALL treat it as an available update, even if the internet check has never run.
- **FR3.4** The phone's effective update version SHALL be the higher of:
  - the internet check's latest version, when it reports an update;
  - the stored watchapp version, when FR3.3 applies.
  
  It drives the dashboard card's shown version, and flag bit 4 and key 14 sent to the watch (#94).
- **FR3.5** With the Core Devices Pebble app, WATCH_READY does not reach the phone, so FR3 has no effect there. This is accepted [Q1]. The watch side (FR2) covers that case.

Acceptance:
- Given a WATCH_READY with key 11 = `0.0.72` while the phone app is `0.0.71`, When it is decoded and handled, Then the stored watchapp version is `0.0.72` and the phone reports an update to `0.0.72` with no network call (JVM test).
- Given a WATCH_READY without key 11, Then the stored watchapp version is unchanged.
- Given the internet check says `0.0.73` and the watch says `0.0.72`, Then the effective version is `0.0.73`.

### FR4 — Install source

- **FR4.1** The phone SHALL detect its own install source once per app start, using `PackageManager.getInstallSourceInfo(packageName).installingPackageName`:
  - **F-Droid** for the F-Droid client and the known F-Droid-repo clients: `org.fdroid.fdroid`, `org.fdroid.basic`, `com.looker.droidify`, `com.machiav3lli.fdroid`;
  - **Obtainium** for `dev.imranr.obtainium` and package names that start with `dev.imranr.obtainium.`;
  - **GitHub** for everything else: a missing installer, a browser, the package installer, adb, an unknown store, or a failed lookup.
- **FR4.2** Each source SHALL have one destination [Q2]:
  - **GitHub:** the latest release page (`update_releases_url`).
  - **F-Droid:** `https://f-droid.org/packages/com.pebblentn.app/`. An installed F-Droid client handles this link; otherwise it opens in the browser.
  - **Obtainium:** the Obtainium app's launch screen when it is installed; otherwise its website `https://obtainium.imranr.dev/`.

Acceptance:
- Given each installer package name above, `null`, `com.android.packageinstaller` and an exception, When the source is resolved, Then the result is F-Droid, Obtainium or GitHub as listed (JVM test of a pure mapping function).

### FR5 — The update card

- **FR5.1** The main button SHALL read "Update from {source}" (GitHub, F-Droid or Obtainium) and open that source's destination (FR4.2). It replaces "Download latest version".
- **FR5.2** A second, smaller "Other sources" button SHALL open a short list of the other two sources. Picking one opens its destination once. The detected source stays the default and nothing is stored [Q3].
- **FR5.3** The card text SHALL name the effective update version and the installed version. The existing advice, "If it won't install over the current app, uninstall PebbleNTN first, then install the downloaded version", SHALL stay, because switching sources can hit a signing mismatch. The GitHub-only phrase "download the latest APK from GitHub" SHALL be replaced by text that fits any source.
- **FR5.4** When the detected source is F-Droid, the card SHALL add: "F-Droid may take about a day to offer this update." [Q4]. Other sources show no delay text.
- **FR5.5** All new text SHALL be string resources. URLs and package names SHALL be `translatable="false"`.

Acceptance:
- Given the source is F-Droid and an update to `0.0.72`, When the dashboard shows the card, Then the button reads "Update from F-Droid", the delay sentence is present, and "Other sources" lists GitHub and Obtainium (pure helper test for the card model; layout checked on a device).
- Given the source is GitHub, Then there is no delay sentence, and "Other sources" lists F-Droid and Obtainium.

### FR6 — Specification

- **FR6.1** The specs SHALL be updated:
  - `requirements/REQ-WATCH.md`: amend REQ-WATCH-023 for FR2.
  - `requirements/REQ-ANDROID.md`: add a requirement for FR3, and one for FR4 and FR5.
  - `spec/200-architecture/Protocol.md`: note that the phone now reads key 11 from WATCH_READY. The protocol itself does not change.
  - `MANIFEST.sha256.json`: resync if a listed file changes.

Acceptance:
- Given the updated files, When `scripts/test-all.sh` runs, Then spec-asset validation passes.

## Non-Functional Requirements

- **NFR1 — No protocol change**: no new key, flag or event, and no version bump. Pass: `protocol/protocol-definition.json` is unchanged and the generated-constants check passes.
- **NFR2 — No new network access**: detection by version comparison and install-source lookup use no network. Pass: the existing "no call on launch" tests stay green.
- **NFR3 — Flash wear**: the watch adds no persist writes. The phone writes the watchapp version only when it changes. Pass: host test (no new keys) and a JVM test (same value again → no write).
- **NFR4 — Layout**: the watch reuses the #94 texts. The card's two buttons and text fit on a phone screen. Pass: watch emulator screenshots on basalt, chalk, emery and diorite for the FR2 case. The card layout is a manual on-device check (no Android emulator in WSL).
- **NFR5 — Tests**: the version rule has host C tests and JVM tests (FR1), and so do FR2.2, FR3 and the FR4 mapping. `scripts/test-all.sh` stays green.

## Constraints

- Both apps share one version number from `scripts/version.sh`. This work depends on it.
- The watch's inbox processing and the generated protocol files are unchanged.
- PebbleKit 2 cannot provide the watchapp version, and the Core Devices app does not forward watch messages. Watch AppMessages cannot be rejected on purpose, because the SDK sends ACK and NACK by itself.
- No Android emulator in WSL; Pebble emulator only, closed after use.

## Assumptions

- A newer watchapp means a newer phone app exists on GitHub. F-Droid may lag by about a day, which FR5.4 covers.
- The watch's own version and the phone version (key 11) are plain `x.y.z` release versions. A developer build with a different format is ignored (FR1.3).
- `https://f-droid.org/packages/com.pebblentn.app/` is handled by installed F-Droid clients and works in a browser otherwise.
- The F-Droid client package list in FR4.1 covers the common clients. Anything not listed falls back to GitHub, which still works.

## Out of Scope

- Asking Core Devices or Rebble to expose the watchapp version in PebbleKit 2.
- Remembering a chosen source [Q3].
- Changes to the internet check itself, or to the #94 stale-check and weekly prompts.
- Showing the install source or the F-Droid delay on the watch.

## Open Questions

None.
