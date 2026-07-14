package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.ui.components.AccessStatusChip
import com.pebblentn.app.ui.theme.PebbleNtnTheme
import java.text.DateFormat
import java.util.Date

/**
 * Dashboard: the master switch, notification-access status, and the last eligible notification.
 */
@Composable
fun DashboardScreen(
    accessGranted: Boolean,
    lastEligibleAtMillis: Long?,
    appEnabled: Boolean = true,
    onAppEnabledChange: (Boolean) -> Unit = {},
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

            // The master switch, first thing on the screen: when it is off nothing is read, matched,
            // stored, or sent to the watch.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_app_enabled),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            if (appEnabled) R.string.dashboard_app_enabled_on else R.string.dashboard_app_enabled_off,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = appEnabled, onCheckedChange = onAppEnabledChange)
            }
            HorizontalDivider()

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
