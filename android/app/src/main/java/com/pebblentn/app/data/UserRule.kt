package com.pebblentn.app.data

import com.pebblentn.app.rules.Rule

/** Validation state of a stored user rule. */
object RuleValidationStatus {
    const val VALID = "VALID"
    const val INVALID = "INVALID"
}

/** A user rule joined with its parsed form, for the Rules UI. */
data class UserRule(
    val ruleId: String,
    val sourceRuleId: String?,
    val packageName: String,
    val rule: Rule?,
    val canonicalJson: String,
    val enabled: Boolean,
    val validationStatus: String,
    val updatedAt: Long,
)
