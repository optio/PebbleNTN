package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rule regression for the bundled Vela Maps English ruleset (#120; AGENTS.md rule 14: rule changes
 * require fixtures + regression tests).
 *
 * Vela Maps' card (from its public source): title `In <distance> · <instruction>` while the next
 * maneuver is ahead, the bare instruction at distance 0, `Arrived` at the end and `Navigating`
 * before the first instruction; text `<duration> · <distance> · Arrive <time>`. The ruleset starts
 * from the rules proposed by the #120 reporter, with the gaps fixed (bare titles, exits, roundabouts,
 * U-turn side, "bear", arrival). Every fixture is synthetic until real captures exist.
 */
class VelaMapsRulesRegressionTest {

    @Serializable
    private data class FixtureSnapshot(
        val title: String? = null,
        val text: String? = null,
        val subText: String? = null,
        val bigText: String? = null,
    )

    @Serializable
    private data class Expected(
        val matched: Boolean = true,
        val maneuver: String? = null,
        val distanceMeters: Int? = null,
        val ruleId: String? = null,
        val primaryText: String? = null,
        /** The ETA clock from "Arrive …" in the text. */
        val secondaryText: String? = null,
        /** The distance must be absent: a bare-instruction title carries none. */
        val noDistance: Boolean = false,
    )

    @Serializable
    private data class Fixture(
        val name: String,
        val locale: String,
        val packageName: String,
        val source: String = "synthetic",
        val snapshot: FixtureSnapshot,
        val expected: Expected,
    )

    @Serializable
    private data class FixtureFile(val note: String = "", val fixtures: List<Fixture>)

    private val json = Json { ignoreUnknownKeys = true }
    private val engine = RuleEngine()

    private fun resource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("resource not found: $path")

    private val ruleset: Ruleset by lazy { RulesetCodec.parse(resource("/rules/bundled/vela-maps/en.json")) }

    private val bundledRules: LayeredRules by lazy { LayeredRules(bundled = ruleset.rules) }

    private val fixtures: List<Fixture> by lazy {
        json.decodeFromString(FixtureFile.serializer(), resource("/fixtures/vela-maps.json")).fixtures
    }

    private fun evaluate(title: String?, text: String?, locale: String = "en-US") = engine.evaluate(
        NotificationSnapshot(packageName = "app.vela", notificationId = 1, title = title, text = text),
        bundledRules,
        locale = locale,
        nowEpochSeconds = 0,
    )

    @Test
    fun bundledRulesetIsValidAndListsOnlyVela() {
        assertTrue(ruleset.rules.isNotEmpty())
        assertTrue(ruleset.rules.all { it.packageNames == listOf("app.vela") && it.locales == listOf("en") })
        assertEquals("vela-maps-en-2026.10.30", ruleset.rulesetVersion)
        assertTrue("credits the proposal", "#120" in ruleset.publisher)
    }

    @Test
    fun everyFixtureExtractsExpectedOutput() {
        assertTrue("expected fixtures to exist", fixtures.isNotEmpty())
        for (fixture in fixtures) {
            val snapshot = NotificationSnapshot(
                packageName = fixture.packageName,
                notificationId = 1,
                title = fixture.snapshot.title,
                text = fixture.snapshot.text,
                subText = fixture.snapshot.subText,
                bigText = fixture.snapshot.bigText,
            )
            val result = engine.evaluate(snapshot, bundledRules, locale = fixture.locale, nowEpochSeconds = 0)
            val name = fixture.name

            if (!fixture.expected.matched) {
                assertEquals("fixture '$name' must not match any rule (matched ${result.matchedRuleId})", null, result.instruction)
                continue
            }
            assertTrue("fixture '$name' should match a rule", result.matched)
            val instruction = result.instruction!!
            assertEquals("fixture '$name' maneuver", fixture.expected.maneuver, instruction.maneuver.name)
            fixture.expected.ruleId?.let { assertEquals("fixture '$name' matched rule", it, result.matchedRuleId) }
            fixture.expected.distanceMeters?.let { assertEquals("fixture '$name' distance", it, instruction.distanceMeters) }
            fixture.expected.primaryText?.let { assertEquals("fixture '$name' primary text", it, instruction.primaryText) }
            fixture.expected.secondaryText?.let { assertEquals("fixture '$name' ETA", it, instruction.secondaryText) }
            if (fixture.expected.noDistance) {
                assertNull("fixture '$name' must carry no distance", instruction.distanceMeters)
            }
        }
    }

    /** Every rule is pinned by at least one fixture, so none can silently stop matching. */
    @Test
    fun everyRuleIsCoveredByAFixture() {
        val pinned = fixtures.mapNotNull { it.expected.ruleId }.toSet()
        assertEquals(emptySet<String>(), ruleset.rules.map { it.id }.toSet() - pinned)
    }

    /** Overlapping rules never depend on id order: every priority is distinct (#120). */
    @Test
    fun everyPriorityIsDistinct() {
        val priorities = ruleset.rules.map { it.priority }
        assertEquals(priorities.size, priorities.toSet().size)
        val byId = ruleset.rules.associate { it.id to it.priority }
        assertTrue("exit above roundabout", byId.getValue("vela-in-distance-exit-en") > byId.getValue("vela-in-distance-roundabout-en"))
        assertTrue("roundabout above turns", byId.getValue("vela-in-distance-roundabout-en") > byId.getValue("vela-in-distance-turn-right-en"))
    }

    /** Vela formats a 12-hour clock with U+202F before AM/PM; the ETA comes out with a plain space. */
    @Test
    fun aNarrowNoBreakSpaceBeforePmGivesAPlainEta() {
        val result = evaluate("In 500 ft · Turn right onto Main St", "12 min · 3.4 mi · Arrive 12:45 PM")
        assertEquals("RIGHT", result.instruction!!.maneuver.name)
        assertEquals("12:45 PM", result.instruction.secondaryText)
    }

    @Test
    fun aMetricInstructionWithoutAClockHasNoEta() {
        val result = evaluate("In 1.5 km · Turn left onto Ringlaan", "", locale = "en-GB")
        assertEquals("LEFT", result.instruction!!.maneuver.name)
        assertEquals(1500, result.instruction.distanceMeters)
        assertNull(result.instruction.secondaryText)
    }

    @Test
    fun anotherLanguageIsNotMatchedByTheEnglishRules() {
        val result = evaluate("In 200 m · Rechts abbiegen auf Hauptstraße", "8 Min. · 2,1 km · Ankunft 14:05", locale = "de-DE")
        assertNull(result.instruction)
    }

    @Test
    fun anotherPackageIsNotMatched() {
        val result = engine.evaluate(
            NotificationSnapshot(packageName = "com.example.other", notificationId = 1, title = "In 500 ft · Turn right onto Main St"),
            bundledRules,
            locale = "en-US",
            nowEpochSeconds = 0,
        )
        assertNull(result.instruction)
    }
}
