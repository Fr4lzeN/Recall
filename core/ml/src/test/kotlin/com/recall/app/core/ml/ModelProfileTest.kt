package com.recall.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelProfileTest {
    @Test
    fun lite_hasExpectedProperties() {
        assertEquals("Lite", ModelProfile.LITE.name)
        assertEquals(512, ModelProfile.LITE.dimensions)
        assertEquals(QuantizationType.INT8, ModelProfile.LITE.quantization)
        assertEquals("models/mobileclip_s0_image_encoder_fp16.tflite", ModelProfile.LITE.imageModelFileName)
        assertNotNull(ModelProfile.LITE.textModelFileName)
        assertEquals("models/mobileclip_s0_text_encoder_fp16.tflite", ModelProfile.LITE.textModelFileName)
        assertEquals(256, ModelProfile.LITE.inputImageSize)
        assertEquals(77, ModelProfile.LITE.maxTextTokens)
        assertTrue(ModelProfile.LITE.useSimpleNormalization)
        assertEquals(2048, ModelProfile.LITE.minRamMb)
        assertEquals(100L, ModelProfile.LITE.expectedLatencyMs)
    }

    @Test
    fun standard_hasExpectedProperties() {
        assertEquals("Standard", ModelProfile.STANDARD.name)
        assertEquals(512, ModelProfile.STANDARD.dimensions)
        assertEquals(QuantizationType.FLOAT16, ModelProfile.STANDARD.quantization)
        assertEquals("models/mobileclip_s0_image_encoder_fp16.tflite", ModelProfile.STANDARD.imageModelFileName)
        assertNotNull(ModelProfile.STANDARD.textModelFileName)
        assertEquals(256, ModelProfile.STANDARD.inputImageSize)
        assertEquals(4096, ModelProfile.STANDARD.minRamMb)
        assertEquals(200L, ModelProfile.STANDARD.expectedLatencyMs)
    }

    @Test
    fun pro_hasExpectedProperties() {
        assertEquals("Pro", ModelProfile.PRO.name)
        assertEquals(512, ModelProfile.PRO.dimensions)
        assertEquals(QuantizationType.FLOAT32, ModelProfile.PRO.quantization)
        assertEquals("models/mobileclip_s0_image_encoder_fp16.tflite", ModelProfile.PRO.imageModelFileName)
        assertNotNull(ModelProfile.PRO.textModelFileName)
        assertEquals(256, ModelProfile.PRO.inputImageSize)
        assertEquals(8192, ModelProfile.PRO.minRamMb)
        assertEquals(500L, ModelProfile.PRO.expectedLatencyMs)
    }

    @Test
    fun all_containsEveryProfile() {
        assertEquals(listOf(ModelProfile.LITE, ModelProfile.STANDARD, ModelProfile.PRO), ModelProfile.ALL)
        assertTrue(ModelProfile.ALL.all { it.dimensions > 0 })
        assertTrue(ModelProfile.ALL.all { it.inputImageSize > 0 })
        assertTrue(ModelProfile.ALL.all { it.maxTextTokens > 0 })
    }

    @Test
    fun allProfiles_haveConsistentDimensions() {
        val dims = ModelProfile.ALL.map { it.dimensions }.toSet()
        assertEquals("All profiles should share the same embedding dimension", 1, dims.size)
        assertEquals(512, dims.first())
    }
}
