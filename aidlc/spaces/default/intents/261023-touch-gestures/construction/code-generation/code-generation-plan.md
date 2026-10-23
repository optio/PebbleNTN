# Code Generation Plan — Shortcuts: touch gestures and UP/DOWN buttons (#12)

## Summary

- **Builds:**
  - a Settings → Shortcuts submenu: Up/Down button rows on every watch, and Touch plus five gesture rows on the Pebble Time 2;
  - 16 actions;
  - a 1.5 s notice banner;
  - UP/DOWN and swipe/tap handling on the navigation screen.
- **Touches:**
  - new `watchapp/src/c/shortcuts.c/.h` (pure) and `watchapp/src/c/shortcut_apply.c/.h` (Pebble side);
  - `theme.c/.h`, `main.c`, `settings_window.c`;
  - `watchapp/tools/capture_screenshots.py`;
  - `requirements/REQ-WATCH.md`, `MANIFEST.sha256.json`, `docs/IMPLEMENTATION_STATUS.md`.
- **Tests:** a new host test `watchapp/tests/test_shortcuts.c`, then the emulator check with `touch_emulator.py`.

## Context

Zero-Unit express work from `inception/requirements-analysis/requirements.md` (FR1–FR6, NFRs). The code knowledge base is at `aidlc/spaces/default/codekb/PebbleNTN/`. Only the watchapp changes; there is no protocol or phone-app change.

### Design decisions

**Pure unit `shortcuts.c/.h`** (no `pebble.h`, so the host test links it alone):
- **Types:**
  - `ShortcutSlot`: `SLOT_UP_BUTTON, SLOT_DOWN_BUTTON, SLOT_SWIPE_LEFT, SLOT_SWIPE_RIGHT, SLOT_SWIPE_UP, SLOT_SWIPE_DOWN, SLOT_TAP, SLOT_COUNT`.
  - `ShortcutAction`, with stored values fixed once released:
    `ACTION_NONE, ACTION_ETA_MODE, ACTION_ETA_LABEL, ACTION_NEXT_ACCENT, ACTION_PREV_ACCENT, ACTION_INVERT, ACTION_ARROW_SIDE, ACTION_NEXT_GLYPH, ACTION_VIBRATION, ACTION_EARLY_WARN_NEXT, ACTION_EARLY_WARN_PREV, ACTION_BACKLIGHT_NOW, ACTION_NEXT_BACKLIGHT_COLOUR, ACTION_UNITS, ACTION_OPEN_SETTINGS, ACTION_COUNT`.
- **Functions:**
  - `shortcut_default(slot)`: Swipe left and right give `ACTION_ETA_MODE`, every other slot `ACTION_NONE` (FR2).
  - `shortcut_action_name(action)` gives the menu label (for example "ETA: arrival time / time left").
  - `shortcut_offered_count(rgb_backlight)`, `shortcut_action_for_row(row, rgb_backlight)` and `shortcut_row_for_action(action, rgb_backlight)` drop `ACTION_NEXT_BACKLIGHT_COLOUR` on watches without an RGB backlight (FR3).
  - `shortcut_cycle(current, count, step)` wraps both ways, with `step` +1 or −1 (FR3).
  - `shortcut_vibe_toggle(current, last, off, fallback)`: a non-Off pattern gives `off`; Off gives `last` when it isn't Off, else `fallback` (FR3, Q3).
  - `shortcut_valid_action(int stored)` sanitizes a stored byte to `ACTION_NONE` when it is out of range.
  - `shortcut_notice(buf, size, action, value)` formats the notice: "Vibration off" or "Vibration: %s", "Early warning off" or "Early warning %s", "%s glyphs", "Arrow left"/"Arrow right", and the plain value for the rest. It returns false for None, Backlight now and Open settings, which show no notice (FR4).

**Settings storage (`theme.c/.h`):**
- New persist keys, all clear of keys 1–18 (NFR compatibility):
  - 19: Touch on/off (default false);
  - 20–26: one byte per slot (missing → `shortcut_default`, out of range → `ACTION_NONE`);
  - 27: the last non-Off New instruction pattern.
- New getters and setters: `settings_touch_enabled()` / `settings_set_touch_enabled()`, and `settings_shortcut(slot)` / `settings_set_shortcut(slot, action)`.
- `settings_set_vibe_pattern` also records the last non-Off pattern, and `settings_vibe_last_pattern()` returns it (Single when none is stored).

**Pebble side (`shortcut_apply.c/.h`):**
- `ShortcutResult shortcut_apply(ShortcutAction a, char *notice, size_t size)` runs the action through the existing setters, with the same save as the menu. It returns `{ show_notice, open_settings }`.
- **Colour cycling:** goes through `row_for_accent` / `accent_for_row` and the available accent count, so black-and-white watches cycle only Black and White.
- **Vibration:** turning it on plays `settings_vibe_play()` as a preview.
- **Backlight now:** `light_enable_interaction()`.
- **Early warning:** the notice distance uses the units-aware label the Vibration menu already uses.

**Navigation screen (`main.c`):**
- **Buttons:** the click config adds UP and DOWN single clicks, which run `settings_shortcut(SLOT_UP_BUTTON / SLOT_DOWN_BUTTON)`.
- **Touch (emery only, `#if defined(PBL_PLATFORM_EMERY)`):**
  - in `window_load`, attach `swipe_recognizer_create(cb, NULL, Up|Down|Left|Right)` and `tap_recognizer_create(cb, NULL)` (the window owns them);
  - on `RecognizerEvent_Completed`, map the direction or tap to a slot, and run it only while `settings_touch_enabled() && touch_service_is_enabled()` (FR5);
  - the bridge stays disabled.
- **Running an action:** a shared `run_shortcut(action)` calls `shortcut_apply`. Then:
  - "Open settings" calls the existing `select_click_handler` path;
  - otherwise it calls `on_settings_changed()` to redraw and reapply the backlight, and shows the banner when asked.
- **Banner:** a `TextLayer` over the navigation window, hidden by default. It is small, centred, in a theme colour and the system font, plain text. It shows for 1500 ms through one `AppTimer`; a new action replaces the text and reschedules the timer. It is destroyed in `window_unload`.

**Settings menu (`settings_window.c`):**
- A new main row `ROW_SHORTCUTS` just above About (About stays last). Its subtitle summarizes the button shortcuts, for example "Up: None · Down: None".
- **Shortcuts window:**
  - section "Buttons": Up button, Down button;
  - on emery while `touch_service_is_enabled()`, section "Touch": the Touch On/Off row, which toggles in place, and while On, Swipe left / right / up / down and Tap;
  - each gesture row's subtitle is its action name.
- **Action list window:** shared by every slot (the slot passed in). It lists `shortcut_offered_count(PBL_IF_RGB_BACKLIGHT)` rows with "Selected" on the current action; picking one saves it and pops the window.
- **Platform check:** the RGB-backlight check uses the same compile-time condition the Backlight colour menu already uses.

**Screenshots tool:** `capture_screenshots.py` `ROW_*` indices shift by one for About, and a Shortcuts capture is added.

**Requirement:** REQ-WATCH-024, "Shortcuts: buttons and touch gestures on the navigation screen". REQ-WATCH-021's "navigation-screen gestures are #12" line points to it.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "express",
  "test_strategy": "minimal",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "minimal",
    "strategy_volume": [
      "One verifiable test per requirement at the narrowest effective level.",
      "At least one happy-path unit test per component.",
      "Unit tests are the default; a bugfix/security scope floor may require an integration or E2E regression when that is the narrowest level that reproduces the defect."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:c8f84e35190e007d9863fad236154d0269ab9bf633fc2a3ff106ed965ba4a9b9",
  "contract_sha256": "sha256:bb76c1b22f8307be6e5780d50cabab891684449d5b164bd0e825e9e4e5c61e5b"
}
```

## Steps

There is no database, repository or API layer. The persisted settings are the data layer, and the watch UI is the frontend.

- [x] **Step 1: Project structure.** Create `watchapp/src/c/shortcuts.c/.h` and `watchapp/src/c/shortcut_apply.c/.h`, with the header comments and empty bodies.
- [x] **Step 2: Verify the test runner.** Run `scripts/test-watchapp-unit.sh` and confirm that the existing host tests pass.
- [x] **Step 3: Data model: implement.** Add persist keys 19–27 and the getters and setters in `theme.c/.h`, including the last non-Off pattern (FR1, FR2, FR3).
- [x] **Step 4: Data model: tests.** The defaults and the sanitizing of stored bytes go through `shortcut_default` and `shortcut_valid_action`, covered in Step 6. The persist calls themselves are checked in the emulator (Step 10).
- [x] **Step 5: Business logic: implement.** Write `shortcuts.c` as described in Context (FR2, FR3, FR4).
- [x] **Step 6: Business logic: tests.** Write `watchapp/tests/test_shortcuts.c` and run `scripts/test-watchapp-unit.sh`. The cases are listed in unit-test-instructions.md.
- [x] **Step 7: Frontend: implement.**
  - `shortcut_apply.c` (FR3).
  - `main.c`: the UP/DOWN clicks, the emery recognizers gated by Touch and `touch_service_is_enabled()`, `run_shortcut`, and the banner with its 1.5 s timer (FR4, FR5).
  - `settings_window.c`: the Shortcuts row, the Shortcuts window and the action list window (FR1).
- [x] **Step 8: Build.** Run `pebble build` in `watchapp/` for all five platforms, with no new warnings. Check that aplite still links and that the app fits.
- [x] **Step 9: Tools.** Update the `capture_screenshots.py` row indices, and add a Shortcuts capture (FR6).
- [x] **Step 10: Frontend: emulator check (FR1–FR5).**
  - **basalt:** UP with Up = Invert inverts the colours, and the banner shows and then clears.
  - **emery** (the no-wipe `touch_emulator.py` flow):
    - Touch Off: a swipe changes nothing;
    - Touch On: swipe left toggles the ETA with a notice, and a tap with Tap = Next glyph pack changes the glyphs;
    - a swipe right does not close the app.
  - Screenshots go in `watchapp/build/`.
  - Close the emulator afterwards.
- [x] **Step 11: Specification and docs.**
  - Add REQ-WATCH-024 and point REQ-WATCH-021 to it.
  - Resync `MANIFEST.sha256.json` and run `scripts/validate-spec-assets.sh`.
  - Add the `docs/IMPLEMENTATION_STATUS.md` entry (FR6).
  - Run the full `scripts/test-all.sh`.

## Traceability (plan step → requirement)

| Requirement | Steps |
|---|---|
| FR1 Shortcuts submenu | 3, 7, 10 |
| FR2 Defaults | 3, 5, 6, 10 |
| FR3 Actions | 3, 5, 6, 7, 10 |
| FR4 Change notice | 5, 6, 7, 10 |
| FR5 Input handling | 7, 10 |
| FR6 Specs and tools | 9, 11 |
| NFR testability | 5, 6 |
| NFR compatibility and memory | 3, 8 |
