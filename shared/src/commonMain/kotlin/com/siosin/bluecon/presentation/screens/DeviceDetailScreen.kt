package com.siosin.bluecon.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.bluetooth.*
import com.siosin.bluecon.presentation.ScannerUiState
import com.siosin.bluecon.presentation.components.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailScreen(ui: ScannerUiState, onStart: () -> Unit, onStop: () -> Unit, formatTime: (Long) -> String, banners: (@Composable () -> Unit)? = null) {
    val device = ui.selectedDevice
    val saved = ui.selectedHistoryDevice
    val fresh = ui.scanning && device != null && !device.isStale
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        state = rememberLazyListState(), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (banners != null) item { banners() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(device?.name?.takeIf { it.isNotBlank() } ?: saved?.name ?: "Unnamed device", style = MaterialTheme.typography.headlineSmall)
                SelectionContainer { Text(ui.selectedId.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusTag(when { fresh -> "Advertising"; device?.isStale == true -> "No recent signal"; device != null -> "Scan stopped"; else -> "Saved observation" }, active = fresh)
                    device?.advertisement?.connectable?.let { StatusTag(if (it) "Connectable" else "Non-connectable") }
                }
            }
        }
        if (device != null) {
            item { ProximityCard(device, fresh) }
            item {
                SectionCard("Signal") {
                    MetricRow("Raw RSSI", "${device.rawRssi} dBm", monospace = true)
                    MetricRow("Smoothed RSSI", "${device.smoothedRssi.roundToInt()} dBm", monospace = true)
                    Text("Smoothed using an exponential moving average. Raw RSSI determines the proximity range.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            item { SignalHistoryCard(device) }
            item {
                SectionCard("Observation") {
                    MetricRow("First observed", formatTime(device.firstSeenAtMillis))
                    MetricRow("Last observed", formatTime(device.lastSeenAtMillis))
                    MetricRow("Reports received", "${device.observationCount}", monospace = true)
                    Text("Counts refer to reports received by this app in the current observation window.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            item {
                val ad = device.advertisement
                SectionCard("Radio") {
                    MetricRow("Connectable", when (ad.connectable) { true -> "Yes"; false -> "No"; null -> "Not reported" })
                    MetricRow("Advertising", when (ad.format) { AdvertisingFormat.LEGACY -> "Legacy"; AdvertisingFormat.EXTENDED -> "Extended"; null -> "Not reported" })
                    MetricRow("TX power", ad.txPowerDbm?.let { "$it dBm" } ?: "Not advertised", monospace = ad.txPowerDbm != null)
                    MetricRow("Primary PHY", phyLabel(ad.primaryPhy))
                    MetricRow("Secondary PHY", phyLabel(ad.secondaryPhy))
                    MetricRow("Advertising SID", ad.advertisingSid?.toString() ?: "Not reported")
                    MetricRow("Data status", when (ad.dataStatus) { AdvertisingDataStatus.COMPLETE -> "Complete"; AdvertisingDataStatus.TRUNCATED -> "Truncated"; null -> "Not reported" })
                    MetricRow("AD flags", ad.flags?.let { "0x${it.toString(16).uppercase().padStart(2, '0')}" } ?: "Not advertised", monospace = ad.flags != null)
                }
            }
            item {
                ExpandableDataSection("Advertised services", "${device.advertisement.serviceUuids.size} UUIDs", initiallyExpanded = true) {
                    if (device.advertisement.serviceUuids.isEmpty()) UnavailableData()
                    else device.advertisement.serviceUuids.forEach { SelectableValue(it) }
                }
            }
            item {
                ExpandableDataSection("Manufacturer data", "${device.advertisement.manufacturerData.size} entries") {
                    if (device.advertisement.manufacturerData.isEmpty()) UnavailableData()
                    else device.advertisement.manufacturerData.toList().sortedBy { it.first }.forEach { (id, hex) ->
                        Text("Company ID 0x${id.toString(16).uppercase().padStart(4, '0')}", style = MaterialTheme.typography.labelLarge)
                        SelectableValue(hex.ifEmpty { "Empty payload" })
                    }
                }
            }
            item {
                ExpandableDataSection("Service data", "${device.advertisement.serviceData.size} entries") {
                    if (device.advertisement.serviceData.isEmpty()) UnavailableData()
                    else device.advertisement.serviceData.forEach { (uuid, hex) ->
                        SelectableValue(uuid)
                        SelectableValue(hex.ifEmpty { "Empty payload" })
                    }
                }
            }
            item {
                ExpandableDataSection("Raw advertising", "${device.advertisement.payloadSizeBytes} bytes") {
                    device.advertisement.rawPayloadHex?.let { SelectableValue(it.ifEmpty { "Empty scan record" }) } ?: UnavailableData()
                }
            }
            item {
                Text("Details come from the last received scan record. Connectable does not mean connected. Company IDs do not verify a product's manufacturer.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        } else {
            item {
                SectionCard("Saved observation") {
                    MetricRow("Last RSSI", saved?.let { "${it.lastRssi} dBm" } ?: "Not available", monospace = saved != null)
                    MetricRow("Last seen", saved?.let { formatTime(it.lastSeenAtMillis) } ?: "Not available")
                    Text("Start a scan to collect advertising details for this address. Private addresses may change.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        item { ScanControls(ui, onStart, onStop) }
    }
}

@Composable
private fun ProximityCard(device: BleDevice, fresh: Boolean) {
    val colors = MaterialTheme.colorScheme
    val strength = ((device.smoothedRssi + 100) / 70).toFloat().coerceIn(0f, 1f)
    SectionCard("Proximity estimate") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(Modifier.size(88.dp).clearAndSetSemantics { }) {
                val stroke = 7.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(colors.outlineVariant, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (fresh) drawArc(colors.primary, 135f, 270f * strength, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (!fresh) "No live estimate" else device.zone.distanceLabel, style = MaterialTheme.typography.headlineSmall)
                Text(when { !fresh -> "Showing the last observation"; device.zone == ProximityZone.LOST -> "Below ranging threshold"; else -> device.zone.label }, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Text("RSSI-based range, not a precise distance. Obstacles and device orientation affect signal strength.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ExpandableDataSection(title: String, summary: String, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    SectionCard("") {
        TextButton(
            onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            contentPadding = PaddingValues(0.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIcon(if (expanded) AppSymbol.UP else AppSymbol.DOWN)
        }
        if (expanded) content()
    }
}

@Composable
private fun SelectableValue(value: String) {
    SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun UnavailableData() {
    Text("Not advertised", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun phyLabel(phy: BluetoothPhy?): String = when (phy) {
    BluetoothPhy.LE_1M -> "LE 1M"
    BluetoothPhy.LE_2M -> "LE 2M"
    BluetoothPhy.LE_CODED -> "LE Coded"
    null -> "Not reported"
}
