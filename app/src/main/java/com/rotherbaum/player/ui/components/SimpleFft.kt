package com.rotherbaum.player.ui.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Minimale iterative Radix-2-FFT für den Spektrum-Analyzer. Erwartet
 * eine Eingabelänge, die eine Zweierpotenz ist (der Aufrufer schneidet
 * entsprechend zu). Reicht für eine flüssige Balkenanzeige locker aus
 * - kein Anspruch auf eine vollwertige Analyse-Bibliothek.
 */
object SimpleFft {

    fun magnitudes(samples: FloatArray): FloatArray {
        var n = 1
        while (n * 2 <= samples.size) n *= 2
        if (n < 2) return FloatArray(0)

        val real = FloatArray(n) { samples[it] }
        val imag = FloatArray(n)

        fft(real, imag)

        val half = n / 2
        return FloatArray(half) { i ->
            sqrt(real[i] * real[i] + imag[i] * imag[i]) / n
        }
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                real[i] = real[j].also { real[j] = real[i] }
                imag[i] = imag[j].also { imag[j] = imag[i] }
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2f * PI.toFloat() / len
            val wReal = cos(angle)
            val wImag = sin(angle)

            var i = 0
            while (i < n) {
                var curReal = 1f
                var curImag = 0f
                for (k in 0 until len / 2) {
                    val evenIdx = i + k
                    val oddIdx = i + k + len / 2

                    val tReal = real[oddIdx] * curReal - imag[oddIdx] * curImag
                    val tImag = real[oddIdx] * curImag + imag[oddIdx] * curReal

                    real[oddIdx] = real[evenIdx] - tReal
                    imag[oddIdx] = imag[evenIdx] - tImag
                    real[evenIdx] += tReal
                    imag[evenIdx] += tImag

                    val nextReal = curReal * wReal - curImag * wImag
                    val nextImag = curReal * wImag + curImag * wReal
                    curReal = nextReal
                    curImag = nextImag
                }
                i += len
            }
            len = len shl 1
        }
    }
}
