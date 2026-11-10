# Code Generation Plan Approval

AI-DLC built this plan without asking because plan approval is off for this
piece of work. This file is the record and asks nothing; to look at a plan
before it is built, ask in chat.

## Plan Approval

Built without asking: plan approval is off for this piece of work (from scope express).

- Builds: OsmAnd street/arrival rules in 9 languages, a compact road line for Google Maps and Vela, an arrival-clock ETA extractor, the synced Directions text setting (Full / Compact, protocol 1.5), ...
- Touches: `rules/bundled/` (osmand, google-maps, vela-maps), `rules/fixtures/`, `schemas/ruleset.schema.json`, `android/app/.../rules/`, `core/`, `protocol/`, `pebble/NavigationController.kt`, watch...
- Tests: about 15 new unit tests (extractor, engine, codec, settings, controller, watch C) plus fixture-driven regression for every language in the Kotlin and Python engines
- Work on branch `feat/osmand-bigtext` (created from `main` at Step 1). Never commit to `main`.

Full plan: aidlc/spaces/default/intents/261110-osmand-bigtext/construction/code-generation/code-generation-plan.md
Test instructions: aidlc/spaces/default/intents/261110-osmand-bigtext/construction/code-generation/unit-test-instructions.md

[Approval Fingerprint]: sha256:v3:994a4a7b4bece6ad1af06d3972912d662ee23fb97a4002e25dae79b6a9e338d3
[Planned Source]: 7a5863bb5b00553d235ae7c102ff82c76e64b68ee2faeeef40a4babf7b0cf8b5


[Answer]: Plan approval off
