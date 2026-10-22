# Security Test Instructions — Watchapp version in the dashboard status

## Applicability

Not applicable. There is no security requirement, and no new input, storage or network access.

## Input Handling Note

- The shown value is the stored watchapp version from the paired watch, displayed as plain text in a `Text` composable, at most two lines with an ellipsis.
- The value is never executed and never used in a link.
