package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.AudioFeatures

class RuleBasedClassifier : RipenessClassifier {
    override fun classify(features: AudioFeatures, pcm: ShortArray, sampleRate: Int): Classification {
        // 仅为 MVP 占位规则，不是科学成熟度标准；需用真实标注数据重新校准。
        val result = when {
            features.dominantFrequencyHz > 360 || features.spectralCentroidHz > 1450 -> Ripeness.UNDERRIPE
            features.dominantFrequencyHz < 190 || features.decayRatio > -7 -> Ripeness.OVERRIPE
            else -> Ripeness.RIPE
        }
        val why = when (result) {
            Ripeness.UNDERRIPE -> "声音偏高、偏亮（占位规则）"
            Ripeness.RIPE -> "主频与衰减处于中间范围（占位规则）"
            Ripeness.OVERRIPE -> "声音偏低或衰减较慢（占位规则）"
        }
        return Classification(result, 0.55f, why)
    }
}
