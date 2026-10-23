# Requirements Analysis Questions — Touch gestures on the navigation screen (#12)

Context:
- **Already done since the issue was written:** touch browsing in the settings menus shipped with #53 (REQ-WATCH-021). That part of #12 is dropped.
- **Corrected without a question:**
  - The issue says the firmware maps swipe left to BACK. The emulator tests for #53 show swipe right is BACK and swipe left/tap are SELECT. Either way, the navigation screen keeps the system bridge off, so no gesture there can close the app.
  - "Toggle the RGB background colour" matches no setting: the background is black or white (Invert). The RGB setting is the backlight colour; see Q4.
  - "Switch ETA / IN" now clashes with the "ETA / IN" label style (#13). The action is renamed "ETA: arrival time / time left".
- **Facts that shape the questions:**
  - Today the navigation screen ignores touch completely on the Pebble Time 2 (the bridge is off, nothing else listens).
  - The firmware has tap and swipe recognizers; double tap and long press don't exist and would be built in the app (timers on raw touch events).
  - The navigation screen uses only SELECT. UP, DOWN and the long presses are free on every watch.
  - "Vibration off" isn't one switch any more: the New instruction pattern, the phone's maneuver pulse (when the pattern is Off) and the early warning each vibrate on their own.

## Q1. [Product] Which gestures should be configurable?

A. Four swipes and a single tap (5 rows): the firmware recognizes these, and they react instantly
B. All seven from the issue: four swipes, tap, double tap and long press (double tap and long press built in the app; a mapped double tap delays single taps by 300 ms)
C. Only the four swipes (taps are the easiest to trigger by accident)
X. Other (please specify)

[Answer]: A

## Q2. [Product] What should touch do out of the box?

A. As the issue: Touch on, swipe left and right switch the ETA, everything else None
B. Touch off until the user turns it on in Settings → Touch
C. Touch on, swipes left/right switch the ETA, and tap opens Settings
X. Other (please specify)

[Answer]: B

## Q3. [Product] What should the "Vibration on / off" action switch?

A. Mute everything PebbleNTN vibrates for (New instruction, the phone's maneuver pulse, early warning), kept across launches; switching it back on restores the previous settings untouched
B. Only the New instruction pattern (Off ↔ the last pattern), as the issue says
C. Drop this action
X. Other (please specify)

[Answer]: B

## Q4. [Product] Which actions should be added to the issue's list?

The issue's list stays: None, ETA: arrival time / time left, Next colour, Previous colour, Invert colours, Swap arrow side, Next glyph pack, Vibration on / off, Early warning next / previous distance, Backlight now, Units, Open settings.

A. Add "Next backlight colour" (the Pebble Time 2 has the RGB backlight) and "ETA label style" (ETA at / ETA in ↔ ETA / IN)
B. Add only "Next backlight colour"
C. Add nothing
X. Other (please specify)

[Answer]: A

## Q5. [Product] Should the UP and DOWN buttons get the same choice of actions?

They're unused on the navigation screen and would bring these shortcuts to every watch, not only the Pebble Time 2.

A. No, touch only for now; I'll open a separate issue for the buttons
B. Yes, add "Up button" and "Down button" rows (default None) in a "Shortcuts" menu shown on every watch, with the touch rows only on the Pebble Time 2
X. Other (please specify)

[Answer]: B
