# Alarms: Watch settings help text (#139)

There are no automated alarms, because the app sends nothing home. These signals act as alarms:

| Signal | Threshold | Severity | Response |
|---|---|---|---|
| Play Console Android vitals (crash rate, ANR rate) | above Play's bad-behaviour thresholds (1.09 % crashes, 0.47 % ANRs) | high | Read the stack traces. A crash in `WatchSettingsScreen`, `OptionListScreen`, `DrivingSection` or `WatchSettingsUi` (for example a missing resource) needs a hotfix release. |
| GitHub issue: a help text is wrong, unclear or contradicts the watch | any | low | Check the text against the watchapp code (wording table in `construction/code-generation/code-summary.md`), fix `strings.xml`, and ship in the next release. |
| GitHub issue or tester report: text cut off or overlapping | any | medium | Ask for a screenshot with font and display size, fix the row layout, release. |
| Release, android or play-release workflow failure on `main` | any red run | medium | Fix it before the next release. |

Escalation: the single maintainer, through GitHub issues and the Play Console.
