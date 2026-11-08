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
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.protocol.WatchMessages
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.FakeWatchTransport
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RulesetCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationControllerTest {

    private fun controller(transport: FakeWatchTransport, scope: kotlinx.coroutines.CoroutineScope) =
        NavigationController(transport, scope, appVersion = "0.0.1", clock = { 0 })

    private fun events(transport: FakeWatchTransport) =
        transport.sent.mapNotNull { it.intOrNull(Protocol.Keys.EVENT) }

    private fun readyMessage() = AppMessage.builder()
        .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
        .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
        .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
        .build()

    @Test
    fun instructionBeforeReadyLaunchesOnceAndDefersSend() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()

        assertEquals(1, transport.launchCount)
        assertTrue("no state sent before READY", transport.sent.isEmpty())
    }

    @Test
    fun launchIsFollowedByAutonomousReadyWhenNoInboundHandshakeArrives() = runTest {
        // Mimic a companion where the watch's WATCH_READY does not reach us (as the Core Devices
        // Pebble app did before `companionApp` was declared; forwarding was confirmed in v0.0.74):
        // no start(), no inbound READY. The controller must still send state on its own after the
        // settle delay, so the watch does not sit on "Connecting" forever.
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()
        assertEquals(1, transport.launchCount)
        assertTrue("nothing sent before the settle delay", transport.sent.isEmpty())

        testScheduler.advanceTimeBy(1_600) // pass the ~1.5s settle window
        runCurrent()

        assertTrue(
            "state is sent autonomously after launch, without an inbound READY",
            events(transport).contains(Protocol.Events.NAVIGATION_UPDATE),
        )
    }

    @Test
    fun watchReadySendsCurrentState() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent()

        assertTrue(events(transport).contains(Protocol.Events.NAVIGATION_UPDATE))
    }

    @Test
    fun stopSendsStoppedWithExitFlag() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent()
        controller.onNavigationStopped()
        runCurrent()

        val stopped = transport.sent.last()
        assertEquals(Protocol.Events.NAVIGATION_STOPPED, stopped.intOrNull(Protocol.Keys.EVENT))
        assertTrue(
            (stopped.intOrNull(Protocol.Keys.FLAGS) ?: 0) and Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK != 0,
        )
    }

    @Test
    fun everySendReadsTheLatestPhoneUpdate() = runTest {
        // REQ-ANDROID-017 / #94: the provider is read on each send, so a check that finds an update
        // reaches the watch with the next state message.
        var latest: String? = null
        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport, backgroundScope, appVersion = "0.0.1", clock = { 0 }, latestPhoneUpdate = { latest },
        )
        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent()
        assertEquals(null, transport.sent.last().stringOrNull(Protocol.Keys.LATEST_PHONE_VERSION))

        latest = "v0.0.71"
        controller.onNavigationStopped()
        runCurrent()
        val stopped = transport.sent.last()
        assertEquals("0.0.71", stopped.stringOrNull(Protocol.Keys.LATEST_PHONE_VERSION))
        assertTrue((stopped.intOrNull(Protocol.Keys.FLAGS) ?: 0) and Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK != 0)
    }

    @Test
    fun genuineWatchReadyReportsTheWatchappVersion() = runTest {
        // REQ-ANDROID-019 (FR3.1): key 11 of a WATCH_READY the watch sent reaches the callback.
        val versions = mutableListOf<String>()
        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport, backgroundScope, appVersion = "0.0.71", clock = { 0 }, onWatchAppVersion = { versions += it },
        )
        controller.start()
        runCurrent()

        transport.emitInbound(
            AppMessage.builder()
                .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
                .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
                .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
                .putString(Protocol.Keys.APP_VERSION, "0.0.72")
                .build(),
        )
        runCurrent()

        assertEquals(listOf("0.0.72"), versions)
    }

    @Test
    fun madeUpReadinessNeverReportsAWatchappVersion() = runTest {
        // REQ-ANDROID-019 (FR3.2): the timed READY after a launch and onAppOpened's READY carry no
        // version, so they must not touch the stored one.
        val versions = mutableListOf<String>()
        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport, backgroundScope, appVersion = "0.0.71", clock = { 0 }, onWatchAppVersion = { versions += it },
        )
        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        testScheduler.advanceTimeBy(1_600) // the autonomous READY fires
        runCurrent()
        transport.emitInbound(WatchListenerService.appOpenedReadyMessage())
        runCurrent()

        assertTrue("state was sent, so readiness was handled", events(transport).contains(Protocol.Events.NAVIGATION_UPDATE))
        assertTrue("no version reported", versions.isEmpty())
    }

    @Test
    fun failedSendIsRetriedThenSucceeds() = runTest {
        // READY triggers a send; script the first send to fail, second to succeed.
        val transport = FakeWatchTransport(listOf(SendResult.FAILED, SendResult.SENT))
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent() // process READY: first send fails and schedules a backoff retry
        testScheduler.advanceTimeBy(1_000) // advance the backoff window
        runCurrent() // run the retry send

        // The same state was sent twice (one failed attempt + one success).
        assertEquals(2, transport.sent.size)
    }

    @Test
    fun incompatibleWatchGetsCompatibilityError() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        val incompatible = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR + 1)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .build()
        transport.emitInbound(incompatible)
        runCurrent()

        assertTrue(events(transport).contains(Protocol.Events.PHONE_COMPATIBILITY_ERROR))
    }

    /**
     * A transport whose inbound stream fails (e.g. the platform rejects the Pebble broadcast
     * receiver) must not take the app down: outbound navigation must keep working.
     */
    @Test
    fun failingInboundStreamDoesNotKillTheController() = runTest {
        val transport = object : FakeWatchTransport() {
            override val inbound = kotlinx.coroutines.flow.flow<AppMessage> {
                throw SecurityException("receiver rejected")
            }
        }
        val controller = controller(transport, backgroundScope)

        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()

        assertEquals("watch app still launched", 1, transport.launchCount)
    }

    @Test
    fun instructionWithAutoLaunchDisabledDoesNotLaunchOrScheduleAutonomousReady() = runTest {
        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.1",
            clock = { 0 },
            initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false),
        )

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()

        assertEquals("launch count must be 0 when auto-launch is disabled", 0, transport.launchCount)
        assertTrue("nothing sent before READY", transport.sent.isEmpty())

        testScheduler.advanceTimeBy(3_000)
        runCurrent()

        assertTrue("autonomous ready must NOT be scheduled when auto-launch is disabled", transport.sent.isEmpty())
    }

    @Test
    fun autoLaunchDisabledThenManualWatchReadySendsState() = runTest {
        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.1",
            clock = { 0 },
            initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false),
        )
        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()
        assertEquals(0, transport.launchCount)

        // Simulate user manually opening the watchapp on the watch
        transport.emitInbound(readyMessage())
        runCurrent()

        assertTrue("state is sent after manual WATCH_READY", events(transport).contains(Protocol.Events.NAVIGATION_UPDATE))
    }

    @Test
    fun autoLaunchDisabledThenWatchappOpenedByHandSendsState() = runTest {
        // Fallback case: the watch's own WATCH_READY does not reach us (forwarding by the Core Devices
        // Pebble app was confirmed in v0.0.74 with `companionApp` declared, but this stays covered),
        // and with auto-launch off there is no launch and so no autonomous READY. The readiness
        // signal left is WatchListenerService.onAppOpened; its synthetic READY must unblock the send.
        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.1",
            clock = { 0 },
            initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false),
        )
        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 450))
        testScheduler.advanceTimeBy(3_000)
        runCurrent()
        assertEquals("not launched", 0, transport.launchCount)
        assertTrue("nothing sent while the watchapp is closed", transport.sent.isEmpty())

        transport.emitInbound(WatchListenerService.appOpenedReadyMessage())
        runCurrent()

        assertEquals(listOf(Protocol.Events.NAVIGATION_UPDATE), events(transport))
        assertEquals(0, transport.launchCount)
    }

    @Test
    fun toggleAutoLaunchViaSettingsChangedUpdatesControllerState() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)

        // Initially autoLaunch is enabled by default; disable it via SettingsChanged
        controller.onSettingsChanged(WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false))
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.LEFT, distanceMeters = 200))
        runCurrent()

        assertEquals("launch count must be 0 after disabling via onSettingsChanged", 0, transport.launchCount)
    }

    @Test
    fun googleMapsNotificationWithAutoLaunchDisabledDoesNotLaunchWatchapp() = runTest {
        val rulesJson = javaClass.getResourceAsStream("/rules/bundled/google-maps/en.json")!!.bufferedReader().readText()
        val rules = LayeredRules(bundled = RulesetCodec.parse(rulesJson).rules)
        val snapshot = NotificationSnapshot(
            packageName = "com.google.android.apps.maps",
            notificationId = 1,
            text = "In 200 m, turn right",
            title = "Main St",
        )
        val eval = RuleEngine().evaluate(snapshot, rules, locale = "en")
        assertTrue("Google Maps notification must match", eval.matched)
        val instruction = eval.instruction!!

        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.1",
            clock = { 0 },
            initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false),
        )

        controller.onInstruction(instruction)
        runCurrent()

        assertEquals("launch count must be 0 for Google Maps notification format when auto-launch disabled", 0, transport.launchCount)
    }

    @Test
    fun osmandNotificationWithAutoLaunchDisabledDoesNotLaunchWatchapp() = runTest {
        val rulesJson = javaClass.getResourceAsStream("/rules/bundled/osmand/en.json")!!.bufferedReader().readText()
        val rules = LayeredRules(bundled = RulesetCodec.parse(rulesJson).rules)
        val snapshot = NotificationSnapshot(
            packageName = "net.osmand",
            notificationId = 1,
            title = "200 m • Turn right",
        )
        val eval = RuleEngine().evaluate(snapshot, rules, locale = "en")
        assertTrue("OsmAnd notification must match", eval.matched)
        val instruction = eval.instruction!!

        val transport = FakeWatchTransport()
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.1",
            clock = { 0 },
            initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false),
        )

        controller.onInstruction(instruction)
        runCurrent()

        assertEquals("launch count must be 0 for OsmAnd notification format when auto-launch disabled", 0, transport.launchCount)
    }

    @Test
    fun statusReportsTheLastInstructionSentAndClearsOnStop() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent()
        assertEquals(WatchLinkStatus(), controller.status.value)

        val turn = NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100, primaryText = "Main St")
        controller.onInstruction(turn)
        transport.emitInbound(readyMessage())
        runCurrent()
        assertEquals(WatchLinkStatus(navigating = true, lastSent = turn), controller.status.value)

        controller.onNavigationStopped()
        runCurrent()
        assertEquals(WatchLinkStatus(navigating = false, lastSent = null), controller.status.value)
    }

    @Test
    fun aSuccessfulSendProvesTheWatchappIsInstalled() = runTest {
        var reached = 0
        val transport = FakeWatchTransport(sendResults = listOf(SendResult.FAILED, SendResult.SENT))
        val controller = NavigationController(
            transport,
            backgroundScope,
            appVersion = "0.0.1",
            clock = { 0 },
            baseBackoffMillis = 1,
            onWatchappReached = { reached++ },
        )
        controller.start()
        runCurrent()
        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent() // the first send fails
        assertEquals("a failed send proves nothing", 0, reached)
        testScheduler.advanceTimeBy(1_000)
        runCurrent() // the retry is delivered

        assertEquals("only the delivered attempt counts", 1, reached)
    }

    // --- Watch settings (#102, REQ-ANDROID-021) ---------------------------------------------------

    private fun settingsEvents(transport: FakeWatchTransport) =
        events(transport).count { it == Protocol.Events.SETTINGS }

    @Test
    fun noSettingsAreSentToAClosedWatchapp() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent()

        // Never opened in this session: only the phone's own READY, which carries no settings.
        transport.emitInbound(readyMessage())
        runCurrent()
        controller.onPhoneSettingEdit(WatchSetting.UNITS, 1)
        runCurrent()
        assertEquals(0, settingsEvents(transport))

        // Opened, then closed: still nothing.
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 1)))
        runCurrent()
        transport.emitClosed()
        runCurrent()
        controller.onPhoneSettingEdit(WatchSetting.UNITS, 1)
        runCurrent()
        assertEquals(0, settingsEvents(transport))
        assertEquals("never launched for settings", 0, transport.launchCount)
    }

    @Test
    fun aRealReadyFlushesTheQueue() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent()
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 20)))
        runCurrent()
        transport.emitClosed()
        runCurrent()
        controller.onPhoneSettingEdit(WatchSetting.ARROW_LEFT, 1)
        runCurrent()
        assertEquals(0, settingsEvents(transport))

        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 20)))
        runCurrent()
        assertEquals(1, settingsEvents(transport))
        assertTrue(controller.watchappOpen.value)
    }

    // --- When driving (#132) ----------------------------------------------------------------------

    /** A watchapp with protocol 1.4 whose When driving is [mode], at the default 25 km/h. */
    private fun drivingStore(mode: Int = DrivingSpeeds.MODE_HIDE, minor: Int = 4) = InMemoryWatchSettingsStore(
        WatchSettingsSyncState(
            mirror = WatchMessages.settings(revision = 1, overrides = mapOf(WatchSetting.DRIVING_MODE to mode)),
            receivedFromWatch = true,
            watchProtocolMinor = minor,
        ),
    )

    private fun TestScope.drivingController(
        transport: FakeWatchTransport,
        store: InMemoryWatchSettingsStore = drivingStore(),
        log: DrivingLogRepository = DrivingLogRepository(),
        autoLaunch: Boolean = true,
    ) = NavigationController(
        transport,
        backgroundScope,
        appVersion = "0.0.82",
        clock = { testScheduler.currentTime },
        watchSettings = store,
        drivingLog = log,
        initialSettings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = autoLaunch),
    )

    /** Google Maps' remaining route distance counting down; one card every 5 s, as a test route. */
    private class Route(var remaining: Double = 40_000.0)

    /**
     * [seconds] of travel at [metresPerSecond]: every 5 s a turn instruction and its speed fields,
     * as DebugCaptureProcessor forwards them.
     */
    private suspend fun TestScope.travel(
        controller: NavigationController,
        route: Route,
        metresPerSecond: Double,
        seconds: Int,
    ) {
        repeat(seconds / 5) {
            testScheduler.advanceTimeBy(5_000)
            route.remaining -= metresPerSecond * 5
            val turnIn = (route.remaining % 1_000).toInt() + 10
            controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = turnIn))
            controller.onSpeedSample(
                SpeedSample(
                    postTimeMillis = testScheduler.currentTime,
                    progress = 50_000 - route.remaining.toInt(),
                    progressMax = 50_000,
                    subText = null,
                    packageName = DrivingSpeedEstimator.GOOGLE_MAPS_PACKAGE,
                    hasInstruction = true,
                ),
            )
            runCurrent()
        }
    }

    private fun navigationUpdates(transport: FakeWatchTransport) =
        events(transport).count { it == Protocol.Events.NAVIGATION_UPDATE }

    @Test
    fun withHideTheWatchappStillLaunchesAtSessionStart() = runTest {
        val transport = FakeWatchTransport()
        val controller = drivingController(transport)
        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 300))
        testScheduler.advanceTimeBy(1_600)
        runCurrent()

        assertEquals("FR6: launched as today", 1, transport.launchCount)
        assertEquals(1, navigationUpdates(transport))
        assertEquals(0, transport.closeCount)
    }

    @Test
    fun sustainedDrivingClosesTheWatchappOnceAndStopsTheUpdates() = runTest {
        val transport = FakeWatchTransport()
        val log = DrivingLogRepository()
        val controller = drivingController(transport, log = log)
        controller.start()
        runCurrent()
        val route = Route()

        travel(controller, route, metresPerSecond = 14.0, seconds = 90) // about 50 km/h
        assertEquals("FR7: closed once", 1, transport.closeCount)
        val sentWhenHidden = navigationUpdates(transport)
        assertTrue("updates went out before the hide", sentWhenHidden > 0)

        travel(controller, route, metresPerSecond = 14.0, seconds = 60)
        assertEquals("FR8.1: no update while hidden", sentWhenHidden, navigationUpdates(transport))
        assertEquals("never relaunched", 1, transport.launchCount)
        assertEquals(1, transport.closeCount)
        assertTrue("FR8.3: still navigating", controller.status.value.navigating)

        val status = controller.drivingStatus.value
        assertTrue("FR13", status.hidden)
        assertEquals(50.4, status.measuredKmh!!, 2.0)
        assertTrue(log.snapshot().any { it.decision == DrivingDecision.HIDE })
        assertTrue(log.snapshot().any { it.decision == DrivingDecision.SAMPLE })
    }

    @Test
    fun slowingDownFor2MinutesReopensTheWatchappWithTheCurrentState() = runTest {
        val transport = FakeWatchTransport()
        val controller = drivingController(transport)
        controller.start()
        runCurrent()
        val route = Route()
        travel(controller, route, metresPerSecond = 14.0, seconds = 90)
        assertTrue(controller.drivingStatus.value.hidden)
        val sentWhenHidden = navigationUpdates(transport)

        travel(controller, route, metresPerSecond = 1.4, seconds = 240) // walking pace
        testScheduler.advanceTimeBy(1_600) // the launch path's autonomous READY
        runCurrent()

        assertEquals("FR9: reopened", 2, transport.launchCount)
        assertFalse(controller.drivingStatus.value.hidden)
        assertTrue("the current instruction was sent", navigationUpdates(transport) > sentWhenHidden)
    }

    @Test
    fun withAutoLaunchOffItClosesButNeverReopens() = runTest {
        val transport = FakeWatchTransport()
        val controller = drivingController(transport, autoLaunch = false)
        controller.start()
        runCurrent()
        val route = Route()
        travel(controller, route, metresPerSecond = 14.0, seconds = 90)
        assertEquals(1, transport.closeCount)

        // Stopped: Google Maps posts nothing new; the timer sees the speed drop (FR9, FR9.1).
        testScheduler.advanceTimeBy(300_000)
        runCurrent()

        assertEquals("FR9.1: never launched", 0, transport.launchCount)
        assertFalse("the hidden state ended", controller.drivingStatus.value.hidden)
    }

    @Test
    fun openingTheWatchappWhileHiddenKeepsItOpenForTheSession() = runTest {
        val transport = FakeWatchTransport()
        val log = DrivingLogRepository()
        val controller = drivingController(transport, log = log)
        controller.start()
        runCurrent()
        val route = Route()
        travel(controller, route, metresPerSecond = 14.0, seconds = 90)
        val sentWhenHidden = navigationUpdates(transport)

        transport.emitInbound(WatchListenerService.appOpenedReadyMessage()) // the user opened it
        runCurrent()
        assertFalse(controller.drivingStatus.value.hidden)
        assertTrue("FR10: it gets the state", navigationUpdates(transport) > sentWhenHidden)

        travel(controller, route, metresPerSecond = 14.0, seconds = 180)
        assertEquals("never closed again this session", 1, transport.closeCount)
        assertTrue(log.snapshot().any { it.decision == DrivingDecision.KEPT_OPEN })
    }

    @Test
    fun aStopWhileHiddenSendsNothingAndTheNextSessionStartsFresh() = runTest {
        val transport = FakeWatchTransport()
        val log = DrivingLogRepository()
        val controller = drivingController(transport, log = log)
        controller.start()
        runCurrent()
        val route = Route()
        travel(controller, route, metresPerSecond = 14.0, seconds = 90)
        val sentBefore = transport.sent.size

        controller.onNavigationStopped()
        runCurrent()
        assertEquals("FR11: nothing relaunches it for the stop", sentBefore, transport.sent.size)
        assertFalse(controller.drivingStatus.value.hidden)
        assertEquals(DrivingDecision.RESET, log.snapshot().last().decision)

        travel(controller, Route(), metresPerSecond = 14.0, seconds = 90)
        assertEquals("a new session can hide again", 2, transport.closeCount)
    }

    @Test
    fun switchingToShowDirectionsWhileHiddenReopensAtOnce() = runTest {
        val transport = FakeWatchTransport()
        val controller = drivingController(transport)
        controller.start()
        runCurrent()
        travel(controller, Route(), metresPerSecond = 14.0, seconds = 90)
        assertTrue(controller.drivingStatus.value.hidden)

        controller.onPhoneSettingEdit(WatchSetting.DRIVING_MODE, DrivingSpeeds.MODE_SHOW)
        runCurrent()

        assertEquals("FR12", 2, transport.launchCount)
        assertFalse(controller.drivingStatus.value.hidden)
    }

    @Test
    fun aWatchappWithoutTheDrivingSettingIsNeverHidden() = runTest {
        val transport = FakeWatchTransport()
        val controller = drivingController(transport, store = drivingStore(minor = 3))
        controller.start()
        runCurrent()
        travel(controller, Route(), metresPerSecond = 14.0, seconds = 180)
        assertEquals("FR4, NFR5", 0, transport.closeCount)
    }

    @Test
    fun withTheDefaultSettingsNothingChanges() = runTest {
        // NFR6: Show directions (the default) gives the same sends as a run without speed samples.
        val withSpeed = FakeWatchTransport()
        val controller = drivingController(withSpeed, store = drivingStore(mode = DrivingSpeeds.MODE_SHOW))
        controller.start()
        runCurrent()
        travel(controller, Route(), metresPerSecond = 14.0, seconds = 180)

        val withoutSpeed = FakeWatchTransport()
        val plain = drivingController(withoutSpeed, store = drivingStore(mode = DrivingSpeeds.MODE_SHOW))
        plain.start()
        runCurrent()
        val route = Route()
        repeat(36) {
            testScheduler.advanceTimeBy(5_000)
            route.remaining -= 14.0 * 5
            plain.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = (route.remaining % 1_000).toInt() + 10))
            runCurrent()
        }

        assertEquals(0, withSpeed.closeCount)
        assertEquals(1, withSpeed.launchCount)
        assertEquals(events(withoutSpeed), events(withSpeed))
        assertFalse(controller.drivingStatus.value.hidden)
    }
}
