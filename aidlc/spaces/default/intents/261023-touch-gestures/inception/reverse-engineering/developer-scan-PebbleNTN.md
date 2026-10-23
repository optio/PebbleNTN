## Developer Code Scan Results

Repo `PebbleNTN` at 3727ddc8ab3c1dd531940aced5cfcd6e0be98de1 (v0.0.75). Intent `261023-touch-gestures` (GitHub issue #12). Focused scan, depth Minimal, lead-only: this developer link also wrote the nine-artifact candidate in `aidlc/spaces/default/intents/261023-touch-gestures/.aidlc-engine/codekb-stage-PebbleNTN/`. The full findings are in that candidate's `code-quality-assessment.md` ("Touch gestures findings"), and the surfaces are in its `api-documentation.md`.

### Scan Coverage
- **Analyzed deeply**:
  - watchapp/src/c/main.c
  - watchapp/src/c/theme.c
  - watchapp/src/c/theme.h
  - watchapp/src/c/settings_window.c
  - watchapp/src/c/settings_window.h
  - watchapp/src/c/early_warning.c
  - watchapp/src/c/early_warning.h
  - watchapp/src/c/update_notice_window.c
  - watchapp/src/c/update_notice_window.h
  - watchapp/src/c/about_window.c
  - watchapp/src/c/about_window.h
  - watchapp/src/c/stops_text.c
  - watchapp/src/c/stops_text.h
  - watchapp/tests/test_stops_text.c
  - watchapp/tools/touch_emulator.py
  - watchapp/tools/capture_screenshots.py
  - watchapp/package.json
  - watchapp/wscript
  - requirements/REQ-WATCH.md
- **Skimmed only**:
  - watchapp/src/c/ (the other pure-unit headers: about_text, eta_text, glance_text, panel_layout, phone_version, update_notice, version_compare; their `.c` files not opened; `generated/protocol.h` not opened)
  - watchapp/tests/ (file list and assertion style; test_early_warning.c head only)
  - watchapp/tools/ (gen_maneuver_bitmaps.py, gen_connect_qr.py, gen_store_icon.py by name only)
  - scripts/test-watchapp-unit.sh (outside the snapshot; read only to confirm how host tests build)
  - Issue #12 body and edit dates (GitHub), and git history of the watchapp since 2026-08-09, including `theme.c` and `settings_window.c` at 50c4c3f
- **Left out**:
  - watchapp/build/ (Pebble build output, gitignored)
  - watchapp/.lock-waf_linux_build (gitignored)
  - watchapp/tools/__pycache__/ (Python cache, gitignored)
  - watchapp/resources/, watchapp/store/ (binary assets; outside the snapshot)
  - the AI-DLC install and `aidlc/` workspace

### Packages Found
- watchapp — Pebble watchapp (C) — C11 / Pebble SDK 3 — navigation renderer, on-watch settings, About, update notice
- watchapp/tools — maintainer scripts — Python 3 (stdlib) — asset generation, screenshot capture, touch emulation over VNC

### Build System
- **Type**: Pebble SDK waf (`pebble build`); host tests with gcc
- **Config Files**: watchapp/package.json (targetPlatforms aplite, basalt, chalk, diorite, emery; sdkVersion "3"; 79 media resources, 39 emery-only; `companionApp` com.pebblentn.app), watchapp/wscript (compiles `src/c/**/*.c` per platform)
- **Build Dependencies**: main.c -> every pure unit, theme, settings_window, update_notice_window; settings_window -> theme, about_window; theme -> early_warning (enums)

### APIs Discovered
- Pebble AppMessage — main.c `inbox_received_handler` / `send_ready` — 4 inbound events handled, 1 outbound (unchanged; see api-documentation.md)
- Watch settings API — theme.h — 15 persisted settings (keys 1-15), 15 setters/getters, 11 name helpers, 2 vibration previews
- Watch input — main.c navigation window — 1 button (SELECT); touch bridge opt-in plus opt-out on that window (emery); no recognizers

### Frameworks & Libraries
- Pebble C SDK — sdkVersion 3 (installed SDK unpinned; issue #12 cites 4.33.1) — Window, Layer, MenuLayer, ScrollLayer, AppTimer, persist, light, vibes, AppGlance, touch bridge
- Python standard library — 3.x — touch_emulator.py RFB client, capture_screenshots.py PNG reader

### Test Coverage
- **Test Directories**: watchapp/tests/ (9 `test_<unit>.c`, one per pure unit)
- **Test Frameworks**: plain C `main` with a failure counter, built by scripts/test-watchapp-unit.sh (`gcc -std=c11 -Wall -Wextra -Werror`, test + unit only), run by scripts/test-all.sh (not in CI); emulator checks through watchapp/tools
- **Coverage Config**: absent

### Code Quality Indicators
- **Linting**: none for C beyond `-Werror` in host tests
- **CI/CD**: watchapp.yml builds the PBW only
- **Documentation**: rationale header on every C module, REQ and issue IDs in comments; REQ-WATCH.md has acceptance criteria per requirement

### Technical Debt Signals
- main.c is 1,261 lines and mixes decoding, rendering, timers, input and glance (watchapp/src/c/main.c)
- the settings row order is kept twice by hand (settings_window.c `ROW_*` and capture_screenshots.py)
- tuple types are not validated in `render_navigation` / `handle_stopped` (main.c:845-860, 994-998)
- the touch guards use `PBL_PLATFORM_EMERY` rather than a capability check (main.c:1176, 1192)

## Handoff Summary
- **Intent-relevant finding**: the navigation window (main.c:1108-1110, 1186-1197) subscribes only SELECT, and on emery disables the touch bridge with no recognizer attached, after `app_touch_navigation_enable(true)` in `init()` (main.c:1180). The two preconditions #12 found for gestures are already met; what is missing is recognizers, the gesture-to-action logic, a short change notice and the Touch settings. Applying an action is `settings_set_*` then `on_settings_changed()` (main.c:1091-1097), the same path the menu uses.
- **Risks / follow-up**: resolve which swipe direction the firmware maps to BACK (the issue and the code disagree); keep About as the last menu row and keep capture_screenshots.py in sync; the new pure unit cannot include theme.h; touch screenshots need the no-wipe touch_emulator.py flow.

### Facts relevant to #12
- **#53 already shipped settings touch browsing** (af8fb66, 2026-10-10, REQ-WATCH-021). The issue's "Settings browsing by touch" section is done; REQ-WATCH-021 defers navigation-screen gestures to #12 and says the menus have no touch switch of their own.
- **Swipe direction conflict**: the issue says swipe left = BACK and swipe right = SELECT, while main.c:1193, REQ-WATCH-021's acceptance and touch_emulator.py `check` say swipe right = BACK.
- **New settings since 2026-08-09**: ETA label style (#13, key 11, "ETA at / ETA in" vs "ETA / IN") and the early warning (keys 12-15: distance Off/10/25/50/100/200 m, repeat Once/Every update, pattern without Off, strength). Correction to the brief: **backlight colour (key 10) and vibration strength (key 9) already existed** when the issue was opened (50c4c3f). Also new: About (REQ-WATCH-020), accent previews (#81), transit, glance, update notice.
- **"Switch ETA / IN" naming**: "ETA / IN" is now the label of the compact ETA *label style*, while the mode values are "Arrival time" / "Time to arrival" and the default strip reads "ETA at" / "ETA in". The "→" glyph in the notice has not been checked against the system fonts.
- **"Toggle the RGB background colour"** matches no setting: the background is black or white (Invert) plus the accent panel. The RGB setting is the backlight colour (emery only), which the action list omits.
- **"Vibration off" is not a single switch**: pattern Off still lets the phone's maneuver-change pulse play (main.c:896-904), and the early warning has its own Off.
- **Navigation-screen buttons**: SELECT opens Settings; UP, DOWN and long presses are unused; BACK is the default exit. There are no window appear/disappear handlers and no small overlay or toast layer (only the full-screen message layer and the update notice window).
- **Persist keys in use**: 1-15 (theme.c), 16-18 (main.c); next free is **19**. Touch on/off plus 7 gestures needs 19-26, plus one more if a mute must survive relaunches.
- **Menu**: 10 rows, the same on every platform (Backlight, Vibration, Colour, Invert, ETA display, ETA label, Arrow side, Glyph pack, Distance units, About); About must stay last; capture_screenshots.py copies `ROW_*` for basalt, chalk and emery. `touch_service_is_enabled()` is not used anywhere yet.
- **B/W accent cycling** must go through `accent_for_row` / `row_for_accent` (`ACCENT_AVAILABLE_COUNT` 2): `settings_set_accent` accepts all 16 ids.
- **REQ-WATCH numbering**: 001-023 exist (012 listed after 013); the newest is 023, so the next ID is **REQ-WATCH-024**.
- **Platforms**: aplite, basalt, chalk, diorite, emery; no flint or gabbro anywhere; emery is the only touch target.
- **Tooling**: touch_emulator.py already provides `tap`/`swipe` over VNC (stdlib only, no vncdotool) and never wipes; capture_screenshots.py wipes, which turns emulator touch navigation off.
- **Host tests**: each `test_<unit>.c` is linked with its unit alone, under `-Werror`, outside CI; a `touch_actions.c` needs its own ids and takes counts as parameters.
