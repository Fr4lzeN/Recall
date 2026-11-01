package com.recall.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ModelAssetResolverTest {
    private val legacyImage = "models/mobileclip_s0_image_encoder_float32.tflite"
    private val legacyText = "models/mobileclip_s0_text_encoder_float32.tflite"

    @Test
    fun prefersCompleteConfiguredPairWhenBothPrecisionsExist() {
        val profile = ModelProfile.STANDARD
        val assets = setOf(profile.imageModelFileName, profile.textModelFileName, legacyImage, legacyText)
        assertSame(profile, ModelAssetResolver.resolve(profile) { it in assets })
    }

    @Test
    fun loadsCompleteLegacyPairWithHonestPrecisionAndName() {
        val assets = setOf(legacyImage, legacyText)
        val resolved = ModelAssetResolver.resolve(ModelProfile.LITE) { it in assets }
        assertEquals(legacyImage, resolved.imageModelFileName)
        assertEquals(legacyText, resolved.textModelFileName)
        assertEquals(QuantizationType.FLOAT32, resolved.quantization)
        assertEquals("MobileCLIP-S0 FP32", resolved.modelName)
        assertEquals(ModelProfile.LITE.name, resolved.name)
    }

    @Test
    fun partialConfiguredPairStillUsesMatchedLegacyPair() {
        val assets = setOf(ModelProfile.PRO.imageModelFileName, legacyImage, legacyText)
        val resolved = ModelAssetResolver.resolve(ModelProfile.PRO) { it in assets }
        assertEquals(QuantizationType.FLOAT32, resolved.quantization)
    }

    @Test(expected = IllegalStateException::class)
    fun neverMixesImageAndTextFromDifferentPairs() {
        val assets = setOf(ModelProfile.STANDARD.imageModelFileName, legacyText)
        ModelAssetResolver.resolve(ModelProfile.STANDARD) { it in assets }
    }

    @Test(expected = IllegalStateException::class)
    fun reportsMissingAssetsWithoutSwitchingToMock() {
        ModelAssetResolver.resolve(ModelProfile.STANDARD) { false }
    }
}
