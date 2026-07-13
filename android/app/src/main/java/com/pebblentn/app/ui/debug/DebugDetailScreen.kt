package com.pebblentn.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.data.DebugEvent
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDetailScreen(
    event: DebugEvent?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        IconButton(onClick = onDelete) {
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
            // What the watch actually showed for this notification — the first thing a rule author
            // or a user debugging a wrong arrow wants to see.
            Text(
                text = stringResource(R.string.debug_watch_output),
                style = MaterialTheme.typography.titleMedium,
            )
            val instruction = event.instruction
            if (instruction == null) {
                Text(stringResource(R.string.debug_watch_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                Field(stringResource(R.string.debug_field_maneuver), instruction.maneuver.name)
                Field(
                    stringResource(R.string.debug_field_distance),
                    instruction.distanceMeters
                        ?.let { stringResource(R.string.debug_distance_meters, it) }
                        ?: stringResource(R.string.debug_no_distance),
                )
                Field(stringResource(R.string.debug_field_primary_text), instruction.primaryText)
                Field(stringResource(R.string.debug_field_secondary_text), instruction.secondaryText)
            }
            Field(stringResource(R.string.debug_field_matched_rule), event.matchedRuleId)

            HorizontalDivider()
            Text(
                text = stringResource(R.string.debug_notification_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Field(stringResource(R.string.debug_field_package), event.packageName)
            Field(stringResource(R.string.debug_field_event_type), event.eventType.name)
            Field(stringResource(R.string.debug_field_disposition), event.disposition)
            Field(
                stringResource(R.string.debug_field_received),
                DateFormat.getDateTimeInstance().format(Date(event.receivedTimestampMillis)),
            )
            Field(
                stringResource(R.string.debug_field_posted),
                DateFormat.getDateTimeInstance().format(Date(event.eventTimestampMillis)),
            )
            event.snapshot?.let { snap ->
                Field(stringResource(R.string.debug_field_title), snap.title)
                Field(stringResource(R.string.debug_field_text), snap.text)
                Field(stringResource(R.string.debug_field_subtext), snap.subText)
                Field(stringResource(R.string.debug_field_bigtext), snap.bigText)
                Field(stringResource(R.string.debug_field_category), snap.category)
                Field(stringResource(R.string.debug_field_channel), snap.channelId)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
