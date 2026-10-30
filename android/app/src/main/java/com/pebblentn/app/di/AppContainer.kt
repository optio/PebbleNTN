package com.pebblentn.app.di

import android.content.Context
import androidx.room.Room
import com.pebblentn.app.catalog.EffectiveCatalog
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.catalog.PackageCheck
import com.pebblentn.app.catalog.RuleFamilies
import com.pebblentn.app.data.AppEnabledRepository
import com.pebblentn.app.data.CustomPackagesRepository
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.EnabledAppRepository
import com.pebblentn.app.data.NavigationStateRepository
import com.pebblentn.app.data.UserRuleRepository
import com.pebblentn.app.data.WatchSettingsRepository
import com.pebblentn.app.data.WatchappPresenceRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.export.DiagnosticExporter
import com.pebblentn.app.export.DiagnosticShareManager
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.RuleRepository
import com.pebblentn.app.notification.DebugCaptureProcessor
import com.pebblentn.app.notification.LastEligibleNotificationStore
import com.pebblentn.app.notification.NotificationDispatcher
import com.pebblentn.app.notification.SerialProcessingQueue
import com.pebblentn.app.pebble.NavigationController
import com.pebblentn.app.pebble.PebbleWatchTransport
import com.pebblentn.app.protocol.WatchTransport
import com.pebblentn.app.rules.AssetRuleRepository
import com.pebblentn.app.rules.Rule
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RulePreviewService
import com.pebblentn.app.update.RuleUpdateRepository
import com.pebblentn.app.system.NotificationAccess
import com.pebblentn.app.system.NotificationListenerRefresher
import com.pebblentn.app.system.SystemNotificationAccess
import com.pebblentn.app.system.SystemNotificationListenerRefresher
import com.pebblentn.app.update.InstallRecord
import com.pebblentn.app.update.InstallRecords
import com.pebblentn.app.update.InstallSource
import com.pebblentn.app.update.InstallSources
import com.pebblentn.app.update.UpdateCheckRepository
import com.pebblentn.app.update.UpdatePolicy
import kotlinx.coroutines.CoroutineScope
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manual dependency container (no Hilt/Dagger, per the coding standards). Built once by the
 * [com.pebblentn.app.PebbleNtnApplication] and shared with the listener service and view models.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where this app was installed from, detected once per app start (REQ-ANDROID-020). */
    val installSource: InstallSource = InstallSources.detect(appContext)

    /**
     * What installed or last updated the running version, with the install times, detected once per
     * app start for the "Phone app version" row and debug exports (REQ-ANDROID-020, REQ-DEBUG-015).
     */
    val installRecord: InstallRecord = InstallRecords.detect(appContext)

    /** The package names the user added to navigation apps (FR5). */
    private val customPackagesRepository = CustomPackagesRepository(appContext)

    /**
     * The navigation-app catalog as this install uses it, with the user-added packages after each
     * app's own (FR6). On Google Play, apps that are not listed there (MicroG Maps, #113) join the app
     * whose rules they use and are never shown or linked. It changes when the user adds or removes a
     * package; see [observeCatalogChanges].
     */
    val effectiveCatalog = EffectiveCatalog(
        bundled = AssetCatalogSource(appContext).load(),
        googlePlay = installSource == InstallSource.PLAY,
        store = customPackagesRepository,
    )

    /** The current effective catalog. */
    val catalog: NavigationAppCatalog get() = effectiveCatalog.current

    /** Packages that share rules (#113): every rule applies to its whole family; always current. */
    val ruleFamilies: RuleFamilies = effectiveCatalog.families

    /** Master switch (REQ-ANDROID-011); on by default. */
    val appEnabledRepository = AppEnabledRepository(appContext)

    private val database: PebbleNtnDatabase = Room.databaseBuilder(
        appContext,
        PebbleNtnDatabase::class.java,
        PebbleNtnDatabase.NAME,
    ).addMigrations(*PebbleNtnDatabase.ALL_MIGRATIONS).build()

    val enabledAppRepository = EnabledAppRepository(database.appEnablementDao(), effectiveCatalog.catalog)

    private val assetRuleRepository = AssetRuleRepository(appContext, families = ruleFamilies)

    val debugHistoryRepository = DebugHistoryRepository(
        database.debugEventDao(),
        versions = com.pebblentn.app.data.SystemEventVersionsProvider(
            appContext,
            appVersionName = com.pebblentn.app.BuildConfig.VERSION_NAME,
            appVersionCode = com.pebblentn.app.BuildConfig.VERSION_CODE.toLong(),
            rulesetVersions = assetRuleRepository::rulesetVersions,
        ),
    )

    val processingQueue = SerialProcessingQueue(applicationScope)

    val lastEligibleNotificationStore = LastEligibleNotificationStore()

    val notificationAccess: NotificationAccess = SystemNotificationAccess(appContext)

    /** Rebinds the notification listener when it silently stops delivering posts (REQ-ANDROID-012). */
    val notificationListenerRefresher: NotificationListenerRefresher =
        SystemNotificationListenerRefresher(appContext)

    /** What the update features do for [installSource] (REQ-ANDROID-020, REQ-ANDROID-022). */
    val updatePolicy: UpdatePolicy = UpdatePolicy.forSource(installSource)

    /**
     * Update detection: by default offline, comparing the watchapp version with this app's
     * (REQ-ANDROID-019); the GitHub check is an optional extra, on demand or weekly when turned on
     * (REQ-ANDROID-013), and never on a Google Play install (REQ-ANDROID-022).
     */
    val updateCheckRepository = UpdateCheckRepository(
        appContext,
        currentVersion = com.pebblentn.app.BuildConfig.VERSION_NAME,
        installSource = installSource,
    )

    private val ruleEngine = RuleEngine(families = ruleFamilies)

    val userRuleRepository = UserRuleRepository(database.userRuleDao())

    // Remote official rules are OFF by default in the initial version (spec/000-overview.md).
    // Maintainers embed the official Ed25519 public key(s) here before enabling remote updates.
    val ruleUpdateRepository = RuleUpdateRepository(
        dao = database.officialRulesetDao(),
        publicKeys = emptyList(),
    )

    private val ruleRepository = RuleRepository {
        LayeredRules(
            user = userRuleRepository.userRulesSnapshot(),
            downloaded = ruleUpdateRepository.downloadedLayer(),
            bundled = assetRuleRepository.current().bundled,
        )
    }

    // The same families, so a MicroG Maps drawable is also looked up under the Google Maps name.
    private val iconReferences = com.pebblentn.app.notification.icon.AppIconReferences(appContext, families = ruleFamilies)

    /** Recognises the maneuver in large icons for apps whose rules map icons (#74). */
    val largeIconRecognizer: com.pebblentn.app.notification.icon.LargeIconRecognizer =
        com.pebblentn.app.notification.icon.RuleDrivenLargeIconRecognizer(
            appContext,
            ruleRepository::current,
            references = iconReferences,
            families = ruleFamilies,
        )

    /** Bundled official rules, for the Rules screen's official tab. */
    val bundledOfficialRules: List<Rule> get() = assetRuleRepository.current().bundled

    val rulePreviewService = RulePreviewService(engine = ruleEngine, localeProvider = { Locale.getDefault().toLanguageTag() })

    val diagnosticExporter = DiagnosticExporter(
        debugHistory = debugHistoryRepository,
        userRules = userRuleRepository,
        appVersion = com.pebblentn.app.BuildConfig.VERSION_NAME,
        appVersionCode = com.pebblentn.app.BuildConfig.VERSION_CODE.toLong(),
        // FR7: the export says which packages the user added, per app and per event.
        userAddedPackages = { effectiveCatalog.current.userAddedByApp() },
        // #119: what installed or last updated this version, in every export mode.
        installRecord = { installRecord },
    )

    val diagnosticShareManager = DiagnosticShareManager(appContext)

    val watchSettingsRepository = WatchSettingsRepository(appContext)

    /** Whether the watchapp is known to be installed (setup checklist, #28). */
    val watchappPresence = WatchappPresenceRepository(appContext)

    /** The optional Pebble monochrome theme (#28). */
    val appearance = com.pebblentn.app.data.AppearanceRepository(appContext)

    /** The temporary feedback campaign's dismissal state (#29). */
    val feedbackCampaign = com.pebblentn.app.data.FeedbackCampaignRepository(appContext)

    val watchTransport: WatchTransport = PebbleWatchTransport(
        appContext,
        autoLaunchEnabled = watchSettingsRepository::isAutoLaunchEnabled,
    )

    /** Whether a Pebble is reachable, for the dashboard (#28). */
    val watchLink: kotlinx.coroutines.flow.StateFlow<com.pebblentn.app.protocol.WatchLink> = watchTransport.watchLink()
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), com.pebblentn.app.protocol.WatchLink.Unknown)

    private val navigationStateRepository = NavigationStateRepository(database.navigationStateDao())

    /** The phone's mirror of the watchapp's own settings and the edits waiting for it (#102). */
    val watchSettingsMirror = com.pebblentn.app.data.WatchSettingsMirrorRepository(appContext)

    val navigationController = NavigationController(
        transport = watchTransport,
        scope = applicationScope,
        appVersion = com.pebblentn.app.BuildConfig.VERSION_NAME,
        stateStore = navigationStateRepository,
        onWatchappReached = { watchappPresence.markSeen(System.currentTimeMillis()) },
        initialSettings = watchSettingsRepository.settings.value,
        // Tell the watch about a newer phone app on every state message (REQ-ANDROID-017, #94): the
        // higher of the GitHub version and the watch's own version (REQ-ANDROID-019).
        latestPhoneUpdate = { updateCheckRepository.state.value.effectiveLatest },
        // A genuine WATCH_READY carries the watchapp version; a newer one means a phone app update.
        onWatchAppVersion = { updateCheckRepository.recordWatchAppVersion(it) },
        watchSettings = watchSettingsMirror,
    )

    init {
        applicationScope.launch {
            watchSettingsRepository.settings.collect { settings ->
                navigationController.onSettingsChanged(settings)
            }
        }
    }

    private val notificationProcessor = DebugCaptureProcessor(
        debugHistory = debugHistoryRepository,
        lastEligibleStore = lastEligibleNotificationStore,
        ruleEngine = ruleEngine,
        ruleRepository = ruleRepository,
        localeProvider = { Locale.getDefault().toLanguageTag() },
        onMatchedInstruction = { navigationController.onInstruction(it) },
        onSessionEnded = { navigationController.onNavigationStopped() },
    )

    val notificationDispatcher = NotificationDispatcher(
        allowlist = enabledAppRepository,
        queue = processingQueue,
        processor = notificationProcessor,
        appEnabled = appEnabledRepository::isEnabled,
    )

    /**
     * Flip the master switch. Turning it off also ends any navigation currently on the watch —
     * leaving a stale arrow on someone's wrist after they disabled the app would be worse than
     * useless.
     */
    fun setAppEnabled(enabled: Boolean) {
        appEnabledRepository.setEnabled(enabled)
        if (!enabled) {
            applicationScope.launch { navigationController.onNavigationStopped() }
        }
    }

    /**
     * Toggle automatic watchapp launch when navigation starts (REQ-WATCH-005, REQ-ANDROID-009).
     */
    fun setWatchAutoLaunch(enabled: Boolean) {
        watchSettingsRepository.setAutoLaunch(enabled)
    }

    /**
     * Change one of the watchapp's own settings from the Watch settings screen (REQ-ANDROID-021): sent
     * now while the watchapp is open, else when it next opens.
     */
    fun setWatchSetting(setting: com.pebblentn.app.core.WatchSetting, value: Int) {
        applicationScope.launch { navigationController.onPhoneSettingEdit(setting, value) }
    }

    private val installedAppsProvider = InstalledAppsProvider(appContext)

    /** Restore state, warm the caches, and begin listening for watch messages on app start. */
    fun start() {
        // A navigation app installed while we run must be picked up without a restart (REQ-ANDROID-004).
        com.pebblentn.app.system.NavigationAppInstallReceiver({ catalog.allPackageNames }, ::discoverInstalledApps)
            .register(appContext)
        observeCatalogChanges()
        applicationScope.launch {
            navigationController.restore()
            navigationController.start()
            enabledAppRepository.refreshCache()
            userRuleRepository.backfillSourceHashes(bundledOfficialRules)
            userRuleRepository.refreshCache()
            // The dashboard's "last navigation notification" survives restarts via the debug history.
            debugHistoryRepository.observeRecent(LAST_ELIGIBLE_LOOKBACK).first()
                .firstOrNull { it.eventType == com.pebblentn.app.data.DebugEventType.POSTED }
                ?.let { lastEligibleNotificationStore.seed(it.receivedTimestampMillis) }
            ruleUpdateRepository.refreshCache()
            // Log the watch link once so a logcat capture shows whether a PebbleKit 2 companion app
            // is present and a watch is connected — the usual reason the watchapp stays "Connecting".
            timber.log.Timber.i("Watch link at startup: %s", watchTransport.linkDiagnostics())
            // Weekly, self-throttling app-update check (REQ-ANDROID-013) — only if the user opted in,
            // so the app makes no network connection on its own by default; never on a Google Play
            // install, which gets its updates from Play only (REQ-ANDROID-022).
            if (updatePolicy.allowsGithubCheck && updateCheckRepository.autoCheckEnabled.value) {
                updateCheckRepository.checkForUpdate(force = false)
            }
        }
    }

    /**
     * A user-added package takes effect without a restart (FR6): the families already answer with the
     * new catalog; this re-renders the icon references and re-runs discovery, which creates the app's
     * row and rebuilds the allowlist from the new catalog.
     */
    private fun observeCatalogChanges() {
        applicationScope.launch {
            effectiveCatalog.catalog.drop(1).collect {
                iconReferences.clear()
                discoverInstalledApps()
            }
        }
    }

    /** Add a package name to a navigation app (FR5); the result says why it was refused. */
    fun addCustomPackage(appId: String, input: String): PackageCheck = effectiveCatalog.add(appId, input)

    /** Remove a user-added package name from a navigation app (FR5). */
    fun removeCustomPackage(appId: String, packageName: String) = effectiveCatalog.remove(appId, packageName)

    /** Called when the listener connects: discover installed catalog apps. */
    fun onListenerConnected() = discoverInstalledApps()

    /**
     * Discover installed catalog apps, default-enabling new ones (REQ-ANDROID-004), and refresh the
     * allowlist cache. Runs when the listener connects, when a catalog app is installed or updated,
     * and when the app returns to the foreground; it is cheap and idempotent.
     */
    fun discoverInstalledApps() {
        applicationScope.launch {
            val installed = installedAppsProvider.installedPackages(catalog.allPackageNames)
            enabledAppRepository.syncInstalledApps(installed)
        }
    }

    private companion object {
        /** How many recent debug events to scan for the newest posted notification. */
        const val LAST_ELIGIBLE_LOOKBACK = 20
    }
}
