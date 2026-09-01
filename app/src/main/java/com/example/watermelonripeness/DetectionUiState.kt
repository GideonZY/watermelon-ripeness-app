package com.example.watermelonripeness

enum class DetectionPhase {
    IDLE,
    DETECTING,
    COMPLETE
}

data class DetectionUiVisibility(
    val showInstructions: Boolean,
    val showLiveDetection: Boolean,
    val showResult: Boolean,
    val showStatus: Boolean
)

object DetectionUiState {
    fun forPhase(phase: DetectionPhase): DetectionUiVisibility = when (phase) {
        DetectionPhase.IDLE -> DetectionUiVisibility(
            showInstructions = true,
            showLiveDetection = false,
            showResult = false,
            showStatus = false
        )
        DetectionPhase.DETECTING -> DetectionUiVisibility(
            showInstructions = false,
            showLiveDetection = true,
            showResult = false,
            showStatus = true
        )
        DetectionPhase.COMPLETE -> DetectionUiVisibility(
            showInstructions = false,
            showLiveDetection = false,
            showResult = true,
            showStatus = false
        )
    }
}
