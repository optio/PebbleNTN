package com.pebblentn.app.di

import android.content.Context
import androidx.room.Room
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.EnabledAppRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.notification.DebugCaptureProcessor
import com.pebblentn.app.notification.LastEligibleNotificationStore
import com.pebblentn.app.notification.NotificationDispatcher
import com.pebblentn.app.notification.SerialProcessingQueue
import com.pebblentn.app.rules.AssetRuleRepository
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.system.NotificationAccess
import com.pebblentn.app.system.SystemNotificationAccess
import kotlinx.coroutines.CoroutineScope
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container (no Hilt/Dagger, per the coding standards). Built once by the
 * [com.pebblentn.app.PebbleNtnApplication] and shared with the listener service and view models.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val catalog: NavigationAppCatalog = AssetCatalogSource(appContext).load()

    private val database: PebbleNtnDatabase = Room.databaseBuilder(
        appContext,
        PebbleNtnDatabase::class.java,
        PebbleNtnDatabase.NAME,
    ).addMigrations(*PebbleNtnDatabase.ALL_MIGRATIONS).build()

    val enabledAppRepository = EnabledAppRepository(database.appEnablementDao(), catalog)

    val debugHistoryRepository = DebugHistoryRepository(database.debugEventDao())

    val processingQueue = SerialProcessingQueue(applicationScope)

    val lastEligibleNotificationStore = LastEligibleNotificationStore()

    val notificationAccess: NotificationAccess = SystemNotificationAccess(appContext)

    private val ruleEngine = RuleEngine()

    private val ruleRepository = AssetRuleRepository(appContext)

    private val notificationProcessor = DebugCaptureProcessor(
        debugHistory = debugHistoryRepository,
        lastEligibleStore = lastEligibleNotificationStore,
        ruleEngine = ruleEngine,
        ruleRepository = ruleRepository,
        localeProvider = { Locale.getDefault().toLanguageTag() },
    )

    val notificationDispatcher = NotificationDispatcher(
        allowlist = enabledAppRepository,
        queue = processingQueue,
        processor = notificationProcessor,
    )

    private val installedAppsProvider = InstalledAppsProvider(appContext)

    /** Warm the allowlist cache on app start so the gate is authoritative before any callback. */
    fun start() {
        applicationScope.launch { enabledAppRepository.refreshCache() }
    }

    /**
     * Called when the listener connects: discover installed catalog apps (default-enabling new
     * ones) and refresh the allowlist cache.
     */
    fun onListenerConnected() {
        applicationScope.launch {
            val installed = installedAppsProvider.installedPackages(catalog.allPackageNames)
            enabledAppRepository.syncInstalledApps(installed)
        }
    }
}
