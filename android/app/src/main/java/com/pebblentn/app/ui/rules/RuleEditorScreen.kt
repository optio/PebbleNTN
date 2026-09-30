package com.pebblentn.app.ui.rules

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.rules.PreviewResult
import com.pebblentn.app.ui.components.ConfirmDialog
import com.pebblentn.app.ui.format.DisplayLabels
import kotlinx.coroutines.launch

/** A captured notification the editor can preview against: its id and a one-line label. */
data class CaptureChoice(val eventId: Long, val label: String)

/**
 * Expert JSON rule editor. Validate/Format/Preview/Save operate on the same domain model; Save is
 * blocked when validation fails (RuleEngine "Expert editor": cannot save invalid JSON). Preview runs
 * against a capture the author picks and shows every watch element (#28); leaving with unsaved
 * changes asks first.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RuleEditorScreen(
    initialJson: String,
    onBack: () -> Unit,
    onValidate: (String) -> List<String>,
    onFormat: (String) -> String?,
    onSave: suspend (String) -> List<String>,
    onPreview: suspend (json: String, eventId: Long?) -> PreviewResult?,
    captures: List<CaptureChoice> = emptyList(),
    initialCaptureId: Long? = null,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(initialJson) }
    var savedText by remember { mutableStateOf(initialJson) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var status by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<PreviewResult?>(null) }
    var previewRan by remember { mutableStateOf(false) }
    var captureId by remember { mutableStateOf(initialCaptureId ?: captures.firstOrNull()?.eventId) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val validMessage = stringResource(R.string.rule_editor_valid)
    val dirty = text != savedText
    val leave = { if (dirty) confirmDiscard = true else onBack() }

    BackHandler(enabled = dirty) { confirmDiscard = true }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.rule_editor_discard_title),
            message = stringResource(R.string.rule_editor_discard_message),
            confirmLabel = stringResource(R.string.rule_editor_discard),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = leave) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; status = null },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                label = { Text("JSON") },
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { errors = onValidate(text); status = if (errors.isEmpty()) validMessage else null }) {
                    Text(stringResource(R.string.rule_editor_validate))
                }
                OutlinedButton(onClick = { onFormat(text)?.let { text = it } }) {
                    Text(stringResource(R.string.rule_editor_format))
                }
                Button(onClick = {
                    scope.launch {
                        val result = onSave(text)
                        errors = result
                        if (result.isEmpty()) {
                            savedText = text
                            onBack()
                        }
                    }
                }) {
                    Text(stringResource(R.string.rule_editor_save))
                }
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            errors.forEach { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Text(stringResource(R.string.rule_editor_preview_title), style = MaterialTheme.typography.titleMedium)
            if (captures.isEmpty()) {
                Text(stringResource(R.string.rule_editor_no_capture), style = MaterialTheme.typography.bodyMedium)
            } else {
                CapturePicker(captures, captureId, onPick = { captureId = it; preview = null; previewRan = false })
                OutlinedButton(onClick = {
                    scope.launch {
                        preview = onPreview(text, captureId)
                        previewRan = true
                    }
                }) { Text(stringResource(R.string.rule_editor_preview)) }
                if (previewRan) PreviewCard(preview)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapturePicker(captures: List<CaptureChoice>, selectedId: Long?, onPick: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = captures.firstOrNull { it.eventId == selectedId } ?: captures.first()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.rule_editor_preview_against)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            captures.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        onPick(choice.eventId)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** What the rule would put on the watch for the chosen capture, element by element. */
@Composable
private fun PreviewCard(result: PreviewResult?) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (result) {
                null -> Text(stringResource(R.string.rule_editor_no_capture))
                is PreviewResult.InvalidRule -> result.errors.forEach {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                is PreviewResult.Evaluated -> {
                    val instruction = result.evaluation.instruction
                    if (instruction == null) {
                        Text(stringResource(R.string.rule_editor_preview_no_match), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(stringResource(R.string.rule_editor_preview_match), style = MaterialTheme.typography.titleSmall)
                        val none = stringResource(R.string.debug_element_none)
                        PreviewRow(stringResource(R.string.debug_field_maneuver), stringResource(DisplayLabels.maneuver(instruction.maneuver)))
                        PreviewRow(
                            stringResource(R.string.debug_field_distance),
                            instruction.distanceMeters?.let { stringResource(R.string.debug_distance_meters, it) } ?: none,
                        )
                        PreviewRow(stringResource(R.string.debug_field_primary_text), instruction.primaryText ?: none)
                        PreviewRow(stringResource(R.string.debug_field_secondary_text), instruction.secondaryText ?: none)
                        instruction.stopsRemaining?.let {
                            PreviewRow(stringResource(R.string.debug_field_stops_remaining), pluralStringResource(R.plurals.debug_stops_remaining, it, it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
