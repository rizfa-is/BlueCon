package com.siosin.bluecon

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.siosin.bluecon.bluetooth.AndroidBluetoothScanner
import com.siosin.bluecon.data.HistoryDatabase
import com.siosin.bluecon.data.RoomDeviceHistoryRepository
import com.siosin.bluecon.data.SdkScannerRepository
import com.siosin.bluecon.domain.usecase.history.ClearDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.ObserveDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.SaveDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.SearchDeviceHistoryUseCase
import com.siosin.bluecon.domain.usecase.history.SyncScanHistoryUseCase
import com.siosin.bluecon.domain.usecase.scanner.FilterScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.ObserveScanStateUseCase
import com.siosin.bluecon.domain.usecase.scanner.SortScannedDevicesUseCase
import com.siosin.bluecon.domain.usecase.scanner.StartScanUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanOnBackgroundUseCase
import com.siosin.bluecon.domain.usecase.scanner.StopScanUseCase
import com.siosin.bluecon.presentation.ScannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val scannerRepository = SdkScannerRepository(scanner)
    private val observeScannedDevices = ObserveScannedDevicesUseCase(scannerRepository)
    private val observeScanState = ObserveScanStateUseCase(scannerRepository)
    private val saveDeviceHistory = SaveDeviceHistoryUseCase(history)
    private val syncScanHistory = SyncScanHistoryUseCase(observeScanState, observeScannedDevices, saveDeviceHistory)

    init {
        scope.launch { syncScanHistory() }
    }

    fun createScannerViewModel() = ScannerViewModel(
        startScan = StartScanUseCase(scannerRepository),
        stopScan = StopScanUseCase(scannerRepository),
        stopScanOnBackground = StopScanOnBackgroundUseCase(scannerRepository),
        observeScannedDevices = observeScannedDevices,
        observeScanState = observeScanState,
        observeDeviceHistory = ObserveDeviceHistoryUseCase(history),
        filterScannedDevices = FilterScannedDevicesUseCase(),
        sortScannedDevices = SortScannedDevicesUseCase(),
        searchDeviceHistory = SearchDeviceHistoryUseCase(),
        clearDeviceHistory = ClearDeviceHistoryUseCase(history),
    )

    fun flushHistory() {
        val snapshot = observeScannedDevices().value
        scope.launch { saveDeviceHistory(snapshot) }
    }
}
