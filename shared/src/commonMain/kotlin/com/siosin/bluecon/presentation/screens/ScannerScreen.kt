package com.siosin.bluecon.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.domain.DeviceSort
import com.siosin.bluecon.bluetooth.ProximityZone
import com.siosin.bluecon.presentation.ScannerUiState
import com.siosin.bluecon.presentation.ScannerViewModel
import com.siosin.bluecon.presentation.components.*

@Composable
fun ScannerScreen(ui: ScannerUiState, viewModel: ScannerViewModel, onStart: () -> Unit, listState: LazyListState, banners: (@Composable () -> Unit)? = null) {
    val visible = remember(ui.devices, ui.query, ui.minimumRssi, ui.sort) { ui.visibleDevices }
    val keyboard = LocalSoftwareKeyboardController.current
    var sortExpanded by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Scanner", style = MaterialTheme.typography.headlineMedium)
                Text("Bluetooth Low Energy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (banners != null) item { banners() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryMetric("Observed", "${ui.devices.size}", Modifier.weight(1f))
                SummaryMetric("Named", "${ui.devices.count { !it.name.isNullOrBlank() }}", Modifier.weight(1f))
                SummaryMetric(if (ui.scanning) "Recent" else "Last scan", "${ui.devices.count { !it.isStale }}", Modifier.weight(1f))
            }
        }
        item { ScanControls(ui, onStart, viewModel::stop) }
        item {
            OutlinedTextField(
                value = ui.query, onValueChange = viewModel::search,
                label = { Text("Name or address") }, leadingIcon = { AppIcon(AppSymbol.SEARCH) },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) IconButton(onClick = { viewModel.search("") }, modifier = Modifier.semantics { contentDescription = "Clear search" }) { AppIcon(AppSymbol.CLOSE) }
                },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { sortExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "Sort devices: ${sortLabel(ui.sort)}" }) {
                        AppIcon(AppSymbol.SORT); Spacer(Modifier.width(8.dp)); Text(sortLabel(ui.sort))
                    }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        DeviceSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortLabel(option)) },
                                modifier = Modifier.semantics { selected = ui.sort == option },
                                leadingIcon = { if (ui.sort == option) AppIcon(AppSymbol.CHECK) },
                                onClick = { viewModel.setSort(option); sortExpanded = false },
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { filterExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = ui.minimumRssi?.let { "Signal filter: at least $it dBm" } ?: "Signal filter: any strength" }) {
                        AppIcon(AppSymbol.FILTER); Spacer(Modifier.width(8.dp)); Text(ui.minimumRssi?.let { "≥ $it dBm" } ?: "Any signal")
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf(null, -80, -60).forEach { threshold ->
                            DropdownMenuItem(
                                text = { Text(threshold?.let { "At least $it dBm" } ?: "Any signal strength") },
                                modifier = Modifier.semantics { selected = ui.minimumRssi == threshold },
                                leadingIcon = { if (ui.minimumRssi == threshold) AppIcon(AppSymbol.CHECK) },
                                onClick = { viewModel.setMinimumRssi(threshold); filterExpanded = false },
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Devices", style = MaterialTheme.typography.titleMedium)
                Text("${visible.size} shown", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (visible.isEmpty()) item {
            when {
                ui.devices.isNotEmpty() -> EmptyState("No matches", "Try a different name, address, or signal filter.")
                ui.scanning -> EmptyState("No devices yet", "Only devices broadcasting BLE advertisements will appear. Keep the device nearby and check that it is advertising.")
                else -> EmptyState("No observations", "Start a scan to find nearby BLE devices.")
            }
        }
        items(visible, key = { it.id }) { device ->
            val proximity = if (device.zone == ProximityZone.LOST) "Very weak signal" else device.zone.label
            val summary = when {
                !ui.scanning -> "Last observation · $proximity"
                device.isStale -> "No recent signal"
                device.advertisement.connectable == true -> "$proximity · Connectable"
                else -> proximity
            }
            DeviceRow(device.name, device.id, device.rawRssi, summary, muted = device.isStale || !ui.scanning) { viewModel.select(device.id) }
        }
        item {
            Text("Devices without recent reports appear last. Addresses may change between scans.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun sortLabel(sort: DeviceSort): String = when (sort) {
    DeviceSort.NAMED_FIRST -> "Named first"
    DeviceSort.SIGNAL_STRENGTH -> "Strongest signal"
    DeviceSort.NAME -> "Name A–Z"
    DeviceSort.LAST_SEEN -> "Most recent"
}
