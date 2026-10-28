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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.export.ExportMode
import com.pebblentn.app.rules.RuleValidationResult
import com.pebblentn.app.ui.apps.NavigationAppsScreen
import com.pebblentn.app.ui.dashboard.DashboardScreen
import com.pebblentn.app.ui.dashboard.updatePrompt
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
import com.pebblentn.app.ui.watchsettings.OptionListScreen
import com.pebblentn.app.ui.watchsettings.WatchSettingsScreen
import com.pebblentn.app.ui.watchsettings.WatchSettingsUi
import com.pebblentn.app.update.UpdateCheckMessage
import com.pebblentn.app.update.updateCheckMessage

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
                        families = container.ruleFamilies,
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
                    OnboardingScreen(
                        accessGranted = false,
                        onOpenSettings = ::openNotificationListenerSettings,
                        installSource = container.installSource,
                    )
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
        // REQ-ANDROID-018 (#94): reading lastCheckMillis recomposes when a check records its time,
        // which is what moves the stale item off the top.
        val lastUpdateCheck by container.updateCheckRepository.lastCheckMillis.collectAsState()
        val updateCheckStale = lastUpdateCheck.let { container.updateCheckRepository.isCheckStale() }
        // A check started from the stale item finished while weekly checks were off. Kept for this
        // screen only, not persisted: the question comes back only with the next stale item.
        var staleCheckFinished by rememberSaveable { mutableStateOf(false) }
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
                    // The higher of the GitHub version and the watch's version (REQ-ANDROID-019).
                    latestVersion = updateState.effectiveLatest,
                    // Follows the stored version live, so a new WATCH_READY shows at once (REQ-ANDROID-019).
                    watchAppVersion = updateState.watchAppVersion,
                    installSource = container.installSource,
                    onOpenUpdateSource = ::openUpdateSource,
                    onCheckForUpdate = ::checkForUpdate,
                    autoCheckUpdates = autoCheckUpdates,
                    onAutoCheckUpdatesChange = ::setAutoCheckUpdates,
                    autoLaunch = autoLaunch,
                    onAutoLaunchChange = container::setWatchAutoLaunch,
                    onOpenWatchSettings = { navController.navigate("watch-settings") },
                    lastSentToWatch = watchStatus.lastSent.takeIf { watchStatus.navigating },
                    enabledAppCount = discoveredApps.count { it.enabled && it.installed },
                    installedAppCount = discoveredApps.count { it.installed },
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
                    updatePrompt = updatePrompt(
                        stale = updateCheckStale,
                        autoCheckEnabled = autoCheckUpdates,
                        staleCheckFinished = staleCheckFinished,
                        githubCheckAllowed = container.updatePolicy.allowsGithubCheck,
                    ),
                    updateNeverChecked = lastUpdateCheck == 0L,
                    onStaleCheck = {
                        // Whatever the outcome, ask about weekly checks afterwards, if they are off.
                        checkForUpdate(onFinished = {
                            staleCheckFinished = !container.updateCheckRepository.autoCheckEnabled.value
                        })
                    },
                    onEnableWeeklyChecks = {
                        staleCheckFinished = false
                        // The check just ran, so turning weekly checks on needs no second one now.
                        container.updateCheckRepository.setAutoCheckEnabled(true)
                    },
                    onDismissWeeklyQuestion = { staleCheckFinished = false },
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
            composable("watch-settings") {
                val watchSettings by container.watchSettingsMirror.state.collectAsState()
                val watchappOpen by container.navigationController.watchappOpen.collectAsState()
                WatchSettingsScreen(
                    state = watchSettings,
                    watchappOpen = watchappOpen,
                    onOpenSetting = { setting -> navController.navigate("watch-settings/${setting.name}") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "watch-settings/{setting}",
                arguments = listOf(navArgument("setting") { type = NavType.StringType }),
            ) { entry ->
                val setting = entry.arguments?.getString("setting")
                    ?.let { name -> WatchSetting.entries.firstOrNull { it.name == name } }
                val watchSettings by container.watchSettingsMirror.state.collectAsState()
                val watchappOpen by container.navigationController.watchappOpen.collectAsState()
                val mirror = watchSettings.mirror
                if (setting == null || mirror == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    OptionListScreen(
                        setting = setting,
                        model = mirror,
                        queued = WatchSettingsUi.showsQueuedNotice(watchSettings, watchappOpen),
                        onSelect = { value -> container.setWatchSetting(setting, value) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable("navigation-apps") {
                val navigationApps by container.enabledAppRepository.observeEnablement()
                    .collectAsState(initial = emptyList())
                val catalog by container.effectiveCatalog.catalog.collectAsState()
                // An uninstalled app keeps its row; it is listed with the apps that aren't installed.
                val installedApps = navigationApps.filter { it.installed }
                NavigationAppsScreen(
                    apps = installedApps,
                    onToggle = ::setNavigationAppEnabled,
                    onBack = { navController.popBackStack() },
                    notInstalled = com.pebblentn.app.catalog.supportedNotInstalled(
                        catalog,
                        installedApps.map { it.appId }.toSet(),
                    ),
                    onGetApp = { pkg -> openUrl(getString(R.string.navigation_apps_store_url, pkg)) },
                    // Only non-Play installs have websites in their catalog (#113).
                    onOpenWebsite = ::openUrl,
                    onViewRules = { appId -> navController.navigate("rules?app=$appId") },
                    languagesByApp = rulesViewModel.languagesByApp,
                    // Only listed apps are offered, so never MicroG Maps on Google Play (FR4).
                    onOpenPackages = { appId -> navController.navigate("app-packages/$appId") },
                )
            }
            composable(
                route = "app-packages/{appId}",
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                val appId = entry.arguments?.getString("appId").orEmpty()
                val effective = container.effectiveCatalog
                // Recompose with every added or removed package.
                val catalog by effective.catalog.collectAsState()
                val app = effective.builtIn.apps.firstOrNull { it.appId == appId && it.hasOfficialRules }
                if (app == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    com.pebblentn.app.ui.apps.AppPackagesScreen(
                        appName = app.displayName,
                        builtIn = app.packageNames,
                        userAdded = catalog.userAddedByApp()[appId].orEmpty(),
                        onAdd = { input -> container.addCustomPackage(appId, input) },
                        onRemove = { pkg -> container.removeCustomPackage(appId, pkg) },
                        onBack = { navController.popBackStack() },
                        ownerName = effective::displayNameOf,
                    )
                }
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
     * Send the user to [source] for the update (REQ-ANDROID-020): GitHub's latest release page, the
     * F-Droid package page (an installed F-Droid client handles the link, else the browser), or the
     * Obtainium app's launch screen, falling back to its website when it is not installed, or this
     * app's Google Play listing (the Play app, else the web listing; REQ-ANDROID-022).
     */
    private fun openUpdateSource(source: com.pebblentn.app.update.InstallSource) {
        when (source) {
            com.pebblentn.app.update.InstallSource.PLAY -> {
                val market = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(getString(R.string.update_play_market_url, packageName)))
                try {
                    startActivity(market)
                } catch (e: android.content.ActivityNotFoundException) {
                    timber.log.Timber.w(e, "No app handles market://; opening the Play web listing")
                    openUrl(getString(R.string.update_play_web_url, packageName))
                }
            }
            com.pebblentn.app.update.InstallSource.GITHUB -> openUrl(getString(R.string.update_releases_url))
            com.pebblentn.app.update.InstallSource.FDROID -> openUrl(getString(R.string.update_fdroid_url))
            com.pebblentn.app.update.InstallSource.OBTAINIUM -> {
                val launch = com.pebblentn.app.update.InstallSources.OBTAINIUM_PACKAGES
                    .firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }
                val opened = launch != null && runCatching { startActivity(launch) }
                    .onFailure { timber.log.Timber.w(it, "Obtainium did not open; using its website") }
                    .isSuccess
                if (!opened) openUrl(getString(R.string.update_obtainium_url))
            }
        }
    }

    /**
     * Toggle the opt-in weekly update check. Turning it on does an immediate check so the user sees a
     * result right away (and that first network call is their explicit action).
     */
    private fun setAutoCheckUpdates(enabled: Boolean) {
        // Unreachable on Google Play, whose dashboard has no toggle; guarded anyway (REQ-ANDROID-022).
        if (!container.updatePolicy.allowsGithubCheck) return
        container.updateCheckRepository.setAutoCheckEnabled(enabled)
        if (enabled) checkForUpdate()
    }

    /** Toggle one navigation app's enablement (REQ-ANDROID-009); the listener cache picks it up via [com.pebblentn.app.data.EnabledAppRepository.setEnabled]. */
    private fun setNavigationAppEnabled(appId: String, enabled: Boolean) {
        lifecycleScope.launch { container.enabledAppRepository.setEnabled(appId, enabled) }
    }

    /**
     * Manual "Check for updates": force a GitHub check now and report the result combined with the
     * watchapp version, so the message matches the update card (REQ-ANDROID-013, REQ-ANDROID-019).
     * [onFinished] runs after the outcome is shown, whatever it was (the stale item's follow-up, #94).
     */
    private fun checkForUpdate(onFinished: () -> Unit = {}) {
        // Unreachable on Google Play, whose dashboard has no check row or stale item (REQ-ANDROID-022).
        if (!container.updatePolicy.allowsGithubCheck) return
        lifecycleScope.launch {
            val repository = container.updateCheckRepository
            val outcome = repository.checkForUpdate(force = true)
            val message = when (val result = updateCheckMessage(outcome, repository.state.value)) {
                is UpdateCheckMessage.Available -> getString(R.string.update_check_available_version, result.version)
                UpdateCheckMessage.UpToDate -> getString(R.string.update_check_up_to_date)
                UpdateCheckMessage.Failed -> getString(R.string.update_check_failed)
            }
            android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_SHORT).show()
            onFinished()
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
