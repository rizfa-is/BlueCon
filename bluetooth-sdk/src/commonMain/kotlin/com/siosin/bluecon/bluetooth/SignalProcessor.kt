package com.siosin.bluecon.bluetooth

/** Extension point for a measured native implementation; called serially by the scanner. */
interface SignalProcessor {
    fun process(deviceId: String, rssi: Int): Double
    fun forget(deviceId: String)
    fun reset()
}

class EmaSignalProcessor(private val alpha: Double = 0.25) : SignalProcessor {
    init { require(alpha > 0.0 && alpha <= 1.0) }
    private val values = mutableMapOf<String, Double>()

    override fun process(deviceId: String, rssi: Int): Double {
        val previous = values[deviceId]
        val next = if (previous == null) rssi.toDouble() else alpha * rssi + (1 - alpha) * previous
        values[deviceId] = next
        return next
    }

    override fun forget(deviceId: String) { values.remove(deviceId) }
    override fun reset() { values.clear() }
}
