# Log Queries: "When driving" (#132)

## Diagnostics export (`drivingLog`, schema 6)

From a user's export JSON (privacy-safe or full):

```bash
# hide/show decisions with the speeds behind them
jq '.drivingLog[] | select(.decision != "SAMPLE")' export.json
# measured speed over time, to check a wrong guess against the threshold
jq -r '.drivingLog[] | select(.decision == "SAMPLE") | [.timeMillis, .measuredKmh, .plannedKmh, .thresholdKmh] | @tsv' export.json
```

## Logcat (debug builds, adb; Timber's DebugTree tags lines with the class name)

```bash
adb logcat -s NavigationController:* | grep -E "Driving at about|No longer driving|Hidden while driving|opened while hidden"
```

Lines written by `NavigationController`: "Driving at about N km/h: closing the watchapp", "No longer driving: reopening the watchapp", "No longer driving: the watchapp stays closed until it is opened (auto-launch off)", "Hidden while driving: state … not sent", "Watchapp opened while hidden for driving: it stays open for this navigation".
