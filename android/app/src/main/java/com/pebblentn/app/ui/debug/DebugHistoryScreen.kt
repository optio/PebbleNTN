package com.pebblentn.app.ui.debug

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.data.DebugDisposition
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugEventType
import com.pebblentn.app.ui.components.ConfirmDialog
import com.pebblentn.app.ui.format.DisplayLabels
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/**
 * Debug history (#28): filter by status and app, grouped by day under sticky date headers, with
 * readable rows and a status badge. Sharing and deleting live in the overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebugHistoryScreen(
    events: List<DebugEvent>,
    onEventClick: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    onShare: () -> Unit = {},
    onBack: () -> Unit = {},
    appName: (String) -> String = { it },
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var packageName by rememberSaveable { mutableStateOf<String?>(null) }

    if (confirmDeleteAll) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_all_title),
            message = stringResource(R.string.confirm_delete_all_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDeleteAll = false
                onDeleteAll()
            },
            onDismiss = { confirmDeleteAll = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more_actions))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.debug_share)) },
                                onClick = {
                                    menuOpen = false
                                    onShare()
                                },
                            )
                            if (events.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.debug_delete_all)) },
                                    onClick = {
                                        menuOpen = false
                                        confirmDeleteAll = true
                                    },
                                )
                            }
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
            return@Scaffold
        }

        val filter = DebugHistoryFilter(status, packageName)
        val days = remember(events, filter) { filter.apply(events) }
        val packages = remember(events) { DebugHistoryFilter.packagesIn(events) }

        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item(key = "filters") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ChipRow {
                        StatusFilter.entries.forEach { option ->
                            FilterChip(
                                selected = status == option,
                                onClick = { status = option },
                                label = { Text(statusFilterLabel(option)) },
                            )
                        }
                    }
                    if (packages.size > 1) {
                        ChipRow {
                            FilterChip(
                                selected = packageName == null,
                                onClick = { packageName = null },
                                label = { Text(stringResource(R.string.debug_filter_all_apps)) },
                            )
                            packages.forEach { pkg ->
                                FilterChip(
                                    selected = packageName == pkg,
                                    onClick = { packageName = pkg },
                                    label = { Text(appName(pkg)) },
                                )
                            }
                        }
                    }
                }
            }
            if (days.isEmpty()) {
                item(key = "no-match") {
                    Text(
                        stringResource(R.string.debug_filter_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            days.forEach { day ->
                stickyHeader(key = "day-${day.date}") { DayHeader(day) }
                items(day.events, key = { it.id }) { event ->
                    DebugEventRow(event = event, appName = appName(event.packageName), onClick = { onEventClick(event.id) })
                    HorizontalDivider()
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
private fun statusFilterLabel(filter: StatusFilter): String = when (filter) {
    StatusFilter.ALL -> stringResource(R.string.debug_filter_all)
    StatusFilter.RECOGNISED -> stringResource(DisplayLabels.disposition(DebugDisposition.MATCHED))
    StatusFilter.NOT_RECOGNISED -> stringResource(DisplayLabels.disposition(DebugDisposition.CAPTURED_UNMATCHED))
    StatusFilter.NOT_A_DIRECTION -> stringResource(DisplayLabels.disposition(DebugDisposition.CAPTURED_NON_MANEUVER))
}

/** "Today", "Yesterday", or the full date, with how many events the day has. */
@Composable
private fun DayHeader(day: DayGroup) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val label = when (day.date) {
        today -> stringResource(R.string.debug_today)
        today.minusDays(1) -> stringResource(R.string.debug_yesterday)
        else -> day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label · " + pluralStringResource(R.plurals.debug_day_count, day.events.size, day.events.size),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DebugEventRow(event: DebugEvent, appName: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.receivedTimestampMillis)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(event)
        }
        // Its own line, so a long app name never gets squeezed between the time and the badge.
        Text(text = appName, style = MaterialTheme.typography.bodySmall)
        // What went to the watch, so a drive can be scanned without opening every event.
        val instruction = event.instruction
        if (instruction != null) {
            val distance = instruction.distanceMeters?.let { stringResource(R.string.debug_distance_meters, it) }
            val stops = instruction.stopsRemaining?.let { pluralStringResource(R.plurals.debug_stops_remaining, it, it) }
            Text(
                text = listOfNotNull(
                    stringResource(DisplayLabels.maneuver(instruction.maneuver)),
                    distance,
                    stops,
                    instruction.primaryText,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            // Nothing was sent: show what arrived, so an unrecognised card can be spotted.
            event.snapshot?.title?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, maxLines = 2) }
        }
    }
}

/** A small coloured label: Recognised, Not recognised, Not a direction, or Navigation ended. */
@Composable
private fun StatusBadge(event: DebugEvent) {
    val colors = MaterialTheme.colorScheme
    val label = if (event.eventType == DebugEventType.REMOVED) {
        stringResource(DisplayLabels.eventType(event.eventType))
    } else {
        stringResource(DisplayLabels.disposition(event.disposition))
    }
    val (container, content) = when {
        event.eventType == DebugEventType.REMOVED -> colors.surfaceVariant to colors.onSurfaceVariant
        event.disposition == DebugDisposition.MATCHED -> colors.primaryContainer to colors.onPrimaryContainer
        event.disposition == DebugDisposition.CAPTURED_UNMATCHED -> colors.errorContainer to colors.onErrorContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}
