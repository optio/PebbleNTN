# Requirements — Prepare the first Google Play release (#114)

## Sources

- Initial description: [desc] "Implement GitHub issue #114: prepare the first Google Play release (target SDK 36, Play install source without self-updates, privacy policy and data safety for network use, store graphics, reviewer app access, Play Console answers doc). Commits and the merge must carry [skip ci] so no release or version bump is triggered."
- Workflow-selected scope: [scope] express
- GitHub issue #114
- Conversation decisions before this stage:
  - one app for every source, with the Play install detected at runtime;
  - Play App Signing uses the existing release key.
- The code knowledge base (focused scan at 6dec020), and the handoff `inception/reverse-engineering/developer-scan-PebbleNTN.md` ("Facts relevant to #114")
- Answers Q1–Q6 in `requirements-analysis-questions.md`

## Intent Analysis

- **Request type:** release readiness for a new distribution channel. Brownfield.
- **Parts:** Android code (install source, update UI, SDK target), CI workflows, docs, store graphics, and one watchapp asset (the QR code).
- **Corrections to the issue, from the scan:**
  - Online rule updates are not implemented. The docs describe only the GitHub update check and the links the user taps.
  - A Play install must also ignore the GitHub update data saved by an earlier GitHub install.
  - The target-SDK gate never fails today.
  - The raw watch screenshots are too small for Play.
- **Delivery:** commits and the merge carry `[skip ci]`. No release and no version bump comes from this work; the changes ship with the next normal release.
- **Requirements depth:** Minimal.

## Functional Requirements

### FR1 — Target SDK 36 and a real gate

- Set `targetSdk = 36` in `android/app/build.gradle.kts` (compileSdk is already 36), and `playMinTargetSdk = 36`.
- `.github/workflows/play-release.yml` runs `verifyReleaseTargetSdk` and fails the job when it fails (remove the `|| echo` fallback). `release.yml` runs the same gate before building.
- `docs/RELEASE_CHECKLIST.md` keeps the gate under "Automated".
- **Pass/fail:**
  - `./gradlew verifyReleaseTargetSdk` passes at 36 and fails when `targetSdk` is set below the floor;
  - the full suite stays green;
  - on the maintainer's Android 16 phone, onboarding, the dashboard (with 3-button and gesture navigation), the rule editor's back/discard dialog and a navigation session to the watch work.

### FR2 — Google Play as an install source

- Add `InstallSource.PLAY`, detected when the installer is `com.android.vending`.
- **On a Play install:**
  - **(a)** no GitHub request is ever made: no manual "Check for updates" row, no weekly toggle, no stale-check prompt, no weekly question, and no check on app start;
  - **(b)** the GitHub update data saved by an earlier install (`latest_version`, `auto_check_enabled`, `last_check_millis`) is ignored, so it never shows an update [scan];
  - **(c)** the watchapp-newer-than-phone notice stays [Q1]:
    - its button reads "Update from Google Play" and opens `market://details?id=com.pebblentn.app`, falling back to `https://play.google.com/store/apps/details?id=com.pebblentn.app`;
    - a line says a new version can take a day or two to reach Google Play;
    - no "Other sources" choice and no "uninstall first" text;
  - **(d)** the onboarding disclosure uses a variant without the GitHub-check sentence.
- **Other sources keep today's behaviour.** The watch's bit 4 / key 14 notice still comes from the watchapp-newer comparison.
- **Pass/fail:** JVM tests for:
  - `com.android.vending` → PLAY (replacing today's → GITHUB assertion);
  - Play mode ignoring a stored GitHub version;
  - the update card for PLAY;
  - the dashboard showing no GitHub controls for PLAY;
  - the disclosure variant.

### FR3 — Accurate privacy policy and Data safety answers

- **`docs/PRIVACY_POLICY.md`:**
  - fill in the effective date and the contact, `pebblentn@mg.mijnesite.be` and the GitHub issues page [Q6];
  - add a "Network" section: the optional GitHub update check on non-Play installs (what is requested, that no personal data is sent, that GitHub sees the IP address, how to turn it off), none on Play installs;
  - describe the links the user opens (GitHub, Play, Pebble store pages, feedback forms) and the email drafts the user sends;
  - make no claim of online rule updates.
- **`docs/PLAY_DATA_SAFETY.md`:** answers matching the policy:
  - no data collected or shared by the developer;
  - the Play build makes no automatic network requests;
  - user-initiated sharing is described;
  - INTERNET is listed with its use.
- **Also:**
  - fix the outdated line in `docs/PRIVACY_REVIEW.md` and the `AndroidManifest.xml` INTERNET comment;
  - remove or reword "More apps can be added without a new release" in `full_description.txt`.
- **Pass/fail:** every network use and outbound link in the scan's table appears in the policy, and no statement contradicts the code.

### FR4 — Store graphics, made by a script [Q5]

- Add a script, `tools/play_store_graphics.py`, with Pillow, runnable from the pebble tool's Python or any Python with Pillow. It writes into `fastlane/metadata/android/en-US/images/`:
  - phone screenshots cropped to 1080×2160 (2:1);
  - at least two extra phone-size images that show the watch screenshots from `screenshots/`, scaled up with nearest-neighbour and composed on a canvas with a short caption. Every side is at least 320 px and the ratio is at most 2:1;
  - `featureGraphic.png`, 1024×500, from the icon, the name and a watch screenshot.
- The outputs are committed. A JVM or script check verifies the sizes and ratios against Play's rules.
- **Pass/fail:**
  - the check passes;
  - the images open, at the stated sizes;
  - title ≤ 30, short description ≤ 80 and full description ≤ 4000 characters.

### FR5 — Separate Play upload key [Q2], manual bundle upload [Q3]

- `play-release.yml` signs the AAB with `PLAY_UPLOAD_KEYSTORE_BASE64`, `PLAY_UPLOAD_KEYSTORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS` and `PLAY_UPLOAD_KEY_PASSWORD`. It fails with a clear message when any are missing. `release.yml` keeps the release key.
- The workflow attaches the AAB and its checksum to the run, as today. There is no automatic Play upload and no AAB on the GitHub release.
- **Pass/fail:**
  - the workflow parses (`scripts/test-all.sh` YAML check);
  - a local `bundleRelease` with an upload key in the same environment variables produces a signed AAB.

### FR6 — One install page for every source [Q4]

- **README:** the "Distribution" section lists every source: Google Play ("Coming soon on Google Play" until the listing is live), GitHub releases, F-Droid and Obtainium, with the one-signature note.
- **Watch QR:** the code points to `https://github.com/optio/PebbleNTN#distribution`. `watchapp/tools/gen_connect_qr.py` regenerates `connect_qr.png`, and it stays QR version 3 (29×29), so aplite's memory is unchanged.
- **About page:** the link text under the QR matches.
- **Pass/fail:**
  - the regenerated QR decodes to the new URL;
  - `pebble build` passes with aplite free heap no lower than before;
  - About and the connection hint render on the emulator.

### FR7 — `docs/PLAY_CONSOLE.md`

One doc with every Play Console answer and step:
- **App content:**
  - privacy policy URL;
  - ads: none;
  - app access: reviewer instructions for what works without a watch and what needs one, plus a script for a short demo video;
  - content rating answers;
  - target audience: adults;
  - news, government, financial and health: no;
  - Data safety: from FR3.
- **Notification access:** the justification and the disclosure.
- **Store listing:** category Maps & Navigation, contact (Q6), website, and where the graphics come from.
- **Signing:** the steps for the existing release key with PEPK, the new upload key with keytool, the GitHub secrets, and backups.
- **Release:** the `play-release` workflow, downloading the AAB, and release notes from `changelogs/<versionCode>.txt`.
- **Before production:** the closed test (12 testers, 14 days) and developer verification of the package and key.

Update `docs/RELEASE_CHECKLIST.md` → "Store submission" to point to it.

### FR8 — Specs and docs

- **Amend:**
  - REQ-ANDROID-020: Play as a source, with no external update links or checks on Play installs;
  - REQ-ANDROID-013: the GitHub check is the only automatic network access, and none on Play installs;
  - REQ-ANDROID-018 where it names the sources;
  - `spec/600-security-release/SecurityPrivacy.md`: the disclosure, including the Play variant;
  - REQ-WATCH-017/020 for the QR URL.
- **Add:** REQ-ANDROID-022, Google Play distribution: target SDK gate, upload key, store assets and the Play Console doc.
- **Then:** resync `MANIFEST.sha256.json`, and add an entry to `docs/IMPLEMENTATION_STATUS.md` before coding (CLAUDE.md).

## Non-Functional Requirements

- **Policy compliance:**
  - nothing in a Play install downloads or points to app updates outside Google Play;
  - the app downloads no executable code.
- **No release:** every commit on the branch, and the merge commit, contains `[skip ci]`. `versionCode` and `versionName` stay at 77 / 0.0.77.
- **Compatibility:** GitHub, F-Droid and Obtainium installs behave exactly as in v0.0.77.
- **Testability:** the install-source and update-UI rules stay pure functions with JVM tests. The full `scripts/test-all.sh` stays green and runs locally before the merge, because `[skip ci]` also skips the PR checks.

## Constraints

- Generated files are not edited by hand. MANIFEST-protected files are resynced in the same change.
- No signing material in the repository (REQ-SEC-007).
- No Android emulator in WSL. Device checks use the maintainer's phone over wireless adb.

## Assumptions

- Pillow is available through the pebble tool's Python. The system Python has none.
- The README's "Distribution" heading keeps the anchor `#distribution`.

## Out of Scope

- Automatic upload to Play (Q3 B) and an AAB on GitHub releases (Q3 C).
- The closed test, the Play Console entries and developer verification themselves: these are the maintainer's manual steps, documented in FR7.
- Implementing online rule updates.

## Open Questions

None.
