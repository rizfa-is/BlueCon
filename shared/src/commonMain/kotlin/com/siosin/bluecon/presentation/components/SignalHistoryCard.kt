package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.RssiSample
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun SignalHistoryCard(device: BleDevice) {
    val colors = MaterialTheme.colorScheme
    var pausedSamples by remember(device.id) { mutableStateOf<List<RssiSample>?>(null) }
    var showReports by remember(device.id) { mutableStateOf(false) }
    val samples = pausedSamples ?: device.signalHistory
    val minimum = samples.minOfOrNull { it.rssi } ?: device.rawRssi
    val maximum = samples.maxOfOrNull { it.rssi } ?: device.rawRssi
    val latest = samples.lastOrNull()?.rssi ?: device.rawRssi
    val firstTime = samples.firstOrNull()?.elapsedMillis ?: 0L
    val lastTime = samples.lastOrNull()?.elapsedMillis ?: 0L
    val windowTenths = (lastTime - firstTime).coerceAtLeast(0) / 100
    val canPlot = samples.size >= 4 && lastTime > firstTime
    val top = maxOf(-20, ceil(maximum / 10.0).toInt() * 10 + 10)
    val bottom = minOf(-100, floor(minimum / 10.0).toInt() * 10 - 10)
    SectionCard("") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Signal history", style = MaterialTheme.typography.titleMedium)
                Text(if (pausedSamples != null) "Chart paused" else "${samples.size} recent reports", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            TextButton(
                enabled = samples.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp),
                onClick = { pausedSamples = if (pausedSamples == null) device.signalHistory.toList() else null },
            ) {
                AppIcon(if (pausedSamples == null) AppSymbol.PAUSE else AppSymbol.PLAY)
                Spacer(Modifier.width(8.dp))
                Text(if (pausedSamples == null) "Pause" else "Resume")
            }
        }
        if (canPlot) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.height(160.dp).width(36.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf(top, (top + bottom) / 2, bottom).forEach {
                        Text("$it", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant)
                    }
                }
                Canvas(Modifier.weight(1f).height(160.dp).semantics {
                    contentDescription = "RSSI history, ${samples.size} reports. Latest $latest dBm. Minimum $minimum, maximum $maximum dBm. Use Show report values for the readings."
                }) {
                    repeat(3) { index ->
                        val y = size.height * index / 2
                        drawLine(colors.outlineVariant, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    val points = samples.map { sample ->
                        Offset(
                            (sample.elapsedMillis - firstTime).toFloat() / (lastTime - firstTime) * size.width,
                            (top - sample.rssi).toFloat() / (top - bottom) * size.height,
                        )
                    }
                    val path = Path().apply {
                        points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
                    }
                    val area = Path().apply {
                        moveTo(points.first().x, size.height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, size.height)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.18f), colors.primary.copy(alpha = 0.02f))))
                    drawPath(path, colors.primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(colors.primary, 3.dp.toPx(), points.last())
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${windowTenths / 10}.${windowTenths % 10} s window", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text("Latest", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        } else {
            Text("At least four timestamped reports are needed for a trend.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        MetricRow("RSSI range", "$minimum to $maximum dBm", monospace = true)
        TextButton(onClick = { showReports = !showReports }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (showReports) "Hide report values" else "Show report values")
            Spacer(Modifier.width(8.dp))
            AppIcon(if (showReports) AppSymbol.UP else AppSymbol.DOWN)
        }
        if (showReports) {
            Text("Offsets are relative to the latest displayed report.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            samples.asReversed().forEach { sample ->
                val offsetMillis = (lastTime - sample.elapsedMillis).coerceAtLeast(0)
                MetricRow(if (offsetMillis == 0L) "Latest" else "-$offsetMillis ms", "${sample.rssi} dBm", monospace = true)
            }
        }
    }
}
