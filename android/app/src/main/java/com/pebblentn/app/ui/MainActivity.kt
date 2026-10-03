package com.pebblentn.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
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
import com.pebblentn.app.export.ExportMode
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.ui.apps.NavigationAppsScreen
import com.pebblentn.app.ui.dashboard.DashboardScreen
import com.pebblentn.app.ui.debug.DebugDetailScreen
import com.pebblentn.app.ui.debug.DebugHistoryScreen
import com.pebblentn.app.ui.debug.DebugHistoryViewModel
import com.pebblentn.app.ui.onboarding.OnboardingScreen
import com.pebblentn.app.ui.onboarding.OnboardingViewModel
import com.pebblentn.app.ui.rules.CaptureChoice
import com.pebblentn.app.ui.rules.OfficialRuleScreen
import com.pebblentn.app.ui.rules.RuleCompareScreen
import com.pebblentn.app.ui.rules.RuleEditorScreen
import com.pebblentn.app.ui.rules.RuleReviewScreen
import com.pebblentn.app.ui.rules.RulesScreen
import com.pebblentn.app.ui.rules.RulesViewModel
import com.pebblentn.app.ui.share.ShareDiagnosticsScreen
import com.pebblentn.app.ui.share.ShareDiagnosticsViewModel
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

    private val shareDiagnosticsViewModel: ShareDiagnosticsViewModel by lazy {
        val exporter = container.diagnosticExporter
        ViewModelProvider(this, viewModelFactory { initializer { ShareDiagnosticsViewModel(exporter) } })[ShareDiagnosticsViewModel::class.java]
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
                        catalog = container.catalog,
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
            val monochrome by container.appearance.monochrome.collectAsState()
            PebbleNtnTheme(monochrome = monochrome) {
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
        // Catch navigation apps installed while we were in the background (REQ-ANDROID-004).
        container.discoverInstalledApps()
    }

    @androidx.compose.runtime.Composable
    private fun AppNavHost() {
        val navController = rememberNavController()
        val lastEligible by container.lastEligibleNotificationStore.lastEligibleAtMillis.collectAsState()
        val events by debugViewModel.events.collectAsState()
        val appEnabled by container.appEnabledRepository.enabled.collectAsState()
        val unmatchedCount by container.debugHistoryRepository.observeUnmatchedCount()
            .collectAsState(initial = 0)
        val updateState by container.updateCheckRepository.state.collectAsState()
        val autoCheckUpdates by container.updateCheckRepository.autoCheckEnabled.collectAsState()
        val autoLaunch by container.watchSettingsRepository.autoLaunchEnabled.collectAsState()
        val watchStatus by container.navigationController.status.collectAsState()
        val watchappInstalled by container.watchappPresence.detected.collectAsState()
        val watchLink by container.watchLink.collectAsState()
        val monochrome by container.appearance.monochrome.collectAsState()
        val feedbackState by container.feedbackCampaign.state.collectAsState()
        val discoveredApps by container.enabledAppRepository.observeEnablement().collectAsState(initial = emptyList())
        val userRuleCount by produceState(initialValue = 0) {
            container.userRuleRepository.observeUserRules().collect { value = it.size }
        }

        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                val overrides by rulesViewModel.overrides.collectAsState()
                DashboardScreen(
                    accessGranted = true,
                    lastEligibleAtMillis = lastEligible,
                    appEnabled = appEnabled,
                    onAppEnabledChange = container::setAppEnabled,
                    onOpenDebugHistory = { navController.navigate("debug") },
                    onOpenRules = { navController.navigate("rules") },
                    onOpenNavigationApps = { navController.navigate("navigation-apps") },
                    onRefreshApp = { container.notificationListenerRefresher.refresh() },
                    unmatchedCaptureCount = unmatchedCount,
                    onShareDiagnostics = { navController.navigate("share-diagnostics") },
                    updateAvailable = updateState.updateAvailable,
                    latestVersion = updateState.latestVersion,
                    onDownloadUpdate = { openUrl(getString(R.string.update_releases_url)) },
                    onCheckForUpdate = ::checkForUpdate,
                    autoCheckUpdates = autoCheckUpdates,
                    onAutoCheckUpdatesChange = ::setAutoCheckUpdates,
                    autoLaunch = autoLaunch,
                    onAutoLaunchChange = container::setWatchAutoLaunch,
                    lastSentToWatch = watchStatus.lastSent.takeIf { watchStatus.navigating },
                    enabledAppCount = discoveredApps.count { it.enabled },
                    installedAppCount = discoveredApps.size,
                    officialRuleCount = container.bundledOfficialRules.size,
                    userRuleCount = userRuleCount,
                    watchappInstalled = watchappInstalled,
                    onGetWatchapp = { openUrl(getString(R.string.watchapp_store_url)) },
                    onConfirmWatchapp = container.watchappPresence::confirmManually,
                    watchLink = watchLink,
                    monochrome = monochrome,
                    onMonochromeChange = container.appearance::setMonochrome,
                    showFeedback = com.pebblentn.app.data.FeedbackCampaign.CURRENT.isVisible(
                        today = java.time.LocalDate.now(),
                        usedForNavigation = lastEligible != null,
                        state = feedbackState,
                    ),
                    feedbackActions = feedbackActions(),
                    overriddenOfficialCount = overrides.values.count { it.needsAttention },
                    onReviewOverrides = { navController.navigate("rules-review") },
                )
            }
            composable(
                route = "share-diagnostics?mode={mode}&event={event}",
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType; defaultValue = ExportMode.FULL.name },
                    navArgument("event") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                // Every visit starts on the mode it was opened for: full (REQ-DEBUG-011), rules only
                // from "Share your rules", or one event from "Share this event".
                val mode = entry.arguments?.getString("mode")?.let { runCatching { ExportMode.valueOf(it) }.getOrNull() } ?: ExportMode.FULL
                val eventId = entry.arguments?.getLong("event")?.takeIf { it >= 0 }
                LaunchedEffect(mode, eventId) { shareDiagnosticsViewModel.open(mode, eventId) }
                val shareState by shareDiagnosticsViewModel.state.collectAsState()
                ShareDiagnosticsScreen(
                    state = shareState,
                    onBack = { navController.popBackStack() },
                    onShareEmail = ::shareCaptureLogsByEmail,
                    onModeChange = shareDiagnosticsViewModel::setMode,
                    onShareSheet = ::shareDiagnosticsWithSharesheet,
                )
            }
            composable("debug") {
                DebugHistoryScreen(
                    events = events,
                    onEventClick = { id -> navController.navigate("debug/$id") },
                    onDeleteAll = debugViewModel::deleteAll,
                    onShare = { navController.navigate("share-diagnostics") },
                    onBack = { navController.popBackStack() },
                    appName = ::appName,
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
                    appName = ::appName,
                    onCreateRule = { navController.navigate("rule-editor-from/$id") },
                    onShareEvent = { navController.navigate("share-diagnostics?event=$id") },
                )
            }
            composable("navigation-apps") {
                val navigationApps by container.enabledAppRepository.observeEnablement()
                    .collectAsState(initial = emptyList())
                NavigationAppsScreen(
                    apps = navigationApps,
                    onToggle = ::setNavigationAppEnabled,
                    onBack = { navController.popBackStack() },
                    notInstalled = com.pebblentn.app.catalog.supportedNotInstalled(container.catalog, navigationApps.map { it.appId }.toSet()),
                    onGetApp = { pkg -> openUrl(getString(R.string.navigation_apps_store_url, pkg)) },
                    onViewRules = { appId -> navController.navigate("rules?app=$appId") },
                    languagesByApp = rulesViewModel.languagesByApp,
                )
            }
            composable(
                route = "rules?app={app}",
                arguments = listOf(navArgument("app") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val userRules by rulesViewModel.userRules.collectAsState()
                val overrides by rulesViewModel.overrides.collectAsState()
                RulesScreen(
                    officialGroups = rulesViewModel.officialGroups,
                    userRules = userRules,
                    onOpenOfficial = { id -> navController.navigate("official-rule/$id") },
                    onToggleUser = { id, enabled -> rulesViewModel.setEnabled(id, enabled) },
                    onEditUser = { id -> navController.navigate("rule-editor/$id") },
                    onDeleteUser = { id -> rulesViewModel.delete(id) },
                    onNewRule = { navController.navigate("rule-editor") },
                    onBack = { navController.popBackStack() },
                    appName = ::appName,
                    onShareRules = { navController.navigate("share-diagnostics?mode=${ExportMode.RULES_ONLY.name}") },
                    onUndoDelete = { rulesViewModel.undoDelete() },
                    initialAppId = entry.arguments?.getString("app"),
                    overrides = overrides,
                    onReviewOverrides = { navController.navigate("rules-review") },
                )
            }
            composable("rules-review") {
                val userRules by rulesViewModel.userRules.collectAsState()
                val overrides by rulesViewModel.overrides.collectAsState()
                RuleReviewScreen(
                    userRules = userRules,
                    overrides = overrides,
                    onRevert = { ids -> rulesViewModel.revert(ids) },
                    onKeep = { rulesViewModel.keepMine(it) },
                    onCompare = { id -> navController.navigate("rule-compare/$id") },
                    onShareRules = { navController.navigate("share-diagnostics?mode=${ExportMode.RULES_ONLY.name}") },
                    onBack = { navController.popBackStack() },
                    appName = ::appName,
                )
            }
            composable(
                route = "rule-compare/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
            ) { entry ->
                val ruleId = entry.arguments?.getString("ruleId").orEmpty()
                val pair by androidx.compose.runtime.produceState<Pair<String, String?>?>(null, ruleId) {
                    value = rulesViewModel.comparison(ruleId)
                }
                RuleCompareScreen(mine = pair?.first, official = pair?.second, onBack = { navController.popBackStack() })
            }
            composable(
                route = "official-rule/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
            ) { entry ->
                val rule = entry.arguments?.getString("ruleId")?.let(rulesViewModel::officialRule)
                OfficialRuleScreen(
                    rule = rule,
                    appName = rule?.let(rulesViewModel::officialRuleAppName),
                    onBack = { navController.popBackStack() },
                    onClone = { rulesViewModel.clone(it) },
                )
            }
            composable("rule-editor") { RuleEditorRoute(navController, ruleId = null) }
            composable(
                route = "rule-editor/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
            ) { entry -> RuleEditorRoute(navController, ruleId = entry.arguments?.getString("ruleId")) }
            composable(
                route = "rule-editor-from/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
            ) { entry -> RuleEditorRoute(navController, ruleId = null, fromEventId = entry.arguments?.getLong("eventId")) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun RuleEditorRoute(navController: androidx.navigation.NavController, ruleId: String?, fromEventId: Long? = null) {
        val initialJson by produceState<String?>(initialValue = null, ruleId, fromEventId) {
            value = fromEventId?.let { rulesViewModel.editorJsonFromCapture(it) } ?: rulesViewModel.editorInitialJson(ruleId)
        }
        val recent by rulesViewModel.recentCaptures.collectAsState()
        val captures = recent.map { event ->
            val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(event.receivedTimestampMillis))
            CaptureChoice(event.id, listOfNotNull(time, appName(event.packageName), event.snapshot?.title).joinToString(" · "))
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
                onPreview = { text, eventId -> rulesViewModel.previewAgainst(text, eventId) },
                captures = captures,
                initialCaptureId = fromEventId,
            )
        }
    }

    /** The feedback card's actions (#29): GitHub pages pre-filled with versions, or an email draft. */
    private fun feedbackActions(): com.pebblentn.app.ui.dashboard.FeedbackActions {
        val app = com.pebblentn.app.BuildConfig.VERSION_NAME
        val android = android.os.Build.VERSION.RELEASE
        return com.pebblentn.app.ui.dashboard.FeedbackActions(
            reportBug = { openUrl(getString(R.string.feedback_bug_url, app, android)) },
            requestFeature = { openUrl(getString(R.string.feedback_feature_url, app)) },
            suggestName = { openUrl(getString(R.string.feedback_name_url)) },
            email = { emailFeedback(app, android) },
            notNow = { container.feedbackCampaign.snooze(java.time.LocalDate.now()) },
            dismiss = container.feedbackCampaign::dismiss,
        )
    }

    /** For people without a GitHub account: a pre-addressed email draft they send themselves. */
    private fun emailFeedback(appVersion: String, androidVersion: String) {
        val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.share_logs_recipient)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_email_body, appVersion, androidVersion))
        }
        runCatching { startActivity(intent) }
    }

    /** The catalog's name for a package, for every screen that shows which app something came from. */
    private fun appName(packageName: String): String =
        com.pebblentn.app.ui.format.DisplayLabels.appName(container.catalog, packageName)

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /** Open an external URL (the GitHub releases page) in the browser. */
    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    }

    /**
     * Toggle the opt-in weekly update check. Turning it on does an immediate check so the user sees a
     * result right away (and that first network call is their explicit action).
     */
    private fun setAutoCheckUpdates(enabled: Boolean) {
        container.updateCheckRepository.setAutoCheckEnabled(enabled)
        if (enabled) checkForUpdate()
    }

    /** Toggle one navigation app's enablement (REQ-ANDROID-009); the listener cache picks it up via [com.pebblentn.app.data.EnabledAppRepository.setEnabled]. */
    private fun setNavigationAppEnabled(appId: String, enabled: Boolean) {
        lifecycleScope.launch { container.enabledAppRepository.setEnabled(appId, enabled) }
    }

    /** Manual "Check for updates": force a check now and report the outcome (REQ-ANDROID-013). */
    private fun checkForUpdate() {
        lifecycleScope.launch {
            val messageRes = when (container.updateCheckRepository.checkForUpdate(force = true)) {
                com.pebblentn.app.update.UpdateCheckOutcome.UPDATE_AVAILABLE -> R.string.update_check_available
                com.pebblentn.app.update.UpdateCheckOutcome.UP_TO_DATE -> R.string.update_check_up_to_date
                else -> R.string.update_check_failed
            }
            android.widget.Toast.makeText(this@MainActivity, messageRes, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share the reviewed payload with any app through the Android Sharesheet (REQ-DEBUG-006). It is
     * the same data the user just reviewed; nothing is sent until they pick an app and send it.
     */
    private fun shareDiagnosticsWithSharesheet() {
        val json = shareDiagnosticsViewModel.payloadJson() ?: return
        container.diagnosticShareManager.share(json, shareDiagnosticsViewModel.currentMode())
    }

    /**
     * Share the reviewed, 10 MB-capped capture logs by email, with the recipient and subject
     * prefilled (REQ-DEBUG-008). The user still presses send in their mail app — nothing is
     * transmitted automatically.
     */
    private fun shareCaptureLogsByEmail() {
        val json = shareDiagnosticsViewModel.payloadJson() ?: return
        container.diagnosticShareManager.shareViaEmail(
            json = json,
            mode = shareDiagnosticsViewModel.currentMode(),
            recipient = getString(R.string.share_logs_recipient),
            subject = getString(R.string.share_logs_subject),
            body = getString(R.string.share_logs_body),
        )
    }
}
