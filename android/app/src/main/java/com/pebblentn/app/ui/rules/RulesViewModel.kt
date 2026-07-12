package com.pebblentn.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** Backs the Rules screen (official + user tabs) and the expert editor. */
class RulesViewModel(
    private val userRuleRepository: UserRuleRepository,
    private val debugHistoryRepository: DebugHistoryRepository,
    private val previewService: RulePreviewService,
    val officialRules: List<Rule>,
) : ViewModel() {

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
