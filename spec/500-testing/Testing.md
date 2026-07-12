# Testing Strategy

## Pyramid

1. Pure JVM tests for rule engine, reducers, serializers and quantization.
2. Room and Compose instrumentation tests.
3. Synthetic notification publisher tests.
4. Google Maps emulator capture sessions.
5. Pebble emulator rendering tests.
6. Physical Android + Pebble end-to-end release tests.

## Mandatory test categories

- disabled package never reads/stores content;
- app catalog defaults;
- notification snapshot field extraction;
- locale handling;
- every rule operator and extractor;
- malformed/empty/oversized inputs;
- regex limits and timeout;
- layer precedence;
- canonical JSON;
- debug retention;
- privacy redaction;
- import/export roundtrip;
- rule preview trace;
- navigation session transitions;
- launch once;
- READY handshake;
- pending state replacement;
- stop and exit flag;
- protocol compatibility;
- database migrations.

## Golden tests

Watch layouts use screenshot/golden comparison where the Pebble toolchain permits deterministic captures. Android Compose screens may use screenshot tests if stable in CI; semantic tests are mandatory regardless.
