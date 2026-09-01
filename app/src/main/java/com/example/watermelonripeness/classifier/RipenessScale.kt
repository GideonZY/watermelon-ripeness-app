package com.example.watermelonripeness.classifier

/**
 * v0.2.0 占位标定参数。
 * 真实阈值必须在收集并切瓜标注足够样本后重新校准。
 */
object RipenessScale {
    const val RIPE_CENTER_HZ = 275.0
    const val RIPE_LOW_HZ = 190.0
    const val RIPE_HIGH_HZ = 360.0

    // 让现有 190/360 Hz 边界对应仪表盘中心两侧约 1/3 的位置。
    private const val GAUGE_FULL_SCALE_HZ = 255.0

    fun gaugeValue(dominantFrequencyHz: Double): Float =
        ((RIPE_CENTER_HZ - dominantFrequencyHz) / GAUGE_FULL_SCALE_HZ)
            .coerceIn(-1.0, 1.0)
            .toFloat()

    fun ripenessFor(dominantFrequencyHz: Double): Ripeness = when {
        dominantFrequencyHz > RIPE_HIGH_HZ -> Ripeness.UNDERRIPE
        dominantFrequencyHz < RIPE_LOW_HZ -> Ripeness.OVERRIPE
        else -> Ripeness.RIPE
    }
}
