package com.pebblentn.app.rules

import kotlinx.serialization.SerializationException
import java.util.regex.PatternSyntaxException

/** Result of validating rule JSON from the editor. */
sealed interface RuleValidationResult {
    data class Valid(val rule: Rule) : RuleValidationResult
    data class Invalid(val errors: List<String>) : RuleValidationResult
}

/**
 * Validates a single rule's JSON for the expert editor (spec RuleEngine "Expert editor": schema +
 * semantic validation; cannot save invalid JSON). Structural validity comes from the strict codec;
 * semantic rules mirror `schemas/ruleset.schema.json` bounds plus operator/regex requirements. All
 * semantic problems are collected so the editor can show them together.
 */
object RuleValidator {

    private val ID_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

    private val OPERATORS_REQUIRING_VALUE = setOf(
        ConditionOperator.EQUALS,
        ConditionOperator.EQUALS_IGNORE_CASE,
        ConditionOperator.CONTAINS,
        ConditionOperator.CONTAINS_IGNORE_CASE,
        ConditionOperator.STARTS_WITH,
        ConditionOperator.ENDS_WITH,
        ConditionOperator.REGEX,
    )

    fun validate(json: String): RuleValidationResult {
        val rule = try {
            RulesetCodec.parseRule(json)
        } catch (e: SerializationException) {
            return RuleValidationResult.Invalid(listOf("Invalid rule JSON: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return RuleValidationResult.Invalid(listOf("Invalid rule JSON: ${e.message}"))
        }

        val errors = buildList {
            if (!ID_PATTERN.matches(rule.id) || rule.id.length > 120) {
                add("Rule id must be lowercase kebab-case (<=120 chars): '${rule.id}'")
            }
            if (rule.packageNames.isEmpty() || rule.packageNames.size > 20) {
                add("A rule must declare 1-20 package names")
            }
            if (rule.priority !in -100_000..100_000) {
                add("priority must be within -100000..100000")
            }
            if (rule.conditions.size > 50) add("A rule may have at most 50 conditions")
            if (rule.locales.size > 30) add("A rule may declare at most 30 locales")

            rule.conditions.forEachIndexed { index, condition ->
                validateCondition(index, condition, this)
            }
            collectExtractorErrors(rule.output, this)
        }

        return if (errors.isEmpty()) RuleValidationResult.Valid(rule) else RuleValidationResult.Invalid(errors)
    }

    private fun validateCondition(index: Int, condition: Condition, errors: MutableList<String>) {
        val where = "condition[$index] (${condition.operator})"
        when (condition.operator) {
            ConditionOperator.IN ->
                if (condition.values.isEmpty()) errors.add("$where requires a non-empty 'values' list")
            in OPERATORS_REQUIRING_VALUE -> {
                if (condition.value.isNullOrEmpty()) {
                    errors.add("$where requires a non-empty 'value'")
                } else if (condition.operator == ConditionOperator.REGEX) {
                    checkRegex(condition.value, where, errors)
                }
            }
            else -> Unit // exists / notExists need nothing
        }
    }

    private fun collectExtractorErrors(output: RuleOutput, errors: MutableList<String>) {
        listOfNotNull(output.maneuver, output.distanceMeters, output.primaryText, output.secondaryText, output.etaEpochSeconds)
            .forEach { extractor ->
                if (extractor is RegexCaptureExtractor) {
                    checkRegex(extractor.pattern, "regexCapture extractor", errors)
                }
            }
    }

    private fun checkRegex(pattern: String, where: String, errors: MutableList<String>) {
        try {
            SafeRegex.compile(pattern)
        } catch (e: PatternSyntaxException) {
            // Must precede IllegalArgumentException: PatternSyntaxException is a subclass of it.
            errors.add("$where: invalid regex (${e.description})")
        } catch (e: IllegalArgumentException) {
            errors.add("$where: ${e.message}")
        }
    }
}
