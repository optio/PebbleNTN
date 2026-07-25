# Android Requirements

## REQ-ANDROID-001 — Platform
The application SHALL be Kotlin-based, use Compose Material 3, have minSdk 31, and use a release-time-verified Play-compatible target SDK.

**Acceptance:** Gradle configuration and CI build succeed on the documented JDK/SDK.

## REQ-ANDROID-002 — System listener
The app SHALL use `NotificationListenerService` and SHALL NOT require a permanent foreground service for normal operation.

**Acceptance:** Eligible notifications are received while the activity is closed.

## REQ-ANDROID-003 — Early package filter
The listener SHALL inspect only source metadata needed for allowlisting before deciding eligibility. It SHALL NOT read notification content for disabled packages.

**Acceptance:** Instrumented spy proves snapshot factory, parser and database are never called for a disabled package.

## REQ-ANDROID-004 — Default app enablement
Every installed app represented by the bundled navigation-app catalog SHALL be enabled on first discovery by default. The user MAY disable each app.

**Acceptance:** Fresh-install test with mocked installed packages.

## REQ-ANDROID-005 — Capture-only apps
Catalog apps without official rules MAY capture debug notifications when enabled but SHALL be labeled capture-only and SHALL NOT claim valid watch output.

## REQ-ANDROID-006 — Event processing
Processing SHALL be serialized per application process and SHALL not block the main callback thread with database or rule evaluation work.

## REQ-ANDROID-007 — No polling
The app SHALL be event-driven and SHALL NOT poll notification state, Pebble connection, or GPS continuously.

## REQ-ANDROID-008 — Debug retention
Default eligible-event retention SHALL be 500 with configurable limits and delete-all.

## REQ-ANDROID-009 — Settings
Settings SHALL include per-app enablement, auto-launch, return-to-watchface, vibration, optional backlight, retention and export privacy default.

## REQ-ANDROID-010 — Process recovery
The app SHALL restore latest state safely and SHALL never replay obsolete turn events as a queue.

## REQ-ANDROID-011 — Master switch
The app SHALL provide a global enable/disable toggle, enabled by default. While disabled the app SHALL NOT read notification content, SHALL NOT store events, and SHALL NOT send state to the watch; the check SHALL happen before notification content is accessed. Disabling SHALL end any navigation currently shown on the watch. The setting SHALL persist across restarts.

**Acceptance:** Dispatcher unit test proving the content builder is never invoked while disabled (even for an allowlisted package); persistence test across repository instances.

## REQ-ANDROID-012 — Listener refresh
The app SHALL provide a user-initiated action that re-establishes the `NotificationListenerService` binding without a full application restart, for the case where Android has silently stopped delivering notifications to the listener while navigation is active. The action SHALL request a rebind (`NotificationListenerService.requestRebind`) and SHALL force a fresh bind by toggling the listener component's enabled state (`PackageManager` disable then enable with `DONT_KILL_APP`), ending with the component enabled; the notification-access grant SHALL be preserved across the toggle. The action SHALL be presented on the dashboard with an explanation of when to use it (navigation running on the phone but not reaching the watch), and SHALL confirm to the user that it ran. This is a recovery action only: it SHALL NOT read notification content itself and SHALL NOT change the protocol.

**Acceptance:** Unit test (Robolectric) proving the refresh leaves the listener component enabled, including recovery from a previously-disabled component. User-facing strings are Android resources; the dashboard button shows an explanation and a confirmation.

## REQ-ANDROID-013 — App update check
The app SHALL detect when a newer version of itself is available by reading the latest release tag from the project's GitHub Releases API and comparing it against the installed `versionName`. The automatic weekly check SHALL be opt-in via a user setting that is **off by default**; when enabled it SHALL run at most once per week (on app start, only if the last check was more than a week ago). An on-demand "Check for updates" control SHALL always be available regardless of the setting, as an explicit user action. With the setting off and no manual check, the app SHALL make no network connection of its own. Because `INTERNET` is a normal (install-time) Android permission with no runtime prompt, the opt-in setting — not a permission request — is what gates automatic network access. The check SHALL be best-effort: any failure (offline, error, throttling) SHALL be silent and retain the last known result. This check is the app's only network access; it SHALL send no user or notification data — only an unauthenticated GET of public release metadata — and SHALL NOT upload anything (REQ-SEC-002). The last-check time and last-known latest version SHALL persist. When the installed version is lower than the latest, the app SHALL show a prompt with the available and installed versions, a control to download the latest release from GitHub, and guidance to uninstall and reinstall if installing over the top fails. The manual check SHALL confirm its outcome to the user. Version comparison SHALL tolerate a leading `v`, differing component counts and pre-release suffixes, and an unparseable remote tag SHALL never be treated as newer. All user-facing strings SHALL be Android resources, and the network access SHALL be reflected in the disclosure.

**Acceptance:** Unit tests for the version comparison (leading `v`, differing lengths, pre-release suffix, unparseable tag) and for the repository (weekly throttle skips within a week, forced check bypasses it, update-available detection, failure retains last known, persistence across instances, auto-check default off). Emulator verification that a manual check finds a newer release and shows the prompt with a working download link and reports its outcome, and that with auto-check off no network call is made on launch.
