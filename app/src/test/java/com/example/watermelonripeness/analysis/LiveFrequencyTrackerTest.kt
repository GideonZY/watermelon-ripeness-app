package com.example.watermelonripeness.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LiveFrequencyTrackerTest {
    @Test
    fun quietFrameDoesNotMoveGauge() {
        val tracker = LiveFrequencyTracker(minRms = 0.012)
        assertNull(tracker.analyze(ShortArray(1600), 16_000))
        assertEquals(0, tracker.validReadingCount)
    }

    @Test
    fun validFrameProducesFrequencyReading() {
        val tracker = LiveFrequencyTracker(minRms = 0.001)
        val reading = tracker.analyze(sineFrame(320.0), 16_000)
        assertNotNull(reading)
        assertEquals(320.0, reading!!.displayFrequencyHz, 20.0)
        assertEquals(1, tracker.validReadingCount)
    }

    @Test
    fun subsequentFrameUsesLightSmoothing() {
        val tracker = LiveFrequencyTracker(minRms = 0.001, smoothingAlpha = 0.60)
        val first = tracker.analyze(sineFrame(250.0), 16_000)!!
        val second = tracker.analyze(sineFrame(350.0), 16_000)!!
        val expected = 0.60 * second.rawFrequencyHz + 0.40 * first.displayFrequencyHz
        assertEquals(expected, second.displayFrequencyHz, 0.001)
    }

    private fun sineFrame(frequencyHz: Double): ShortArray = ShortArray(1600) { index ->
        val value = 0.60 * sin(2.0 * PI * frequencyHz * index / 16_000.0)
        (value * Short.MAX_VALUE).toInt().toShort()
    }
}
