package com.example.watermelonripeness.analysis

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class TapFeatureExtractorTest {
    @Test
    fun extractsResonanceDecayAndSignalQualityFromTransientTap() {
        val sampleRate = 16_000
        val peakSample = 1600
        val pcm = syntheticRecording(sampleRate, peakSample, 150.0, decayRate = 20.0)
        val features = TapFeatureExtractor.extract(pcm, sampleRate, peakSample)

        assertTrue(features.dominantFrequencyHz in 135.0..165.0)
        assertTrue(features.lowBandEnergyRatio > 0.60)
        assertTrue(features.decayDb < -4.0)
        assertTrue(features.snrDb > 10.0)
    }

    @Test
    fun higherFrequencyTapHasLessEnergyInLowBand() {
        val sampleRate = 16_000
        val peakSample = 1600
        val low = TapFeatureExtractor.extract(syntheticRecording(sampleRate, peakSample, 150.0, 18.0), sampleRate, peakSample)
        val high = TapFeatureExtractor.extract(syntheticRecording(sampleRate, peakSample, 300.0, 18.0), sampleRate, peakSample)
        assertTrue(low.lowBandEnergyRatio > high.lowBandEnergyRatio)
    }

    private fun syntheticRecording(sampleRate: Int, peakSample: Int, frequencyHz: Double, decayRate: Double): ShortArray {
        return ShortArray(sampleRate) { index ->
            val noise = 0.002 * sin(2.0 * PI * 713.0 * index / sampleRate)
            if (index < peakSample) return@ShortArray (noise * Short.MAX_VALUE).toInt().toShort()
            val t = (index - peakSample) / sampleRate.toDouble()
            val tap = 0.82 * exp(-decayRate * t) * sin(2.0 * PI * frequencyHz * t)
            ((noise + tap) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
