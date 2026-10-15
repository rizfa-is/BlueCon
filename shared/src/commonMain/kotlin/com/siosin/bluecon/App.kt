package com.siosin.bluecon

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ProximityZone
import com.siosin.bluecon.bluetooth.ScanBlocker
import com.siosin.bluecon.bluetooth.ScanState
import com.siosin.bluecon.bluetooth.StopReason
import com.siosin.bluecon.domain.DeviceSort
import com.siosin.bluecon.presentation.ScannerTab
import com.siosin.bluecon.presentation.ScannerUiState
import com.siosin.bluecon.presentation.ScannerViewModel
import com.siosin.bluecon.presentation.components.NavigationIcon
import com.siosin.bluecon.presentation.theme.BlueConTheme
import kotlin.math.roundToInt

@Composable
fun App(
    viewModel: ScannerViewModel,
    onStartRequested: () -> Unit,
    onOpenAppSettings: () -> Unit,
    hostMessage: String?,
    formatTime: (Long) -> String,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    BlueConTheme {
        val colors = MaterialTheme.colorScheme
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                if (ui.selectedId == null) NavigationBar(containerColor = colors.surfaceContainerLow) {
                    NavigationBarItem(
                        selected = ui.tab == ScannerTab.SCANNER,
                        onClick = { viewModel.changeTab(ScannerTab.SCANNER) },
                        icon = { NavigationIcon(ScannerTab.SCANNER) }, label = { Text("Scanner") },
                    )
                    NavigationBarItem(
                        selected = ui.tab == ScannerTab.HISTORY,
                        onClick = { viewModel.changeTab(ScannerTab.HISTORY) },
                        icon = { NavigationIcon(ScannerTab.HISTORY) }, label = { Text("Riwayat") },
                    )
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                hostMessage?.let { Notice(it, "Pengaturan aplikasi", onOpenAppSettings) }
                ui.history.error?.let { Notice(it) }
                if (ui.selectedId != null) {
                    RadarScreen(ui, viewModel::back, onStartRequested, viewModel::stop, formatTime)
                } else {
                    Header(if (ui.tab == ScannerTab.SCANNER) "Temukan yang dekat." else "Jejak perangkat.")
                    if (ui.tab == ScannerTab.SCANNER) ScannerScreen(ui, viewModel, onStartRequested)
                    else HistoryScreen(ui, viewModel::searchHistory, formatTime) { confirmClear = true }
                }
            }
        }
        if (confirmClear) AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus riwayat?") },
            text = { Text("Seluruh riwayat tersimpan akan dihapus. Perangkat yang terdeteksi kembali akan dicatat lagi.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; viewModel.clearHistory() }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun Header(title: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text("BLUECON  /  BLUETOOTH EXPLORER", color = colors.primary, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Kenali sinyal di sekitarmu.", color = colors.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun Notice(message: String, action: String? = null, onAction: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.tertiaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(message, color = colors.onTertiaryContainer, fontSize = 13.sp)
            if (action != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun ScannerScreen(ui: ScannerUiState, viewModel: ScannerViewModel, onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val visible = remember(ui.devices, ui.query, ui.minimumRssi, ui.sort) { ui.visibleDevices }
    var sortExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScanControl(ui, onStart, viewModel::stop) }
        item {
            OutlinedTextField(
                value = ui.query, onValueChange = viewModel::search,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Cari nama atau MAC address") }, shape = RoundedCornerShape(14.dp),
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = ui.minimumRssi == null, onClick = { viewModel.setMinimumRssi(null) }, label = { Text("Semua") })
                FilterChip(selected = ui.minimumRssi == -80, onClick = { viewModel.setMinimumRssi(-80) }, label = { Text("≥ -80 dBm") })
                FilterChip(selected = ui.minimumRssi == -60, onClick = { viewModel.setMinimumRssi(-60) }, label = { Text("≥ -60 dBm") })
            }
        }
        item {
            Box {
                OutlinedButton(onClick = { sortExpanded = true }) {
                    Text("Urutkan: ${sortLabel(ui.sort)} ▾")
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    DeviceSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(option)) },
                            leadingIcon = { Text(if (ui.sort == option) "✓" else "") },
                            onClick = { viewModel.setSort(option); sortExpanded = false },
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PERANGKAT SEKITAR", color = colors.onSurfaceVariant, fontSize = 11.sp, letterSpacing = 1.sp)
                Text("${visible.size} ditemukan", color = colors.primary, fontSize = 12.sp)
            }
        }
        if (visible.isEmpty()) item {
            EmptyState(
                if (ui.scanning) "Mendengarkan sinyal…" else "Ruang untuk penemuan baru.",
                if (ui.devices.isNotEmpty()) "Tidak ada perangkat yang cocok dengan filter." else
                    "Mulai scan untuk melihat perangkat BLE yang sedang advertising. Tidak semua perangkat Bluetooth memancarkan advertising.",
            )
        }
        items(visible, key = { it.id }) { device -> DeviceCard(device) { viewModel.select(device.id) } }
        item { Text("${sortLabel(ui.sort)} • Perangkat tanpa sinyal terbaru di bawah • Sesi maksimal 2 menit", color = colors.onSurfaceVariant, fontSize = 11.sp) }
    }
}

@Composable
private fun ScanControl(ui: ScannerUiState, onStart: () -> Unit, onStop: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(if (ui.scanning) colors.primary else colors.onSurfaceVariant, CircleShape))
                Text(if (ui.scanning) "SCANNER AKTIF" else "SCANNER SIAP", color = colors.primary, fontSize = 11.sp, letterSpacing = 1.sp)
            }
            Text(scanMessage(ui.scanState), fontSize = 14.sp, color = colors.onSurfaceVariant)
            if (ui.scanning) OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Hentikan scan") }
            else Button(onClick = onStart, enabled = ui.scanState != ScanState.Closed, modifier = Modifier.fillMaxWidth()) { Text("Mulai scan") }
        }
    }
}

@Composable
private fun DeviceCard(device: BleDevice, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalBars(device.rawRssi, device.isStale, Modifier.size(34.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(device.name ?: "Perangkat tanpa nama", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.id, color = colors.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(if (device.isStale) "Tidak ada sinyal terbaru" else "${device.zone.label} · ≈ ${device.zone.distanceLabel}", color = zoneColor(device.zone), fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${device.rawRssi}", color = if (device.isStale) colors.onSurfaceVariant else colors.primary, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                Text("raw dBm", color = colors.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SignalBars(rssi: Int, stale: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val active = if (stale || rssi < -90) 0 else when { rssi >= -50 -> 4; rssi >= -70 -> 3; rssi >= -80 -> 2; else -> 1 }
        val barWidth = size.width / 7
        repeat(4) { i ->
            val height = size.height * (i + 1) / 4
            drawRoundRect(
                if (i < active) colors.primary else colors.outlineVariant,
                topLeft = Offset(i * barWidth * 1.8f, size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun RadarScreen(ui: ScannerUiState, onBack: () -> Unit, onStart: () -> Unit, onStop: () -> Unit, formatTime: (Long) -> String) {
    val colors = MaterialTheme.colorScheme
    val device = ui.selectedDevice
    val live = ui.scanning && device != null && !device.isStale
    val signal by animateFloatAsState(
        targetValue = if (live) ((device!!.smoothedRssi + 100) / 70).toFloat().coerceIn(0.05f, 1f) else 0f,
        label = "proximity",
    )
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
        item { TextButton(onClick = onBack) { Text("← Kembali ke scanner") } }
        item {
            Text("PELACAKAN TARGET", color = colors.primary, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(device?.name ?: "Perangkat target", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(ui.selectedId.orEmpty(), color = colors.onSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(270.dp)) {
                    val radius = size.minDimension / 2 - 8.dp.toPx()
                    repeat(4) { i -> drawCircle(colors.primary.copy(alpha = 0.1f + i * 0.025f), radius * (i + 1) / 4, style = Stroke(1.dp.toPx())) }
                    drawLine(colors.primary.copy(alpha = 0.12f), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y))
                    drawLine(colors.primary.copy(alpha = 0.12f), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius))
                    drawCircle(colors.primary.copy(alpha = 0.06f), radius * (0.2f + signal * 0.65f))
                    drawCircle(if (live) colors.primary else colors.onSurfaceVariant, 7.dp.toPx())
                    if (live) {
                        // Radial indicator only; RSSI provides no bearing/angle.
                        val point = Offset(center.x, center.y - radius * (1 - signal * 0.85f))
                        drawCircle(colors.primary.copy(alpha = 0.16f), 17.dp.toPx(), point)
                        drawCircle(colors.primary, 6.dp.toPx(), point)
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when { !ui.scanning -> "Pelacakan dijeda"; device == null || device.isStale -> "Sinyal hilang"; else -> device.zone.label },
                    color = if (live) zoneColor(device!!.zone) else colors.onSurfaceVariant, fontSize = 25.sp, fontWeight = FontWeight.SemiBold,
                )
                Text(if (live) "≈ ${device!!.zone.distanceLabel}" else "Menunggu pengamatan terbaru", color = colors.onSurfaceVariant)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metric("RSSI mentah", device?.let { "${it.rawRssi} dBm" } ?: "—")
                    Metric("RSSI diperhalus (EMA)", device?.let { "${it.smoothedRssi.roundToInt()} dBm" } ?: "—")
                    Metric("Terakhir terlihat", device?.let { formatTime(it.lastSeenAtMillis) } ?: "—")
                    Metric("Mode", "Advertising BLE · tanpa koneksi")
                }
            }
        }
        item { Text("Jarak adalah perkiraan berdasarkan zona RSSI, bukan pengukuran presisi. Radar menunjukkan kedekatan tanpa informasi arah.", color = colors.onSurfaceVariant, fontSize = 12.sp) }
        item { ScanControl(ui, onStart, onStop) }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HistoryScreen(ui: ScannerUiState, onQueryChange: (String) -> Unit, formatTime: (Long) -> String, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val devices = ui.history.devices
    val visible = remember(devices, ui.historyQuery) { ui.visibleHistory }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = ui.historyQuery, onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Cari riwayat nama atau MAC address") }, shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (ui.historyQuery.isNotEmpty()) TextButton(onClick = { onQueryChange("") }) { Text("Hapus") }
                },
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${visible.size} dari ${devices.size} perangkat", color = colors.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear, enabled = devices.isNotEmpty()) { Text("Hapus semua") }
            }
        }
        if (visible.isEmpty()) item {
            if (devices.isEmpty()) EmptyState("Belum ada jejak.", "Perangkat yang terdeteksi akan tersimpan di sini, termasuk setelah aplikasi ditutup.")
            else EmptyState("Tidak ada hasil.", "Coba nama atau MAC address lain, atau hapus pencarian.")
        }
        items(visible, key = { it.id }) { device ->
            Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(device.name ?: "Perangkat tanpa nama", fontWeight = FontWeight.SemiBold)
                    Text(device.id, color = colors.onSurfaceVariant, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Terakhir terlihat · ${formatTime(device.lastSeenAtMillis)}", color = colors.onSurfaceVariant, fontSize = 12.sp)
                    Text("RSSI terakhir ${device.lastRssi} dBm", color = colors.primary, fontSize = 12.sp)
                }
            }
        }
        item { Text("Alamat BLE dapat berubah. Riwayat berdasarkan alamat pengamatan, bukan identitas permanen perangkat.", color = colors.onSurfaceVariant, fontSize = 11.sp) }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("◎", color = colors.primary.copy(alpha = 0.7f), fontSize = 54.sp)
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(message, color = colors.onSurfaceVariant, fontSize = 13.sp)
    }
}

private fun scanMessage(state: ScanState): String = when (state) {
    ScanState.Idle -> "Pindai advertising BLE untuk menemukan perangkat dan memantau kekuatan sinyal."
    ScanState.Scanning -> "Mendengarkan advertising BLE. Perubahan RSSI muncul secara langsung."
    ScanState.Closed -> "Sesi scanner telah ditutup."
    is ScanState.Failed -> state.message
    is ScanState.Stopped -> when (state.reason) {
        StopReason.USER -> "Scan dihentikan. Hasil terakhir tetap tersedia."
        StopReason.BACKGROUND -> "Scan dijeda saat aplikasi masuk background. Tekan Mulai untuk melanjutkan."
        StopReason.TIMEOUT -> "Sesi 2 menit selesai. Mulai sesi baru jika masih diperlukan."
    }
    is ScanState.Blocked -> when (state.reason) {
        ScanBlocker.Unsupported -> "Perangkat ini tidak mendukung Bluetooth LE."
        ScanBlocker.BluetoothOff -> "Bluetooth sedang mati. Tekan Mulai untuk mengaktifkannya."
        ScanBlocker.LocationOff -> "Layanan lokasi diperlukan untuk mode estimasi kedekatan. Tekan Mulai untuk membuka pengaturan."
        is ScanBlocker.MissingPermissions -> "Izin perangkat sekitar dan lokasi diperlukan. Tekan Mulai untuk melanjutkan."
    }
}

@Composable
private fun zoneColor(zone: ProximityZone): Color {
    val colors = MaterialTheme.colorScheme
    return when (zone) {
        ProximityZone.VERY_CLOSE, ProximityZone.CLOSE, ProximityZone.NEARBY -> colors.primary
        ProximityZone.FAR, ProximityZone.VERY_FAR -> colors.tertiary
        ProximityZone.LOST -> colors.onSurfaceVariant
    }
}

private fun sortLabel(sort: DeviceSort): String = when (sort) {
    DeviceSort.NAMED_FIRST -> "Bernama dulu"
    DeviceSort.SIGNAL_STRENGTH -> "Sinyal terkuat"
    DeviceSort.NAME -> "Nama A–Z"
    DeviceSort.LAST_SEEN -> "Terbaru"
}
