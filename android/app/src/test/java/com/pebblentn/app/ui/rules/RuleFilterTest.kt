package com.pebblentn.app.ui.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.ConditionOperator
import com.pebblentn.app.rules.FieldCopyExtractor
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #28: the official rules list filters by app, language and a search query. */
class RuleFilterTest {

    private fun rule(id: String, pattern: String = "turn", comment: String? = null, maneuver: String? = "RIGHT") = Rule(
        id = id,
        enabled = true,
        priority = 100,
        packageNames = listOf("pkg"),
        conditions = listOf(Condition("title", ConditionOperator.REGEX, pattern)),
        output = RuleOutput(maneuver = maneuver?.let(::LiteralExtractor) ?: FieldCopyExtractor("title")),
        comment = comment,
    )

    private val groups = listOf(
        OfficialAppGroup(
            appId = "comaps",
            displayName = "CoMaps",
            languages = listOf(OfficialLanguageGroup(LOCALE_ALL, listOf(rule("comaps-step", maneuver = null)))),
        ),
        OfficialAppGroup(
            appId = "google-maps",
            displayName = "Google Maps",
            languages = listOf(
                OfficialLanguageGroup("de", listOf(rule("gm-right-de", "rechts"))),
                OfficialLanguageGroup("en", listOf(rule("gm-right-en", "turn right"), rule("gm-lane-en", "use the left lane", comment = "Lane guidance"))),
            ),
        ),
    )

    private fun ids(result: List<OfficialAppGroup>) = result.flatMap { app -> app.languages.flatMap { it.rules.map(Rule::id) } }

    @Test
    fun noFilterShowsEverything() {
        assertEquals(listOf("comaps-step", "gm-right-de", "gm-right-en", "gm-lane-en"), ids(RuleFilter().apply(groups)))
    }

    @Test
    fun appFilterKeepsOneApp() {
        assertEquals(listOf("comaps-step"), ids(RuleFilter(appId = "comaps").apply(groups)))
    }

    @Test
    fun languageFilterKeepsThatLanguageAndAllLanguageRules() {
        assertEquals(listOf("comaps-step", "gm-right-en", "gm-lane-en"), ids(RuleFilter(language = "en").apply(groups)))
    }

    @Test
    fun searchMatchesIdsCommentsAndConditionValuesIgnoringCase() {
        assertEquals(listOf("gm-right-de"), ids(RuleFilter(query = "RECHTS").apply(groups)))
        assertEquals("comment", listOf("gm-lane-en"), ids(RuleFilter(query = "lane guidance").apply(groups)))
        assertEquals("id", listOf("comaps-step"), ids(RuleFilter(query = "comaps").apply(groups)))
    }

    @Test
    fun emptyAppsAndLanguagesDisappear() {
        val result = RuleFilter(query = "rechts").apply(groups)
        assertEquals(listOf("google-maps"), result.map { it.appId })
        assertEquals(listOf("de"), result.single().languages.map { it.locale })
        assertTrue(RuleFilter(query = "nothing like this").apply(groups).isEmpty())
    }

    @Test
    fun startsOnThePhoneLanguageOnlyWhenRulesExistForIt() {
        assertEquals(listOf("de", "en"), RuleFilter.languagesIn(groups))
        assertEquals("en", RuleFilter.initial(groups, "en").language)
        assertNull("no Japanese rules: show every language", RuleFilter.initial(groups, "ja").language)
    }

    @Test
    fun fixedManeuverIsKnownOnlyForLiteralOutputs() {
        assertEquals(Maneuver.RIGHT, rule("a").fixedManeuver())
        assertNull(rule("b", maneuver = null).fixedManeuver())
    }
}
