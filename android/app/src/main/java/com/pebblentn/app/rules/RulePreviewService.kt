package com.pebblentn.app.rules

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.notification.NotificationSnapshot

/** Outcome of previewing a candidate rule against a snapshot. */
sealed interface PreviewResult {
    data class Evaluated(val evaluation: RuleEvaluation) : PreviewResult
    data class InvalidRule(val errors: List<String>) : PreviewResult
}

/**
 * Runs rules against a captured snapshot for the test bench / preview (spec RuleEngine "Expert
 * editor": test against captures; rerun a stored event). Pure wrapper over [RuleEngine] so previews
 * behave exactly like live evaluation.
 */
class RulePreviewService(
    private val engine: RuleEngine = RuleEngine(),
    private val clock: EpochClock = EpochClock.SYSTEM,
    private val localeProvider: () -> String? = { null },
) {
    /** Re-run a snapshot against an arbitrary layered rule set (e.g. the current active rules). */
    fun previewWithRules(snapshot: NotificationSnapshot, rules: LayeredRules): RuleEvaluation =
        engine.evaluate(snapshot, rules, localeProvider(), clock.nowMillis() / 1000)

    /**
     * Validate [candidateJson] and, if valid, run it as the sole rule against [snapshot] so an
     * author can see the trace and normalized output before saving.
     */
    fun previewCandidate(snapshot: NotificationSnapshot, candidateJson: String): PreviewResult =
        when (val validation = RuleValidator.validate(candidateJson)) {
            is RuleValidationResult.Invalid -> PreviewResult.InvalidRule(validation.errors)
            is RuleValidationResult.Valid ->
                PreviewResult.Evaluated(previewWithRules(snapshot, LayeredRules(user = listOf(validation.rule))))
        }
}
