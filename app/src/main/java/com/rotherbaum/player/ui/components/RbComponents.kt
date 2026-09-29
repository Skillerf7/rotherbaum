package com.rotherbaum.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

/** Umrandete Pille mit Text (Eq / Limit / Preset ...). */
@Composable
fun RbPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outline: Color = Color.White,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0E0E0E))
            .border(1.5.dp, outline, RoundedCornerShape(50))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

/** Dunkle, leicht transparente Pille mit einem Icon. */
@Composable
fun RbIconPill(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillWidth: Dp = 72.dp,
    pillHeight: Dp = 46.dp
) {
    Box(
        modifier = modifier
            .size(pillWidth, pillHeight)
            .clip(RoundedCornerShape(50))
            .background(Color(0x99000000))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

/** Tab-Leiste mit Icons in einer abgerundeten Karte. */
@Composable
fun RbTabBar(
    icons: List<ImageVector>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1C1C1C))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        icons.forEachIndexed { index, icon ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (index == selected) Color.White else Color(0xFF7A7A7A),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

/**
 * Dunkler Drehregler mit leuchtendem Bogen (Wert 0..1).
 * Senkrechtes Ziehen verändert den Wert.
 */
@Composable
fun RbKnob(
    label: String,
    valueText: String,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 120.dp,
    bipolar: Boolean = false,
    enabled: Boolean = true
) {
    val latest by rememberUpdatedState(fraction)
    val callback by rememberUpdatedState(onFractionChange)

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(diameter)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            callback((latest - drag.y / 350f).coerceIn(0f, 1f))
                        }
                    }
                }
        ) {
            val stroke = 6.dp.toPx()
            val outerR = this.size.minDimension / 2f - stroke
            val innerR = outerR - stroke / 2f - 7.dp.toPx()
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val f = fraction.coerceIn(0f, 1f)

            drawCircle(Color(0xFF242424), radius = innerR, center = c)
            drawCircle(Color(0xFF3A3A3A), radius = innerR, center = c, style = Stroke(2.dp.toPx()))

            var arcStart = 135f
            var arcSweep = 270f * f
            if (bipolar) {
                if (f >= 0.5f) {
                    arcStart = 270f
                    arcSweep = 270f * (f - 0.5f)
                } else {
                    arcStart = 270f + 270f * (f - 0.5f)
                    arcSweep = 270f * (0.5f - f)
                }
            }
            if (arcSweep > 0.5f) {
                val topLeft = Offset(c.x - outerR, c.y - outerR)
                val arcSize = Size(outerR * 2f, outerR * 2f)
                drawArc(
                    color.copy(alpha = 0.25f), arcStart, arcSweep, false, topLeft, arcSize,
                    style = Stroke(stroke * 1.9f, cap = StrokeCap.Round)
                )
                drawArc(
                    color, arcStart, arcSweep, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }

            val angle = (135f + 270f * f) * PI.toFloat() / 180f
            val p1 = Offset(c.x + innerR * 0.55f * cos(angle), c.y + innerR * 0.55f * sin(angle))
            val p2 = Offset(c.x + innerR * 0.85f * cos(angle), c.y + innerR * 0.85f * sin(angle))
            drawLine(Color(0xFFE6E6E6), p1, p2, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
        Text(valueText, color = Color(0xFFB5B5B5), fontSize = 15.sp)
    }
}

/**
 * Senkrechter Fader mit Pillen-Griff und leuchtender Füllung.
 * bipolarFill = true: Füllung geht von der Nullmarke aus (PreAmp),
 * sonst von unten bis zum Griff (Bänder).
 */
@Composable
fun RbFader(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    bipolarFill: Boolean = false
) {
    val callback by rememberUpdatedState(onValueChange)

    Canvas(
        modifier = modifier.pointerInput(range) {
            val thumbH = 56.dp.toPx()
            fun setFromY(y: Float) {
                val usable = (size.height.toFloat() - thumbH).coerceAtLeast(1f)
                val frac = 1f - ((y - thumbH / 2f) / usable).coerceIn(0f, 1f)
                callback(range.start + frac * (range.endInclusive - range.start))
            }
            detectDragGestures(onDragStart = { setFromY(it.y) }) { change, _ ->
                change.consume()
                setFromY(change.position.y)
            }
        }
    ) {
        val w = this.size.width
        val h = this.size.height
        val thumbH = 56.dp.toPx()
        val thumbW = minOf(w * 0.92f, 30.dp.toPx())
        val cx = w / 2f
        val usable = h - thumbH
        val span = range.endInclusive - range.start
        val frac = ((value - range.start) / span).coerceIn(0f, 1f)
        val thumbY = thumbH / 2f + (1f - frac) * usable
        val zeroFrac = ((0f - range.start) / span).coerceIn(0f, 1f)
        val zeroY = thumbH / 2f + (1f - zeroFrac) * usable
        val top = thumbH / 2f
        val bottom = h - thumbH / 2f

        drawLine(Color(0xFF3B3B3B), Offset(cx, top), Offset(cx, bottom), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)

        val fillFrom = if (bipolarFill) zeroY else bottom
        val glow = lerp(Color(0xFF19E619), Color(0xFFFFD60A), frac)
        drawLine(glow.copy(alpha = 0.28f), Offset(cx, fillFrom), Offset(cx, thumbY), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
        drawLine(glow, Offset(cx, fillFrom), Offset(cx, thumbY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

        val topLeft = Offset(cx - thumbW / 2f, thumbY - thumbH / 2f)
        drawRoundRect(Color(0xFF262626), topLeft, Size(thumbW, thumbH), CornerRadius(thumbW / 2f))
        drawRoundRect(Color(0xFF4A4A4A), topLeft, Size(thumbW, thumbH), CornerRadius(thumbW / 2f), style = Stroke(2.dp.toPx()))
        drawRoundRect(
            Color(0xFFDDDDDD),
            Offset(cx - thumbW * 0.3f, thumbY - 1.5.dp.toPx()),
            Size(thumbW * 0.6f, 3.dp.toPx()),
            CornerRadius(2.dp.toPx())
        )
    }
}

/** Grobe Frequenzgang-Kurve aus den Bändern (freq, gainDb, q). */
@Composable
fun EqCurve(bands: List<Triple<Float, Float, Float>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height
        drawLine(Color(0xFF444444), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1.dp.toPx())

        val path = Path()
        val steps = 120
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val f = 20f * 1000f.pow(t)
            var g = 0f
            for ((fc, gain, q) in bands) {
                val octaves = ln(f / fc) / ln(2f)
                val sigma = 0.9f / q.coerceAtLeast(0.3f)
                g += gain * exp(-(octaves * octaves) / (2f * sigma * sigma))
            }
            val y = h / 2f - (g / 18f).coerceIn(-1f, 1f) * (h / 2f - 6.dp.toPx())
            val x = t * w
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFF7CFF3A), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Wellenform-Balken als Suchleiste: Tippen oder Ziehen springt in den Titel. */
@Composable
fun WaveSeekBar(
    seed: Long,
    fraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val heights = remember(seed) {
        val random = java.util.Random(seed)
        List(64) { 0.22f + random.nextFloat() * 0.78f }
    }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shown = dragFraction ?: fraction
    val seekCallback by rememberUpdatedState(onSeek)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    seekCallback((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragFraction = (it.x / size.width.toFloat()).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragFraction?.let { seekCallback(it) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val n = heights.size
        val slot = this.size.width / n
        val barW = slot * 0.6f
        heights.forEachIndexed { i, hFrac ->
            val barH = this.size.height * hFrac
            val x = i * slot + (slot - barW) / 2f
            val played = (i + 0.5f) / n <= shown
            drawRoundRect(
                color = if (played) Color.White else Color(0xFF8A8A8A),
                topLeft = Offset(x, (this.size.height - barH) / 2f),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}
