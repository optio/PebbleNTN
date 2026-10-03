package com.pebblentn.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.data.RuleValidationStatus
import com.pebblentn.app.data.UserRule
import com.pebblentn.app.data.UserRuleRepository
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.rules.PreviewResult
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RulePreviewService
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.rules.RuleValidator
import com.pebblentn.app.rules.RulesetCodec
import com.pebblentn.app.data.RuleOverride
import com.pebblentn.app.data.RuleOverrides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A group of official rules for one navigation app, split by language. */
data class OfficialLanguageGroup(
    /**
     * Canonical locale key: comma-joined language codes, or [LOCALE_ALL] when the rules apply to
     * every language. The screen turns it into a label (system language names, string resources).
     */
    val locale: String,
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

    /** Language codes the official rules cover, per app id, for the navigation apps screen (#28). */
    val languagesByApp: Map<String, List<String>> = officialGroups.associate { app ->
        app.appId to RuleFilter.languagesIn(listOf(app))
    }

    /** An official rule by id, for the rule detail screen. */
    fun officialRule(id: String): Rule? = officialRules.firstOrNull { it.id == id }

    /** The app an official rule belongs to, for the rule detail screen. */
    fun officialRuleAppName(rule: Rule): String? =
        rule.packageNames.firstNotNullOfOrNull { catalog.entryForPackage(it)?.displayName }

    val userRules: StateFlow<List<UserRule>> = userRuleRepository.observeUserRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clone(official: Rule): Job = viewModelScope.launch { userRuleRepository.cloneToUser(official) }

    /**
     * User rules that override official rules (#58), by user rule id: copies (identical, edited, or
     * behind a newer official rule) and other rules that take over notifications an official rule
     * handles, judged on recent captures.
     */
    val overrides: StateFlow<Map<String, RuleOverride>> =
        combine(userRuleRepository.observeUserRules(), debugHistoryRepository.observeRecent(OVERRIDE_CAPTURES)) { rules, events ->
            RuleOverrides.analyze(
                userRules = rules,
                official = officialRules,
                captures = events.mapNotNull { it.snapshot },
                locale = java.util.Locale.getDefault().toLanguageTag(),
            ).associateBy { it.userRuleId }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Revert to the official rules: remove only these overlapping user rules (#58). */
    fun revert(ruleIds: Collection<String>): Job = viewModelScope.launch { userRuleRepository.deleteAll(ruleIds) }

    /** Keep this user rule despite the newer official rule; asked again when it changes once more. */
    fun keepMine(override: RuleOverride): Job = viewModelScope.launch {
        override.officialFingerprint?.let { userRuleRepository.keepMine(override.userRuleId, it) }
    }

    /** The user rule's and the official rule's JSON, for the Compare screen. */
    suspend fun comparison(ruleId: String): Pair<String, String?>? {
        val mine = userRuleRepository.getUserRule(ruleId) ?: return null
        val officialId = overrides.value[ruleId]?.officialRuleIds?.firstOrNull() ?: mine.sourceRuleId ?: ruleId
        // Both sides in the same canonical layout, so only real differences are highlighted.
        val mineJson = runCatching { RulesetCodec.canonicalizeRule(RulesetCodec.parseRule(mine.canonicalJson)) }
            .getOrDefault(mine.canonicalJson)
        return mineJson to officialRule(officialId)?.let(RulesetCodec::canonicalizeRule)
    }

    fun setEnabled(ruleId: String, enabled: Boolean): Job =
        viewModelScope.launch { userRuleRepository.setEnabled(ruleId, enabled) }

    /** The most recently deleted rule, so the delete can be undone (#28). */
    private var lastDeleted: UserRule? = null

    fun delete(ruleId: String): Job = viewModelScope.launch {
        lastDeleted = userRuleRepository.getUserRule(ruleId)
        userRuleRepository.delete(ruleId)
    }

    /** Restore the rule deleted last, if any. */
    fun undoDelete(): Job = viewModelScope.launch {
        lastDeleted?.let { userRuleRepository.restore(it) }
        lastDeleted = null
    }

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

    /** Recent captured notifications the editor can preview against (#28), newest first. */
    val recentCaptures: StateFlow<List<DebugEvent>> = debugHistoryRepository.observeRecent(RECENT_CAPTURES)
        .map { events -> events.filter { it.snapshot != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Preview a candidate against the chosen capture, or the most recent one when [eventId] is null.
     * Null when there is no capture to preview against.
     */
    suspend fun previewAgainst(json: String, eventId: Long?): PreviewResult? {
        val event = eventId?.let { debugHistoryRepository.getById(it) }
            ?: debugHistoryRepository.observeRecent(1).first().firstOrNull()
        val snapshot = event?.snapshot ?: return null
        return previewService.previewCandidate(snapshot, json)
    }

    /** A starting rule made from a captured notification ("Create rule from this notification"). */
    suspend fun editorJsonFromCapture(eventId: Long): String? =
        debugHistoryRepository.getById(eventId)?.let { RulesetCodec.canonicalizeRule(RuleTemplates.fromCapture(it)) }

    /** JSON to open the editor with: the user rule's canonical form, or a new-rule template. */
    suspend fun editorInitialJson(ruleId: String?): String =
        ruleId?.let { userRuleRepository.getUserRule(it)?.canonicalJson } ?: NEW_RULE_TEMPLATE

    companion object {
        private const val RECENT_CAPTURES = 20

        /** Recent notifications checked for user rules that take over official ones. */
        private const val OVERRIDE_CAPTURES = 200

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
                        rules = localeRules.sortedWith(compareByDescending<Rule> { it.priority }.thenBy { it.id }),
                    )
                }
            OfficialAppGroup(appKey.appId, appKey.displayName, languages)
        }
}

private data class AppKey(val appId: String, val displayName: String)

/** Locale key of rules that apply to every language. */
const val LOCALE_ALL = "all"
