package com.recall.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelProfileTest {
    @Test
    fun lite_hasExpectedProperties() {
        assertEquals("Lite", ModelProfile.LITE.name)
        assertEquals(384, ModelProfile.LITE.dimensions)
        assertEquals(QuantizationType.INT8, ModelProfile.LITE.quantization)
        assertEquals("mobileclip_lite_int8.tflite", ModelProfile.LITE.modelFileName)
        assertEquals(2048, ModelProfile.LITE.minRamMb)
        assertEquals(100L, ModelProfile.LITE.expectedLatencyMs)
    }

    @Test
    fun standard_hasExpectedProperties() {
        assertEquals("Standard", ModelProfile.STANDARD.name)
        assertEquals(512, ModelProfile.STANDARD.dimensions)
        assertEquals(QuantizationType.FLOAT16, ModelProfile.STANDARD.quantization)
        assertEquals("mobileclip_standard_fp16.tflite", ModelProfile.STANDARD.modelFileName)
        assertEquals(4096, ModelProfile.STANDARD.minRamMb)
        assertEquals(200L, ModelProfile.STANDARD.expectedLatencyMs)
    }

    @Test
    fun pro_hasExpectedProperties() {
        assertEquals("Pro", ModelProfile.PRO.name)
        assertEquals(512, ModelProfile.PRO.dimensions)
        assertEquals(QuantizationType.FLOAT32, ModelProfile.PRO.quantization)
        assertEquals("mobileclip_pro_fp32.tflite", ModelProfile.PRO.modelFileName)
        assertEquals(8192, ModelProfile.PRO.minRamMb)
        assertEquals(500L, ModelProfile.PRO.expectedLatencyMs)
    }

    @Test
    fun all_containsEveryProfile() {
        assertEquals(listOf(ModelProfile.LITE, ModelProfile.STANDARD, ModelProfile.PRO), ModelProfile.ALL)
        assertTrue(ModelProfile.ALL.all { it.dimensions > 0 })
    }
}
