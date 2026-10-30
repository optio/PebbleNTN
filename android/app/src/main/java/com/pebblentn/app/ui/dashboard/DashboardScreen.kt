package com.pebblentn.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.pebblentn.app.update.InstallRecord
import com.pebblentn.app.update.InstallSource
import com.pebblentn.app.update.UpdatePolicy
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Dashboard, grouped into sections (#28): Status, Watch, Navigation, Troubleshooting, Updates. The
 * update prompt and the share nudge appear above the sections only when they apply; a stale update
 * check, or the weekly-check question after it, comes first of all (#94).
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
    /** Where this app was installed from: the card's main "Update from …" button (REQ-ANDROID-020). */
    installSource: InstallSource = InstallSource.GITHUB,
    /** What installed or last updated this version: the "Phone app version" row (REQ-ANDROID-020, #119). */
    installRecord: InstallRecord = InstallRecord.UNKNOWN,
    /** Open a source's update destination: the detected one, or one picked once under "Other sources". */
    onOpenUpdateSource: (InstallSource) -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    autoCheckUpdates: Boolean = false,
    onAutoCheckUpdatesChange: (Boolean) -> Unit = {},
    autoLaunch: Boolean = true,
    onAutoLaunchChange: (Boolean) -> Unit = {},
    /** Open the Watch settings screen (REQ-ANDROID-021, #102). */
    onOpenWatchSettings: () -> Unit = {},
    lastSentToWatch: NavigationInstruction? = null,
    enabledAppCount: Int = 0,
    installedAppCount: Int = 0,
    officialRuleCount: Int = 0,
    userRuleCount: Int = 0,
    watchappInstalled: Boolean = true,
    watchLink: WatchLink = WatchLink.Unknown,
    monochrome: Boolean = false,
    onMonochromeChange: (Boolean) -> Unit = {},
    showFeedback: Boolean = false,
    overriddenOfficialCount: Int = 0,
    onReviewOverrides: () -> Unit = {},
    feedbackActions: FeedbackActions = FeedbackActions(),
    onGetWatchapp: () -> Unit = {},
    onConfirmWatchapp: () -> Unit = {},
    appVersion: String = BuildConfig.VERSION_NAME,
    /** The stale-check item or the weekly-check question, first on the dashboard (REQ-ANDROID-018, #94). */
    updatePrompt: UpdatePrompt = UpdatePrompt.NONE,
    /** No update check has ever run, so the stale item says "never" rather than "over 30 days". */
    updateNeverChecked: Boolean = false,
    onStaleCheck: () -> Unit = {},
    onEnableWeeklyChecks: () -> Unit = {},
    onDismissWeeklyQuestion: () -> Unit = {},
    /** The last watchapp version a WATCH_READY carried, or null when none arrived (REQ-ANDROID-019). */
    watchAppVersion: String? = null,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshDoneMessage = stringResource(R.string.dashboard_refresh_app_done)
    // A Google Play install has no GitHub check: no stale item, no weekly question, no check row and
    // no weekly toggle, whatever the caller passes (REQ-ANDROID-022).
    val githubCheckAllowed = UpdatePolicy.forSource(installSource).allowsGithubCheck
    val prompt = if (githubCheckAllowed) updatePrompt else UpdatePrompt.NONE
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

            // A stale update check comes first, above the setup checklist, and after a check run from
            // it, the weekly-check question (REQ-ANDROID-018, #94).
            when (prompt) {
                UpdatePrompt.STALE_CHECK -> PromptCard(
                    title = stringResource(R.string.dashboard_stale_check_title),
                    body = stringResource(
                        if (updateNeverChecked) R.string.dashboard_stale_check_never_body else R.string.dashboard_stale_check_body,
                    ),
                    action = stringResource(R.string.dashboard_stale_check_action),
                    onAction = onStaleCheck,
                    primary = true,
                )
                UpdatePrompt.WEEKLY_QUESTION -> PromptCard(
                    title = stringResource(R.string.dashboard_weekly_question_title),
                    body = stringResource(R.string.dashboard_weekly_question_body),
                    action = stringResource(R.string.dashboard_weekly_question_turn_on),
                    onAction = onEnableWeeklyChecks,
                    primary = false,
                    secondaryAction = stringResource(R.string.dashboard_weekly_question_not_now),
                    onSecondaryAction = onDismissWeeklyQuestion,
                )
                UpdatePrompt.NONE -> Unit
            }

            // Until setup is done: access, the watchapp, a first navigation (#28).
            val setup = SetupProgress(
                accessGranted = accessGranted,
                watchappInstalled = watchappInstalled,
                navigationSeen = lastEligibleAtMillis != null,
            )
            if (!setup.complete) {
                SetupChecklist(progress = setup, onGetWatchapp = onGetWatchapp, onConfirmWatchapp = onConfirmWatchapp)
            }

            // Temporary feedback campaign (#29): after first use, until its end date or dismissal.
            if (showFeedback) {
                FeedbackCard(
                    onReportBug = feedbackActions.reportBug,
                    onRequestFeature = feedbackActions.requestFeature,
                    onSuggestName = feedbackActions.suggestName,
                    onEmail = feedbackActions.email,
                    onNotNow = feedbackActions.notNow,
                    onDismiss = feedbackActions.dismiss,
                )
            }

            if (updateAvailable) {
                UpdateAvailableCard(
                    model = updateCardModel(installSource),
                    latestVersion = latestVersion ?: "",
                    appVersion = appVersion,
                    onOpenSource = onOpenUpdateSource,
                )
            }
            // Official rules were updated under rules of yours that override them (#58).
            if (overriddenOfficialCount > 0) {
                PromptCard(
                    title = stringResource(R.string.dashboard_rules_updated_title),
                    body = pluralStringResource(R.plurals.dashboard_rules_updated_body, overriddenOfficialCount, overriddenOfficialCount),
                    action = stringResource(R.string.dashboard_rules_updated_action),
                    onAction = onReviewOverrides,
                    primary = false,
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
                // Shows whether the companion app delivers watch-to-phone messages (REQ-ANDROID-019).
                val seenFormat = stringResource(R.string.dashboard_watchapp_version_seen)
                InfoRow(
                    title = stringResource(R.string.dashboard_watchapp_version),
                    value = watchAppVersionText(
                        version = watchAppVersion,
                        lastSeen = { seenFormat.format(it) },
                        none = stringResource(R.string.dashboard_watchapp_version_none),
                    ),
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
                // The watchapp's own settings, changed from the phone (REQ-ANDROID-021, #102).
                NavRow(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.dashboard_watch_settings),
                    summary = stringResource(R.string.dashboard_watch_settings_summary),
                    onClick = onOpenWatchSettings,
                )
                HorizontalDivider()
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
                // The version and what installed or last updated it, for every source, even while the
                // stale check is the top card (REQ-ANDROID-020, #119).
                InfoRow(
                    title = stringResource(R.string.dashboard_app_version),
                    value = resolve(phoneAppVersionText(appVersion, installRecord)),
                )
                // Google Play updates the app; there is no GitHub check (REQ-ANDROID-022).
                if (githubCheckAllowed) {
                    HorizontalDivider()
                    // While the check is stale, "Check for updates" is the top card instead (#94).
                    if (prompt != UpdatePrompt.STALE_CHECK) {
                        NavRow(
                            icon = Icons.Filled.Refresh,
                            title = stringResource(R.string.dashboard_check_update),
                            summary = stringResource(R.string.dashboard_check_update_hint),
                            onClick = onCheckForUpdate,
                            showChevron = false,
                        )
                        HorizontalDivider()
                    }
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
}

/** The row text: the format with the version and the source's name or package, or the bare version. */
@Composable
private fun resolve(text: PhoneAppVersionText): String {
    val format = text.format ?: return text.version
    val source = text.sourceName?.let { stringResource(it) } ?: text.sourcePackage
    return if (source == null) stringResource(format, text.version) else stringResource(format, text.version, source)
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

/**
 * A read-only value, such as the last notification time. A compact column rather than a `ListItem`,
 * whose minimum height and vertical padding left big gaps between the Status rows; it keeps the
 * `ListItem` overline and headline colours.
 */
@Composable
private fun InfoRow(title: String, value: String, dimmed: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) DIMMED_ALPHA else 1f)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The name a source has on the card and in the "Other sources" list (REQ-ANDROID-020). */
@Composable
private fun sourceName(source: InstallSource): String = stringResource(
    when (source) {
        InstallSource.GITHUB -> R.string.dashboard_update_source_github
        InstallSource.FDROID -> R.string.dashboard_update_source_fdroid
        InstallSource.OBTAINIUM -> R.string.dashboard_update_source_obtainium
        InstallSource.PLAY -> R.string.dashboard_update_source_play
    },
)

/**
 * The update-available card (REQ-ANDROID-013, amended by REQ-ANDROID-020 and REQ-ANDROID-022):
 * "Update from {source}" for the install source, the F-Droid or Google Play delay line, and "Other
 * sources", a short list of the other two (not on Google Play, whose card has only its own button). A
 * pick is used once; nothing is stored, so the detected source stays the default.
 */
@Composable
private fun UpdateAvailableCard(
    model: UpdateCardModel,
    latestVersion: String,
    appVersion: String,
    onOpenSource: (InstallSource) -> Unit,
) {
    var pickingSource by remember { mutableStateOf(false) }
    val body = stringResource(
        if (model.showUninstallHint) R.string.dashboard_update_body else R.string.dashboard_update_body_play,
        latestVersion,
        appVersion,
    )
    val delay = when {
        model.showFdroidDelay -> stringResource(R.string.dashboard_update_fdroid_delay)
        model.showPlayDelay -> stringResource(R.string.dashboard_update_play_delay)
        else -> null
    }
    PromptCard(
        title = stringResource(R.string.dashboard_update_title),
        body = if (delay != null) body + "\n\n" + delay else body,
        action = stringResource(R.string.dashboard_update_from, sourceName(model.main)),
        onAction = { onOpenSource(model.main) },
        primary = true,
        secondaryAction = if (model.offersOtherSources) stringResource(R.string.dashboard_update_other_sources) else null,
        onSecondaryAction = { pickingSource = true },
    )
    if (pickingSource) {
        AlertDialog(
            onDismissRequest = { pickingSource = false },
            title = { Text(stringResource(R.string.dashboard_update_pick_source)) },
            text = {
                Column {
                    for (source in model.others) {
                        TextButton(
                            onClick = {
                                pickingSource = false
                                onOpenSource(source)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(sourceName(source)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingSource = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/**
 * An attention card above the sections (update available, captures to share). [secondaryAction],
 * when given, adds a text button beside the main one (the weekly-check question's "Not now", #94),
 * wrapping below it when both do not fit on one line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    primary: Boolean,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
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
            if (secondaryAction == null) {
                Button(onClick = onAction) { Text(action) }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAction) { Text(action) }
                    TextButton(onClick = onSecondaryAction) { Text(secondaryAction) }
                }
            }
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

/** What the feedback card's buttons do (#29). */
data class FeedbackActions(
    val reportBug: () -> Unit = {},
    val requestFeature: () -> Unit = {},
    val suggestName: () -> Unit = {},
    val email: () -> Unit = {},
    val notNow: () -> Unit = {},
    val dismiss: () -> Unit = {},
)
