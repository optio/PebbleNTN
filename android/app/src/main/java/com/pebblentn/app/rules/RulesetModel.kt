package com.pebblentn.app.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Declarative ruleset model — pure data, mirroring `schemas/ruleset.schema.json`. Interpreted by
 * fixed engine code; it can express no executable behavior (spec/200-architecture/RuleEngine.md).
 * Field declaration order matches the schema so canonical serialization emits keys in schema order.
 */
@Serializable
data class Ruleset(
    val schemaVersion: Int,
    val rulesetVersion: String,
    val minimumAppVersionCode: Int,
    val createdAt: String,
    val publisher: String,
    val rules: List<Rule>,
) {
    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
    }
}

@Serializable
data class Rule(
    val id: String,
    val enabled: Boolean,
    val priority: Int,
    val packageNames: List<String>,
    val locales: List<String> = emptyList(),
    val conditions: List<Condition>,
    val output: RuleOutput,
    /** Maintainer note: why the rule is shaped this way. Ignored by the engine; shown in the UI. */
    val comment: String? = null,
)

/** Operators for a [Condition]. Serialized names match the schema enum. */
@Serializable
enum class ConditionOperator {
    @SerialName("exists") EXISTS,
    @SerialName("notExists") NOT_EXISTS,
    @SerialName("equals") EQUALS,
    @SerialName("equalsIgnoreCase") EQUALS_IGNORE_CASE,
    @SerialName("contains") CONTAINS,
    @SerialName("containsIgnoreCase") CONTAINS_IGNORE_CASE,
    @SerialName("startsWith") STARTS_WITH,
    @SerialName("endsWith") ENDS_WITH,
    @SerialName("regex") REGEX,
    @SerialName("in") IN,
}

@Serializable
data class Condition(
    val field: String,
    val operator: ConditionOperator,
    val value: String? = null,
    val values: List<String> = emptyList(),
)

@Serializable
data class RuleOutput(
    val maneuver: Extractor? = null,
    val distanceMeters: Extractor? = null,
    val primaryText: Extractor? = null,
    val secondaryText: Extractor? = null,
    val etaEpochSeconds: Extractor? = null,
)

/**
 * A fixed extraction operation. The `type` discriminator selects one of a closed set; unknown types
 * fail validation (RulesJSON contract). No extractor can execute code, load classes or fetch URLs.
 */
@Serializable
sealed interface Extractor

@Serializable
@SerialName("literal")
data class LiteralExtractor(val value: String) : Extractor

@Serializable
@SerialName("regexCapture")
data class RegexCaptureExtractor(
    val field: String,
    val pattern: String,
    val group: Int = 1,
) : Extractor

@Serializable
@SerialName("fieldCopy")
data class FieldCopyExtractor(val field: String) : Extractor

@Serializable
@SerialName("firstNonEmpty")
data class FirstNonEmptyExtractor(val fields: List<String>) : Extractor

@Serializable
@SerialName("distance")
data class DistanceExtractor(val field: String) : Extractor

@Serializable
@SerialName("duration")
data class DurationExtractor(val field: String) : Extractor

@Serializable
@SerialName("maneuverMap")
data class ManeuverMapExtractor(
    val field: String,
    val mapping: Map<String, String> = emptyMap(),
    val default: String? = null,
) : Extractor

@Serializable
@SerialName("normalizeString")
data class NormalizeStringExtractor(
    val field: String,
    val lowercase: Boolean = false,
    val collapseWhitespace: Boolean = true,
    val trim: Boolean = true,
) : Extractor

@Serializable
@SerialName("boundedJoin")
data class BoundedJoinExtractor(
    val fields: List<String>,
    val separator: String = " ",
    val maxLength: Int = 64,
) : Extractor
