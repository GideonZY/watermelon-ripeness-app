package com.example.watermelonripeness

import com.example.watermelonripeness.analysis.AudioFeatures
import com.example.watermelonripeness.classifier.Classification
import com.example.watermelonripeness.classifier.Ripeness
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionSummaryFormatterTest {
    @Test
    fun formatsReadableDetectionResult() {
        val text = DetectionSummaryFormatter.format(
            Classification(Ripeness.RIPE, 0.55f, "测试说明"),
            AudioFeatures(275.4, 932.6, 0.12345, -8.75)
        )
        assertTrue(text.contains("适熟"))
        assertTrue(text.contains("主频 275 Hz"))
        assertTrue(text.contains("测试说明"))
        assertTrue(text.contains("频谱质心 933 Hz"))
        assertTrue(text.contains("能量 0.1235"))
        assertTrue(text.contains("衰减 -8.8 dB"))
        assertFalse(text.contains("%."))
    }
}
