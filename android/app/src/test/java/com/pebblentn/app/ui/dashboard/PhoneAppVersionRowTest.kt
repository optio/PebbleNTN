package com.pebblentn.app.ui.dashboard

import com.pebblentn.app.R
import com.pebblentn.app.update.InstallKind
import com.pebblentn.app.update.InstallRecord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * REQ-ANDROID-020 (#119, FR2): the "Phone app version" row says what installed or last updated the
 * running version, "installed" until an update and "updated" after, and only the version after a
 * failed lookup.
 */
class PhoneAppVersionRowTest {

    private fun record(kind: InstallKind, installer: String?, updated: Boolean) =
        InstallRecord(kind, installer, firstInstallTime = 1_000L, lastUpdateTime = if (updated) 2_000L else 1_000L)

    @Test
    fun namedSourcesUseTheirNameInstalledThenUpdated() {
        val cases = listOf(
            Triple(InstallKind.PLAY, "com.android.vending", R.string.dashboard_update_source_play),
            Triple(InstallKind.FDROID, "org.fdroid.fdroid", R.string.dashboard_update_source_fdroid),
            Triple(InstallKind.OBTAINIUM, "dev.imranr.obtainium", R.string.dashboard_update_source_obtainium),
        )
        for ((kind, installer, name) in cases) {
            assertEquals(
                "$kind installed",
                PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_installed_by, sourceName = name),
                phoneAppVersionText("0.0.80", record(kind, installer, updated = false)),
            )
            assertEquals(
                "$kind updated",
                PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_updated_by, sourceName = name),
                phoneAppVersionText("0.0.80", record(kind, installer, updated = true)),
            )
        }
    }

    @Test
    fun aManualInstallSaysManually() {
        assertEquals(
            PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_installed_manually),
            phoneAppVersionText("0.0.80", record(InstallKind.MANUAL, null, updated = false)),
        )
        assertEquals(
            PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_updated_manually),
            phoneAppVersionText("0.0.80", record(InstallKind.MANUAL, "com.android.shell", updated = true)),
        )
    }

    @Test
    fun anotherInstallerShowsItsPackageName() {
        assertEquals(
            PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_installed_by, sourcePackage = "com.aurora.store"),
            phoneAppVersionText("0.0.80", record(InstallKind.OTHER, "com.aurora.store", updated = false)),
        )
        assertEquals(
            PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_updated_by, sourcePackage = "com.aurora.store"),
            phoneAppVersionText("0.0.80", record(InstallKind.OTHER, "com.aurora.store", updated = true)),
        )
    }

    @Test
    fun aFailedLookupShowsOnlyTheVersion() {
        assertEquals(PhoneAppVersionText("0.0.80"), phoneAppVersionText("0.0.80", InstallRecord.UNKNOWN))
    }

    @Test
    fun unknownTimesCountAsAFirstInstall() {
        assertEquals(
            PhoneAppVersionText("0.0.80", R.string.dashboard_app_version_installed_by, sourceName = R.string.dashboard_update_source_play),
            phoneAppVersionText("0.0.80", InstallRecord(InstallKind.PLAY, "com.android.vending")),
        )
    }
}
