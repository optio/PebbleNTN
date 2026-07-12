# Coding Standards

## Kotlin

- Kotlin official style.
- Immutable data classes.
- Sealed interfaces for domain events/results.
- Explicit error types instead of broad exceptions.
- Public APIs documented with KDoc.
- No `GlobalScope`.
- No blocking I/O on main thread.
- No Android framework types in the pure rule engine.
- Coroutines and Flow for asynchronous streams.
- Version catalog for dependencies.

## Compose

- Unidirectional data flow.
- Stateless reusable components where possible.
- Screen state represented by immutable models.
- Navigation isolated from screen content.
- Previews use synthetic data only.

## C

- Compile with warnings treated seriously.
- Bounded string operations.
- Validate every tuple and type.
- No dynamic allocation unless justified.
- Keep renderer independent from AppMessage decoding.
- Generated protocol header is read-only.

## JSON and rules

- Schema-valid and canonical.
- No hand-edited duplicated protocol constants.
- Fixtures are sanitized and have expected results.
- Rule IDs stable.

## Error handling

User-visible failures provide an action:
- grant notification access;
- install/open watchapp;
- check connection;
- view invalid rule;
- roll back ruleset;
- export diagnostics.
