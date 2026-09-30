package com.pebblentn.app.ui.rules

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.rules.LiteralExtractor
import com.pebblentn.app.rules.Rule

/**
 * What the official rules list shows (#28): one app or all, one language or all, and a free-text
 * search over rule ids, maintainer comments and condition values. Pure, so the screen stays thin.
 */
data class RuleFilter(
    /** Catalog app id, or null for every app. */
    val appId: String? = null,
    /** Language code such as "en", or null for every language. */
    val language: String? = null,
    val query: String = "",
) {
    /** The groups, apps and rules that pass the filter; empty groups are dropped. */
    fun apply(groups: List<OfficialAppGroup>): List<OfficialAppGroup> = groups
        .filter { appId == null || it.appId == appId }
        .map { app ->
            app.copy(
                languages = app.languages
                    .filter { language == null || it.locale == LOCALE_ALL || language in it.locale.split(",") }
                    .map { group -> group.copy(rules = group.rules.filter(::matchesQuery)) }
                    .filter { it.rules.isNotEmpty() },
            )
        }
        .filter { it.languages.isNotEmpty() }

    private fun matchesQuery(rule: Rule): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        val haystack = sequenceOf(rule.id, rule.comment.orEmpty()) +
            rule.conditions.asSequence().flatMap { sequenceOf(it.value.orEmpty()) + it.values.asSequence() }
        return haystack.any { it.contains(needle, ignoreCase = true) }
    }

    companion object {
        /** The language codes present in [groups] ("all"-language groups don't add a choice). */
        fun languagesIn(groups: List<OfficialAppGroup>): List<String> = groups
            .flatMap { app -> app.languages.flatMap { it.locale.split(",") } }
            .filter { it != LOCALE_ALL }
            .distinct()
            .sorted()

        /** Start on the phone's language when rules exist for it, otherwise show every language. */
        fun initial(groups: List<OfficialAppGroup>, phoneLanguage: String): RuleFilter =
            RuleFilter(language = phoneLanguage.takeIf { it in languagesIn(groups) })
    }
}

/** The maneuver a rule always produces, or null when it is extracted from the notification. */
fun Rule.fixedManeuver(): Maneuver? =
    (output.maneuver as? LiteralExtractor)?.value?.let(Maneuver::fromToken)
