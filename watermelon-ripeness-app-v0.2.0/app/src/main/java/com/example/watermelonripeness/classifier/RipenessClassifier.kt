package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.AudioFeatures

enum class Ripeness(val displayName: String) { UNDERRIPE("偏生"), RIPE("适熟"), OVERRIPE("偏熟") }
data class Classification(val ripeness: Ripeness, val confidence: Float, val explanation: String)

interface RipenessClassifier {
    fun classify(features: AudioFeatures, pcm: ShortArray, sampleRate: Int): Classification
}
