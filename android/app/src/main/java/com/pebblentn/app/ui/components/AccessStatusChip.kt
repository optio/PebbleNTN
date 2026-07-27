package com.pebblentn.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
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
 * Compact status chip for notification-access state: a green check-circle when access is granted, a
 * warning sign when it is not. Status is conveyed by the icon shape and the text (plus an explicit
 * content description) — never by colour alone (accessibility, spec/400-ui/AndroidUI.md).
 */
@Composable
fun AccessStatusChip(accessGranted: Boolean, modifier: Modifier = Modifier) {
    val label = stringResource(
        if (accessGranted) R.string.access_status_granted else R.string.access_status_denied,
    )
    val description = stringResource(
        if (accessGranted) R.string.cd_access_granted else R.string.cd_access_denied,
    )
    // A green that stays legible on both light and dark chip surfaces (M3 has no "success" role).
    val grantedColor = if (isSystemInDarkTheme()) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val indicator = if (accessGranted) grantedColor else MaterialTheme.colorScheme.error
    val icon = if (accessGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = indicator)
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledLeadingIconContentColor = indicator,
        ),
        modifier = modifier.semantics { contentDescription = description },
    )
}
