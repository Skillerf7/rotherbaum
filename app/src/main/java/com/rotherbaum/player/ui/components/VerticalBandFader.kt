package com.rotherbaum.player.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

/**
 * Ein senkrechter Fader pro Frequenzband (wie beim klassischen
 * Grafik-EQ). Material3 hat keinen eingebauten Vertikal-Slider,
 * daher wird ein normaler Slider um 270 Grad gedreht und die
 * Zwangs-Breite/Höhe über einen eigenen Layout-Wrapper getauscht.
 */
@Composable
fun VerticalBandFader(
    label: String,
    valueDb: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = -15f..15f,
    height: androidx.compose.ui.unit.Dp = 160.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RotatedSlider(
            value = valueDb,
            onValueChange = onValueChange,
            valueRange = range,
            height = height
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            String.format("%.1f", valueDb),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun RotatedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    height: androidx.compose.ui.unit.Dp
) {
    Layout(
        content = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .width(height)
                    .rotate(-90f),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { measurables, constraints ->
        // Nach der 90-Grad-Rotation vertauschen sich Breite/Höhe
        // sichtbar, das eigentliche Compose-Layout misst aber weiter
        // in der ursprünglichen Ausrichtung - hier wird der
        // verfügbare Platz entsprechend getauscht.
        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = 0, maxWidth = Int.MAX_VALUE,
                minHeight = 0, maxHeight = Int.MAX_VALUE
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = (placeable.width - placeable.height) / 2
            )
        }
    }
}
