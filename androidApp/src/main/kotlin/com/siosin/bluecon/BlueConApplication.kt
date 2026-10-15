package com.siosin.bluecon

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.siosin.bluecon.bluetooth.AndroidBluetoothScanner
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.data.HistoryDatabase
import com.siosin.bluecon.data.RoomDeviceHistoryRepository
import com.siosin.bluecon.data.SdkScannerRepository
import com.siosin.bluecon.domain.ScanDevices
import com.siosin.bluecon.presentation.ScannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BlueConApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Constructor injection at the composition root; the SDK does not impose a DI framework. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val scanner = AndroidBluetoothScanner(context, derivesPhysicalLocation = true)
    private val database = Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "bluecon-history.db").build()
    private val history = RoomDeviceHistoryRepository(database, scope)
    private val scanDevices = ScanDevices(SdkScannerRepository(scanner))

    init {
        // Batched writes, independent of screen collectors and rotation.
        scope.launch {
            scanner.state.collectLatest { state ->
                if (state == ScanState.Scanning) {
                    while (isActive) {
                        delay(2_000)
                        history.save(scanner.devices.value)
                    }
                } else {
                    history.save(scanner.devices.value)
                }
            }
        }
    }

    fun createScannerViewModel() = ScannerViewModel(scanDevices, history)
    fun flushHistory() {
        val snapshot = scanner.devices.value
        scope.launch { history.save(snapshot) }
    }
}
