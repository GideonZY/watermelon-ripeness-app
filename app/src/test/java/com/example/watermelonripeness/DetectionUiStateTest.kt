package com.example.watermelonripeness

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionUiStateTest {
    @Test
    fun idleShowsInstructionsOnly() {
        val state = DetectionUiState.forPhase(DetectionPhase.IDLE)
        assertTrue(state.showInstructions)
        assertFalse(state.showLiveDetection)
        assertFalse(state.showResult)
        assertFalse(state.showStatus)
    }

    @Test
    fun detectingShowsLiveGaugeOnly() {
        val state = DetectionUiState.forPhase(DetectionPhase.DETECTING)
        assertFalse(state.showInstructions)
        assertTrue(state.showLiveDetection)
        assertFalse(state.showResult)
        assertTrue(state.showStatus)
    }

    @Test
    fun completeShowsResultDirectlyUnderButton() {
        val state = DetectionUiState.forPhase(DetectionPhase.COMPLETE)
        assertFalse(state.showInstructions)
        assertFalse(state.showLiveDetection)
        assertTrue(state.showResult)
        assertFalse(state.showStatus)
    }
}
