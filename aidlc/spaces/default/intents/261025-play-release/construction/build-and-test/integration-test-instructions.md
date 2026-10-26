# Integration Test Instructions — Prepare the first Google Play release (#114)

## Done in Code Generation

- **Signed bundle:** `bundleRelease`, signed through the four upload-key environment variables with a temporary key, gives an AAB that `jarsigner` verifies. The key was deleted afterwards.
- **QR:** `connect_qr.png` and the QR in the basalt emulator screenshots (connection hint and About) decode to `https://github.com/optio/PebbleNTN#distribution`, version 3-L.
- **Store images:** rendered from the current app as a Play install, and captured from the current watchapp. `--check` passes.

## On the Maintainer's Android 16 Phone (before the first Play upload)

Install the build over wireless adb with `adb install -i com.android.vending app-debug.apk`, so the app sees a Play install. A different signing key needs the current app uninstalled first.

1. Onboarding shows the disclosure without the GitHub sentence.
2. The dashboard has no "Check for updates" row, no weekly toggle and no check prompt. The bottom of the screen clears the navigation bar, with both 3-button and gesture navigation.
3. When the watchapp is newer than the phone app, the card reads "Update from Google Play", shows the delay line and opens the Play page.
4. In the rule editor, back with unsaved changes shows the discard dialog (predictive back, target 36).
5. A navigation session reaches the watch through the Pebble companion app.
6. Install as GitHub instead (plain `adb install`): the update controls are back, as in v0.0.77.

## On Play (internal testing track)

Repeat 1–3 with the app installed from the internal track. That is the only place the installer is really `com.android.vending`.
