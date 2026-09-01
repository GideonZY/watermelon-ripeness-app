package com.example.watermelonripeness

enum class DetectionPhase {
    IDLE,
    DETECTING,
    COMPLETE
}

data class DetectionUiVisibility(
    val showLiveDetection: Boolean,
    val showResult: Boolean,
    val showStatus: Boolean
)

object DetectionUiState {
    fun forPhase(phase: DetectionPhase): DetectionUiVisibility = when (phase) {
        DetectionPhase.DETECTING -> DetectionUiVisibility(
            showLiveDetection = true,
            showResult = false,
            showStatus = true
        )
        DetectionPhase.IDLE,
        DetectionPhase.COMPLETE -> DetectionUiVisibility(
            showLiveDetection = false,
            showResult = true,
            showStatus = false
        )
    }
}
