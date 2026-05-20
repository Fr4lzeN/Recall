package com.recall.app.core.ml

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelProfileSelector @Inject constructor(
    private val deviceProfiler: DeviceProfiler,
) {
    fun selectProfile(): ModelProfile {
        val info = deviceProfiler.profile()
        return when {
            info.totalRamMb >= ModelProfile.PRO.minRamMb -> ModelProfile.PRO
            info.totalRamMb >= ModelProfile.STANDARD.minRamMb -> ModelProfile.STANDARD
            else -> ModelProfile.LITE
        }
    }

    fun canUseProfile(profile: ModelProfile): Boolean {
        val info = deviceProfiler.profile()
        return info.totalRamMb >= profile.minRamMb
    }
}
