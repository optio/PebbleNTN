package com.pebblentn.app.protocol

import com.pebblentn.app.core.WatchCapabilities
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsModel

/** Watch messages as a watchapp sends them, for the settings sync tests (#102). */
object WatchMessages {

    /** Colour, RGB backlight and touch: an emery (Pebble Time 2). */
    val EMERY_CAPS = WatchCapabilities(
        Protocol.CapabilityMasks.COLOR_MASK or Protocol.CapabilityMasks.RGB_BACKLIGHT_MASK or
            Protocol.CapabilityMasks.TOUCH_MASK,
    )

    /** A 1.3 watchapp's settings with [overrides] on top of the defaults. */
    fun settings(
        revision: Int,
        overrides: Map<WatchSetting, Int> = emptyMap(),
        platform: WatchPlatform = WatchPlatform.EMERY,
        caps: WatchCapabilities = EMERY_CAPS,
    ): WatchSettingsModel {
        val defaults = WatchSettingsModel.defaults(platform, caps, revision)
        return defaults.copy(values = defaults.values + overrides)
    }

    /** A genuine WATCH_READY from a 1.3 watchapp, carrying [model]. */
    fun ready13(model: WatchSettingsModel, appVersion: String = "0.0.77"): AppMessage =
        withSettings(Protocol.Events.WATCH_READY, model)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, 1)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, 3)
            .putString(Protocol.Keys.APP_VERSION, appVersion)
            .build()

    /** A SETTINGS message from the watch (after an edit, or as the reply to the phone's). */
    fun settingsMessage(model: WatchSettingsModel): AppMessage =
        withSettings(Protocol.Events.SETTINGS, model).build()

    /** A genuine WATCH_READY from a 1.2 watchapp: no settings at all. */
    fun ready12(appVersion: String = "0.0.76"): AppMessage = AppMessage.builder()
        .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
        .putInt(Protocol.Keys.PROTOCOL_MAJOR, 1)
        .putInt(Protocol.Keys.PROTOCOL_MINOR, 2)
        .putString(Protocol.Keys.APP_VERSION, appVersion)
        .build()

    private fun withSettings(event: Int, model: WatchSettingsModel): AppMessage.Builder {
        val builder = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, event)
            .putInt(Protocol.Keys.SETTINGS_REVISION, model.revision)
            .putInt(Protocol.Keys.WATCH_PLATFORM, model.platform.code)
            .putInt(Protocol.Keys.WATCH_CAPABILITIES, model.capabilities.mask)
        for (setting in WatchSetting.entries) builder.putInt(setting.key, model[setting])
        return builder
    }
}
