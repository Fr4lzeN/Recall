package com.recall.app.core.ml

enum class QuantizationType { FLOAT32, FLOAT16, INT8 }

data class ModelProfile(
    val name: String,
    val dimensions: Int,
    val quantization: QuantizationType,
    val imageModelFileName: String,
    val textModelFileName: String?,
    val inputImageSize: Int,
    val maxTextTokens: Int,
    val useSimpleNormalization: Boolean,
    val minRamMb: Int,
    val expectedLatencyMs: Long,
) {
    companion object {
        val LITE = ModelProfile(
            name = "Lite",
            dimensions = 512,
            quantization = QuantizationType.INT8,
            imageModelFileName = "models/mobileclip_s0_image_encoder_fp16.tflite",
            textModelFileName = "models/mobileclip_s0_text_encoder_fp16.tflite",
            inputImageSize = 256,
            maxTextTokens = 77,
            useSimpleNormalization = true,
            minRamMb = 2048,
            expectedLatencyMs = 100,
        )

        val STANDARD = ModelProfile(
            name = "Standard",
            dimensions = 512,
            quantization = QuantizationType.FLOAT16,
            imageModelFileName = "models/mobileclip_s0_image_encoder_fp16.tflite",
            textModelFileName = "models/mobileclip_s0_text_encoder_fp16.tflite",
            inputImageSize = 256,
            maxTextTokens = 77,
            useSimpleNormalization = true,
            minRamMb = 4096,
            expectedLatencyMs = 200,
        )

        val PRO = ModelProfile(
            name = "Pro",
            dimensions = 512,
            quantization = QuantizationType.FLOAT32,
            imageModelFileName = "models/mobileclip_s0_image_encoder_fp16.tflite",
            textModelFileName = "models/mobileclip_s0_text_encoder_fp16.tflite",
            inputImageSize = 256,
            maxTextTokens = 77,
            useSimpleNormalization = true,
            minRamMb = 8192,
            expectedLatencyMs = 500,
        )

        val ALL = listOf(LITE, STANDARD, PRO)
    }
}
