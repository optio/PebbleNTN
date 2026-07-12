package com.pebblentn.app.ui.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.export.ExportMode
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugHistoryScreen(
    events: List<DebugEvent>,
    onEventClick: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    onExport: (ExportMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = {
                showExportDialog = false
                onExport(it)
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                actions = {
                    TextButton(onClick = { showExportDialog = true }) {
                        Text(stringResource(R.string.debug_export))
                    }
                    if (events.isNotEmpty()) {
                        TextButton(onClick = onDeleteAll) {
                            Text(stringResource(R.string.debug_delete_all))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.debug_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(events, key = { it.id }) { event ->
                    DebugEventRow(event = event, onClick = { onEventClick(event.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DebugEventRow(event: DebugEvent, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = DateFormat.getDateTimeInstance().format(Date(event.receivedTimestampMillis)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = event.packageName,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "${event.eventType.name} · ${event.disposition}",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * Export mode chooser. Rules-only and privacy-safe export immediately; full requires acknowledging
 * the privacy warning (REQ-DEBUG-007) before sharing.
 */
@Composable
private fun ExportDialog(onDismiss: () -> Unit, onExport: (ExportMode) -> Unit) {
    var confirmingFull by remember { mutableStateOf(false) }

    if (confirmingFull) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.export_privacy_warning_title)) },
            text = { Text(stringResource(R.string.export_privacy_warning)) },
            confirmButton = {
                TextButton(onClick = { onExport(ExportMode.FULL) }) {
                    Text(stringResource(R.string.export_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.export_cancel)) }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_dialog_title)) },
        text = {
            Column {
                TextButton(onClick = { onExport(ExportMode.RULES_ONLY) }) {
                    Text(stringResource(R.string.export_rules_only))
                }
                TextButton(onClick = { onExport(ExportMode.PRIVACY_SAFE) }) {
                    Text(stringResource(R.string.export_privacy_safe))
                }
                TextButton(onClick = { confirmingFull = true }) {
                    Text(stringResource(R.string.export_full))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.export_cancel)) }
        },
    )
}
