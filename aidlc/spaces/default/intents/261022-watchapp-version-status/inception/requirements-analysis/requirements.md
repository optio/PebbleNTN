# Requirements — Watchapp version in the dashboard status

## Sources

- [desc] Initial description: "Show the watchapp version (last seen) in the phone app dashboard status section, from the watch_app_version the phone stores when the watch WATCH_READY arrives (v0.0.73); show "not received yet" when none was stored. Purpose: lets the user check on-device whether the Core Devices companion forwards watch-to-phone messages."
- [Q1] Value: A. The version only, "0.0.73 (last seen)", or "Not received yet".
- [Q2] Place: A. Directly under the "Watch" row, titled "Watchapp version".
- Code knowledge base: `aidlc/spaces/default/codekb/PebbleNTN/`, refreshed by this intent's focused scan. The developer scan is at `inception/reverse-engineering/developer-scan.md`.

## Intent Analysis

Since v0.0.73 the phone stores the watchapp version (`UpdateState.watchAppVersion`, prefs `watch_app_version`) when a WATCH_READY from the watch arrives, but nothing shows it. Showing it on the dashboard has two uses:
- It is a simple on-device test of whether the user's Pebble companion app (in particular the Core Devices app) delivers watch→phone messages to PebbleNTN. That decides whether an upstream issue is needed.
- It tells the user which watchapp version is installed, mirroring the watch's About screen, which shows the phone app version (#103).

- **Type**: small enhancement, Android app only, Minimal depth. No protocol or watch change.

## Functional Requirements

### FR1 — Watchapp version row

- **FR1.1** The dashboard Status section SHALL show a row titled "Watchapp version" directly below the "Watch" row [Q2].
- **FR1.2** When a watchapp version is stored, the row's value SHALL be "{version} (last seen)" (for example "0.0.73 (last seen)"), with the version shown exactly as stored [Q1].
- **FR1.3** When none is stored, the value SHALL be "Not received yet" [Q1].
- **FR1.4** The row SHALL update while the dashboard is open, as soon as a newly received WATCH_READY changes the stored version, because it follows `updateCheckRepository.state`.
- **FR1.5** The title and both texts SHALL be string resources.

Acceptance:
- Given a stored watchapp version `0.0.73`, When the row text is computed, Then it is "0.0.73 (last seen)" (JVM test of a pure helper).
- Given none is stored (null or blank), Then it is "Not received yet".
- Given a stored value that doesn't follow the version rule (for example `0.0.72-debug`), Then it is shown as stored: "0.0.72-debug (last seen)".
- On a phone, opening PebbleNTN on the watch with the original Pebble app fills the row. With the Core Devices app, the row shows whether the companion forwards the message (manual check).

### FR2 — Specification

- **FR2.1** `requirements/REQ-ANDROID.md` REQ-ANDROID-019 SHALL mention that the dashboard shows the last seen watchapp version. `MANIFEST.sha256.json` SHALL be resynced if a listed file changes.

## Non-Functional Requirements

- **NFR1 — No new storage or network**: the row only reads the existing `watch_app_version`. No new preference, protocol key or network call.
- **NFR2 — Tests**: the row text has a JVM unit test, and `scripts/test-all.sh` stays green.
- **NFR3 — Layout**: the row uses the existing `InfoRow` style, so it looks like the other Status rows. It is checked on a device (no Android emulator in WSL).

## Constraints

- The stored version has no time and is never cleared. The row proves only that a WATCH_READY arrived at least once with that version ("last seen") [Q1].
- Android app only. Pass `DashboardScreen` a new defaulted parameter, following the existing pattern.

## Assumptions

- The user tests with the watchapp opened from the watch launcher, after installing a build with this row.
- With the Core Devices app, "Not received yet" after opening the watchapp means watch→phone messages don't reach PebbleNTN.

## Out of Scope

- Storing or showing when the version was last received (option B, not chosen).
- A hint text explaining what "Not received yet" means (option C, not chosen).
- Any watch change.

## Open Questions

None.
