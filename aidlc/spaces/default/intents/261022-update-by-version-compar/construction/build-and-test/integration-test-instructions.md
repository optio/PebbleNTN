# Integration Test Instructions — Version comparison as the default update check

## Applicability

Not generated, because the test strategy is Minimal.

## Manual Check on a Phone

1. Install the new build. Its version will be lower than the watchapp's when the watchapp updates first. To get that case today, install an older phone APK next to the newer watchapp.
2. Tap "Check for updates now". The message reads "PebbleNTN {watch version} is available." even if GitHub reports nothing newer, and the update card shows the same version.
3. With both apps on the same version, the message reads "PebbleNTN is up to date."
4. Check that the onboarding disclosure and the weekly toggle hint show the new texts.
