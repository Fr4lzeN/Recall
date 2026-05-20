package com.recall.app.core.ml

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceInfo(
    val totalRamMb: Int,
    val availableRamMb: Int,
    val cpuCores: Int,
    val androidVersion: Int,
    val freeDiskMb: Long,
    val supportsNnapi: Boolean,
)

@Singleton
class DeviceProfiler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun profile(): DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availableRamMb = (memInfo.availMem / (1024 * 1024)).toInt()
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val dataDir = context.filesDir
        val stat = StatFs(dataDir.path)
        val freeDiskMb = stat.availableBytes / (1024 * 1024)

        val supportsNnapi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        return DeviceInfo(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            cpuCores = cpuCores,
            androidVersion = Build.VERSION.SDK_INT,
            freeDiskMb = freeDiskMb,
            supportsNnapi = supportsNnapi,
        )
    }
}
