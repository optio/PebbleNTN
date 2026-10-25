# Build and Test Summary — Settings sync (#102)

## Status: Ready

- **Build:** all five watch platforms and the debug and release APKs build. aplite has 3597 B free at load.
- **Tests:** 12 watch host suites (317 assertions) pass; `scripts/test-all.sh` passes with 479 Android tests per variant plus lint.
- **Emulator:** FR2a/b, FR3, FR8 and the aplite memory target pass on basalt and aplite.
- **Traceability:** pass, FR1–FR9.
- **Gate changes:** new defaults, About first and larger, Watch settings row first; an aplite fault in the Shortcuts action list found and fixed (see test-results.md).

## Quality Targets

| Target | Result |
|---|---|
| Minimal strategy: one test per unit-testable requirement, plus a happy path per component | Met: FR1, FR3–FR8 in unit tests; FR2 in the emulator |
| Existing suite stays green | Met |
| aplite: no worse than v0.0.76 − 500 B with Settings and a submenu open (NFR1) | Met at load (319 B less); the three-deep action list frees the door menu behind it |

## Open Items for the Person

- On a real phone and watch: the steps in `integration-test-instructions.md`, especially the edit-while-closed queue and the reconnect resend.
- Review the aplite memory changes (table-driven settings store and menus, glyph-pack door loading one glyph at a time). They behave the same in the emulator.
