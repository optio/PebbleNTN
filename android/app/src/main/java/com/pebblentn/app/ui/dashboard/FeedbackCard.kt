package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R

/**
 * The temporary feedback campaign card (REQ-ANDROID-015, #29). Every action opens the browser or
 * the email app; nothing is sent automatically.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackCard(
    onReportBug: () -> Unit,
    onRequestFeature: () -> Unit,
    onSuggestName: () -> Unit,
    onEmail: () -> Unit,
    onNotNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feedback_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.feedback_body), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onReportBug) { Text(stringResource(R.string.feedback_bug)) }
                OutlinedButton(onClick = onRequestFeature) { Text(stringResource(R.string.feedback_feature)) }
                OutlinedButton(onClick = onSuggestName) { Text(stringResource(R.string.feedback_name)) }
                OutlinedButton(onClick = onEmail) { Text(stringResource(R.string.feedback_email)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onNotNow) { Text(stringResource(R.string.feedback_not_now)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.feedback_dismiss)) }
            }
        }
    }
}
