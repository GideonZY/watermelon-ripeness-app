package com.example.watermelonripeness.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class RipenessScaleTest {
    @Test
    fun liveGaugeUsesLiteratureInformedFrequencyDirection() {
        assertEquals(0f, RipenessScale.gaugeValue(150.0), 0.002f)
        assertEquals(-1f, RipenessScale.gaugeValue(240.0), 0.002f)
        assertEquals(1f, RipenessScale.gaugeValue(90.0), 0.002f)
    }

    @Test
    fun liveGaugeClampsExtremes() {
        assertEquals(-1f, RipenessScale.gaugeValue(500.0), 0.002f)
        assertEquals(1f, RipenessScale.gaugeValue(20.0), 0.002f)
    }
}
