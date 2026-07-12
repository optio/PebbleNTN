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

    /** Maximum UTF-16 length for text fields sent to the watch; longer strings are truncated. */
    const val MAX_TEXT_CHARS = 64

    fun encodeState(state: NavigationState, flags: Int, appVersion: String): AppMessage {
        val builder = AppMessage.builder()
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .putString(Protocol.Keys.APP_VERSION, appVersion)

        when (state) {
            is NavigationState.Navigating -> {
                val i = state.instruction
                builder
                    .putInt(Protocol.Keys.EVENT, Protocol.Events.NAVIGATION_UPDATE)
                    .putInt(Protocol.Keys.MANEUVER, i.maneuver.code)
                    .putInt(Protocol.Keys.DISTANCE_METERS, i.distanceMeters)
                    .putString(Protocol.Keys.PRIMARY_TEXT, limitText(i.primaryText))
                    .putString(Protocol.Keys.SECONDARY_TEXT, limitText(i.secondaryText))
                    .putInt(Protocol.Keys.ETA_EPOCH_SECONDS, i.etaEpochSeconds?.let(::toInt32))
                    .putInt(Protocol.Keys.STATE_TIMESTAMP_SECONDS, toInt32(state.stateTimestampSeconds))
                    .putInt(Protocol.Keys.SESSION_ID, state.sessionId)
                    .putInt(Protocol.Keys.FLAGS, flags)
            }

            is NavigationState.Stopped ->
                builder
                    .putInt(Protocol.Keys.EVENT, Protocol.Events.NAVIGATION_STOPPED)
                    .putInt(Protocol.Keys.SESSION_ID, state.sessionId)
                    .putInt(Protocol.Keys.FLAGS, flags)

            NavigationState.NoActiveNavigation ->
                builder
                    .putInt(Protocol.Keys.EVENT, Protocol.Events.NO_ACTIVE_NAVIGATION)
                    .putInt(Protocol.Keys.FLAGS, flags)
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

    private fun limitText(text: String?): String? {
        if (text == null || text.length <= MAX_TEXT_CHARS) return text
        var end = MAX_TEXT_CHARS
        // Do not split a surrogate pair at the cut point.
        if (Character.isHighSurrogate(text[end - 1])) end -= 1
        return text.substring(0, end)
    }

    /** Clamp a Long (epoch/timestamp seconds) into the int32 wire range instead of overflowing. */
    private fun toInt32(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
