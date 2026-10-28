# Deployment Log — MicroG arrows and user-added package names (v0.0.79)

## Decision

Q1: A. The person approved "move on to deployment (release v0.0.79)".

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-28 | Commits on `fix/microg-icon-fallback`: 17c3535 (feature) and the AI-DLC record. Pre-commit passed. |
| 2026-10-28 | PR https://github.com/optio/PebbleNTN/pull/118 opened. Checks passed: build, Analyze (java-kotlin), CodeQL. Merged as 440eb50. |
| 2026-10-28 20:28:50 | Release **v0.0.79** published: APK and PBW, versionCode 79, targetSdk 36, signed with the release key `75:EF:A3:EA:…`. |
| 2026-10-28 | `play-release` for `v0.0.79` (run 40763784925) succeeded. AAB version 0.0.79, signed with the upload key `D6:35:EB:62:…` (SHA-1 `BC:DA:23:74:…`), checksum OK. Copied to `C:\Users\sacha\Downloads\pebblentn-0.0.79.aab`. |

Play accepts this bundle from **2026-10-30 00:14 UTC**, when the reset upload key becomes valid.

## Marketplace Changelog (v0.0.79)

- Add your own package names per navigation app (Navigation apps → Packages), for builds such as MicroG Maps that use a different package name.
- MicroG Maps: turn arrows on icon-only cards are recognised more reliably.
- Debug exports now show which package a notification came from and whether you added it.

## Rollback

From a branch, open a PR with `git revert -m 1 440eb50`. Merging it publishes a release without this change.
