# Integration Test Instructions — Shortcuts (#12)

There is no service integration. The phone and the watch exchange the same messages as before (no protocol change). The integration checks are on-watch.

## Emulator (done in Code Generation)

- basalt or chalk: set Settings → Shortcuts → Up button = Invert colours, go back, press UP. The road inverts, a banner shows, and it clears after about 1.5 s.
- emery: start the emulator with `--vnc` and never wipe it, then use `python3 watchapp/tools/touch_emulator.py tap X Y`. Set Touch On and Tap = Next glyph pack, then tap the navigation screen.

## On a Pebble Time 2 (manual, after release)

1. On a fresh install, touch and UP/DOWN do nothing.
2. Turn Settings → Shortcuts → Touch On. Swipe left and swipe right switch the ETA between arrival time and time left, each with a notice.
3. Set Swipe up, Swipe down and Tap to different actions. Each gesture runs only its own action, and a swipe never also runs Tap.
4. Turn touch navigation off in the watch's own settings. Gestures on the navigation screen do nothing, and the Touch rows are hidden.
5. A swipe right never closes the app.

## On any other watch

UP and DOWN run their configured actions. The Shortcuts menu shows only the two button rows.
