package com.siosin.bluecon.bluetooth

/** Bounded in-memory reducer. Elapsed time, not wall time, controls expiry. */
internal class DeviceTracker(private val processor: SignalProcessor) {
    private data class Entry(val device: BleDevice, val elapsedMillis: Long)
    private val entries = mutableMapOf<String, Entry>()

    fun record(
        id: String, name: String?, rssi: Int,
        wallTimeMillis: Long, elapsedMillis: Long, config: ScanConfig,
    ) {
        // Android uses 127 for unavailable RSSI. Ignore invalid/unavailable values.
        if (rssi !in -127..126 || id.isBlank()) return
        val previous = entries[id]
        // Batched or delayed callbacks must never replace a newer observation.
        if (previous != null && elapsedMillis < previous.elapsedMillis) return
        if (previous == null && entries.size >= config.maxDevices) {
            val oldest = entries.minByOrNull { it.value.elapsedMillis }?.key
            if (oldest != null) { entries.remove(oldest); processor.forget(oldest) }
        }
        if (previous != null && elapsedMillis - previous.elapsedMillis >= config.staleAfterMillis) {
            processor.forget(id)
        }
        entries[id] = Entry(
            BleDevice(
                id, name?.takeIf { it.isNotBlank() } ?: previous?.device?.name,
                rssi, processor.process(id, rssi), proximityZone(rssi), wallTimeMillis,
                observedAtElapsedMillis = elapsedMillis,
            ),
            elapsedMillis,
        )
    }

    fun snapshot(elapsedMillis: Long, config: ScanConfig): List<BleDevice> {
        val expired = entries.filterValues { elapsedMillis - it.elapsedMillis >= config.retainForMillis }.keys
        expired.forEach { entries.remove(it); processor.forget(it) }
        return entries.values.map { entry ->
            val stale = elapsedMillis - entry.elapsedMillis >= config.staleAfterMillis
            entry.device.copy(isStale = stale, zone = if (stale) ProximityZone.LOST else entry.device.zone)
        }.sortedWith(compareBy<BleDevice> { it.isStale }.thenByDescending { it.rawRssi }.thenBy { it.id })
    }

    fun clear() { entries.clear(); processor.reset() }
}
