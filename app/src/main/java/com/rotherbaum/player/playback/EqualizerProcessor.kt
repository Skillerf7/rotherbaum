package com.rotherbaum.player.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tanh

data class EqBand(val freqHz: Float, var gainDb: Float = 0f, var q: Float = 1.0f)

/**
 * Eigener vollwertiger parametrischer Equalizer, direkt im Audio-Pfad
 * von ExoPlayer (als AudioProcessor). Anders als der eingebaute,
 * geräteabhängige android.media.audiofx.Equalizer-Effekt sind hier
 * Frequenz, Gain UND Güte (Q) pro Band frei wählbar - nicht nur ein
 * paar vom Hersteller vorgegebene Fixbänder.
 *
 * Kette pro Sample: Preamp -> 10 Peaking-Bänder -> Bass-Shelf ->
 * Höhen-Shelf -> optionaler Limiter.
 */
class EqualizerProcessor : BaseAudioProcessor() {

    // 10 Bänder wie bei klassischen Grafik-EQs (31 Hz - 16 kHz),
    // aber jedes davon ist hier echt parametrisch (Freq/Gain/Q
    // einzeln über setBandFreq/Gain/Q änderbar).
    val bands: List<EqBand> = listOf(
        EqBand(31f), EqBand(62f), EqBand(125f), EqBand(250f), EqBand(500f),
        EqBand(1000f), EqBand(2000f), EqBand(4000f), EqBand(8000f), EqBand(16000f)
    )

    @Volatile var preampDb: Float = 0f
        set(value) { field = value.coerceIn(-24f, 24f) }

    /** 0-100 %, 50 % = neutral (0 dB Shelf) */
    @Volatile var bassPercent: Float = 50f
        set(value) { field = value.coerceIn(0f, 100f); coefficientsDirty = true }

    /** 0-100 %, 50 % = neutral (0 dB Shelf) */
    @Volatile var treblePercent: Float = 50f
        set(value) { field = value.coerceIn(0f, 100f); coefficientsDirty = true }

    @Volatile var limiterEnabled: Boolean = true
    @Volatile var eqEnabled: Boolean = true

    private var sampleRate = 44100
    private var channelCount = 2

    private var bandFilters: Array<Array<Biquad>> = arrayOf()
    private var bassFilters: Array<Biquad> = arrayOf()
    private var trebleFilters: Array<Biquad> = arrayOf()

    @Volatile private var coefficientsDirty = true

    fun setBandGain(index: Int, gainDb: Float) {
        bands[index].gainDb = gainDb.coerceIn(-15f, 15f)
        coefficientsDirty = true
    }

    fun setBandQ(index: Int, q: Float) {
        bands[index].q = q.coerceIn(0.2f, 8f)
        coefficientsDirty = true
    }

    fun resetAll() {
        bands.forEach { it.gainDb = 0f; it.q = 1f }
        preampDb = 0f
        bassPercent = 50f
        treblePercent = 50f
        coefficientsDirty = true
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != android.media.AudioFormat.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        bandFilters = Array(channelCount) { Array(bands.size) { Biquad() } }
        bassFilters = Array(channelCount) { Biquad() }
        trebleFilters = Array(channelCount) { Biquad() }
        coefficientsDirty = true

        return inputAudioFormat
    }

    private fun rebuildCoefficientsIfNeeded() {
        if (!coefficientsDirty) return

        val bassDb = (bassPercent - 50f) / 50f * 12f
        val trebleDb = (treblePercent - 50f) / 50f * 12f

        for (ch in 0 until channelCount) {
            bands.forEachIndexed { i, band ->
                bandFilters[ch][i].setPeaking(sampleRate, band.freqHz, band.gainDb, band.q)
            }
            bassFilters[ch].setLowShelf(sampleRate, 150f, bassDb)
            trebleFilters[ch].setHighShelf(sampleRate, 6000f, trebleDb)
        }

        coefficientsDirty = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        rebuildCoefficientsIfNeeded()

        val outputBuffer = replaceOutputBuffer(remaining)
        val preampGain = 10f.pow(preampDb / 20f)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        var ch = 0
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short.toFloat() / 32768f
            var s = sample

            if (eqEnabled) {
                s *= preampGain

                for (i in bands.indices) {
                    s = bandFilters[ch][i].process(s)
                }
                s = bassFilters[ch].process(s)
                s = trebleFilters[ch].process(s)

                if (limiterEnabled) {
                    val threshold = 0.891f // ca. -1 dBFS
                    s = when {
                        s > threshold -> threshold + (1f - threshold) * tanh((s - threshold) * 4f)
                        s < -threshold -> -threshold - (1f - threshold) * tanh((-s - threshold) * 4f)
                        else -> s
                    }
                }
            }

            val clamped = max(-1f, min(1f, s))
            outputBuffer.putShort((clamped * 32767f).toInt().toShort())

            ch = (ch + 1) % channelCount
        }

        outputBuffer.flip()
    }

    override fun onQueueEndOfStream() {
        // nichts zu tun - der Filter hat keinen internen Puffer, der
        // noch "nachlaufen" müsste
    }

    override fun onFlush() {
        bandFilters.forEach { perChannel -> perChannel.forEach { it.reset() } }
        bassFilters.forEach { it.reset() }
        trebleFilters.forEach { it.reset() }
    }

    override fun onReset() {
        bandFilters = arrayOf()
        bassFilters = arrayOf()
        trebleFilters = arrayOf()
    }
}
