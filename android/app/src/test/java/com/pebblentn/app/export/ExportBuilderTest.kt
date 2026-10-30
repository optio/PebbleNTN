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
import com.pebblentn.app.update.InstallKind
import com.pebblentn.app.update.InstallRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
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

    // --- FR7: user-added packages -----------------------------------------------------------------

    private val customPackage = "app.revanced.android.apps.maps"

    private val userAdded = mapOf("microg-maps" to listOf(customPackage), "osmand" to listOf("net.osmand.patched"))

    private fun exported(mode: ExportMode, added: Map<String, List<String>> = userAdded) = Json.parseToJsonElement(
        builder.build(
            mode,
            emptyList(),
            listOf(event, event.copy(id = 2, packageName = customPackage, snapshot = event.snapshot?.copy(packageName = customPackage))),
            appVersion = "0.0.79",
            androidRelease = "15",
            exportedAt = "2026-10-28T00:00:00Z",
            userAddedPackages = added,
        ),
    ).jsonObject

    @Test
    fun anEventFromAUserAddedPackageIsFlaggedCustomAndABuiltInOneIsNot() {
        for (mode in listOf(ExportMode.PRIVACY_SAFE, ExportMode.FULL)) {
            val (builtIn, custom) = exported(mode).getValue("events").jsonArray.map { it.jsonObject }
            assertEquals("com.google.android.apps.maps", builtIn.getValue("packageName").jsonPrimitive.content)
            assertFalse("no flag on a built-in package", "customPackage" in builtIn)
            assertEquals(customPackage, custom.getValue("packageName").jsonPrimitive.content)
            assertEquals("true", custom.getValue("customPackage").jsonPrimitive.content)
        }
    }

    @Test
    fun theHeaderListsTheUserAddedPackagesPerAppInEveryMode() {
        for (mode in ExportMode.entries) {
            val listed = exported(mode).getValue("metadata").jsonObject.getValue("userAddedPackages").jsonObject
            assertEquals(setOf("microg-maps", "osmand"), listed.keys)
            assertEquals(listOf(customPackage), listed.getValue("microg-maps").jsonArray.map { it.jsonPrimitive.content })
            assertEquals(listOf("net.osmand.patched"), listed.getValue("osmand").jsonArray.map { it.jsonPrimitive.content })
        }
    }

    @Test
    fun withoutUserAddedPackagesTheHeaderListIsEmptyAndNoEventIsFlagged() {
        val root = exported(ExportMode.FULL, added = emptyMap())
        assertTrue(root.getValue("metadata").jsonObject.getValue("userAddedPackages").jsonObject.isEmpty())
        assertTrue(root.getValue("events").jsonArray.none { "customPackage" in it.jsonObject })
    }

    // --- REQ-DEBUG-015 (#119): the install section -------------------------------------------------

    private fun installSection(mode: ExportMode, record: InstallRecord) = Json.parseToJsonElement(
        builder.build(
            mode,
            listOf(userRule),
            listOf(event),
            appVersion = "0.0.80",
            androidRelease = "15",
            exportedAt = "2026-10-30T00:00:00Z",
            installRecord = record,
        ),
    ).jsonObject.getValue("metadata").jsonObject.getValue("install").jsonObject

    @Test
    fun theInstallSectionIsInEveryModeWithEveryField() {
        // 2026-10-01T10:00:00Z and 2026-10-29T08:30:00Z.
        val record = InstallRecord(InstallKind.FDROID, "org.fdroid.fdroid", "org.fdroid.fdroid", null, 1_790_848_800_000L, 1_793_262_600_000L)
        for (mode in ExportMode.entries) {
            val install = installSection(mode, record)
            assertEquals(
                setOf("installSource", "installingPackage", "initiatingPackage", "originatingPackage", "firstInstallTime", "lastUpdateTime", "updatedSinceInstall"),
                install.keys,
            )
            assertEquals("fdroid", install.getValue("installSource").jsonPrimitive.content)
            assertEquals("org.fdroid.fdroid", install.getValue("installingPackage").jsonPrimitive.content)
            assertEquals("org.fdroid.fdroid", install.getValue("initiatingPackage").jsonPrimitive.content)
            assertEquals(JsonNull, install.getValue("originatingPackage"))
            assertEquals("2026-10-01T10:00:00Z", install.getValue("firstInstallTime").jsonPrimitive.content)
            assertEquals("2026-10-29T08:30:00Z", install.getValue("lastUpdateTime").jsonPrimitive.content)
            assertEquals(true, install.getValue("updatedSinceInstall").jsonPrimitive.boolean)
        }
    }

    @Test
    fun eachKindIsExportedInLowerCase() {
        for (kind in InstallKind.entries) {
            assertEquals(kind.name.lowercase(), installSection(ExportMode.RULES_ONLY, InstallRecord(kind)).getValue("installSource").jsonPrimitive.content)
        }
    }

    @Test
    fun aFailedLookupExportsUnknownAndNulls() {
        val install = installSection(ExportMode.PRIVACY_SAFE, InstallRecord.UNKNOWN)
        assertEquals("unknown", install.getValue("installSource").jsonPrimitive.content)
        for (field in listOf("installingPackage", "initiatingPackage", "originatingPackage", "firstInstallTime", "lastUpdateTime", "updatedSinceInstall")) {
            assertEquals(field, JsonNull, install.getValue(field))
        }
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
            assertEquals(5L, metadata.getValue("schemaVersion").jsonPrimitive.long)
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
