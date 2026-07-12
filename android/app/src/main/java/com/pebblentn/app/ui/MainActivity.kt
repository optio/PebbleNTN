package com.pebblentn.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.pebblentn.app.PebbleNtnApplication
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.ui.dashboard.DashboardScreen
import com.pebblentn.app.ui.debug.DebugDetailScreen
import com.pebblentn.app.ui.debug.DebugHistoryScreen
import com.pebblentn.app.ui.debug.DebugHistoryViewModel
import com.pebblentn.app.ui.onboarding.OnboardingScreen
import com.pebblentn.app.ui.onboarding.OnboardingViewModel
import com.pebblentn.app.ui.theme.PebbleNtnTheme

/**
 * Single host activity. Shows onboarding until notification access is granted; once granted, hosts
 * the dashboard → debug-history → detail navigation graph. Access is re-checked in [onResume].
 */
class MainActivity : ComponentActivity() {

    private val container by lazy { (application as PebbleNtnApplication).container }

    private val onboardingViewModel: OnboardingViewModel by lazy {
        val access = container.notificationAccess
        ViewModelProvider(this, viewModelFactory { initializer { OnboardingViewModel(access) } })[OnboardingViewModel::class.java]
    }

    private val debugViewModel: DebugHistoryViewModel by lazy {
        val repo = container.debugHistoryRepository
        ViewModelProvider(this, viewModelFactory { initializer { DebugHistoryViewModel(repo) } })[DebugHistoryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PebbleNtnTheme {
                val uiState by onboardingViewModel.uiState.collectAsState()
                if (uiState.accessGranted) {
                    AppNavHost()
                } else {
                    OnboardingScreen(accessGranted = false, onOpenSettings = ::openNotificationListenerSettings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onboardingViewModel.refresh()
    }

    @androidx.compose.runtime.Composable
    private fun AppNavHost() {
        val navController = rememberNavController()
        val lastEligible by container.lastEligibleNotificationStore.lastEligibleAtMillis.collectAsState()
        val events by debugViewModel.events.collectAsState()

        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                DashboardScreen(
                    accessGranted = true,
                    lastEligibleAtMillis = lastEligible,
                    onOpenDebugHistory = { navController.navigate("debug") },
                )
            }
            composable("debug") {
                DebugHistoryScreen(
                    events = events,
                    onEventClick = { id -> navController.navigate("debug/$id") },
                    onDeleteAll = debugViewModel::deleteAll,
                )
            }
            composable(
                route = "debug/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val event by produceState<DebugEvent?>(initialValue = null, id, events) {
                    value = debugViewModel.detail(id)
                }
                DebugDetailScreen(
                    event = event,
                    onBack = { navController.popBackStack() },
                    onDelete = {
                        debugViewModel.deleteEvent(id)
                        navController.popBackStack()
                    },
                )
            }
        }
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}
