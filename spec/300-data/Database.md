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
- active ruleset versions: the bundled rulesets holding rules for the package, comma-separated (REQ-DEBUG-012)
- matched rule ID
- extraction JSON
- trace JSON
- disposition
- transport status
- privacy classification
- PebbleNTN version name and code, nullable (schema v7, REQ-DEBUG-012)
- source navigation app version name and code, nullable (schema v7, REQ-DEBUG-012)
- group key, occurrence count (default 1), latest event and received timestamps, latest snapshot JSON (schema v8, REQ-DEBUG-013)

#### Grouping (REQ-DEBUG-013)
A new event whose group key equals the most recently updated event's key is not inserted: that event's occurrence count is incremented and its latest timestamps and snapshot are replaced, in one transaction. The key is a SHA-256 of the package, channel, category and template, the text fields with every number replaced by `N`, the matched icon and the icon-match diagnostic (numbers replaced), the disposition and matched rule, and the PebbleNTN, navigation-app and ruleset versions. Only consecutive repeats group. Events order and trim by their latest received time.

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

Default debug retention is 1,000 eligible events. The user may select 50, 100, 500, 1,000, or unlimited with a warning. Consecutive repeats of the same card are one event (see grouping below), so the limit counts distinct cards in a row, not raw notifications. Retention cleanup occurs transactionally after insert and through periodic maintenance.

## Privacy

Only enabled-package events enter the database. Notification keys and tags are hashed when their literal values are not needed. PendingIntents, actions, RemoteViews, icon images, contact identifiers and arbitrary bundles are never serialized. For apps whose rules map icons, the snapshot JSON carries only the matched turn drawable's name (`iconDrawable`) and a match diagnostic (`iconMatch`), never the image (REQ-SEC-003).
