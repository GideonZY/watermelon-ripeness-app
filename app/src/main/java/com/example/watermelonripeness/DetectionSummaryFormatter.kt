package com.example.watermelonripeness

import com.example.watermelonripeness.analysis.AudioFeatures
import com.example.watermelonripeness.classifier.Classification

object DetectionSummaryFormatter {
    fun format(result: Classification, features: AudioFeatures): String =
        "${result.ripeness.displayName}\n主频 %.0f Hz\n\n${result.explanation}\n频谱质心 %.0f Hz · 能量 %.4f · 衰减 %.1f dB".format(
            features.dominantFrequencyHz,
            features.spectralCentroidHz,
            features.rms,
            features.decayRatio
        )
}
