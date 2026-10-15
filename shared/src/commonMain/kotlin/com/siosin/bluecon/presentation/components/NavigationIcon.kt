package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.siosin.bluecon.presentation.ScannerTab

/** Decorative vector drawing; the NavigationBarItem label supplies the accessible name. */
@Composable
fun NavigationIcon(tab: ScannerTab) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(32.dp)) {
        val radius = size.minDimension * 0.4f
        val strokeWidth = 2.25.dp.toPx()
        drawCircle(tint, radius, style = Stroke(strokeWidth))
        when (tab) {
            ScannerTab.SCANNER -> {
                drawCircle(tint, radius * 0.5f, style = Stroke(strokeWidth * 0.7f))
                drawLine(
                    tint, center, center + Offset(radius * 0.7f, -radius * 0.7f),
                    strokeWidth, cap = StrokeCap.Round,
                )
                drawCircle(tint, 2.dp.toPx(), center + Offset(-radius * 0.5f, radius * 0.35f))
            }
            ScannerTab.HISTORY -> {
                drawLine(tint, center, center + Offset(0f, -radius * 0.55f), strokeWidth, cap = StrokeCap.Round)
                drawLine(tint, center, center + Offset(radius * 0.45f, radius * 0.2f), strokeWidth, cap = StrokeCap.Round)
            }
        }
    }
}
