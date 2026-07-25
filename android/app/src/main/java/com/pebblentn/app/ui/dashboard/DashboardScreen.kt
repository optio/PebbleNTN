package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.pebblentn.app.BuildConfig
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
    onRefreshApp: () -> Unit = {},
    appVersion: String = BuildConfig.VERSION_NAME,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshDoneMessage = stringResource(R.string.dashboard_refresh_app_done)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
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

            // Recovery action for the case where Android has silently stopped delivering
            // notifications to our listener: reconnect it without a full app restart.
            HorizontalDivider()
            Text(
                text = stringResource(R.string.dashboard_refresh_app_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    onRefreshApp()
                    scope.launch { snackbarHostState.showSnackbar(refreshDoneMessage) }
                },
            ) {
                Text(stringResource(R.string.dashboard_refresh_app))
            }

            // Push the version to the bottom of the screen.
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.dashboard_version, appVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
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
