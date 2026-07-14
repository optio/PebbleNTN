package com.pebblentn.app.ui.rules

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.data.RuleValidationStatus
import com.pebblentn.app.data.UserRule
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RulesetCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    officialGroups: List<OfficialAppGroup>,
    userRules: List<UserRule>,
    onClone: (Rule) -> Unit,
    onToggleUser: (String, Boolean) -> Unit,
    onEditUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onNewRule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.rules_title)) }) },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = onNewRule) { Text("+") }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.rules_tab_official)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.rules_tab_user)) })
            }
            when (selectedTab) {
                0 -> OfficialList(officialGroups, onClone)
                else -> UserList(userRules, onToggleUser, onEditUser, onDeleteUser)
            }
        }
    }
}

@Composable
private fun OfficialList(groups: List<OfficialAppGroup>, onClone: (Rule) -> Unit) {
    if (groups.isEmpty()) {
        EmptyState(stringResource(R.string.rules_official_empty))
        return
    }
    var viewing by remember { mutableStateOf<Rule?>(null) }

    LazyColumn {
        groups.forEach { app ->
            item(key = "app-${app.appId}") {
                Text(
                    app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            app.languages.forEach { language ->
                item(key = "lang-${app.appId}-${language.locale}") {
                    Text(
                        language.languageLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                items(language.rules, key = { it.id }) { rule ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewing = rule }
                            .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(rule.id, style = MaterialTheme.typography.bodyLarge)
                        Text(rule.packageNames.joinToString(), style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { viewing = rule }) { Text(stringResource(R.string.rules_view)) }
                            TextButton(onClick = { onClone(rule) }) { Text(stringResource(R.string.rules_clone)) }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    viewing?.let { rule ->
        OfficialRuleDialog(
            rule = rule,
            onClone = {
                onClone(rule)
                viewing = null
            },
            onDismiss = { viewing = null },
        )
    }
}

/**
 * Read-only view of an official rule: official rules ship with the app and cannot be edited in
 * place — cloning is the supported way to change one (REQ-RULES: user rules override official ones).
 */
@Composable
private fun OfficialRuleDialog(rule: Rule, onClone: () -> Unit, onDismiss: () -> Unit) {
    val json = remember(rule) { RulesetCodec.canonicalizeRule(rule) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(rule.id) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.rules_official_read_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                rule.comment?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = json,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        },
        confirmButton = { TextButton(onClick = onClone) { Text(stringResource(R.string.rules_clone)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun UserList(
    rules: List<UserRule>,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (rules.isEmpty()) {
        EmptyState(stringResource(R.string.rules_user_empty))
        return
    }
    LazyColumn {
        items(rules, key = { it.ruleId }) { rule ->
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.ruleId, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule.ruleId, it) })
                }
                Text(rule.packageName, style = MaterialTheme.typography.bodySmall)
                if (rule.validationStatus == RuleValidationStatus.INVALID) {
                    Text(stringResource(R.string.rules_invalid_badge), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    TextButton(onClick = { onEdit(rule.ruleId) }) { Text(stringResource(R.string.rules_edit)) }
                    TextButton(onClick = { onDelete(rule.ruleId) }) { Text(stringResource(R.string.rules_delete)) }
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
