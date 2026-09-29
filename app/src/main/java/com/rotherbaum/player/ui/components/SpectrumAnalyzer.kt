package com.rotherbaum.player.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

private const val BAR_COUNT = 32

/**
 * Zeigt ein Live-Spektrum des gerade wiedergegebenen Audios. Nutzt
 * Androids Visualizer-Effekt (hängt sich an die Audio-Session des
 * Players) und rechnet die Wellenform per kleiner Eigen-FFT in
 * Frequenzbalken um.
 *
 * Voraussetzung: android.permission.RECORD_AUDIO wird vom
 * Visualizer NICHT gebraucht, solange man sich an eine eigene
 * Audio-Session hängt (audioSessionId von ExoPlayer) statt an das
 * Mikrofon - das ist hier der Fall.
 */
@Composable
fun SpectrumAnalyzer(audioSessionId: Int, modifier: Modifier = Modifier) {
    var magnitudes by remember { mutableStateOf(FloatArray(BAR_COUNT)) }

    DisposableEffect(audioSessionId) {
        var visualizer: Visualizer? = null

        if (audioSessionId != 0) {
            try {
                visualizer = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                v: Visualizer?, waveform: ByteArray?, samplingRate: Int
                            ) {
                                waveform ?: return
                                val floatSamples = FloatArray(waveform.size) {
                                    (waveform[it].toInt() and 0xFF) / 128f - 1f
                                }
                                val mags = SimpleFft.magnitudes(floatSamples)
                                magnitudes = downsampleToBars(mags, BAR_COUNT)
                            }

                            override fun onFftDataCapture(
                                v: Visualizer?, fft: ByteArray?, samplingRate: Int
                            ) {
                                // Wellenform-Callback oben reicht uns.
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        true,
                        false
                    )
                    enabled = true
                }
            } catch (e: Exception) {
                // Visualizer ist auf manchen Geräten/ROMs
                // eingeschränkt - Analyzer bleibt dann einfach leer
                // statt die App zum Absturz zu bringen.
            }
        }

        onDispose {
            visualizer?.release()
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val barWidth = size.width / BAR_COUNT
        magnitudes.forEachIndexed { i, magnitude ->
            val barHeight = (magnitude.coerceIn(0f, 1f) * size.height)
            drawRect(
                color = androidx.compose.ui.graphics.Color(0xFFE5484D),
                topLeft = Offset(i * barWidth + 2f, size.height - barHeight),
                size = Size(barWidth - 4f, barHeight)
            )
        }
    }
}

private fun downsampleToBars(mags: FloatArray, barCount: Int): FloatArray {
    if (mags.isEmpty()) return FloatArray(barCount)
    val bars = FloatArray(barCount)
    val chunk = (mags.size / barCount).coerceAtLeast(1)
    for (i in 0 until barCount) {
        val start = i * chunk
        val end = (start + chunk).coerceAtMost(mags.size)
        if (start >= end) continue
        var sum = 0f
        for (j in start until end) sum += mags[j]
        // Logarithmische Skalierung, damit leise Höhen nicht im
        // Vergleich zu lauten Bässen komplett untergehen.
        bars[i] = (kotlin.math.ln(1f + sum / (end - start) * 20f)).coerceIn(0f, 1f)
    }
    return bars
}
