package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeviceRow(name: String?, address: String, rssi: Int, summary: String, muted: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer, shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { AppIcon(AppSymbol.BLUETOOTH) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name?.takeIf { it.isNotBlank() } ?: "Unnamed device", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$rssi", fontSize = 22.sp, fontFamily = FontFamily.Monospace, color = if (muted) colors.onSurfaceVariant else colors.primary)
                Text("dBm", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            AppIcon(AppSymbol.NEXT, tint = colors.onSurfaceVariant)
        }
    }
}
