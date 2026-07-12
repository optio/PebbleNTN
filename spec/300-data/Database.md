# Database Specification

## Tables

### `supported_app_settings`
- `appId` primary key
- `packageName`
- `enabled`
- `captureUnmatched`
- `firstSeenAt`
- `updatedAt`

### `notification_debug_event`
- auto ID
- event timestamp
- received timestamp
- package name
- notification key hash
- notification ID
- tag hash, nullable
- channel ID
- event type
- selected raw snapshot JSON
- active ruleset versions
- matched rule ID
- extraction JSON
- trace JSON
- disposition
- transport status
- privacy classification

### `user_rule`
- rule ID primary key
- source rule ID nullable
- package name
- canonical JSON
- enabled
- created/updated timestamps
- validation status

### `official_ruleset`
- version primary key
- source bundled/downloaded
- schema version
- signature status
- activation status
- installed timestamp
- payload hash

### `navigation_state`
- singleton ID
- session ID
- active flag
- normalized state JSON
- state timestamp
- watch launch status
- watch readiness status

## Retention

Default debug retention is 500 eligible events. The user may select 50, 100, 500, 1,000, or unlimited with a warning. Retention cleanup occurs transactionally after insert and through periodic maintenance.

## Privacy

Only enabled-package events enter the database. Notification keys and tags are hashed when their literal values are not needed. PendingIntents, actions, RemoteViews, icons, contact identifiers and arbitrary bundles are never serialized.
