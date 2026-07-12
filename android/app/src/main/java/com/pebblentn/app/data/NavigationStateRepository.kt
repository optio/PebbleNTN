package com.pebblentn.app.data

import com.pebblentn.app.core.NavigationState
import com.pebblentn.app.core.ReducerState
import com.pebblentn.app.data.db.NavigationStateDao
import com.pebblentn.app.data.db.NavigationStateEntity
import kotlinx.serialization.json.Json

/**
 * Persists and restores the current navigation state for safe process restoration
 * (REQ-ANDROID-010). Only the normalized state + session bookkeeping are stored; watch readiness is
 * intentionally not restored (the watch re-handshakes), and nothing is ever replayed as a queue.
 */
class NavigationStateRepository(
    private val dao: NavigationStateDao,
    private val json: Json = Json,
) {
    suspend fun save(state: ReducerState) {
        val current = state.current
        val entity = NavigationStateEntity(
            sessionId = when (current) {
                is NavigationState.Navigating -> current.sessionId
                is NavigationState.Stopped -> current.sessionId
                NavigationState.NoActiveNavigation -> null
            },
            active = current is NavigationState.Navigating,
            normalizedStateJson = json.encodeToString(NavigationState.serializer(), current),
            stateTimestampSeconds = (current as? NavigationState.Navigating)?.stateTimestampSeconds ?: 0,
            nextSessionId = state.nextSessionId,
            launchedSessionId = state.launchedSessionId,
        )
        dao.upsert(entity)
    }

    /**
     * Reconstruct a [ReducerState] from the cached row, or null if none. Watch readiness resets to
     * false and the last-sent instruction is cleared so the reconnecting watch is re-synced.
     */
    suspend fun loadReducerState(): ReducerState? {
        val entity = dao.get() ?: return null
        val current = runCatching {
            json.decodeFromString(NavigationState.serializer(), entity.normalizedStateJson)
        }.getOrElse { return null }
        return ReducerState(
            current = current,
            nextSessionId = entity.nextSessionId,
            launchedSessionId = entity.launchedSessionId,
        )
    }
}
