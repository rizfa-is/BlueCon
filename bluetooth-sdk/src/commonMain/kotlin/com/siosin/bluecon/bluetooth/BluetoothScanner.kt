package com.siosin.bluecon.bluetooth

import kotlinx.coroutines.flow.StateFlow

/** One shared scan session. Collecting a flow never starts a platform scan. */
interface BluetoothScanner {
    val state: StateFlow<ScanState>
    val devices: StateFlow<List<BleDevice>>
    fun start(config: ScanConfig = ScanConfig())
    fun stop(reason: StopReason = StopReason.USER)
    /** Releases callbacks, receiver and scope. The instance cannot be reused afterwards. */
    fun close()
}

data class ScanConfig(
    val durationMillis: Long = 120_000,
    val staleAfterMillis: Long = 8_000,
    val retainForMillis: Long = 60_000,
    val publishIntervalMillis: Long = 300,
    val maxDevices: Int = 500,
    val highResponsiveness: Boolean = false,
    /** Optional canonical UUIDs; an empty set discovers all advertising devices. */
    val serviceUuids: Set<String> = emptySet(),
) {
    init {
        require(durationMillis in 1_000..600_000)
        require(staleAfterMillis >= 1_000)
        require(retainForMillis > staleAfterMillis)
        require(publishIntervalMillis in 100..5_000)
        require(maxDevices in 1..5_000)
    }
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Stopped(val reason: StopReason) : ScanState
    data class Blocked(val reason: ScanBlocker) : ScanState
    data class Failed(val code: Int?, val message: String) : ScanState
    data object Closed : ScanState
}

enum class StopReason { USER, BACKGROUND, TIMEOUT }

sealed interface ScanBlocker {
    data object Unsupported : ScanBlocker
    data object BluetoothOff : ScanBlocker
    data object LocationOff : ScanBlocker
    data class MissingPermissions(val permissions: List<String>) : ScanBlocker
}
