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
            Classification(Ripeness.RIPE, 0.55f, "声音较饱满，当前更接近适熟状态。"),
            AudioFeatures(275.4, 932.6, 0.12345, -8.75)
        )
        assertTrue(text.contains("检测结果：适熟"))
        assertTrue(text.contains("敲击声频率：275 Hz"))
        assertTrue(text.contains("声音较饱满，当前更接近适熟状态。"))
        assertTrue(text.contains("结果仅供参考"))
        assertFalse(text.contains("频谱质心"))
        assertFalse(text.contains("占位规则"))
        assertFalse(text.contains("本次录音未保存"))
        assertFalse(text.contains("%."))
    }
}
