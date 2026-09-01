package com.example.watermelonripeness.analysis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

data class TapFeatures(
    val dominantFrequencyHz: Double,
    val spectralCentroidHz: Double,
    val lowBandEnergyRatio: Double,
    val decayDb: Double,
    val snrDb: Double
)

/**
 * 从单次拍击峰值附近提取瞬态声特征。
 *
 * 这些特征仅用于 0.3.0 的文献启发式判断，不代表已经通过真实标注样本完成校准。
 */
object TapFeatureExtractor {
    fun extract(pcm: ShortArray, sampleRate: Int, peakSample: Int): TapFeatures {
        require(pcm.isNotEmpty())
        require(sampleRate > 0)
        require(peakSample in pcm.indices)

        val normalized = DoubleArray(pcm.size) { pcm[it] / 32768.0 }

        val fftSize = 2048
        val spectralStart = (peakSample + sampleRate * 5 / 1000).coerceAtMost(pcm.lastIndex)
        val real = DoubleArray(fftSize) { i ->
            val sample = normalized.getOrElse(spectralStart + i) { 0.0 }
            sample * (0.5 - 0.5 * cos(2.0 * PI * i / (fftSize - 1)))
        }
        val imag = fft(real)
        val magnitudes = DoubleArray(fftSize / 2) { i ->
            sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        fun binFor(hz: Double): Int = (hz * fftSize / sampleRate).toInt().coerceIn(1, magnitudes.lastIndex)
        val minBin = binFor(80.0)
        val maxBin = binFor(800.0)
        val peakBin = (minBin..maxBin).maxByOrNull { magnitudes[it] } ?: minBin
        val dominant = peakBin.toDouble() * sampleRate / fftSize

        val centroidMaxBin = binFor(3000.0)
        var weighted = 0.0
        var magnitudeTotal = 0.0
        for (i in 1..centroidMaxBin) {
            val hz = i.toDouble() * sampleRate / fftSize
            weighted += hz * magnitudes[i]
            magnitudeTotal += magnitudes[i]
        }
        val centroid = if (magnitudeTotal > 0.0) weighted / magnitudeTotal else 0.0

        val lowStart = binFor(80.0)
        val lowEnd = binFor(220.0)
        val totalEnd = binFor(600.0)
        var lowEnergy = 0.0
        var totalEnergy = 0.0
        for (i in lowStart..totalEnd) {
            val energy = magnitudes[i] * magnitudes[i]
            totalEnergy += energy
            if (i <= lowEnd) lowEnergy += energy
        }
        val lowRatio = if (totalEnergy > 1e-12) lowEnergy / totalEnergy else 0.0

        fun rms(startSample: Int, endSampleExclusive: Int): Double {
            val start = startSample.coerceIn(0, normalized.size)
            val end = endSampleExclusive.coerceIn(start, normalized.size)
            if (end <= start) return 0.0
            var sum = 0.0
            for (i in start until end) sum += normalized[i] * normalized[i]
            return sqrt(sum / (end - start))
        }

        val earlyStart = peakSample + sampleRate * 10 / 1000
        val earlyEnd = peakSample + sampleRate * 55 / 1000
        val lateStart = peakSample + sampleRate * 115 / 1000
        val lateEnd = peakSample + sampleRate * 175 / 1000
        val earlyRms = rms(earlyStart, earlyEnd)
        val lateRms = rms(lateStart, lateEnd)
        val decayDb = 20.0 * log10((lateRms + 1e-8) / (earlyRms + 1e-8))

        val noiseStart = peakSample - sampleRate * 35 / 1000
        val noiseEnd = peakSample - sampleRate * 5 / 1000
        val noiseRms = rms(noiseStart, noiseEnd).coerceAtLeast(1e-6)
        val signalRms = rms(peakSample, peakSample + sampleRate * 70 / 1000).coerceAtLeast(1e-6)
        val snrDb = 20.0 * log10(signalRms / noiseRms)

        return TapFeatures(
            dominantFrequencyHz = dominant,
            spectralCentroidHz = centroid,
            lowBandEnergyRatio = lowRatio.coerceIn(0.0, 1.0),
            decayDb = decayDb,
            snrDb = snrDb
        )
    }

    private fun log10(value: Double): Double = ln(value) / ln(10.0)

    // radix-2 FFT；real 原地变为实部，返回虚部。
    private fun fft(real: DoubleArray): DoubleArray {
        val n = real.size
        val imag = DoubleArray(n)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tmp = real[i]
                real[i] = real[j]
                real[j] = tmp
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wLenR = cos(angle)
            val wLenI = sin(angle)
            for (start in 0 until n step len) {
                var wr = 1.0
                var wi = 0.0
                for (k in 0 until len / 2) {
                    val even = start + k
                    val odd = even + len / 2
                    val vr = real[odd] * wr - imag[odd] * wi
                    val vi = real[odd] * wi + imag[odd] * wr
                    val ur = real[even]
                    val ui = imag[even]
                    real[even] = ur + vr
                    imag[even] = ui + vi
                    real[odd] = ur - vr
                    imag[odd] = ui - vi
                    val nextWr = wr * wLenR - wi * wLenI
                    wi = wr * wLenI + wi * wLenR
                    wr = nextWr
                }
            }
            len = len shl 1
        }
        return imag
    }
}
