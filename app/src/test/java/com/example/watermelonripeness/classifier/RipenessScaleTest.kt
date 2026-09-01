package com.example.watermelonripeness.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class RipenessScaleTest {
    @Test
    fun gaugeUsesExistingRipeBoundariesAndCenter() {
        assertEquals(0f, RipenessScale.gaugeValue(275.0), 0.002f)
        assertEquals(-1f / 3f, RipenessScale.gaugeValue(360.0), 0.002f)
        assertEquals(1f / 3f, RipenessScale.gaugeValue(190.0), 0.002f)
    }

    @Test
    fun gaugeClampsExtremeFrequencies() {
        assertEquals(-1f, RipenessScale.gaugeValue(600.0), 0.002f)
        assertEquals(1f, RipenessScale.gaugeValue(0.0), 0.002f)
    }

    @Test
    fun classificationKeepsV01FrequencyBoundaries() {
        assertEquals(Ripeness.UNDERRIPE, RipenessScale.ripenessFor(361.0))
        assertEquals(Ripeness.RIPE, RipenessScale.ripenessFor(360.0))
        assertEquals(Ripeness.RIPE, RipenessScale.ripenessFor(275.0))
        assertEquals(Ripeness.RIPE, RipenessScale.ripenessFor(190.0))
        assertEquals(Ripeness.OVERRIPE, RipenessScale.ripenessFor(189.0))
    }
}
