# Publishing PebbleNTN on F-Droid

This folder holds the F-Droid build recipe for the **Android companion app**
(`com.pebblentn.app`). The Pebble watchapp (`.pbw`) is not an Android app and is
not distributed through F-Droid.

## Status — what's already done in this repo

- **License:** MIT (FOSS) — eligible. ✅
- **No proprietary dependencies bundled:** all libraries are AndroidX / Compose /
  Room / kotlinx / Timber, plus Rebble's FOSS **PebbleKit 2** (see the caveat below).
- **No binary blobs:** the app embeds no `.pbw`, `.so`, `.jar` or `.aar`. The rule
  data shipped in the APK is generated from source by the Gradle `syncRuleData` task.
- **Builds from source, unsigned:** `./gradlew :app:assembleRelease` (with no release
  keystore in the environment) produces `app-release-unsigned.apk` — this is exactly
  what F-Droid builds and then signs with its own key.
- **Release tags exist:** each release is tagged `vX.Y.Z` (e.g. `v0.0.19`), so
  `UpdateCheckMode: Tags` + `AutoUpdateMode: Version v%v` work.
- **Store metadata (fastlane):** `fastlane/metadata/android/en-US/` — title, short and
  full descriptions, and per-version changelogs. F-Droid reads these from the built tag.
- **Recipe:** `metadata/com.pebblentn.app.yml` (in this folder) — copy this into an
  `fdroiddata` fork.

## ⚠️ The one likely review blocker: PebbleKit from JitPack

`io.rebble.pebblekit2:client` is resolved from **JitPack** (see
`android/settings.gradle.kts`). JitPack builds artifacts on demand and is **not
reproducible**, which F-Droid discourages and reviewers often reject. PebbleKit 2 is
itself MIT-licensed (Rebble), so the fix is a *sourcing* change, not a licensing one.
Options, best first:

1. **Ask Rebble to publish PebbleKit 2 to Maven Central** — then just switch the
   repository in `settings.gradle.kts` and the problem disappears.
2. **Vendor PebbleKit 2 into this repo** as a local Gradle module (it's MIT; include
   its LICENSE). Reproducible and fully under our control.
3. Attempt the build as-is and let the F-Droid reviewer decide — likely to bounce.

Recommend doing (1) or (2) before opening the merge request.

## ⚠️ Screenshots are still needed

F-Droid wants **2+ phone screenshots of the Android app**. The images under
`watchapp/store/` are the *Pebble watch* graphics, not the phone UI, so they don't
count. Add real screenshots of the companion app to:

```
fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
fastlane/metadata/android/en-US/images/phoneScreenshots/2.png
```

(An optional 512×512 `images/icon.png` and `images/featureGraphic.png` can be added
too; if omitted, F-Droid takes the icon from the APK.)

## Steps only you can do (external accounts)

You are the app author, so F-Droid's "author does not oppose inclusion" is satisfied.

1. Create/sign in to a **GitLab** account.
2. Fork **https://gitlab.com/fdroid/fdroiddata**.
3. Create a branch named `com.pebblentn.app`.
4. Copy `fdroid/metadata/com.pebblentn.app.yml` from this repo to
   `metadata/com.pebblentn.app.yml` in the fork.
5. From the `fdroiddata` checkout, validate and test-build:
   ```
   fdroid lint com.pebblentn.app
   fdroid readmeta
   fdroid build -v -l com.pebblentn.app     # needs the fdroidserver toolchain
   ```
6. Commit (label the MR **"New App"**) and push to your fork.
7. Open a **merge request** to `fdroiddata`, and respond to the reviewer.

After the MR is merged it's ~24–48 h until the app appears in the main repo (F-Droid
signs with an offline key).

## Keeping it updated

`AutoUpdateMode: Version v%v` means F-Droid auto-detects each new `vX.Y.Z` tag — no
further action per release. Just keep adding a
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for each release
(versionCode = `major*10000 + minor*100 + patch`, e.g. `0.0.19` → `19`).

Reference: https://f-droid.org/docs/Inclusion_How-To/
