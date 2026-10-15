package com.siosin.bluecon.domain

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.proximityZone
import com.siosin.bluecon.domain.model.DeviceSort
import com.siosin.bluecon.domain.usecase.scanner.FilterScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.SortScannedDevicesUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class ScannedDeviceUseCasesTest {
    private val filter = FilterScannedDevicesUseCase()
    private val sort = SortScannedDevicesUseCase()
    private fun device(id: String, name: String?, rssi: Int, stale: Boolean = false) =
        BleDevice(id, name, rssi, rssi.toDouble(), proximityZone(rssi), 100, stale)

    @Test
    fun searchMatchesNamesAndAddressesAndRespectsInclusiveSignalThresholdWithoutReordering() {
        val devices = listOf(device("AA:01", "Beacon", -80), device("BB:02", "BEACON TWO", -60), device("CC:03", null, -50))
        assertEquals(listOf("AA:01", "BB:02"), filter(devices, " beacon ", -80).map { it.id })
        assertEquals(listOf("CC:03"), filter(devices, "cc:03", null).map { it.id })
        assertEquals(emptyList(), filter(devices, "beacon", -50))
        assertEquals(devices, filter(devices, "  ", null))
    }

    @Test
    fun everySortModeKeepsStaleDevicesBelowRecentReports() {
        val devices = listOf(device("A", "Alpha", -20, stale = true), device("B", "Zebra", -70))
        DeviceSort.entries.forEach { mode ->
            assertEquals(listOf("B", "A"), sort(devices, mode).map { it.id })
        }
    }

    @Test
    fun namedFirstTreatsBlankNamesAsUnnamedAndSignalSortRemainsIndependent() {
        val devices = listOf(
            device("unnamed", null, -20), device("blank", "  ", -30),
            device("named", "Sensor", -80), device("stale", "Beacon", -10, stale = true),
        )
        assertEquals(listOf("named", "unnamed", "blank", "stale"), sort(devices, DeviceSort.NAMED_FIRST).map { it.id })
        assertEquals(listOf("unnamed", "blank", "named", "stale"), sort(devices, DeviceSort.SIGNAL_STRENGTH).map { it.id })
    }

    @Test
    fun alphabeticalAndRecentSortingComposeWithFiltering() {
        val devices = listOf(
            device("B", "zebra", -60).copy(lastSeenAtMillis = 200),
            device("A", " Alpha ", -70).copy(lastSeenAtMillis = 100),
            device("C", null, -30).copy(lastSeenAtMillis = 300),
        )
        assertEquals(listOf("A", "B", "C"), sort(devices, DeviceSort.NAME).map { it.id })
        assertEquals(listOf("C", "B", "A"), sort(devices, DeviceSort.LAST_SEEN).map { it.id })
        assertEquals(listOf("B"), sort(filter(devices, "ZEB", -60), DeviceSort.NAME).map { it.id })
    }

    @Test
    fun equalSortValuesUseAddressToPreventOrderJitter() {
        val devices = listOf(device("B", "Sensor", -60), device("A", "Sensor", -60))
        DeviceSort.entries.forEach { mode ->
            assertEquals(listOf("A", "B"), sort(devices, mode).map { it.id })
        }
    }
}
