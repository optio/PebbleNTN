package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rule regression for the bundled OsmAnd ruleset (AGENTS.md rule 14).
 *
 * OsmAnd puts the whole step in the notification title as `"<distance> • <instruction>"`
 * (BigTextStyle; text/subText null). Only turn-left/right are capture-confirmed (real FULL export,
 * 2026-08); the other maneuvers are authored from OsmAnd's known English phrasing and marked
 * synthetic in `rules/fixtures/osmand.json`. ARRIVE sits at the bottom of the ladder for the same
 * reason as the localized Google Maps rulesets: the title carries the destination road name.
 */
class OsmandRulesRegressionTest {

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
        /** The road line on the watch (#26: no leading "200 m • "). */
        val primaryText: String? = null,
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
        // Every bundled OsmAnd language; the engine picks by each fixture's locale (#16 Italian, #61 German, #57 fr/es/nl/pt/pl/zh).
        LayeredRules(bundled = listOf("en", "it", "de", "fr", "es", "nl", "pt", "pl", "zh").flatMap { RulesetCodec.parse(resource("/rules/bundled/osmand/$it.json")).rules })
    }

    private val fixtures: List<Fixture> by lazy {
        json.decodeFromString(FixtureFile.serializer(), resource("/fixtures/osmand.json")).fixtures
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
            fixture.expected.primaryText?.let { expected ->
                assertEquals("fixture '${fixture.name}' road line", expected, result.instruction!!.primaryText)
            }
        }
    }

    /**
     * ARRIVE must rank below every maneuver rule so a title that carries an explicit maneuver (and a
     * destination road name that may begin with an arrival stem) resolves as that maneuver.
     */
    @Test
    fun arriveRanksBelowEveryManeuverRule() {
        // Per language (the id's suffix; the Chinese rules have no locales): each language's arrive
        // rule against that language's maneuver rules.
        for ((language, rules) in bundledRules.bundled.groupBy { it.id.substringAfterLast('-') }) {
            val arrive = rules.single { it.id == "osmand-arrive-$language" }
            val lowestManeuver = rules.filterNot { it.id == arrive.id }.minOf { it.priority }
            assertTrue(
                "${arrive.id} (priority ${arrive.priority}) must rank below every maneuver rule " +
                    "(lowest is $lowestManeuver)",
                arrive.priority < lowestManeuver,
            )
        }
    }
}
