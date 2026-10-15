package com.siosin.bluecon.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

enum class AppSymbol { BACK, SEARCH, CLOSE, CHECK, DOWN, UP, NEXT, BLUETOOTH, SORT, FILTER, PAUSE, PLAY }

/** Decorative inside labeled controls. Icon-only buttons provide their own semantics. */
@Composable
fun AppIcon(symbol: AppSymbol, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier.size(24.dp)) {
        withTransform({ scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) }) {
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                drawLine(tint, Offset(x1, y1), Offset(x2, y2), 2f, cap = StrokeCap.Round)
            when (symbol) {
                AppSymbol.BACK -> { line(20f, 12f, 4f, 12f); line(4f, 12f, 11f, 5f); line(4f, 12f, 11f, 19f) }
                AppSymbol.SEARCH -> { drawCircle(tint, 6.5f, Offset(10f, 10f), style = Stroke(2f)); line(15f, 15f, 21f, 21f) }
                AppSymbol.CLOSE -> { line(6f, 6f, 18f, 18f); line(6f, 18f, 18f, 6f) }
                AppSymbol.CHECK -> { line(4f, 12f, 9f, 17f); line(9f, 17f, 20f, 6f) }
                AppSymbol.DOWN -> { line(6f, 9f, 12f, 15f); line(12f, 15f, 18f, 9f) }
                AppSymbol.UP -> { line(6f, 15f, 12f, 9f); line(12f, 9f, 18f, 15f) }
                AppSymbol.NEXT -> { line(9f, 6f, 15f, 12f); line(15f, 12f, 9f, 18f) }
                AppSymbol.BLUETOOTH -> {
                    val path = Path().apply {
                        moveTo(12f, 3f); lineTo(18f, 8f); lineTo(6f, 17f)
                        moveTo(12f, 3f); lineTo(12f, 21f); lineTo(18f, 16f); lineTo(6f, 7f)
                    }
                    drawPath(path, tint, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                AppSymbol.SORT -> { line(4f, 6f, 20f, 6f); line(4f, 12f, 15f, 12f); line(4f, 18f, 10f, 18f) }
                AppSymbol.FILTER -> { line(4f, 6f, 20f, 6f); line(7f, 12f, 17f, 12f); line(10f, 18f, 14f, 18f) }
                AppSymbol.PAUSE -> { line(8f, 5f, 8f, 19f); line(16f, 5f, 16f, 19f) }
                AppSymbol.PLAY -> drawPath(Path().apply { moveTo(8f, 4f); lineTo(20f, 12f); lineTo(8f, 20f); close() }, tint)
            }
        }
    }
}
