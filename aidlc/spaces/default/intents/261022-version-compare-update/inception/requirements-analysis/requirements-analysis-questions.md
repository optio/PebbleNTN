# Requirements Analysis Questions — version-compare update detection

Context: your request settles the core of this work.
- **Watch side:** when the watchapp's own version is higher than the connected phone app's version (key 11, which arrives on every phone message), treat it as "update available" and reuse the #94 notice, idle text and `Update: {version}` subtitle.
- **Version comparison:** numeric, part by part (0.0.9 < 0.0.10). A version that doesn't parse is ignored.
- **F-Droid:** if the phone app was installed from F-Droid, show a warning that F-Droid may take about a day to make the update available.
- **Update card:** it links to the install source, says "Update from {source}" instead of the current "Download latest version", and lets the user pick another source.

The code scan found one limit. The phone learns the watchapp's version only from the watch's start-up message (WATCH_READY). With the Core Devices Pebble app, that message never reaches the phone app. The questions below cover what your request leaves open.

## Q1. [Product] How should the phone side learn the watchapp's version?

Context: the watch side always works. The phone-side card for "watch is newer" can only work when the watch's start-up message reaches the phone, which is true with the original Pebble app but not with the Core Devices app.

A. Best effort: read the watch's version from its start-up message when it arrives and remember it, so the card can appear; with the Core Devices app only the watch shows the update
B. Watch only: don't change the phone for this; the watch shows the update, and the phone card keeps relying on the internet check
C. Best effort as in A, and also add a "watch is newer" reminder on the watch telling the user to open the phone app's update card
X. Other (please specify)

[Answer]: A

Discussion before the answer:
- The person asked whether the companion app could detect the newer watchapp. PebbleKit 2 exposes only the watch firmware version and the running app's ID, name and type, not the watchapp version. The Core Devices app does not forward watch messages.
- The person asked whether it could be added to the protocol. A new key would be blocked in the same direction (watch → phone).
- A "version check message" that the watch rejects (NACK) when it is newer was proposed and then withdrawn: the Pebble SDK sends ACK and NACK by itself ("The AppMessage subsystem takes care of this implicitly"), so a watchapp cannot choose to reject a message.
- The person then chose A (best effort).

## Q2. [Product] Which sources should the update card offer, and what does each open?

Context: today the card always opens the GitHub "latest release" page. Install-source detection uses Android's installer information. Anything not recognised (a browser download, adb, an unknown store) counts as GitHub.

A. Three sources:
   - GitHub (opens the latest release page);
   - F-Droid (opens the PebbleNTN page in the F-Droid app, or the F-Droid website if the app isn't installed);
   - Obtainium (opens the Obtainium app, or its website if it isn't installed).
B. As A, but without Obtainium (only GitHub and F-Droid)
C. As A, plus the project's README "Install" section as a fallback page
X. Other (please specify)

[Answer]: A

## Q3. [Product] How should the user pick another source?

A. A second, smaller button on the card, "Other sources", that opens a short list. The pick is used once and the detected source stays the default
B. The same button and list, but the pick is remembered and becomes the default from then on (with a way back to "detected")
C. A setting in the app's settings screen ("Update source: detected / GitHub / F-Droid / Obtainium") instead of a button on the card
X. Other (please specify)

[Answer]: A

## Q4. [Product] Where should the F-Droid delay warning appear, and what should it say?

A. In the update card's text, only when the source is F-Droid: "F-Droid can take about a day to publish a new version. If it isn't there yet, try again tomorrow or pick another source."
B. As A, but shorter: "F-Droid may take about a day to offer this update."
C. As A, and also on the watch notice and idle text when the phone app came from F-Droid (needs a new protocol field)
X. Other (please specify)

[Answer]: B
