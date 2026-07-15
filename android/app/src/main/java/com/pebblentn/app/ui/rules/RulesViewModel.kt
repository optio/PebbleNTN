package com.pebblentn.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.data.RuleValidationStatus
import com.pebblentn.app.data.UserRule
import com.pebblentn.app.data.UserRuleRepository
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.rules.PreviewResult
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RulePreviewService
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.rules.RuleValidator
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A group of official rules for one navigation app, split by language. */
data class OfficialLanguageGroup(
    /** Canonical locale key ("all" when the rules apply to every language). */
    val locale: String,
    /** User-facing label, e.g. "English" / "All languages". */
    val languageLabel: String,
    val rules: List<Rule>,
)

/** Official rules for one navigation app, grouped so the list reads app -> language -> rules. */
data class OfficialAppGroup(
    val appId: String,
    val displayName: String,
    val languages: List<OfficialLanguageGroup>,
)

/** Backs the Rules screen (official + user tabs) and the expert editor. */
class RulesViewModel(
    private val userRuleRepository: UserRuleRepository,
    private val debugHistoryRepository: DebugHistoryRepository,
    private val previewService: RulePreviewService,
    private val catalog: NavigationAppCatalog,
    val officialRules: List<Rule>,
) : ViewModel() {

    /**
     * Official rules grouped app -> language, so the screen shows a short, navigable tree instead of
     * one flat list of every bundled rule. Computed once: the bundled set is immutable at runtime.
     */
    val officialGroups: List<OfficialAppGroup> = groupOfficialRules(officialRules, catalog)

    val userRules: StateFlow<List<UserRule>> = userRuleRepository.observeUserRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clone(official: Rule): Job = viewModelScope.launch { userRuleRepository.cloneToUser(official) }

    fun setEnabled(ruleId: String, enabled: Boolean): Job =
        viewModelScope.launch { userRuleRepository.setEnabled(ruleId, enabled) }

    fun delete(ruleId: String): Job = viewModelScope.launch { userRuleRepository.delete(ruleId) }

    fun validate(json: String): RuleValidationResult = RuleValidator.validate(json)

    /** Canonical-format command; returns null (leaving the text unchanged) if the JSON won't parse. */
    fun format(json: String): String? =
        runCatching { RulesetCodec.canonicalizeRule(RulesetCodec.parseRule(json)) }.getOrNull()

    /** Save only if valid; returns the validation result so the editor can show errors. */
    suspend fun save(json: String): RuleValidationResult {
        val result = validate(json)
        if (result is RuleValidationResult.Valid) {
            userRuleRepository.save(result.rule, sourceRuleId = null, validationStatus = RuleValidationStatus.VALID)
        }
        return result
    }

    /** Preview a candidate against the most recently captured notification, if any. */
    suspend fun previewAgainstLatest(json: String): PreviewResult? {
        val snapshot = debugHistoryRepository.observeRecent(1).first().firstOrNull()?.snapshot ?: return null
        return previewService.previewCandidate(snapshot, json)
    }

    /** JSON to open the editor with: the user rule's canonical form, or a new-rule template. */
    suspend fun editorInitialJson(ruleId: String?): String =
        ruleId?.let { userRuleRepository.getUserRule(it)?.canonicalJson } ?: NEW_RULE_TEMPLATE

    companion object {
        val NEW_RULE_TEMPLATE: String = """
            {
              "id": "my-rule",
              "enabled": true,
              "priority": 100,
              "packageNames": ["com.google.android.apps.maps"],
              "conditions": [
                {"field": "combinedText", "operator": "containsIgnoreCase", "value": "turn right"}
              ],
              "output": {
                "maneuver": {"type": "literal", "value": "RIGHT"}
              }
            }
        """.trimIndent()
    }
}

/**
 * Group official rules first by their navigation app (resolved through the catalog by package name),
 * then by language. A rule with no `locales` applies to every language and lands under "All
 * languages"; apps and languages are sorted for a stable, readable order.
 */
private fun groupOfficialRules(
    rules: List<Rule>,
    catalog: NavigationAppCatalog,
): List<OfficialAppGroup> {
    return rules
        .groupBy { rule ->
            val pkg = rule.packageNames.firstOrNull().orEmpty()
            val entry = catalog.entryForPackage(pkg)
            AppKey(entry?.appId ?: pkg.ifEmpty { "unknown" }, entry?.displayName ?: pkg.ifEmpty { "Unknown app" })
        }
        .toSortedMap(compareBy({ it.displayName }, { it.appId }))
        .map { (appKey, appRules) ->
            val languages = appRules
                .groupBy { rule -> rule.locales.sorted().joinToString(",").ifEmpty { LOCALE_ALL } }
                .toSortedMap()
                .map { (localeKey, localeRules) ->
                    OfficialLanguageGroup(
                        locale = localeKey,
                        languageLabel = languageLabel(localeKey),
                        rules = localeRules.sortedWith(compareByDescending<Rule> { it.priority }.thenBy { it.id }),
                    )
                }
            OfficialAppGroup(appKey.appId, appKey.displayName, languages)
        }
}

private data class AppKey(val appId: String, val displayName: String)

private const val LOCALE_ALL = "all"

/** A short, human label for a locale key (comma-joined codes, or "all"). */
private fun languageLabel(localeKey: String): String {
    if (localeKey == LOCALE_ALL) return "All languages"
    return localeKey.split(",").joinToString(", ") { code ->
        when (code.lowercase()) {
            "en" -> "English"
            "de" -> "German"
            "fr" -> "French"
            "es" -> "Spanish"
            "it" -> "Italian"
            "nl" -> "Dutch"
            "pt" -> "Portuguese"
            else -> code.uppercase()
        }
    }
}
