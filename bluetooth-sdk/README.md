# BlueCon Bluetooth SDK

Library KMP dengan kontrak/algoritma portable dan implementasi BLE Android API 24+. SDK tidak bergantung pada UI aplikasi, database, atau DI framework.

## Dependency lokal

```kotlin
dependencies {
    implementation(project(":bluetooth-sdk"))
}
```

Untuk konsumen KMP, letakkan dependency tersebut di source set yang menggunakannya. Akses `AndroidBluetoothScanner` hanya dari source set Android. Plugin `maven-publish` sudah dipasang dengan group `com.siosin.bluecon` dan version `0.1.0`; publikasi remote belum dikonfigurasi atau dilakukan. Publikasi target iOS memerlukan host yang sesuai.

## Integrasi Android

```kotlin
// Buat satu instance sesuai lifetime fitur/aplikasi, bukan setiap recomposition.
val scanner = AndroidBluetoothScanner(
    context = applicationContext,
    derivesPhysicalLocation = true,
)

val blocker = scanner.readinessBlocker()
// Host menyelesaikan blocker dengan Activity Result API/settings sebelum Start.
if (blocker == null) {
    scanner.start(ScanConfig(durationMillis = 120_000))
}

// Collect menggunakan lifecycle/scope milik host.
scope.launch { scanner.devices.collect { devices -> /* update state */ } }
scope.launch { scanner.state.collect { state -> /* render status/error */ } }

// Hentikan saat host masuk background; kecualikan pergantian konfigurasi.
scanner.stop(StopReason.BACKGROUND)

// Panggil hanya ketika pemilik instance benar-benar selesai.
scanner.close()
```

`start` idempotent selama sesi aktif. Konfigurasi baru diterapkan pada sesi berikutnya; navigasi layar tidak memulai ulang scanner. Setelah `close`, buat instance baru jika diperlukan. Method sesi dimarshalling ke main thread; koleksi StateFlow aman dari scope konsumen. Start tidak memberikan jaminan hasil langsung: pantau `ScanState`.

## Kontrak manifest dan izin

SDK tidak menambahkan permission ke manifest konsumen. Host dengan target SDK modern dan `derivesPhysicalLocation=true` membutuhkan:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Gunakan `requiredPermissions()` untuk permission runtime sesuai OS. CONNECT diperlukan untuk status adapter. SDK memeriksa ulang izin selama sesi dan menangani SecurityException saat kondisi berubah. Penolakan permanen/rationale ditentukan oleh host; SDK tidak memiliki Activity.

Jika `derivesPhysicalLocation=false`, host harus benar-benar tidak menggunakan hasil scan untuk memperoleh lokasi fisik, memasang `neverForLocation` pada SCAN, dan membatasi izin lokasi legacy sesuai kebutuhan. Android dapat menyaring sebagian beacon dalam mode tersebut. Jangan mengubah hanya boolean tanpa menyelaraskan manifest dan penggunaan data.

## Pemrosesan dan batas

- `ScanConfig` memvalidasi durasi 1 detik hingga 10 menit; default sesi 2 menit.
- Snapshot default setiap 300 ms, stale setelah 8 detik, eviction setelah 60 detik, maksimal 500 perangkat.
- ScanFilter service UUID opsional. Untuk discovery umum gunakan set kosong; filter nama/RSSI sebaiknya dilakukan terhadap snapshot.
- `BleDevice.id` adalah alamat pengamatan, `lastSeenAtMillis` waktu untuk tampilan/persistence, dan `observedAtElapsedMillis` waktu monoton dalam boot saat ini. Jangan persist waktu monoton sebagai tanggal.
- `zone` diturunkan dari RSSI mentah sesuai tabel studi kasus; `smoothedRssi` adalah sinyal EMA untuk visualisasi. Alamat acak dapat terlihat sebagai perangkat baru.
- `Stopped` mempertahankan snapshot terakhir. Anggap hasil tersebut sebagai pengamatan terakhir, bukan pengukuran live. Sesi baru mengosongkan cache dan state EMA.
- Error frekuensi scan dikembalikan ke host. SDK tidak melakukan auto-retry atau loop restart.
- Scanner belum menyediakan GATT, pairing, background service, atau backend iOS.

## Extension point untuk NDK

Implementasikan `SignalProcessor` dan inject lewat constructor bila benchmark di perangkat membuktikan manfaatnya. Method dipanggil serial oleh scanner. Implementasi harus cepat, tidak memblokir main thread, dan tidak menyimpan state tanpa batas; `forget` dan `reset` digunakan untuk cleanup.

Untuk komputasi berat, rancang adapter pemrosesan batch di worker sebelum memindahkannya ke NDK, lalu pertahankan implementasi Kotlin sebagai fallback. Memanggil JNI per sampel tanpa pengukuran tidak dijamin lebih cepat. Implementasi NDK/Python belum disertakan.
