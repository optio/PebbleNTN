# Security Test Instructions — #94

## Applicability

Not applicable. There is no security requirement for this work, and the test strategy is Minimal.

## Input Handling Note

- **Watch:** the new input is the phone's `LATEST_PHONE_VERSION` string (key 14). It comes from the phone app the user installed. Unit tests check its handling:
  - a missing or empty value keeps the stored version;
  - the value is capped at 16 bytes and always NUL-terminated;
  - in the glance template, `{`, `}` and `\` are dropped, so a version can't inject template syntax.
  
  The tests are in `watchapp/tests/test_update_notice.c` and `watchapp/tests/test_glance_text.c`.
- **Phone:** no new network access. The version comes from the existing update check, and weekly checks stay off by default (NFR2). `UpdateCheckRepositoryTest` checks this.
