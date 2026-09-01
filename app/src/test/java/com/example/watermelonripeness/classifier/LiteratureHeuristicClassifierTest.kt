package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.TapFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiteratureHeuristicClassifierTest {
    private val classifier = LiteratureHeuristicClassifier()

    @Test
    fun fewerThanThreeTapsRequestsRetry() {
        val result = classifier.classify(listOf(ripeTap(), ripeTap()))
        assertEquals(PurchaseDecision.RETRY, result.purchaseDecision)
        assertNull(result.ripeness)
    }

    @Test
    fun consistentRipeLikeTapsRecommendPurchase() {
        val result = classifier.classify(listOf(ripeTap(145.0), ripeTap(151.0), ripeTap(148.0)))
        assertEquals(PurchaseDecision.RECOMMEND, result.purchaseDecision)
        assertEquals(Ripeness.RIPE, result.ripeness)
        assertEquals(DetectionStability.STABLE, result.stability)
    }

    @Test
    fun consistentlyHighFrequencyTapsAreUnderRipeAndNotRecommended() {
        val taps = listOf(underTap(255.0), underTap(262.0), underTap(258.0))
        val result = classifier.classify(taps)
        assertEquals(PurchaseDecision.DO_NOT_BUY, result.purchaseDecision)
        assertEquals(Ripeness.UNDERRIPE, result.ripeness)
    }

    @Test
    fun consistentlyVeryLowFrequencyFastDecayTapsAreOverRipeAndNotRecommended() {
        val taps = listOf(overTap(92.0), overTap(97.0), overTap(94.0))
        val result = classifier.classify(taps)
        assertEquals(PurchaseDecision.DO_NOT_BUY, result.purchaseDecision)
        assertEquals(Ripeness.OVERRIPE, result.ripeness)
    }

    @Test
    fun inconsistentTapsRequestRetryInsteadOfGuessing() {
        val result = classifier.classify(listOf(underTap(265.0), ripeTap(145.0), overTap(90.0)))
        assertEquals(PurchaseDecision.RETRY, result.purchaseDecision)
        assertNull(result.ripeness)
        assertEquals(DetectionStability.UNSTABLE, result.stability)
    }

    @Test
    fun oneVeryNoisyTapRequestsRetryEvenIfAverageLooksUsable() {
        val noisy = ripeTap(148.0).copy(snrDb = 1.0)
        val result = classifier.classify(listOf(noisy, ripeTap(150.0), ripeTap(146.0)))
        assertEquals(PurchaseDecision.RETRY, result.purchaseDecision)
        assertNull(result.ripeness)
    }

    private fun ripeTap(frequency: Double = 148.0) = TapFeatures(frequency, 700.0, 0.75, -12.0, 18.0)
    private fun underTap(frequency: Double) = TapFeatures(frequency, 1450.0, 0.25, -5.0, 18.0)
    private fun overTap(frequency: Double) = TapFeatures(frequency, 450.0, 0.94, -18.0, 18.0)
}
