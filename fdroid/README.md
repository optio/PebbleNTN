# Publishing PebbleNTN on F-Droid

This folder holds the F-Droid build recipe for the **Android companion app**
(`com.pebblentn.app`). The Pebble watchapp (`.pbw`) is not an Android app and is
not distributed through F-Droid.

## Eligibility

- **License:** MIT (FOSS).
- **No proprietary dependencies:** all libraries are AndroidX / Compose / Room /
  kotlinx / Timber, plus Rebble's PebbleKit Android 2, which is **vendored from
  source** under `android/pebblekit/` (Apache-2.0) rather than pulled from JitPack —
  so the whole app builds from source with no non-reproducible binary dependency.
- **No binary blobs:** the app embeds no `.pbw`, `.so`, `.jar` or `.aar`. The rule
  data shipped in the APK is generated from source by the Gradle `syncRuleData` task.
- **Builds from source, unsigned:** `./gradlew :app:assembleRelease` (with no release
  keystore in the environment) produces `app-release-unsigned.apk`, which F-Droid
  signs with its own key.
- **Release tags:** each release is tagged `vX.Y.Z`, so `UpdateCheckMode: Tags` +
  `AutoUpdateMode: Version v%v` work.
- **Permissions:** notification listener (core function) + INTERNET (an optional,
  off-by-default weekly version check against GitHub that sends no personal data).

## Files

- `metadata/com.pebblentn.app.yml` — the fdroiddata build recipe.
- `../fastlane/metadata/android/en-US/` — store listing: title, short and full
  descriptions, per-version changelogs, and phone screenshots. F-Droid reads these
  from the built tag.

## Manual steps

The build recipe and store metadata are prepared in-repo; submission to F-Droid is
done through GitLab:

1. Sign in to GitLab and fork **https://gitlab.com/fdroid/fdroiddata**.
2. Create a branch named `com.pebblentn.app`.
3. Copy `fdroid/metadata/com.pebblentn.app.yml` to `metadata/com.pebblentn.app.yml`
   in the fork.
4. From the `fdroiddata` checkout, validate and test-build:
   ```
   fdroid lint com.pebblentn.app
   fdroid build -v -l com.pebblentn.app
   ```
5. Commit (label the merge request **"New App"**) and push to the fork.
6. Open a merge request to `fdroiddata` and respond to reviewer feedback.

If the submitter is not the app's author, the author must be notified and not oppose
inclusion; otherwise open an issue on the source repository requesting inclusion.

After the merge request is merged it is roughly 24–48 h until the app appears in the
main repository, because F-Droid signs with an offline key.

## Keeping it updated

`AutoUpdateMode: Version v%v` makes F-Droid detect each new `vX.Y.Z` tag automatically.
Per release, add a `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
(versionCode = `major*10000 + minor*100 + patch`, e.g. `0.0.19` → `19`).

Reference: https://f-droid.org/docs/Inclusion_How-To/
