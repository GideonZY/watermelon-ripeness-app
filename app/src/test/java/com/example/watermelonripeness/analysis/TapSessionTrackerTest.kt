package com.example.watermelonripeness.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class TapSessionTrackerTest {
    @Test
    fun quietFramesDoNotCreateTaps() {
        val tracker = TapSessionTracker()
        repeat(10) { index ->
            val update = tracker.processFrame(ShortArray(1600), 16_000, index * 1600)
            assertFalse(update.detectedTap)
        }
        assertEquals(0, tracker.tapPeakSamples.size)
    }

    @Test
    fun threeSeparatedTransientTapsAreCounted() {
        val tracker = TapSessionTracker()
        val starts = listOf(0, 3200, 6400)
        starts.forEach { start ->
            val update = tracker.processFrame(tapFrame(), 16_000, start)
            assertTrue(update.detectedTap)
        }
        assertEquals(3, tracker.tapPeakSamples.size)
        assertFalse(tracker.shouldStopAfterFrame())
    }

    @Test
    fun sameTransientTailIsNotDoubleCountedInsideRefractoryWindow() {
        val tracker = TapSessionTracker(refractoryMs = 140)
        assertTrue(tracker.processFrame(tapFrame(), 16_000, 0).detectedTap)
        assertFalse(tracker.processFrame(tapFrame(amplitude = 0.25), 16_000, 1600).detectedTap)
        assertEquals(1, tracker.tapPeakSamples.size)
    }

    @Test
    fun recordingStopsOnlyAfterTwoTailFramesFollowingThirdTap() {
        val tracker = TapSessionTracker(tailFramesAfterThirdTap = 2)
        listOf(0, 3200, 6400).forEach { tracker.processFrame(tapFrame(), 16_000, it) }
        assertFalse(tracker.shouldStopAfterFrame())
        tracker.processFrame(ShortArray(1600), 16_000, 8000)
        assertFalse(tracker.shouldStopAfterFrame())
        tracker.processFrame(ShortArray(1600), 16_000, 9600)
        assertTrue(tracker.shouldStopAfterFrame())
    }

    private fun tapFrame(amplitude: Double = 0.85): ShortArray = ShortArray(1600) { index ->
        if (index < 120) return@ShortArray 0
        val t = (index - 120) / 16_000.0
        val value = amplitude * exp(-22.0 * t) * sin(2.0 * PI * 165.0 * t)
        (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}
