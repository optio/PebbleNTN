# Requirements Analysis Questions: Watch settings help text (#139)

Context: today each row of the phone's Watch settings screen shows the setting's name, with its current value as the second line, and a chevron. Tapping a row opens the list of values, which has no description. The only explanations are the intro at the top of the screen, a note under the Directions card ("Compact shows only the street name when the app gives one."), and the paragraph under the When driving card. All strings are English only.

## Q1. [Designer] Where should each setting's help text appear?

The row's second line already shows the current value, so the help needs its own place.

A. Under every row, always visible: a short grey line below the current value (one or two sentences per setting).
B. Only on the value list: a short paragraph at the top of the screen that opens when you tap the setting. The main screen stays as compact as it is today.
C. Both: one short line under each row, and a fuller explanation at the top of the value list.
D. An info (ⓘ) icon on each row that opens a small dialog with the help.
X. Other (please specify)

[Answer]: C (Both: a short line under each row, plus a fuller explanation at the top of the value list)

## Q2. [Product owner] Should values be explained too, where their names aren't enough?

Some value names are hard to understand on their own: the shortcut actions ("ETA label style", "Next glyph pack", "Open settings"), the backlight levels, and the vibration patterns.

A. No: one help text per setting is enough.
B. Yes, only for the shortcut actions: a short description beside each action in the value list.
C. Yes, for every value whose name isn't self-explanatory (shortcut actions, backlight levels, early-warning repeat, and others): a short description beside the value in the list.
X. Other (please specify)

[Answer]: B (Yes, only for the shortcut actions)

## Q3. [Product owner] What happens to the two explanations that exist today?

The Directions note sits under its card as a section note, and When driving has a long paragraph under its card.

A. Turn the Directions note into that row's help text, the same as every other setting. Keep the When driving paragraph as it is, since it explains the whole section.
B. Keep both exactly as they are, and add the new per-setting help alongside them.
C. Turn both into per-setting help text (Directions text, Hide directions when driving, Driving speed) and remove the section notes.
X. Other (please specify)

[Answer]: C (Turn both into per-setting help text and remove the section notes)

## Q4. [Developer] Should the specs, docs and store screenshot be updated in the same change?

The requirement and UI spec files (`requirements/REQ-ANDROID.md`, `spec/400-ui/AndroidUI.md`) are hash-checked through `MANIFEST.sha256.json`. The Play Store screenshot of this screen (`tools/play-store-sources/phone/2-watch-settings.png`) will look different once help text is added.

A. Update the requirement, the UI spec and the manifest. Leave the store screenshot for a later store refresh.
B. Update the requirement, the UI spec, the manifest, and regenerate the store screenshot too.
C. Code and strings only. No spec or screenshot changes.
X. Other (please specify)

[Answer]: B (Update the requirement, the UI spec, the manifest, and regenerate the store screenshot)

## Q5. [Designer] Where does the fuller When driving explanation go?

Follow-up to Q1 (short line under each row plus a fuller explanation on the value list) and Q3 (both section notes become per-setting help). The two When driving rows, the "Hide directions when driving" switch and the "Driving speed" slider, are changed in place and have no value list. So the fuller text has no screen to sit on. Today's paragraph explains how the speed guess works and that fast cycling or slow traffic can fool it.

A. Each driving row gets its short line. The fuller explanation sits behind a "More" or "Learn more" link under the switch row that expands in place.
B. Each driving row gets its short line, and the switch row's line is longer (two or three sentences) so it carries the important part of today's paragraph. No expandable text.
C. Each driving row gets its short line, and the full paragraph stays as a section note under the When driving card after all. This is an exception to Q3.
X. Other (please specify)

[Answer]: C (Short line under each driving row; the full paragraph stays as the When driving section note, an exception to Q3)
