package com.pebblentn.app.ui.share

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.export.ExportMode

/**
 * Guided "share logs to help add app support" screen (REQ-DEBUG-011). Explains that no personal data
 * is shared, shows the exact redacted dataset for review, and opens the user's email app with the
 * attachment. The payload is already capped to the 10 MB email budget upstream.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDiagnosticsScreen(
    state: ShareDiagnosticsState,
    onBack: () -> Unit,
    onShareEmail: () -> Unit,
    onModeChange: (ExportMode) -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_diag_title)) },
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                // First load, before we know whether there is anything to share.
                state.loading && !state.hasContent -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator() }
                }

                !state.hasContent -> {
                    Text(
                        text = stringResource(R.string.share_diag_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.share_diag_explainer),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    // Let the user choose the redacted dataset or the fuller one that keeps street
                    // names (more valuable for adding direction/turn-word translations).
                    Text(
                        text = stringResource(R.string.share_diag_mode_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    ModeOption(
                        selected = state.mode == ExportMode.PRIVACY_SAFE,
                        title = stringResource(R.string.share_diag_mode_redacted),
                        hint = stringResource(R.string.share_diag_mode_redacted_hint),
                        onSelect = { onModeChange(ExportMode.PRIVACY_SAFE) },
                    )
                    ModeOption(
                        selected = state.mode == ExportMode.FULL,
                        title = stringResource(R.string.share_diag_mode_full),
                        hint = stringResource(R.string.share_diag_mode_full_hint),
                        onSelect = { onModeChange(ExportMode.FULL) },
                    )

                    Text(
                        text = stringResource(
                            R.string.share_diag_summary,
                            state.includedEvents,
                            formatSize(state.sizeBytes),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (state.truncatedToFit) {
                        Text(
                            text = stringResource(
                                R.string.share_diag_truncated,
                                state.includedEvents,
                                state.totalEvents,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = stringResource(R.string.share_diag_preview_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    // The exact data to be shared, so the user can review it before sending.
                    OutlinedCard(modifier = Modifier.height(220.dp).fillMaxWidth()) {
                        if (state.loading) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) { CircularProgressIndicator() }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = state.previewText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                )
                                if (state.previewTrimmed) {
                                    Text(
                                        text = stringResource(R.string.share_diag_preview_trimmed, state.includedEvents),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onShareEmail,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.share_diag_share_email))
                    }
                }
            }
        }
    }
}

/** One selectable share-mode row: a radio button, its title, and an explanatory hint. */
@Composable
private fun ModeOption(selected: Boolean, title: String, hint: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatSize(bytes: Int): String {
    val kb = bytes / 1024.0
    return if (kb >= 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
}
