# Requirements Analysis Questions — Sync watchapp settings with the companion app (#102)

Context:
- **Decided in the issue:**
  - the sync works in both directions, and the watch is the source of truth;
  - a revision number 0–99 with wrap-around comparison, where the watch wins ties and the ambiguous case;
  - the app only offers what the watch supports;
  - older apps on either side keep working;
  - static emulator previews per visual value, with basalt shown in greyscale for black-and-white watches.
- **Corrected from the scan, without a question:**
  - The watch sends its settings in its real READY at launch, after each edit on the watch while connected, and again when the phone reconnects. It doesn't send them only "on every connect", because READY goes out once per launch today.
  - The app shows the settings screen only once it has received the watch's real settings. A protocol version isn't enough, because the phone sends READY messages of its own.
  - Phone edits are queued while the watchapp is closed and sent on its next READY. Sending earlier would relaunch the watchapp (REQ-WATCH-006).
  - The watch applies only values that changed, checks each one against what this watch supports, does not bump its own revision for a phone edit, and does not echo it back as a new edit.
  - Protocol 1.3 adds a `SETTINGS` event and new keys from 15 upward.

## Q1. [Product] Ship it all at once, or in two steps?

A. One release: the sync and the previews together
B. Two releases: first the sync with a text-only settings screen, then the previews
X. Other (please specify)

[Answer]: A

## Q2. [Product] Should the Shortcuts settings (Up/Down buttons, Touch, swipe and tap actions) sync too?

A. Yes, all 23 settings sync, and the app shows a Shortcuts section (touch rows only for a Pebble Time 2)
B. No, only the 16 appearance, backlight and vibration settings sync; Shortcuts stay on the watch
X. Other (please specify)

[Answer]: A

## Q3. [Product] What happens to the phone's fixed "watch settings" (vibrate on maneuver change, backlight on update, return to watchface)?

They have no screen and only set message flags. The watch already uses them only while its own vibration and backlight are on their defaults.

A. Leave them as they are; out of scope
B. Remove the vibrate and backlight flags. The watch's own settings become the only ones, and a watch with vibration Off then no longer gets the phone's pulse, which changes today's default behaviour
C. Rename instead: keep the flags; the watch's New instruction "Off" becomes "Short pulse (default)" (the phone's pulse, as today) and a new, truly silent "Off" is added
X. Other (please specify)

[Answer]: C

## Q4. [Product] Where should the previews show?

A. One large preview at the top of the Watch settings screen, showing the visual setting you last changed, with a thumbnail per row in the accent colour list
B. In each option list: the preview of the highlighted value above the list, with thumbnails for the accent colours
C. Both: the large preview on the main screen, and each option list shows its value's preview
X. Other (please specify)

[Answer]: B

## Q5. [Product] What if the sync code doesn't fit in aplite's memory (about 3.9 KB free)?

A. Leave aplite out of the sync: the app says "not available on this watch", and the watch behaves as today
B. It must fit: trim elsewhere on aplite if needed (for example smaller message buffers)
X. Other (please specify)

[Answer]: B
