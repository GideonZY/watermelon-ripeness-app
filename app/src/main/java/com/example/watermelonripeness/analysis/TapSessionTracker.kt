package com.example.watermelonripeness.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class TapFrameUpdate(
    val detectedTap: Boolean,
    val tapCount: Int,
    val frameRms: Double,
    val peakAmplitude: Double
)

/**
 * 实时拍击会话跟踪器。
 *
 * 目标不是做成熟度分类，而是从 100 ms 音频帧中识别明显的瞬态敲击，
 * 记录峰值在整段 PCM 中的位置，并在第 3 次拍击后保留少量尾音再结束录音。
 */
class TapSessionTracker(
    private val minTapRms: Double = DEFAULT_MIN_TAP_RMS,
    private val minPeakAmplitude: Double = DEFAULT_MIN_PEAK_AMPLITUDE,
    private val noiseMultiplier: Double = DEFAULT_NOISE_MULTIPLIER,
    private val minCrestFactor: Double = DEFAULT_MIN_CREST_FACTOR,
    private val refractoryMs: Int = DEFAULT_REFRACTORY_MS,
    private val requiredTaps: Int = REQUIRED_TAPS,
    private val tailFramesAfterThirdTap: Int = DEFAULT_TAIL_FRAMES
) {
    init {
        require(minTapRms >= 0.0)
        require(minPeakAmplitude >= 0.0)
        require(noiseMultiplier >= 1.0)
        require(minCrestFactor >= 1.0)
        require(refractoryMs >= 0)
        require(requiredTaps > 0)
        require(tailFramesAfterThirdTap >= 0)
    }

    private var noiseFloorRms = DEFAULT_INITIAL_NOISE_RMS
    private var lastTapPeakSample: Int? = null
    private var tailFramesRemaining: Int? = null

    private val mutableTapPeakSamples = mutableListOf<Int>()
    val tapPeakSamples: List<Int> get() = mutableTapPeakSamples.toList()

    fun reset() {
        noiseFloorRms = DEFAULT_INITIAL_NOISE_RMS
        lastTapPeakSample = null
        tailFramesRemaining = null
        mutableTapPeakSamples.clear()
    }

    fun processFrame(frame: ShortArray, sampleRate: Int, frameStartSample: Int): TapFrameUpdate {
        require(sampleRate > 0)
        require(frameStartSample >= 0)

        if (frame.isEmpty()) {
            advanceTailIfNeeded(false)
            return TapFrameUpdate(false, mutableTapPeakSamples.size, 0.0, 0.0)
        }

        var sumSquares = 0.0
        var peak = 0.0
        var peakIndex = 0
        frame.forEachIndexed { index, sample ->
            val normalized = sample / 32768.0
            sumSquares += normalized * normalized
            val magnitude = abs(normalized)
            if (magnitude > peak) {
                peak = magnitude
                peakIndex = index
            }
        }
        val rms = sqrt(sumSquares / frame.size)
        val threshold = max(minTapRms, noiseFloorRms * noiseMultiplier)
        val crestFactor = if (rms > 1e-9) peak / rms else 0.0

        val peakSample = frameStartSample + peakIndex
        val refractorySamples = sampleRate * refractoryMs / 1000
        val outsideRefractory = lastTapPeakSample?.let { peakSample - it >= refractorySamples } ?: true

        val detected = mutableTapPeakSamples.size < requiredTaps &&
            rms >= threshold &&
            peak >= minPeakAmplitude &&
            crestFactor >= minCrestFactor &&
            outsideRefractory

        if (detected) {
            mutableTapPeakSamples += peakSample
            lastTapPeakSample = peakSample
            if (mutableTapPeakSamples.size == requiredTaps) {
                tailFramesRemaining = tailFramesAfterThirdTap
            }
        } else if (rms < threshold) {
            // 只用安静帧缓慢更新噪声底，避免把敲击余响学成“环境噪声”。
            noiseFloorRms = NOISE_EMA_ALPHA * noiseFloorRms + (1.0 - NOISE_EMA_ALPHA) * rms
        }

        advanceTailIfNeeded(detected)

        return TapFrameUpdate(
            detectedTap = detected,
            tapCount = mutableTapPeakSamples.size,
            frameRms = rms,
            peakAmplitude = peak
        )
    }

    fun shouldStopAfterFrame(): Boolean = tailFramesRemaining == 0

    private fun advanceTailIfNeeded(detectedTap: Boolean) {
        val remaining = tailFramesRemaining ?: return
        if (detectedTap) return
        if (remaining > 0) tailFramesRemaining = remaining - 1
    }

    companion object {
        const val REQUIRED_TAPS = 3
        const val DEFAULT_MIN_TAP_RMS = 0.025
        const val DEFAULT_MIN_PEAK_AMPLITUDE = 0.10
        const val DEFAULT_NOISE_MULTIPLIER = 2.8
        const val DEFAULT_MIN_CREST_FACTOR = 1.65
        const val DEFAULT_REFRACTORY_MS = 140
        const val DEFAULT_TAIL_FRAMES = 2
        private const val DEFAULT_INITIAL_NOISE_RMS = 0.006
        private const val NOISE_EMA_ALPHA = 0.88
    }
}
