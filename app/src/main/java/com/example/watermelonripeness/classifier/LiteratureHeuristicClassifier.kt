package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.TapFeatures
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * v0.3.0 文献启发式分类器。
 *
 * 公开研究较一致地支持“成熟度增加时共振频率总体下降、瞬态衰减加快”，
 * 但品种、拍击位置和力度会显著影响绝对数值。因此这里用多个特征形成连续成熟度指数，
 * 再用三次拍击的一致性做质量门控。阈值是工程起点，不是经过本项目真实标注样本验证的最终模型。
 */
class LiteratureHeuristicClassifier {
    fun classify(taps: List<TapFeatures>): SessionClassification {
        if (taps.size < REQUIRED_TAPS) {
            return SessionClassification(
                ripeness = null,
                purchaseDecision = PurchaseDecision.RETRY,
                stability = DetectionStability.INSUFFICIENT,
                referenceFrequencyHz = taps.takeIf { it.isNotEmpty() }?.map { it.dominantFrequencyHz }?.median(),
                maturityIndex = null,
                explanation = "没有识别到足够的清晰拍击，请靠近西瓜中部重新检测。"
            )
        }

        val selected = taps.take(REQUIRED_TAPS)
        val indices = selected.map(::maturityIndex)
        val frequencies = selected.map { it.dominantFrequencyHz }
        val referenceFrequency = frequencies.median()
        val meanFrequency = frequencies.average().coerceAtLeast(1e-6)
        val frequencyStd = sqrt(frequencies.sumOf { (it - meanFrequency).pow(2) } / frequencies.size)
        val frequencyCv = frequencyStd / meanFrequency
        val indexRange = (indices.maxOrNull() ?: 0.0) - (indices.minOrNull() ?: 0.0)
        val averageSnr = selected.map { it.snrDb }.average()

        val hasVeryNoisyTap = selected.any { it.snrDb < MIN_TAP_SNR_DB }

        if (hasVeryNoisyTap || averageSnr < MIN_AVERAGE_SNR_DB || frequencyCv > MAX_FREQUENCY_CV || indexRange > MAX_INDEX_RANGE) {
            return SessionClassification(
                ripeness = null,
                purchaseDecision = PurchaseDecision.RETRY,
                stability = DetectionStability.UNSTABLE,
                referenceFrequencyHz = referenceFrequency,
                maturityIndex = indices.median(),
                explanation = "三次拍击的声音差异较大，建议保持相同位置和力度重新检测。"
            )
        }

        val sessionIndex = indices.median()
        val ripeness = when {
            sessionIndex < RIPE_LOWER_INDEX -> Ripeness.UNDERRIPE
            sessionIndex > RIPE_UPPER_INDEX -> Ripeness.OVERRIPE
            else -> Ripeness.RIPE
        }
        val purchaseDecision = if (ripeness == Ripeness.RIPE) {
            PurchaseDecision.RECOMMEND
        } else {
            PurchaseDecision.DO_NOT_BUY
        }
        val explanation = when (ripeness) {
            Ripeness.UNDERRIPE -> "三次拍击整体偏高频，内部组织可能仍较紧实，更接近偏生状态。"
            Ripeness.RIPE -> "三次拍击的共振与衰减特征较一致，更接近适熟区间。"
            Ripeness.OVERRIPE -> "三次拍击整体偏低频且衰减较快，声音特征更接近偏熟状态。"
        }

        return SessionClassification(
            ripeness = ripeness,
            purchaseDecision = purchaseDecision,
            stability = DetectionStability.STABLE,
            referenceFrequencyHz = referenceFrequency,
            maturityIndex = sessionIndex,
            explanation = explanation
        )
    }

    fun maturityIndex(tap: TapFeatures): Double {
        val frequencyMaturity = inverseNormalize(
            tap.dominantFrequencyHz,
            matureLow = FREQUENCY_OVERRIPE_HZ,
            immatureHigh = FREQUENCY_UNDERRIPE_HZ
        )
        val decayMaturity = inverseNormalize(
            tap.decayDb,
            matureLow = DECAY_FAST_DB,
            immatureHigh = DECAY_SLOW_DB
        )
        val lowBandMaturity = normalize(tap.lowBandEnergyRatio, LOW_BAND_LOW, LOW_BAND_HIGH)
        val centroidMaturity = inverseNormalize(
            tap.spectralCentroidHz,
            matureLow = CENTROID_LOW_HZ,
            immatureHigh = CENTROID_HIGH_HZ
        )

        return (
            frequencyMaturity * FREQUENCY_WEIGHT +
                decayMaturity * DECAY_WEIGHT +
                lowBandMaturity * LOW_BAND_WEIGHT +
                centroidMaturity * CENTROID_WEIGHT
            ).coerceIn(0.0, 1.0)
    }

    private fun normalize(value: Double, low: Double, high: Double): Double {
        if (high <= low) return 0.0
        return ((value - low) / (high - low)).coerceIn(0.0, 1.0)
    }

    private fun inverseNormalize(value: Double, matureLow: Double, immatureHigh: Double): Double {
        if (immatureHigh <= matureLow) return 0.0
        return ((immatureHigh - value) / (immatureHigh - matureLow)).coerceIn(0.0, 1.0)
    }

    private fun List<Double>.median(): Double {
        if (isEmpty()) return 0.0
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    companion object {
        const val REQUIRED_TAPS = 3

        // 以下均为 0.3.0 的可校准工程参数；后续必须用真实切瓜标注数据重新拟合。
        const val FREQUENCY_UNDERRIPE_HZ = 240.0
        const val FREQUENCY_OVERRIPE_HZ = 90.0
        const val DECAY_SLOW_DB = -4.0
        const val DECAY_FAST_DB = -18.0
        const val LOW_BAND_LOW = 0.30
        const val LOW_BAND_HIGH = 0.90
        const val CENTROID_LOW_HZ = 500.0
        const val CENTROID_HIGH_HZ = 1600.0

        const val FREQUENCY_WEIGHT = 0.60
        const val DECAY_WEIGHT = 0.22
        const val LOW_BAND_WEIGHT = 0.13
        const val CENTROID_WEIGHT = 0.05

        const val RIPE_LOWER_INDEX = 0.38
        const val RIPE_UPPER_INDEX = 0.82

        const val MIN_TAP_SNR_DB = 5.0
        const val MIN_AVERAGE_SNR_DB = 8.0
        const val MAX_FREQUENCY_CV = 0.18
        const val MAX_INDEX_RANGE = 0.28
    }
}
