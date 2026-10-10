package com.pebblentn.app.notification.icon

import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.ManeuverMapExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import com.pebblentn.app.rules.RulesetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The drawable → maneuver candidates come from the rules (#74). */
class IconCandidatesTest {

    private fun rule(id: String, pkg: String, maneuver: com.pebblentn.app.rules.Extractor, enabled: Boolean = true, priority: Int = 100) = Rule(
        id = id, enabled = enabled, priority = priority, packageNames = listOf(pkg),
        conditions = listOf(Condition("title", ConditionOperator.EXISTS)),
        output = RuleOutput(maneuver = maneuver),
    )

    private fun iconMap(vararg pairs: Pair<String, String>) = ManeuverMapExtractor(field = IconCandidates.FIELD, mapping = mapOf(*pairs), default = "UNKNOWN")

    @Test
    fun collectsTheIconMapsOfTheApp() {
        val rules = LayeredRules(bundled = listOf(
            rule("a", "app.x", iconMap("ic_left" to "LEFT")),
            rule("b", "app.x", iconMap("ic_right" to "RIGHT")),
            rule("other-app", "app.y", iconMap("ic_up" to "STRAIGHT")),
            rule("text-rule", "app.x", LiteralExtractor("LEFT")),
            rule("other-field", "app.x", ManeuverMapExtractor(field = "title", mapping = mapOf("Left" to "LEFT"))),
            rule("disabled", "app.x", iconMap("ic_off" to "LEFT"), enabled = false),
        ))
        assertEquals(mapOf("ic_left" to "LEFT", "ic_right" to "RIGHT"), IconCandidates.forPackage(rules, "app.x"))
    }

    @Test
    fun noIconRuleMeansNoCandidatesSoTheIconIsNeverRead() {
        val rules = LayeredRules(bundled = listOf(rule("text-rule", "app.x", LiteralExtractor("LEFT"))))
        assertTrue(IconCandidates.forPackage(rules, "app.x").isEmpty())
    }

    @Test
    fun aUserRuleOverridesTheBundledMeaningOfADrawable() {
        val rules = LayeredRules(
            user = listOf(rule("mine", "app.x", iconMap("ic_exit" to "RIGHT"))),
            bundled = listOf(rule("official", "app.x", iconMap("ic_exit" to "SLIGHT_RIGHT", "ic_left" to "LEFT"))),
        )
        assertEquals(mapOf("ic_exit" to "RIGHT", "ic_left" to "LEFT"), IconCandidates.forPackage(rules, "app.x"))
    }

    @Test
    fun bundledCoMapsAndGoogleMapsRulesProvideTheirDrawables() {
        fun bundled(path: String) = RulesetCodec.parse(javaClass.getResourceAsStream(path)!!.bufferedReader().readText()).rules
        val comaps = IconCandidates.forPackage(LayeredRules(bundled = bundled("/rules/bundled/comaps/any.json")), "app.comaps")
        assertEquals(13, comaps.size)
        assertEquals("ROUNDABOUT", comaps["ic_turn_round"])
        val google = IconCandidates.forPackage(LayeredRules(bundled = bundled("/rules/bundled/google-maps/any.json")), "com.google.android.apps.maps")
        assertEquals(64, google.size)
        assertEquals("LEFT", google["maneuver_turn_normal_left"])
    }
}
