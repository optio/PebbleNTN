package com.pebblentn.app.ui

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.catalog.NavigationAppEntry
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsSyncState
import com.pebblentn.app.data.AppEnablement
import com.pebblentn.app.protocol.WatchLink
import com.pebblentn.app.protocol.WatchMessages
import com.pebblentn.app.ui.apps.NavigationAppsScreen
import com.pebblentn.app.ui.dashboard.DashboardScreen
import com.pebblentn.app.ui.theme.PebbleNtnTheme
import com.pebblentn.app.ui.watchsettings.OptionListScreen
import com.pebblentn.app.ui.watchsettings.WatchSettingsScreen
import com.pebblentn.app.update.InstallKind
import com.pebblentn.app.update.InstallRecord
import com.pebblentn.app.update.InstallSource
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the Google Play / F-Droid phone screenshots from the current app's real screens
 * (REQ-ANDROID-022, #114 revision 1). Not a check: it runs only when
 * `PEBBLENTN_STORE_SCREENSHOTS_DIR` names an output folder, and is skipped otherwise.
 *
 *     PEBBLENTN_STORE_SCREENSHOTS_DIR=$PWD/tools/play-store-sources/phone \
 *       android/gradlew -p android :app:testDebugUnitTest --tests 'com.pebblentn.app.ui.StoreScreenshotsTest'
 *
 * Every screen shows a Google Play install with a connected colour Pebble Time 2 (emery) and the
 * state a user has after a first drive. The data is made up here; nothing comes from a device.
 * 360x720 dp at xxhdpi is 1080x2160 px, Play's 2:1 maximum. `tools/play_store_graphics.py` turns the
 * files into `fastlane/metadata/android/en-US/images/phoneScreenshots/`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h720dp-xxhdpi")
class StoreScreenshotsTest {

    val compose = createAndroidComposeRule<ComponentActivity>()

    /** See RuleReviewScreenTest: register the host activity so release unit tests run too. */
    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                shadowOf(context.packageManager)
                    .addActivityIfNotPresent(ComponentName(context, ComponentActivity::class.java))
            }
        })
        .around(compose)

    private val outDir: File? = System.getenv("PEBBLENTN_STORE_SCREENSHOTS_DIR")?.takeIf { it.isNotBlank() }?.let(::File)

    private val emery = WatchMessages.settings(42, platform = WatchPlatform.EMERY)
    private val watch = WatchSettingsSyncState(mirror = emery, receivedFromWatch = true)

    private fun capture(name: String, content: @Composable () -> Unit) {
        val dir = outDir
        assumeTrue("set PEBBLENTN_STORE_SCREENSHOTS_DIR to render the store screenshots", dir != null)
        compose.setContent { PebbleNtnTheme(darkTheme = false) { content() } }
        compose.waitForIdle()
        // captureToImage waits for a window redraw that Robolectric never reports; drawing the decor
        // view into a software canvas renders the same pixels with native graphics.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        check(dir!!.isDirectory || dir.mkdirs()) { "cannot create $dir" }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun dashboard() = capture("1-dashboard") {
        DashboardScreen(
            accessGranted = true,
            // A recent afternoon; it is only shown as a date and time.
            lastEligibleAtMillis = 1_792_946_100_000L,
            installSource = InstallSource.PLAY,
            installRecord = InstallRecord(InstallKind.PLAY, "com.android.vending", firstInstallTime = 1L, lastUpdateTime = 2L),
            appVersion = "0.0.77",
            watchAppVersion = "0.0.77",
            watchappInstalled = true,
            watchLink = WatchLink.Connected(listOf("Pebble Time 2")),
            lastSentToWatch = NavigationInstruction(
                maneuver = Maneuver.RIGHT,
                distanceMeters = 450,
                primaryText = "Rue de la Loi",
                secondaryText = "13:00",
            ),
            enabledAppCount = 3,
            installedAppCount = 3,
            officialRuleCount = 128,
            userRuleCount = 2,
        )
    }

    @Test
    fun watchSettings() = capture("2-watch-settings") {
        WatchSettingsScreen(watch, watchappOpen = true, onOpenSetting = {}, onBack = {})
    }

    @Test
    fun accentColourWithPreview() = capture("3-accent-colour") {
        OptionListScreen(WatchSetting.ACCENT, emery, queued = false, onSelect = {}, onBack = {})
    }

    @Test
    fun navigationApps() = capture("4-navigation-apps") {
        NavigationAppsScreen(
            apps = listOf(
                AppEnablement("google-maps", "Google Maps", listOf("com.google.android.apps.maps"), enabled = true, captureOnly = false),
                AppEnablement("osmand", "OsmAnd", listOf("net.osmand"), enabled = true, captureOnly = false),
                AppEnablement("organic-maps", "Organic Maps", listOf("app.organicmaps"), enabled = true, captureOnly = false),
            ),
            onToggle = { _, _ -> },
            onBack = {},
            notInstalled = listOf(
                NavigationAppEntry("comaps", "CoMaps", listOf("app.comaps"), hasOfficialRules = true, captureAvailable = true, defaultEnabled = true),
                NavigationAppEntry("waze", "Waze", listOf("com.waze"), hasOfficialRules = false, captureAvailable = true, defaultEnabled = true),
                NavigationAppEntry("here", "HERE WeGo", listOf("com.here.app.maps"), hasOfficialRules = false, captureAvailable = true, defaultEnabled = true),
            ),
            languagesByApp = mapOf("google-maps" to listOf("en", "de", "fr", "es", "it", "nl")),
        )
    }
}
