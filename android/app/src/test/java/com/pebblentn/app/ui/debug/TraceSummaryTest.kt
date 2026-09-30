package com.pebblentn.app.ui.debug

import com.pebblentn.app.rules.RuleLayer
import com.pebblentn.app.rules.RuleOutcome
import com.pebblentn.app.rules.RuleTraceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #28: the event detail shows the deciding rules and collapses the rest. */
class TraceSummaryTest {

    private fun entry(id: String, outcome: RuleOutcome) = RuleTraceEntry(id, RuleLayer.BUNDLED, outcome)

    @Test
    fun theMatchingRuleIsDecisiveAndTheRestAreCollapsed() {
        val trace = listOf(
            entry("transit", RuleOutcome.CONDITIONS_FAILED),
            entry("arrive", RuleOutcome.CONDITIONS_FAILED),
            entry("de-rule", RuleOutcome.SKIPPED_LOCALE),
            entry("slight-left", RuleOutcome.MATCHED),
        )
        val summary = TraceSummary.of(trace)
        assertEquals(listOf("slight-left"), summary.decisive.map { it.ruleId })
        assertEquals("evaluation order is kept", listOf("transit", "arrive", "de-rule"), summary.others.map { it.ruleId })
    }

    @Test
    fun errorsAreAlwaysShown() {
        val summary = TraceSummary.of(listOf(entry("slow-regex", RuleOutcome.ERROR), entry("x", RuleOutcome.DISABLED)))
        assertEquals(listOf("slow-regex"), summary.decisive.map { it.ruleId })
    }

    @Test
    fun noMatchLeavesNothingDecisive() {
        val summary = TraceSummary.of(listOf(entry("a", RuleOutcome.CONDITIONS_FAILED)))
        assertTrue(summary.decisive.isEmpty())
        assertEquals(1, summary.others.size)
    }
}
