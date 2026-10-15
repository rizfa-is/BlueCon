package com.siosin.bluecon.bluetooth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceTrackerTest {
    private val config = ScanConfig(staleAfterMillis = 2_000, retainForMillis = 5_000)

    @Test
    fun silentDeviceBecomesLostAndExpiresWithoutAnotherPacket() {
        val tracker = DeviceTracker(EmaSignalProcessor())
        tracker.record("A", "Beacon", -60, 100_000, 1_000, config)
        assertFalse(tracker.snapshot(2_999, config).single().isStale)
        val lost = tracker.snapshot(3_000, config).single()
        assertTrue(lost.isStale)
        assertEquals(ProximityZone.LOST, lost.zone)
        assertEquals(-60, lost.rawRssi)
        assertTrue(tracker.snapshot(6_000, config).isEmpty())
    }

    @Test
    fun wallClockJumpDoesNotExpireADevice() {
        val tracker = DeviceTracker(EmaSignalProcessor())
        tracker.record("A", null, -60, 1_000_000, 1_000, config)
        tracker.record("A", null, -50, 1, 1_500, config)
        assertFalse(tracker.snapshot(1_600, config).single().isStale)
        assertEquals(1L, tracker.snapshot(1_600, config).single().lastSeenAtMillis)
    }

    @Test
    fun delayedBatchCannotOverwriteANewerObservation() {
        val tracker = DeviceTracker(EmaSignalProcessor())
        tracker.record("A", "New", -50, 200, 200, config)
        tracker.record("A", "Old", -90, 100, 100, config)
        val latest = tracker.snapshot(200, config).single()
        assertEquals("New", latest.name)
        assertEquals(-50, latest.rawRssi)
    }

    @Test
    fun returningDeviceStartsANewSmoothingWindowAndPreservesName() {
        val tracker = DeviceTracker(EmaSignalProcessor())
        tracker.record("A", "Beacon", -90, 100, 100, config)
        tracker.record("A", null, -40, 3_000, 3_000, config)
        val returned = tracker.snapshot(3_000, config).single()
        assertEquals(-40.0, returned.smoothedRssi)
        assertEquals("Beacon", returned.name)
        assertFalse(returned.isStale)
    }

    @Test
    fun rotatingAddressesCannotGrowTheCacheBeyondItsLimit() {
        val tracker = DeviceTracker(EmaSignalProcessor())
        val bounded = config.copy(maxDevices = 2)
        tracker.record("A", null, -50, 1, 1, bounded)
        tracker.record("B", null, -60, 2, 2, bounded)
        tracker.record("C", null, -70, 3, 3, bounded)
        assertEquals(listOf("B", "C"), tracker.snapshot(3, bounded).map { it.id })
    }

    @Test
    fun unavailableRssiIsIgnoredAndRawValuesAreNotReplacedByEma() {
        val tracker = DeviceTracker(EmaSignalProcessor(alpha = 0.5))
        tracker.record("invalid", null, 127, 1, 1, config)
        tracker.record("A", null, -80, 1, 1, config)
        tracker.record("A", null, -40, 2, 2, config)
        val device = tracker.snapshot(2, config).single()
        assertEquals(-40, device.rawRssi)
        assertEquals(-60.0, device.smoothedRssi)
        assertEquals(ProximityZone.CLOSE, device.zone)
    }

    @Test
    fun thresholdBoundariesBelongToOneZoneOnly() {
        val inputs = listOf(-30, -31, -50, -51, -70, -71, -80, -81, -90, -91)
        assertEquals(
            listOf(ProximityZone.VERY_CLOSE, ProximityZone.CLOSE, ProximityZone.CLOSE,
                ProximityZone.NEARBY, ProximityZone.NEARBY, ProximityZone.FAR,
                ProximityZone.FAR, ProximityZone.VERY_FAR, ProximityZone.VERY_FAR, ProximityZone.LOST),
            inputs.map(::proximityZone),
        )
    }
}
