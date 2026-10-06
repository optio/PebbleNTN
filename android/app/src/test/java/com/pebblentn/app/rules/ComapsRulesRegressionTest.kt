package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rule regression for the bundled CoMaps ruleset (AGENTS.md rule 14: rule changes require fixtures +
 * regression tests).
 *
 * CoMaps (an Organic Maps fork) structures its navigation notification differently from Google Maps:
 * the title is the distance to the next turn ("92 m"), the text is the road ("Kroonstraat"), and the
 * turn DIRECTION is only a graphical arrow icon — never text (real capture, 2026-07-30). The app
 * matches that icon against CoMaps' own turn drawables and passes the drawable's name as
 * `iconDrawable` (#74); the ruleset maps it to a maneuver and stays UNKNOWN without a match. These
 * tests pin that mapping and guard the title-distance gate against non-navigation notifications.
 */
class ComapsRulesRegressionTest {

    @Serializable
    private data class FixtureSnapshot(
        val title: String? = null,
        val text: String? = null,
        val subText: String? = null,
        val bigText: String? = null,
        /** The large icon's matched drawable (#74), as the app's icon recognizer sets it. */
        val iconDrawable: String? = null,
    )

    @Serializable
    private data class Expected(
        val matched: Boolean = true,
        val maneuver: String? = null,
        val distanceMeters: Int? = null,
        val ruleId: String? = null,
        val secondaryText: String? = null,
        val primaryText: String? = null,
        /** The road line must be empty (#57): a card with only a distance must not repeat it there. */
        val noPrimaryText: Boolean = false,
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

    private val bundledRules: LayeredRules by lazy {
        LayeredRules(bundled = RulesetCodec.parse(resource("/rules/bundled/comaps/any.json")).rules)
    }

    private val fixtures: List<Fixture> by lazy {
        json.decodeFromString(FixtureFile.serializer(), resource("/fixtures/comaps.json")).fixtures
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
                iconDrawable = fixture.snapshot.iconDrawable,
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
            fixture.expected.primaryText?.let { expected ->
                assertEquals("fixture '${fixture.name}' road line", expected, result.instruction.primaryText)
            }
            if (fixture.expected.noPrimaryText) {
                assertEquals("fixture '${fixture.name}' road line must be empty", null, result.instruction.primaryText)
            }
        }
    }

    /**
     * The real capture that motivated the ruleset: CoMaps posts many identical "92 m / Kroonstraat"
     * updates while stationary. Each must produce a matched instruction (distance + road), with the
     * maneuver left UNKNOWN because the direction is icon-only — never a fabricated STRAIGHT arrow.
     */
    @Test
    fun iconOnlyTurnYieldsDistanceAndRoadButUnknownManeuver() {
        val snapshot = NotificationSnapshot(
            packageName = "app.comaps.google",
            notificationId = 1,
            title = "92 m",
            text = "Kroonstraat",
        )
        val result = engine.evaluate(snapshot, bundledRules, locale = "nl", nowEpochSeconds = 0)
        assertTrue("should match the CoMaps navigation rule", result.matched)
        assertEquals("distance comes from the title", 92, result.instruction!!.distanceMeters)
        assertEquals("the road is surfaced as primary text", "Kroonstraat", result.instruction.primaryText)
        assertEquals("direction is icon-only, so maneuver stays UNKNOWN", "UNKNOWN", result.instruction.maneuver.name)
    }
}
