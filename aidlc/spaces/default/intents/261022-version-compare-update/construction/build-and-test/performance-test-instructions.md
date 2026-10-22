# Performance Test Instructions — Version-compare update detection

## Applicability

Not applicable. This work has no performance requirement, and the test strategy is Minimal.

## Related Target

The only resource target is flash wear (NFR3):
- **Watch:** it adds no persist writes. The effective version is derived at runtime from keys 16 and 17 and its own version.
- **Phone:** it writes `watch_app_version` only when the value changes. `UpdateCheckRepositoryTest` checks this ("same value again → no write").
