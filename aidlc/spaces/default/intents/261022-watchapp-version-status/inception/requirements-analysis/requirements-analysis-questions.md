# Requirements Analysis Questions — Watchapp version in the dashboard status

Context:
- **The request:** show the watchapp version (last seen) in the dashboard's Status section, from the `watch_app_version` the phone stores when the watch's WATCH_READY arrives (v0.0.73). Show "not received yet" when nothing is stored.
- **Its purpose:** to check on a device whether the Core Devices companion forwards watch→phone messages.
- **Current Status section:** "PebbleNTN on" switch, notification access, Watch (connected), Last navigation notification, Last sent to watch.
- **A limit:** the stored version has no time and is never cleared, so on its own it only proves that a message arrived at least once.

## Q1. [Product] What should the row show?

A. The version only: "0.0.73 (last seen)", or "Not received yet"
B. The version and when it last arrived: "0.0.73 · last seen 22 Oct 2026, 21:30", or "Not received yet". This needs the time stored with each received WATCH_READY, so opening the watchapp visibly updates the row, which makes the test unambiguous
C. As B, plus a short hint under "Not received yet": "Open PebbleNTN on the watch. If this stays empty, your Pebble app doesn't pass watch messages to PebbleNTN."
X. Other (please specify)

[Answer]: A

## Q2. [Product] Where in the Status section should the row go, and what is its title?

A. Directly under the "Watch" row, titled "Watchapp version"
B. At the bottom of the Status section, titled "Watchapp version"
X. Other (please specify)

[Answer]: A
