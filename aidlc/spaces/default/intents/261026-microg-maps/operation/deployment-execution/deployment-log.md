# Deployment Log — MicroG Maps as a Google Maps variant (#113)

## Target

The release pipeline (`.github/workflows/release.yml`) on push to `main`, followed by the manual `play-release` workflow for the Google Play bundle.

## Decision

Q1: A, "If A will also make the play release action create a valid google play version then Aversion".

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-27 | Commits on `feat/microg-maps`: bf6d68c (feature) and the AI-DLC record. Pre-commit passed (rules 195/195, unit tests, lint). |
| 2026-10-27 | PR https://github.com/optio/PebbleNTN/pull/116 opened. Checks passed: build (android, watchapp), validate, Analyze (java-kotlin), CodeQL. |
| 2026-10-27 | PR #116 merged as 67ec730. Issue #113 closed. |
| 2026-10-27 21:58:23 | Release **v0.0.78** published. It ships #114 (Play preparation, targetSdk 36, the new watch QR) and #113. |
| 2026-10-27 | `play-release` for `v0.0.78` (run 40591210375) succeeded. The `release-aab` artifact holds `app-release.aab`, versionCode 78, versionName 0.0.78, targetSdk 36, signed with the Play upload key (SHA-256 `D6:35:EB:62:…:C8:D4:50:C7`). Its checksum matches. |

## Marketplace Changelog (v0.0.78)

- Ready for Google Play: the app now targets Android 16, and installs from Google Play get their updates from Google Play.
- MicroG Maps (Google Maps patched for microG) is supported with all Google Maps rules, outside the Google Play version.
- The watch's "Install phone app" QR code now opens an install page that lists every source.

## Rollback

From a branch, open a PR with `git revert -m 1 67ec730`. Merging it publishes a release without #113. #114 stays, because it was merged earlier as faf8cb1. Nothing has been uploaded to Play yet.
