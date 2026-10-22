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

    // REQ-ANDROID-019 (FR3): the watchapp version from a genuine WATCH_READY reveals an update with
    // no network call.

    @Test
    fun newerWatchVersionIsAnUpdateWithoutAFetch() {
        val fetcher = FakeFetcher("0.0.99")
        val repo = repo(fetcher, current = "0.0.71")
        assertTrue("first sighting is written", repo.recordWatchAppVersion("0.0.72"))
        assertTrue(repo.state.value.updateAvailable)
        assertEquals("0.0.72", repo.state.value.effectiveLatest)
        assertEquals("no network call", 0, fetcher.calls)
        // A fresh instance over the same prefs still knows it.
        assertEquals("0.0.72", repo(FakeFetcher(null), current = "0.0.71").state.value.effectiveLatest)
    }

    @Test
    fun sameWatchVersionAgainIsNotRewritten() {
        val repo = repo(FakeFetcher(null), current = "0.0.71")
        assertTrue(repo.recordWatchAppVersion("0.0.72"))
        assertFalse("same value again: no write", repo.recordWatchAppVersion("0.0.72"))
        assertFalse("blank is no version", repo.recordWatchAppVersion(" "))
        assertEquals("0.0.72", repo.state.value.watchAppVersion)
    }

    @Test
    fun unparsableOrOlderWatchVersionIsNoUpdate() {
        val repo = repo(FakeFetcher(null), current = "0.0.71")
        repo.recordWatchAppVersion("0.0.72-debug")
        assertFalse(repo.state.value.updateAvailable)
        assertEquals(null, repo.state.value.effectiveLatest)
        repo.recordWatchAppVersion("0.0.71")
        assertFalse("the same version is not newer", repo.state.value.updateAvailable)
    }

    @Test
    fun higherGitHubVersionWinsOverTheWatch() = runTest {
        val repo = repo(FakeFetcher("v0.0.73"), current = "0.0.71")
        repo.recordWatchAppVersion("0.0.72")
        repo.checkForUpdate(force = true)
        assertEquals("v0.0.73", repo.state.value.effectiveLatest)
        assertEquals("the check keeps the watch version", "0.0.72", repo.state.value.watchAppVersion)
    }

    @Test
    fun higherWatchVersionWinsOverGitHub() = runTest {
        val repo = repo(FakeFetcher("0.0.72"), current = "0.0.71")
        repo.checkForUpdate(force = true)
        repo.recordWatchAppVersion("0.0.74")
        assertTrue(repo.state.value.updateAvailable)
        assertEquals("0.0.74", repo.state.value.effectiveLatest)
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
