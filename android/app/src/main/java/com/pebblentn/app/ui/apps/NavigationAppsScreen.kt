package com.pebblentn.app.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.catalog.NavigationAppEntry
import com.pebblentn.app.data.AppEnablement
import com.pebblentn.app.ui.rules.languageLabel
import androidx.core.graphics.drawable.toBitmap

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
    /** Open an app's official rules, pre-filtered (#28); only offered for apps with rules. */
    onViewRules: (String) -> Unit = {},
    /** Language codes the official rules cover, per app id (#28). */
    languagesByApp: Map<String, List<String>> = emptyMap(),
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
                NavigationAppRow(
                    app = app,
                    onToggle = { enabled -> onToggle(app.appId, enabled) },
                    onViewRules = { onViewRules(app.appId) }.takeUnless { app.captureOnly },
                    languages = languagesByApp[app.appId].orEmpty(),
                )
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
                    SupportedAppRow(entry, languagesByApp[entry.appId].orEmpty(), onGet = { onGetApp(entry.packageNames.first()) })
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
private fun SupportedAppRow(entry: NavigationAppEntry, languages: List<String>, onGet: () -> Unit) {
    val action = stringResource(R.string.navigation_apps_get, entry.displayName)
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = action, onClick = onGet),
        headlineContent = { Text(entry.displayName) },
        supportingContent = {
            Text(
                if (entry.hasOfficialRules && languages.isNotEmpty()) {
                    stringResource(R.string.navigation_apps_languages, languageLabel(languages.joinToString(",")))
                } else {
                    stringResource(
                        if (entry.hasOfficialRules) R.string.navigation_apps_directions else R.string.navigation_apps_capture_only_badge,
                    )
                },
            )
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}

@Composable
private fun NavigationAppRow(
    app: AppEnablement,
    onToggle: (Boolean) -> Unit,
    onViewRules: (() -> Unit)?,
    languages: List<String>,
) {
    Column {
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
        AppIcon(app.packageNames)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = app.displayName, style = MaterialTheme.typography.titleMedium)
            if (!app.captureOnly && languages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.navigation_apps_languages, languageLabel(languages.joinToString(","))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    // Outside the toggle row, so it can't flip the switch.
    onViewRules?.let {
        TextButton(onClick = it, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.navigation_apps_view_rules))
        }
    }
    }
}

/** The installed app's own launcher icon (decorative: its name is right next to it). */
@Composable
private fun AppIcon(packageNames: List<String>) {
    val context = LocalContext.current
    val icon = remember(packageNames) {
        packageNames.firstNotNullOfOrNull { pkg ->
            runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
    }
}

