package com.example.watermelonripeness.analysis

import com.example.watermelonripeness.classifier.RipenessScale

/**
 * 100 ms 实时显示用的轻量频率跟踪器。
 *
 * 低于能量阈值的帧视为环境噪声，不推动仪表盘；有效帧使用轻度指数平滑，
 * 在保留响应速度的同时减少指针抖动。
 */
data class LiveFrequencyReading(
    val rawFrequencyHz: Double,
    val displayFrequencyHz: Double,
    val gaugeValue: Float,
    val rms: Double
)

class LiveFrequencyTracker(
    private val minRms: Double = DEFAULT_MIN_RMS,
    private val smoothingAlpha: Double = DEFAULT_SMOOTHING_ALPHA
) {
    init {
        require(minRms >= 0.0) { "minRms must be >= 0" }
        require(smoothingAlpha in 0.0..1.0) { "smoothingAlpha must be between 0 and 1" }
    }

    private var previousDisplayFrequencyHz: Double? = null

    var validReadingCount: Int = 0
        private set

    fun reset() {
        previousDisplayFrequencyHz = null
        validReadingCount = 0
    }

    fun analyze(pcm: ShortArray, sampleRate: Int): LiveFrequencyReading? {
        val features = FeatureExtractor.extract(pcm, sampleRate)
        if (features.rms < minRms) return null

        val rawFrequency = features.dominantFrequencyHz
        val displayFrequency = previousDisplayFrequencyHz?.let { previous ->
            smoothingAlpha * rawFrequency + (1.0 - smoothingAlpha) * previous
        } ?: rawFrequency

        previousDisplayFrequencyHz = displayFrequency
        validReadingCount += 1

        return LiveFrequencyReading(
            rawFrequencyHz = rawFrequency,
            displayFrequencyHz = displayFrequency,
            gaugeValue = RipenessScale.gaugeValue(displayFrequency),
            rms = features.rms
        )
    }

    companion object {
        // 占位阈值：后续需要结合真实手机、环境噪声和西瓜样本校准。
        const val DEFAULT_MIN_RMS = 0.012
        const val DEFAULT_SMOOTHING_ALPHA = 0.60
    }
}
