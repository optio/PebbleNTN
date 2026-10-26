# Release Checklist (M10)

Gates before a public release. Automated gates run in CI (`.github/workflows/release.yml`); the
manual matrix must be completed on hardware because it cannot run in this build environment.

## Automated (CI / local)

- [ ] `./scripts/test-all.sh` green (spec integrity, protocol `--check`, catalog + rule schema,
      rule regression, Android unit tests + lint).
- [ ] `./gradlew verifyReleaseTargetSdk` passes (target SDK ≥ Play floor, 36). `release.yml` and
      `play-release.yml` both run it and fail the job when it fails.
- [ ] `./gradlew bundleRelease` produces an AAB signed with the Play upload key (`play-release.yml`,
      using the `PLAY_UPLOAD_*` secrets of the protected `release` environment).
- [ ] `tools/play_store_graphics.py --check` passes (run by `scripts/test-all.sh` when Pillow is
      available): icon, feature graphic and phone screenshots meet Play's sizes.
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
- [ ] Early-warning vibration (REQ-WATCH-022): at 50 m / Once, one distinct buzz as a turn comes
      within 50 m; with Every update, one buzz per closer distance.
- [ ] Connection loss then reconnect → current state re-synced, no stale replay.
- [ ] Disabled app produces no debug records (allowlist).
- [ ] Debug export (rules-only / privacy-safe / full) shares via the sheet; full shows the warning.
- [ ] Process restart mid-session restores the current state.

### Android 16 (target SDK 36)

Once per target-SDK change, on an Android 16 phone (the behaviour changes for apps targeting 36):

- [ ] Edge-to-edge can no longer be opted out of: onboarding and the dashboard bottoms are not
      hidden behind the navigation bar, with 3-button navigation and with gesture navigation.
- [ ] Predictive back is on: back from every screen returns where expected, and the rule editor's
      back with unsaved changes shows the discard dialog (Compose `BackHandler`).
- [ ] A navigation session reaches the watch (the PebbleKit 2 binding to the Pebble companion app).
- [ ] An install from Google Play (internal testing track) shows no "Check for updates" row, weekly
      toggle or stale-check card, and its update card opens the Play listing.

## Instrumented tests (device/emulator)

- [ ] `./gradlew connectedDebugAndroidTest` green, including the REQ-ANDROID-003 spy proving the
      snapshot factory / parser / DB are never called for a disabled package.

## Store submission

Every Play Console answer and step is in [`PLAY_CONSOLE.md`](PLAY_CONSOLE.md).

- [ ] Signing: the release key enrolled in Play App Signing (PEPK), a separate upload key in the
      `PLAY_UPLOAD_*` secrets, both keystores backed up offline (`PLAY_CONSOLE.md` §1).
- [ ] App content answered: privacy policy URL (`docs/PRIVACY_POLICY.md`), ads, app access with the
      demo video, content rating, target audience, Data safety (`docs/PLAY_DATA_SAFETY.md`),
      notification-access justification and disclosure (`PLAY_CONSOLE.md` §2).
- [ ] Store listing from `fastlane/metadata/android/en-US/` (`PLAY_CONSOLE.md` §3).
- [ ] Closed test (12 testers, 14 consecutive days) and Android developer verification of
      `com.pebblentn.app` and the release signing key (`PLAY_CONSOLE.md` §5).

## Beta

- [ ] Internal testing track build installed by maintainers; run the device matrix.
- [ ] Closed beta with a small group; collect privacy-safe diagnostics for rule gaps.
- [ ] Triage rule misses (non-English locales especially) before production rollout.
