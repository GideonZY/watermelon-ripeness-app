package com.example.watermelonripeness.analysis

data class AudioFeatures(
    val dominantFrequencyHz: Double,
    val spectralCentroidHz: Double,
    val rms: Double,
    val decayRatio: Double
)
