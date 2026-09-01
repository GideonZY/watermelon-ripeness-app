package com.example.watermelonripeness.classifier

/**
 * 仅用于“检测中”的实时频率仪表盘，不负责最终购买结论。
 * 最终结论由 LiteratureHeuristicClassifier 综合三次拍击的多种特征给出。
 */
object RipenessScale {
    const val RIPE_CENTER_HZ = 150.0
    const val UNDERRIPE_REFERENCE_HZ = 240.0
    const val OVERRIPE_REFERENCE_HZ = 90.0

    fun gaugeValue(dominantFrequencyHz: Double): Float {
        val value = if (dominantFrequencyHz >= RIPE_CENTER_HZ) {
            -((dominantFrequencyHz - RIPE_CENTER_HZ) / (UNDERRIPE_REFERENCE_HZ - RIPE_CENTER_HZ))
        } else {
            (RIPE_CENTER_HZ - dominantFrequencyHz) / (RIPE_CENTER_HZ - OVERRIPE_REFERENCE_HZ)
        }
        return value.coerceIn(-1.0, 1.0).toFloat()
    }

    @Deprecated("0.3.0 final result uses LiteratureHeuristicClassifier")
    fun ripenessFor(dominantFrequencyHz: Double): Ripeness = when {
        dominantFrequencyHz > 210.0 -> Ripeness.UNDERRIPE
        dominantFrequencyHz < 110.0 -> Ripeness.OVERRIPE
        else -> Ripeness.RIPE
    }
}
