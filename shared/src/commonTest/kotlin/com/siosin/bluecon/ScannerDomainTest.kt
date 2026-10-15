package com.siosin.bluecon

import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ScanConfig
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.bluetooth.proximityZone
import com.siosin.bluecon.domain.ScanDevices
import com.siosin.bluecon.domain.ScannerRepository
import com.siosin.bluecon.domain.filterDevices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class ScannerDomainTest {
    private fun device(id: String, name: String?, rssi: Int, stale: Boolean = false) =
        BleDevice(id, name, rssi, rssi.toDouble(), proximityZone(rssi), 100, stale)

    @Test
    fun searchMatchesNamesAndAddressesAndRespectsInclusiveSignalThreshold() {
        val devices = listOf(device("AA:01", "Beacon", -80), device("BB:02", "BEACON TWO", -60), device("CC:03", null, -50))
        assertEquals(listOf("BB:02", "AA:01"), filterDevices(devices, " beacon ", -80).map { it.id })
        assertEquals(listOf("CC:03"), filterDevices(devices, "cc:03", null).map { it.id })
        assertEquals(emptyList(), filterDevices(devices, "beacon", -50))
    }

    @Test
    fun staleDevicesSortAfterLiveSignalsEvenIfTheirLastSignalWasStronger() {
        val devices = listOf(device("A", null, -20, stale = true), device("B", null, -70))
        assertEquals(listOf("B", "A"), filterDevices(devices, "", null).map { it.id })
    }

    @Test
    fun leavingForegroundEndsTheSessionWithAnExplicitReason() {
        val fake = object : ScannerRepository {
            override val devices = MutableStateFlow<List<BleDevice>>(emptyList())
            override val state = MutableStateFlow<ScanState>(ScanState.Idle)
            var requestedConfig: ScanConfig? = null
            override fun start(config: ScanConfig) { requestedConfig = config; state.value = ScanState.Scanning }
            override fun stop(reason: StopReason) { state.value = ScanState.Stopped(reason) }
        }
        val useCase = ScanDevices(fake)
        useCase.start()
        assertEquals(120_000L, fake.requestedConfig?.durationMillis)
        useCase.onBackground()
        assertEquals(ScanState.Stopped(StopReason.BACKGROUND), fake.state.value)
    }
}
