# Alarms: "When driving" (#132)

No automated alarms: the app sends nothing home. The signals that act as alarms, and the response:

| Signal | Threshold | Response |
|---|---|---|
| Play Console Android vitals (crash rate, ANR rate) | above Play's bad-behaviour thresholds (1.09 % crashes, 0.47 % ANRs) | read the stack traces; a crash in `DrivingSpeedEstimator`, `DrivingPolicy` or `NavigationController` → hotfix release |
| GitHub issues about the watchapp closing on foot or bike, or not reopening | any | ask for a privacy-safe diagnostics export; read `drivingLog`; tune constants or fix; users can switch back to Show directions meanwhile |
| Closed-test feedback | any report about When driving | same as above |
| Release workflow failure on `main` | any red run | fix before the next release (no user impact until a release is published) |
