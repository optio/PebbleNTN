package com.pebblentn.app.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.catalog.PackageCheck

/**
 * An app's package names (FR5): its built-in ones, locked, then the ones the user added, each
 * removable, and a field to add another. A name is checked before it is stored ([onAdd]); a refusal
 * is explained under the field.
 *
 * @param onAdd checks and, when valid, stores a name; returns the check.
 * @param ownerName the display name of an app id, for "already used by" messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPackagesScreen(
    appName: String,
    builtIn: List<String>,
    userAdded: List<String>,
    onAdd: (String) -> PackageCheck,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    ownerName: (String) -> String = { it },
) {
    var input by rememberSaveable { mutableStateOf("") }
    var refusal by rememberSaveable { mutableStateOf<String?>(null) }
    var refusalOwner by rememberSaveable { mutableStateOf<String?>(null) }

    fun add() {
        when (val check = onAdd(input)) {
            is PackageCheck.Valid -> {
                input = ""
                refusal = null
                refusalOwner = null
            }
            is PackageCheck.Invalid -> {
                refusal = check.reason.name
                refusalOwner = check.ownerAppId
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_packages_title, appName)) },
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
                    text = stringResource(R.string.app_packages_explanation, appName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
                Text(
                    text = stringResource(R.string.app_packages_limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item(key = "built-in-title") { PackagesSectionTitle(stringResource(R.string.app_packages_built_in_title)) }
            items(builtIn, key = { "built-in-$it" }) { pkg ->
                ListItem(
                    headlineContent = { Text(pkg) },
                    supportingContent = { Text(stringResource(R.string.app_packages_built_in)) },
                    // Decorative: the supporting text says it is locked.
                    leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                )
            }
            item(key = "added-title") { PackagesSectionTitle(stringResource(R.string.app_packages_added_title)) }
            if (userAdded.isEmpty()) {
                item(key = "added-none") {
                    Text(
                        text = stringResource(R.string.app_packages_added_none),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(userAdded, key = { "added-$it" }) { pkg ->
                ListItem(
                    headlineContent = { Text(pkg) },
                    supportingContent = { Text(stringResource(R.string.app_packages_added)) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(pkg) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_app_packages_remove, pkg))
                        }
                    },
                )
            }
            item(key = "add") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            refusal = null
                            refusalOwner = null
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.app_packages_add_label)) },
                        placeholder = { Text(stringResource(R.string.app_packages_add_placeholder)) },
                        singleLine = true,
                        isError = refusal != null,
                        supportingText = refusal?.let { reason ->
                            {
                                Text(
                                    text = refusalMessage(PackageCheck.Reason.valueOf(reason), refusalOwner?.let(ownerName)),
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                    )
                    Button(onClick = ::add, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.app_packages_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun PackagesSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** Why a name was refused, naming the app that already uses it when there is one. */
@Composable
private fun refusalMessage(reason: PackageCheck.Reason, owner: String?): String = when (reason) {
    PackageCheck.Reason.EMPTY -> stringResource(R.string.app_packages_error_empty)
    PackageCheck.Reason.TOO_LONG -> stringResource(R.string.app_packages_error_too_long)
    PackageCheck.Reason.FORMAT -> stringResource(R.string.app_packages_error_format)
    PackageCheck.Reason.BUILT_IN_HERE -> stringResource(R.string.app_packages_error_built_in_here)
    PackageCheck.Reason.BUILT_IN_ELSEWHERE -> stringResource(R.string.app_packages_error_built_in_elsewhere, owner.orEmpty())
    PackageCheck.Reason.ADDED_HERE -> stringResource(R.string.app_packages_error_added_here)
    PackageCheck.Reason.ADDED_ELSEWHERE -> stringResource(R.string.app_packages_error_added_elsewhere, owner.orEmpty())
}
