package com.pebblentn.app.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.catalog.NavigationAppEntry
import com.pebblentn.app.data.AppEnablement

/**
 * Per-app enablement (REQ-ANDROID-009): every supported app is enabled by default on discovery
 * (REQ-ANDROID-004), and this is the only UI that can turn one off again. Below the installed
 * apps, every other supported app is listed with what PebbleNTN does with it (#28).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationAppsScreen(
    apps: List<AppEnablement>,
    onToggle: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    /** Supported apps that aren't installed (#28), and how to get one by package name. */
    notInstalled: List<NavigationAppEntry> = emptyList(),
    onGetApp: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.navigation_apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item(key = "explanation") {
                Text(
                    text = stringResource(R.string.navigation_apps_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            item(key = "installed-title") { SectionTitle(stringResource(R.string.navigation_apps_installed)) }
            if (apps.isEmpty()) {
                item(key = "installed-empty") {
                    Text(
                        text = stringResource(R.string.navigation_apps_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(apps, key = { "installed-${it.appId}" }) { app ->
                NavigationAppRow(app = app, onToggle = { enabled -> onToggle(app.appId, enabled) })
                HorizontalDivider()
            }
            if (notInstalled.isNotEmpty()) {
                item(key = "supported-title") {
                    SectionTitle(stringResource(R.string.navigation_apps_also_supported))
                    Text(
                        text = stringResource(R.string.navigation_apps_also_supported_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                items(notInstalled, key = { "supported-${it.appId}" }) { entry ->
                    SupportedAppRow(entry, onGet = { onGetApp(entry.packageNames.first()) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** A supported app that isn't installed: what PebbleNTN does with it, and a link to get it. */
@Composable
private fun SupportedAppRow(entry: NavigationAppEntry, onGet: () -> Unit) {
    val action = stringResource(R.string.navigation_apps_get, entry.displayName)
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = action, onClick = onGet),
        headlineContent = { Text(entry.displayName) },
        supportingContent = {
            Text(
                stringResource(
                    if (entry.hasOfficialRules) R.string.navigation_apps_directions else R.string.navigation_apps_capture_only_badge,
                ),
            )
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}

@Composable
private fun NavigationAppRow(app: AppEnablement, onToggle: (Boolean) -> Unit) {
    // The whole row toggles, so TalkBack reads the app name with the switch state and a tap on the
    // name works too.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = app.enabled, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.displayName, style = MaterialTheme.typography.titleMedium)
            if (app.captureOnly) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(
                            text = stringResource(R.string.navigation_apps_capture_only_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
        Switch(checked = app.enabled, onCheckedChange = null)
    }
}
