package com.pebblentn.app.export

import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugEventType
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportBuilderTest {

    private val builder = ExportBuilder()

    private val userRule = Rule(
        id = "my-rule",
        enabled = true,
        priority = 100,
        packageNames = listOf("com.google.android.apps.maps"),
        conditions = listOf(Condition("combinedText", ConditionOperator.CONTAINS, "turn")),
        output = RuleOutput(maneuver = LiteralExtractor("RIGHT")),
    )

    private val event = DebugEvent(
        id = 1,
        packageName = "com.google.android.apps.maps",
        eventType = DebugEventType.POSTED,
        eventTimestampMillis = 100,
        receivedTimestampMillis = 100,
        snapshot = NotificationSnapshot(
            packageName = "com.google.android.apps.maps",
            notificationId = 1,
            title = "Turn right",
            text = "In 500 m onto Elm Street",
        ),
        matchedRuleId = "google-maps-turn-right-en",
        disposition = "MATCHED",
    )

    private fun build(mode: ExportMode) =
        builder.build(mode, listOf(userRule), listOf(event), appVersion = "0.0.1", androidRelease = "14", exportedAt = "2026-07-12T00:00:00Z")

    @Test
    fun rulesOnlyHasNoNotificationContent() {
        val out = build(ExportMode.RULES_ONLY)
        assertTrue(out.contains("my-rule"))
        assertTrue("events omitted/empty", out.contains("\"events\": []"))
        assertFalse(out.contains("Elm"))
        assertFalse(out.contains("Turn right"))
    }

    @Test
    fun privacySafeRedactsRoadNamesButKeepsStructure() {
        val out = build(ExportMode.PRIVACY_SAFE)
        assertTrue("keeps maneuver keyword", out.contains("Turn right") || out.contains("right"))
        assertTrue("keeps distance", out.contains("500 m"))
        assertFalse("redacts road name", out.contains("Elm"))
        assertTrue("uses placeholder", out.contains(Redactor.PLACEHOLDER))
    }

    @Test
    fun fullIncludesRawFields() {
        val out = build(ExportMode.FULL)
        assertTrue(out.contains("Elm Street"))
        assertTrue(out.contains("Turn right"))
    }

    @Test
    fun metadataRecordsMode() {
        assertTrue(build(ExportMode.FULL).contains("\"mode\": \"FULL\""))
    }
}
