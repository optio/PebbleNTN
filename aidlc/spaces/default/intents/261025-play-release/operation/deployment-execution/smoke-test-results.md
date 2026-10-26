# Smoke Test Results — #114 merge (no release)

| Check | Result |
|---|---|
| `origin/main` head is the merge commit faf8cb1, with `[skip ci]` in its subject | Pass |
| No workflow run was created for faf8cb1 | Pass (`gh run list --branch main`) |
| Latest GitHub release is still v0.0.77 | Pass |
| versionCode / versionName on `main` | Pass: 77 / 0.0.77 |
| Issue #114 closed by the merge | Pass |

## Not Covered

There is no published artifact to smoke-test. The app was checked on the maintainer's Android 16 phone before the merge.
