package com.example.watermelonripeness

import com.example.watermelonripeness.classifier.SessionClassification

data class DetectionSummaryUiModel(
    val headline: String,
    val ripenessLabel: String,
    val stabilityLabel: String,
    val explanation: String,
    val referenceText: String
)

object DetectionSummaryFormatter {
    fun format(result: SessionClassification): DetectionSummaryUiModel {
        val reference = result.referenceFrequencyHz?.let { "声音参考值：%.0f Hz".format(it) }.orEmpty()
        return DetectionSummaryUiModel(
            headline = result.purchaseDecision.displayName,
            ripenessLabel = result.ripeness?.displayName.orEmpty(),
            stabilityLabel = result.stability.displayName,
            explanation = result.explanation,
            referenceText = reference
        )
    }
}
