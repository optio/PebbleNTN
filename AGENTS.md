# AI Implementation Contract

This file applies to Claude Code, Codex, and other autonomous coding agents.

## Mission

Implement PebbleNTN exactly as specified. Do not redesign the product, substitute technologies, silently reduce scope, or invent external service behavior.

## Mandatory operating rules

1. Read the specification and requirement files before changing code.
2. Work milestone-by-milestone in `spec/800-roadmap/Milestones.md`.
3. Keep Android and watch builds green after every completed milestone.
4. Add or update automated tests for every behavior change.
5. Never edit generated protocol files directly.
6. Never introduce executable remote code. Remote rules are declarative data only.
7. Never inspect notification text before the package allowlist check.
8. Never store notifications from disabled packages.
9. Never automatically upload notification content.
10. Do not commit secrets, signing keys, tokens, account credentials, local SDK paths, or unredacted private captures.
11. No placeholder implementations, fake success paths, commented-out production code, or unresolved TODOs in completed milestones.
12. If an SDK/API dependency is unavailable, isolate it behind the specified interface and provide a compiling test double. Record the blocker in `docs/IMPLEMENTATION_STATUS.md`; do not fabricate an API.
13. Prefer small modules and pure functions. Parsing must be deterministic.
14. All rule changes require fixtures and regression tests.
15. All database migrations require migration tests.
16. All user-facing strings must use Android resources.
17. Accessibility labels and scalable text are required.
18. Run the definition-of-done commands before declaring a milestone complete.

## Required validation commands

```bash
./scripts/validate-spec-assets.sh
./android/gradlew -p android test lint assembleDebug
./scripts/validate-rules.sh
./scripts/build-watchapp.sh
```

When Android instrumentation infrastructure is available:

```bash
./android/gradlew -p android connectedDebugAndroidTest
```

## Commit discipline

Each commit should represent one coherent change and mention relevant requirement IDs. Example:

```text
feat(rules): add deterministic Google Maps distance extraction

Implements REQ-RULE-021, REQ-RULE-022.
```
