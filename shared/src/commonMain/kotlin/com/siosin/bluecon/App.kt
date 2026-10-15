package com.siosin.bluecon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siosin.bluecon.presentation.ScannerTab
import com.siosin.bluecon.presentation.ScannerViewModel
import com.siosin.bluecon.presentation.components.*
import com.siosin.bluecon.presentation.screens.DeviceDetailScreen
import com.siosin.bluecon.presentation.screens.HistoryScreen
import com.siosin.bluecon.presentation.screens.ScannerScreen
import com.siosin.bluecon.presentation.theme.BlueConTheme

@Composable
fun App(
    viewModel: ScannerViewModel,
    onStartRequested: () -> Unit,
    onOpenAppSettings: () -> Unit,
    hostMessage: String?,
    formatTime: (Long) -> String,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val scannerListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }
    BlueConTheme {
        val colors = MaterialTheme.colorScheme
        val bannerContent: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                hostMessage?.let { Notice(it, "App settings", onOpenAppSettings) }
                ui.history.error?.let { Notice(it) }
            }
        }
        val banners: (@Composable () -> Unit)? = if (hostMessage != null || ui.history.error != null) bannerContent else null
        Scaffold(
            modifier = Modifier.imePadding(), containerColor = colors.background,
            topBar = {
                Surface(color = colors.surfaceContainerLow) {
                    Box(Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.Center) {
                        Row(
                            Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (ui.selectedId != null) {
                                IconButton(onClick = viewModel::back, modifier = Modifier.semantics { contentDescription = "Back" }) { AppIcon(AppSymbol.BACK) }
                            } else {
                                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { AppIcon(AppSymbol.BLUETOOTH, tint = colors.primary) }
                            }
                            Text(if (ui.selectedId != null) "Device details" else "BlueCon", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            StatusTag("BLE")
                        }
                    }
                }
            },
            bottomBar = {
                if (ui.selectedId == null) NavigationBar(containerColor = colors.surfaceContainerLow) {
                    NavigationBarItem(
                        selected = ui.tab == ScannerTab.SCANNER, onClick = { viewModel.changeTab(ScannerTab.SCANNER) },
                        icon = { NavigationIcon(ScannerTab.SCANNER) }, label = { Text("Scanner") },
                    )
                    NavigationBarItem(
                        selected = ui.tab == ScannerTab.HISTORY, onClick = { viewModel.changeTab(ScannerTab.HISTORY) },
                        icon = { NavigationIcon(ScannerTab.HISTORY) }, label = { Text("History") },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    when {
                        ui.selectedId != null -> key(ui.selectedId) {
                            DeviceDetailScreen(ui, onStartRequested, viewModel::stop, formatTime, banners)
                        }
                        ui.tab == ScannerTab.SCANNER -> ScannerScreen(ui, viewModel, onStartRequested, scannerListState, banners)
                        else -> HistoryScreen(ui, viewModel, formatTime, { confirmClear = true }, historyListState, banners)
                    }
                }
            }
        }
        if (confirmClear) AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("This removes every saved observation, including devices outside your search. Devices found again will be saved again.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearHistory() }, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
