# BlueCon Bluetooth SDK

KMP library with portable models and signal processing, plus a foreground BLE advertising scanner for Android API 24+. The library has no UI, database, Activity, or DI framework dependency.

## Integration

```kotlin
dependencies {
    implementation(project(":bluetooth-sdk"))
}
```

For KMP consumers, add the dependency to the relevant source set. Instantiate `AndroidBluetoothScanner` from Android code and keep one instance per owner, outside recomposition:

```kotlin
val scanner = AndroidBluetoothScanner(applicationContext, derivesPhysicalLocation = true)
// Resolve missing permissions, Bluetooth, or location settings in the host first.
if (scanner.readinessBlocker() == null) scanner.start(ScanConfig(durationMillis = 120_000))
scope.launch { scanner.devices.collect { devices -> /* update state */ } }
scope.launch { scanner.state.collect { state -> /* display status */ } }
scanner.stop(StopReason.BACKGROUND)
// Release only when the owner is permanently finished, not on rotation.
scanner.close()
```

Start is idempotent while scanning. New configuration takes effect on the next session. Flow collection does not start another scan. Stopped sessions retain their last snapshot; a new session clears it. Session methods run on the main thread. Create a new instance after closing.

`maven-publish` is configured with group `com.siosin.bluecon`, version `0.1.0`. No remote publication has been configured or performed. iOS targets provide shared contracts and algorithms only; no iOS BLE backend is implemented.

## Permissions

The host owns manifest declarations, runtime requests, rationale, settings navigation, and lifecycle decisions. For a modern-target host with physical proximity estimation:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Use `requiredPermissions()` for the runtime list. CONNECT is used to inspect adapter state, even though the scanner does not connect over GATT. The scanner rechecks readiness while active and handles permission changes during cleanup.

With `derivesPhysicalLocation=false`, align the host's actual data use and manifest with `neverForLocation`. Android may filter some beacons in that mode. Do not change the boolean independently of the manifest policy. Background scanning and its permission/service requirements are outside this implementation.

## Device data

`BleDevice` is declared in `src/commonMain/kotlin/com/siosin/bluecon/bluetooth/BleDevice.kt`, imported as `com.siosin.bluecon.bluetooth.BleDevice`.

| Field | Meaning |
| --- | --- |
| `id`, `name` | Observed address and advertised name; neither proves permanent identity. |
| `rawRssi`, `smoothedRssi`, `zone` | Current RSSI, EMA, and the study-case proximity range. No calibrated meter measurement. |
| `firstSeenAtMillis`, `lastSeenAtMillis` | Calendar timestamps for the current cache window. |
| `observedAtElapsedMillis` | Monotonic observation time in this boot; do not store it as a calendar date. |
| `observationCount` | Accepted Android scan reports in the current cache window, not transmitted packet count. |
| `signalHistory` | Up to 48 recent timestamped RSSI reports. A stale gap resets this window. |
| `advertisement` | Latest scan record and radio fields exposed by Android. |

Advertising details include connectability, legacy/extended format, TX power, primary/secondary PHY, advertising SID, complete/truncated data status, AD flags, advertised service UUIDs, company identifiers with manufacturer payloads, service payloads, and raw scan-record hex. Values unavailable in the record or OS remain null/empty. Android 26+ radio APIs are guarded; ScanRecord data remains available on API 24-25.

TX power uses the AD field when present and otherwise the extended advertising header. Company identifiers describe the advertising data, not a verified product manufacturer. Advertised UUIDs do not represent a completed GATT service discovery. Battery level, firmware, MTU, pairing, and connection state are not inferred from these fields. The SDK does not estimate the configured advertising interval from callback frequency.

## Resource limits and extensions

Default configuration: two-minute session, snapshots every 300 ms, stale after eight seconds, eviction after 60 seconds, and at most 500 cached devices. The signal history is capped per device. Optional service UUID filters and a higher-responsiveness scan mode are available in `ScanConfig`.

Extended advertising is enabled on API 26+ when the controller reports support, using all supported scanning PHYs. Older or unsupported devices retain legacy scanning. Set `includeExtendedAdvertisements=false` to request legacy reports only.

Implement `SignalProcessor` only when device profiling shows a need. Calls are serialized; an implementation must not block the main thread and must release per-device state in `forget`/`reset`. Heavy native work would need a worker/batching adapter. No NDK or Python runtime is included, and no performance improvement has been benchmarked.

## References

- [Android ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult)
- [Android ScanRecord](https://developer.android.com/reference/android/bluetooth/le/ScanRecord)
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [nRF Connect for Android](https://github.com/nordicsemi/Android-nRF-Connect)

Builds and tests are left to the project owner, as requested.
