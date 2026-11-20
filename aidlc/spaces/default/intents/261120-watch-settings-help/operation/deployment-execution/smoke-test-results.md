# Smoke Test Results: Watch settings help text (#139)

| Check | Expected | Result |
|---|---|---|
| GitHub release exists | `v0.0.84` with APK, PBW and checksums | Pass |
| Release notes | mention the #139 feature | Pass ("help text for every Watch settings row (#139)") |
| Version on `main` | `VERSION` 0.0.84 after the release commit | Pass |
| CI on the merge | release, android, codeql green | Pass |
| Play bundle | `play-release` green, AAB artifact present | Pass (4,515,367 bytes) |
| Hand-off integrity | the downloaded AAB's SHA-256 equals the workflow's `.sha256` | Pass |
| Issue closed | #139 closed by the PR | Pass |
| On-device check | help lines visible, value-list explanation, shortcut descriptions, TalkBack, largest font, a setting still reaches the watch | **Not run by me.** It needs a real phone and the watch, which only you can check. The steps are in `construction/build-and-test/integration-test-instructions.md`. The same targets passed in the Robolectric tests. |

Overall: every automated smoke check passes. The on-device check is still to do.
