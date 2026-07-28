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
- shortcuts to debug history and rules;
- a "Refresh app" recovery action that reconnects the notification listener (REQ-ANDROID-012), with an explanation of when to use it (navigation running on the phone but not reaching the watch) and a confirmation that it ran;
- when captures exist that no rule matched, a prompt to contribute those diagnostics (REQ-DEBUG-011), leading to the share-to-help screen;
- a "Check for updates" control (always available), an opt-in "check weekly" toggle that is off by default, and, when a newer release exists, an update prompt with the available/installed versions, a download-latest action, and uninstall/reinstall guidance (REQ-ANDROID-013);
- the dashboard scrolls, so its cards and controls remain reachable as they accumulate.

### Share to help add support
- reached from the dashboard prompt when unmatched captures exist;
- lets the user choose what to share: the redacted (privacy-safe) dataset, the default, or the full dataset that keeps street names — the latter presented with the privacy explanation and a note that it is more valuable for adding missing direction/turn-word translations;
- shows the exact chosen dataset for review before sharing; nothing is sent automatically;
- shares by opening the user's email app with the chosen diagnostics attached and recipient/subject prefilled where the platform allows, truncated to the newest 10 MB if larger.

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
