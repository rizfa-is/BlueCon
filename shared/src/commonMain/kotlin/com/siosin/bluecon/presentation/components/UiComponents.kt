package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (title.isNotEmpty()) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun StatusTag(text: String, active: Boolean = false, warning: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = when { warning -> colors.tertiaryContainer; active -> colors.primaryContainer; else -> colors.surfaceContainerHighest },
        contentColor = when { warning -> colors.onTertiaryContainer; active -> colors.onPrimaryContainer; else -> colors.onSurfaceVariant },
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun MetricRow(label: String, value: String, monospace: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(0.4f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer(Modifier.weight(0.6f)) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String) {
    SectionCard("") {
        AppIcon(AppSymbol.BLUETOOTH, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Notice(message: String, action: String? = null, onAction: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (action != null) TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
        }
    }
}
