package com.recall.app.core.vector

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveSearchConfigTest {
    private val config = AdaptiveSearchConfig()

    @Test
    fun effectiveEfSearch_default_returnsBase() {
        assertEquals(50, config.effectiveEfSearch())
    }

    @Test
    fun effectiveEfSearch_largeIndex_scalesUp() {
        assertEquals(75, config.effectiveEfSearch(indexSize = 50_001))
    }

    @Test
    fun effectiveEfSearch_lowBattery_reducesEf() {
        assertEquals(25, config.effectiveEfSearch(isBatteryLow = true))
    }

    @Test
    fun effectiveEfSearch_thermalThrottled_reducesEfCoercedToMin() {
        assertEquals(20, config.effectiveEfSearch(isThermalThrottled = true))
    }

    @Test
    fun effectiveEfSearch_largeIndexAndLowBattery_appliesBoth() {
        assertEquals(37, config.effectiveEfSearch(isBatteryLow = true, indexSize = 60_000))
    }

    @Test
    fun effectiveEfSearch_batteryAndThermal_appliesBothReductions() {
        assertEquals(20, config.effectiveEfSearch(isBatteryLow = true, isThermalThrottled = true))
    }

    @Test
    fun effectiveEfSearch_customConfig_respectsBounds() {
        val tight = AdaptiveSearchConfig(
            baseEfSearch = 300,
            minEfSearch = 10,
            maxEfSearch = 100,
        )
        assertEquals(100, tight.effectiveEfSearch())
    }

    @Test
    fun effectiveEfSearch_smallIndex_doesNotScale() {
        assertEquals(50, config.effectiveEfSearch(indexSize = 50_000))
    }
}
