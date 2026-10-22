package com.pebblentn.app.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class UpdateCheckRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var nowMillis = 1_700_000_000_000L

    private class FakeFetcher(var result: String?) : ReleaseFetcher {
        var calls = 0
        override suspend fun latestVersion(): String? {
            calls++
            return result
        }
    }

    private fun repo(fetcher: ReleaseFetcher, current: String = "0.0.13") =
        UpdateCheckRepository(context, current, fetcher, now = { nowMillis })

    @Test
    fun forcedCheckDetectsNewerVersion() = runTest {
        val fetcher = FakeFetcher("v0.0.14")
        val repo = repo(fetcher)
        assertEquals(UpdateCheckOutcome.UPDATE_AVAILABLE, repo.checkForUpdate(force = true))
        assertTrue(repo.state.value.updateAvailable)
        assertEquals("v0.0.14", repo.state.value.latestVersion)
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun upToDateWhenLatestIsNotNewer() = runTest {
        val repo = repo(FakeFetcher("0.0.13"))
        assertEquals(UpdateCheckOutcome.UP_TO_DATE, repo.checkForUpdate(force = true))
        assertFalse(repo.state.value.updateAvailable)
    }

    @Test
    fun autoCheckSkippedWhenCheckedWithinAWeek() = runTest {
        val fetcher = FakeFetcher("0.0.14")
        val repo = repo(fetcher)
        repo.checkForUpdate(force = true) // records lastCheck = now
        nowMillis += TimeUnit.DAYS.toMillis(3)
        assertEquals(UpdateCheckOutcome.SKIPPED_NOT_DUE, repo.checkForUpdate(force = false))
        assertEquals(1, fetcher.calls) // no second network hit
    }

    @Test
    fun autoCheckRunsAfterAWeek() = runTest {
        val fetcher = FakeFetcher("0.0.14")
        val repo = repo(fetcher)
        repo.checkForUpdate(force = true)
        nowMillis += TimeUnit.DAYS.toMillis(8)
        assertEquals(UpdateCheckOutcome.UPDATE_AVAILABLE, repo.checkForUpdate(force = false))
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun failedCheckRetainsLastKnownVersion() = runTest {
        val fetcher = FakeFetcher("0.0.14")
        val repo = repo(fetcher)
        repo.checkForUpdate(force = true) // learns 0.0.14
        fetcher.result = null // go offline
        nowMillis += TimeUnit.DAYS.toMillis(8)
        assertEquals(UpdateCheckOutcome.FAILED, repo.checkForUpdate(force = false))
        assertEquals("0.0.14", repo.state.value.latestVersion) // retained
        assertTrue(repo.state.value.updateAvailable)
    }

    @Test
    fun autoCheckIsOffByDefaultAndPersistsWhenToggled() = runTest {
        val repo = repo(FakeFetcher("0.0.14"))
        assertFalse("auto-check must default off", repo.autoCheckEnabled.value)

        repo.setAutoCheckEnabled(true)
        assertTrue(repo.autoCheckEnabled.value)

        // A fresh instance over the same prefs reflects the stored choice.
        assertTrue(repo(FakeFetcher(null)).autoCheckEnabled.value)
    }

    // REQ-ANDROID-018 / #94 (FR7.1): a check is stale after 30 days, or when one never ran.

    @Test
    fun neverCheckedIsStale() {
        val repo = repo(FakeFetcher("0.0.14"))
        assertEquals(0L, repo.lastCheckMillis.value)
        assertTrue(repo.isCheckStale())
    }

    @Test
    fun staleOnlyAfterMoreThanThirtyDays() = runTest {
        val repo = repo(FakeFetcher("0.0.14"))
        repo.checkForUpdate(force = true)
        val checkedAt = nowMillis
        assertFalse("exactly 30 days is not stale", repo.isCheckStale(checkedAt + TimeUnit.DAYS.toMillis(30)))
        assertTrue("30 days + 1 ms is stale", repo.isCheckStale(checkedAt + TimeUnit.DAYS.toMillis(30) + 1))
    }

    @Test
    fun forcedCheckClearsStalenessAndRecordsItsTime() = runTest {
        val fetcher = FakeFetcher("0.0.14")
        val repo = repo(fetcher)
        nowMillis += TimeUnit.DAYS.toMillis(40)
        assertEquals(UpdateCheckOutcome.UPDATE_AVAILABLE, repo.checkForUpdate(force = true))
        assertEquals(nowMillis, repo.lastCheckMillis.value)
        assertFalse(repo.isCheckStale())
        assertEquals("the stale check itself makes no network call", 1, fetcher.calls)
        // A fresh instance over the same prefs sees the same time, so it is not stale either.
        assertEquals(nowMillis, repo(FakeFetcher(null)).lastCheckMillis.value)
    }

    @Test
    fun failedCheckAlsoClearsStaleness() = runTest {
        val repo = repo(FakeFetcher(null))
        assertTrue(repo.isCheckStale())
        assertEquals(UpdateCheckOutcome.FAILED, repo.checkForUpdate(force = true))
        assertEquals(nowMillis, repo.lastCheckMillis.value)
        assertFalse(repo.isCheckStale())
    }

    @Test
    fun skippedCheckDoesNotMoveTheLastCheckTime() = runTest {
        val repo = repo(FakeFetcher("0.0.14"))
        repo.checkForUpdate(force = true)
        val checkedAt = nowMillis
        nowMillis += TimeUnit.DAYS.toMillis(3)
        assertEquals(UpdateCheckOutcome.SKIPPED_NOT_DUE, repo.checkForUpdate(force = false))
        assertEquals(checkedAt, repo.lastCheckMillis.value)
    }

    @Test
    fun latestVersionPersistsAcrossInstances() = runTest {
        repo(FakeFetcher("0.0.14")).checkForUpdate(force = true)
        // A fresh repository over the same prefs surfaces the stored latest without a new fetch.
        val fresh = repo(FakeFetcher(null))
        assertEquals("0.0.14", fresh.state.value.latestVersion)
        assertTrue(fresh.state.value.updateAvailable)
    }
}
