package com.pebblentn.app.ui

import com.pebblentn.app.system.NotificationAccess
import com.pebblentn.app.ui.onboarding.OnboardingViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingViewModelTest {

    /** Fake access whose result can flip between reads. */
    private class FakeAccess(var granted: Boolean) : NotificationAccess {
        override fun isGranted(): Boolean = granted
    }

    @Test
    fun initialStateReflectsAccessAtCreation() {
        val vm = OnboardingViewModel(FakeAccess(granted = true))
        assertTrue(vm.uiState.value.accessGranted)
    }

    @Test
    fun initialStateDeniedWhenNotGranted() {
        val vm = OnboardingViewModel(FakeAccess(granted = false))
        assertFalse(vm.uiState.value.accessGranted)
    }

    @Test
    fun refreshPicksUpNewlyGrantedAccess() {
        val access = FakeAccess(granted = false)
        val vm = OnboardingViewModel(access)
        assertFalse(vm.uiState.value.accessGranted)

        access.granted = true
        vm.refresh()
        assertTrue(vm.uiState.value.accessGranted)
    }

    @Test
    fun refreshPicksUpRevokedAccess() {
        val access = FakeAccess(granted = true)
        val vm = OnboardingViewModel(access)
        access.granted = false
        vm.refresh()
        assertFalse(vm.uiState.value.accessGranted)
    }
}
