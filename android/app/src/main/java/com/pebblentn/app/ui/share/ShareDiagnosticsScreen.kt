package com.pebblentn.app.ui.share

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                    // The exact redacted data, so the user can confirm no personal text is present.
                    OutlinedCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
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

                    Button(
                        onClick = onShareEmail,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.share_diag_share_email))
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Int): String {
    val kb = bytes / 1024.0
    return if (kb >= 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
}
