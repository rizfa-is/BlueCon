package com.siosin.bluecon.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build

internal fun ScanResult.advertisementData(): AdvertisementData {
    val record = scanRecord
    val bytes = record?.bytes
    val recordPower = record?.txPowerLevel?.takeIf { it in -127..126 }
    val manufacturer = record?.manufacturerSpecificData
    return AdvertisementData(
        connectable = if (Build.VERSION.SDK_INT >= 26) isConnectable else null,
        format = if (Build.VERSION.SDK_INT >= 26) {
            if (isLegacy) AdvertisingFormat.LEGACY else AdvertisingFormat.EXTENDED
        } else null,
        txPowerDbm = recordPower ?: if (Build.VERSION.SDK_INT >= 26) txPower.takeIf { it in -127..126 } else null,
        primaryPhy = if (Build.VERSION.SDK_INT >= 26) phy(primaryPhy) else null,
        secondaryPhy = if (Build.VERSION.SDK_INT >= 26) phy(secondaryPhy) else null,
        advertisingSid = if (Build.VERSION.SDK_INT >= 26) advertisingSid.takeIf { it != ScanResult.SID_NOT_PRESENT } else null,
        dataStatus = if (Build.VERSION.SDK_INT >= 26) when (dataStatus) {
            ScanResult.DATA_COMPLETE -> AdvertisingDataStatus.COMPLETE
            ScanResult.DATA_TRUNCATED -> AdvertisingDataStatus.TRUNCATED
            else -> null
        } else null,
        flags = record?.advertiseFlags?.takeIf { it >= 0 },
        serviceUuids = record?.serviceUuids.orEmpty().map { it.toString() },
        manufacturerData = buildMap {
            if (manufacturer != null) for (index in 0 until manufacturer.size()) {
                put(manufacturer.keyAt(index), manufacturer.valueAt(index).payloadHex())
            }
        },
        serviceData = record?.serviceData.orEmpty().mapKeys { it.key.toString() }.mapValues { it.value.payloadHex() },
        payloadSizeBytes = bytes?.size ?: 0,
        rawPayloadHex = bytes?.payloadHex(),
    )
}

private fun phy(value: Int): BluetoothPhy? = when (value) {
    BluetoothDevice.PHY_LE_1M -> BluetoothPhy.LE_1M
    BluetoothDevice.PHY_LE_2M -> BluetoothPhy.LE_2M
    BluetoothDevice.PHY_LE_CODED -> BluetoothPhy.LE_CODED
    else -> null
}

internal fun ByteArray.payloadHex(): String = buildString(size * 3) {
    val digits = "0123456789ABCDEF"
    for (index in this@payloadHex.indices) {
        if (index > 0) append(' ')
        val value = this@payloadHex[index].toInt() and 0xFF
        append(digits[value ushr 4])
        append(digits[value and 15])
    }
}
