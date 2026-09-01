package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.AudioFeatures

class RuleBasedClassifier : RipenessClassifier {
    override fun classify(features: AudioFeatures, pcm: ShortArray, sampleRate: Int): Classification {
        // v0.2.0 仍为占位规则，不是科学成熟度标准；真实阈值需用标注样本重新校准。
        val result = RipenessScale.ripenessFor(features.dominantFrequencyHz)
        val why = when (result) {
            Ripeness.UNDERRIPE -> "主频高于 %.0f Hz，声音偏高（占位规则）".format(RipenessScale.RIPE_HIGH_HZ)
            Ripeness.RIPE -> "主频位于 %.0f～%.0f Hz 的暂定适熟区间（占位规则）".format(
                RipenessScale.RIPE_LOW_HZ,
                RipenessScale.RIPE_HIGH_HZ
            )
            Ripeness.OVERRIPE -> "主频低于 %.0f Hz，声音偏低（占位规则）".format(RipenessScale.RIPE_LOW_HZ)
        }
        return Classification(result, 0.55f, why)
    }
}
