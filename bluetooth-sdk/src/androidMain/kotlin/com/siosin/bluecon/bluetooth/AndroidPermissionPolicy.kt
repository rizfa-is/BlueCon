package com.siosin.bluecon.bluetooth

/** Runtime permissions for a modern-target host. Background scanning is intentionally separate. */
object AndroidPermissionPolicy {
    const val SCAN = "android.permission.BLUETOOTH_SCAN"
    const val CONNECT = "android.permission.BLUETOOTH_CONNECT"
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"

    fun requiredPermissions(sdkInt: Int, derivesPhysicalLocation: Boolean): List<String> = buildList {
        if (sdkInt >= 31) {
            add(SCAN)
            // Needed for BluetoothAdapter.isEnabled even though no GATT connection is made.
            add(CONNECT)
            if (derivesPhysicalLocation) {
                add(COARSE_LOCATION)
                add(FINE_LOCATION)
            }
        } else {
            add(FINE_LOCATION)
        }
    }

    fun requiresLocationServices(sdkInt: Int, derivesPhysicalLocation: Boolean): Boolean =
        sdkInt < 31 || derivesPhysicalLocation
}
