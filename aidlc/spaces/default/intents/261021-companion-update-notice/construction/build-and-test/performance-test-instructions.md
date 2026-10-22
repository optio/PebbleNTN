# Performance Test Instructions — #94

## Applicability

Not applicable. There is no performance requirement for this work, and the test strategy is Minimal.

## Related Target

The only resource target is flash wear (NFR3): persist keys 17 and 18 are written only when their value changes. It is checked by host unit tests in `watchapp/tests/test_update_notice.c`, for example "same again → no change" and "clear again → no change".
