package com.pebblentn.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The optional Pebble monochrome theme (spec/400-ui: "system dynamic color optional, with a Pebble
 * monochrome theme available"; #28). Off by default, so the app follows the system's colours.
 */
class AppearanceRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _monochrome = MutableStateFlow(prefs.getBoolean(KEY_MONOCHROME, false))
    val monochrome: StateFlow<Boolean> = _monochrome.asStateFlow()

    fun setMonochrome(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONOCHROME, enabled).apply()
        _monochrome.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "pebblentn_settings"
        const val KEY_MONOCHROME = "monochrome_theme"
    }
}
