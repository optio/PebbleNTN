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

## REQ-DEBUG-010 — Per-element parse breakdown
The event detail SHALL present the normalized output broken down per watchface element — maneuver, distance, primary text, secondary text and ETA — with a key for each element, shown even when an element is empty. When the maneuver is UNKNOWN, the detail SHALL indicate that the watch renders the fallback glyph. The persisted rule-evaluation trace SHALL be displayed (rule id, layer and outcome per evaluated rule).

## REQ-DEBUG-011 — In-app contribution of unmatched captures
When notifications have been captured that no rule matched (apps without rules yet, or new notification shapes), the app SHALL surface a low-friction way for the user to contribute those diagnostics so support can be added. It SHALL show an indication (e.g. a dashboard prompt) whose visibility is driven by the count of such captures. When the user opens the contribution flow, the app SHALL let them choose what to share: the **privacy-safe redacted** dataset (REQ-DEBUG-005; free text such as road names and destinations removed, only structure — maneuver keywords, digits, units — and package name retained), which is the default; or the **full** dataset that retains the raw notification text **including street names**. The full option SHALL be presented with the privacy explanation (REQ-DEBUG-007) so the user understands it includes destinations, road names and route context, together with a note that it is more valuable because it lets maintainers add missing translations for direction/turn words in the user's language. Before sharing, the app SHALL let the user review the exact dataset for the chosen option. Sharing SHALL open the user's email app with the chosen diagnostics attached and the recipient and subject prefilled where the platform allows; nothing SHALL be transmitted automatically (REQ-DEBUG-006). If the attachment would exceed 10 MB it SHALL be truncated to the newest events that fit. All user-facing strings SHALL be Android resources.

**Acceptance:** Unit test that the capped export keeps only the newest events under the byte cap and reports truncation; unit test that the unmatched-capture count excludes matched events; emulator verification that a captured unmatched notification surfaces the prompt, that the review screen offers redacted vs full (with street names) and shows the exact chosen dataset, and that the share action opens an email app with the attachment.
