# Security Test Instructions — Settings sync (#102)

The feature adds no network access, no accounts and no personal data. The new input surface is the `SETTINGS` message from the phone, and the watch's own persisted bytes.

- **Input validation on the watch:** every incoming value is checked against its range and the watch's capabilities before it is stored. Out-of-range values, colour accents on black-and-white watches, RGB-only values without an RGB backlight, and touch values without touch are ignored (`test_settings_sync`).
- **Revision:** values outside 0–99 are rejected; the wrap-around compare is host-tested at 0, 1, 49, 50, 51 and the 99 → 0 rollover (`test_revision`, `WatchSettingsSyncTest`).
- **Phone side:** incoming watch settings are validated in `WatchSettingsModel` before they enter the mirror; a synthetic READY never updates it (`ProtocolCodecTest`).
- **Launch policy:** settings never relaunch the watchapp (`sendToOpenApp`, `WatchSettingsQueueTest`), so the phone can't pop the app up on the wrist (REQ-WATCH-006).
