package com.recall.app.core.vector

data class AdaptiveSearchConfig(
    val baseEfSearch: Int = 50,
    val minEfSearch: Int = 20,
    val maxEfSearch: Int = 200,
    val lowBatteryReduction: Float = 0.5f,
    val thermalThrottleReduction: Float = 0.3f,
) {
    fun effectiveEfSearch(
        isBatteryLow: Boolean = false,
        isThermalThrottled: Boolean = false,
        indexSize: Int = 0,
    ): Int {
        var ef = baseEfSearch
        if (indexSize > 50_000) ef = (ef * 1.5).toInt()
        if (isBatteryLow) ef = (ef * lowBatteryReduction).toInt()
        if (isThermalThrottled) ef = (ef * thermalThrottleReduction).toInt()
        return ef.coerceIn(minEfSearch, maxEfSearch)
    }
}
