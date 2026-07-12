package com.pebblentn.app.di

import android.content.Context
import android.content.pm.PackageManager

/**
 * Reports which of a set of candidate packages are installed. Uses targeted package queries (the
 * catalog packages are declared in the manifest `<queries>`), not QUERY_ALL_PACKAGES.
 */
class InstalledAppsProvider(private val context: Context) {

    fun installedPackages(candidates: Set<String>): Set<String> {
        val pm = context.packageManager
        return candidates.filterTo(mutableSetOf()) { pkg -> isInstalled(pm, pkg) }
    }

    private fun isInstalled(pm: PackageManager, packageName: String): Boolean =
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
