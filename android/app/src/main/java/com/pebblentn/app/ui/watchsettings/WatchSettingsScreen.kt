package com.pebblentn.app.ui.watchsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsModel
import com.pebblentn.app.core.WatchSettingsSyncState

/**
 * The watchapp's own settings, shown and changed on the phone (REQ-ANDROID-021, #102). The groups
 * mirror the watch's menu, and only what the watch reported it can do is offered. Each row opens
 * [OptionListScreen]. Before a watchapp sent its settings the screen says to open it; with a
 * watchapp too old to send them it says to update it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSettingsScreen(
    state: WatchSettingsSyncState,
    watchappOpen: Boolean,
    onOpenSetting: (WatchSetting) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.watch_settings_title)) },
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val mirror = state.mirror
            when (WatchSettingsUi.status(state)) {
                WatchSettingsStatus.WAITING -> NoticeCard(stringResource(R.string.watch_settings_waiting))
                WatchSettingsStatus.OUTDATED -> NoticeCard(stringResource(R.string.watch_settings_outdated))
                WatchSettingsStatus.READY -> if (mirror != null) {
                    Text(
                        text = stringResource(R.string.watch_settings_explanation),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    if (WatchSettingsUi.showsQueuedNotice(state, watchappOpen)) {
                        NoticeCard(stringResource(R.string.watch_settings_queued))
                    }
                    for (section in WatchSettingsUi.sections(mirror)) {
                        if (section.settings.isEmpty()) continue
                        SettingsSection(stringResource(section.title)) {
                            section.settings.forEachIndexed { index, setting ->
                                if (index > 0) HorizontalDivider()
                                SettingRow(setting, mirror, onClick = { onOpenSetting(setting) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

/** A titled group, in the dashboard's style. */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
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

/** A setting and its current value; the whole row opens the option list. */
@Composable
private fun SettingRow(setting: WatchSetting, model: WatchSettingsModel, onClick: () -> Unit) {
    val title = stringResource(WatchSettingsUi.title(setting))
    ListItem(
        modifier = Modifier.clickable(
            role = Role.Button,
            onClickLabel = stringResource(R.string.cd_watch_setting_change, title),
            onClick = onClick,
        ),
        headlineContent = { Text(title) },
        supportingContent = { Text(valueLabel(setting, model[setting], model)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

/** The label of [value] of [setting], as the watch's menu spells it. */
@Composable
internal fun valueLabel(setting: WatchSetting, value: Int, model: WatchSettingsModel): String {
    val labels = stringArrayResource(WatchSettingsUi.valueLabels(setting, imperial = model.units == 1))
    return labels.getOrElse(value) { value.toString() }
}
