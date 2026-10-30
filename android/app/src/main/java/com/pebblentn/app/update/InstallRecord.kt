package com.pebblentn.app.update

import android.content.Context
import timber.log.Timber

/**
 * What installed or last updated the running version, as shown by the "Phone app version" row and
 * written to debug exports (REQ-ANDROID-020, REQ-DEBUG-015). Finer than [InstallSource], which only
 * says where updates come from: a manual install, another store and a failed lookup are all
 * [InstallSource.GITHUB] there but are told apart here.
 */
enum class InstallKind {
    PLAY,
    FDROID,
    OBTAINIUM,

    /** No installer, the system package installer, or adb. */
    MANUAL,

    /** Any other installer; its package name is kept in [InstallRecord.installingPackage]. */
    OTHER,

    /** The package manager lookup failed. */
    UNKNOWN,
}

/**
 * The install facts the package manager gives about this app, read once per start with no network
 * (REQ-ANDROID-020). Android keeps only the installer of the current version, so [installingPackage]
 * is whatever installed the latest install or update. Every field but [kind] is null after a failed
 * lookup; [originatingPackage] is usually null for this app.
 */
data class InstallRecord(
    val kind: InstallKind,
    val installingPackage: String? = null,
    val initiatingPackage: String? = null,
    val originatingPackage: String? = null,
    /** `PackageInfo.firstInstallTime`, epoch millis. */
    val firstInstallTime: Long? = null,
    /** `PackageInfo.lastUpdateTime`, epoch millis; equal to [firstInstallTime] until an update. */
    val lastUpdateTime: Long? = null,
) {
    /** True once a version was installed over the first one; null when either time is unknown. */
    val updatedSinceInstall: Boolean?
        get() = if (firstInstallTime == null || lastUpdateTime == null) null else firstInstallTime != lastUpdateTime

    companion object {
        /** The record after a failed lookup. */
        val UNKNOWN = InstallRecord(InstallKind.UNKNOWN)
    }
}

/** Install-record classification (pure) and the platform lookup. */
object InstallRecords {

    /** The system package installers (AOSP and Google builds), i.e. an APK opened by the user. */
    private val PACKAGE_INSTALLERS = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
    )

    /** The shell's package, the installer of `adb install`. */
    private const val SHELL_PACKAGE = "com.android.shell"

    /**
     * The kind for an installer package name. Google Play, F-Droid and Obtainium reuse
     * [InstallSources.fromInstaller]'s package lists; no installer (null or blank), the system package
     * installer and the shell are [InstallKind.MANUAL]; anything else is [InstallKind.OTHER].
     */
    fun classify(installingPackage: String?): InstallKind {
        if (installingPackage.isNullOrBlank() || installingPackage in PACKAGE_INSTALLERS || installingPackage == SHELL_PACKAGE) {
            return InstallKind.MANUAL
        }
        return when (InstallSources.fromInstaller(installingPackage)) {
            InstallSource.PLAY -> InstallKind.PLAY
            InstallSource.FDROID -> InstallKind.FDROID
            InstallSource.OBTAINIUM -> InstallKind.OBTAINIUM
            InstallSource.GITHUB -> InstallKind.OTHER
        }
    }

    /** The record for the values the package manager returned. */
    fun from(
        installingPackage: String?,
        initiatingPackage: String?,
        originatingPackage: String?,
        firstInstallTime: Long?,
        lastUpdateTime: Long?,
    ): InstallRecord = InstallRecord(
        kind = classify(installingPackage),
        installingPackage = installingPackage,
        initiatingPackage = initiatingPackage,
        originatingPackage = originatingPackage,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
    )

    /**
     * Run [lookup] (the platform calls) and turn a failure into [InstallRecord.UNKNOWN], logged. Split
     * from [detect] so the failure path is testable without a package manager.
     */
    fun detectWith(lookup: () -> InstallRecord): InstallRecord = runCatching(lookup).getOrElse { e ->
        Timber.w(e, "Install record lookup failed")
        InstallRecord.UNKNOWN
    }

    /**
     * This app's install record, from `PackageManager.getInstallSourceInfo` and
     * `getPackageInfo(packageName, 0)` (no `<queries>` entry needed for the app itself). Reads no
     * network; a failed lookup gives [InstallRecord.UNKNOWN].
     */
    fun detect(context: Context): InstallRecord = detectWith {
        val pm = context.packageManager
        val source = pm.getInstallSourceInfo(context.packageName)
        val info = pm.getPackageInfo(context.packageName, 0)
        from(
            installingPackage = source.installingPackageName,
            initiatingPackage = source.initiatingPackageName,
            originatingPackage = source.originatingPackageName,
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
        )
    }
}
