package com.pebblentn.app.ui.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugEventType
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.rules.RuleValidator
import com.pebblentn.app.rules.RulesetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #28: "Create rule from this notification" gives a valid, sensible starting rule. */
class RuleTemplatesTest {

    private fun event(pkg: String, title: String?, maneuver: Maneuver? = null) = DebugEvent(
        id = 1,
        packageName = pkg,
        eventType = DebugEventType.POSTED,
        eventTimestampMillis = 0,
        receivedTimestampMillis = 0,
        snapshot = NotificationSnapshot(packageName = pkg, notificationId = 1, title = title),
        matchedRuleId = null,
        disposition = "CAPTURED_UNMATCHED",
        instruction = maneuver?.let { NavigationInstruction(it) },
    )

    @Test
    fun matchesTheTitleWithoutItsLeadingDistance() {
        val rule = RuleTemplates.fromCapture(event("com.waze", "350 m · Use the left lane to merge"))
        val condition = rule.conditions.single()
        assertEquals("title", condition.field)
        assertEquals(ConditionOperator.CONTAINS_IGNORE_CASE, condition.operator)
        assertEquals("Use the left lane to merge", condition.value)
        assertEquals(listOf("com.waze"), rule.packageNames)
    }

    @Test
    fun keepsTheManeuverTheWatchShowedOrUnknown() {
        val shown = RuleTemplates.fromCapture(event("net.osmand.plus", "Turn right", Maneuver.RIGHT))
        assertEquals("RIGHT", (shown.output.maneuver as LiteralExtractor).value)
        assertEquals("my-plus-right", shown.id)
        val unknown = RuleTemplates.fromCapture(event("com.waze", "Something new"))
        assertEquals("UNKNOWN", (unknown.output.maneuver as LiteralExtractor).value)
    }

    @Test
    fun theTemplateIsValidAsIs() {
        for (e in listOf(event("com.waze", "Turn left"), event("com.waze", null))) {
            val json = RulesetCodec.canonicalizeRule(RuleTemplates.fromCapture(e))
            assertTrue(json, RuleValidator.validate(json) is RuleValidationResult.Valid)
        }
    }
}
