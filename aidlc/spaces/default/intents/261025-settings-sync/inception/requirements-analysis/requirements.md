# Requirements — Sync watchapp settings with the companion app (#102)

## Sources

- Initial description: [desc] "Implement GitHub issue #102: sync the watchapp settings with the companion app in both directions, with static emulator screenshot previews of each visual setting value in the app (greyscale basalt images for black-and-white watches)."
- Workflow-selected scope: [scope] express
- GitHub issue #102, as groomed on 2026-10-24 and 2026-10-25
- The code knowledge base (focused scan at 4779760): `aidlc/spaces/default/codekb/PebbleNTN/`, and the handoff `inception/reverse-engineering/developer-scan-PebbleNTN.md` ("Facts relevant to #102")
- Answers Q1–Q5 in `requirements-analysis-questions.md`

## Intent Analysis

- **Request type:** a new feature across the protocol, the watchapp and the Android app. Brownfield.
- **Shape:**
  - one release, with the sync and the previews together [Q1];
  - all 23 watch settings sync, Shortcuts included [Q2];
  - the watch's New instruction "Off" becomes an honest "Short pulse (default)", and a real silent "Off" is added [Q3];
  - previews show inside each option list [Q4];
  - it must fit on aplite [Q5].
- **Corrections to the issue, from the scan:**
  - The watch announces itself once per launch, so the watch also sends its settings on a reconnect and after edits made on the watch.
  - The phone makes up READY messages of its own, so the settings screen waits for the watch's real settings, not a version number.
  - A send to a closed watchapp relaunches it, so phone edits wait for the next READY.
  - Several values need extra checks: the bools, the accents on black-and-white watches, and the RGB-only action.
- **Requirements depth:** Minimal in form, but the feature is large.

## Functional Requirements

### FR1 — Protocol 1.3: settings in both directions

- Bump the protocol to **1.3** (a minor bump; old peers ignore the new keys). Add:
  - an event **`SETTINGS`** (id 4, which is free);
  - one key per synced setting, numbered from 15 upward, carrying the watch's enum indices, with bools as 0/1;
  - a `SETTINGS_REVISION` key (0–99);
  - a `WATCH_PLATFORM` key and a `WATCH_CAPABILITIES` bitmask: colour, round, RGB backlight, touch.
- The 23 synced settings:
  - accent, units, invert, glyph pack, arrow side, ETA mode, ETA label;
  - backlight duration and colour;
  - New instruction pattern and strength;
  - early-warning distance, repeat, pattern and strength;
  - Touch;
  - the 7 shortcut slots.
- The watch-internal keys 16–18 and 27 are not synced.
- Keep both protocol definition files consistent (`protocol/protocol-definition.json` and `examples/protocol-definition.json`). Regenerate the constants with `scripts/generate-protocol.sh`. Update `spec/200-architecture/Protocol.md`: a minor version may add an event.
- **Pass/fail:**
  - `protocol/check_consistency.py --check` passes;
  - a 1.2 watchapp with a 1.3 phone app, and a 1.3 watchapp with a 1.2 phone app, both keep navigating as today (unit tests for each side's decoder with and without the new keys).

### FR2 — When the watch sends its settings

The watch sends all its settings, its revision, platform and capabilities:
- **(a)** in its real `WATCH_READY` at launch;
- **(b)** as a `SETTINGS` message after every edit on the watch while the phone is connected. Edits come from a menu, the Shortcuts door, Touch, or a shortcut action;
- **(c)** again when the phone reconnects (connection service).

Each edit on the watch bumps the watch revision, which wraps 99 → 0 and is persisted at key 28.

**Pass/fail:** a host-tested encoder produces every key, and the emulator shows the phone receiving a change made on the watch.

### FR3 — Applying phone edits on the watch

- When the watch receives `SETTINGS` from the phone, it compares revisions with the wrap-around rule:
  - `a` is newer than `b` when `(a − b + 100) % 100` is in 1..49;
  - 0 means the same revision;
  - 50, and an equal revision with different values, mean **the watch wins**.
- **When the phone is newer**, the watch:
  - validates each value: in range, and supported by this watch. Accents on black-and-white watches are only Black or White; the RGB-only actions and backlight colour need an RGB backlight; Touch and the gesture slots need touch. Invalid values are ignored;
  - writes only values that changed;
  - adopts the phone's revision;
  - redraws, and refreshes an open settings menu or closes it back to its root;
  - replies with its stored settings as confirmation.
- Applying a phone edit doesn't count as a watch edit: no revision bump, and no echo beyond the one confirmation.
- **When the watch is newer or wins**, it replies with its stored settings, which the phone then adopts.
- **Pass/fail:** host tests of the comparison, including the 99 → 0 rollover, 50, and the equal-revision case, plus the validation; in the emulator, a phone edit changes the watch screen.

### FR4 — The phone mirror and the offline queue

- The phone stores a mirror of the watch's last confirmed settings, revision, platform and capabilities, and keeps it across restarts.
- **A phone edit:**
  - updates the mirror;
  - bumps the phone revision;
  - is sent as `SETTINGS` **only while the watchapp is open**, meaning a real READY was received in this session and the app hasn't closed.
- **Otherwise the edit is queued.** The screen says "Will apply when the watchapp next opens", and the queue is sent on the next real READY.
- Incoming watch settings are adopted by the same wrap-around rule. When they win, they replace queued edits for the values they carry.
- **Pass/fail:**
  - JVM tests of the Kotlin comparison (the same cases as FR3), the mirror merge, and the queue (an edit while closed is sent once on the next READY, with no relaunch);
  - the transport must never send settings to a closed watchapp.

### FR5 — The "Watch settings" screen

- A **Watch settings** entry in the dashboard's Watch section opens a screen whose groups mirror the on-watch menu:
  - Appearance: colour, invert, glyph pack, arrow side;
  - ETA display and ETA label;
  - distance units;
  - Backlight: duration, and colour on RGB watches only;
  - Vibration: New instruction pattern and strength, and the early warning with its distance, repeat, pattern and strength;
  - Shortcuts: Up and Down buttons; on touch watches, Touch and, while Touch is on, the 4 swipes and Tap [Q2].
- Each row shows its current value and opens an option list with the current value selected.
- The screen offers only what the reported capabilities allow.
- **Before the first real settings arrive**, the screen shows "Open PebbleNTN on your watch to load its settings". An older watchapp never sends them; the screen then says "Update the watchapp to change these here".
- **Strings and accessibility:** every string is an Android resource, and every control has an accessibility label (AGENTS rules 16–17).
- **Pass/fail:** Compose UI tests for capability gating (no backlight colour without RGB, no touch rows without touch, only Black and White accents on black-and-white watches) and for the waiting state.

### FR6 — Previews inside the option lists [Q4]

- Every option list of a **visual** setting shows, above the list, the preview image of the highlighted value. The visual settings are colour, invert, glyph pack, arrow side, ETA display, ETA label and units.
- The colour list also shows a thumbnail per row.
- Non-visual settings show no image.
- **Image set:** the image comes from the set for the reported platform:
  - basalt 144×168, chalk 180×180, emery 200×228;
  - basalt before any platform is known;
  - **for aplite and diorite, basalt in greyscale** (a saturation-0 colour filter).
- Each image's content description names the value, for example "Preview: light on dark".
- **Pass/fail:** a JVM test that maps every visual (setting, value, platform) to a drawable that exists, referenced statically so release builds keep it.

### FR7 — Generating and checking the previews

- `watchapp/tools/capture_screenshots.py` gets a preview mode. It captures one image per value of each visual setting, with every other setting at its default, for basalt, chalk and emery. That includes all 16 accents; basalt also needs Black and White for the greyscale case.
- **Output and capture conditions:**
  - images are written as optimised PNGs into `android/app/src/main/res/drawable-nodpi/`, named `watch_preview_<setting>_<value>_<platform>.png`, which are valid resource names;
  - the sample instruction and clock are fixed, so reruns give the same images.
- **Completeness check:** a JVM test fails when a visual value has no image, or an image has no value. It runs in CI.
- **Pass/fail:** the mode regenerates the full set, and the check passes.

### FR8 — Honest vibration "Off" [Q3]

- The watch's New instruction pattern keeps stored value 0, now labelled **"Short pulse (default)"**: the phone's single buzz on a turn change, as today.
- A new value **"Off"** (stored value 5) is truly silent: the watch ignores the phone's vibrate flag.
- The phone keeps sending its fixed flags unchanged.
- **The Shortcut "Vibration on / off" action:**
  - any pattern other than Off switches to Off;
  - Off switches to the last pattern, which may be Short pulse;
  - the last pattern defaults to Short pulse.
- Older stored values keep their meaning.
- **Pass/fail:**
  - host tests for the toggle and the pattern names;
  - on the emulator, Off gives no vibration with the phone flag set, and Short pulse gives the phone's buzz.

### FR9 — Specs and docs

- Add REQ-WATCH-025 (settings sync on the watch) and REQ-ANDROID-021 (the Watch settings screen, the sync and the previews).
- Amend REQ-WATCH-012/014/015/016/022/024: remove "SHALL NOT change the protocol (or the phone app)" and point to the new requirements. Amend REQ-WATCH-016 for FR8.
- Update REQ-ANDROID-009 (the phone's own watch flags), `Protocol.md` and `Pebble.md`, and resync `MANIFEST.sha256.json`. Add an entry to `docs/IMPLEMENTATION_STATUS.md`.

## Non-Functional Requirements

- **aplite memory [Q5]:** the watch-side sync SHALL fit on aplite. Measure `heap_bytes_free()` with Settings and a submenu open, and make it no worse than v0.0.76 minus 500 bytes.
  - Design it table-driven: one table of key, validator, getter and setter, plus one encoder and one decoder. Do not write per-setting code.
  - Size the AppMessage buffers to the largest real message instead of the platform maximum, if needed.
- **Reliability:** use the watch outbox one message at a time, with sent/failed handlers and one retry. A message that can't be sent stays pending until the next connect.
- **Flash:** sync writes only changed values.
- **Testability:** pure units on both sides, for the revision comparison, validation, encoding and decoding, and preview mapping. Keep the existing suites green.
- **Compatibility:** no change in behaviour for users who never open the screen, except FR8's relabelled default, which works the same.

## Constraints

- Generated protocol files are never edited by hand. MANIFEST-protected files are resynced in the same change.
- Never send settings to a closed watchapp (launch policy, REQ-WATCH-006).
- No Android emulator in WSL. Pebble emulators are fine.

## Assumptions

- About 30 preview images per platform, at a few KB each, is an acceptable APK increase (roughly 100–160 KB).
- A phone and a watch never drift 50 or more edits apart between syncs, as the issue says.

## Out of Scope

- A "preview buzz" button (optional in the issue).
- Removing or exposing the phone's fixed flags (Q3 chose the rename).
- Live rendering of combined previews.

## Open Questions

None.
