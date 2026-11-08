package com.pebblentn.app.pebble

import com.pebblentn.app.core.DrivingSpeedEstimator
import com.pebblentn.app.core.DrivingSpeeds
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.SpeedSample
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettings
import com.pebblentn.app.core.WatchSettingsSyncState
import com.pebblentn.app.data.DrivingDecision
import com.pebblentn.app.data.DrivingLogRepository
import com.pebblentn.app.data.InMemoryWatchSettingsStore
import com.pebblentn.app.protocol.FakeWatchTransport
import com.pebblentn.app.protocol.WatchMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * "When driving" (#132) on real Google Maps notifications: the speed fields Google Maps posted on
 * the Android emulator while route-capture moved the location at known speeds (Brussels route, one
 * geo fix per second), replayed through the real [NavigationController] at their own post times.
 * The fixtures in `src/test/resources/driving/` keep only the progress values, the arrival line and
 * the post times; titles and street names were left out.
 *
 * This checks what the unit tests could only assume: that Google Maps' `progressMax − progress` is
 * the remaining distance in metres (the measured speed matches the simulated one), and that the
 * watchapp closes when driving, reopens after slowing down, and stays open on foot and by bike.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DrivingEmulatorReplayTest {

    private data class Post(
        val whenMillis: Long,
        val progress: Int?,
        val progressMax: Int?,
        val subText: String?,
        val rerouting: Boolean,
        val simulatedKmh: Double,
    )

    private data class Fixture(val zone: ZoneId, val posts: List<Post>)

    /** What happened during a replay, for the assertions and the printed timeline. */
    private data class Replay(
        val hideAtSeconds: List<Double>,
        val showAtSeconds: List<Double>,
        val measured: List<Pair<Double, Double>>, // (seconds since start, km/h)
        val closeCount: Int,
        val launchCount: Int,
    )

    private fun load(name: String): Fixture {
        val text = javaClass.classLoader!!.getResource("driving/$name.json")!!.readText()
        val root = Json.parseToJsonElement(text).jsonObject
        val posts = root["posts"]!!.jsonArray.map { element ->
            val o: JsonObject = element.jsonObject
            Post(
                whenMillis = o["when"]!!.jsonPrimitive.long,
                progress = o["progress"]!!.jsonPrimitive.intOrNull,
                progressMax = o["progressMax"]!!.jsonPrimitive.intOrNull,
                subText = o["subText"]!!.jsonPrimitive.contentOrNull,
                rerouting = o["rerouting"]!!.jsonPrimitive.boolean,
                simulatedKmh = o["simulatedKmh"]!!.jsonPrimitive.content.toDouble(),
            )
        }
        return Fixture(ZoneId.of(root["zone"]!!.jsonPrimitive.content), posts)
    }

    private fun TestScope.replay(name: String, extraSeconds: Int = 0): Replay {
        val fixture = load(name)
        val start = fixture.posts.first().whenMillis
        val transport = FakeWatchTransport()
        val log = DrivingLogRepository()
        val store = InMemoryWatchSettingsStore(
            WatchSettingsSyncState(
                mirror = WatchMessages.settings(revision = 1, overrides = mapOf(WatchSetting.DRIVING_MODE to DrivingSpeeds.MODE_HIDE)),
                receivedFromWatch = true,
                watchProtocolMinor = 4,
            ),
        )
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.82",
            clock = { start + testScheduler.currentTime },
            watchSettings = store,
            drivingLog = log,
            initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = true),
            speedEstimator = DrivingSpeedEstimator(zone = { fixture.zone }),
        )
        controller.start()
        runCurrent()

        val hides = mutableListOf<Double>()
        val shows = mutableListOf<Double>()
        val measured = mutableListOf<Pair<Double, Double>>()
        fun seconds() = testScheduler.currentTime / 1000.0
        fun observe(before: Pair<Int, Int>) {
            if (transport.closeCount > before.first) hides += seconds()
            if (transport.launchCount > before.second && before.second > 0) shows += seconds()
        }

        for (post in fixture.posts) {
            val before = transport.closeCount to transport.launchCount
            testScheduler.advanceTimeBy(post.whenMillis - start - testScheduler.currentTime)
            runCurrent()
            observe(before)
            val before2 = transport.closeCount to transport.launchCount
            kotlinx.coroutines.runBlocking {
                if (!post.rerouting) {
                    controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 200))
                }
                controller.onSpeedSample(
                    SpeedSample(
                        postTimeMillis = post.whenMillis,
                        progress = post.progress,
                        progressMax = post.progressMax,
                        subText = post.subText,
                        packageName = DrivingSpeedEstimator.GOOGLE_MAPS_PACKAGE,
                        hasInstruction = !post.rerouting,
                    ),
                )
            }
            runCurrent()
            observe(before2)
            controller.drivingStatus.value.measuredKmh?.let { measured += seconds() to it }
        }
        repeat(extraSeconds / 10) {
            val before = transport.closeCount to transport.launchCount
            testScheduler.advanceTimeBy(10_000)
            runCurrent()
            observe(before)
        }
        val samples = log.snapshot().filter { it.decision == DrivingDecision.SAMPLE && it.measuredKmh != null }
        samples.forEach { measured += ((it.timeMillis - start) / 1000.0) to it.measuredKmh!! }
        println("[$name] hides at $hides s, shows at $shows s; closes=${transport.closeCount}, launches=${transport.launchCount}")
        samples.filterIndexed { i, _ -> i % 10 == 0 }.forEach {
            println("[$name]   t=%.0f s measured=%.1f km/h planned=%s km/h".format(
                (it.timeMillis - start) / 1000.0, it.measuredKmh, it.plannedKmh?.let { p -> "%.1f".format(p) }))
        }
        return Replay(hides, shows, measured.sortedBy { it.first }, transport.closeCount, transport.launchCount)
    }

    private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

    @Test
    fun carAt45KmhIsMeasuredInMetresClosesTheWatchappThenReopensAfterSlowingDown() = runTest {
        val r = replay("google-maps-car-then-slow")

        // Metres: the simulated 45 km/h leg (0–150 s) measures as about 45 km/h.
        val drivingLeg = r.measured.filter { it.first in 60.0..150.0 }.map { it.second }
        assertEquals("measured speed while driving at a simulated 45 km/h", 45.0, median(drivingLeg), 6.0)

        assertEquals("closed once while driving", 1, r.closeCount)
        assertTrue("closed 30–90 s into the drive, at ${r.hideAtSeconds}", r.hideAtSeconds.single() in 30.0..90.0)

        // Slowed to about 3 km/h at 150 s: reopened about 2 minutes later.
        assertEquals("reopened once after slowing down", 1, r.showAtSeconds.size)
        assertTrue("reopened 2–3.5 min after slowing at 150 s, at ${r.showAtSeconds}", r.showAtSeconds.single() in 270.0..360.0)
        assertEquals("launched at the start and once to reopen", 2, r.launchCount)
    }

    @Test
    fun bikeAt18KmhIsMeasuredAndNeverClosesTheWatchapp() = runTest {
        val r = replay("google-maps-bike")
        val legSpeeds = r.measured.filter { it.first >= 45.0 }.map { it.second }
        assertEquals("measured speed by bike at a simulated 18 km/h", 18.0, median(legSpeeds), 5.0)
        assertEquals("never closed by bike", 0, r.closeCount)
        assertEquals("launched once at the start", 1, r.launchCount)
    }

    @Test
    fun walkingAt5KmhNeverClosesTheWatchapp() = runTest {
        val r = replay("google-maps-foot")
        assertTrue("measured walking speeds stay far below 25 km/h", r.measured.all { it.second < 10.0 })
        assertEquals("never closed on foot", 0, r.closeCount)
        assertEquals("launched once at the start", 1, r.launchCount)
    }
}
