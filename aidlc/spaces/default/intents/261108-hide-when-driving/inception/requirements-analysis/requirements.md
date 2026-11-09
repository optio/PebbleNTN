# Requirements: "When driving" (GitHub issue #132)

## Intent Analysis

**Source:** GitHub issue #132 "Walking Notifications Only, No Driving Distractions" and the agreed "Proposed design" comment on it [desc]; answers Q1–Q5 in `requirements-analysis-questions.md`; code scan findings in `aidlc/spaces/default/codekb/PebbleNTN/code-quality-assessment.md` ("Hide when driving findings").

**Goal:** people who use PebbleNTN mainly for walking and cycling do not want the watch to show directions and buzz while they drive, because the car and the phone already show them. The user can choose to hide the watchapp while driving. The default stays as today: directions are always shown.

**Key fact:** navigation apps do not say which travel mode is active (verified for Google Maps in driving, walking and cycling mode on the emulator). PebbleNTN therefore infers driving from speed, and says so to the user.

- **Type:** enhancement across the phone app, the watchapp and the shared protocol.
- **Scope:** multi-component (phone app: speed estimation, controller, settings UI, dashboard, export; watchapp: settings menu, settings sync; protocol).
- **Complexity:** standard. The rules are already agreed; the risk is in timing and in not reopening the watchapp by accident.
- **Depth:** Minimal (express plan), with requirements testable one by one.

## Functional Requirements

### Settings (synced between watchapp and phone app)

- **FR1** Three new synced watch settings are added and behave like the existing synced settings (editable on both devices, the latest change wins, persisted on the watch).
  - **FR1.1** *When driving*: two values, `Show directions` (default) and `Hide directions`.
  - **FR1.2** *Driving speed (metric)*: 10, 15, … 60 km/h (11 values), default 25 km/h.
  - **FR1.3** *Driving speed (imperial)*: 5, 10, … 40 mph (8 values), default 15 mph. [Q1]
  - **FR1.4** The speed that is shown and used is the one that matches the watch's Distance units setting. Changing Distance units switches to the other stored speed; neither value is converted or changed. [Q1]
  - **FR1.5** A freshly installed or reset watchapp uses the defaults, so behaviour is unchanged from today until the user picks "Hide directions".

### Watchapp settings menu

- **FR2** The main Settings list gets a row **When driving**, with its current value underneath: "Show directions", or "Hide above <speed> <unit>" (for example "Hide above 25 km/h").
  - **FR2.1** Selecting it opens a menu headed **When driving** with the row **Directions** (value Show or Hide; Select switches it).
  - **FR2.2** The row **Driving speed** (value "Above <speed> <unit>") is listed only when Directions is Hide.
  - **FR2.3** *Driving speed* opens a list whose first row is **Default (25 km/h)**, or **Default (15 mph)** with imperial units, followed by every speed of the active unit; the selected row is marked "Selected", like the other watch lists. Choosing "Default" resets that unit's speed.
  - **FR2.4** The About page carries a short line saying that "When driving" is guessed from the speed.

### Phone app settings

- **FR3** The Watch settings screen gets a section **When driving** with:
  - **FR3.1** a switch **Hide directions when driving** (off by default);
  - **FR3.2** **Driving speed** with the current value ("Above 25 km/h") and a slider over the active unit's range in steps of 5, greyed out while the switch is off;
  - **FR3.3** a **Reset to default (25 km/h / 15 mph)** button, shown only when the active unit's speed differs from its default;
  - **FR3.4** the explanation: "Navigation apps don't tell PebbleNTN whether you're driving, walking or cycling, so PebbleNTN guesses from your speed. Above the driving speed the watchapp closes; when you slow down for a while, it comes back. Fast cycling can count as driving, and slow traffic as walking. Adjust the speed if it guesses wrong."
  - **FR3.5** This section has no watch screenshot preview, unlike the other watch settings.
- **FR4** While the connected watchapp is older than this feature (it does not report the new protocol version), the phone app hides the When driving section entirely. [Q2]

### Speed estimation

- **FR5** For Google Maps navigation (including MicroG Maps, which uses the Google Maps rules), the phone estimates the travel speed from the navigation notification.
  - **FR5.1** *Measured speed*: from the remaining route distance (`progressMax − progress`, in metres) over time, smoothed over the last 30–60 s.
  - **FR5.2** *Planned speed*: remaining route distance divided by the time until the arrival time in the notification ("Arrive h:mm").
  - **FR5.3** Notifications that carry no usable distance (for example "Rerouting…" or a notification without progress values), and periods where the distance does not count down, are not used as speed samples.
  - **FR5.4** Navigation apps without these values do not get speed estimates: for them the feature never hides the watchapp (behaviour as today).

### Hide and show behaviour

The rules below apply only when When driving is `Hide directions`.

- **FR6** The watchapp still opens when navigation starts as it does today (auto-launch unchanged), so walking and cycling trips have no delay.
- **FR7** **Hide:** when the measured speed stays above the driving speed for about 30 s, the phone closes the watchapp (back to the watch face) and enters the *hidden* state. If the planned speed is above about 40 km/h (25 mph), it may hide sooner.
- **FR8** **While hidden:**
  - **FR8.1** the phone sends no navigation updates that would open or wake the watchapp, so the watchapp stays closed and does not vibrate for new instructions or early warnings;
  - **FR8.2** the phone keeps estimating the speed;
  - **FR8.3** the hidden state does not count as "No navigation detected";
  - **FR8.4** a future "keep the watchapp in front" behaviour (#127) must not reopen it.
- **FR9** **Show again:** when the speed stays below the driving speed minus a margin (about 5 km/h or 3 mph) for about 2 minutes, the phone reopens the watchapp, which shows the current instruction, and leaves the hidden state.
  - **FR9.1** If "Launch watchapp when navigation starts" is off, the phone still closes the watchapp when driving but never reopens it automatically; it leaves the hidden state, and updates resume when the user opens the watchapp. [Q3]
- **FR10** **The user wins:** if the user opens the watchapp manually while it is hidden, it stays open and receives updates for the rest of that navigation session; it is not closed again for driving until navigation stops and a new session starts.
- **FR11** When navigation stops, the hidden state and the "kept open" choice end. The next navigation session starts fresh.
- **FR12** Changing When driving to `Show directions` while hidden leaves the hidden state at once and reopens the watchapp (when auto-launch is on).

### Phone dashboard

- **FR13** While hidden, the Status section of the phone app's dashboard shows that the watch is hidden for driving, with the estimated speed (for example "Watch: hidden while driving (about 48 km/h)", in the active unit). [Q4]

### Diagnostics

- **FR14** The diagnostics export contains a driving log: for each speed estimate and each hide or show decision, the time, the estimated speed (measured and planned), the threshold in use, and the decision.
  - **FR14.1** The log contains no notification text, street names or place names.
  - **FR14.2** It is included in both the privacy-safe and the full export. [Q5]
  - **FR14.3** The export schema version is raised and the export format document describes the new section.

## Non-Functional Requirements

- **NFR1** *Privacy:* speed estimation runs entirely on the phone; no location permission, no new permission and no network use are added.
- **NFR2** *Stability of the decision:* with speed samples that cross the threshold back and forth (for example traffic lights), the watchapp is not closed and reopened more than once in any 2-minute window. Verified with unit tests using simulated time.
- **NFR3** *Testability:* the speed estimator and the hide/show decision are pure logic (inputs: samples, clock, settings; output: decision) with unit tests that use virtual time, following the existing session reducer pattern.
- **NFR4** *Watch memory:* the new menu and settings stay within the aplite heap budget in REQ-WATCH-025.
- **NFR5** *Compatibility:* a watchapp with this feature still works with an older phone app (the new settings simply keep their defaults), and an older watchapp works with the new phone app (FR4).
- **NFR6** *Default unchanged:* with the default settings, the watch shows exactly the same as before this change, and existing tests stay green.

## Constraints

- The protocol gains three setting keys (41–43) and a minor version bump (1.3 to 1.4); the generated `Protocol.kt` and `protocol.h` come from `protocol/generate.py`.
- On the watch, the new settings need persist keys 29–31: the current key formula would collide with keys 27 and 28 (code scan finding).
- The fixed count of 23 synced settings in tests, C asserts and REQ-WATCH-025 becomes 26.
- REQ-WATCH-006 currently forbids relaunching the watchapp on a timer or after the user left it; it is amended so that reopening after a driving hide (FR9) is the one allowed exception. REQ-WATCH-005 and REQ-WATCH-007 say how hiding combines with auto-launch, and `spec/200-architecture/Pebble.md`'s launch policy is updated accordingly.
- New requirement IDs in the project's requirement files: REQ-WATCH-026, REQ-ANDROID-024, REQ-DEBUG-016.
- Express plan: no separate design stage; requirement-driven unit tests, existing tests stay green.

## Assumptions

- `[assumption]` Google Maps' `progressMax − progress` is the remaining route distance in metres. It matched the route lengths in the emulator runs and the speeds in the reporter's diagnostics, but it is not documented by Google.
- `[assumption]` The thresholds (30 s to hide, 2 minutes and a 5 km/h margin to show, 40 km/h for the early planned-speed hide) are starting values to tune with real trips; they are constants in code, not user settings.
- `[assumption]` Closing the watchapp from the phone uses PebbleKit's stop-app call, which works with any watchapp version; if it proves unreliable, a watch-side exit message is the fallback (decided in code generation).
- `[assumption]` Whether the Pebble mobile app itself mirrors Google Maps' own notification to the watch is outside PebbleNTN and not affected by this feature.

## Out of Scope

- Separate settings for walking and cycling.
- Android activity recognition (needs a new permission and Google Play services, not suited to F-Droid).
- Car detection through Bluetooth or Android Auto.
- Speed estimation for navigation apps other than Google Maps and MicroG Maps (later, for apps that show distance and arrival time).
- A screenshot preview for the When driving settings.

## Open Questions

- None blocking. The timing constants are tuned after real trips (see Assumptions).
