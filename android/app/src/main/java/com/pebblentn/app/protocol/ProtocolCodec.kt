package com.pebblentn.app.protocol

import com.pebblentn.app.core.NavigationState
import com.pebblentn.app.core.ReducerEvent

/**
 * Translates between the normalized domain state and AppMessage payloads, using the generated
 * [Protocol] constants as the single key/event vocabulary. Pure and deterministic.
 *
 * Outbound: [encodeState] / [encodeCompatibilityError] build the dictionary the phone sends.
 * Inbound: [decodeInbound] turns a watch AppMessage into a [ReducerEvent] (or null if irrelevant).
 */
object ProtocolCodec {

    /**
     * The watch's text buffers, in UTF-8 bytes (`PRIMARY_TEXT_MAX` / `SECONDARY_TEXT_MAX` in
     * watchapp/src/c/main.c). Longer text is shortened here, on whole characters, because the watch
     * copies bytes and would cut an "ä" or "ß" in half (#76).
     */
    const val MAX_PRIMARY_BYTES = 64
    const val MAX_SECONDARY_BYTES = 24

    /**
     * Encode a state message. [latestPhoneVersion] is the newer phone app version when an update is
     * available, else null (REQ-ANDROID-017, #94): when set, every state message carries
     * [Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK] in FLAGS and the version, without a leading
     * `v`, as LATEST_PHONE_VERSION. When null both are left out and [flags] is sent as given.
     */
    fun encodeState(
        state: NavigationState,
        flags: Int,
        appVersion: String,
        latestPhoneVersion: String? = null,
    ): AppMessage {
        val update = latestPhoneVersion?.trim()?.removePrefix("v")?.takeIf { it.isNotEmpty() }
        val wireFlags = if (update != null) flags or Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK else flags
        val builder = AppMessage.builder()
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .putString(Protocol.Keys.APP_VERSION, appVersion)
            .putString(Protocol.Keys.LATEST_PHONE_VERSION, update)

        when (state) {
            is NavigationState.Navigating -> {
                val i = state.instruction
                builder
                    .putInt(Protocol.Keys.EVENT, Protocol.Events.NAVIGATION_UPDATE)
                    .putInt(Protocol.Keys.MANEUVER, i.maneuver.code)
                    .putInt(Protocol.Keys.DISTANCE_METERS, i.distanceMeters)
                    .putString(Protocol.Keys.PRIMARY_TEXT, fitText(i.primaryText, MAX_PRIMARY_BYTES))
                    .putString(Protocol.Keys.SECONDARY_TEXT, fitText(i.secondaryText, MAX_SECONDARY_BYTES))
                    .putInt(Protocol.Keys.STOPS_REMAINING, i.stopsRemaining)
                    .putInt(Protocol.Keys.ETA_EPOCH_SECONDS, i.etaEpochSeconds?.let(::toInt32))
                    .putInt(Protocol.Keys.STATE_TIMESTAMP_SECONDS, toInt32(state.stateTimestampSeconds))
                    .putInt(Protocol.Keys.SESSION_ID, state.sessionId)
                    .putInt(Protocol.Keys.FLAGS, wireFlags)
            }

            is NavigationState.Stopped ->
                builder
                    .putInt(Protocol.Keys.EVENT, Protocol.Events.NAVIGATION_STOPPED)
                    .putInt(Protocol.Keys.SESSION_ID, state.sessionId)
                    .putInt(Protocol.Keys.FLAGS, wireFlags)

            NavigationState.NoActiveNavigation ->
                builder
                    .putInt(Protocol.Keys.EVENT, Protocol.Events.NO_ACTIVE_NAVIGATION)
                    .putInt(Protocol.Keys.FLAGS, wireFlags)
        }
        return builder.build()
    }

    fun encodeCompatibilityError(errorCode: Int): AppMessage =
        AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.PHONE_COMPATIBILITY_ERROR)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .putInt(Protocol.Keys.ERROR_CODE, errorCode)
            .build()

    /**
     * Decode an inbound AppMessage from the watch into a reducer event, or null when the message is
     * not one the phone acts on. [atEpochSeconds] stamps the resulting event.
     */
    fun decodeInbound(message: AppMessage, atEpochSeconds: Long): ReducerEvent? {
        return when (message.intOrNull(Protocol.Keys.EVENT)) {
            Protocol.Events.WATCH_READY -> ReducerEvent.WatchReady(
                protocolMajor = message.intOrNull(Protocol.Keys.PROTOCOL_MAJOR) ?: 0,
                protocolMinor = message.intOrNull(Protocol.Keys.PROTOCOL_MINOR) ?: 0,
                atEpochSeconds = atEpochSeconds,
            )

            Protocol.Events.WATCH_REQUEST_STATE ->
                ReducerEvent.WatchRequestedState(atEpochSeconds)

            else -> null
        }
    }

    /**
     * The watchapp version a WATCH_READY carries as APP_VERSION (key 11), or null when [message] is
     * another event or has no non-blank version (REQ-ANDROID-019). The phone uses it to spot a newer
     * watchapp, which means a newer phone app has been published. No protocol change: the watch has
     * always sent key 11 in WATCH_READY.
     */
    fun watchAppVersion(message: AppMessage): String? {
        if (message.intOrNull(Protocol.Keys.EVENT) != Protocol.Events.WATCH_READY) return null
        return message.stringOrNull(Protocol.Keys.APP_VERSION)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * [text] within [maxBytes] of UTF-8. Too long: cut on a whole character, back to the last word
     * boundary when one is in the second half (a road ends on a word, not "Bundesstr"), trailing
     * separators dropped, and "…" appended so the cut shows.
     */
    fun fitText(text: String?, maxBytes: Int): String? {
        if (text == null || utf8Length(text) <= maxBytes) return text
        val budget = maxBytes - utf8Length(ELLIPSIS)
        var end = 0
        var bytes = 0
        while (end < text.length) {
            val cp = text.codePointAt(end)
            val size = utf8Length(cp)
            if (bytes + size > budget) break
            bytes += size
            end += Character.charCount(cp)
        }
        var cut = text.substring(0, end)
        val space = cut.lastIndexOf(' ')
        if (space >= cut.length / 2) cut = cut.substring(0, space)
        return cut.trimEnd(' ', ',', ';', ':', '-', '–', '/', '(') + ELLIPSIS
    }

    private fun utf8Length(s: String): Int {
        var n = 0
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            n += utf8Length(cp)
            i += Character.charCount(cp)
        }
        return n
    }

    private fun utf8Length(cp: Int): Int = when {
        cp < 0x80 -> 1
        cp < 0x800 -> 2
        cp < 0x10000 -> 3
        else -> 4
    }

    private const val ELLIPSIS = "…"

    /** Clamp a Long (epoch/timestamp seconds) into the int32 wire range instead of overflowing. */
    private fun toInt32(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
