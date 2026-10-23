# Requirements — Version comparison as the default update check

## Sources

- [desc] Initial description: "Refactor how the "there is an update" notification works: no update check from the companion app (no GitHub/internet check), only by comparing that the watchapp version is higher than the companion app version. Confirmed on a phone with the Core Devices app: the phone does receive the watchapp version (v0.0.74 dashboard row shows it), so the earlier "not with the Core Devices app" limitation no longer applies."
- Person, later in the same request: "Note that the app internet check can remain, this is jsut an additional check", and "the non internet check should be the default, optionally the user can check using the existing online feature manually or automated as what the current feature supports".
- [Q1] C. Keep the #94 "Check for updates" item as it is.
- [Q2] B. Keep the weekly-check question.
- [Q3] A. The manual check's result message combines both sources.
- [Q4] B. The texts describe the offline check as the default, in shorter wording.
- Code knowledge base: `aidlc/spaces/default/codekb/PebbleNTN/` (refreshed by this intent's focused scan), and `inception/reverse-engineering/developer-scan.md`.

## Intent Analysis

Since v0.0.73 both apps detect an update offline: the watchapp is newer than the phone app. v0.0.74 showed on a real phone that the Core Devices app passes the watchapp version to the phone, so the offline check works for every user. It is the default way PebbleNTN learns about a new version. The online GitHub check stays unchanged as an optional extra: manual, or weekly when turned on.

What's left:
- the manual check's message only reflects GitHub, so it can contradict the card;
- the texts present GitHub as the only source;
- several code comments and specs still say the Core Devices app doesn't forward watch messages.

- **Type**: small enhancement and correction, Android app plus specs and docs, Minimal depth. No watch or protocol change.

## Functional Requirements

### FR1 — The manual check's result message combines both sources [Q3]

- **FR1.1** After "Check for updates now" or the stale "Check for updates" item, the message SHALL say:
  - "PebbleNTN {version} is available." when the combined update state (`UpdateState.updateAvailable`, GitHub or watchapp) reports an update, with `{version}` = `effectiveLatest` and any leading `v` removed;
  - "PebbleNTN is up to date." only when neither source reports a newer version;
  - "Couldn't check for updates. Try again later." when the GitHub fetch failed and the watchapp version shows no update.
- **FR1.2** When the GitHub fetch fails but the watchapp version shows an update, the message SHALL still say that version is available. The offline check is the default and doesn't need the network.
- **FR1.3** The choice of message SHALL be a pure function that is unit-tested.

Acceptance:
- Given GitHub says up to date and the stored watchapp version is newer (`0.0.75` versus `0.0.74`), When the manual check finishes, Then the message is "PebbleNTN 0.0.75 is available.".
- Given both sources show no newer version, Then "PebbleNTN is up to date.".
- Given the fetch fails and there is no watch update, Then "Couldn't check for updates…". Given the fetch fails and there is a watch update, Then "PebbleNTN {version} is available.".
- Given GitHub reports `v0.0.76`, Then "PebbleNTN 0.0.76 is available." (the `v` removed).

### FR2 — Texts describe the offline check as the default [Q4]

- **FR2.1** The onboarding disclosure's network sentence SHALL become: "PebbleNTN spots new versions offline, by comparing the phone app with your watchapp. It uses the network only for the optional GitHub check (Check for updates, or weekly if you turn it on), which sends no personal data."
- **FR2.2** The weekly toggle hint SHALL become: "Optional: also check GitHub about once a week. Nothing is sent."
- **FR2.3** The stale "Check for updates" item and the weekly-check question SHALL keep their behaviour and texts [Q1, Q2].
- **FR2.4** The KDoc in `UpdateCheckRepository` and `AppContainer`, and the INTERNET comment in `AndroidManifest.xml`, SHALL describe the GitHub check as optional and the version comparison as the default.

### FR3 — Correct the Core Devices statements

- **FR3.1** Code comments that say the Core Devices app doesn't forward watch messages, or that WATCH_READY never arrives, SHALL be corrected. They are in the `NavigationController.scheduleAutonomousReady` KDoc, the `WatchListenerService.onAppOpened` KDoc, the `UpdateCheckRepository.recordWatchAppVersion` KDoc, and comments in `NavigationControllerTest`. The new wording: forwarding was confirmed in v0.0.74 once `companionApp` is declared; the made-up readiness stays as a fallback (auto-launch off, settle delay). Behaviour is unchanged.
- **FR3.2** REQ-ANDROID-019 SHALL drop its "best effort / not with the Core Devices app" limitation and state that the phone reads the watchapp version from WATCH_READY. `spec/200-architecture/Pebble.md` §"Companion limitation and synthesized readiness" and the open note in `docs/IMPLEMENTATION_STATUS.md` SHALL be corrected the same way. `MANIFEST.sha256.json` SHALL be resynced for any listed file.
- **FR3.3** REQ-ANDROID-013 and REQ-ANDROID-018 SHALL say that the GitHub check is an optional extra, and that the version comparison (REQ-ANDROID-019) is the default way updates are detected.

## Non-Functional Requirements

- **NFR1 — No behaviour change beyond FR1:** the update card, the stale item, the weekly question, bit 4 / key 14 and the watch side stay the same. No protocol or watch change.
- **NFR2 — No new network access:** the GitHub check's triggers stay as they are (manual, or weekly when turned on).
- **NFR3 — Tests:** a JVM test for the FR1 message choice. Existing tests stay green, and `scripts/test-all.sh` passes.
- **NFR4 — Strings:** the new and changed texts are string resources. The message with a version uses a placeholder.

## Constraints

- Android app, specs and docs only. No Android emulator in WSL; the texts are checked on a device.

## Assumptions

- "Default" means the offline check runs without any setting or network. The online check stays opt-in, as it is today.
- `watch_app_version` is never cleared. Updating the phone app still clears the update, because the comparison uses the running phone version.

## Out of Scope

- Removing or reworking the #94 stale item or the weekly question [Q1, Q2].
- Any change to the GitHub fetch, its 7-day limit or the auto-check default.
- Watch or protocol changes.

## Open Questions

None.
