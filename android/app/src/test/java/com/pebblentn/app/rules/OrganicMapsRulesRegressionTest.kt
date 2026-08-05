package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rule regression for the bundled Organic Maps ruleset (AGENTS.md rule 14).
 *
 * Organic Maps builds the identical navigation notification to its CoMaps fork (verified in OM's
 * NavigationService.java): title = distance-to-turn, text = road, and the turn direction is only a
 * graphical arrow icon — never text. The ruleset therefore emits maneuver = UNKNOWN on purpose and
 * recovers only distance + road. Fixtures are synthetic (no real OM capture yet); see
 * rules/fixtures/organic-maps.json.
 */
class OrganicMapsRulesRegressionTest {

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
        val secondaryText: String? = null,
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
        LayeredRules(bundled = RulesetCodec.parse(resource("/rules/bundled/organic-maps/any.json")).rules)
    }

    private val fixtures: List<Fixture> by lazy {
        json.decodeFromString(FixtureFile.serializer(), resource("/fixtures/organic-maps.json")).fixtures
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
        }
    }
}
