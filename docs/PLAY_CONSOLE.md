# Google Play Console — answers and steps

Everything needed to publish PebbleNTN (`com.pebblentn.app`) on Google Play and to release updates:
the App content answers, the store listing, signing, the release procedure, and what Google requires
before production (REQ-ANDROID-022, #114). Google changes Play Console screens and rules from time
to time; when a screen differs from this page, follow Play Console and update this page.

PebbleNTN is free and has no in-app purchases. The Play build is the same app as on the other
sources: it detects a Play install at runtime (installer
`com.android.vending`): such an install never asks GitHub for updates, shows no GitHub update link,
and gets its updates from Play only. GitHub, F-Droid and Obtainium installs behave as before.

## 1. Before the first upload: signing

PebbleNTN has one signature everywhere, so users can switch between Play, GitHub, F-Droid and
Obtainium without uninstalling. Play therefore signs with the **existing release key** (the one
that signs the GitHub APK and that F-Droid verifies), and a **separate upload key** signs the
bundles sent to Play. Never commit either key (REQ-SEC-007).

1. **Enrol the release key in Play App Signing (PEPK).** Create the app in Play Console, then in
   *Test and release > App integrity > App signing* choose to use an existing app signing key from a
   Java keystore. Download `pepk.jar` and Google's encryption public key from that page and run, on
   the machine that holds the release keystore:

   ```bash
   java -jar pepk.jar --keystore=pebblentn-release.jks --alias=<release key alias> \
     --output=pebblentn-signing-key.zip --include-cert --rsa-aes-encryption \
     --encryption-key-path=encryption_public_key.pem
   ```

   Upload `pebblentn-signing-key.zip` there, then delete it. Play Console shows the app signing
   certificate's SHA-256: it must equal the one in the `release` workflow's run summary
   (F-Droid's `AllowedAPKSigningKeys`).

2. **Create the upload key** (a new key, used only for Play):

   ```bash
   keytool -genkeypair -v -keystore pebblentn-play-upload.jks -alias play-upload \
     -keyalg RSA -keysize 4096 -validity 10000
   keytool -export -rfc -keystore pebblentn-play-upload.jks -alias play-upload \
     -file upload_certificate.pem
   ```

   Register `upload_certificate.pem` as the upload key in the same App signing page (the first
   uploaded bundle also registers it). If the upload key is ever lost or leaked, ask Google for an
   upload key reset there; the app signing key is not affected.

3. **Store the upload key as GitHub secrets.** In *GitHub > Settings > Environments > release*, add:

   | Secret | Value |
   |---|---|
   | `PLAY_UPLOAD_KEYSTORE_BASE64` | `base64 -w0 pebblentn-play-upload.jks` (the whole output) |
   | `PLAY_UPLOAD_KEYSTORE_PASSWORD` | the keystore password |
   | `PLAY_UPLOAD_KEY_ALIAS` | `play-upload` |
   | `PLAY_UPLOAD_KEY_PASSWORD` | the key password |

   The `play-release` workflow fails with a message naming these four secrets if any is empty. The
   release key's own secrets (`UPLOAD_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD`) stay as they are, for `release.yml` only.

4. **Back up both keystores offline**, each with its passwords: at least two copies on encrypted
   storage kept apart (for example an encrypted USB drive and a password manager attachment), never
   in the repository or a cloud folder synced with it. Losing the release key would break updates
   for GitHub, F-Droid and Obtainium users; Play keeps its own copy after step 1.

## 2. App content (Policy and programs > App content)

### Privacy policy

`https://github.com/optio/PebbleNTN/blob/main/docs/PRIVACY_POLICY.md`

### Ads

No, the app contains no ads.

### App access

Choose **All or some functionality is restricted**, and add these instructions (no login is needed):

> PebbleNTN shows turn-by-turn directions from navigation apps on a Pebble smartwatch. There is no
> account and no login.
>
> Works without a watch: onboarding and the notification-access disclosure; granting notification
> access; the dashboard; Navigation apps (enable or disable each supported app); Rules (browse the
> official rules, create, edit and test your own); Debug history (start Google Maps, Organic Maps,
> CoMaps or OsmAnd navigation to see the captured notifications and how they were parsed); Share
> diagnostics (review and share a privacy-safe or full export).
>
> Needs a Pebble watch with the Pebble companion app and the PebbleNTN watchapp: the directions on
> the watch, the watchapp version row and the Watch settings screen (it opens only after the watch
> has sent its settings). The demo video shows these parts: <link to the video>.

**Demo video script** (1–2 minutes, phone screen recording plus a camera shot of the watch;
upload it unlisted on YouTube and paste the link above):

1. Open PebbleNTN for the first time: the onboarding screen with the disclosure.
2. Tap **Grant notification access**, enable PebbleNTN in Android settings, go back: the dashboard.
3. Open **Navigation apps**: the supported apps, one toggle each.
4. Start navigation to an address in Google Maps; show the phone notification.
5. Show the watch: the PebbleNTN watchapp opens with the arrow, distance, road and ETA; walk or
   drive until the next turn and show it update.
6. Back in PebbleNTN: **Debug history** lists the notification and how it was read.
7. Open **Watch settings**, change the accent colour, and show the watch follow.
8. End navigation: the watch returns to the watch face.

### Content rating (IARC questionnaire)

- Category: **All other app types** (a utility; not a game, not social, not a store).
- Violence, sexuality, language, controlled substances, gambling, horror: **No** to every question.
- Users interact or exchange content with other users: **No**.
- Shares the user's current location with other users: **No** (the app has no location access).
- Digital purchases: **No**.
- Unrestricted internet access through an in-app browser: **No** (links open in the user's browser).

Expected rating: Everyone / PEGI 3 / USK 0.

### Target audience and content

- Target age groups: **18 and over** only. The app is a driving, riding and walking aid, not
  designed for children.
- Appeal to children: **No**.

### Other declarations

| Declaration | Answer |
|---|---|
| News app | No |
| Government app | No |
| Financial features | None |
| Health apps | No health features |
| Advertising ID | No, the app does not use the advertising ID |
| COVID-19 contact tracing or status | No |

### Data safety

Enter the answers in [`PLAY_DATA_SAFETY.md`](PLAY_DATA_SAFETY.md): **no data collected or shared**.

### Notification access

Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`) is the app's core function. When Play
Console or a reviewer asks why the app needs it, use:

> PebbleNTN's only purpose is to show turn-by-turn directions from the user's navigation app on a
> Pebble smartwatch. Navigation apps publish their next turn only as an ongoing notification, so
> PebbleNTN needs notification access to read it. It reads notifications only from the navigation
> apps the user keeps enabled (checked by package name before any content is read), extracts the
> next turn on the device, and sends it to the watch. Nothing is uploaded; there is no server.

**Prominent disclosure** (shown in onboarding before the user is sent to the notification-access
settings; `onboarding_disclosure_play` on a Play install):

> PebbleNTN reads notifications from supported navigation apps, extracts upcoming turn information,
> and sends it to your Pebble watch. Every supported app is enabled by default when installed; you
> can turn any of them off in Navigation apps. Processing occurs on this device. Notification content
> is not uploaded automatically. Diagnostic files are shared only when you explicitly export them.
> PebbleNTN spots new versions offline, by comparing the phone app with your watchapp, and Google
> Play delivers the updates. PebbleNTN makes no network connections of its own.

## 3. Store listing (Grow users > Store presence > Main store listing)

| Field | Value |
|---|---|
| App name | `fastlane/metadata/android/en-US/title.txt` (PebbleNTN, 30 characters max) |
| Short description | `short_description.txt` (80 max) |
| Full description | `full_description.txt` (4,000 max) |
| App icon | `images/icon.png` (512×512) |
| Feature graphic | `images/featureGraphic.png` (1024×500) |
| Phone screenshots | `images/phoneScreenshots/1.png`–`6.png` (1080×2160) |
| App category | Maps & Navigation |
| Tags | Navigation, Smartwatch, Maps (pick the closest Play offers) |
| Email | pebblentn@mg.mijnesite.be |
| Website | https://github.com/optio/PebbleNTN |
| Privacy policy | as in section 2 |

The listing files are the same ones F-Droid reads, so both stores show the same images. To
refresh them for a new version:

1. Phone screens: render the current app's screens (a Play install with a connected Pebble Time 2;
   dashboard, Watch settings, the colour list with its preview, Navigation apps) into
   `tools/play-store-sources/phone/`:

   ```bash
   PEBBLENTN_STORE_SCREENSHOTS_DIR=$PWD/tools/play-store-sources/phone \
     android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.StoreScreenshotsTest'
   ```

   A device screenshot (`adb exec-out screencap -p`) can be dropped in that folder instead; the
   script crops or pads it to 1080×2160 without cutting content.
2. Watch screens: `./scripts/build-watchapp.sh`, then, with the pebble tool's Python,
   `watchapp/tools/capture_screenshots.py --store basalt chalk emery` writes
   `tools/play-store-sources/watch/` (no emulator wipe; the settings are set by `SETTINGS`
   messages).
3. `tools/play_store_graphics.py`, with a Python that has Pillow (the pebble tool's:
   `head -1 "$(command -v pebble)"`), writes the phone screenshots (the phone screens first, then
   two watch images) and the feature graphic; `--check` validates the sizes and is part of
   `scripts/test-all.sh`.

Any image can be replaced by hand as long as `--check` passes.

When the listing is live, replace "Coming soon on Google Play" in the README's
[Distribution](../README.md#distribution) section with the listing link.

## 4. Releasing a version

The `release` workflow publishes every version on GitHub first (tag `vX.Y.Z`, APK, PBW, and the
release notes for F-Droid in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`). To
send that version to Play:

1. *GitHub > Actions > play-release > Run workflow*, with the tag (for example `v0.0.78`). It checks
   the target SDK against Play's floor (`verifyReleaseTargetSdk`, fails below 36), builds the AAB
   signed with the upload key, and attaches it to the run as the `release-aab` artifact with its
   `.sha256`. Nothing is uploaded to Play and no AAB is added to the GitHub release.
2. Download `release-aab` from the run and check it: `sha256sum -c app-release.aab.sha256`.
3. In Play Console, *Test and release > (Internal / Closed testing or Production) > Create new
   release*, upload `app-release.aab`.
4. Release notes: paste `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` of that tag
   under `<en-US>`. Play allows 500 characters; the release workflow already caps the file to that.
5. Review and roll out. Google's review can take from hours to a few days; the app tells Play users
   that a new version can take a day or two to reach Google Play.

## 5. Before the first production release

- **Closed test.** A personal developer account created after 13 November 2023 must run a closed
  test with at least **12 testers who stay opted in for 14 consecutive days** before it can apply
  for production access. Create a closed testing track, add the testers' Google accounts (an email
  list or a Google Group), share the opt-in link, release the AAB to that track, and keep it running
  for the 14 days. Then apply for production access in the Dashboard and answer Google's questions
  about the test.
- **Android developer verification.** Google is rolling out developer verification for apps
  installed on certified Android devices, including apps installed outside Play. Complete the
  identity verification for the developer account, and register the package `com.pebblentn.app`
  with the **release signing key** (the app signing certificate's SHA-256 from section 1). The Play
  listing is covered through Play Console; the registration also keeps GitHub, F-Droid and Obtainium
  installs installing where the requirement applies. Check the current rollout dates and countries
  on developer.android.com.
- **Phone check on Android 16.** Target SDK 36 changes some platform behaviour; run the checks in
  [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) on an Android 16 phone before the first upload.
