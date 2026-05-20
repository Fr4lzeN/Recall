package com.recall.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelProfileSelectorTest {
    @Test
    fun selectsPro_forHighRamDevice() {
        val profile = ModelProfileSelector.selectProfileForDevice(deviceInfo(totalRamMb = 12_000))
        assertEquals(ModelProfile.PRO, profile)
    }

    @Test
    fun selectsStandard_forMidRamDevice() {
        val profile = ModelProfileSelector.selectProfileForDevice(deviceInfo(totalRamMb = 6_000))
        assertEquals(ModelProfile.STANDARD, profile)
    }

    @Test
    fun selectsLite_forLowRamDevice() {
        val profile = ModelProfileSelector.selectProfileForDevice(deviceInfo(totalRamMb = 3_000))
        assertEquals(ModelProfile.LITE, profile)
    }

    @Test
    fun selectsPro_atProRamThreshold() {
        val profile = ModelProfileSelector.selectProfileForDevice(
            deviceInfo(totalRamMb = ModelProfile.PRO.minRamMb),
        )
        assertEquals(ModelProfile.PRO, profile)
    }

    @Test
    fun selectsStandard_atStandardRamThreshold() {
        val profile = ModelProfileSelector.selectProfileForDevice(
            deviceInfo(totalRamMb = ModelProfile.STANDARD.minRamMb),
        )
        assertEquals(ModelProfile.STANDARD, profile)
    }

    @Test
    fun selectsLite_justBelowStandardThreshold() {
        val profile = ModelProfileSelector.selectProfileForDevice(
            deviceInfo(totalRamMb = ModelProfile.STANDARD.minRamMb - 1),
        )
        assertEquals(ModelProfile.LITE, profile)
    }

    private fun deviceInfo(totalRamMb: Int) = DeviceInfo(
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        cpuCores = 4,
        androidVersion = 34,
        freeDiskMb = 10_000L,
        supportsNnapi = true,
    )
}
