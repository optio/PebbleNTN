package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import java.util.regex.Pattern

/**
 * Evaluates a single [Condition] against a snapshot. Pure and deterministic. Regex patterns are
 * compiled through [SafeRegex] and cached per instance; a [RegexTimeoutException] propagates so the
 * engine can disable the offending rule for that run (RuleEngine "Regex safety").
 */
class ConditionEvaluator(
    private val regexBudgetMillis: Long = SafeRegex.DEFAULT_BUDGET_MILLIS,
) {
    private val patternCache = HashMap<String, Pattern>()

    fun evaluate(condition: Condition, snapshot: NotificationSnapshot): Boolean {
        val fieldValue = SnapshotFields.resolve(snapshot, condition.field)
        return when (condition.operator) {
            ConditionOperator.EXISTS -> !fieldValue.isNullOrEmpty()
            ConditionOperator.NOT_EXISTS -> fieldValue.isNullOrEmpty()
            ConditionOperator.EQUALS -> fieldValue != null && fieldValue == condition.value
            ConditionOperator.EQUALS_IGNORE_CASE -> fieldValue != null && fieldValue.equals(condition.value, ignoreCase = true)
            ConditionOperator.CONTAINS -> fieldValue != null && condition.value != null && fieldValue.contains(condition.value)
            ConditionOperator.CONTAINS_IGNORE_CASE -> fieldValue != null && condition.value != null && fieldValue.contains(condition.value, ignoreCase = true)
            ConditionOperator.STARTS_WITH -> fieldValue != null && condition.value != null && fieldValue.startsWith(condition.value)
            ConditionOperator.ENDS_WITH -> fieldValue != null && condition.value != null && fieldValue.endsWith(condition.value)
            ConditionOperator.IN -> fieldValue != null && fieldValue in condition.values
            ConditionOperator.REGEX -> fieldValue != null && condition.value != null &&
                SafeRegex.containsMatch(patternFor(condition.value), fieldValue, regexBudgetMillis)
        }
    }

    private fun patternFor(pattern: String): Pattern =
        patternCache.getOrPut(pattern) { SafeRegex.compile(pattern) }
}
