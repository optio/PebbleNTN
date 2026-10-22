# Integration Test Instructions — Watchapp version in the dashboard status

## Applicability

Not generated: the test strategy is Minimal.

## Manual Check on a Phone (the purpose of this work)

1. Install the build that has the new row, and make sure the watchapp was installed through your Pebble companion app.
2. Open the PebbleNTN phone app. The "Watchapp version" row is under "Watch". On a fresh install it reads "Not received yet".
3. Open PebbleNTN on the watch from the launcher, and keep the phone dashboard visible.
4. Check the row:
   - **It changes to "0.0.xx (last seen)":** the companion app delivers watch→phone messages, and the phone-side version check works.
   - **It stays "Not received yet":** the companion app does not deliver them. That's the case for the upstream issue.

Also check that the Status rows now sit closer together and that nothing is cut off at a large font size.
