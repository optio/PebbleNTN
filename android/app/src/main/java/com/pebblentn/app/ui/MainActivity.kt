package com.pebblentn.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pebblentn.app.PebbleNtnApplication
import com.pebblentn.app.ui.dashboard.DashboardScreen
import com.pebblentn.app.ui.onboarding.OnboardingScreen
import com.pebblentn.app.ui.onboarding.OnboardingViewModel
import com.pebblentn.app.ui.theme.PebbleNtnTheme

/**
 * Single host activity. Shows onboarding until notification access is granted, then the dashboard.
 * Access is re-checked in [onResume] so returning from system settings updates the UI immediately.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by lazy {
        val access = (application as PebbleNtnApplication).container.notificationAccess
        ViewModelProvider(
            this,
            viewModelFactory { initializer { OnboardingViewModel(access) } },
        )[OnboardingViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val lastEligibleStore = (application as PebbleNtnApplication).container.lastEligibleNotificationStore
        setContent {
            PebbleNtnTheme {
                val uiState by viewModel.uiState.collectAsState()
                if (uiState.accessGranted) {
                    val lastEligible by lastEligibleStore.lastEligibleAtMillis.collectAsState()
                    DashboardScreen(accessGranted = true, lastEligibleAtMillis = lastEligible)
                } else {
                    OnboardingScreen(
                        accessGranted = false,
                        onOpenSettings = ::openNotificationListenerSettings,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}
