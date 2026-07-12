# Release Checklist (M10)

Gates before a public release. Automated gates run in CI (`.github/workflows/release.yml`); the
manual matrix must be completed on hardware because it cannot run in this build environment.

## Automated (CI / local)

- [ ] `./scripts/test-all.sh` green (spec integrity, protocol `--check`, catalog + rule schema,
      rule regression, Android unit tests + lint).
- [ ] `./gradlew verifyReleaseTargetSdk` passes (target SDK ≥ Play floor).
- [ ] `./gradlew bundleRelease` produces a signed AAB (CI, using the protected environment secrets).
- [ ] `pebble build` produces `watchapp.pbw`.
- [ ] CodeQL analysis clean.
- [ ] SBOM / dependency license report generated where practical.

## Manual device matrix (must run on hardware)

Phone (Android) × Pebble platform. Cover at least one b/w and one color watch, plus a round watch.

| Phone Android | Pebble | Notes |
|---|---|---|
| 12 (min SDK 31) | Pebble 2 / Time (b/w, basalt/diorite) | oldest supported |
| 14 | Pebble Time Round (chalk) | round display layout |
| latest | Pebble Time 2 / emery | large display |

For each cell verify:

- [ ] Onboarding disclosure shown before opening listener settings; access grant detected.
- [ ] Google Maps navigation → correct maneuver bitmap, distance and road text on the watch.
- [ ] Launch-once: watchapp launches once at session start; no relaunch loop after leaving it.
- [ ] READY handshake after opening the watchapp mid-session syncs the current state.
- [ ] Navigation end → watch returns to the watchface (with the setting on).
- [ ] Vibrate-on-maneuver-change and backlight settings behave.
- [ ] Connection loss then reconnect → current state re-synced, no stale replay.
- [ ] Disabled app produces no debug records (allowlist).
- [ ] Debug export (rules-only / privacy-safe / full) shares via the sheet; full shows the warning.
- [ ] Process restart mid-session restores the current state.

## Instrumented tests (device/emulator)

- [ ] `./gradlew connectedDebugAndroidTest` green, including the REQ-ANDROID-003 spy proving the
      snapshot factory / parser / DB are never called for a disabled package.

## Store submission

- [ ] `docs/PRIVACY_POLICY.md` finalized and hosted; URL added in Play Console.
- [ ] `docs/PLAY_DATA_SAFETY.md` answers entered accurately.
- [ ] Notification-access justification + prominent disclosure provided.
- [ ] Play App Signing enabled; upload key protected.

## Beta

- [ ] Internal testing track build installed by maintainers; run the device matrix.
- [ ] Closed beta with a small group; collect privacy-safe diagnostics for rule gaps.
- [ ] Triage rule misses (non-English locales especially) before production rollout.
