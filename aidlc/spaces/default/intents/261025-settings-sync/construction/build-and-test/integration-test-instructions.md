# Integration Test Instructions — Settings sync (#102)

The integration surface is the new protocol 1.3 exchange between the phone and the watch: `WATCH_READY` with settings, and `SETTINGS` in both directions.

## Emulator (done in Code Generation, basalt)

Settings messages were injected with the Pebble tool, and the watch's replies were read from its logs.

- A menu edit on the watch sends `SETTINGS` with the next revision.
- A newer phone `SETTINGS` (Red, invert off) changes the screen, and the watch confirms with the phone's revision (97).
- A stale phone revision (60) is ignored, and the watch replies with its own settings.
- The launch `WATCH_READY` carries protocol 1.3, revision, platform, capabilities and all 23 settings.
- Vibration "Off" with the phone's vibrate flag gives no buzz; "Short pulse" gives one.

## On a real phone and watch (manual, after release)

1. Open PebbleNTN on the watch. In the app, Dashboard → Watch → Watch settings shows the watch's current values. Only rows the watch supports appear: backlight colour and touch rows only on a Pebble Time 2, and only Black and White accents on black-and-white watches.
2. With the watchapp open, change the accent in the app. The watch redraws in the new colour within a second, and the app shows the value as confirmed.
3. Change a setting in the watch's own menu. The app shows the new value.
4. Close the watchapp, change a setting in the app. The app says "Will apply when the watchapp next opens", and the watchapp does **not** start. Open the watchapp: the change applies.
5. Turn Bluetooth off, change a setting on the watch, turn Bluetooth on (watchapp still open). The app picks up the change (reconnect resend, FR2c, not checked on the emulator).
6. Option lists for the visual settings show the preview of the highlighted value, in the watch's shape, and in greyscale on aplite/diorite.
7. A fresh install (or settings never changed) has vibration Off, the early warning at 25 m on every update, Touch On, Up and the side swipes toggling the ETA, and Down and Tap turning the backlight on. Settings → About is the first row.
8. Compatibility: the v0.0.76 watchapp with this app shows "Update the watchapp to change these here" and navigation works; this watchapp with an older app keeps navigating.
