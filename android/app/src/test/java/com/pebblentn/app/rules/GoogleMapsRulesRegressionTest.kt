package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rule regression for the bundled Google Maps ruleset (AGENTS.md rule 14: rule changes require
 * fixtures + regression tests). Runs each fixture snapshot through the engine with the bundled
 * ruleset and checks the expected normalized output. Fixtures are synthetic; see
 * rules/fixtures/README.md.
 */
class GoogleMapsRulesRegressionTest {

    @Serializable
    private data class FixtureSnapshot(
        val title: String? = null,
        val text: String? = null,
        val subText: String? = null,
        val bigText: String? = null,
    )

    @Serializable
    private data class Expected(
        /** True unless the fixture asserts that nothing should match. */
        val matched: Boolean = true,
        val maneuver: String? = null,
        val distanceMeters: Int? = null,
        /** When set, the rule that must win — pins precedence, not just the maneuver. */
        val ruleId: String? = null,
        /** The ETA the watch shows in its status strip, extracted from Google Maps' subText. */
        val secondaryText: String? = null,
    )

    @Serializable
    private data class Fixture(
        val name: String,
        val locale: String,
        val packageName: String,
        /** "capture" (derived from a real notification) or "synthetic". */
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

    private val bundledRules: LayeredRules by lazy {
        LayeredRules(bundled = RulesetCodec.parse(resource("/rules/bundled/google-maps.json")).rules)
    }

    private val fixtures: List<Fixture> by lazy {
        json.decodeFromString(FixtureFile.serializer(), resource("/fixtures/google-maps.json")).fixtures
    }

    @Test
    fun bundledRulesetIsValid() {
        assertTrue(bundledRules.bundled.isNotEmpty())
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

            if (!fixture.expected.matched) {
                assertEquals(
                    "fixture '${fixture.name}' must not match any rule (matched ${result.matchedRuleId})",
                    null,
                    result.instruction,
                )
                continue
            }

            assertTrue("fixture '${fixture.name}' should match a rule", result.matched)
            assertEquals(
                "fixture '${fixture.name}' maneuver",
                fixture.expected.maneuver,
                result.instruction!!.maneuver.name,
            )
            fixture.expected.distanceMeters?.let { expected ->
                assertEquals("fixture '${fixture.name}' distance", expected, result.instruction.distanceMeters)
            }
            fixture.expected.ruleId?.let { expected ->
                assertEquals("fixture '${fixture.name}' matched rule", expected, result.matchedRuleId)
            }
            fixture.expected.secondaryText?.let { expected ->
                assertEquals("fixture '${fixture.name}' ETA", expected, result.instruction.secondaryText)
            }
        }
    }

    /**
     * The regression that motivated the fixture rewrite: Google Maps carries the ETA in subText
     * ("Arrive 23:51") on *every* navigation notification. A rule matching /arrive/ on combinedText
     * therefore classified every turn as ARRIVE (real capture, 2026-07-13). ARRIVE must come from
     * the title alone.
     */
    /**
     * The ETA's hour is not a distance. Rules extract distance from combinedText, which includes the
     * subText "Arrive 23:41" — and the parser used to treat a unit-less number as metres, so every
     * step with no real distance showed "23 m" on the watch (real capture, 2026-07-13).
     */
    @Test
    fun etaHourIsNeverReadAsADistance() {
        val snapshot = NotificationSnapshot(
            packageName = "com.google.android.apps.maps",
            notificationId = 1,
            title = "Head east on Sample Avenue",
            subText = "Arrive 23:41",
        )
        val result = engine.evaluate(snapshot, bundledRules, locale = "en", nowEpochSeconds = 0)
        assertTrue("should match the continue rule", result.matched)
        assertEquals("no distance may be invented from the ETA", null, result.instruction!!.distanceMeters)
        assertEquals("the ETA itself is surfaced as secondary text", "23:41", result.instruction.secondaryText)
    }

    @Test
    fun etaInSubTextNeverProducesArrive() {
        for (title in listOf("Head toward Example Street", "Turn left onto Sample Avenue", "At the roundabout, take the 2nd exit")) {
            val snapshot = NotificationSnapshot(
                packageName = "com.google.android.apps.maps",
                notificationId = 1,
                title = title,
                subText = "Arrive 23:51",
            )
            val result = engine.evaluate(snapshot, bundledRules, locale = "en", nowEpochSeconds = 0)
            assertTrue("'$title' should match a rule", result.matched)
            assertTrue(
                "'$title' + ETA subText must not be classified ARRIVE",
                result.instruction!!.maneuver.name != "ARRIVE",
            )
        }
    }
}
