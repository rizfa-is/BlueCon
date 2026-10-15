package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.bluetooth.BleDevice
import com.siosin.bluecon.bluetooth.ProximityZone

// Ring spacing represents categories rather than a linear scale of metres.
private val radarBands = listOf(
    ProximityZone.VERY_CLOSE,
    ProximityZone.CLOSE,
    ProximityZone.NEARBY,
    ProximityZone.FAR,
    ProximityZone.VERY_FAR,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProximityRadarCard(device: BleDevice, scanning: Boolean, formatTime: (Long) -> String) {
    val colors = MaterialTheme.colorScheme
    val fresh = scanning && !device.isStale
    val activeBand = if (fresh) radarBands.indexOf(device.zone).takeIf { it >= 0 } else null
    val status = when {
        !scanning -> "Scan stopped"
        device.isStale -> "No recent signal"
        activeBand == null -> "Below ranging threshold"
        else -> device.zone.label
    }
    SectionCard("Proximity radar") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (activeBand != null) device.zone.distanceLabel else "No live estimate", style = MaterialTheme.typography.headlineSmall)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        MetricRow(if (fresh) "Raw RSSI" else "Last RSSI", "${device.rawRssi} dBm", monospace = true)
        RadarPlot(activeBand, device.rawRssi, status)
        Text("Range bands · nearest to farthest", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            radarBands.forEachIndexed { index, zone ->
                val active = activeBand == index
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) colors.primaryContainer else colors.surfaceContainer,
                    contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    border = if (active) BorderStroke(1.dp, colors.primary) else null,
                ) {
                    Text(
                        "${index + 1} · ${zone.distanceLabel}${if (active) " · Current" else ""}",
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        MetricRow("Last observed", formatTime(device.lastSeenAtMillis))
        Text(
            "RSSI-based estimate. Bands are not to scale. The marker shows range only; direction is unavailable.",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RadarPlot(activeBand: Int?, rssi: Int, status: String) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
    val ringLabels = radarBands.indices.map { textMeasurer.measure("${it + 1}", labelStyle) }
    val description = if (activeBand != null) {
        "Proximity radar. Band ${activeBand + 1} of ${radarBands.size}: ${radarBands[activeBand].label}, estimated ${radarBands[activeBand].distanceLabel}. RSSI $rssi dBm. Direction unavailable."
    } else {
        "Proximity radar. No active range marker. $status. Last received RSSI $rssi dBm."
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 16.dp.toPx()
            // Leave room for the receiver icon inside the first band.
            val innerRadius = radius * 0.22f
            val step = (radius - innerRadius) / radarBands.size
            val showRingNumbers = ringLabels.maxOf { it.size.width } + 4.dp.toPx() <= step
            if (activeBand != null) {
                drawCircle(colors.primary.copy(alpha = 0.12f), innerRadius + step * (activeBand + 1), center)
                drawCircle(colors.surfaceContainerLow, innerRadius + step * activeBand, center)
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
            drawLine(colors.outlineVariant, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx(), pathEffect = dash)
            drawLine(colors.outlineVariant, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.dp.toPx(), pathEffect = dash)
            drawCircle(colors.outlineVariant, innerRadius, center, style = Stroke(1.dp.toPx()))
            radarBands.indices.forEach { index ->
                val active = activeBand == index
                drawCircle(
                    if (active) colors.primary else colors.outlineVariant,
                    innerRadius + step * (index + 1), center,
                    style = Stroke(if (active) 2.dp.toPx() else 1.dp.toPx()),
                )
                if (showRingNumbers) {
                    val labelCenter = Offset(center.x + innerRadius + step * (index + 0.5f), center.y)
                    val label = ringLabels[index]
                    drawRoundRect(
                        colors.surfaceContainerLow, topLeft = labelCenter - Offset(label.size.width / 2f + 2.dp.toPx(), label.size.height / 2f),
                        size = Size(label.size.width + 4.dp.toPx(), label.size.height.toFloat()), cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    drawText(label, topLeft = labelCenter - Offset(label.size.width / 2f, label.size.height / 2f))
                }
            }
            if (activeBand != null) {
                // Fixed bearing: the SDK reports signal strength, not an angle of arrival.
                val marker = Offset(center.x, center.y - innerRadius - step * (activeBand + 0.5f))
                drawLine(colors.primary, Offset(center.x, center.y - innerRadius), marker, 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(colors.primary.copy(alpha = 0.16f), 11.dp.toPx(), marker)
                drawCircle(colors.surfaceContainerLow, 6.dp.toPx(), marker)
                drawCircle(colors.primary, 4.dp.toPx(), marker)
            }
            drawCircle(colors.surfaceContainerLow, innerRadius - 1.dp.toPx(), center)
            val phoneWidth = 12.dp.toPx()
            val phoneHeight = 20.dp.toPx()
            drawRoundRect(
                colors.onSurfaceVariant, topLeft = center - Offset(phoneWidth / 2, phoneHeight / 2),
                size = Size(phoneWidth, phoneHeight), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()),
            )
            drawLine(colors.onSurfaceVariant, center + Offset(-2.dp.toPx(), 6.dp.toPx()), center + Offset(2.dp.toPx(), 6.dp.toPx()), 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
        Text("Center: this phone", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}
