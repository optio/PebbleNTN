package com.pebblentn.app.rules

import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.data.RuleOverrides
import com.pebblentn.app.data.UserRule
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.icon.IconCandidates
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules apply to the whole rule family (#113, REQ-RULE-015): MicroG Maps (`app.morphe.android.apps.maps`)
 * uses the Google Maps rules with no rule file listing its package, and a user rule written for either
 * package applies to both. The family comes from the real bundled catalog.
 */
class RuleFamiliesMatchingTest {

    private val google = "com.google.android.apps.maps"
    private val microg = "app.morphe.android.apps.maps"

    private fun resource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("resource not found: $path")

    private val catalog = NavigationAppCatalog.parse(resource("/catalog/navigation-apps.json"))
    private val engine = RuleEngine(families = catalog.ruleFamilies())

    private val googleRulesets: List<Ruleset> by lazy {
        listOf("any", "en", "it", "fr", "es", "de", "nl", "zh", "pt", "pl")
            .map { RulesetCodec.parse(resource("/rules/bundled/google-maps/$it.json")) }
    }
    private val bundled by lazy { LayeredRules(bundled = googleRulesets.flatMap { it.rules }) }

    /** Every Google Maps fixture, as (name, locale, snapshot fields). */
    private val fixtures: List<Triple<String, String, JsonObject>> by lazy {
        Json.parseToJsonElement(resource("/fixtures/google-maps.json")).jsonObject["fixtures"]!!.jsonArray
            .map { it.jsonObject }
            .filter { it["packageName"]!!.jsonPrimitive.content == google }
            .map { Triple(it["name"]!!.jsonPrimitive.content, it["locale"]!!.jsonPrimitive.content, it["snapshot"]!!.jsonObject) }
    }

    private fun snapshot(pkg: String, fields: JsonObject) = NotificationSnapshot(
        packageName = pkg,
        notificationId = 1,
        title = fields["title"]?.jsonPrimitive?.content,
        text = fields["text"]?.jsonPrimitive?.content,
        subText = fields["subText"]?.jsonPrimitive?.content,
        bigText = fields["bigText"]?.jsonPrimitive?.content,
        iconDrawable = fields["iconDrawable"]?.jsonPrimitive?.content,
    )

    private fun fixture(name: String) = fixtures.single { it.first == name }

    private fun assertSameResult(name: String) {
        val (_, locale, fields) = fixture(name)
        val asGoogle = engine.evaluate(snapshot(google, fields), bundled, locale)
        val asMicrog = engine.evaluate(snapshot(microg, fields), bundled, locale)
        assertTrue("$name should match for Google Maps", asGoogle.matched)
        assertEquals("$name: same rule", asGoogle.matchedRuleId, asMicrog.matchedRuleId)
        assertEquals("$name: same instruction", asGoogle.instruction, asMicrog.instruction)
        assertEquals("$name: same trace", asGoogle.trace, asMicrog.trace)
    }

    @Test
    fun anIconOnlyCardGivesTheSameResultForMicrogMaps() = assertSameResult("classic-icon-turn-left")

    @Test
    fun anEnglishTextCardGivesTheSameResultForMicrogMaps() = assertSameResult("eta-subtext-must-not-mean-arrive")

    @Test
    fun aFrenchTextCardGivesTheSameResultForMicrogMaps() = assertSameResult("fr-turn-right")

    @Test
    fun everyGoogleMapsFixtureGivesTheSameResultForMicrogMaps() {
        assertTrue(fixtures.size > 100)
        for ((name, locale, fields) in fixtures) {
            val asGoogle = engine.evaluate(snapshot(google, fields), bundled, locale)
            val asMicrog = engine.evaluate(snapshot(microg, fields), bundled, locale)
            assertEquals("$name: same rule", asGoogle.matchedRuleId, asMicrog.matchedRuleId)
            assertEquals("$name: same instruction", asGoogle.instruction, asMicrog.instruction)
        }
    }

    @Test
    fun noBundledRuleListsTheMicrogPackage() {
        assertTrue(googleRulesets.flatMap { it.rules }.none { microg in it.packageNames })
    }

    @Test
    fun anUnrelatedPackageMatchesNoGoogleMapsRule() {
        val (_, locale, fields) = fixture("fr-turn-right")
        val result = engine.evaluate(snapshot("com.waze", fields), bundled, locale)
        assertFalse(result.matched)
        assertTrue("no rule is even evaluated", result.trace.isEmpty())
        assertFalse(engine.evaluate(snapshot("com.example.unknown", fields), bundled, locale).matched)
    }

    @Test
    fun withoutFamiliesOnlyTheListedPackageMatches() {
        val (_, locale, fields) = fixture("fr-turn-right")
        assertFalse(RuleEngine().evaluate(snapshot(microg, fields), bundled, locale).matched)
    }

    private fun userRule(pkg: String) = Rule(
        id = "user-exit-$pkg",
        enabled = true,
        priority = 900,
        packageNames = listOf(pkg),
        conditions = listOf(Condition("title", ConditionOperator.CONTAINS, "Take the exit")),
        output = RuleOutput(maneuver = LiteralExtractor("RIGHT")),
    )

    private fun takeTheExit(pkg: String) = NotificationSnapshot(packageName = pkg, notificationId = 1, title = "Take the exit", text = "1 km")

    @Test
    fun aUserRuleForGoogleMapsAppliesToMicrogMaps() {
        val rules = LayeredRules(user = listOf(userRule(google)), bundled = bundled.bundled)
        for (pkg in listOf(google, microg)) {
            val result = engine.evaluate(takeTheExit(pkg), rules, "en")
            assertEquals(pkg, "user-exit-$google", result.matchedRuleId)
            assertEquals(RuleLayer.USER, result.matchedLayer)
        }
    }

    @Test
    fun aUserRuleForMicrogMapsAppliesToGoogleMaps() {
        val rules = LayeredRules(user = listOf(userRule(microg)))
        assertEquals("user-exit-$microg", engine.evaluate(takeTheExit(google), rules, "en").matchedRuleId)
        assertEquals("user-exit-$microg", engine.evaluate(takeTheExit(microg), rules, "en").matchedRuleId)
        assertNull(engine.evaluate(takeTheExit("com.waze"), rules, "en").matchedRuleId)
    }

    @Test
    fun aMicrogUserRuleTakesOverGoogleMapsCaptures() {
        val mine = userRule(microg)
        val stored = UserRule(
            ruleId = mine.id,
            sourceRuleId = null,
            packageName = microg,
            rule = mine,
            canonicalJson = RulesetCodec.canonicalizeRule(mine),
            enabled = true,
            validationStatus = com.pebblentn.app.data.RuleValidationStatus.VALID,
            updatedAt = 0,
        )
        val official = listOf(
            Rule(
                id = "official-exit",
                enabled = true,
                priority = 100,
                packageNames = listOf(google),
                conditions = listOf(Condition("title", ConditionOperator.EXISTS)),
                output = RuleOutput(maneuver = LiteralExtractor("STRAIGHT")),
            ),
        )
        val overrides = RuleOverrides.analyze(listOf(stored), official, captures = listOf(takeTheExit(google)), engine = engine)
        assertEquals(listOf("official-exit"), overrides.single().officialRuleIds)
    }

    @Test
    fun microgMapsGetsTheGoogleMapsIconTable() {
        val families = catalog.ruleFamilies()
        val googleIcons = IconCandidates.forPackage(bundled, google, families)
        assertTrue(googleIcons.isNotEmpty())
        assertEquals(googleIcons, IconCandidates.forPackage(bundled, microg, families))
        assertTrue(IconCandidates.forPackage(bundled, "com.waze", families).isEmpty())
    }

    @Test
    fun microgEventsRecordTheGoogleMapsRulesetVersions() {
        val families = catalog.ruleFamilies()
        val versions = AssetRuleRepository.rulesetVersionsFor(googleRulesets, google, families)
        assertEquals(googleRulesets.size, versions.size)
        assertEquals(versions, AssetRuleRepository.rulesetVersionsFor(googleRulesets, microg, families))
        assertEquals(emptyList<String>(), AssetRuleRepository.rulesetVersionsFor(googleRulesets, "com.waze", families))
    }
}
