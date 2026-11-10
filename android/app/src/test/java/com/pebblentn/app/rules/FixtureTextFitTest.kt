package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.protocol.ProtocolCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #134 NFR1: whatever the rules extract from any fixture, both road-line forms the phone may send
 * (Full `primaryText`, Compact `compactText`) fit the watch's 64-byte buffer and the second line its
 * 24 bytes once [ProtocolCodec.fitText] has shortened them, and shortening never drops a value.
 */
class FixtureTextFitTest {

    private fun resource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("resource not found: $path")

    private val apps = mapOf(
        "osmand" to listOf("en", "it", "de", "fr", "es", "nl", "pt", "pl", "zh"),
        "google-maps" to listOf("en", "it", "fr", "es", "de", "nl", "zh", "pt", "pl", "any"),
        "vela-maps" to listOf("en"),
        "organic-maps" to listOf("any"),
        "comaps" to listOf("any"),
    )

    private val families =
        com.pebblentn.app.catalog.NavigationAppCatalog.parse(resource("/catalog/navigation-apps.json")).ruleFamilies()
    private val engine = RuleEngine(families = families, zone = { java.time.ZoneOffset.UTC })

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    @Test
    fun everyFixtureFitsTheWatchBuffersInBothForms() {
        var checked = 0
        for ((app, languages) in apps) {
            val rules = LayeredRules(
                bundled = languages.flatMap { RulesetCodec.parse(resource("/rules/bundled/$app/$it.json")).rules },
            )
            val fixtures = Json.parseToJsonElement(resource("/fixtures/$app.json")).jsonObject["fixtures"]!!.jsonArray
            for (element in fixtures) {
                val fixture = element.jsonObject
                val snap = fixture["snapshot"]!!.jsonObject
                val snapshot = NotificationSnapshot(
                    packageName = fixture.text("packageName")!!,
                    notificationId = 1,
                    title = snap.text("title"),
                    text = snap.text("text"),
                    subText = snap.text("subText"),
                    bigText = snap.text("bigText"),
                    iconDrawable = snap.text("iconDrawable"),
                )
                val instruction = engine.evaluate(snapshot, rules, locale = fixture.text("locale")).instruction ?: continue
                val name = "$app/${fixture.text("name")}"
                assertFits(name, "primaryText", instruction.primaryText, ProtocolCodec.MAX_PRIMARY_BYTES)
                assertFits(name, "compactText", instruction.compactText, ProtocolCodec.MAX_PRIMARY_BYTES)
                assertFits(name, "secondaryText", instruction.secondaryText, ProtocolCodec.MAX_SECONDARY_BYTES)
                checked++
            }
        }
        assertTrue("expected matched fixtures, checked $checked", checked > 400)
    }

    private fun assertFits(name: String, field: String, value: String?, maxBytes: Int) {
        if (value == null) return
        val sent = ProtocolCodec.fitText(value, maxBytes)!!
        assertTrue("$name $field '$sent' exceeds $maxBytes bytes", sent.toByteArray(Charsets.UTF_8).size <= maxBytes)
        assertTrue("$name $field was dropped instead of shortened", sent.isNotBlank())
    }
}
