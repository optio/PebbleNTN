package com.pebblentn.app.ui.debug

import com.pebblentn.app.rules.RuleOutcome
import com.pebblentn.app.rules.RuleTraceEntry

/**
 * The rule trace split for display (#28): the entries that decided the outcome (the matching rule,
 * and any rule that errored) are shown directly; the many rules that simply didn't apply are
 * collapsed behind a count. Order within each group is the evaluation order.
 */
data class TraceSummary(
    val decisive: List<RuleTraceEntry>,
    val others: List<RuleTraceEntry>,
) {
    companion object {
        private val DECISIVE = setOf(RuleOutcome.MATCHED, RuleOutcome.ERROR)

        fun of(trace: List<RuleTraceEntry>): TraceSummary {
            val (decisive, others) = trace.partition { it.outcome in DECISIVE }
            return TraceSummary(decisive, others)
        }
    }
}
