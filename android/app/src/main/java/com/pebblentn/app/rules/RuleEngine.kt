package com.pebblentn.app.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.notification.NotificationSnapshot
import kotlinx.serialization.Serializable

/** Rule precedence layers (spec/200-architecture/RuleEngine.md). User overrides win. */
@Serializable
enum class RuleLayer { USER, DOWNLOADED, BUNDLED }

/** Rules grouped by layer. Evaluated user-first, then downloaded, then bundled. */
data class LayeredRules(
    val user: List<Rule> = emptyList(),
    val downloaded: List<Rule> = emptyList(),
    val bundled: List<Rule> = emptyList(),
) {
    /** Flatten to precedence order, sorting each layer by descending priority then stable id. */
    fun inPrecedenceOrder(): List<Pair<Rule, RuleLayer>> =
        sortedLayer(user, RuleLayer.USER) + sortedLayer(downloaded, RuleLayer.DOWNLOADED) + sortedLayer(bundled, RuleLayer.BUNDLED)

    private fun sortedLayer(rules: List<Rule>, layer: RuleLayer): List<Pair<Rule, RuleLayer>> =
        rules.sortedWith(compareByDescending<Rule> { it.priority }.thenBy { it.id }).map { it to layer }

    companion object {
        val EMPTY = LayeredRules()
    }
}

/** Outcome recorded for each evaluated rule (debug trace). */
@Serializable
enum class RuleOutcome { MATCHED, CONDITIONS_FAILED, DISABLED, SKIPPED_LOCALE, ERROR }

@Serializable
data class RuleTraceEntry(
    val ruleId: String,
    val layer: RuleLayer,
    val outcome: RuleOutcome,
    val message: String? = null,
)

/** Result of evaluating rules against one snapshot. */
data class RuleEvaluation(
    val instruction: NavigationInstruction?,
    val matchedRuleId: String?,
    val matchedLayer: RuleLayer?,
    val trace: List<RuleTraceEntry>,
) {
    val matched: Boolean get() = instruction != null

    companion object {
        fun unmatched(trace: List<RuleTraceEntry>) = RuleEvaluation(null, null, null, trace)
    }
}

/**
 * Deterministic declarative rule engine. Selects rules for the snapshot's package, filters by
 * enabled/locale, evaluates conditions with short-circuit AND, and assembles the first matching
 * rule's output into a [NavigationInstruction]. A rule whose regex times out is disabled for that
 * run and recorded as ERROR (RuleEngine "Regex safety"). Produces a structured trace for every
 * evaluated rule. Pure: no I/O.
 */
class RuleEngine(
    regexBudgetMillis: Long = SafeRegex.DEFAULT_BUDGET_MILLIS,
) {
    private val conditions = ConditionEvaluator(regexBudgetMillis)
    private val extractors = ExtractorRunner(regexBudgetMillis)

    companion object {
        /** Safety bound on rules evaluated per source package (RuleEngine "Regex safety"). */
        const val MAX_RULES_PER_PACKAGE = 500
    }

    fun evaluate(
        snapshot: NotificationSnapshot,
        rules: LayeredRules,
        locale: String? = null,
        nowEpochSeconds: Long = 0,
    ): RuleEvaluation {
        val trace = mutableListOf<RuleTraceEntry>()
        var evaluatedForPackage = 0

        for ((rule, layer) in rules.inPrecedenceOrder()) {
            if (snapshot.packageName !in rule.packageNames) continue // not evaluated; not traced
            // Bound work per package (RuleEngine "Maximum rules per package").
            if (evaluatedForPackage >= MAX_RULES_PER_PACKAGE) break
            evaluatedForPackage++
            if (!rule.enabled) {
                trace += RuleTraceEntry(rule.id, layer, RuleOutcome.DISABLED)
                continue
            }
            if (!localeMatches(rule, locale)) {
                trace += RuleTraceEntry(rule.id, layer, RuleOutcome.SKIPPED_LOCALE)
                continue
            }
            try {
                if (!rule.conditions.all { conditions.evaluate(it, snapshot) }) {
                    trace += RuleTraceEntry(rule.id, layer, RuleOutcome.CONDITIONS_FAILED)
                    continue
                }
                val instruction = assemble(rule.output, snapshot, nowEpochSeconds)
                trace += RuleTraceEntry(rule.id, layer, RuleOutcome.MATCHED)
                return RuleEvaluation(instruction, rule.id, layer, trace)
            } catch (e: RegexTimeoutException) {
                trace += RuleTraceEntry(rule.id, layer, RuleOutcome.ERROR, "regex timeout")
            } catch (e: RuntimeException) {
                trace += RuleTraceEntry(rule.id, layer, RuleOutcome.ERROR, e.message)
            }
        }
        return RuleEvaluation.unmatched(trace)
    }

    private fun localeMatches(rule: Rule, locale: String?): Boolean {
        if (rule.locales.isEmpty()) return true
        val language = locale?.substringBefore('-')?.substringBefore('_')
        return language != null && rule.locales.any { it.equals(language, ignoreCase = true) }
    }

    private fun assemble(output: RuleOutput, snapshot: NotificationSnapshot, nowEpochSeconds: Long): NavigationInstruction {
        val maneuver = output.maneuver?.let { asText(extractors.run(it, snapshot)) }
            ?.let(Maneuver::fromToken) ?: Maneuver.UNKNOWN
        val distance = output.distanceMeters?.let { asNum(extractors.run(it, snapshot)) }?.toInt()?.coerceAtLeast(0)
        val primary = output.primaryText?.let { asText(extractors.run(it, snapshot)) }
        val secondary = output.secondaryText?.let { asText(extractors.run(it, snapshot)) }
        val eta = output.etaEpochSeconds?.let { extractor ->
            asNum(extractors.run(extractor, snapshot))?.let { value ->
                // A duration yields seconds-from-now; anything else is treated as an absolute epoch.
                if (extractor is DurationExtractor) nowEpochSeconds + value else value
            }
        }
        return NavigationInstruction(maneuver, distance, primary, secondary, eta)
    }

    private fun asText(result: ExtractionResult): String? = (result as? ExtractionResult.Text)?.value
    private fun asNum(result: ExtractionResult): Long? = (result as? ExtractionResult.Num)?.value
}
