package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R

/** How far setup has got (#28). The checklist disappears once [complete]. */
data class SetupProgress(
    val accessGranted: Boolean,
    val watchappInstalled: Boolean,
    val navigationSeen: Boolean,
) {
    val complete: Boolean get() = accessGranted && watchappInstalled && navigationSeen
}

/**
 * "Get started" card on the dashboard: notification access, the watchapp, a first navigation.
 * The watchapp step ticks itself when the watch answers (WatchappPresenceRepository); the user
 * can also confirm it by hand, since PebbleKit can't see an app that was never opened.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupChecklist(
    progress: SetupProgress,
    onGetWatchapp: () -> Unit,
    onConfirmWatchapp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
            Step(number = 1, done = progress.accessGranted, title = stringResource(R.string.setup_step_access))
            Step(
                number = 2,
                done = progress.watchappInstalled,
                title = stringResource(
                    if (progress.watchappInstalled) R.string.setup_step_watchapp_done else R.string.setup_step_watchapp,
                ),
                hint = stringResource(R.string.setup_step_watchapp_hint).takeUnless { progress.watchappInstalled },
            ) {
                if (!progress.watchappInstalled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onGetWatchapp) { Text(stringResource(R.string.setup_get_watchapp)) }
                        OutlinedButton(onClick = onConfirmWatchapp) { Text(stringResource(R.string.setup_watchapp_installed)) }
                    }
                }
            }
            Step(
                number = 3,
                done = progress.navigationSeen,
                title = stringResource(
                    if (progress.navigationSeen) R.string.setup_step_navigation_done else R.string.setup_step_navigation,
                ),
                hint = stringResource(R.string.setup_step_navigation_hint).takeUnless { progress.navigationSeen },
            )
        }
    }
}

@Composable
private fun Step(
    number: Int,
    done: Boolean,
    title: String,
    hint: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        val state = if (done) stringResource(R.string.setup_step_done) else stringResource(R.string.setup_step_todo, number)
        Box(modifier = Modifier.size(28.dp).clearAndSetSemantics { contentDescription = state }, contentAlignment = Alignment.Center) {
            if (done) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiaryContainer)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            actions()
        }
    }
}
