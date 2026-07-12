package com.pebblentn.app.ui.rules

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import kotlinx.coroutines.launch

/**
 * Expert JSON rule editor. Validate/Format/Preview/Save operate on the same domain model; Save is
 * blocked when validation fails (RuleEngine "Expert editor": cannot save invalid JSON).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RuleEditorScreen(
    initialJson: String,
    onBack: () -> Unit,
    onValidate: (String) -> List<String>,
    onFormat: (String) -> String?,
    onSave: suspend (String) -> List<String>,
    onPreview: suspend (String) -> String?,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(initialJson) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Resolve strings in composition so click/coroutine handlers can use them.
    val validMessage = stringResource(R.string.rule_editor_valid)
    val savedMessage = stringResource(R.string.rule_editor_saved)
    val noCaptureMessage = stringResource(R.string.rule_editor_no_capture)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_editor_title)) },
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

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { errors = onValidate(text); status = if (errors.isEmpty()) validMessage else null }) {
                    Text(stringResource(R.string.rule_editor_validate))
                }
                OutlinedButton(onClick = { onFormat(text)?.let { text = it } }) {
                    Text(stringResource(R.string.rule_editor_format))
                }
                OutlinedButton(onClick = { scope.launch { status = onPreview(text) ?: noCaptureMessage } }) {
                    Text(stringResource(R.string.rule_editor_preview))
                }
                Button(onClick = {
                    scope.launch {
                        val result = onSave(text)
                        errors = result
                        if (result.isEmpty()) {
                            status = savedMessage
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
        }
    }
}
