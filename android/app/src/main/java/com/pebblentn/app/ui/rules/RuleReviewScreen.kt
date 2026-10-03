package com.pebblentn.app.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.data.OverrideStatus
import com.pebblentn.app.data.RuleOverride
import com.pebblentn.app.data.UserRule

/** What a user rule does to the official rules, in one sentence (#58). */
@Composable
fun overrideLabel(override: RuleOverride): String {
    val official = override.officialRuleIds.joinToString(", ")
    return stringResource(
        when (override.status) {
            OverrideStatus.IDENTICAL -> R.string.rules_override_identical
            OverrideStatus.EDITED -> R.string.rules_override_edited
            OverrideStatus.OFFICIAL_UPDATED -> R.string.rules_override_updated
            OverrideStatus.OFFICIAL_UPDATED_EDITED -> R.string.rules_override_updated_edited
            OverrideStatus.DIFFERS -> R.string.rules_override_differs
            OverrideStatus.TAKES_OVER -> R.string.rules_override_takes_over
        },
        official,
    )
}

/** "Official rules were updated, but N of your rules override them", with a Review action. */
@Composable
fun OverrideNotice(count: Int, onReview: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.rules_override_banner_title), style = MaterialTheme.typography.titleSmall)
            Text(pluralStringResource(R.plurals.rules_override_banner_body, count, count), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onReview) { Text(stringResource(R.string.rules_override_review)) }
        }
    }
}

/**
 * Review the user rules that overlap official rules (#58): use the official rule, keep yours, or
 * compare. Reverting removes only these overlapping rules, and asks first whether to share them so
 * they can be merged into the official rules.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleReviewScreen(
    userRules: List<UserRule>,
    overrides: Map<String, RuleOverride>,
    onRevert: (List<String>) -> Unit,
    onKeep: (RuleOverride) -> Unit,
    onCompare: (String) -> Unit,
    onShareRules: () -> Unit,
    onBack: () -> Unit,
    appName: (String) -> String = { it },
    modifier: Modifier = Modifier,
) {
    var pendingRevert by remember { mutableStateOf<List<String>?>(null) }
    pendingRevert?.let { ids ->
        AlertDialog(
            onDismissRequest = { pendingRevert = null },
            title = { Text(stringResource(R.string.rules_revert_share_title)) },
            text = { Text(stringResource(R.string.rules_revert_share_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRevert = null
                    onShareRules()
                }) { Text(stringResource(R.string.rules_revert_share_first)) }
            },
            dismissButton = {
                FlowRow {
                    TextButton(onClick = { pendingRevert = null }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        pendingRevert = null
                        onRevert(ids)
                    }) { Text(stringResource(R.string.rules_revert_without_sharing)) }
                }
            },
        )
    }

    val byId = userRules.associateBy { it.ruleId }
    val overlapping = overrides.values.filter { it.userRuleId in byId }.sortedBy { it.userRuleId }
    val updated = overlapping.filter { it.needsAttention }
    val others = overlapping.filterNot { it.needsAttention }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rules_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            item(key = "intro") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (overlapping.isEmpty()) R.string.rules_review_empty else R.string.rules_review_intro),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.rules_share_yours_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (overlapping.isNotEmpty()) {
                        OutlinedButton(onClick = { pendingRevert = overlapping.map { it.userRuleId } }) {
                            Text(pluralStringResource(R.plurals.rules_review_revert_all, overlapping.size, overlapping.size))
                        }
                    }
                }
                HorizontalDivider()
            }
            listOf(R.string.rules_review_section_updated to updated, R.string.rules_review_section_other to others)
                .filter { it.second.isNotEmpty() }
                .forEach { (title, section) ->
                    item(key = "section-$title") {
                        Text(
                            stringResource(title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        )
                    }
                    items(section, key = { it.userRuleId }) { override ->
                        val rule = byId.getValue(override.userRuleId)
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rule.ruleId, style = MaterialTheme.typography.bodyLarge)
                            Text(appName(rule.packageName), style = MaterialTheme.typography.bodySmall)
                            Text(overrideLabel(override), style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { pendingRevert = listOf(rule.ruleId) }) {
                                    Text(stringResource(R.string.rules_review_revert))
                                }
                                if (override.needsAttention) {
                                    TextButton(onClick = { onKeep(override) }) { Text(stringResource(R.string.rules_review_keep)) }
                                }
                                TextButton(onClick = { onCompare(rule.ruleId) }) { Text(stringResource(R.string.rules_review_compare)) }
                            }
                        }
                        HorizontalDivider()
                    }
                }
        }
    }
}

/** A user rule's JSON next to the official rule it overrides; lines that differ are highlighted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCompareScreen(mine: String?, official: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rules_compare_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.rules_compare_hint), style = MaterialTheme.typography.bodySmall)
            val mineLines = mine?.lines().orEmpty()
            val officialLines = official?.lines().orEmpty()
            JsonBlock(stringResource(R.string.rules_compare_yours), mineLines, officialLines.toSet())
            if (official == null) {
                Text(stringResource(R.string.rules_compare_missing), style = MaterialTheme.typography.bodyMedium)
            } else {
                JsonBlock(stringResource(R.string.rules_compare_official), officialLines, mineLines.toSet())
            }
        }
    }
}

@Composable
private fun JsonBlock(title: String, lines: List<String>, other: Set<String>) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        lines.forEach { line ->
            val differs = line !in other
            Text(
                line,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = if (differs) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                modifier = if (differs) Modifier.background(MaterialTheme.colorScheme.errorContainer) else Modifier,
            )
        }
    }
}
