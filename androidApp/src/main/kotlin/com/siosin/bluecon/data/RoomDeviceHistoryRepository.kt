package com.siosin.bluecon.data

import android.os.SystemClock
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.domain.model.DeviceHistory
import com.siosin.bluecon.domain.model.HistoryState
import com.siosin.bluecon.domain.repository.DeviceHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RoomDeviceHistoryRepository(
    private val database: HistoryDatabase,
    scope: CoroutineScope,
) : DeviceHistoryRepository {
    private val mutableState = MutableStateFlow(HistoryState())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    // Never rewrite an observation already committed, including after clearing history.
    private val committed = mutableMapOf<String, Long>()
    private var clearedAtElapsedMillis = Long.MIN_VALUE

    init { scope.launch { access { refresh() } } }

    override suspend fun save(devices: List<BleDevice>) = access {
        val updates = devices.filter {
            it.observedAtElapsedMillis > clearedAtElapsedMillis &&
                it.observedAtElapsedMillis > (committed[it.id] ?: Long.MIN_VALUE)
        }
        if (updates.isNotEmpty()) {
            database.runInTransaction {
                database.historyDao().upsert(updates.map {
                    HistoryEntity(it.id, it.name, it.rawRssi, it.lastSeenAtMillis)
                })
                database.historyDao().trim()
            }
            updates.forEach { committed[it.id] = it.observedAtElapsedMillis }
            // Bound the write cache independently of devices with rotating addresses.
            if (committed.size > 2_500) {
                val keep = committed.entries.sortedByDescending { it.value }.take(2_000).associate { it.toPair() }
                committed.clear()
                committed.putAll(keep)
            }
            refresh()
        } else if (mutableState.value.error != null) {
            refresh()
        }
    }

    override suspend fun clear() = access {
        database.historyDao().clear()
        clearedAtElapsedMillis = SystemClock.elapsedRealtime()
        committed.clear()
        refresh()
    }

    private fun refresh() {
        val records = database.historyDao().getAll()
        mutableState.value = HistoryState(records.map { DeviceHistory(it.id, it.name, it.lastRssi, it.lastSeenAtMillis) })
    }

    private suspend fun access(block: () -> Unit) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = mutableState.value.copy(error = "Unable to read or save history. Scan again to retry.")
                }
            }
        }
    }
}
