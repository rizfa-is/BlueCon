package com.siosin.bluecon

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import java.util.Locale

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
                "Some permissions are blocked. Allow Nearby devices and precise location in app settings."
            } else {
                "Permissions are incomplete. Tap Start scan to allow Nearby devices and precise location."
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
        enableEdgeToEdge()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ScannerViewModel::class.java)
                return container.createScannerViewModel() as T
            }
        }
        scannerViewModel = ViewModelProvider(this, factory)[ScannerViewModel::class.java]
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.ENGLISH)
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
                    hostMessage = "Open app settings and allow Nearby devices and precise location before scanning."
                } else {
                    showRationale(blocker.permissions)
                }
            }
            ScanBlocker.BluetoothOff -> {
                container.scanner.start()
                try {
                    bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } catch (_: SecurityException) {
                    hostMessage = "Bluetooth permission changed. Tap Start scan to review permissions."
                } catch (_: android.content.ActivityNotFoundException) {
                    hostMessage = "Enable Bluetooth in device settings, then tap Start scan."
                }
            }
            ScanBlocker.LocationOff -> {
                container.scanner.start()
                try {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (_: android.content.ActivityNotFoundException) {
                    hostMessage = "Enable location services in device settings, then tap Start scan."
                }
            }
            ScanBlocker.Unsupported -> container.scanner.start()
        }
    }

    private fun showRationale(permissions: List<String>) {
        if (activeDialog?.isShowing == true) return
        activeDialog = AlertDialog.Builder(this)
            .setTitle("Permissions for scanning")
            .setMessage("BlueCon uses Bluetooth to scan BLE advertisements and precise location permission to estimate proximity from signal strength. Scanning stops when the app is in the background. Choose precise location when prompted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
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
