package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pebblentn.app.BuildConfig
import com.pebblentn.app.R
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.protocol.WatchLink
import com.pebblentn.app.ui.components.AccessStatusChip
import com.pebblentn.app.ui.format.DisplayLabels
import com.pebblentn.app.ui.theme.PebbleNtnTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Dashboard, grouped into sections (#28): Status, Watch, Navigation, Troubleshooting, Updates. The
 * update prompt and the share nudge appear above the sections only when they apply.
 */
@Composable
fun DashboardScreen(
    accessGranted: Boolean,
    lastEligibleAtMillis: Long?,
    appEnabled: Boolean = true,
    onAppEnabledChange: (Boolean) -> Unit = {},
    onOpenDebugHistory: () -> Unit = {},
    onOpenRules: () -> Unit = {},
    onOpenNavigationApps: () -> Unit = {},
    onRefreshApp: () -> Unit = {},
    unmatchedCaptureCount: Int = 0,
    onShareDiagnostics: () -> Unit = {},
    updateAvailable: Boolean = false,
    latestVersion: String? = null,
    onDownloadUpdate: () -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    autoCheckUpdates: Boolean = false,
    onAutoCheckUpdatesChange: (Boolean) -> Unit = {},
    autoLaunch: Boolean = true,
    onAutoLaunchChange: (Boolean) -> Unit = {},
    lastSentToWatch: NavigationInstruction? = null,
    enabledAppCount: Int = 0,
    installedAppCount: Int = 0,
    officialRuleCount: Int = 0,
    userRuleCount: Int = 0,
    watchappInstalled: Boolean = true,
    watchLink: WatchLink = WatchLink.Unknown,
    monochrome: Boolean = false,
    onMonochromeChange: (Boolean) -> Unit = {},
    onGetWatchapp: () -> Unit = {},
    onConfirmWatchapp: () -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Until setup is done: access, the watchapp, a first navigation (#28).
            val setup = SetupProgress(
                accessGranted = accessGranted,
                watchappInstalled = watchappInstalled,
                navigationSeen = lastEligibleAtMillis != null,
            )
            if (!setup.complete) {
                SetupChecklist(progress = setup, onGetWatchapp = onGetWatchapp, onConfirmWatchapp = onConfirmWatchapp)
            }

            if (updateAvailable) {
                PromptCard(
                    title = stringResource(R.string.dashboard_update_title),
                    body = stringResource(R.string.dashboard_update_body, latestVersion ?: "", appVersion),
                    action = stringResource(R.string.dashboard_update_download),
                    onAction = onDownloadUpdate,
                    primary = true,
                )
            }
            // Captures no rule could turn into directions: invite the user to share them (REQ-DEBUG-011).
            if (unmatchedCaptureCount > 0) {
                PromptCard(
                    title = stringResource(R.string.dashboard_share_nudge_title),
                    body = stringResource(R.string.dashboard_share_nudge_body, unmatchedCaptureCount),
                    action = stringResource(R.string.dashboard_share_nudge_action),
                    onAction = onShareDiagnostics,
                    primary = false,
                )
            }

            Section(stringResource(R.string.dashboard_section_status)) {
                // The master switch comes first: when it is off nothing is read, matched, stored, or sent.
                ToggleRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.dashboard_app_enabled),
                    summary = stringResource(
                        if (appEnabled) R.string.dashboard_app_enabled_on else R.string.dashboard_app_enabled_off,
                    ),
                    checked = appEnabled,
                    onCheckedChange = onAppEnabledChange,
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { AccessStatusChip(accessGranted = accessGranted) },
                    colors = sectionColors(),
                )
                InfoRow(
                    title = stringResource(R.string.dashboard_watch),
                    value = when (watchLink) {
                        WatchLink.Unknown -> stringResource(R.string.dashboard_watch_checking)
                        WatchLink.NoCompanion -> stringResource(R.string.dashboard_watch_no_companion)
                        WatchLink.NotConnected -> stringResource(R.string.dashboard_watch_not_connected)
                        is WatchLink.Connected -> stringResource(R.string.dashboard_watch_connected, watchLink.watchNames.joinToString())
                    },
                )
                InfoRow(
                    title = stringResource(R.string.dashboard_last_notification),
                    value = lastEligibleAtMillis
                        ?.let { stringResource(R.string.dashboard_last_eligible, DateFormat.getDateTimeInstance().format(Date(it))) }
                        ?: stringResource(R.string.dashboard_last_eligible_none),
                )
                InfoRow(
                    title = stringResource(R.string.dashboard_last_sent),
                    value = lastSentToWatch?.let { lastSentSummary(it) }
                        ?: stringResource(R.string.dashboard_last_sent_none),
                    dimmed = !appEnabled,
                )
            }

            Section(stringResource(R.string.dashboard_section_watch)) {
                ToggleRow(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.dashboard_auto_launch),
                    summary = stringResource(
                        if (appEnabled) R.string.dashboard_auto_launch_hint else R.string.dashboard_watch_paused,
                    ),
                    checked = autoLaunch,
                    onCheckedChange = onAutoLaunchChange,
                    enabled = appEnabled,
                )
            }

            Section(stringResource(R.string.dashboard_section_navigation)) {
                NavRow(
                    icon = Icons.Filled.Place,
                    title = stringResource(R.string.dashboard_open_navigation_apps),
                    summary = if (installedAppCount == 0) {
                        stringResource(R.string.dashboard_navigation_apps_none)
                    } else {
                        pluralStringResource(
                            R.plurals.dashboard_navigation_apps_summary,
                            installedAppCount,
                            enabledAppCount,
                            installedAppCount,
                        )
                    },
                    onClick = onOpenNavigationApps,
                )
                HorizontalDivider()
                NavRow(
                    icon = Icons.Filled.Edit,
                    title = stringResource(R.string.dashboard_open_rules),
                    summary = stringResource(R.string.dashboard_rules_summary, officialRuleCount, userRuleCount),
                    onClick = onOpenRules,
                )
            }

            Section(stringResource(R.string.dashboard_section_troubleshooting)) {
                NavRow(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = stringResource(R.string.dashboard_open_debug),
                    summary = stringResource(R.string.dashboard_debug_summary),
                    onClick = onOpenDebugHistory,
                )
                HorizontalDivider()
                NavRow(
                    icon = Icons.Filled.Share,
                    title = stringResource(R.string.dashboard_share_nudge_title),
                    summary = stringResource(R.string.dashboard_share_summary),
                    onClick = onShareDiagnostics,
                )
                HorizontalDivider()
                // Recovery for when Android silently stops delivering notifications to our listener.
                NavRow(
                    icon = Icons.Filled.Build,
                    title = stringResource(R.string.dashboard_refresh_app),
                    summary = stringResource(R.string.dashboard_refresh_app_explanation),
                    onClick = {
                        onRefreshApp()
                        scope.launch { snackbarHostState.showSnackbar(refreshDoneMessage) }
                    },
                    showChevron = false,
                )
            }

            Section(stringResource(R.string.dashboard_section_appearance)) {
                ToggleRow(
                    icon = Icons.Filled.Face,
                    title = stringResource(R.string.dashboard_monochrome),
                    summary = stringResource(R.string.dashboard_monochrome_hint),
                    checked = monochrome,
                    onCheckedChange = onMonochromeChange,
                )
            }

            Section(stringResource(R.string.dashboard_section_updates)) {
                NavRow(
                    icon = Icons.Filled.Refresh,
                    title = stringResource(R.string.dashboard_check_update),
                    summary = stringResource(R.string.dashboard_version, appVersion),
                    onClick = onCheckForUpdate,
                    showChevron = false,
                )
                HorizontalDivider()
                ToggleRow(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.dashboard_auto_update),
                    summary = stringResource(R.string.dashboard_auto_update_hint),
                    checked = autoCheckUpdates,
                    onCheckedChange = onAutoCheckUpdatesChange,
                )
            }

        }
    }
}

/** "Slight left · 70 m · Rue de la Loi · ETA 17:47", with readable maneuver names. */
@Composable
private fun lastSentSummary(instruction: NavigationInstruction): String = listOfNotNull(
    stringResource(DisplayLabels.maneuver(instruction.maneuver)),
    instruction.distanceMeters?.let { stringResource(R.string.debug_distance_meters, it) },
    instruction.primaryText,
    instruction.secondaryText?.let { stringResource(R.string.dashboard_last_sent_eta, it) },
).joinToString(" · ")

@Composable
private fun sectionColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

/** A setting row; the whole row toggles, not only the switch (bigger touch target, one TalkBack node). */
@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = Modifier
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else DIMMED_ALPHA),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = sectionColors(),
    )
}

/** A row that opens a screen or runs an action. */
@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = if (showChevron) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        },
        colors = sectionColors(),
    )
}

/** A read-only value, such as the last notification time. */
@Composable
private fun InfoRow(title: String, value: String, dimmed: Boolean = false) {
    ListItem(
        modifier = Modifier.alpha(if (dimmed) DIMMED_ALPHA else 1f),
        overlineContent = { Text(title) },
        headlineContent = { Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        colors = sectionColors(),
    )
}

/** An attention card above the sections (update available, captures to share). */
@Composable
private fun PromptCard(title: String, body: String, action: String, onAction: () -> Unit, primary: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (primary) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = body, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onAction) { Text(action) }
        }
    }
}

private const val DIMMED_ALPHA = 0.5f

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, heightDp = 1400)
@Composable
private fun DashboardPreview() {
    PebbleNtnTheme {
        DashboardScreen(
            accessGranted = true,
            lastEligibleAtMillis = null,
            installedAppCount = 2,
            enabledAppCount = 1,
            officialRuleCount = 31,
        )
    }
}
