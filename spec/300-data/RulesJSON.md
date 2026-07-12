# Rules JSON Contract

## Ruleset envelope

A ruleset contains:
- schema version;
- ruleset version;
- minimum app version code;
- creation timestamp;
- publisher;
- source type;
- array of rules;
- optional test vectors;
- signature metadata for downloaded official rules.

## Rule identifiers

IDs are lowercase ASCII kebab-case and globally stable. Updated behavior keeps the same ID when conceptually the same rule evolves. Breaking semantic replacements use a new ID and deprecate the old one.

## Canonicalization

- UTF-8;
- Unix newlines;
- two-space indentation;
- object keys emitted in schema order;
- rule arrays sorted by package, descending priority, ID;
- no insignificant numeric formats;
- canonical payload is what is signed.

## Compatibility

Unknown fields are rejected in strict official mode and preserved-but-ignored only in an explicit future-compatible import mode. Unknown operators or extractor types always fail validation.
