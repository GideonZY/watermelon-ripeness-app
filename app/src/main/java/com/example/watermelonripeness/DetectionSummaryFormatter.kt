package com.example.watermelonripeness

import com.example.watermelonripeness.analysis.AudioFeatures
import com.example.watermelonripeness.classifier.Classification

object DetectionSummaryFormatter {
    fun format(result: Classification, features: AudioFeatures): String = buildString {
        append("检测结果：${result.ripeness.displayName}")
        append('\n')
        append("敲击声频率：%.0f Hz".format(features.dominantFrequencyHz))
        append('\n')
        append(result.explanation)
        append('\n')
        append("温馨提示：结果仅供参考，建议结合手感、瓜纹和瓜脐综合判断。")
    }
}
