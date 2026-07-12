# Developer Rule Maintenance Workflow

## Goal

A maintainer must be able to convert captured databases or exported JSON into reviewed official rules with minimal manual data wrangling and a clear before/after preview.

## Tool

Create a repository tool at `tools/rule-workbench/`. Preferred implementation is a Kotlin/JVM CLI sharing the production rule-engine module. A small local web UI may be added later, but the CLI is authoritative and CI-compatible.

## Inputs

- privacy-safe diagnostic JSON;
- full diagnostic JSON supplied intentionally;
- Android Room database copied from a debug build;
- existing fixture directories;
- one or more candidate rulesets.

The tool opens Room databases read-only. It never modifies the source database.

## Commands

```bash
./tools/rule-workbench/bin/pebblentn-rules inspect capture.json
./tools/rule-workbench/bin/pebblentn-rules inspect debug.db --package com.google.android.apps.maps
./tools/rule-workbench/bin/pebblentn-rules cluster debug.db --by field-shape
./tools/rule-workbench/bin/pebblentn-rules test rules/candidate.json --input debug.db
./tools/rule-workbench/bin/pebblentn-rules diff rules/bundled/current.json rules/candidate.json --input fixtures/
./tools/rule-workbench/bin/pebblentn-rules sanitize debug.db --output fixtures/proposed/
./tools/rule-workbench/bin/pebblentn-rules scaffold --from capture.json --output proposed-rule.json
./tools/rule-workbench/bin/pebblentn-rules promote proposed-rule.json --fixtures fixtures/proposed/
```

## Inspection output

For every event:
- source package/version where available;
- locale;
- field presence and values;
- matched rule and layer;
- condition/extractor trace;
- normalized output;
- expected output when annotated;
- nearby updates in the same notification/session;
- privacy warning.

## Clustering

The workbench groups unknown notifications by:
- package;
- locale;
- set of populated fields;
- normalized text shape;
- channel/category;
- recurring token pattern.

This helps discover new notification variants without reading every capture manually.

## Scaffold workflow

1. Select representative unmatched captures.
2. Generate a rule skeleton with exact package and field-presence conditions.
3. Maintainer adds minimal textual conditions and extractors.
4. Run preview against all related captures.
5. Annotate expected outputs.
6. Sanitize and save fixtures.
7. Run regression against the entire official corpus.
8. Promote into candidate official ruleset.
9. Open GitHub pull request with generated report.

## Preview report

Generate Markdown and optional static HTML showing:
- old result;
- new result;
- changed fields;
- captures newly matched;
- captures no longer matched;
- ambiguous matches;
- performance timing;
- privacy-safe sample display.

## Pull-request gate

A rule PR must include:
- rule JSON diff;
- at least one new sanitized fixture per new variant;
- expected output;
- generated regression report;
- schema validation;
- zero unexpected changes in existing fixtures;
- maintainer review.

## Initial Google Maps workflow

Before claiming Google Maps support:
- collect multiple maneuvers;
- collect distance units and thresholds;
- collect navigation start, progress, reroute, arrival and stop;
- collect English, Dutch, French and German samples;
- distinguish notification variants by Android and Maps version where observable;
- document unsupported variants explicitly.
