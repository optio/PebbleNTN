package com.pebblentn.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.rules.RuleOutcome
import com.pebblentn.app.rules.RuleTraceEntry
import com.pebblentn.app.ui.components.ConfirmDialog
import com.pebblentn.app.ui.format.DisplayLabels
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DebugDetailScreen(
    event: DebugEvent?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    appName: (String) -> String = { it },
    onCreateRule: (() -> Unit)? = null,
    onShareEvent: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_event_title),
            message = stringResource(R.string.confirm_delete_event_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (event != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_event))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (event == null) {
                Text(stringResource(R.string.debug_not_found))
                return@Column
            }
            // Read top to bottom as a story (#28): what arrived, what the watch showed, and why.
            SectionTitle(stringResource(R.string.debug_notification_section))
            Field(stringResource(R.string.debug_field_app), appName(event.packageName))
            Field(
                stringResource(R.string.debug_field_posted),
                DateFormat.getDateTimeInstance().format(Date(event.eventTimestampMillis)),
            )
            event.snapshot?.let { snap ->
                Field(stringResource(R.string.debug_field_title), snap.title)
                Field(stringResource(R.string.debug_field_text), snap.text)
                Field(stringResource(R.string.debug_field_subtext), snap.subText)
                Field(stringResource(R.string.debug_field_bigtext), snap.bigText)
            }

            HorizontalDivider()
            // Every element the watch renders is shown, even when empty: an empty element is itself
            // what someone debugging a wrong arrow is looking for.
            SectionTitle(stringResource(R.string.debug_watch_elements))
            val instruction = event.instruction
            val emptyPlaceholder = stringResource(R.string.debug_element_none)
            if (instruction == null) {
                Text(stringResource(R.string.debug_watch_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                val maneuverValue =
                    if (instruction.maneuver == Maneuver.UNKNOWN) {
                        stringResource(R.string.debug_maneuver_unknown_hint)
                    } else {
                        stringResource(DisplayLabels.maneuver(instruction.maneuver))
                    }
                ElementRow(stringResource(R.string.debug_field_maneuver), maneuverValue)
                ElementRow(
                    stringResource(R.string.debug_field_distance),
                    instruction.distanceMeters
                        ?.let { stringResource(R.string.debug_distance_meters, it) }
                        ?: stringResource(R.string.debug_no_distance),
                )
                ElementRow(
                    stringResource(R.string.debug_field_primary_text),
                    instruction.primaryText ?: emptyPlaceholder,
                )
                ElementRow(
                    stringResource(R.string.debug_field_secondary_text),
                    instruction.secondaryText ?: emptyPlaceholder,
                )
                ElementRow(
                    stringResource(R.string.debug_field_eta),
                    instruction.etaEpochSeconds
                        ?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it * 1000)) }
                        ?: emptyPlaceholder,
                )
                ElementRow(
                    stringResource(R.string.debug_field_stops_remaining),
                    instruction.stopsRemaining
                        ?.let { pluralStringResource(R.plurals.debug_stops_remaining, it, it) }
                        ?: emptyPlaceholder,
                )
            }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.debug_trace_section))
            TraceSection(event)
            // Turn this notification into a rule, or share just this one event (#28).
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onCreateRule != null && event.snapshot != null) {
                    OutlinedButton(onClick = onCreateRule) { Text(stringResource(R.string.debug_create_rule)) }
                }
                if (onShareEvent != null) {
                    OutlinedButton(onClick = onShareEvent) { Text(stringResource(R.string.debug_share_event)) }
                }
            }

            HorizontalDivider()
            TechnicalDetails(event)
        }
    }
}

/** The deciding rules first; the rules that simply didn't apply stay collapsed behind a count. */
@Composable
private fun TraceSection(event: DebugEvent) {
    if (event.trace.isEmpty()) {
        Text(stringResource(R.string.debug_trace_none), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val summary = remember(event.trace) { TraceSummary.of(event.trace) }
    var showOthers by rememberSaveable(event.id) { mutableStateOf(false) }
    if (summary.decisive.isEmpty()) {
        Text(stringResource(R.string.debug_trace_no_match), style = MaterialTheme.typography.bodyMedium)
    }
    summary.decisive.forEach { TraceRow(it) }
    if (summary.others.isNotEmpty()) {
        TextButton(onClick = { showOthers = !showOthers }) {
            Text(
                if (showOthers) {
                    stringResource(R.string.debug_trace_hide_others)
                } else {
                    pluralStringResource(R.plurals.debug_trace_show_others, summary.others.size, summary.others.size)
                },
            )
        }
        if (showOthers) summary.others.forEach { TraceRow(it) }
    }
}

@Composable
private fun TraceRow(entry: RuleTraceEntry) {
    val label = stringResource(
        R.string.debug_trace_entry_label,
        stringResource(DisplayLabels.ruleLayer(entry.layer)),
        stringResource(DisplayLabels.ruleOutcome(entry.outcome)),
    )
    ElementRow(
        label = entry.ruleId,
        value = listOfNotNull(label, entry.message).joinToString(" · "),
        valueColor = if (entry.outcome == RuleOutcome.ERROR) MaterialTheme.colorScheme.error else null,
    )
}

/** Fields only a maintainer needs, collapsed by default. */
@Composable
private fun TechnicalDetails(event: DebugEvent) {
    var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(stringResource(if (expanded) R.string.debug_technical_details_hide else R.string.debug_technical_details))
    }
    if (!expanded) return
    Field(stringResource(R.string.debug_field_package), event.packageName)
    Field(stringResource(R.string.debug_field_event_type), stringResource(DisplayLabels.eventType(event.eventType)))
    Field(stringResource(R.string.debug_field_disposition), stringResource(DisplayLabels.disposition(event.disposition)))
    Field(stringResource(R.string.debug_field_matched_rule), event.matchedRuleId)
    Field(
        stringResource(R.string.debug_field_received),
        DateFormat.getDateTimeInstance().format(Date(event.receivedTimestampMillis)),
    )
    event.snapshot?.let { snap ->
        Field(stringResource(R.string.debug_field_category), snap.category)
        Field(stringResource(R.string.debug_field_channel), snap.channelId)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Field(label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Like [Field] but always rendered — used for the per-element parse breakdown and the trace, where
 *  showing an empty element is itself the information the user is looking for. */
@Composable
private fun ElementRow(label: String, value: String, valueColor: Color? = null) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = valueColor ?: Color.Unspecified)
    }
}
