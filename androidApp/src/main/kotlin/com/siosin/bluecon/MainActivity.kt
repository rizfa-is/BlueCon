package com.siosin.bluecon

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siosin.bluecon.bluetooth.ScanBlocker
import com.siosin.bluecon.presentation.ScannerViewModel
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val container get() = (application as BlueConApplication).container
    private val preferences by lazy { getSharedPreferences("permission-requests", MODE_PRIVATE) }
    private var hostMessage by mutableStateOf<String?>(null)
    private var activeDialog: AlertDialog? = null
    private lateinit var scannerViewModel: ScannerViewModel
    private var pendingPermissionStart = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val shouldStart = pendingPermissionStart
        pendingPermissionStart = false
        val missing = container.scanner.requiredPermissions().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            hostMessage = null
            // Do not restart after the user moved away while the system dialog was open.
            if (shouldStart && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) requestScan()
        } else {
            val permanent = missing.any { !shouldShowRequestPermissionRationale(it) }
            hostMessage = if (permanent) {
                "Sebagian izin diblokir. Izinkan Perangkat sekitar dan Lokasi presisi melalui pengaturan aplikasi."
            } else {
                "Izin belum lengkap. Scan memerlukan perangkat sekitar dan lokasi presisi untuk estimasi kedekatan. Tekan Mulai untuk mencoba lagi."
            }
            container.scanner.start() // Publishes the precise blocker; no scan starts without permission.
        }
    }

    private val bluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Return to the scanner; an explicit tap starts the next session.
        hostMessage = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ScannerViewModel::class.java)
                return container.createScannerViewModel() as T
            }
        }
        scannerViewModel = ViewModelProvider(this, factory)[ScannerViewModel::class.java]
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        setContent {
            val ui by scannerViewModel.state.collectAsStateWithLifecycle()
            BackHandler(enabled = ui.selectedId != null) { scannerViewModel.back() }
            App(
                viewModel = scannerViewModel,
                onStartRequested = ::requestScan,
                onOpenAppSettings = ::openAppSettings,
                hostMessage = hostMessage,
                formatTime = { dateFormat.format(Date(it)) },
            )
        }
    }

    private fun requestScan() {
        hostMessage = null
        when (val blocker = container.scanner.readinessBlocker()) {
            null -> scannerViewModel.start()
            is ScanBlocker.MissingPermissions -> {
                container.scanner.start()
                val permanentlyDenied = blocker.permissions.any {
                    preferences.getBoolean(it, false) && !shouldShowRequestPermissionRationale(it)
                }
                if (permanentlyDenied) {
                    hostMessage = "Izin diperlukan untuk scan. Buka pengaturan aplikasi dan izinkan Perangkat sekitar serta Lokasi presisi."
                } else {
                    showRationale(blocker.permissions)
                }
            }
            ScanBlocker.BluetoothOff -> {
                container.scanner.start()
                try {
                    bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } catch (_: SecurityException) {
                    hostMessage = "Izin Bluetooth berubah. Tekan Mulai untuk memeriksa kembali izin."
                } catch (_: android.content.ActivityNotFoundException) {
                    hostMessage = "Aktifkan Bluetooth melalui pengaturan perangkat, lalu tekan Mulai."
                }
            }
            ScanBlocker.LocationOff -> {
                container.scanner.start()
                try {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (_: android.content.ActivityNotFoundException) {
                    hostMessage = "Aktifkan layanan lokasi melalui pengaturan perangkat, lalu tekan Mulai."
                }
            }
            ScanBlocker.Unsupported -> container.scanner.start()
        }
    }

    private fun showRationale(permissions: List<String>) {
        if (activeDialog?.isShowing == true) return
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Izin untuk mencari perangkat")
            .setMessage("BlueCon memakai Bluetooth untuk memindai advertising BLE dan izin lokasi presisi untuk memperkirakan kedekatan dari RSSI. Tidak ada scan di background. Pilih lokasi presisi saat diminta.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Lanjutkan") { _, _ ->
                val editor = preferences.edit()
                permissions.forEach { editor.putBoolean(it, true) }
                editor.apply()
                pendingPermissionStart = true
                permissionLauncher.launch(permissions.toTypedArray())
            }
            .create()
        activeDialog?.show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    override fun onResume() {
        super.onResume()
        container.scanner.refreshReadiness()
        if (container.scanner.readinessBlocker() == null) hostMessage = null
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            pendingPermissionStart = false
            scannerViewModel.onBackground()
            container.flushHistory()
        }
        super.onStop()
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
        activeDialog = null
        super.onDestroy()
    }
}
