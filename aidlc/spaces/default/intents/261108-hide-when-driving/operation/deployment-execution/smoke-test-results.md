# Smoke Test Results: "When driving" (#132)

| Check | Expected | Actual | Result |
|---|---|---|---|
| GitHub release | `v0.0.82` with APK, PBW, checksums | `v0.0.82`: `pebble-ntn.apk`, `pebble-ntn.pbw`, `SHA256SUMS.txt` | pass |
| Release workflow | success on the merge commit | run 42538364335 success on `be05f56` | pass |
| Watchapp version | PBW `versionLabel` 0.0.82 | 0.0.82 | pass |
| Play AAB | built from `v0.0.82`, versionName 0.0.82 | `app-release.aab`, versionName 0.0.82 | pass |
| Play upload key | upload certificate SHA-1 `BC:DA:23:74:…:5E:81` | `BC:DA:23:74:47:BD:14:6E:75:50:C2:E8:45:5B:ED:75:F5:A3:5E:81` | pass |
| On-device check (When driving section, watch row, default behaviour unchanged) | as in `../deployment-pipeline/deployment-strategy.md` | to be done by the person after installing 0.0.82 and the new watchapp | pending (person) |
