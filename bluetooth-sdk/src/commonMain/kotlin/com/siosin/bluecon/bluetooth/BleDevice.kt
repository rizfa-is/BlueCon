package com.siosin.bluecon.bluetooth

/** [id] is an observed address, not a permanent physical-device identity. */
data class BleDevice(
    val id: String,
    val name: String?,
    val rawRssi: Int,
    val smoothedRssi: Double,
    val zone: ProximityZone,
    val lastSeenAtMillis: Long,
    val isStale: Boolean = false,
    /** Monotonic time within this boot. Never persist it as a calendar date. */
    val observedAtElapsedMillis: Long = 0,
    val advertisement: AdvertisementData = AdvertisementData(),
    val firstSeenAtMillis: Long = lastSeenAtMillis,
    /** Android scan reports accepted in the current cache window, not transmitted packets. */
    val observationCount: Long = 1,
    /** At most 48 recent observations. A stale gap starts a new signal window. */
    val signalHistory: List<RssiSample> = emptyList(),
)

data class RssiSample(val rssi: Int, val elapsedMillis: Long)

/** Missing fields stay null; an advertised service is not a discovered GATT service. */
data class AdvertisementData(
    val connectable: Boolean? = null,
    val format: AdvertisingFormat? = null,
    val txPowerDbm: Int? = null,
    val primaryPhy: BluetoothPhy? = null,
    val secondaryPhy: BluetoothPhy? = null,
    val advertisingSid: Int? = null,
    val dataStatus: AdvertisingDataStatus? = null,
    val flags: Int? = null,
    val serviceUuids: List<String> = emptyList(),
    /** Company identifiers and payload hex, without inferring the physical manufacturer. */
    val manufacturerData: Map<Int, String> = emptyMap(),
    val serviceData: Map<String, String> = emptyMap(),
    val payloadSizeBytes: Int = 0,
    val rawPayloadHex: String? = null,
)

enum class AdvertisingFormat { LEGACY, EXTENDED }
enum class BluetoothPhy { LE_1M, LE_2M, LE_CODED }
enum class AdvertisingDataStatus { COMPLETE, TRUNCATED }

enum class ProximityZone(val label: String, val distanceLabel: String) {
    VERY_CLOSE("Very close", "< 1 m"),
    CLOSE("Close", "1–3 m"),
    NEARBY("Nearby", "3–10 m"),
    FAR("Far", "10–20 m"),
    VERY_FAR("Very far", "> 20 m"),
    LOST("Out of range", "Unavailable"),
}

/** Overlapping boundaries belong to the stronger zone. These are RSSI ranges, not calibrated distances. */
fun proximityZone(rssi: Int): ProximityZone = when {
    rssi >= -30 -> ProximityZone.VERY_CLOSE
    rssi >= -50 -> ProximityZone.CLOSE
    rssi >= -70 -> ProximityZone.NEARBY
    rssi >= -80 -> ProximityZone.FAR
    rssi >= -90 -> ProximityZone.VERY_FAR
    else -> ProximityZone.LOST
}
