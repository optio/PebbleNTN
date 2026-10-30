# Deployment Log — install/update source and a second MicroG Maps package (v0.0.80)

## Decision

Q1: A. Push, PR, merge, which publishes v0.0.80.

## Timeline (UTC)

| Time | Event |
|---|---|
| 2026-10-30 | Commit fc7aa7c on `feat/install-source`. Pre-commit passed. |
| 2026-10-30 | PR https://github.com/optio/PebbleNTN/pull/121 opened. Checks passed: build, validate, Analyze (java-kotlin), CodeQL. Merged as 5c18c08; closes #119. |
| 2026-10-30 06:53:17 | Release **v0.0.80** published: APK and PBW, versionCode 80, targetSdk 36, signed with the release key `75:EF:A3:EA:…`. |
| 2026-10-30 | `play-release` for `v0.0.80` (run 41016789430) succeeded. AAB signed with the upload key `D6:35:EB:62:…` (SHA-1 `BC:DA:23:74:…`). Copied to `C:\Users\sacha\Downloads\pebblentn-0.0.80.aab`. |

## Marketplace Changelog (v0.0.80)

- Updates now shows where the phone app came from, for every source: "updated by F-Droid", "installed manually", and so on.
- Debug exports include the install details (source, installer, install and update times).
- MicroG Maps: the `org.ungoogled.android.apps.maps.microg` build is recognised too.

## Rollback

From a branch, open a PR with `git revert -m 1 5c18c08`. Merging it publishes a release without this change.
