package com.pebblentn.app.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.data.RuleOverride
import com.pebblentn.app.data.RuleValidationStatus
import com.pebblentn.app.data.UserRule
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.ui.components.ConfirmDialog
import com.pebblentn.app.ui.format.DisplayLabels
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    officialGroups: List<OfficialAppGroup>,
    userRules: List<UserRule>,
    onOpenOfficial: (String) -> Unit,
    onToggleUser: (String, Boolean) -> Unit,
    onEditUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onNewRule: () -> Unit,
    onBack: () -> Unit = {},
    appName: (String) -> String = { it },
    phoneLanguage: String = Locale.getDefault().language,
    onShareRules: () -> Unit = {},
    onUndoDelete: () -> Unit = {},
    initialAppId: String? = null,
    overrides: Map<String, RuleOverride> = emptyMap(),
    onReviewOverrides: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.rules_deleted)
    val undoLabel = stringResource(R.string.undo)
    // Deleting asks first, then still offers an undo (#28).
    val deleteWithUndo: (String) -> Unit = { id ->
        onDeleteUser(id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onUndoDelete()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rules_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = onNewRule) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_rule))
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Official rules changed under rules of yours that override them (#58).
            val attention = overrides.values.count { it.needsAttention }
            if (attention > 0) {
                OverrideNotice(attention, onReviewOverrides, Modifier.padding(16.dp))
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.rules_tab_official)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.rules_tab_user)) })
            }
            when (selectedTab) {
                0 -> OfficialList(officialGroups, phoneLanguage, onOpenOfficial, initialAppId)
                else -> UserList(userRules, onToggleUser, onEditUser, deleteWithUndo, appName, onShareRules, overrides)
            }
        }
    }
}

/**
 * Official rules with search, app and language filters, and collapsible app → language sections
 * (#28). Starts on the phone's language, since that is the ruleset in use.
 */
@Composable
private fun OfficialList(
    groups: List<OfficialAppGroup>,
    phoneLanguage: String,
    onOpen: (String) -> Unit,
    initialAppId: String?,
) {
    if (groups.isEmpty()) {
        EmptyState(stringResource(R.string.rules_official_empty))
        return
    }
    // Opened from an app (Navigation apps → its rules): start on that app (#28).
    val initial = remember(groups, initialAppId) {
        RuleFilter.initial(groups, phoneLanguage).let { base ->
            if (initialAppId != null && groups.any { it.appId == initialAppId }) {
                val filter = base.copy(appId = initialAppId)
                // Keep the language only if that app has rules for it.
                if (filter.language != null && filter.apply(groups).isEmpty()) filter.copy(language = null) else filter
            } else {
                base
            }
        }
    }
    var appId by rememberSaveable { mutableStateOf(initial.appId) }
    var language by rememberSaveable { mutableStateOf(initial.language) }
    var query by rememberSaveable { mutableStateOf("") }
    // Sections are expanded unless the user collapsed them; keys are "app" and "app/locale".
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }

    val filter = RuleFilter(appId, language, query)
    val visible = remember(groups, filter) { filter.apply(groups) }
    val languagesForApp = remember(groups, appId) { RuleFilter.languagesIn(RuleFilter(appId = appId).apply(groups)) }

    LazyColumn {
        item(key = "controls") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.rules_precedence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.rules_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                            }
                        }
                    },
                )
                ChipRow {
                    FilterChip(
                        selected = appId == null,
                        onClick = { appId = null },
                        label = { Text(stringResource(R.string.rules_filter_all_apps)) },
                    )
                    groups.forEach { app ->
                        FilterChip(
                            selected = appId == app.appId,
                            onClick = {
                                appId = app.appId
                                // Keep the language only if that app has rules for it.
                                val available = RuleFilter.languagesIn(RuleFilter(appId = app.appId).apply(groups))
                                if (language != null && language !in available) language = null
                            },
                            label = { Text(app.displayName) },
                        )
                    }
                }
                if (languagesForApp.isNotEmpty()) {
                    ChipRow {
                        FilterChip(
                            selected = language == null,
                            onClick = { language = null },
                            label = { Text(stringResource(R.string.rules_all_languages)) },
                        )
                        languagesForApp.forEach { code ->
                            FilterChip(
                                selected = language == code,
                                onClick = { language = code },
                                label = { Text(languageLabel(code)) },
                            )
                        }
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item(key = "no-results") {
                Text(
                    stringResource(R.string.rules_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        visible.forEach { app ->
            val appKey = app.appId
            val appCount = app.languages.sumOf { it.rules.size }
            item(key = "app-$appKey") {
                SectionHeader(
                    title = app.displayName,
                    count = appCount,
                    expanded = appKey !in collapsed,
                    onToggle = { collapsed = collapsed.toggle(appKey) },
                    level = 0,
                )
            }
            if (appKey in collapsed) return@forEach
            app.languages.forEach { group ->
                val groupKey = "$appKey/${group.locale}"
                item(key = "lang-$groupKey") {
                    SectionHeader(
                        title = languageLabel(group.locale),
                        count = group.rules.size,
                        expanded = groupKey !in collapsed,
                        onToggle = { collapsed = collapsed.toggle(groupKey) },
                        level = 1,
                    )
                }
                if (groupKey !in collapsed) {
                    items(group.rules, key = { "rule-${it.id}" }) { rule ->
                        OfficialRuleRow(rule, onClick = { onOpen(rule.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit, level: Int) {
    val action = stringResource(if (expanded) R.string.cd_collapse_section else R.string.cd_expand_section, title)
    ListItem(
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = action, onClick = onToggle)
            .padding(start = (level * 16).dp),
        headlineContent = {
            Text(
                title,
                style = if (level == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            )
        },
        supportingContent = { Text(pluralStringResource(R.plurals.rules_count, count, count)) },
        trailingContent = {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
            )
        },
    )
}

/** One official rule: its id, and what it shows on the watch. Tapping opens the rule detail. */
@Composable
private fun OfficialRuleRow(rule: Rule, onClick: () -> Unit) {
    val maneuver = rule.fixedManeuver()
    ListItem(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 32.dp),
        headlineContent = { Text(rule.id) },
        supportingContent = {
            Text(
                if (maneuver != null) {
                    stringResource(R.string.rules_summary_fixed, stringResource(DisplayLabels.maneuver(maneuver)), rule.priority)
                } else {
                    stringResource(R.string.rules_summary_extracted, rule.priority)
                },
            )
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}

/** "English", "German, French", or "All languages", from the system's own language names. */
@Composable
fun languageLabel(localeKey: String): String {
    if (localeKey == LOCALE_ALL) return stringResource(R.string.rules_all_languages)
    val display = Locale.getDefault()
    return localeKey.split(",").joinToString(", ") { code ->
        Locale.forLanguageTag(code).getDisplayLanguage(display).replaceFirstChar { it.titlecase(display) }
            .ifEmpty { code.uppercase() }
    }
}

private fun Set<String>.toggle(key: String): Set<String> = if (key in this) this - key else this + key

@Composable
private fun UserList(
    rules: List<UserRule>,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    appName: (String) -> String,
    onShare: () -> Unit,
    overrides: Map<String, RuleOverride> = emptyMap(),
) {
    if (rules.isEmpty()) {
        EmptyState(stringResource(R.string.rules_user_empty))
        return
    }
    var confirmingDelete by remember { mutableStateOf<String?>(null) }
    confirmingDelete?.let { ruleId ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_rule_title),
            message = stringResource(R.string.confirm_delete_rule_message, ruleId),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmingDelete = null
                onDelete(ruleId)
            },
            onDismiss = { confirmingDelete = null },
        )
    }
    LazyColumn {
        // Send custom rules to the developers the same way as logs: reviewed, then emailed (#28).
        item(key = "share") {
            ListItem(
                modifier = Modifier.clickable(role = Role.Button, onClick = onShare),
                leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.rules_share_yours)) },
                supportingContent = { Text(stringResource(R.string.rules_share_yours_hint)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            )
            HorizontalDivider()
        }
        items(rules, key = { it.ruleId }) { rule ->
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.ruleId, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule.ruleId, it) })
                }
                Text(appName(rule.packageName), style = MaterialTheme.typography.bodySmall)
                overrides[rule.ruleId]?.let { override ->
                    Text(
                        overrideLabel(override),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (override.needsAttention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (rule.validationStatus == RuleValidationStatus.INVALID) {
                    Text(stringResource(R.string.rules_invalid_badge), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    TextButton(onClick = { onEdit(rule.ruleId) }) { Text(stringResource(R.string.rules_edit)) }
                    TextButton(onClick = { confirmingDelete = rule.ruleId }) { Text(stringResource(R.string.rules_delete)) }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
