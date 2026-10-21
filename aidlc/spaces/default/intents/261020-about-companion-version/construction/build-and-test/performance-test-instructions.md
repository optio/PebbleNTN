# Performance Test Instructions — #103

## Applicability

Not applicable. There is no performance requirement for this work, and the test strategy is Minimal.

## Related Target

The only resource-related target is flash wear: NFR2 allows at most one persist write per distinct version. It is checked by a unit test (`watchapp/tests/test_phone_version.c`, "same version again is not rewritten" and "cut value seen again is not rewritten").
