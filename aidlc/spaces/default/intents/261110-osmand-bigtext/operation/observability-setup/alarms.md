# Alarms: OsmAnd street and arrival time (#134)

There are no automated alarms, because the app sends nothing home. These signals act as alarms:

| Signal | Threshold | Response |
|---|---|---|
| Play Console Android vitals (crash rate, ANR rate) | above Play's bad-behaviour thresholds (1.09 % crashes, 0.47 % ANRs) | Read the stack traces. A crash in `RuleEngine`, `ArrivalClock`, `ProtocolCodec` or `NavigationController` needs a hotfix release. |
| GitHub issue: OsmAnd road line wrong or cut off in a language, or a wrong arrival time | any | Ask for a privacy-safe diagnostics export. Fix the rule pack and release it through the remote rules; no app release is needed. Meanwhile the user can switch Directions text to Compact. |
| Route-capture run with unrecognised OsmAnd shapes | any new shape | Add a rule or a `matched: false` fixture. |
| Closed-test feedback | any report about road lines or arrival times | Same as the GitHub issue row. |
| Release or rules workflow failure on `main` | any red run | Fix it before the next release. |
