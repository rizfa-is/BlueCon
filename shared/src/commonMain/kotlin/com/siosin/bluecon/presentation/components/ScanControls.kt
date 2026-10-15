package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.bluetooth.ScanBlocker
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.presentation.ScannerUiState

@Composable
fun ScanControls(ui: ScannerUiState, onStart: () -> Unit, onStop: () -> Unit) {
    SectionCard("") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Scan session", style = MaterialTheme.typography.titleMedium)
            Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                StatusTag(scanTitle(ui.scanState), active = ui.scanning, warning = ui.scanState is ScanState.Failed || ui.scanState is ScanState.Blocked)
            }
        }
        Text(scanMessage(ui.scanState), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (ui.scanning) {
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Stop scan") }
        } else {
            Button(onClick = onStart, enabled = ui.scanState != ScanState.Closed, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start scan") }
        }
    }
}

private fun scanTitle(state: ScanState): String = when (state) {
    ScanState.Idle -> "Idle"
    ScanState.Scanning -> "Scanning"
    ScanState.Closed -> "Closed"
    is ScanState.Stopped -> "Stopped"
    is ScanState.Blocked -> "Action required"
    is ScanState.Failed -> "Scan failed"
}

private fun scanMessage(state: ScanState): String = when (state) {
    ScanState.Idle -> "Scan for nearby devices broadcasting BLE advertisements."
    ScanState.Scanning -> "Listening for BLE advertisements. This session ends after two minutes."
    ScanState.Closed -> "The scanner has been closed. Reopen the app to scan again."
    is ScanState.Failed -> state.message
    is ScanState.Stopped -> when (state.reason) {
        StopReason.USER -> "Scan stopped. The last observations remain available."
        StopReason.BACKGROUND -> "Scan stopped while the app was in the background. Start a new session to continue."
        StopReason.TIMEOUT -> "The two-minute session ended. Start a new scan if needed."
    }
    is ScanState.Blocked -> when (state.reason) {
        ScanBlocker.Unsupported -> "This device does not support Bluetooth Low Energy."
        ScanBlocker.BluetoothOff -> "Bluetooth is off. Tap Start scan to enable it."
        ScanBlocker.LocationOff -> "Location services are off. Tap Start scan to open settings."
        is ScanBlocker.MissingPermissions -> "Nearby devices and precise location permissions are required. Tap Start scan to review them."
    }
}
