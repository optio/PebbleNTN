package com.pebblentn.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pebblentn.app.data.DebugEvent
import com.pebblentn.app.data.DebugHistoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the debug-history list and detail screens. */
class DebugHistoryViewModel(
    private val repository: DebugHistoryRepository,
) : ViewModel() {

    val events: StateFlow<List<DebugEvent>> = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Load a single event's detail (returns null if it was deleted). */
    suspend fun detail(id: Long): DebugEvent? = repository.getById(id)

    /** Returns the launched [Job] so tests can await completion; UI callers ignore it. */
    fun deleteEvent(id: Long): Job = viewModelScope.launch { repository.deleteById(id) }

    fun deleteAll(): Job = viewModelScope.launch { repository.deleteAll() }
}
