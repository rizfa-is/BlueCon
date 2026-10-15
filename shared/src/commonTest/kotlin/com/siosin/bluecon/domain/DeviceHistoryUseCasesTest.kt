package com.siosin.bluecon.domain

import com.siosin.bluecon.domain.model.DeviceHistory
import com.siosin.bluecon.domain.model.HistoryState
import com.siosin.bluecon.domain.usecase.history.ClearDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.ObserveDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.SearchDeviceHistoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceHistoryUseCasesTest {
    @Test
    fun historySearchMatchesNameOrAddressWithoutChangingRecencyOrder() {
        val search = SearchDeviceHistoryUseCase()
        val devices = listOf(
            DeviceHistory("AA:01", "Beacon Two", -50, 300),
            DeviceHistory("BB:02", null, -60, 200),
            DeviceHistory("CC:03", "BEACON ONE", -70, 100),
        )
        assertEquals(listOf("AA:01", "CC:03"), search(devices, " beacon ").map { it.id })
        assertEquals(listOf("BB:02"), search(devices, "bb:02").map { it.id })
        assertEquals(devices, search(devices, "  "))
        assertEquals(emptyList(), search(devices, "missing"))
    }

    @Test
    fun clearingHistoryUpdatesTheSameObservedStream() = runTest {
        val repository = FakeDeviceHistoryRepository()
        val observations = ObserveDeviceHistoryUseCase(repository)()
        repository.state.value = HistoryState(listOf(DeviceHistory("AA", "Sensor", -60, 100)), error = "Previous error")
        assertEquals(1, observations.value.devices.size)
        ClearDeviceHistoryUseCase(repository)()
        assertEquals(1, repository.clearCount)
        assertEquals(HistoryState(), observations.value)
    }
}
