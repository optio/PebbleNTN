# Requirements — #94 Phone app update notice on the watch

## Sources

- [desc] Initial description: "Work on GitHub issue #94: show a non-intrusive notice in the watchapp, and an update subtitle in the watch app list, when the companion phone app has an update available; plus the stale update-check entry and weekly-check suggestion in the Android app, as decided in the issue comments"
- GitHub issue #94: the body, plus three maintainer comments (protocol flag design, the decision on the off-by-default check, the launcher subtitle addition)
- [Q1] Frequency: X. "Once per version but if the user manually starts the watchapp an it shows the No navigation detected message then it should show it every time in that screeen"
- [Q2] During a route: C. Wait, then show the notice once the route ends (still in the app)
- [Q3] Launcher subtitle: B. `Update phone app to {version}`
- [Q4] Notice text: C. "A phone app update is available. Open the mobile app to find the update."
- [Q5] Idle screen: A. Replace the idle text while an update is pending
- Code knowledge base: `aidlc/spaces/default/codekb/PebbleNTN/` (api-documentation.md, architecture.md, component-inventory.md, code-quality-assessment.md)

## Intent Analysis

The store updates the watchapp automatically, but users have to update the phone app themselves, so phone installs fall behind. The phone already learns about new versions (REQ-ANDROID-013) but never tells the watch, and its check is off by default, so many installs never learn about an update at all. This work has two parts. First, the phone sends "update available" and the latest version to the watch, and the watch shows it without getting in the way. Second, the phone makes a stale update check visible, so more installs find out about updates.

- **Type**: enhancement, multi-component (protocol, Android app, watchapp), Minimal depth.
- **Users**: everyone who runs the phone app from a GitHub, Obtainium or F-Droid install.

## Functional Requirements

### FR1 — Protocol

- **FR1.1** `protocol/protocol-definition.json` SHALL add flag bit `PHONE_UPDATE_AVAILABLE: 4` and key `LATEST_PHONE_VERSION: 14` (a C string). The protocol minor version SHALL go from 1.1 to 1.2. `Protocol.kt` and `protocol.h` SHALL be regenerated with `scripts/generate-protocol.sh`. `examples/protocol-definition.json` and `spec/200-architecture/Protocol.md` SHALL be updated to match, and `MANIFEST.sha256.json` resynced, all in the same commit.

Acceptance:
- Given the updated definition, When `scripts/test-all.sh` runs the protocol consistency and generated-constants checks, Then both pass.

### FR2 — The phone tells the watch

- **FR2.1** While `UpdateCheckRepository.state.updateAvailable` is true, the phone SHALL set bit 4 in `FLAGS` on every state message it sends: `NAVIGATION_UPDATE`, `NAVIGATION_STOPPED` and `NO_ACTIVE_NAVIGATION`. This includes the no-navigation message, whose flags are currently always 0.
- **FR2.2** Whenever bit 4 is set, the same message SHALL carry `LATEST_PHONE_VERSION`: the latest known version with any leading `v` removed (e.g. `v0.0.71` → `0.0.71`). When the bit is clear, the key SHALL be left out.
- **FR2.3** The compatibility-error message is unchanged. It already tells the user to update the phone app.

Acceptance:
- Given an update is available with latest `v0.0.71`, When the phone encodes a NO_ACTIVE_NAVIGATION, NAVIGATION_UPDATE or NAVIGATION_STOPPED message, Then `FLAGS` has bit 4 set and `LATEST_PHONE_VERSION = "0.0.71"` (JVM test).
- Given no update is available, When it encodes any of them, Then bit 4 is clear, key 14 is absent, and the other flag bits are as before.

### FR3 — The watch records the update state

- **FR3.1** On each inbound state message that carries `FLAGS`, the watch SHALL treat bit 4 as the update-pending state. When the bit is set and key 14 is a non-empty string, it SHALL record that string as the latest phone version (capped at 16 bytes, always NUL-terminated). When the bit is clear, it SHALL clear the recorded version.
- **FR3.2** The recorded latest version, and the version for which the notice was last shown, SHALL be persisted (new persist keys 17 and 18). Each SHALL be written only when its value changes. Both SHALL be loaded at startup, so the launcher subtitle survives a relaunch while the phone is out of range.

Acceptance:
- Given nothing is recorded, When a message arrives with bit 4 set and key 14 = "0.0.71", Then "0.0.71" is recorded and persisted.
- Given "0.0.71" is recorded, When the same message arrives again, Then nothing is written.
- Given "0.0.71" is recorded, When a message arrives with bit 4 clear, Then the recorded version is cleared.
- Given bit 4 is set but key 14 is missing or empty, Then the recorded version is unchanged (host test).

### FR4 — The update notice

- **FR4.1** While an update is recorded and the notice has not yet been shown for that version [Q1], the watch SHALL show a full-screen notice reading "A phone app update is available. Open the mobile app to find the update." [Q4]. It SHALL have no vibration and no backlight change.
- **FR4.2** The notice SHALL close after 10 seconds, or at once when any button is pressed. Pressing a button SHALL only close the notice and SHALL NOT also act on the screen behind it.
- **FR4.3** Once shown, the notice SHALL be recorded as shown for that version (FR3.2). It SHALL appear again only for a different latest version.
- **FR4.4** While a route is active, the watch SHALL NOT show the notice. It SHALL wait and show the notice when the route ends while the app is still open (NAVIGATION_STOPPED or NO_ACTIVE_NAVIGATION) [Q2]. If the app closes first, for example because it exits to the watchface on stop, the notice stays unshown and appears at the next start with no route active.

Acceptance:
- Given "0.0.71" is recorded and never shown, When the watch receives NO_ACTIVE_NAVIGATION, Then the notice appears, closes after 10 s, and "0.0.71" is marked shown.
- Given the notice is open, When any button is pressed, Then it closes and nothing else happens.
- Given "0.0.71" is marked shown, When the app starts again, Then no notice appears.
- Given "0.0.72" then arrives, Then the notice appears once more.
- Given a NAVIGATION_UPDATE is the first message with bit 4 set, Then no notice appears while the route runs. When a NAVIGATION_STOPPED follows and the app stays open, Then the notice appears.
- The show/defer/skip decision is a pure helper with host tests for each case above.

### FR5 — The "No navigation detected" screen

- **FR5.1** While an update is recorded, the idle message SHALL read "No navigation detected. Phone app update available: open the mobile app." in place of the usual "No navigation detected. If navigating, open PebbleNTN on your phone." It SHALL do so every time that screen shows, whether or not the notice was already shown [Q1, Q5].

Acceptance:
- Given an update is recorded, When NO_ACTIVE_NAVIGATION arrives (after any notice closes), Then the idle screen shows the update text.
- Given no update is recorded, Then it shows the usual text.
- Both texts fit within the bezel on basalt, chalk, emery and diorite (emulator).

### FR6 — Launcher subtitle (amends REQ-WATCH-019)

- **FR6.1** When the app closes, the subtitle SHALL follow this priority:
  1. the live route countdown while a route is active, unchanged;
  2. otherwise, while an update is recorded, `Update: {version}` [Q3] (shortened from `Update phone app to {version}` in Build and Test loop-back 1, 2026-10-22, because the launcher cut it after about 16 characters);
  3. otherwise `Navigate on phone`.
- **FR6.2** When a route countdown expires and an update is recorded, the launcher SHALL fall back to the update text, not `Navigate on phone`.
- **FR6.3** Characters the template language treats specially (`{`, `}`, `\`) SHALL be dropped from the version, as the countdown already does for its arrival text. Aplite has no app glances, so it relies on the notice and the idle text.

Acceptance:
- Host tests in `tests/test_glance_text.c`: an update with no route gives the update text; an update during a route gives the countdown with the update text as the expiry fallback; no update gives today's behaviour.
- In the emulator on basalt, chalk and emery, `Update: 0.0.71` is readable in the launcher.

### FR7 — Phone: a stale update check becomes visible

- **FR7.1** `UpdateCheckRepository` SHALL expose `lastCheckMillis` and `isCheckStale(now)`. A check is stale when the last check was more than 30 days ago, or never happened. This uses the same `last_check_millis` the weekly throttle uses.
- **FR7.2** While the check is stale, the dashboard SHALL show a **Check for updates** item as its first item, above everything else, setup checklist included. Tapping it SHALL run a manual (forced) check and confirm the outcome as the manual check does today. Once a check has run, the item SHALL drop back to its normal place in the Updates section.
- **FR7.3** When a check started from that item finishes, whatever the result (up to date, update available or failed), and only while weekly checks are off, the dashboard SHALL ask: "Check for updates automatically every week?", with **Turn on** and **Not now**. **Turn on** SHALL call `setAutoCheckEnabled(true)`. **Not now** SHALL dismiss the question. It comes back only the next time the stale item appears, so at most once every 30 days.
- **FR7.4** All new strings SHALL be Android resources (AGENTS rule 16). The onboarding disclosure stays unchanged, since it already describes the network use.

Acceptance:
- JVM tests: `isCheckStale` is true when there has never been a check, false at exactly 30 days, true just over 30 days, and false right after a check.
- Given a stale check, When the dashboard shows, Then **Check for updates** is the first item. Given a check ran, Then it is gone from the top.
- Given weekly checks are off, When the stale-item check finishes, Then the weekly question appears. **Turn on** enables weekly checks; **Not now** hides the question.
- Given weekly checks are on, Then the question never appears.

### FR8 — Specification

- **FR8.1** These requirement changes SHALL land in the same commit:
  - add REQ-WATCH-023 (update notice and idle text);
  - amend REQ-WATCH-019 (subtitle priority);
  - amend REQ-WATCH-020, whose "no new protocol key" note no longer holds once this change ships;
  - add REQ-ANDROID-017 (tell the watch about an update);
  - add REQ-ANDROID-018 (stale-check entry and weekly question);
  - update `Protocol.md`, the protocol table, to 1.2.
  `MANIFEST.sha256.json` SHALL be resynced.

Acceptance:
- Given the amended files, When `scripts/validate-spec-assets.sh` runs, Then it passes.

## Non-Functional Requirements

- **NFR1 — Compatibility**: a 1.1 watchapp ignores bit 4 and key 14, and a 1.1 phone app never sets them, so either side can update first. Pass: a phone at protocol 1.2 talking to a watch at 1.1 shows no error, and the reverse shows no update UI (the same pattern as `STOPS_REMAINING` in 1.1).
- **NFR2 — No new network access**: weekly checks stay off by default, and the phone makes no network call unless the user taps a check or turns weekly checks on (REQ-ANDROID-013). Pass: the existing "auto-check default off" and "no call on launch" tests stay green.
- **NFR3 — Flash wear**: each persist key is written only when its value changes. Pass: host tests of the record helpers.
- **NFR4 — Layout on every platform**: the notice and both idle texts render inside the bezel on basalt, chalk (round), emery and diorite; the subtitle is readable on basalt, chalk and emery. Pass: emulator screenshots.
- **NFR5 — Tests**: the pure watch helpers (update record, notice decision, idle text, glance text) are host unit-tested, the phone parts (codec flag and key, stale check) are JVM-tested, and `scripts/test-all.sh` stays green.

## Constraints

- The watchapp is C on the Pebble SDK. Pure logic goes in Pebble-free units, each with a `tests/test_<unit>.c` twin (`scripts/test-watchapp-unit.sh`). The watch's C tests run only locally through `test-all.sh`, not in CI.
- Persist keys 1–16 are taken (theme.c 1–15, phone version 16), so 17 and 18 are the next free ones.
- Generated protocol files are never edited by hand (AGENTS rule 5).
- Spec, requirement and example files are integrity-protected by `MANIFEST.sha256.json`.
- Every behaviour change cites requirement IDs (AGENTS.md).

## Assumptions

- The watch learns about an update from the next state message the phone sends. At launch, the handshake reply (NO_ACTIVE_NAVIGATION or a NAVIGATION_UPDATE) carries it. The phone does not push an extra message when a check finds an update while the watchapp is already open.
- F-Droid lags GitHub, so F-Droid users can be told about a version F-Droid does not offer yet. The phone's existing "Update available" card already behaves this way. "Once per version" [Q1] limits the notice to one appearance per version, and the idle text and subtitle stay until the user updates.
- The existing "Update available" card on the phone dashboard is what "the mobile should also clearly indicate there is an update" refers to, and it already exists. This work does not change it.
- The watch checks only the protocol major version, so the minor bump to 1.2 causes no compatibility error on either side.

## Out of Scope

- Pushing a fresh message to the watch as soon as a check finds an update while the watchapp is open.
- Any change to how the phone checks for updates, apart from FR7 (still GitHub releases and the weekly throttle).
- Settings sync between watch and phone (#102).
- A version in the notice text (the user chose the issue's wording [Q4]). The About screen keeps showing the last seen phone version (#103).

## Open Questions

None.
