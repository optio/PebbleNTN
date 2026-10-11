package com.pebblentn.app.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** The ruleset versions recorded with each debug event (#84). */
class RulesetVersionsTest {

    private fun ruleset(version: String, vararg packages: String) = Ruleset(
        schemaVersion = 1,
        rulesetVersion = version,
        minimumAppVersionCode = 1,
        createdAt = "2026-10-11T00:00:00Z",
        publisher = "test",
        rules = listOf(
            Rule(
                id = "r-$version",
                enabled = true,
                priority = 1,
                packageNames = packages.toList(),
                conditions = emptyList(),
                output = RuleOutput(maneuver = LiteralExtractor("LEFT")),
            ),
        ),
    )

    private val rulesets = listOf(
        ruleset("google-maps-any-2026.10.7", "com.google.android.apps.maps"),
        ruleset("osmand-de-2026.10.10", "net.osmand", "net.osmand.plus"),
        ruleset("google-maps-de-2026.10.1", "com.google.android.apps.maps"),
    )

    @Test
    fun listsTheRulesetsHoldingRulesForThePackageInLoadOrder() {
        assertEquals(
            listOf("google-maps-any-2026.10.7", "google-maps-de-2026.10.1"),
            AssetRuleRepository.rulesetVersionsFor(rulesets, "com.google.android.apps.maps"),
        )
        assertEquals(listOf("osmand-de-2026.10.10"), AssetRuleRepository.rulesetVersionsFor(rulesets, "net.osmand.plus"))
    }

    @Test
    fun anAppWithoutRulesHasNone() {
        assertEquals(emptyList<String>(), AssetRuleRepository.rulesetVersionsFor(rulesets, "com.waze"))
    }
}
