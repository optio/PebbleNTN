package com.pebblentn.app.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.notification.NotificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val engine = RuleEngine()
    private val mapsPackage = "com.google.android.apps.maps"

    private fun snapshot(text: String?, pkg: String = mapsPackage, title: String? = null) =
        NotificationSnapshot(packageName = pkg, notificationId = 1, title = title, text = text)

    private fun rule(
        id: String,
        priority: Int = 100,
        enabled: Boolean = true,
        pkg: String = mapsPackage,
        locales: List<String> = emptyList(),
        conditions: List<Condition> = emptyList(),
        output: RuleOutput = RuleOutput(),
    ) = Rule(id, enabled, priority, listOf(pkg), locales, conditions, output)

    private val turnRightRule = rule(
        id = "turn-right",
        conditions = listOf(Condition("combinedText", ConditionOperator.REGEX, "(?i)\\bturn\\s+right\\b")),
        output = RuleOutput(
            maneuver = LiteralExtractor("RIGHT"),
            distanceMeters = DistanceExtractor("combinedText"),
            primaryText = FirstNonEmptyExtractor(listOf("text", "title")),
        ),
    )

    @Test
    fun matchesAndAssemblesInstruction() {
        val result = engine.evaluate(snapshot(text = "In 500 m, turn right"), LayeredRules(bundled = listOf(turnRightRule)))
        assertTrue(result.matched)
        assertEquals("turn-right", result.matchedRuleId)
        assertEquals(RuleLayer.BUNDLED, result.matchedLayer)
        assertEquals(Maneuver.RIGHT, result.instruction!!.maneuver)
        assertEquals(500, result.instruction.distanceMeters)
        assertEquals("In 500 m, turn right", result.instruction.primaryText)
    }

    @Test
    fun noMatchWhenConditionsFail() {
        val result = engine.evaluate(snapshot(text = "In 500 m, turn left"), LayeredRules(bundled = listOf(turnRightRule)))
        assertFalse(result.matched)
        assertNull(result.instruction)
        assertEquals(RuleOutcome.CONDITIONS_FAILED, result.trace.single().outcome)
    }

    @Test
    fun otherPackageIsNotEvaluated() {
        val result = engine.evaluate(snapshot(text = "turn right", pkg = "com.waze"), LayeredRules(bundled = listOf(turnRightRule)))
        assertFalse(result.matched)
        assertTrue("rules for other packages are not traced", result.trace.isEmpty())
    }

    @Test
    fun disabledRuleIsSkippedAndTraced() {
        val disabled = turnRightRule.copy(id = "turn-right", enabled = false)
        val result = engine.evaluate(snapshot(text = "turn right"), LayeredRules(bundled = listOf(disabled)))
        assertFalse(result.matched)
        assertEquals(RuleOutcome.DISABLED, result.trace.single().outcome)
    }

    @Test
    fun userLayerWinsOverBundled() {
        val bundled = rule("shared", priority = 1000, output = RuleOutput(maneuver = LiteralExtractor("LEFT")),
            conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS_IGNORE_CASE, "turn")))
        val user = rule("shared", priority = 1, output = RuleOutput(maneuver = LiteralExtractor("RIGHT")),
            conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS_IGNORE_CASE, "turn")))
        val result = engine.evaluate(snapshot(text = "turn"), LayeredRules(user = listOf(user), bundled = listOf(bundled)))
        // Even with far lower priority, the user layer takes precedence.
        assertEquals(RuleLayer.USER, result.matchedLayer)
        assertEquals(Maneuver.RIGHT, result.instruction!!.maneuver)
    }

    @Test
    fun higherPriorityWinsWithinLayer() {
        val low = rule("low", priority = 10, output = RuleOutput(maneuver = LiteralExtractor("LEFT")),
            conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS, "turn")))
        val high = rule("high", priority = 90, output = RuleOutput(maneuver = LiteralExtractor("RIGHT")),
            conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS, "turn")))
        val result = engine.evaluate(snapshot(text = "turn"), LayeredRules(bundled = listOf(low, high)))
        assertEquals("high", result.matchedRuleId)
    }

    @Test
    fun localeFilterSkipsNonMatchingLocale() {
        val enOnly = turnRightRule.copy(locales = listOf("en"))
        val result = engine.evaluate(snapshot(text = "turn right"), LayeredRules(bundled = listOf(enOnly)), locale = "nl-NL")
        assertFalse(result.matched)
        assertEquals(RuleOutcome.SKIPPED_LOCALE, result.trace.single().outcome)

        val matched = engine.evaluate(snapshot(text = "turn right"), LayeredRules(bundled = listOf(enOnly)), locale = "en-US")
        assertTrue(matched.matched)
    }

    @Test
    fun regexTimeoutDisablesRuleForRunAndRecordsError() {
        val slow = rule(
            id = "slow",
            conditions = listOf(Condition("text", ConditionOperator.REGEX, "navigation")),
        )
        val zeroBudgetEngine = RuleEngine(regexBudgetMillis = 0L)
        val result = zeroBudgetEngine.evaluate(snapshot(text = "navigation text"), LayeredRules(bundled = listOf(slow)))
        assertFalse(result.matched)
        assertEquals(RuleOutcome.ERROR, result.trace.single().outcome)
    }
}
