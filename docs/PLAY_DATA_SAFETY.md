# Google Play Data safety — declared answers

The answers to enter in Play Console > App content > Data safety, matching
[`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) and the code (spec/600-security-release/BuildRelease.md:
"Complete Data Safety accurately"). Re-check them whenever the app's network use, permissions or
sharing change. [`PLAY_CONSOLE.md`](PLAY_CONSOLE.md) lists the rest of the Play Console steps.

## Why the answer is "no data collected"

Play counts data as **collected** when the app sends it off the device, and as **shared** when it
goes to a third party. PebbleNTN does neither:

- **Notification processing is on-device.** Turn instructions go to the watch through the Pebble
  companion app over local IPC and Bluetooth, not to a server.
- **The Play build makes no network requests of its own.** The same app runs everywhere and detects
  a Google Play install at runtime (installer `com.android.vending`). On a Play install the GitHub
  update check never runs, on start, weekly or on demand, and its controls are not shown
  (REQ-ANDROID-022). On other installs the check is optional, sends no user data, and only reads the
  latest release tag from GitHub.
- **No analytics, crash reporting, advertising or account.** There is no third-party SDK that
  sends data.
- **Everything else is user-initiated.** A diagnostic file goes through the Android share sheet or
  an email draft the user sends from their own email app; feedback links open in the browser. Play
  does not count data the user chooses to send through another app as collected by this app.

## Form answers

### Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

With "No", the form skips the data-type, encryption-in-transit and deletion questions. If Play still
asks them: encryption in transit "Not applicable, no data is transmitted"; data deletion "Users
delete all data on the device (Debug history > Delete all, clearing the app's data, or uninstalling)".

### Data types (for the record)

| Data type | Collected | Shared | Notes |
|---|---|---|---|
| App activity / other actions (turn information from navigation notifications) | No | No | Processed and kept on the device only; notification identifiers stored as hashes; the user deletes the debug history at any time. |
| App info and performance (diagnostics) | No | No | Only in a file the user reviews and sends; never sent automatically. |
| Personal info (email address) | No | No | The feedback email is a draft the user sends from their own email app. |
| Location | No | No | The app has no location permission; it reads only the text of navigation notifications. |
| Contacts, messages, photos, files | No | No | Not read. |
| Device or other IDs | No | No | None recorded or sent. |

## Permissions and what they are for

| Permission | Use |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` (notification access, granted by the user) | Core function: read the enabled navigation apps' notifications to show the next turn on the watch. |
| `INTERNET` | Only the optional GitHub update check on installs that are not from Google Play. Never used on a Play install, which makes no network requests of its own. Granted at install, with no runtime prompt. |
| `<queries>` for the catalog navigation apps and Obtainium | Detect which supported apps are installed, without `QUERY_ALL_PACKAGES`. |

`POST_NOTIFICATIONS` is used only by the debug `fixture-publisher` module, which is not uploaded.

## Notification access declaration

Declare `BIND_NOTIFICATION_LISTENER_SERVICE` as **core functionality**: PebbleNTN reads the
notifications of the navigation apps the user keeps enabled and turns them into turn-by-turn
directions on a Pebble watch; the app has no other purpose. Notifications from other apps are
ignored before their content is read. The prominent disclosure (`onboarding_disclosure`, and
`onboarding_disclosure_play` on a Play install) is shown in onboarding before the user is sent to the
notification-access settings. The justification text and the demo video are in
[`PLAY_CONSOLE.md`](PLAY_CONSOLE.md).
