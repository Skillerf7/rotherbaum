package com.rotherbaum.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drehregler à la Poweramp: senkrechtes Ziehen ändert den Wert,
 * ein farbiger Bogen zeigt die Auslenkung relativ zur Mitte.
 */
@Composable
fun RotaryKnob(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    size: androidx.compose.ui.unit.Dp = 88.dp,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 200f * span
                        val newValue = (value + delta).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(newValue)
                    }
                }
        ) {
            val strokeWidth = 8.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Hintergrund-Ring
            drawCircle(
                color = accentColor.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Aktiver Bogen (270 Grad Gesamtwinkel, wie ein klassischer Poti)
            val startAngle = 135f
            val sweep = 270f * fraction
            drawArc(
                color = accentColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Zeiger
            val pointerAngleDeg = startAngle + 270f * fraction
            val pointerAngleRad = pointerAngleDeg * PI.toFloat() / 180f
            val pointerLength = radius - strokeWidth
            val pointerEnd = Offset(
                x = center.x + pointerLength * cos(pointerAngleRad),
                y = center.y + pointerLength * sin(pointerAngleRad)
            )
            drawLine(
                color = accentColor,
                start = center,
                end = pointerEnd,
                strokeWidth = strokeWidth / 2.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(valueText, style = MaterialTheme.typography.bodySmall)
    }
}
