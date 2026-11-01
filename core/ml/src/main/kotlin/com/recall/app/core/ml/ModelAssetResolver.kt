package com.recall.app.core.ml

internal object ModelAssetResolver {
    private const val LEGACY_IMAGE = "models/mobileclip_s0_image_encoder_float32.tflite"
    private const val LEGACY_TEXT = "models/mobileclip_s0_text_encoder_float32.tflite"

    fun resolve(profile: ModelProfile, assetExists: (String) -> Boolean): ModelProfile {
        val textModel = requireNotNull(profile.textModelFileName) { "A text model is required" }
        if (assetExists(profile.imageModelFileName) && assetExists(textModel)) return profile

        // Older real builds bundled a matched FP32 pair under these names.
        if (profile.imageModelFileName == ModelProfile.STANDARD.imageModelFileName &&
            textModel == ModelProfile.STANDARD.textModelFileName &&
            assetExists(LEGACY_IMAGE) && assetExists(LEGACY_TEXT)
        ) {
            return profile.copy(
                quantization = QuantizationType.FLOAT32,
                imageModelFileName = LEGACY_IMAGE,
                textModelFileName = LEGACY_TEXT,
            )
        }
        error("No complete MobileCLIP image/text model pair was found in this build")
    }
}
