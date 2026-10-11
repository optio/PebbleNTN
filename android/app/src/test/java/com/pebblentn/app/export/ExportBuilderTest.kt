package com.pebblentn.app.export

import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugEventType
import com.pebblentn.app.data.EventVersions
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
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

    @Test
    fun eachEventCarriesTheVersionsItWasRecordedWith() {
        val recorded = event.copy(
            versions = EventVersions(
                appVersionName = "0.0.59",
                appVersionCode = 59,
                sourceAppVersionName = "26.40.01",
                sourceAppVersionCode = 1_234_567,
                rulesetVersions = "google-maps-any-2026.10.7,google-maps-de-2026.10.1",
            ),
        )
        for (mode in listOf(ExportMode.PRIVACY_SAFE, ExportMode.FULL)) {
            val root = Json.parseToJsonElement(
                builder.build(mode, emptyList(), listOf(recorded), appVersion = "0.0.60", androidRelease = "15", exportedAt = "2026-10-11T00:00:00Z", appVersionCode = 60),
            ).jsonObject
            val metadata = root.getValue("metadata").jsonObject
            assertEquals(3L, metadata.getValue("schemaVersion").jsonPrimitive.long)
            assertEquals(60L, metadata.getValue("appVersionCode").jsonPrimitive.long)
            // Version strings are not personal data: privacy-safe keeps them unredacted.
            val versions = root.getValue("events").jsonArray.single().jsonObject.getValue("versions").jsonObject
            assertEquals("0.0.59", versions.getValue("appVersion").jsonPrimitive.content)
            assertEquals(59L, versions.getValue("appVersionCode").jsonPrimitive.long)
            assertEquals("26.40.01", versions.getValue("sourceAppVersion").jsonPrimitive.content)
            assertEquals(1_234_567L, versions.getValue("sourceAppVersionCode").jsonPrimitive.long)
            assertEquals(
                listOf("google-maps-any-2026.10.7", "google-maps-de-2026.10.1"),
                versions.getValue("rulesetVersions").jsonArray.map { it.jsonPrimitive.content },
            )
        }
    }

    @Test
    fun eventsFromBeforeVersionsWereStoredExportNullVersions() {
        val root = Json.parseToJsonElement(build(ExportMode.PRIVACY_SAFE)).jsonObject
        val versions = root.getValue("events").jsonArray.single().jsonObject.getValue("versions").jsonObject
        assertEquals("null", versions.getValue("sourceAppVersion").toString())
        assertTrue(versions.getValue("rulesetVersions").jsonArray.isEmpty())
    }

    @Test
    fun aGroupedEventExportsItsCountTimesAndRedactedLatestExample() {
        val grouped = event.copy(
            eventTimestampMillis = 1_000,
            occurrences = 37,
            lastEventTimestampMillis = 90_000,
            lastReceivedTimestampMillis = 90_010,
            lastSnapshot = event.snapshot!!.copy(text = "In 100 m onto Oak Avenue"),
        )
        val safe = Json.parseToJsonElement(
            builder.build(ExportMode.PRIVACY_SAFE, emptyList(), listOf(grouped), appVersion = "0.0.61", androidRelease = "15", exportedAt = "2026-10-11T00:00:00Z"),
        ).jsonObject.getValue("events").jsonArray.single().jsonObject
        assertEquals(37L, safe.getValue("occurrences").jsonPrimitive.long)
        assertEquals(1_000L, safe.getValue("firstSeenMillis").jsonPrimitive.long)
        assertEquals(90_000L, safe.getValue("lastSeenMillis").jsonPrimitive.long)
        val lastText = safe.getValue("lastSnapshot").jsonObject.getValue("text").jsonPrimitive.content
        assertFalse("the latest example is redacted like the first", lastText.contains("Oak"))
        assertTrue(lastText.contains("100 m"))

        val full = Json.parseToJsonElement(
            builder.build(ExportMode.FULL, emptyList(), listOf(grouped), appVersion = "0.0.61", androidRelease = "15", exportedAt = "2026-10-11T00:00:00Z"),
        ).jsonObject.getValue("events").jsonArray.single().jsonObject
        assertEquals("In 100 m onto Oak Avenue", full.getValue("lastSnapshot").jsonObject.getValue("text").jsonPrimitive.content)
    }

    @Test
    fun aSingleEventExportsOneOccurrenceSeenOnce() {
        val single = Json.parseToJsonElement(build(ExportMode.PRIVACY_SAFE)).jsonObject.getValue("events").jsonArray.single().jsonObject
        assertEquals(1L, single.getValue("occurrences").jsonPrimitive.long)
        assertEquals(single.getValue("firstSeenMillis"), single.getValue("lastSeenMillis"))
        assertEquals("null", single.getValue("lastSnapshot").toString())
    }
}
