# Security Test Instructions — #103

## Applicability

Not applicable. There is no security requirement for this work, and the test strategy is Minimal.

## Input Handling Note

The only new input is the phone's `APP_VERSION` string, from the phone app the user installed. Its handling is checked by unit tests:
- Only a non-empty C string is accepted.
- It is capped at 16 bytes and always NUL-terminated (`phone_version_update`).
- It is formatted with `snprintf` into a 128-byte buffer.

Those tests live in `watchapp/tests/test_phone_version.c` and `watchapp/tests/test_about_text.c`.
