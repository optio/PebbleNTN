# Android Technical Design

## Service lifecycle

Use `NotificationListenerService`; do not run a permanent foreground service for ordinary listening. The service performs only lightweight metadata filtering on the callback thread, creates an immutable snapshot for enabled packages, and dispatches processing to an application coroutine scope.

## Early filter

```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    if (!enabledAppRepository.isEnabled(packageName)) return
    processingDispatcher.enqueue(NotificationEvent.Posted(sbn))
}
```

No notification title, text, extras, RemoteViews, actions, or content intents may be read before the allowlist decision.

## Supported-app catalog

Bundled declarative catalog entries define:
- stable app ID;
- package names;
- display name;
- capture availability;
- official-rules availability;
- optional channel hints;
- default enabled policy;
- optionally the app whose rules it uses (`rulesFrom`, its rule family, REQ-RULE-015), a `website`
  linked instead of Google Play, and whether it is listed on Google Play installs (`playListed`,
  REQ-ANDROID-023).

On a Google Play install the app uses the catalog's Google Play view: an entry that is not listed
there is dropped, and its packages join the entry whose rules it uses, so they follow that entry's
switch. An entry that is not listed there and uses no other app's rules (Vela Maps, #120) is kept
for matching, enablement and the listener allowlist but hidden from every screen.

Installed entries are shown first. All installed catalog apps default to enabled on first run. Users can disable them independently.

## Persistence

Use Room for:
- debug events;
- user rules;
- active official downloaded rules metadata;
- app enablement;
- settings;
- cached latest navigation state;
- rule-test run summaries.

Use DataStore for small preferences only if it materially simplifies settings; Room remains authoritative for rule and debug entities.

## Concurrency

- One serialized processing queue for notification events.
- Pure rule evaluation may run on `Dispatchers.Default`.
- Room I/O on `Dispatchers.IO`.
- UI observes Flows.
- Pebble callbacks are converted into typed domain events.
- No global mutable singleton state.

## Process restoration

Persist the current session state and latest instruction. On service reconnect:
- query active notifications only for enabled packages when API behavior permits;
- reconstruct current state;
- never replay an ordered backlog;
- send only current state after watch READY.

## Logging

Use Timber in debug builds. Release logging must omit raw notification content. Debug history is an explicit product feature and separate from logcat.
