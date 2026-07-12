package com.pebblentn.app.di

import android.content.Context
import com.pebblentn.app.catalog.NavigationAppCatalog

/** Loads the bundled navigation-app catalog from app assets. */
class AssetCatalogSource(private val context: Context) {

    fun load(): NavigationAppCatalog {
        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        return NavigationAppCatalog.parse(json)
    }

    private companion object {
        const val ASSET_PATH = "catalog/navigation-apps.json"
    }
}
