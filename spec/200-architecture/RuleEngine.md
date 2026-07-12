# Declarative Rule Engine

## Security boundary

Rules are data interpreted by fixed code. The rule language SHALL NOT support JavaScript, Kotlin, Java reflection, downloadable classes, shell commands, templates capable of execution, arbitrary URLs, or dynamic dependency loading.

## Layers

1. User additions and overrides.
2. Active official downloaded rules.
3. Bundled official rules.

User rules have highest precedence. Bundled and downloaded rules remain immutable in the UI. Users clone an official rule to modify it.

## Evaluation

1. Select rules by exact source package.
2. Filter by enabled status, locale and optional channel/category conditions.
3. Sort by priority descending, then stable rule ID.
4. Evaluate conditions with short-circuit behavior.
5. Run fixed extractors.
6. Validate normalized output.
7. Return the first complete match unless a rule declares continuation.
8. Produce a structured trace for every evaluated rule in debug mode.

## Operators

- exists / not exists;
- equals / equals-ignore-case;
- contains / contains-ignore-case;
- starts-with / ends-with;
- safe-regex;
- integer comparison;
- membership in fixed list.

## Extractors

- literal;
- regex capture;
- field copy;
- first non-empty field;
- distance parser;
- duration parser;
- maneuver mapping;
- string normalization;
- bounded join.

## Regex safety

- Use Java regex initially with hard input, pattern, group and evaluation limits.
- Reject nested pathological constructs where detectable.
- Maximum pattern length: 1,024 characters.
- Maximum tested input per field: 4,096 characters.
- Maximum rules per package: 500.
- Evaluation is cancellable and time-budgeted.
- A timed-out rule is disabled for that run and logged in the trace.

## Simple editor

The simple editor is a projection over the same JSON domain model:
- form changes update an in-memory rule document;
- valid changes serialize to canonical JSON;
- unsupported advanced structures switch the editor to expert-only mode without data loss;
- simple editor never maintains a separate rule representation.

## Expert editor

- JSON syntax highlighting;
- schema validation;
- semantic validation;
- canonical-format command;
- diff from source official rule;
- test against one, selected, or all captures;
- cannot save invalid JSON.
