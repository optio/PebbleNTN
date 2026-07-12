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
