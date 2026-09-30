package com.pebblentn.app.ui.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.DistanceExtractor
import com.pebblentn.app.rules.FirstNonEmptyExtractor
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput

/**
 * Starting points for the rule editor (#28). A rule made from a captured notification matches that
 * notification's title (without a leading distance, which changes every update), targets its app,
 * and keeps the maneuver the watch showed, or UNKNOWN for the author to choose. It is valid as-is,
 * so Preview works straight away.
 */
object RuleTemplates {

    // [\s\u00a0]: Google Maps puts a non-breaking space between the number and the unit, which
    // Java's \s doesn't match.
    private val LEADING_DISTANCE =
        Regex("""^[\s\u00a0]*\d+(?:[.,]\d+)?[\s\u00a0]*(?:km|m|mi|ft|yd)\b[\s\u00a0]*[·•\-]?[\s\u00a0]*""", RegexOption.IGNORE_CASE)
    private const val MAX_MATCH_TEXT = 60

    fun fromCapture(event: DebugEvent): Rule {
        val title = event.snapshot?.title.orEmpty()
        val matchText = title.replace(LEADING_DISTANCE, "").trim().take(MAX_MATCH_TEXT)
        val maneuver = event.instruction?.maneuver ?: Maneuver.UNKNOWN
        val appSlug = event.packageName.substringAfterLast('.').lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "app" }
        return Rule(
            id = "my-$appSlug-${maneuver.name.lowercase().replace('_', '-')}",
            enabled = true,
            priority = 100,
            packageNames = listOf(event.packageName),
            conditions = if (matchText.isEmpty()) {
                listOf(Condition("title", ConditionOperator.EXISTS))
            } else {
                listOf(Condition("title", ConditionOperator.CONTAINS_IGNORE_CASE, matchText))
            },
            output = RuleOutput(
                maneuver = LiteralExtractor(maneuver.name),
                distanceMeters = DistanceExtractor("combinedText"),
                primaryText = FirstNonEmptyExtractor(listOf("title", "text")),
            ),
        )
    }
}
