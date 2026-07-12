package com.pebblentn.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton row caching the current navigation state for process restoration (`navigation_state`,
 * spec/300-data/Database.md, REQ-ANDROID-010). Exactly one row (id = 0). Stores the normalized
 * state — never a queue of events.
 */
@Entity(tableName = "navigation_state")
data class NavigationStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val sessionId: Int?,
    val active: Boolean,
    val normalizedStateJson: String,
    val stateTimestampSeconds: Long,
    val nextSessionId: Int,
    val launchedSessionId: Int?,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
