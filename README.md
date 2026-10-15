# BlueCon

Aplikasi pelacak kedekatan BLE dengan SDK KMP terpisah. Implementasi Bluetooth dan aplikasi yang aktif dikembangkan saat ini berfokus pada Android (minimum API 24).

## Modul dan arsitektur

```text
androidApp (composition root, Android permission UI, Room)
    -> shared (Compose UI + MVVM, use case, repository contracts)
        -> bluetooth-sdk (portable contracts/algorithms + Android BLE adapter)
```

- `bluetooth-sdk/commonMain`: kontrak scanner, model perangkat, konfigurasi sesi, klasifikasi zona RSSI, EMA, dan cache perangkat dengan batas kapasitas serta expiry berdasarkan waktu monoton.
- `bluetooth-sdk/androidMain`: implementasi `BluetoothLeScanner`, permission policy berdasarkan API level, pengecekan Bluetooth/lokasi, callback dan cleanup. Tidak bergantung pada Compose, Room, Activity, atau DI framework.
- `shared/domain`: `ScannerRepository`, `DeviceHistoryRepository`, kebijakan sesi `ScanDevices`, serta pencarian dan pengurutan perangkat. Tidak mengimpor Android/JVM API.
- `shared/data`: adapter SDK ke kontrak repository aplikasi.
- `shared/presentation`: `ScannerViewModel` dan state UI yang immutable. UI mengirim aksi ke ViewModel; data diobservasi melalui StateFlow.
- `androidApp`: constructor injection melalui `AppContainer`, Activity Result API untuk izin, integrasi lifecycle, dan implementasi persistence melalui Room.

Model dan algoritma SDK menggunakan source set KMP. Target iOS tetap tersedia untuk kontrak/algoritma bersama, tetapi belum memiliki backend Bluetooth. Entry point aplikasi iOS menampilkan informasi dukungan Android saja.

## Fitur

- Tema light/dark otomatis mengikuti pengaturan perangkat, termasuk warna kartu, radar, dialog, navigasi, dan ikon system bar.
- Ikon bottom navigation digambar sebagai vektor berukuran 32 dp agar jelas pada berbagai perangkat; warna mengikuti tema dan status pilihan tab.
- Mulai/hentikan pemindaian BLE advertising dengan satu sesi bersama.
- Nama advertised bila tersedia, alamat pengamatan, RSSI mentah, RSSI EMA, dan perkiraan rentang jarak.
- Pilihan urutan perangkat: bernama lebih dulu (default), sinyal terkuat, nama A-Z, atau terbaru. Perangkat stale tetap di bawah; pencarian nama/MAC dan filter minimum -80 atau -60 dBm tetap berlaku.
- Layar radar untuk satu target, indikator kedekatan tanpa mengklaim informasi arah.
- Target stale setelah 8 detik tanpa advertising; cache membuang target setelah 60 detik.
- Riwayat lokal: nama, alamat, RSSI terakhir, dan waktu terakhir terlihat. Maksimal 2.000 entri, dengan pencarian nama/MAC yang terpisah dari pencarian scanner.
- Penanganan hardware tidak tersedia, Bluetooth/lokasi mati, izin ditolak atau dicabut, dan error scanner termasuk pembatasan frekuensi scan dari OS.

## Lifecycle dan optimasi

- Sesi foreground dibatasi 2 menit; pengguna dapat memulai sesi berikutnya secara eksplisit.
- Scan berhenti saat Activity tidak terlihat. Setelah kembali, tekan Mulai untuk melanjutkan. Rotasi tidak menghentikan sesi; ViewModel mempertahankan filter dan pilihan target.
- Semua subscriber berbagi satu scanner. Mengoleksi Flow tidak memulai scan tambahan. Memanggil Start saat sesi aktif tidak membuat sesi baru.
- Callback diproses secara serial, snapshot UI diterbitkan setiap 300 ms, dan cache dibatasi 500 perangkat. Mode default `SCAN_MODE_BALANCED`; konsumen SDK dapat memilih responsivitas lebih tinggi melalui `ScanConfig`.
- EMA per perangkat menggunakan alpha 0,25. Raw RSSI tetap dipertahankan; zona dan filter menggunakan raw RSSI, sementara posisi indikator radar memakai EMA.
- Waktu monoton digunakan untuk expiry dan deduplikasi penulisan, sehingga perubahan jam perangkat tidak mengubah timeout target.
- Room menulis batch setiap 2 detik selama scan, lalu melakukan flush saat sesi berhenti/Activity masuk background. Tidak ada loop penulisan ketika scanner idle.
- Tidak ada Python interpreter atau library NDK dalam implementasi awal. `SignalProcessor` adalah extension point untuk implementasi lain setelah profiling membuktikan kebutuhan. Tidak ada klaim peningkatan performa yang sudah diukur.

## Permission Android

Aplikasi contoh menggunakan kebijakan konservatif karena hasil scan dipakai memperkirakan kedekatan fisik. `neverForLocation` sengaja tidak dipasang.

| OS perangkat | Permission runtime foreground |
| --- | --- |
| API 24-30 | `ACCESS_FINE_LOCATION` |
| API 31+ | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` |

Permission Bluetooth legacy di manifest dibatasi `maxSdkVersion=30`. `BLUETOOTH_CONNECT` dibutuhkan oleh pengecekan status adapter; aplikasi ini tidak membuat koneksi GATT. Pada API 31+, coarse dan fine diminta bersama agar pengguna dapat memilih lokasi presisi. Layanan lokasi harus aktif untuk kebijakan ini.

SDK hanya menghitung kebutuhan dan memeriksa izin. Rationale, dialog runtime, tombol pengaturan, dan permintaan menyalakan Bluetooth dimiliki aplikasi host. Penolakan yang tidak dapat diminta kembali diarahkan ke pengaturan aplikasi. Kembali dari pengaturan Bluetooth/lokasi tidak otomatis memulai scan.

Tidak ada permission background location, foreground service, advertise, atau notifikasi karena kemampuan tersebut belum digunakan. Konsumen SDK yang memilih `derivesPhysicalLocation=false` harus menyesuaikan manifest dengan `neverForLocation`, tidak memperoleh lokasi fisik dari hasil scan, dan menerima potensi penyaringan beacon oleh Android.

## Teknologi

Kotlin Multiplatform, Compose Multiplatform Material 3, AndroidX ViewModel, Kotlin Coroutines/StateFlow, Android BLE API, dan Room. Versi berada di `gradle/libs.versions.toml`.

Persistence Android memakai Entity, DAO, dan Database Kotlin dengan Kotlin Symbol Processing (KSP2) untuk menghasilkan implementasi Room. KSP diterapkan hanya pada modul `androidApp`; SDK tidak membawa dependensi Room atau annotation processing. Operasi DAO tetap dijalankan melalui repository di dispatcher IO, dengan transaksi untuk upsert dan trim. Schema database saat ini versi 1; ekspor schema belum diaktifkan dan perubahan berikutnya memerlukan migration yang sesuai.

## Menjalankan sendiri

1. Buka repository di Android Studio yang mendukung AGP dan Kotlin sesuai version catalog.
2. Gunakan JDK yang kompatibel dengan Gradle wrapper dan AGP proyek. Pasang Android SDK sesuai `android-compileSdk` (saat ini 37).
3. Isi lokasi Android SDK di `local.properties` jika belum tersedia.
4. Jalankan konfigurasi `androidApp` di perangkat fisik Android API 24+ dengan BLE. Izinkan permission yang diminta dan aktifkan Bluetooth/lokasi.
5. Sediakan peripheral yang sedang advertising, misalnya BLE beacon. Perangkat yang hanya menyediakan Bluetooth Classic tidak akan muncul.

Perintah berikut disediakan untuk dijalankan pemilik proyek; build/test implementasi ini belum dijalankan sesuai permintaan:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :bluetooth-sdk:testAndroidHostTest :shared:testAndroidHostTest
```

APK debug dihasilkan di `androidApp/build/outputs/apk/debug/`. Tidak ada APK baru yang disertakan dalam perubahan ini.

## Validasi

Test source mencakup batas zona RSSI, silent target/expiry, perubahan jam, callback terlambat, pemulihan smoothing, batas kapasitas, RSSI tidak tersedia, permission policy lintas versi, pencarian/pengurutan, kebijakan background, serta propagasi state ViewModel. Test tersebut belum dieksekusi.

Uji manual di perangkat fisik pada kelompok API 24-28, 29-30, 31-32, 33, dan 34+ dengan skenario:

- Izin awal, penolakan, penolakan permanen, hanya lokasi perkiraan, dan pencabutan izin melalui Settings.
- Bluetooth/lokasi dimatikan selama scan, lalu diaktifkan kembali.
- Rotasi ketika scan aktif dan saat menampilkan target; navigasi background/foreground.
- Peripheral berhenti advertising dan kembali muncul; pastikan stale dan recovery benar.
- Pencarian, setiap pilihan urutan, dan ambang RSSI; periksa pencarian scanner/riwayat tetap terpisah saat berpindah tab; tutup/buka aplikasi untuk memeriksa persistence; hapus riwayat.
- Scan Stop/Start berulang untuk memeriksa feedback pembatasan OS tanpa auto-retry loop.
- Ganti mode light/dark perangkat pada scanner, radar, dan riwayat; periksa keterbacaan teks, kontrol, dan ikon status/navigation bar.

## Batasan dan asumsi

- RSSI tidak memberikan jarak meter yang presisi. Rentang mengikuti tabel studi kasus dan belum dikalibrasi terhadap hardware/lingkungan. Pada batas yang tumpang tindih, nilai masuk zona lebih kuat: -30 sangat dekat, -50 dekat, -70 cukup dekat, -80 jauh, -90 sangat jauh.
- Radar tidak mengukur arah atau koordinat. Zona berubah sesuai raw RSSI; EMA hanya menghaluskan indikator visual sehingga angka raw tidak disamarkan.
- MAC pengamatan dapat berubah karena privacy addressing. Nama, UUID service, dan MAC bukan jaminan identitas fisik permanen. Tracking lintas pergantian alamat membutuhkan protokol identitas peripheral.
- Unfiltered advertising scan dapat dibatasi Android ketika layar mati. Aplikasi ini tidak menjanjikan pemindaian background atau kelangsungan setelah process death.
- Batching persistence dapat kehilangan pengamatan terakhir yang belum di-flush jika proses dihentikan paksa. Cache bounded juga bukan arsip setiap paket advertising di lingkungan yang sangat padat.
- Tidak ada pairing, koneksi GATT, Bluetooth Classic, background service, atau backend Bluetooth iOS.
- Database baru belum diuji pada perangkat dan UI belum diverifikasi melalui build/runtime. Pemilik proyek melakukan build sesuai kesepakatan.

## Referensi

- [Permission Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Menemukan perangkat BLE](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices)
- [BLE di background](https://developer.android.com/develop/connectivity/bluetooth/ble/background)
- [Android KMP library plugin dan publikasi](https://developer.android.com/kotlin/multiplatform/plugin)
- [Room](https://developer.android.com/jetpack/androidx/releases/room)
- [JNI dan biaya perpindahan data](https://developer.android.com/ndk/guides/jni-tips)

Lihat [panduan SDK](bluetooth-sdk/README.md) untuk integrasi di modul lain.
