# Requirements Analysis Questions — Prepare the first Google Play release (#114)

Context:
- **Decided already:**
  - One app for every source: it detects a Play install at runtime (installer `com.android.vending`), with no separate Play build.
  - App signing on Play uses the existing release key.
  - Commits and the merge carry `[skip ci]`, so nothing is released by this work.
- **Corrected from the scan, without a question:**
  - Online rule updates are not implemented, so the privacy docs, Data safety answers and listing describe only what exists today: the optional GitHub update check and user-initiated links (feedback, store pages, email drafts).
  - The listing sentence "More apps can be added without a new release" is removed or reworded.
  - A Play install also ignores the GitHub update data saved by an earlier GitHub install, not only hides the buttons.
  - The onboarding disclosure gets a Play variant without the GitHub-check sentence.
  - The target-SDK gate fails the Play workflow for real, and the release workflow runs it too.

## Q1. [Product] On a Play install, what happens when the watchapp is newer than the phone app?

The watchapp updates itself from the Pebble store, and Play can lag behind by a review (hours to days).

A. Keep the notice; its button opens the PebbleNTN page on Google Play, with a line that a new version can take a day or two to reach Play
B. Keep the notice without a button: "Update PebbleNTN from Google Play"
C. Hide the notice on Play installs
X. Other (please specify)

[Answer]: A

## Q2. [Release] Which key signs the bundle uploaded to Play?

A. A new, separate upload key in its own `PLAY_UPLOAD_*` secrets for the `play-release` workflow; the release key stays only in the GitHub release workflow
B. Keep using the release key as the upload key (today's `UPLOAD_KEYSTORE_BASE64`)
X. Other (please specify)

[Answer]: A

## Q3. [Release] How does the bundle get to Play?

A. The `play-release` workflow (started by hand for a tag) builds the signed AAB and attaches it to the workflow run; you download it and upload it in Play Console
B. As A, and the workflow also uploads it to the internal testing track with a Play service account (needs a service-account secret)
C. Attach the AAB to the public GitHub release too
X. Other (please specify)

[Answer]: A

## Q4. [Product] Where does the watch's "Install phone app" QR code point?

A. Leave it on the GitHub releases page; no watch change now
B. Point it to the README's install section, which lists every source (Google Play first once it's live; "Coming soon on Google Play" until then). The QR image is regenerated and must still fit on aplite
X. Other (please specify)

[Answer]: B

## Q5. [Product] How are the Play graphics made?

A. A script in the repo: crops the phone screenshots to 1080×2160, composes the watch screenshots (scaled up) into extra phone-size images, and draws the 1024×500 feature graphic from the icon, name and a watch screenshot. You can replace any of them later
B. Only crop the phone screenshots; you design the feature graphic and any watch images yourself
X. Other (please specify)

[Answer]: A

## Q6. [Product] What contact goes in the privacy policy (it has `[MAINTAINER EMAIL / URL]` placeholders) and the Play listing?

A. `pebblentn@mg.mijnesite.be` (the address the app already uses for feedback emails) and the GitHub issues page
B. Only the GitHub issues page
X. Other (please specify)

[Answer]: A
