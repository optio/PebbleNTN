package com.pebblentn.app.update

import android.content.Context
import timber.log.Timber

/**
 * Where the phone app was installed from, so the update card can send the user back there
 * (REQ-ANDROID-020). Four sources have an update destination; anything else is [GITHUB]. A [PLAY]
 * install gets its updates from Google Play only: see [UpdatePolicy] (REQ-ANDROID-022).
 */
enum class InstallSource { GITHUB, FDROID, OBTAINIUM, PLAY }

/** Install-source detection: a pure installer-package mapping, and the platform lookup. */
object InstallSources {

    /** The F-Droid client and the common clients of F-Droid repositories. */
    private val FDROID_INSTALLERS = setOf(
        "org.fdroid.fdroid",
        "org.fdroid.basic",
        "com.looker.droidify",
        "com.machiav3lli.fdroid",
    )

    /** The Google Play Store's package, the installer of every Play install. */
    const val PLAY_STORE_PACKAGE = "com.android.vending"

    /** Obtainium's package; its builds from other channels add a suffix ("….fdroid"). */
    const val OBTAINIUM_PACKAGE = "dev.imranr.obtainium"

    /** The Obtainium packages whose launch screen the card may open, in order of preference. */
    val OBTAINIUM_PACKAGES = listOf(OBTAINIUM_PACKAGE, "$OBTAINIUM_PACKAGE.fdroid")

    /**
     * The source for an installer package name: Google Play, F-Droid and Obtainium by name, and
     * [InstallSource.GITHUB] for everything else — no installer, a browser, the package installer, adb,
     * or another store.
     */
    fun fromInstaller(installerPackage: String?): InstallSource = when {
        installerPackage == null -> InstallSource.GITHUB
        installerPackage == PLAY_STORE_PACKAGE -> InstallSource.PLAY
        installerPackage in FDROID_INSTALLERS -> InstallSource.FDROID
        installerPackage == OBTAINIUM_PACKAGE || installerPackage.startsWith("$OBTAINIUM_PACKAGE.") ->
            InstallSource.OBTAINIUM
        else -> InstallSource.GITHUB
    }

    /**
     * This app's install source, from `PackageManager.getInstallSourceInfo`. Reads no network. A
     * failed lookup is logged and treated as [InstallSource.GITHUB], whose release page always works.
     */
    fun detect(context: Context): InstallSource = try {
        val info = context.packageManager.getInstallSourceInfo(context.packageName)
        fromInstaller(info.installingPackageName)
    } catch (e: Exception) {
        Timber.w(e, "Install source lookup failed; assuming GitHub")
        InstallSource.GITHUB
    }
}
