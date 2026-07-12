package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.ui.components.AccessStatusChip
import com.pebblentn.app.ui.theme.PebbleNtnTheme
import java.text.DateFormat
import java.util.Date

/**
 * Minimal M2 dashboard: notification-access status and the last eligible notification time. More
 * (watch connection, current navigation state, active ruleset, shortcuts) is added in later
 * milestones.
 */
@Composable
fun DashboardScreen(
    accessGranted: Boolean,
    lastEligibleAtMillis: Long?,
    onOpenDebugHistory: () -> Unit = {},
    onOpenRules: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            AccessStatusChip(accessGranted = accessGranted)
            Text(
                text = if (lastEligibleAtMillis == null) {
                    stringResource(R.string.dashboard_last_eligible_none)
                } else {
                    stringResource(
                        R.string.dashboard_last_eligible,
                        DateFormat.getDateTimeInstance().format(Date(lastEligibleAtMillis)),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenDebugHistory) {
                Text(stringResource(R.string.dashboard_open_debug))
            }
            OutlinedButton(onClick = onOpenRules) {
                Text(stringResource(R.string.dashboard_open_rules))
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    PebbleNtnTheme {
        DashboardScreen(accessGranted = true, lastEligibleAtMillis = null)
    }
}
