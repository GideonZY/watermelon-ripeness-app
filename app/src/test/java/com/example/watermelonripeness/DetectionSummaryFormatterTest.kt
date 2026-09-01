package com.example.watermelonripeness

import com.example.watermelonripeness.classifier.DetectionStability
import com.example.watermelonripeness.classifier.PurchaseDecision
import com.example.watermelonripeness.classifier.Ripeness
import com.example.watermelonripeness.classifier.SessionClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionSummaryFormatterTest {
    @Test
    fun ripeResultLeadsWithPurchaseDecision() {
        val ui = DetectionSummaryFormatter.format(
            SessionClassification(
                ripeness = Ripeness.RIPE,
                purchaseDecision = PurchaseDecision.RECOMMEND,
                stability = DetectionStability.STABLE,
                referenceFrequencyHz = 148.4,
                maturityIndex = 0.62,
                explanation = "三次拍击的共振与衰减特征较一致，更接近适熟区间。"
            )
        )
        assertEquals("推荐购买", ui.headline)
        assertEquals("适熟", ui.ripenessLabel)
        assertEquals("检测稳定", ui.stabilityLabel)
        assertEquals("声音参考值：148 Hz", ui.referenceText)
        assertFalse(ui.explanation.contains("频谱质心"))
        assertFalse(ui.explanation.contains("占位规则"))
    }

    @Test
    fun retryDoesNotPretendToKnowRipeness() {
        val ui = DetectionSummaryFormatter.format(
            SessionClassification(
                ripeness = null,
                purchaseDecision = PurchaseDecision.RETRY,
                stability = DetectionStability.UNSTABLE,
                referenceFrequencyHz = 170.0,
                maturityIndex = null,
                explanation = "三次拍击差异较大，请重新检测。"
            )
        )
        assertEquals("建议重新检测", ui.headline)
        assertTrue(ui.ripenessLabel.isEmpty())
        assertEquals("结果不稳定", ui.stabilityLabel)
    }
}
