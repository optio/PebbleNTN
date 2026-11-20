# Deployment Strategy: Watch settings help text (#139)

## Strategy

This is a mobile app, released as a whole new version (0.0.84) through the existing channels. There are no servers, so blue/green and canary do not apply.

- **What users see after updating:** on Dashboard → Watch settings, every row has a short help line under its value. Each value list starts with a fuller explanation, and the shortcut actions have descriptions. The Directions note under its card is gone; its meaning is now in the row help. Nothing else changes: settings, sync and the watch behave as before.
- **No feature flag:** the change is static text with no behaviour risk, so it isn't gated. The app has no backend for remote flags, by design.
- **Compatibility:** the change works with every watchapp that sends its settings (protocol 1.3+). It changes no message and no stored data, so mixed phone and watch versions are fine.

## Promotion

| Stage | Who | Gate |
|---|---|---|
| PR | CI | all checks green |
| GitHub release (Obtainium, direct APK) | merge to `main` | the person's merge |
| F-Droid | F-Droid build server | reproducible build of the tag |
| Play closed test (Alpha) | the person, via `play-release` and Play Console [Q1: A] | Google's review |
| Play production | later, once the closed test reaches 12 testers × 14 days | the person and Google's review |
| Pebble appstore (watchapp, version-only) | the person, optional | upload in the developer portal |

## Smoke test after release

- The GitHub release `v0.0.84` has the APK and `watchapp.pbw`. The release notes mention the help texts.
- **On a phone with the watchapp open:**
  - Watch settings shows a help line under each row;
  - tapping Backlight duration shows the fuller explanation, mentioning battery;
  - Up button lists each action with a description;
  - changing a setting still reaches the watch.
- **With TalkBack on:** each row is read as title, value, then help.
