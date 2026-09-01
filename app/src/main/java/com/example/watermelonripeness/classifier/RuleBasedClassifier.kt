package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.AudioFeatures

class RuleBasedClassifier : RipenessClassifier {
    override fun classify(features: AudioFeatures, pcm: ShortArray, sampleRate: Int): Classification {
        // 仍为首版参考区间，后续需要结合真实样本重新校准。
        val result = RipenessScale.ripenessFor(features.dominantFrequencyHz)
        val why = when (result) {
            Ripeness.UNDERRIPE -> "敲击声偏清脆，当前更接近偏生状态。"
            Ripeness.RIPE -> "敲击声落在当前参考范围内，更接近适熟状态。"
            Ripeness.OVERRIPE -> "敲击声偏沉一些，当前更接近偏熟状态。"
        }
        return Classification(result, 0.55f, why)
    }
}
