package com.siosin.bluecon.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.presentation.ScannerUiState
import com.siosin.bluecon.presentation.ScannerViewModel
import com.siosin.bluecon.presentation.components.*

@Composable
fun HistoryScreen(ui: ScannerUiState, viewModel: ScannerViewModel, formatTime: (Long) -> String, onClear: () -> Unit, listState: LazyListState, banners: (@Composable () -> Unit)? = null) {
    val visible = ui.visibleHistory
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("History", style = MaterialTheme.typography.headlineMedium)
                Text("Saved observations on this device", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (banners != null) item { banners() }
        item {
            OutlinedTextField(
                value = ui.historyQuery, onValueChange = viewModel::searchHistory,
                label = { Text("Search saved names or addresses") }, leadingIcon = { AppIcon(AppSymbol.SEARCH) },
                trailingIcon = {
                    if (ui.historyQuery.isNotEmpty()) IconButton(onClick = { viewModel.searchHistory("") }, modifier = Modifier.semantics { contentDescription = "Clear history search" }) { AppIcon(AppSymbol.CLOSE) }
                },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${visible.size} of ${ui.history.devices.size} devices", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear, enabled = ui.history.devices.isNotEmpty(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear all") }
            }
        }
        if (visible.isEmpty()) item {
            if (ui.history.devices.isEmpty()) EmptyState("No saved devices", "Devices found during a scan are saved here automatically.")
            else EmptyState("No matches", "Try a different name or address, or clear your search.")
        }
        items(visible, key = { it.id }) { device ->
            DeviceRow(device.name, device.id, device.lastRssi, "Last seen ${formatTime(device.lastSeenAtMillis)}", muted = true) { viewModel.select(device.id) }
        }
        item {
            Text("History stores observed addresses. A device using a private address may appear more than once.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
