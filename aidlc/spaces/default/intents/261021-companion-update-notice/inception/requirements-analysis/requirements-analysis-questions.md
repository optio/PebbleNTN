# Requirements Analysis Questions — #94

Context: the issue and its three comments already settle the design.
- **Protocol:** a `PHONE_UPDATE_AVAILABLE` flag bit (bit 4), a `LATEST_PHONE_VERSION` key, and a protocol minor bump.
- **Watch:** a notice when the app starts, closing after about 10 s or on any button. The launcher subtitle (app glance) shows the update text below a live route countdown and above "Navigate on phone".
- **Phone:** when the last check was more than 30 days ago, or never ran, "Check for updates" appears as the first dashboard item. After that check, the phone asks once "Check for updates automatically every week?" (Turn on / Not now).

The phone already shows an "Update available" card with a download button whenever it knows of a newer version, so the issue's "the mobile should also clearly indicate there is an update" already holds. The questions below cover only what the issue leaves open.

## Q1. [Product] How often should the watch notice appear while an update is pending?

Context: the issue says "when the watchapp starts". Some users can't update straight away. For example, F-Droid publishes builds days after GitHub. The launcher subtitle keeps showing the update either way.

A. Every time the watchapp starts, until the phone is updated (as the issue says)
B. Once for each new version: after it has been shown, only the launcher subtitle reminds the user, until a newer version comes out
C. At most once a day
X. Other (please specify)

[Answer]: X. Other: "Once per version but if the user manually starts the watchapp an it shows the No navigation detected message then it should show it every time in that screeen"

## Q2. [Product] What should happen when the watchapp starts during an active route?

Context: with auto-launch on, the phone often opens the watchapp just as navigation begins. A full-screen notice would then cover the first turn for up to 10 seconds.

A. Show it anyway; it closes after 10 s or on any button
B. Don't show it during a route. Show it at the next start with no route active
C. Wait while the route is active, then show it once the route ends (still in the app)
X. Other (please specify)

[Answer]: C

## Q3. [Product] What text should the launcher subtitle show?

Context: the issue asks for `Update Companion App to version 0.0.70` (38 characters) and suggests the fallback `Update phone app to 0.0.70` (26 characters) if it gets cut off. The current hint, "Navigate on phone", allows about 18 characters, so the long text will probably be cut off on 144-px-wide screens.

A. Use the issue's full text. Check it in the emulator on basalt, chalk and emery, and switch to the short text only if it gets cut off
B. Use the short text `Update phone app to {version}` from the start
C. Shorter still: `Phone app {version} ready`
X. Other (please specify)

[Answer]: B

## Q4. [Product] What should the watch notice say?

Context: the issue suggests "open the mobile app to find the update". The notice is a full-screen text window, styled like the About screen.

A. Title "Phone app update", then: "PebbleNTN {latest} is available (you have {current}). Open PebbleNTN on your phone to update."
B. Shorter: "Phone app {latest} available. Open it on your phone to update."
C. The issue's wording: "A phone app update is available. Open the mobile app to find the update."
X. Other (please specify)

[Answer]: C

## Q5. [Product] Follow-up to Q1: how should the "No navigation detected" screen show the update?

Context: from Q1, the full-screen notice appears once per new version. When the watchapp shows "No navigation detected. If navigating, open PebbleNTN on your phone.", that screen should mention the update every time. The message is one large centred block, so long text can overflow on the small screens (144 px wide, and the round chalk).

A. Replace the idle text while an update is pending: "No navigation detected. Phone app update available: open the mobile app."
B. Keep the idle text as it is and add a second, smaller line below it: "Phone app update available"
C. Keep the idle text and append the issue's notice sentence after it, checking it fits on every platform
X. Other (please specify)

[Answer]: A
