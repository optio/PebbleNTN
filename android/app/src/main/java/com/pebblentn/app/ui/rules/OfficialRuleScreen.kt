package com.pebblentn.app.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.rules.Condition
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RulesetCodec
import com.pebblentn.app.ui.format.DisplayLabels
import kotlinx.coroutines.launch

/**
 * One official rule, full screen (#28): what it matches and what it shows in plain words, the
 * maintainer's note, and the JSON on demand. Official rules are read-only; cloning is how to change
 * one (your rules take precedence).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialRuleScreen(
    rule: Rule?,
    appName: String?,
    onBack: () -> Unit,
    onClone: (Rule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.rule_detail_copied)
    val clonedMessage = stringResource(R.string.rules_cloned)
    var showJson by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(rule?.id.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (rule == null) {
                Text(stringResource(R.string.rules_official_empty))
                return@Column
            }
            val json = remember(rule) { RulesetCodec.canonicalizeRule(rule) }

            Text(
                stringResource(R.string.rules_official_read_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onClone(rule)
                    scope.launch { snackbarHostState.showSnackbar(clonedMessage) }
                }) { Text(stringResource(R.string.rules_clone)) }
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(json))
                    scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                }) { Text(stringResource(R.string.rule_detail_copy)) }
            }

            HorizontalDivider()
            Heading(stringResource(R.string.rule_detail_when))
            if (rule.conditions.isEmpty()) {
                Text(stringResource(R.string.rule_detail_when_always), style = MaterialTheme.typography.bodyLarge)
            }
            rule.conditions.forEach { Text("• " + conditionSummary(it), style = MaterialTheme.typography.bodyLarge) }

            Heading(stringResource(R.string.rule_detail_shows))
            val maneuver = rule.fixedManeuver()
            Text(
                if (maneuver != null) {
                    stringResource(DisplayLabels.maneuver(maneuver))
                } else {
                    stringResource(R.string.rule_detail_shows_extracted)
                },
                style = MaterialTheme.typography.bodyLarge,
            )

            HorizontalDivider()
            Heading(stringResource(R.string.rule_detail_details))
            Detail(stringResource(R.string.rule_detail_app), appName ?: rule.packageNames.joinToString())
            Detail(stringResource(R.string.rule_detail_languages), languageLabel(rule.locales.sorted().joinToString(",").ifEmpty { LOCALE_ALL }))
            Detail(stringResource(R.string.rule_detail_priority), rule.priority.toString())

            rule.comment?.let {
                Heading(stringResource(R.string.rule_detail_why))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()
            TextButton(onClick = { showJson = !showJson }) {
                Text(stringResource(if (showJson) R.string.rule_detail_hide_json else R.string.rule_detail_show_json))
            }
            if (showJson) {
                Text(json, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            }
        }
    }
}

/** "Title matches the pattern “(?i)^ride to”", in the user's language where labels exist. */
@Composable
private fun conditionSummary(condition: Condition): String {
    val field = DisplayLabels.conditionField(condition.field)?.let { stringResource(it) } ?: condition.field
    val operator = stringResource(DisplayLabels.conditionOperator(condition.operator))
    val value = condition.value ?: condition.values.takeIf { it.isNotEmpty() }?.joinToString(", ")
    return if (value == null) {
        stringResource(R.string.condition_summary_no_value, field, operator)
    } else {
        stringResource(R.string.condition_summary, field, operator, value)
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
