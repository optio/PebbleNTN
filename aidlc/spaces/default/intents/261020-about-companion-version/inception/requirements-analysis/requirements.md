# Requirements — #103 About screen: last connected phone app version

## Sources

- [desc] Initial description: "Work on GitHub issue #103: About screen shows the last connected companion app version (watch-side only, no protocol change; amend REQ-WATCH-020)"
- GitHub issue #103 (behaviour, implementation notes, spec amendment)
- [Q1] Line wording: answer C (name both versions; phone version marked "last seen")
- [Q2] Refresh: answer A (snapshot when About opens)
- Code knowledge base: `aidlc/spaces/default/codekb/PebbleNTN/` (api-documentation.md, architecture.md "About screen", code-quality-assessment.md)

## Intent Analysis

The watchapp updates itself through the store, but the phone app does not. Users and the maintainer need to see which phone app version the watch last talked to, both to explain behaviour ("which phone build sent this?") and as the basis for the update notice in #94. The phone already sends its version on every message (key 11 `APP_VERSION`), so this is a watch-only change: read it, keep it, show it.

- **Type**: enhancement, single component (watchapp About screen), Minimal depth.
- **Users**: anyone looking at About on the watch; the maintainer when triaging reports.

## Functional Requirements

### FR1 — Record the phone app version

- **FR1.1** When an inbound message carries key 11 (`APP_VERSION`), the watchapp SHALL take its string as the last connected phone app version. A message without key 11 SHALL leave the recorded version unchanged.
- **FR1.2** The watchapp SHALL persist the recorded version (new persist key 16, after theme.c's 1–15) and load it at startup, so it is shown after a relaunch and while the phone is out of range.
- **FR1.3** The watchapp SHALL write the persisted value only when it differs from the stored one, to spare flash writes (the phone sends the key with every navigation update).
- **FR1.4** The recorded version SHALL be kept to a fixed maximum length (16 bytes of text), cut cleanly and always NUL-terminated. An empty string SHALL be treated as no version.

Acceptance:
- Given no version was ever stored, When a NAVIGATION_UPDATE with `APP_VERSION = "0.0.67"` arrives, Then "0.0.67" is recorded and persisted.
- Given "0.0.67" is stored, When another message with "0.0.67" arrives, Then no persist write happens.
- Given "0.0.67" is stored, When a message with "0.0.70" arrives, Then "0.0.70" is recorded and persisted.
- Given "0.0.67" is stored, When a message without key 11 arrives, Then "0.0.67" stays.
- Given a version longer than 16 bytes arrives, Then the stored and shown value is its first bytes, NUL-terminated.

### FR2 — Show both versions on About

- **FR2.1** The About details SHALL read, in this order [Q1]:
  1. `Watchapp <watchapp version>` (renamed from `Version <…>`)
  2. `Phone app <version> (last seen)`, or `Phone app: never connected` when no version was ever recorded
  3. `Protocol <major>.<minor>`
  4. the phone status line, unchanged (`Phone app connected` / `Phone app not answering` / `Pebble app not connected`)
- **FR2.2** About SHALL take the phone version when it opens, as it already does for the phone status; it SHALL NOT refresh while open [Q2].
- **FR2.3** The About row's subtitle in the settings menu SHALL stay the watchapp version.
- **FR2.4** The details text SHALL never be cut mid-line for any combination of a 16-byte watchapp version, a 16-byte phone version and the longest status line: the details buffer SHALL be sized for it (today's 80 bytes is too small for the new worst case).

Acceptance:
- Given "0.0.67" was recorded, When About opens, Then it shows `Watchapp 0.0.68`, `Phone app 0.0.67 (last seen)`, `Protocol 1.1` and the status line.
- Given nothing was ever recorded, When About opens, Then it shows `Phone app: never connected`.
- Given the phone is out of range after a relaunch and "0.0.67" was stored, When About opens, Then it shows `Phone app 0.0.67 (last seen)` and `Pebble app not connected`.
- Given both versions at 16 bytes and the longest status line, Then the details text is complete (host test).
- Given About is open, When a phone message with a new version arrives, Then the screen is unchanged until About is reopened.

### FR3 — Specification

- **FR3.1** REQ-WATCH-020 SHALL be amended to list the last connected phone app version, the `Watchapp` label and the `never connected` case, with matching acceptance criteria, and `MANIFEST.sha256.json` SHALL be resynced in the same commit.

Acceptance:
- Given the amended `requirements/REQ-WATCH.md`, When the spec asset check runs (`scripts/validate-spec-assets.sh`, also in pre-commit and CI), Then it passes.

## Non-Functional Requirements

- **NFR1 — No protocol change**: `protocol/protocol-definition.json`, generated `Protocol.kt` / `protocol.h` and the protocol version stay unchanged; the phone app is untouched. Pass: the generated-constants check passes and the diff touches no Android or protocol file.
- **NFR2 — Flash wear**: at most one persist write per distinct version received (FR1.3). Pass: host test of the "write only when changed" helper.
- **NFR3 — Layout on every platform**: the About screen renders, scrolls to the install link and keeps text inside the bezel on basalt, chalk (round), emery and diorite. Pass: emulator screenshots on all four.
- **NFR4 — Tests**: the pure About text and version-recording helpers are host unit-tested (happy path plus never connected, truncation, unchanged-version and empty-version cases), and `scripts/test-all.sh` stays green.

## Constraints

- Watchapp C on the Pebble SDK; pure logic in Pebble-free units with a `tests/test_<unit>.c` twin (enforced by `scripts/test-watchapp-unit.sh`).
- Persist keys 1–15 are taken by theme.c; key 16 is the next free one.
- Spec and requirement files are integrity-protected by `MANIFEST.sha256.json`.
- Every behaviour change cites requirement IDs (AGENTS.md).

## Assumptions

- The PHONE_COMPATIBILITY_ERROR message carries no `APP_VERSION`, so an incompatible phone leaves the last recorded version as is (code knowledge base, api-documentation.md). If it ever does carry one, FR1.1 records it like any other message.
- Real phone versions are short (`0.0.68`), so the 16-byte cap only matters for unusual builds.

## Out of Scope

- The "update available" notice on the watch (#94); it may reuse the stored version later.
- Settings sync between watch and phone (#102).
- Any change to the Android app or the protocol.
- Live refresh of About while open [Q2].

## Open Questions

None.
