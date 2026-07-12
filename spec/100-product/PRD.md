# Product Requirements Document

## Users

1. Pebble owner who wants turn indications without keeping the phone screen visible.
2. Tester who captures unknown or changed notification formats.
3. Project maintainer who converts captures and community submissions into official rules.
4. Contributor who proposes rules through a GitHub pull request.

## Primary journey

1. User installs Android app and Pebble watchapp.
2. User opens Android app and grants notification-listener access.
3. App discovers installed navigation apps represented in the supported-app catalog.
4. Defined installed apps are enabled by default.
5. User starts Google Maps navigation.
6. Listener receives a notification event and checks package metadata.
7. Rules extract maneuver, distance, road, instruction and optional ETA.
8. Phone starts the watchapp once for the session, waits for READY, then sends current state.
9. Watch shows bitmap arrow, distance and road; maneuver changes may vibrate.
10. When navigation ends, the watch returns to the watchface unless configured otherwise.

## Secondary journey: rule debugging

1. An eligible app emits an unmatched or wrongly parsed notification.
2. The debug view shows raw selected fields, timestamp, match trace and extracted output.
3. User opens the rule test bench and chooses the captured item.
4. Simple editor changes conditions/extractors and updates underlying JSON.
5. Expert editor may edit validated JSON directly.
6. Preview reruns rules without restarting navigation.
7. User exports rules only, a privacy-safe diagnostic, or an explicitly confirmed full diagnostic.
8. Maintainer imports the export into the developer rule workbench, compares results, writes fixtures and promotes a reviewed rule.

## Non-goals

- Route calculation.
- GPS tracking by PebbleNTN.
- Map display on watch.
- Reading arbitrary notifications for analytics.
- iOS support.
- Uploading debug data without explicit user action.
- Arbitrary scripting in rules.
- Guaranteed extraction for capture-only apps.
