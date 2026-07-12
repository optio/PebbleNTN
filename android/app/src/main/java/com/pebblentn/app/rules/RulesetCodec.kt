package com.pebblentn.app.rules

import kotlinx.serialization.json.Json

/**
 * Parses and canonicalizes rulesets. Parsing is strict (unknown fields, operators or extractor
 * types fail — RulesJSON "strict official mode"). Canonicalization produces the deterministic form
 * that is signed for official rules: two-space indent, schema key order, rules sorted by package
 * then descending priority then id.
 */
object RulesetCodec {

    private val strict = Json {
        ignoreUnknownKeys = false
        classDiscriminator = "type"
        prettyPrint = false
    }

    private val canonical = Json {
        ignoreUnknownKeys = false
        classDiscriminator = "type"
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = false
    }

    /**
     * Parse and validate a ruleset document.
     *
     * @throws IllegalArgumentException if the schema version is unsupported or a rule id is duplicated.
     * @throws kotlinx.serialization.SerializationException on malformed JSON, unknown fields, or
     *   unknown operators/extractor types.
     */
    fun parse(text: String): Ruleset {
        val ruleset = strict.decodeFromString(Ruleset.serializer(), text)
        require(ruleset.schemaVersion == Ruleset.SUPPORTED_SCHEMA_VERSION) {
            "unsupported ruleset schemaVersion ${ruleset.schemaVersion}"
        }
        val ids = ruleset.rules.map { it.id }
        require(ids.size == ids.toSet().size) { "duplicate rule id in ruleset" }
        return ruleset
    }

    /** Serialize a ruleset in canonical form (stable ordering; the signed representation). */
    fun canonicalize(ruleset: Ruleset): String {
        val sorted = ruleset.copy(rules = ruleset.rules.sortedWith(RULE_ORDER))
        return canonical.encodeToString(Ruleset.serializer(), sorted)
    }

    /** Strictly parse a single rule (used by the rule editor). */
    fun parseRule(text: String): Rule = strict.decodeFromString(Rule.serializer(), text)

    /** Canonical form of a single rule (used to store user rules). */
    fun canonicalizeRule(rule: Rule): String = canonical.encodeToString(Rule.serializer(), rule)

    private val RULE_ORDER: Comparator<Rule> = compareBy<Rule>(
        { it.packageNames.firstOrNull().orEmpty() },
        { -it.priority },
        { it.id },
    )
}
