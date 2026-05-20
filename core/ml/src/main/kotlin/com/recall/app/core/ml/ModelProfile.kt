package com.recall.app.core.ml

enum class QuantizationType { FLOAT32, FLOAT16, INT8 }

data class ModelProfile(
    val name: String,
    val dimensions: Int,
    val quantization: QuantizationType,
    val modelFileName: String,
    val minRamMb: Int,
    val expectedLatencyMs: Long,
) {
    companion object {
        val LITE = ModelProfile("Lite", 384, QuantizationType.INT8, "mobileclip_lite_int8.tflite", 2048, 100)
        val STANDARD = ModelProfile("Standard", 512, QuantizationType.FLOAT16, "mobileclip_standard_fp16.tflite", 4096, 200)
        val PRO = ModelProfile("Pro", 512, QuantizationType.FLOAT32, "mobileclip_pro_fp32.tflite", 8192, 500)

        val ALL = listOf(LITE, STANDARD, PRO)
    }
}
