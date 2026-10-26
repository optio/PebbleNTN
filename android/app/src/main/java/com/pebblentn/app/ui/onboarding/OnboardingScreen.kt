package com.pebblentn.app.ui.onboarding

import androidx.annotation.StringRes
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
import com.pebblentn.app.update.InstallSource
import com.pebblentn.app.update.UpdatePolicy

/**
 * The prominent disclosure for an app installed from [source] (REQ-SEC-004): a Google Play install
 * never runs the GitHub check, so its text has no GitHub sentence (REQ-ANDROID-022).
 */
@StringRes
fun onboardingDisclosure(source: InstallSource): Int =
    if (UpdatePolicy.forSource(source).allowsGithubCheck) R.string.onboarding_disclosure else R.string.onboarding_disclosure_play

/**
 * Onboarding: product explanation, the prominent notification-access disclosure (REQ-SEC-004,
 * shown before sending the user to system settings), the current access status, and a privacy note.
 */
@Composable
fun OnboardingScreen(
    accessGranted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where this app was installed from; picks the disclosure's network sentence (REQ-ANDROID-022). */
    installSource: InstallSource = InstallSource.GITHUB,
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
            // The whole setup at a glance (spec/400-ui: Pebble/watchapp setup guidance).
            Text(
                text = stringResource(R.string.onboarding_steps_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = listOf(
                    stringResource(R.string.onboarding_step_access),
                    stringResource(R.string.onboarding_step_watchapp),
                    stringResource(R.string.onboarding_step_navigation),
                ).joinToString("\n"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(onboardingDisclosure(installSource)),
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
