package com.example.watermelonripeness

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionUiStateTest {
    @Test
    fun idleAndCompleteKeepResultVisibleAndLiveGaugeHidden() {
        listOf(DetectionPhase.IDLE, DetectionPhase.COMPLETE).forEach { phase ->
            val state = DetectionUiState.forPhase(phase)
            assertFalse(state.showLiveDetection)
            assertTrue(state.showResult)
            assertFalse(state.showStatus)
        }
    }

    @Test
    fun detectingShowsLiveGaugeAndHidesResult() {
        val state = DetectionUiState.forPhase(DetectionPhase.DETECTING)
        assertTrue(state.showLiveDetection)
        assertFalse(state.showResult)
        assertTrue(state.showStatus)
    }
}
