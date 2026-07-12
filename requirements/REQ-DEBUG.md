# Debug and Maintainer Requirements

## REQ-DEBUG-001 — History detail
Each retained eligible event SHALL show timestamp, package, event type, selected raw fields, active ruleset, match result, normalized output, trace and transport result.

## REQ-DEBUG-002 — No unrelated history
No debug record SHALL be created for a disabled package.

## REQ-DEBUG-003 — Rule creation
A user SHALL be able to start a new rule from a captured event.

## REQ-DEBUG-004 — Re-run
A retained event SHALL be rerunnable against current, bundled, downloaded or selected candidate rules.

## REQ-DEBUG-005 — Export modes
The app SHALL provide rules-only, privacy-safe diagnostic and full diagnostic exports.

## REQ-DEBUG-006 — Explicit sharing
The app SHALL use the Android Sharesheet and SHALL never automatically transmit diagnostics.

## REQ-DEBUG-007 — Privacy explanation
Before diagnostic export, the app SHALL explain that private content is limited to what the navigation app displayed in its notification, while clearly listing examples of private information that notification may contain.

## REQ-DEBUG-008 — Developer workbench
The repository SHALL include a CLI capable of inspecting exported JSON and read-only Room databases, testing candidate rules, generating diffs, sanitizing fixtures and producing regression reports.

## REQ-DEBUG-009 — Community contribution
The maintainer workflow SHALL support GitHub pull requests containing rules, fixtures, expected outputs and generated regression reports.
