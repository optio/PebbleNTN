package com.pebblentn.app.ui.onboarding

import androidx.lifecycle.ViewModel
import com.pebblentn.app.system.NotificationAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Immutable UI state for onboarding / access gating. */
data class OnboardingUiState(val accessGranted: Boolean = false)

/**
 * Drives onboarding: exposes whether notification access is granted and re-checks on demand
 * (e.g. when the activity resumes after the user returns from system settings).
 */
class OnboardingViewModel(
    private val notificationAccess: NotificationAccess,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(notificationAccess.isGranted()))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Re-read the current access state. */
    fun refresh() {
        _uiState.value = OnboardingUiState(accessGranted = notificationAccess.isGranted())
    }
}
