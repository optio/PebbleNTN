package com.pebblentn.app.ui.dashboard

import androidx.annotation.StringRes
import com.pebblentn.app.R
import com.pebblentn.app.update.InstallKind
import com.pebblentn.app.update.InstallRecord

/**
 * The value of the Updates section's "Phone app version" row, as resources to resolve: [format]
 * takes the version as `%1$s` and, for a named source, the source as `%2$s` — [sourceName] (a
 * `dashboard_update_source_*` resource) or [sourcePackage] (another installer's raw package name,
 * not translated). A null [format] means the bare [version].
 */
data class PhoneAppVersionText(
    val version: String,
    @param:StringRes val format: Int? = null,
    @param:StringRes val sourceName: Int? = null,
    val sourcePackage: String? = null,
)

/**
 * The "Phone app version" row for [record] (REQ-ANDROID-020, #119): "installed by" until a version is
 * installed over the first one, "updated by" after; "manually" for a manual install; another
 * installer by its package name; only the version after a failed lookup. Pure, so the text is
 * unit-tested apart from the Compose row.
 */
fun phoneAppVersionText(version: String, record: InstallRecord): PhoneAppVersionText {
    val updated = record.updatedSinceInstall == true
    val byFormat = if (updated) R.string.dashboard_app_version_updated_by else R.string.dashboard_app_version_installed_by
    return when (record.kind) {
        InstallKind.PLAY -> PhoneAppVersionText(version, byFormat, sourceName = R.string.dashboard_update_source_play)
        InstallKind.FDROID -> PhoneAppVersionText(version, byFormat, sourceName = R.string.dashboard_update_source_fdroid)
        InstallKind.OBTAINIUM -> PhoneAppVersionText(version, byFormat, sourceName = R.string.dashboard_update_source_obtainium)
        InstallKind.OTHER -> record.installingPackage
            ?.let { PhoneAppVersionText(version, byFormat, sourcePackage = it) }
            ?: PhoneAppVersionText(version)
        InstallKind.MANUAL -> PhoneAppVersionText(
            version,
            if (updated) R.string.dashboard_app_version_updated_manually else R.string.dashboard_app_version_installed_manually,
        )
        InstallKind.UNKNOWN -> PhoneAppVersionText(version)
    }
}
