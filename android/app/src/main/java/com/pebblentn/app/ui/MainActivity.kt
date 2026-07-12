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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.pebblentn.app.PebbleNtnApplication
import com.pebblentn.app.R
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.rules.PreviewResult
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.ui.dashboard.DashboardScreen
import com.pebblentn.app.ui.debug.DebugDetailScreen
import com.pebblentn.app.ui.debug.DebugHistoryScreen
import com.pebblentn.app.ui.debug.DebugHistoryViewModel
import com.pebblentn.app.ui.onboarding.OnboardingScreen
import com.pebblentn.app.ui.onboarding.OnboardingViewModel
import com.pebblentn.app.ui.rules.RuleEditorScreen
import com.pebblentn.app.ui.rules.RulesScreen
import com.pebblentn.app.ui.rules.RulesViewModel
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

    private val rulesViewModel: RulesViewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory {
                initializer {
                    RulesViewModel(
                        userRuleRepository = container.userRuleRepository,
                        debugHistoryRepository = container.debugHistoryRepository,
                        previewService = container.rulePreviewService,
                        officialRules = container.bundledOfficialRules,
                    )
                }
            },
        )[RulesViewModel::class.java]
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
                    onOpenRules = { navController.navigate("rules") },
                )
            }
            composable("debug") {
                DebugHistoryScreen(
                    events = events,
                    onEventClick = { id -> navController.navigate("debug/$id") },
                    onDeleteAll = debugViewModel::deleteAll,
                    onExport = ::exportDiagnostics,
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
            composable("rules") {
                val userRules by rulesViewModel.userRules.collectAsState()
                RulesScreen(
                    officialRules = rulesViewModel.officialRules,
                    userRules = userRules,
                    onClone = { rulesViewModel.clone(it) },
                    onToggleUser = { id, enabled -> rulesViewModel.setEnabled(id, enabled) },
                    onEditUser = { id -> navController.navigate("rule-editor/$id") },
                    onDeleteUser = { id -> rulesViewModel.delete(id) },
                    onNewRule = { navController.navigate("rule-editor") },
                )
            }
            composable("rule-editor") { RuleEditorRoute(navController, ruleId = null) }
            composable(
                route = "rule-editor/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
            ) { entry -> RuleEditorRoute(navController, ruleId = entry.arguments?.getString("ruleId")) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun RuleEditorRoute(navController: androidx.navigation.NavController, ruleId: String?) {
        val initialJson by produceState<String?>(initialValue = null, ruleId) {
            value = rulesViewModel.editorInitialJson(ruleId)
        }
        initialJson?.let { json ->
            RuleEditorScreen(
                initialJson = json,
                onBack = { navController.popBackStack() },
                onValidate = { text -> (rulesViewModel.validate(text) as? RuleValidationResult.Invalid)?.errors ?: emptyList() },
                onFormat = { text -> rulesViewModel.format(text) },
                onSave = { text ->
                    when (val result = rulesViewModel.save(text)) {
                        is RuleValidationResult.Valid -> emptyList()
                        is RuleValidationResult.Invalid -> result.errors
                    }
                },
                onPreview = { text -> previewDisplay(rulesViewModel.previewAgainstLatest(text)) },
            )
        }
    }

    private fun previewDisplay(result: PreviewResult?): String? = when (result) {
        null -> null
        is PreviewResult.InvalidRule -> result.errors.joinToString("; ")
        is PreviewResult.Evaluated -> {
            val eval = result.evaluation
            if (eval.matched) {
                getString(R.string.rule_editor_preview_matched, eval.instruction!!.maneuver.name)
            } else {
                getString(R.string.rule_editor_preview_unmatched, eval.trace.size)
            }
        }
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /** Build the export payload off the main thread, then open the Sharesheet (never auto-sends). */
    private fun exportDiagnostics(mode: com.pebblentn.app.export.ExportMode) {
        lifecycleScope.launch {
            val json = container.diagnosticExporter.build(mode)
            container.diagnosticShareManager.share(json, mode)
        }
    }
}
