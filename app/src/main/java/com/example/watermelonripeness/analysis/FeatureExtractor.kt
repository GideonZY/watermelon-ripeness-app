package com.example.watermelonripeness.analysis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

object FeatureExtractor {
    fun extract(pcm: ShortArray, sampleRate: Int): AudioFeatures {
        require(pcm.isNotEmpty())
        val normalized = DoubleArray(pcm.size) { pcm[it] / 32768.0 }
        val rms = sqrt(normalized.sumOf { it * it } / normalized.size)

        // 取最响的 1024 点窗口，尽量覆盖敲击本身而不是静音。
        val n = 1024
        val hop = 256
        var bestStart = 0
        var bestEnergy = -1.0
        for (start in 0..maxOf(0, normalized.size - n) step hop) {
            var energy = 0.0
            for (i in 0 until minOf(n, normalized.size - start)) energy += normalized[start + i] * normalized[start + i]
            if (energy > bestEnergy) { bestEnergy = energy; bestStart = start }
        }
        val frame = DoubleArray(n) { i ->
            val sample = normalized.getOrElse(bestStart + i) { 0.0 }
            sample * (0.5 - 0.5 * cos(2.0 * PI * i / (n - 1)))
        }
        val imaginary = fft(frame)
        val magnitudes = DoubleArray(n / 2) { i -> sqrt(frame[i] * frame[i] + imaginary[i] * imaginary[i]) }

        val minBin = (80.0 * n / sampleRate).toInt().coerceAtLeast(1)
        val maxBin = (1000.0 * n / sampleRate).toInt().coerceAtMost(magnitudes.lastIndex)
        val peakBin = (minBin..maxBin).maxByOrNull { magnitudes[it] } ?: minBin
        val dominant = peakBin.toDouble() * sampleRate / n

        val centroidMax = (4000.0 * n / sampleRate).toInt().coerceAtMost(magnitudes.lastIndex)
        var weighted = 0.0
        var total = 0.0
        for (i in 1..centroidMax) {
            weighted += (i.toDouble() * sampleRate / n) * magnitudes[i]
            total += magnitudes[i]
        }
        val centroid = if (total > 0) weighted / total else 0.0

        val chunk = (sampleRate * 0.12).coerceAtMost(normalized.size / 2).coerceAtLeast(1)
        fun chunkRms(start: Int) = sqrt((start until start + chunk).sumOf { normalized[it] * normalized[it] } / chunk)
        val earlyStart = bestStart.coerceAtMost(normalized.size - chunk)
        val lateStart = (earlyStart + sampleRate / 2).coerceAtMost(normalized.size - chunk)
        val early = chunkRms(earlyStart)
        val late = chunkRms(lateStart)
        val decayDb = 20.0 * ln((late + 1e-8) / (early + 1e-8)) / ln(10.0)

        return AudioFeatures(dominant, centroid, rms, decayDb)
    }

    // 原地 radix-2 FFT；real 返回实部，返回值为虚部。
    private fun fft(real: DoubleArray): DoubleArray {
        val n = real.size
        val imag = DoubleArray(n)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val r = real[i]; real[i] = real[j]; real[j] = r
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wLenR = cos(angle); val wLenI = sin(angle)
            for (start in 0 until n step len) {
                var wr = 1.0; var wi = 0.0
                for (k in 0 until len / 2) {
                    val even = start + k; val odd = even + len / 2
                    val vr = real[odd] * wr - imag[odd] * wi
                    val vi = real[odd] * wi + imag[odd] * wr
                    val ur = real[even]; val ui = imag[even]
                    real[even] = ur + vr; imag[even] = ui + vi
                    real[odd] = ur - vr; imag[odd] = ui - vi
                    val nextWr = wr * wLenR - wi * wLenI
                    wi = wr * wLenI + wi * wLenR; wr = nextWr
                }
            }
            len = len shl 1
        }
        return imag
    }
}
