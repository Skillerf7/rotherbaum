package com.rotherbaum.player.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standard-Biquad-Filter nach dem RBJ Audio-EQ-Cookbook.
 *
 * Ein eigenes Objekt pro Kanal (links/rechts) UND pro Band ist
 * nötig, weil jeder Filter seinen eigenen Verzögerungsspeicher
 * (x1,x2,y1,y2) hat - der darf zwischen den Kanälen nicht vermischt
 * werden, sonst gibt's hörbare Artefakte im Stereobild.
 */
class Biquad {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }

    /** Parametrisches Peaking-Band: frei wählbare Frequenz, Gain (dB) und Güte Q. */
    fun setPeaking(sampleRate: Int, freqHz: Float, gainDb: Float, q: Float) {
        val a = 10f.pow(gainDb / 40f)
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val alpha = sin(w0) / (2f * q.coerceAtLeast(0.05f))
        val cosW0 = cos(w0)

        val b0u = 1f + alpha * a
        val b1u = -2f * cosW0
        val b2u = 1f - alpha * a
        val a0u = 1f + alpha / a
        val a1u = -2f * cosW0
        val a2u = 1f - alpha / a

        applyNormalized(b0u, b1u, b2u, a0u, a1u, a2u)
    }

    /** Bass-Shelf (alles unterhalb freqHz wird angehoben/abgesenkt). */
    fun setLowShelf(sampleRate: Int, freqHz: Float, gainDb: Float) {
        setShelf(sampleRate, freqHz, gainDb, isLowShelf = true)
    }

    /** Höhen-Shelf (alles oberhalb freqHz wird angehoben/abgesenkt). */
    fun setHighShelf(sampleRate: Int, freqHz: Float, gainDb: Float) {
        setShelf(sampleRate, freqHz, gainDb, isLowShelf = false)
    }

    private fun setShelf(sampleRate: Int, freqHz: Float, gainDb: Float, isLowShelf: Boolean) {
        val a = 10f.pow(gainDb / 40f)
        val w0 = 2f * PI.toFloat() * freqHz / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val shelfSlope = 1f
        val alpha = sinW0 / 2f * sqrt((a + 1f / a) * (1f / shelfSlope - 1f) + 2f)
        val twoSqrtAAlpha = 2f * sqrt(a) * alpha

        val sign = if (isLowShelf) 1f else -1f

        val b0u = a * ((a + 1f) - sign * (a - 1f) * cosW0 + twoSqrtAAlpha)
        val b1u = sign * 2f * a * ((a - 1f) - sign * (a + 1f) * cosW0)
        val b2u = a * ((a + 1f) - sign * (a - 1f) * cosW0 - twoSqrtAAlpha)
        val a0u = (a + 1f) + sign * (a - 1f) * cosW0 + twoSqrtAAlpha
        val a1u = -sign * 2f * ((a - 1f) + sign * (a + 1f) * cosW0)
        val a2u = (a + 1f) + sign * (a - 1f) * cosW0 - twoSqrtAAlpha

        applyNormalized(b0u, b1u, b2u, a0u, a1u, a2u)
    }

    private fun applyNormalized(b0u: Float, b1u: Float, b2u: Float, a0u: Float, a1u: Float, a2u: Float) {
        b0 = b0u / a0u
        b1 = b1u / a0u
        b2 = b2u / a0u
        a1 = a1u / a0u
        a2 = a2u / a0u
    }

    fun process(input: Float): Float {
        val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = input
        y2 = y1; y1 = output
        return output
    }
}
