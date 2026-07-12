package com.pebblentn.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.ui.components.AccessStatusChip
import com.pebblentn.app.ui.theme.PebbleNtnTheme

/**
 * Onboarding: product explanation, the prominent notification-access disclosure (REQ-SEC-004,
 * shown before sending the user to system settings), the current access status, and a privacy note.
 */
@Composable
fun OnboardingScreen(
    accessGranted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.onboarding_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.onboarding_disclosure),
                style = MaterialTheme.typography.bodyMedium,
            )

            AccessStatusChip(accessGranted = accessGranted)

            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.onboarding_open_settings))
            }

            Text(
                text = stringResource(R.string.onboarding_privacy_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    PebbleNtnTheme {
        OnboardingScreen(accessGranted = false, onOpenSettings = {})
    }
}
