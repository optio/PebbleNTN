package com.pebblentn.app.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pebblentn.app.R

/**
 * Compact status chip for notification-access state. Status is conveyed by text and an explicit
 * content description — never by color alone (accessibility, spec/400-ui/AndroidUI.md).
 */
@Composable
fun AccessStatusChip(accessGranted: Boolean, modifier: Modifier = Modifier) {
    val label = stringResource(
        if (accessGranted) R.string.access_status_granted else R.string.access_status_denied,
    )
    val description = stringResource(
        if (accessGranted) R.string.cd_access_granted else R.string.cd_access_denied,
    )
    val indicator: Color =
        if (accessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledLeadingIconContentColor = indicator,
        ),
        modifier = modifier.semantics { contentDescription = description },
    )
}
