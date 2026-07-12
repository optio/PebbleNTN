# Android UI Specification

## Design language

Material 3 with subtle Pebble styling:
- monochrome or low-saturation surfaces;
- bitmap-inspired maneuver previews;
- compact status chips;
- no imitation of copyrighted third-party navigation UI;
- system dynamic color optional, with a Pebble monochrome theme available.

## Screens

### Onboarding
- product explanation;
- prominent notification-access disclosure;
- open system notification-listener settings;
- detect granted state;
- Pebble/watchapp setup guidance;
- privacy statement.

### Dashboard
- notification access status;
- watch connection/readiness;
- current navigation state;
- last eligible notification timestamp;
- active ruleset version;
- shortcuts to debug history and rules.

### Navigation Apps
- catalog apps grouped as installed/uninstalled;
- installed defined apps enabled by default on first discovery;
- per-app enable toggle;
- capture-only badge when no official rules;
- warning that capture-only does not provide navigation output;
- enable all / disable all.

### Debug History
- latest events first;
- filters: app, matched, unmatched, failed, sent;
- timestamp, source, event type and result summary;
- detail screen with raw selected fields, normalized output, trace and transport result;
- re-run using current or selected ruleset;
- create rule from event;
- export selected items;
- delete item/all.

### Rules
- tabs: bundled official, downloaded official, user;
- immutable official rule details;
- clone-to-user action;
- enable/disable user rule;
- simple editor;
- expert JSON editor;
- validation errors;
- rule priority;
- test bench and result diff.

### Settings
- automatic watchapp launch;
- return to watchface when navigation ends;
- vibrate on maneuver change;
- backlight option;
- debug retention;
- unmatched capture per app;
- remote official rule checks, hidden or disabled in initial release;
- export privacy default;
- delete all local data;
- licenses and privacy policy.

## Accessibility

All icons have semantic descriptions. Information is never conveyed by color alone. Touch targets meet Material guidance. Text supports font scaling.
