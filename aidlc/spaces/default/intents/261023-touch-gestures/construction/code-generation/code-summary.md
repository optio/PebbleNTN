# Code Summary — Shortcuts: touch gestures and UP/DOWN buttons (#12)

## Files

**Created**
- `watchapp/src/c/shortcuts.h`, `watchapp/src/c/shortcuts.c`: the pure unit (slots, 15 actions and their labels, defaults, action list with or without an RGB backlight, cycling, vibration toggle, stored-value check, notice texts).
- `watchapp/src/c/shortcut_apply.h`, `watchapp/src/c/shortcut_apply.c`: runs an action through the existing setters; `SHORTCUT_RGB_BACKLIGHT` follows `PBL_RGB_BACKLIGHT`, the Backlight colour menu's condition.
- `watchapp/tests/test_shortcuts.c`: host test, 47 assertions.
- `watchapp/src/c/qr_code.h`, `watchapp/src/c/qr_code.c`: draws the install QR from its module bitmap (not in the plan, see Deviations).

**Modified**
- `watchapp/src/c/theme.h`, `watchapp/src/c/theme.c`: persist keys 19 (Touch), 20–26 (one action per slot), 27 (last New instruction pattern that wasn't Off); getters and setters; `settings_set_vibe_pattern` records the last pattern.
- `watchapp/src/c/main.c`: UP/DOWN clicks, emery swipe and tap recognizers, `run_shortcut`, the 1.5 s banner; plus the QR drawing and the release of cached bitmaps while covered (Deviations).
- `watchapp/src/c/settings_window.c`: Shortcuts row (row 9, About now row 10), the shortcuts door, the shared action list; plus `menu_create` with `static const` callback tables (Deviations).
- `watchapp/src/c/about_window.c`: the QR drawn through `qr_code_draw` (Deviations).
- `watchapp/resources/images/connect_qr.png`, `watchapp/package.json`, `watchapp/tools/gen_connect_qr.py`: the QR resource is now 29x29, one pixel per module, `1Bit` (Deviations).
- `watchapp/tools/capture_screenshots.py`: `ROW_SHORTCUTS = 9`, `ROW_ABOUT = 10`, the shortcuts door and action list captured.
- `requirements/REQ-WATCH.md`: REQ-WATCH-024 added; REQ-WATCH-021 points to it. `MANIFEST.sha256.json` resynced.
- `docs/IMPLEMENTATION_STATUS.md`: the #12 entry, including what replaces the issue's original assumptions.

## Key implementation decisions

- Notice texts follow the requirement table: the ETA action shows "Arrival time" / "Time left" (the menu row keeps "Time to arrival"), the arrow "Arrow left" / "Arrow right". `shortcut_notice` treats a value of "Off" (as `vibe_pattern_name` and `early_warn_distance_label` spell it) as the off form.
- Turning Vibration on plays `settings_vibe_play()` as the preview. The early-warning actions do not play a preview: the requirement asks for it only on the vibration action.
- On a watch without an RGB backlight, a stored Next backlight colour action does nothing and shows no notice. It can't be picked there, because the list leaves it out.
- The banner is a `TextLayer` in the road area's colours swapped, so it contrasts with what it covers on every theme, black-and-white included. It is GOTHIC_18_BOLD (GOTHIC_24_BOLD on emery), sized to its text, inset 4 px (28 px on round, where it is at least 32 px tall to cover the road's single line).
- Action list rows are laid out by hand (as the glyph-pack rows are): long labels such as "Early warning: previous distance" wrap onto a second line instead of being cut, with "Selected" under the current one.
- The main row summary uses " / " ("Up: None / Down: None"), as the Vibration row does, rather than a middle dot.
- The tap and swipe recognizers are attached without `recognizer_set_fail_after`. With it, taps never fired in the emulator (see Tests not executed); without it, taps work. A real swipe also triggering Tap is a hardware check.

## Test coverage

- **Host:** `scripts/test-watchapp-unit.sh` runs 10 suites; `test_shortcuts` has 47 assertions covering FR1–FR4 per the unit-test instructions: defaults; stored values (in range, -1, `ACTION_COUNT`, 255); cycling (+1 in the middle, wrap at both ends, a Black/White pair); vibration toggle (Double → Off, Off with last Triple, Off with nothing stored → Single); action list with and without an RGB backlight (count, round trip, Next backlight colour not offered); every notice form; the three no-notice actions and out of range; truncation in an 8-byte buffer; a label for every action.
- **Build:** `pebble build` for aplite, basalt, chalk, diorite and emery with no new warnings. aplite reports 3940 bytes free at load (was 7430). The run-time heap was measured on the emulator (see Deviations).
- **Emulator:** screenshots are in `watchapp/build/shots/`. Banner timing was measured with screenshots taken 0.2 s and about 2.9 s after the input:
  - **basalt:**
    - defaults: UP/DOWN change nothing (0 differing pixels, `basalt-01-nav-default.png` vs `basalt-02-after-up-down-defaults.png`);
    - the Shortcuts row and the two-row door (`basalt-03-settings-shortcuts-row.png`, `basalt-04-shortcuts-door.png`, `basalt-07-door-up-invert.png`);
    - Up = Invert: the road area turns black with the banner (`basalt-41-final-up-invert-banner.png`), and the banner is gone 2.9 s later (`basalt-42-final-banner-cleared.png`);
    - a second press 0.9 s after the first keeps the banner past the first timer (`basalt-34-restart-check.png`).
  - **aplite:** the two-row door (`aplite-52-door.png`), the action list (`aplite-53-action-list.png`), About (`aplite-54-about.png`), and the hint's QR redrawn on return (`aplite-55-back-on-hint.png`), all without a fault.
  - **emery** (no wipe, VNC):
    - Touch Off shows no gesture rows (`emery-07-door-touch-off.png`); Touch On shows the five rows (`emery-08-door-touch-on.png`, `emery-09-door-tap-row.png`);
    - Tap = Next glyph pack (`emery-11-door-tap-glyph.png`): with Touch On, a tap shows "Bold glyphs" and switches the glyphs (`emery-41-touch-on-tap-banner.png`, `emery-44-touch-on-tap-banner.png`, `emery-45-touch-on-tap-cleared.png`);
    - with Touch Off, a tap and a swipe change nothing (0 differing pixels, `emery-47/48/49-*.png`);
    - after a swipe right the app is still open (`emery-43-swipe-right-app-open.png`).
  - **chalk:** the door, the action list and the banner stay inside the bezel (`chalk-02-door.png`, `chalk-03-action-list.png`, `chalk-07-up-banner.png`, `chalk-08-cleared.png`).
  - All emulators were closed afterwards.
- **Full suite:** `scripts/validate-spec-assets.sh` OK (34 files). `scripts/test-all.sh` printed "All tests finished." (spec assets, protocol check, version sync, watchapp host tests, rule validation and regression, route-capture tests, and 429 Android JVM tests plus lint).

## Tests not executed

- **The four swipe directions on emery.** The emery emulator delivers a VNC swipe as a single tap: no touch movement reaches the watch. Every swipe profile tried (2 to 20 steps, 8 to 50 ms apart) logged only a tap completion, and the system's own swipe-right-to-back in Settings did not react either. So "swipe left toggles the ETA" could not be shown in the emulator. The swipe handler's direction-to-slot mapping is only exercised on hardware. To check on a Pebble Time 2:
  - each swipe runs its own action;
  - a swipe does not also run the Tap action;
  - touch is ignored when touch navigation is off in the watch's settings.
- **The aplite navigation screen.** aplite never receives AppMessages in the emulator. This is unchanged from HEAD (checked with a HEAD build in a throwaway worktree), so the aplite checks ran on the connection-hint screen.
- **Persistence across a relaunch.** It is not checked separately. Each reinstall in the runs above kept Up = Invert, Touch On and Tap = Next glyph pack, which shows the values survive an app restart.

## Deviations from the plan

1. **aplite memory: three changes outside the planned files.** The first build faulted on aplite. The feature adds about 3.8 KB of code to aplite's app RAM. With the 116x116 install QR preloaded (1796 bytes), only 136 bytes of heap were left, and opening Settings faulted. Shortcuts > action list also faulted without the QR. The NFR says the feature SHALL fit aplite's heap, so these behaviour-neutral changes were made:
   - **QR from its modules.**
     - `gen_connect_qr.py` now emits the 29x29 module bitmap (`MODULE_PX = 1`); it was regenerated with the tool.
     - `package.json` loads it as `1Bit`, and the new `qr_code.c` draws each dark module as a 4x4 square, for both the hint (`main.c`) and About (`about_window.c`).
     - Rendering is pixel-identical: 0 differing pixels for the hint and About on basalt and chalk against screenshots taken before the change.
     - Each copy now takes 168 bytes of heap instead of 1796.
   - **Callback tables.** `settings_window.c`'s eight menu load functions (six on aplite) now pass a `static const MenuLayerCallbacks` table to one `menu_create` helper, which saves about 750 bytes of code.
   - **Release while covered.** The navigation window gets a `disappear` handler that frees the cached maneuver glyphs and the QR; the next draw reloads them, the same path as a glyph-pack change. The QR is loaded on first draw instead of at `init`.

   Measured with temporary heap logs, since removed: the Settings > Shortcuts > action list path keeps about 600 bytes and Settings > About about 800, close to the previous release's margin on its About path. aplite's heap remains tight. Each maneuver glyph cached during a route costs about 400 bytes, and those are now released whenever Settings opens.
2. **Recognizers without `recognizer_set_fail_after`.** This is a tuning choice, not a plan change, explained under Key implementation decisions.
3. **Emulator check (plan Step 10).**
   - The planned "Touch On: swipe left toggles the ETA" could not be shown on the emulator, as explained under Tests not executed.
   - The emery tap check used Tap = Next glyph pack as planned. FR5's alternative example (Tap = Invert) was covered by the basalt UP = Invert check.
4. **Scratch tooling.** The emulator checks used scratch scripts in `watchapp/build/`, which is git-ignored (`emu_drive.py`, `fast_shot.py`, `png_diff.py`, `px.py`). `fast_shot.py` sends the input and the screenshot over one libpebble2 connection, since separate `pebble` calls take more than 2 s and miss the 1.5 s banner. They are not part of the change.
