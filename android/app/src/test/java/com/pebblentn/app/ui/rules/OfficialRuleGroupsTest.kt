package com.pebblentn.app.ui.rules

import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RulesetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Rules screen's official groups (#120): Vela Maps' rules are listed under "Vela Maps" outside
 * Google Play; on a Google Play install they still run but are not listed. Uses the real bundled
 * catalog and rules.
 */
class OfficialRuleGroupsTest {

    private fun resource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("resource not found: $path")

    private val bundled = NavigationAppCatalog.parse(resource("/catalog/navigation-apps.json"))

    private val rules: List<Rule> =
        listOf("/rules/bundled/vela-maps/en.json", "/rules/bundled/google-maps/en.json", "/rules/bundled/comaps/any.json")
            .flatMap { RulesetCodec.parse(resource(it)).rules }

    @Test
    fun outsidePlayVelaMapsRulesAreListedUnderItsName() {
        val groups = groupOfficialRules(rules, bundled)
        assertEquals(listOf("comaps", "google-maps", "vela-maps"), groups.map { it.appId })
        val vela = groups.single { it.appId == "vela-maps" }
        assertEquals("Vela Maps", vela.displayName)
        assertEquals(listOf("en"), vela.languages.map { it.locale })
    }

    @Test
    fun onPlayVelaMapsRulesAreNotListedAndTheOthersAreUnchanged() {
        val play = groupOfficialRules(rules, bundled.forGooglePlay())
        assertEquals(listOf("comaps", "google-maps"), play.map { it.appId })
        assertTrue(play.none { "Vela" in it.displayName })
        assertEquals(
            groupOfficialRules(rules, bundled).single { it.appId == "google-maps" },
            play.single { it.appId == "google-maps" },
        )
    }

    @Test
    fun aRuleForAPackageOutsideTheCatalogIsStillListedOnPlay() {
        val stray = RulesetCodec.parse(resource("/rules/bundled/vela-maps/en.json")).rules.first()
            .copy(id = "stray-rule", packageNames = listOf("com.example.unknown"))
        val play = groupOfficialRules(listOf(stray), bundled.forGooglePlay())
        assertEquals(listOf("com.example.unknown"), play.map { it.appId })
    }
}
